package com.example.mediaplayerapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * @class MediaPlayerNotificationManager
 * @brief Gerencia notificações para o status do Media Player.
 *
 * O MediaPlayerNotificationManager é responsável por criar e exibir notificações
 * relacionadas ao status do Media Player, como tocar, pausar ou parar.
 */
class MediaPlayerNotificationManager(private val context: Context, private val channelId: String) {

    /// Gerenciador de notificações do sistema.
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * @brief Inicializa o gerenciador de notificações, criando o canal de notificação se necessário.
     */
    init {
        createNotificationChannel()
    }

    /**
     * @brief Cria um canal de notificação para dispositivos Android Oreo (API 26) e superiores.
     */
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

    /**
     * @brief Exibe uma notificação com o status atual do Media Player.
     * @param status Mensagem de status a ser exibida na notificação.
     */
    fun showNotification(status: String) {
        val notification: Notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Media Player")
            .setContentText(status)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(1, notification)
    }

    /**
     * @brief Cancela a notificação atualmente exibida.
     */
    fun cancelNotification() {
        notificationManager.cancel(1)
    }
}
