package com.example.mediaplayerapp

import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.AudioTrack
import android.net.Uri
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import java.io.InputStream

/**
 * @class MediaPlayerService
 * @brief Serviço responsável por gerenciar a reprodução de áudio.
 *
 * O MediaPlayerService controla as operações de play, pause e stop
 * de uma música, gerencia o foco de áudio e envia notificações e
 * broadcasts sobre o status do player.
 */
class MediaPlayerService : Service() {

    /// Tag utilizada para logs.
    val TAG: String = "MediaPlayerService"

    /// Gerenciador de áudio para solicitar foco de áudio.
    private var audioManager: AudioManager? = null

    /// Sessão de mídia utilizada para integração com controle de mídia.
    private var mediaSession: MediaSessionCompat? = null

    /// Indica se o serviço foi parado.
    private var isServiceStopped = true

    /// ID do canal de notificações.
    private val CHANNEL_ID: String = "MediaPlayerChannel"

    /// ID da notificação.
    private val notificationId = 1

    /// Gerenciador de notificações para enviar atualizações ao usuário.
    private lateinit var notificationManager: MediaPlayerNotificationManager

    /// AudioEqualizer
    private var audioEqualizer: AudioEqualizer = AudioEqualizer()

    private var audioTrack: AudioTrack? = null
    private var playThread: Thread? = null
    private var isPlaying: Boolean = false
    private var isPaused: Boolean = false

    /**
     * Listener para mudanças de foco de áudio.
     * Pausa a música caso o foco seja perdido.
     */
    private val mAudioFocusListener =
        OnAudioFocusChangeListener { focus ->
            when (focus) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> pauseAudio()
            }
        }

    /**
     * @brief Aplica transformações ao buffer de áudio.
     * Por enquanto, retorna o buffer sem modificações.
     */
    private fun applyAudioEffects(buffer: ByteArray): ByteArray {
        var buf = buffer
        buf = audioEqualizer.applyBassMidTreble(buf, 1.0f, 1.0f, 1.0f)
        return buf
    }

    /**
     * @brief Inicia a reprodução de áudio usando AudioTrack em modo streaming.
     * Lê o arquivo wav do recurso, divide em buffers, aplica efeitos e envia para o AudioTrack.
     * @param resourceId ID do recurso de áudio a ser reproduzido.
     */
    private fun playAudio(resourceId: Int) {
        Log.i(TAG, "Música tocando via AudioTrack")
        try {
            isPlaying = true
            isPaused = false

            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val frameSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                audioFormat,
                frameSize,
                AudioTrack.MODE_STREAM
            )

            playThread = Thread {
                val inputStream: InputStream = resources.openRawResource(resourceId)
                inputStream.skip(44) // Pular cabeçalho do WAV

                var currentFrame: ByteArray? = null
                var nextFrame: ByteArray? = null
                val buffer = ByteArray(frameSize)
                var bytesRead: Int

                // Pré-carrega o primeiro frame
                bytesRead = inputStream.read(buffer, 0, frameSize)
                if (bytesRead > 0) {
                    currentFrame = buffer.copyOf(bytesRead)
                }

                // Pré-carrega o próximo frame
                if (inputStream.available() > 0) {
                    bytesRead = inputStream.read(buffer, 0, frameSize)
                    if (bytesRead > 0) {
                        nextFrame = buffer.copyOf(bytesRead)
                    }
                }

                audioTrack?.play()

                while (isPlaying && currentFrame != null) {
                    if (isPaused) {
                        Thread.sleep(100)
                        continue
                    }
                    // Processa e envia o frame atual
                    val processedFrame = applyAudioEffects(currentFrame)

                    Log.d(TAG, "Enviando frame de tamanho: ${processedFrame.size}")
                    audioTrack?.write(processedFrame, 0, processedFrame.size)

                    // TODO: Wait until current frame is over

                    currentFrame = nextFrame
                    nextFrame = null

                    // Lê um novo frame para atualizar o próximo
                    if (isPlaying && inputStream.available() > 0) {
                        bytesRead = inputStream.read(buffer, 0, frameSize)
                        if (bytesRead > 0) {
                            nextFrame = buffer.copyOf(bytesRead)
                        }
                    }
                }
                inputStream.close()
            }
            playThread?.start()
            sendBroadcastResponseToApp("com.example.ACTION_PLAY")
            notificationManager.showNotification("Música tocando")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar reprodução via AudioTrack", e)
        }
    }

    /**
     * @brief Pausa a reprodução de áudio.
     */
    private fun pauseAudio() {
        if (isPlaying && !isPaused) {
            Log.i(TAG, "Música pausada via AudioTrack")
            isPaused = true
            audioTrack?.pause()
            sendBroadcastResponseToApp("com.example.ACTION_PAUSE")
            notificationManager.showNotification("Música pausada")
        }
    }

    /**
     * @brief Para a reprodução de áudio e finaliza o serviço.
     */
    private fun stopAudio() {
        Log.i(TAG, "Música parada via AudioTrack")
        isPlaying = false
        isPaused = false
        playThread?.join(500) // Aguarda a thread terminar
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        notificationManager.showNotification("Música parada")
        sendBroadcastResponseToApp("com.example.ACTION_STOP")
        stopForeground(true)
        stopSelf()
        isServiceStopped = true
    }

    /**
     * @brief Envia um broadcast com o status atual do player.
     * @param message Mensagem indicando o status do player.
     */
    private fun sendBroadcastResponseToApp(message: String) {
        val intent = Intent("com.example.MEDIA_PLAYER_STATUS")
        intent.putExtra("status", message)
        sendBroadcast(intent)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    /**
     * @brief Manipula comandos de início do serviço.
     * @param intent Intent que contém a ação a ser executada.
     * @param flags Flags adicionais.
     * @param startId Identificador do start request.
     * @return Código de retorno indicando como o sistema deve continuar o serviço.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            initServiceIfRequired()
            val action = intent.action
            if (action != null) when (action) {
                "com.example.ACTION_PLAY" -> {
                    val resourceId = intent.getIntExtra("resourceId", 0)
                    requestAudioFocus {
                        if (resourceId != 0) {
                            playAudio(resourceId)
                        }
                    }
                }
                "com.example.ACTION_PAUSE" -> pauseAudio()
                "com.example.ACTION_STOP" -> stopAudio()
                else -> {
                    Log.e(TAG, "Ação não encontrada!")
                }
            }
        }

        return START_STICKY
    }

    /**
     * @brief Solicita foco de áudio.
     * @param onSuccess Runnable a ser executado caso o foco seja concedido.
     * @return Verdadeiro se o foco foi concedido, falso caso contrário.
     */
    private fun requestAudioFocus(onSuccess: Runnable): Boolean {
        val result: Int = audioManager!!.requestAudioFocus(
            mAudioFocusListener, AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            onSuccess.run()
            return true
        }
        Log.e(TAG, "Falha ao tentar requisitar foco de áudio")
        return false
    }

    /**
     * @brief Inicializa o serviço se necessário.
     */
    private fun initServiceIfRequired() {
        if (isServiceStopped) {
            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            mediaSession = MediaSessionCompat(this, "MediaPlayerService")
            notificationManager = MediaPlayerNotificationManager(this, CHANNEL_ID)
            isServiceStopped = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        initServiceIfRequired()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioTrack?.release()
        audioTrack = null
        mediaSession!!.release()
    }
}
