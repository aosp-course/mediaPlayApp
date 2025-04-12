package com.example.mediaplayerapp

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log


class MediaPlayerService : Service() {
    val TAG: String = "MediaPlayerService"
    private var mediaPlayer: MediaPlayer? = null

    private var audioManager: AudioManager? = null

    private var mediaSession: MediaSessionCompat? = null

    private val CHANNEL_ID: String = "MediaPlayerChannel"

    private val notificationId = 1
    private val mAudioFocusListener =
        OnAudioFocusChangeListener { focus ->
            when (focus) {
                //Pausa a media atual em caso de perda de foco
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> pauseAudio()
            }
        }

    private fun playAudio(resourceId: Int) {
        try {
            mediaPlayer!!.reset()

            mediaPlayer!!.setDataSource(this, Uri.parse("android.resource://" + packageName + "/" + resourceId))

            mediaPlayer!!.prepare()

            mediaPlayer!!.start()
            sendBroadcastResponseToApp("com.example.ACTION_PLAY")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pauseAudio() {
        if(mediaPlayer!!.isPlaying()) {
            mediaPlayer!!.pause()
            sendBroadcastResponseToApp("com.example.ACTION_PAUSE")
        }
    }

    private fun stopAudio() {
        mediaPlayer!!.stop();

        stopForeground(true);
        sendBroadcastResponseToApp("com.example.ACTION_STOP")
        stopSelf();
    }

    private fun sendBroadcastResponseToApp(message: String) {
        val intent = Intent("com.example.MEDIA_PLAYER_STATUS")
        intent.putExtra("status", message)
        sendBroadcast(intent)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
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
                    Log.e(TAG, "Ação não encontrado!")
                }
            }
        }

        return START_STICKY
    }


    private fun requestAudioFocus(onSuccess: Runnable): Boolean {
        val result: Int = audioManager!!.requestAudioFocus(
            mAudioFocusListener, AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            onSuccess.run()
            return true
        }
        Log.e(TAG, "Failed to acquire audio focus")
        return false
    }

    override fun onCreate() {
        super.onCreate()

        mediaPlayer = MediaPlayer()

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        mediaSession = MediaSessionCompat(this, "MediaPlayerService")
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer!!.release()
        mediaPlayer = null
        mediaSession!!.release()
    }
}
