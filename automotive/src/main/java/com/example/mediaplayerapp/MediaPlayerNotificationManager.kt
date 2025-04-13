package com.example.mediaplayerapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class MediaPlayerNotificationManager(private val context: Context, private val channelId: String) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Media Player",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(status: String) {
        val notification: Notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Media Player")
            .setContentText(status)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(1, notification)
    }

    fun cancelNotification() {
        notificationManager.cancel(1)
    }
}
