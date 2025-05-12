package com.example.mediaplayerapp

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat as AndroidMediaFormat
import android.util.Log
import java.io.Closeable
import java.nio.ByteBuffer

/**
 * Motor de reprodução de áudio que gerencia decodificação, equalização e reprodução.
 * Responsável por coordenar o ciclo de vida completo da reprodução de áudio.
 */
class AudioPlaybackEngine(
    private val context: Context,
    private val audioEqualizer: AudioEqualizer = AudioEqualizer(),
    private val logger: Logger = DefaultLogger("AudioPlaybackEngine")
) : Closeable {

    // Estados possíveis do mecanismo de reprodução
    enum class State { IDLE, PLAYING, PAUSED, ERROR }
    
    // Estado atual da reprodução
    private var state: State = State.IDLE
    
    // Componentes de mídia Android
    private var mediaExtractor: MediaExtractor? = null
    private var mediaCodec: MediaCodec? = null
    private var audioTrack: AudioTrack? = null
    
    // Thread de reprodução
    private var playThread: Thread? = null
    private var currentResourceId: Int = 0
    
    // Configurações de equalização
    private data class EqualizerSettings(
        var bassDb: Float = 0.0f,
        var midDb: Float = 0.0f,
        var trebleDb: Float = 0.0f
    )
    
    private val eqSettings = EqualizerSettings()
    private var currentSampleRate: Int = 44100

    /**
     * Inicia a reprodução do recurso de áudio especificado
     * @param resourceId ID do recurso raw de áudio
     */
    fun play(resourceId: Int) {
        if (state == State.PLAYING || state == State.PAUSED) {
            stop()
        }
        
        currentResourceId = resourceId
        state = State.PLAYING

        playThread = Thread {
            try {
                setupMediaComponents(resourceId)
                processAudioLoop()
            } catch (e: Exception) {
                logger.error("Erro na reprodução", e)
                state = State.ERROR
            } finally {
                cleanupResources()
                if (state != State.ERROR) {
                    state = State.IDLE
                }
            }
        }.apply {
            name = "AudioPlaybackEngineThread"
            start()
        }
    }

    /**
     * Pausa a reprodução atual
     */
    fun pause() {
        if (state == State.PLAYING) {
            state = State.PAUSED
            audioTrack?.pause()
        }
    }

    /**
     * Retoma a reprodução pausada
     */
    fun resume() {
        if (state == State.PAUSED) {
            state = State.PLAYING
            audioTrack?.play()
        }
    }

    /**
     * Para a reprodução e libera todos os recursos
     */
    fun stop() {
        if (state == State.IDLE) return
        
        state = State.IDLE
        playThread?.interrupt()
        playThread?.join(500)
        cleanupResources()
    }

    /**
     * Configura o nível de graves da equalização
     * @param db Ganho em decibéis (-12 a +12)
     */
    fun setBass(db: Float) { 
        eqSettings.bassDb = db 
    }

    /**
     * Configura o nível de médios da equalização
     * @param db Ganho em decibéis (-12 a +12)
     */
    fun setMid(db: Float) { 
        eqSettings.midDb = db 
    }

    /**
     * Configura o nível de agudos da equalização
     * @param db Ganho em decibéis (-12 a +12)
     */
    fun setTreble(db: Float) { 
        eqSettings.trebleDb = db 
    }

    /**
     * Retorna o estado atual da reprodução
     */
    fun getState(): State = state

    /**
     * Libera todos os recursos quando o objeto for fechado
     */
    override fun close() {
        stop()
    }
    
    // ====== Métodos privados de implementação ======

    private fun setupMediaComponents(resourceId: Int) {
        // 1. Configura o extrator de mídia
        mediaExtractor = MediaExtractor().apply {
            val afd = context.resources.openRawResourceFd(resourceId)
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
        }
        
        // 2. Encontra a faixa de áudio
        val audioFormat = findAudioTrack() ?: throw IllegalStateException("Nenhuma faixa de áudio encontrada")
        
        // 3. Configura o codec para decodificação
        val mimeType = audioFormat.getString(AndroidMediaFormat.KEY_MIME)!!
        mediaCodec = MediaCodec.createDecoderByType(mimeType).apply {
            configure(audioFormat, null, null, 0)
            start()
        }
        
        // 4. Configura o AudioTrack para reprodução
        setupAudioTrack(audioFormat)
    }
    
    private fun findAudioTrack(): AndroidMediaFormat? {
        for (i in 0 until (mediaExtractor?.trackCount ?: 0)) {
            mediaExtractor!!.getTrackFormat(i).let { format ->
                if (format.getString(AndroidMediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    mediaExtractor!!.selectTrack(i)
                    currentSampleRate = format.getInteger(AndroidMediaFormat.KEY_SAMPLE_RATE)
                    logger.debug("Faixa de áudio encontrada com SR=${currentSampleRate}Hz")
                    return format
                }
            }
        }
        return null
    }
    
    private fun setupAudioTrack(audioFormat: AndroidMediaFormat) {
        val channelCount = audioFormat.getInteger(AndroidMediaFormat.KEY_CHANNEL_COUNT)
        val channelConfig = if (channelCount == 1) 
            AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
            
        val minBufSize = AudioTrack.getMinBufferSize(
            currentSampleRate, 
            channelConfig, 
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            currentSampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufSize,
            AudioTrack.MODE_STREAM
        ).apply { play() }
    }
    
    private fun processAudioLoop() {
        val codec = mediaCodec!!
        val inBufs = codec.inputBuffers
        var outBufs = codec.outputBuffers
        val bufferInfo = MediaCodec.BufferInfo()
        
        var sawInputEOS = false
        var sawOutputEOS = false
        val timeoutUs = 10_000L
        
        while (state != State.IDLE && !sawOutputEOS && !Thread.currentThread().isInterrupted) {
            if (state == State.PAUSED) { 
                Thread.sleep(100)
                continue 
            }
            
            // Processa buffers de entrada
            if (!sawInputEOS) {
                processInputBuffer(codec, inBufs, timeoutUs, sawInputEOS)?.let { sawInputEOS = it }
            }
            
            // Processa buffers de saída
            val result = processOutputBuffer(codec, outBufs, bufferInfo, timeoutUs)
            when (result) {
                is OutputResult.BuffersChanged -> outBufs = result.newBuffers
                is OutputResult.EndOfStream -> sawOutputEOS = true
                else -> { /* Continua o processamento */ }
            }
        }
    }
    
    private fun processInputBuffer(
        codec: MediaCodec, 
        inBufs: Array<ByteBuffer>,
        timeoutUs: Long,
        currentInputEOS: Boolean
    ): Boolean? {
        if (currentInputEOS) return null
        
        val inIdx = codec.dequeueInputBuffer(timeoutUs)
        if (inIdx >= 0) {
            val buf = inBufs[inIdx]
            val sampleSize = mediaExtractor!!.readSampleData(buf, 0)
            val presentationTimeUs = if (sampleSize > 0) mediaExtractor!!.sampleTime else 0L
            
            val isEOS = sampleSize < 0
            codec.queueInputBuffer(
                inIdx, 
                0, 
                if (isEOS) 0 else sampleSize, 
                presentationTimeUs,
                if (isEOS) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
            )
            
            if (!isEOS) mediaExtractor!!.advance()
            return isEOS
        }
        return null
    }
    
    private sealed class OutputResult {
        object Continue : OutputResult()
        data class BuffersChanged(val newBuffers: Array<ByteBuffer>) : OutputResult()
        object EndOfStream : OutputResult()
    }
    
    private fun processOutputBuffer(
        codec: MediaCodec,
        outBufs: Array<ByteBuffer>,
        info: MediaCodec.BufferInfo,
        timeoutUs: Long
    ): OutputResult {
        val outIdx = codec.dequeueOutputBuffer(info, timeoutUs)
        
        return when {
            outIdx >= 0 -> {
                val buf = outBufs[outIdx]
                val pcm = ByteArray(info.size).also { buf.get(it); buf.clear() }
                val processed = applyAudioEffects(pcm)
                audioTrack?.write(processed, 0, processed.size)
                codec.releaseOutputBuffer(outIdx, false)
                
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    OutputResult.EndOfStream
                } else {
                    OutputResult.Continue
                }
            }
            outIdx == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                OutputResult.BuffersChanged(codec.outputBuffers)
            }
            else -> OutputResult.Continue
        }
    }
    
    private fun applyAudioEffects(buffer: ByteArray): ByteArray {
        if (buffer.isEmpty()) return buffer
        
        val copy = buffer.copyOf()
        return try {
            audioEqualizer.applyBassMidTreble(
                copy, 
                eqSettings.bassDb, 
                eqSettings.midDb, 
                eqSettings.trebleDb, 
                currentSampleRate
            )
        } catch (e: Throwable) {
            logger.error("Erro ao aplicar efeitos de áudio", e)
            copy
        }
    }
    
    private fun cleanupResources() {
        releaseAudioTrack()
        releaseMediaCodec()
        releaseMediaExtractor()
        playThread = null
    }
    
    private fun releaseAudioTrack() {
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.pause()
                it.flush()
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            logger.error("Erro ao liberar AudioTrack", e)
        } finally {
            audioTrack = null
        }
    }
    
    private fun releaseMediaCodec() {
        try {
            mediaCodec?.apply { 
                stop()
                release() 
            }
        } catch (e: Exception) {
            logger.error("Erro ao liberar MediaCodec", e)
        } finally {
            mediaCodec = null
        }
    }
    
    private fun releaseMediaExtractor() {
        try {
            mediaExtractor?.release()
        } catch (e: Exception) {
            logger.error("Erro ao liberar MediaExtractor", e)
        } finally {
            mediaExtractor = null
        }
    }
    
    /**
     * Interface para logging que permite substituição nos testes
     */
    interface Logger {
        fun debug(message: String)
        fun info(message: String)
        fun error(message: String, throwable: Throwable? = null)
    }
    
    /**
     * Implementação padrão do Logger usando Android Log
     */
    private class DefaultLogger(private val tag: String) : Logger {
        override fun debug(message: String) { Log.d(tag, message) }
        override fun info(message: String) { Log.i(tag, message) }
        override fun error(message: String, throwable: Throwable?)
            { if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message) }
    }
}
