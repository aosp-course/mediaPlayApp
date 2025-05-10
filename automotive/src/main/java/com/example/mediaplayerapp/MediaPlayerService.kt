package com.example.mediaplayerapp

import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat as AndroidMediaFormat
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log

class MediaPlayerService : Service() {

    val TAG: String = "MediaPlayerService"

    private var audioManager: AudioManager? = null
    private var mediaSession: MediaSessionCompat? = null
    private var isServiceStopped = true
    private val CHANNEL_ID: String = "MediaPlayerChannel"
    private val notificationId = 1
    private lateinit var notificationManager: MediaPlayerNotificationManager

    // Instância da sua classe AudioEqualizer que carrega a lib nativa e declara o método JNI
    private val audioEqualizer: AudioEqualizer = AudioEqualizer()

    // Componentes para MediaCodec
    private var mediaExtractor: MediaExtractor? = null
    private var mediaCodec: MediaCodec? = null
    private var audioTrack: AudioTrack? = null

    private var playThread: Thread? = null
    private var isPlaying: Boolean = false
    private var isPausedInternal: Boolean = false
    private var isPausedBeforeFocusLoss: Boolean = false
    private var currentResourceId: Int = 0

    // Variáveis para armazenar os ganhos atuais do EQ em dB
    private var currentBassDb: Float = 0.0f
    private var currentMidDb: Float = 0.0f
    private var currentTrebleDb: Float = 0.0f
    // Armazena a taxa de amostragem da música atual para passar para o JNI
    private var currentSampleRateForJNI: Int = 44100 // Valor padrão, será atualizado

    companion object {
        // Ações para o Service
        const val ACTION_PLAY = "com.example.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_STOP = "com.example.ACTION_STOP"
        const val ACTION_UPDATE_EQ = "com.example.ACTION_UPDATE_EQ"

        // Extras para o Intent de EQ
        const val EXTRA_BASS_DB = "com.example.EXTRA_BASS_DB"
        const val EXTRA_MID_DB = "com.example.EXTRA_MID_DB"
        const val EXTRA_TREBLE_DB = "com.example.EXTRA_TREBLE_DB"
        // (Não precisamos de EXTRA_SAMPLE_RATE aqui, pois o serviço obtém do MediaFormat)
    }

    private val mAudioFocusListener =
        OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    if (isPlaying) {
                        isPausedBeforeFocusLoss = isPlaying && !isPausedInternal
                        stopAudioInternal()
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if (isPlaying && !isPausedInternal) {
                        isPausedBeforeFocusLoss = true
                        pauseAudioInternal(true)
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    if (isPlaying && !isPausedInternal) {
                        isPausedBeforeFocusLoss = true
                        pauseAudioInternal(true)
                    }
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (isPausedBeforeFocusLoss) {
                        resumeAudioInternal()
                        isPausedBeforeFocusLoss = false
                    }
                }
            }
        }

    /**
     * Aplica efeitos de áudio (como equalização) ao buffer PCM decodificado via JNI,
     * usando a instância de AudioEqualizer.
     * @param buffer O buffer de áudio PCM.
     * @param bytesRead O número de bytes válidos no buffer.
     * @param sampleRate A taxa de amostragem do áudio atual.
     * @return O buffer de áudio PCM com os efeitos aplicados, ou o buffer original se o JNI falhar.
     */
    private fun applyAudioEffects(buffer: ByteArray, bytesRead: Int, sampleRate: Int): ByteArray {
        if (bytesRead <= 0) {
            // Retorna uma cópia do buffer original se não houver dados válidos
            return buffer.copyOf(bytesRead)
        }

        // Cria uma cópia exata do buffer com os bytes válidos para passar para o JNI.
        // Isso evita que o JNI tente ler além dos dados válidos se o buffer original for maior.
        val exactBuffer = buffer.copyOf(bytesRead)

        return try {
            // Chama o método da instância de AudioEqualizer.
            // AVISO: Certifique-se que AudioEqualizer.kt e a função JNI C++
            //        ambos aceitam 'sampleRate' como o último parâmetro.
            audioEqualizer.applyBassMidTreble(
                exactBuffer,
                currentBassDb,
                currentMidDb,
                currentTrebleDb,
                sampleRate // Passa a taxa de amostragem correta para o JNI
            )
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Erro de link ao chamar JNI (AudioEqualizer.applyBassMidTreble): ${e.message}", e)
            exactBuffer // Retorna o buffer original em caso de erro de link para não quebrar a reprodução
        } catch (e: Exception) {
            Log.e(TAG, "Exceção ao chamar JNI (AudioEqualizer.applyBassMidTreble): ${e.message}", e)
            exactBuffer // Retorna o buffer original em caso de outra exceção
        }
    }

    private fun playAudio(resourceId: Int) {
        if (isPlaying) {
            stopAudioInternal()
        }
        currentResourceId = resourceId
        isPausedInternal = false
        isPlaying = true

        playThread = Thread {
            try {
                mediaExtractor = MediaExtractor()
                val afd = resources.openRawResourceFd(currentResourceId)
                mediaExtractor!!.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

                var trackIndex = -1
                var audioMediaFormat: AndroidMediaFormat? = null // Renomeado para clareza
                for (i in 0 until mediaExtractor!!.trackCount) {
                    val currentTrackFormat = mediaExtractor!!.getTrackFormat(i)
                    val mime = currentTrackFormat.getString(AndroidMediaFormat.KEY_MIME)
                    if (mime?.startsWith("audio/") == true) {
                        mediaExtractor!!.selectTrack(i)
                        audioMediaFormat = currentTrackFormat
                        trackIndex = i
                        // Obter e armazenar a taxa de amostragem real aqui!
                        currentSampleRateForJNI = audioMediaFormat.getInteger(AndroidMediaFormat.KEY_SAMPLE_RATE)
                        Log.i(TAG, "Faixa de áudio encontrada: Mime=$mime, SR=${currentSampleRateForJNI}Hz")
                        break
                    }
                }

                if (trackIndex == -1 || audioMediaFormat == null) {
                    Log.e(TAG, "Nenhuma faixa de áudio válida encontrada no resourceId: $currentResourceId")
                    cleanupAfterPlayback()
                    return@Thread
                }

                val mimeType = audioMediaFormat.getString(AndroidMediaFormat.KEY_MIME)!!
                mediaCodec = MediaCodec.createDecoderByType(mimeType)
                mediaCodec!!.configure(audioMediaFormat, null, null, 0)
                mediaCodec!!.start()

                // Usa currentSampleRateForJNI (obtido do MediaFormat) para configurar o AudioTrack
                val channelCount = audioMediaFormat.getInteger(AndroidMediaFormat.KEY_CHANNEL_COUNT)
                val channelConfig = if (channelCount == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
                val pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

                val minBufferSize = AudioTrack.getMinBufferSize(currentSampleRateForJNI, channelConfig, pcmEncoding)
                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    currentSampleRateForJNI, // Taxa de amostragem correta
                    channelConfig,
                    pcmEncoding,
                    minBufferSize,
                    AudioTrack.MODE_STREAM
                )
                audioTrack!!.play()
                sendBroadcastResponseToApp(ACTION_PLAY)
                notificationManager.showNotification("Música tocando")

                val codecInputBuffers = mediaCodec!!.inputBuffers
                var codecOutputBuffers = mediaCodec!!.outputBuffers
                val bufferInfo = MediaCodec.BufferInfo()
                var sawInputEOS = false
                var sawOutputEOS = false
                val timeoutUs = 10000L // 10ms

                while (isPlaying && !sawOutputEOS && !Thread.currentThread().isInterrupted) {
                    if (isPausedInternal) {
                        try { Thread.sleep(100) } catch (e: InterruptedException) {
                            Log.w(TAG, "playThread interrompida durante pausa.")
                            Thread.currentThread().interrupt(); break // Sai do loop se interrompida
                        }
                        continue
                    }

                    if (!sawInputEOS) {
                        val inputBufIndex = mediaCodec!!.dequeueInputBuffer(timeoutUs)
                        if (inputBufIndex >= 0) {
                            val dstBuf = codecInputBuffers[inputBufIndex]
                            var sampleSize = mediaExtractor!!.readSampleData(dstBuf, 0)
                            var presentationTimeUs: Long = 0
                            if (sampleSize < 0) {
                                sawInputEOS = true; sampleSize = 0
                            } else {
                                presentationTimeUs = mediaExtractor!!.sampleTime
                            }
                            mediaCodec!!.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs,
                                if (sawInputEOS) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                            if (!sawInputEOS) mediaExtractor!!.advance()
                        }
                    }

                    val outputBufIndex = mediaCodec!!.dequeueOutputBuffer(bufferInfo, timeoutUs)
                    if (outputBufIndex >= 0) {
                        val outputBuffer = codecOutputBuffers[outputBufIndex] // Renomeado para clareza
                        val chunkPCM = ByteArray(bufferInfo.size)
                        outputBuffer.get(chunkPCM)
                        outputBuffer.clear() // Boa prática, embora get() avance a posição

                        if (chunkPCM.isNotEmpty()) {
                            // Passa a sampleRate correta (currentSampleRateForJNI) para applyAudioEffects
                            val processedChunk = applyAudioEffects(chunkPCM, bufferInfo.size, currentSampleRateForJNI)
                            audioTrack?.write(processedChunk, 0, processedChunk.size)
                        }
                        mediaCodec!!.releaseOutputBuffer(outputBufIndex, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            sawOutputEOS = true
                            Log.i(TAG, "Fim do stream de saída (EOS) alcançado.")
                        }
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        codecOutputBuffers = mediaCodec!!.outputBuffers
                        Log.d(TAG, "Buffers de saída do MediaCodec mudaram.")
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // O formato de saída do MediaCodec mudou (raro para decodificação de áudio para PCM consistente)
                        // val newOutputFormat = mediaCodec!!.outputFormat
                        // Log.d(TAG, "Formato de saída do MediaCodec mudou para: $newOutputFormat")
                    }
                }
            } catch (e: Exception) { // Captura mais genérica para erros inesperados na thread
                Log.e(TAG, "Erro na thread de reprodução playAudio", e)
            } finally {
                Log.i(TAG, "playAudio thread finalizando. Chamando cleanupAfterPlayback.")
                cleanupAfterPlayback()
                // Se o loop terminou mas isPlaying ainda era true (ex: erro, ou EOS), reseta.
                if (isPlaying) {
                    isPlaying = false
                    // Se a música acabou (EOS), você pode querer parar o serviço ou ir para a próxima.
                    // Para este exemplo, apenas paramos a reprodução ativa.
                }
            }
        }
        playThread!!.name = "MediaPlayerServicePlayThread"
        playThread!!.start()
    }


    private fun cleanupAfterPlayback() {
        Log.d(TAG, "Iniciando cleanupAfterPlayback...")
        try {
            if (audioTrack != null) {
                if (audioTrack!!.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack!!.pause()
                }
                audioTrack!!.flush()
                audioTrack!!.stop()
                audioTrack!!.release()
                Log.i(TAG, "AudioTrack liberado.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao liberar AudioTrack", e)
        } finally {
            audioTrack = null
        }

        try {
            if (mediaCodec != null) {
                mediaCodec!!.stop()
                mediaCodec!!.release()
                Log.i(TAG, "MediaCodec liberado.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao liberar MediaCodec", e)
        } finally {
            mediaCodec = null
        }

        try {
            if (mediaExtractor != null) {
                mediaExtractor!!.release()
                Log.i(TAG, "MediaExtractor liberado.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao liberar MediaExtractor", e)
        } finally {
            mediaExtractor = null
        }
        Log.i(TAG, "cleanupAfterPlayback concluído.")
    }

    private fun pauseAudio() {
        pauseAudioInternal(false)
    }

    // private fun resumeAudio() { // Descomente se precisar de uma ação RESUME explícita
    //    resumeAudioInternal()
    // }

    private fun stopAudio() {
        Log.i(TAG, "Ação STOP recebida. Parando áudio e serviço.")
        stopAudioInternal()
        notificationManager.showNotification("Música parada")
        sendBroadcastResponseToApp(ACTION_STOP)
        stopForeground(true)
        stopSelf()
        isServiceStopped = true
    }

    private fun pauseAudioInternal(dueToFocusLoss: Boolean) {
        if (isPlaying && !isPausedInternal) {
            Log.i(TAG, "Pausando áudio internamente. Devido à perda de foco: $dueToFocusLoss")
            isPausedInternal = true
            audioTrack?.pause()
            if (!dueToFocusLoss) {
                sendBroadcastResponseToApp(ACTION_PAUSE)
                notificationManager.showNotification("Música pausada")
            }
        }
    }

    private fun resumeAudioInternal() {
        if (isPlaying && isPausedInternal) {
            Log.i(TAG, "Retomando áudio internamente.")
            isPausedInternal = false
            audioTrack?.play()
            sendBroadcastResponseToApp(ACTION_PLAY) // Ou uma nova ação ACTION_RESUME
            notificationManager.showNotification("Música tocando")
        } else if (!isPlaying && currentResourceId != 0) {
            Log.i(TAG, "Retomando áudio (estava parado), iniciando nova reprodução para resourceId: $currentResourceId")
            playAudio(currentResourceId) // Reinicia a reprodução se estava completamente parado
        }
    }

    private fun stopAudioInternal() {
        Log.i(TAG, "Iniciando parada interna do áudio.")
        isPlaying = false
        isPausedInternal = false
        if (playThread?.isAlive == true) {
            Log.d(TAG, "Tentando interromper e juntar-se à playThread.")
            playThread?.interrupt()
            try {
                playThread?.join(1000) // Espera até 1 segundo
                if (playThread?.isAlive == true) {
                    Log.w(TAG, "playThread não terminou após join. Pode indicar um problema.")
                } else {
                    Log.d(TAG, "playThread finalizada com sucesso.")
                }
            } catch (e: InterruptedException) {
                Log.w(TAG, "Thread principal interrompida enquanto esperava playThread terminar.")
                Thread.currentThread().interrupt()
            }
        }
        playThread = null
        cleanupAfterPlayback()
        Log.i(TAG, "Parada interna do áudio concluída.")
    }

    private fun sendBroadcastResponseToApp(message: String) {
        val intent = Intent("com.example.MEDIA_PLAYER_STATUS")
        intent.putExtra("status", message)
        sendBroadcast(intent)
        Log.d(TAG, "Broadcast enviado: $message")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Log.w(TAG, "onStartCommand recebido com Intent nulo. Ignorando.")
            return START_NOT_STICKY // Evita recriar se morto e o intent era nulo
        }

        initServiceIfRequired()
        val action = intent.action
        Log.i(TAG, "onStartCommand recebido com ação: $action")

        when (action) {
            ACTION_PLAY -> {
                val resourceId = intent.getIntExtra("resourceId", 0)
                if (resourceId != 0) {
                    currentResourceId = resourceId
                    requestAudioFocus {
                        Log.i(TAG, "Foco de áudio concedido. Iniciando playAudio para resourceId: $resourceId")
                        playAudio(resourceId)
                    }
                } else {
                    Log.w(TAG, "ACTION_PLAY recebido sem resourceId válido.")
                }
            }
            ACTION_PAUSE -> {
                Log.i(TAG, "Ação PAUSE recebida.")
                pauseAudio()
            }
            ACTION_STOP -> {
                // O stopAudio() já foi definido acima, então não precisa de log duplicado aqui.
                stopAudio()
            }
            ACTION_UPDATE_EQ -> {
                currentBassDb = intent.getFloatExtra(EXTRA_BASS_DB, 0.0f)
                currentMidDb = intent.getFloatExtra(EXTRA_MID_DB, 0.0f)
                currentTrebleDb = intent.getFloatExtra(EXTRA_TREBLE_DB, 0.0f)
                Log.i(TAG, "EQ atualizado via Intent: Bass=${currentBassDb}dB, Mid=${currentMidDb}dB, Treble=${currentTrebleDb}dB")
                // Os novos valores serão usados automaticamente no próximo buffer processado pela playThread.
            }
            else -> Log.e(TAG, "Ação desconhecida ou nula recebida: $action")
        }
        return START_NOT_STICKY
    }

    private fun requestAudioFocus(onSuccess: Runnable): Boolean {
        if (audioManager == null) {
            Log.e(TAG, "AudioManager não inicializado ao tentar solicitar foco.")
            return false
        }
        val result: Int = audioManager!!.requestAudioFocus(
            mAudioFocusListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            onSuccess.run()
            return true
        }
        Log.e(TAG, "Falha ao requisitar foco de áudio. Resultado: $result")
        return false
    }

    private fun initServiceIfRequired() {
        if (isServiceStopped) {
            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            mediaSession = MediaSessionCompat(this, "MediaPlayerServiceTag")
            // Configurar MediaSession se necessário (para controles de mídia, etc.)
            // mediaSession?.isActive = true
            notificationManager = MediaPlayerNotificationManager(this, CHANNEL_ID)
            isServiceStopped = false
            Log.i(TAG, "Serviço inicializado (AudioManager, MediaSession, NotificationManager).")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "MediaPlayerService onCreate chamado.")
        initServiceIfRequired()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "MediaPlayerService onDestroy chamado. Liberando recursos...")
        stopAudioInternal() // Garante que a reprodução pare e libere recursos de mídia
        audioManager?.abandonAudioFocus(mAudioFocusListener)
        mediaSession?.release()
        mediaSession = null
        audioManager = null
        Log.i(TAG, "Recursos do serviço (foco, MediaSession) liberados.")
    }
}
