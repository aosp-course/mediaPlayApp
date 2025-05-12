package com.example.mediaplayerapp


import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MediaPlayerNotificationManagerTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager
    private lateinit var notificationManagerWrapper: MediaPlayerNotificationManager
    private val channelId = "media_player_channel"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)
        // Inicializamos o wrapper APÓS configurar o mock do NotificationManager
        notificationManagerWrapper = MediaPlayerNotificationManager(context, channelId)
    }




    @Test
    fun `showNotification builds and notifies notification`() {
        // Given
        val status = "Playing music"

        // When
        notificationManagerWrapper.showNotification(status)

        // Then
        val notifications = shadowNotificationManager.allNotifications
        assertEquals(1, notifications.size)


        val notification = notifications[0]
        assertEquals("Media Player", notification.extras.getString(NotificationCompat.EXTRA_TITLE))
        assertEquals(status, notification.extras.getString(NotificationCompat.EXTRA_TEXT))


    }

    @Test
    fun `cancelNotification cancels the notification`() {
        // Given
        // Exibe uma notificação primeiro para ter algo para cancelar
        notificationManagerWrapper.showNotification("Some status")
        assertEquals(1, shadowNotificationManager.allNotifications.size)

        // When
        notificationManagerWrapper.cancelNotification()

        // Then
        assertEquals(0, shadowNotificationManager.allNotifications.size)
    }
}