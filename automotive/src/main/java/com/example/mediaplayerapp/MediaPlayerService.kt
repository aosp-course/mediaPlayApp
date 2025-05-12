package com.example.mediaplayerapp

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log

open class MediaPlayerService : Service() {

    val TAG: String = "MediaPlayerService"

    protected var audioManager: AudioManager? = null
    protected var mediaSession: MediaSessionCompat? = null
    protected var isServiceStopped = true
    private val CHANNEL_ID: String = "MediaPlayerChannel"
    private val notificationId = 1
    protected lateinit var notificationManager: MediaPlayerNotificationManager
    protected lateinit var audioEngine: AudioPlaybackEngine

    companion object {
        const val ACTION_PLAY = "com.example.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_STOP = "com.example.ACTION_STOP"
        const val ACTION_UPDATE_EQ = "com.example.ACTION_UPDATE_EQ"
        const val EXTRA_BASS_DB = "com.example.EXTRA_BASS_DB"
        const val EXTRA_MID_DB = "com.example.EXTRA_MID_DB"
        const val EXTRA_TREBLE_DB = "com.example.EXTRA_TREBLE_DB"
    }

    protected val mAudioFocusListener = OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS ->
                audioEngine.stop()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                audioEngine.pause()
            AudioManager.AUDIOFOCUS_GAIN ->
                audioEngine.resume()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "MediaPlayerService onCreate chamado.")
        initServiceIfRequired()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Log.w(TAG, "onStartCommand recebido com Intent nulo. Ignorando.")
            return START_NOT_STICKY
        }

        initServiceIfRequired()
        val action = intent.action
        Log.i(TAG, "onStartCommand recebido com ação: $action")

        when (action) {
            ACTION_PLAY -> {
                val resourceId = intent.getIntExtra("resourceId", 0)
                if (resourceId != 0) {
                    requestAudioFocus {
                        Log.i(TAG, "Foco de áudio concedido. Iniciando playAudio para resourceId: $resourceId")
                        audioEngine.play(resourceId)
                        sendBroadcastResponseToApp(ACTION_PLAY)
                        notificationManager.showNotification("Música tocando")
                    }
                } else {
                    Log.w(TAG, "ACTION_PLAY recebido sem resourceId válido.")
                }
            }
            ACTION_PAUSE -> {
                Log.i(TAG, "Ação PAUSE recebida.")
                audioEngine.pause()
                sendBroadcastResponseToApp(ACTION_PAUSE)
                notificationManager.showNotification("Música pausada")
            }
            ACTION_STOP -> {
                audioEngine.stop()
                sendBroadcastResponseToApp(ACTION_STOP)
                notificationManager.showNotification("Música parada")
                stopForeground(true)
                stopSelf()
                isServiceStopped = true
            }
            ACTION_UPDATE_EQ -> {
                audioEngine.setBass(intent.getFloatExtra(EXTRA_BASS_DB, 0.0f))
                audioEngine.setMid(intent.getFloatExtra(EXTRA_MID_DB, 0.0f))
                audioEngine.setTreble(intent.getFloatExtra(EXTRA_TREBLE_DB, 0.0f))
                Log.i(TAG, "EQ atualizado via Intent: Bass=${intent.getFloatExtra(EXTRA_BASS_DB, 0.0f)}dB, " +
                      "Mid=${intent.getFloatExtra(EXTRA_MID_DB, 0.0f)}dB, " +
                      "Treble=${intent.getFloatExtra(EXTRA_TREBLE_DB, 0.0f)}dB")
            }
            else -> Log.e(TAG, "Ação desconhecida ou nula recebida: $action")
        }
        return START_NOT_STICKY
    }

    protected open fun requestAudioFocus(onSuccess: Runnable): Boolean {
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

    protected open fun initServiceIfRequired() {
        if (isServiceStopped) {
            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            mediaSession = MediaSessionCompat(this, "MediaPlayerServiceTag")
            notificationManager = MediaPlayerNotificationManager(this, CHANNEL_ID)
            audioEngine = AudioPlaybackEngine(this)
            isServiceStopped = false
            Log.i(TAG, "Serviço inicializado (AudioManager, MediaSession, NotificationManager, AudioEngine).")
        }
    }

    protected open fun sendBroadcastResponseToApp(message: String) {
        val intent = Intent("com.example.MEDIA_PLAYER_STATUS")
        intent.putExtra("status", message)
        sendBroadcast(intent)
        Log.d(TAG, "Broadcast enviado: $message")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "MediaPlayerService onDestroy chamado. Liberando recursos...")
        audioEngine.stop()
        audioManager?.abandonAudioFocus(mAudioFocusListener)
        mediaSession?.release()
        mediaSession = null
        audioManager = null
        Log.i(TAG, "Recursos do serviço (foco, MediaSession) liberados.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
