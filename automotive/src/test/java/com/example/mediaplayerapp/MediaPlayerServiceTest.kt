package com.example.mediaplayerapp

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.support.v4.media.session.MediaSessionCompat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // Define a versão do SDK a ser usada pelo Robolectric
class MediaPlayerServiceTest {

    // Mocks para as dependências do serviço
    private lateinit var mockAudioManager: AudioManager
    private lateinit var mockMediaSession: MediaSessionCompat
    private lateinit var mockAudioEngine: AudioPlaybackEngine
    private lateinit var mockNotificationManager: MediaPlayerNotificationManager
    
    // O serviço que será testado
    private lateinit var serviceUnderTest: TestableMediaPlayerService
    private lateinit var shadowService: ShadowService

    @Before
    fun setup() {
        // Ativa registro de logs para depuração
        ShadowLog.stream = System.out
        
        // Cria os mocks
        mockAudioManager = mock()
        mockMediaSession = mock()
        mockAudioEngine = mock()
        mockNotificationManager = mock()
        
        // Define o comportamento padrão para o AudioManager
        whenever(mockAudioManager.requestAudioFocus(any(), any(), any()))
            .thenReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        
        // Cria o serviço testável e configura os mocks
        serviceUnderTest = TestableMediaPlayerService()
        serviceUnderTest.setupTestDependencies(
            mockAudioManager,
            mockMediaSession,
            mockAudioEngine,
            mockNotificationManager
        )
        
        // Obtém o shadow do serviço para verificar comportamentos
        shadowService = shadowOf(serviceUnderTest)
    }

    @Test
    fun `quando recebe ACTION_PLAY com resourceId válido, deve iniciar reprodução`() {
        // Arrange
        val resourceId = 12345
        val playIntent = Intent(MediaPlayerService.ACTION_PLAY).apply {
            putExtra("resourceId", resourceId)
        }
        
        // Act
        serviceUnderTest.onStartCommand(playIntent, 0, 1)
        
        // Assert
        verify(mockAudioManager).requestAudioFocus(
            any(),
            eq(AudioManager.STREAM_MUSIC),
            eq(AudioManager.AUDIOFOCUS_GAIN)
        )
        verify(mockAudioEngine).play(resourceId)
        verify(mockNotificationManager).showNotification("Música tocando")

    }
    
    @Test
    fun `quando recebe ACTION_PLAY sem resourceId, não deve iniciar reprodução`() {
        // Arrange
        val playIntent = Intent(MediaPlayerService.ACTION_PLAY)
        // Sem resourceId definido
        
        // Act
        serviceUnderTest.onStartCommand(playIntent, 0, 1)
        
        // Assert
        verify(mockAudioManager, never()).requestAudioFocus(any(), any(), any())
        verify(mockAudioEngine, never()).play(any())
        verify(mockNotificationManager, never()).showNotification(any())
    }
    
    @Test
    fun `quando recebe ACTION_PAUSE, deve pausar a reprodução`() {
        // Arrange
        val pauseIntent = Intent(MediaPlayerService.ACTION_PAUSE)
        
        // Act
        serviceUnderTest.onStartCommand(pauseIntent, 0, 1)
        
        // Assert
        verify(mockAudioEngine).pause()
        verify(mockNotificationManager).showNotification("Música pausada")
    }
    
    @Test
    fun `quando recebe ACTION_STOP, deve parar a reprodução e o serviço`() {
        // Arrange
        val stopIntent = Intent(MediaPlayerService.ACTION_STOP)
        
        // Act
        serviceUnderTest.onStartCommand(stopIntent, 0, 1)
        
        // Assert
        verify(mockAudioEngine).stop()
        verify(mockNotificationManager).showNotification("Música parada")
        
        // Verifica se o serviço interrompeu a si mesmo
        assert(serviceUnderTest.isServiceStoppedExposed)
    }
    
    @Test
    fun `quando recebe ACTION_UPDATE_EQ, deve atualizar configurações do equalizador`() {
        // Arrange
        val bassDb = 3.0f
        val midDb = -2.0f
        val trebleDb = 4.0f
        val updateEqIntent = Intent(MediaPlayerService.ACTION_UPDATE_EQ).apply {
            putExtra(MediaPlayerService.EXTRA_BASS_DB, bassDb)
            putExtra(MediaPlayerService.EXTRA_MID_DB, midDb)
            putExtra(MediaPlayerService.EXTRA_TREBLE_DB, trebleDb)
        }
        
        // Act
        serviceUnderTest.onStartCommand(updateEqIntent, 0, 1)
        
        // Assert
        verify(mockAudioEngine).setBass(bassDb)
        verify(mockAudioEngine).setMid(midDb)
        verify(mockAudioEngine).setTreble(trebleDb)
    }
    
    @Test
    fun `quando falha ao obter foco de áudio, não deve iniciar reprodução`() {
        // Arrange
        whenever(mockAudioManager.requestAudioFocus(any(), any(), any()))
            .thenReturn(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
            
        val resourceId = 12345
        val playIntent = Intent(MediaPlayerService.ACTION_PLAY).apply {
            putExtra("resourceId", resourceId)
        }
        
        // Act
        serviceUnderTest.onStartCommand(playIntent, 0, 1)
        
        // Assert
        verify(mockAudioEngine, never()).play(any())
        verify(mockNotificationManager, never()).showNotification(any())
    }
    
    @Test
    fun `quando onDestroy é chamado, deve liberar recursos`() {
        // Act
        serviceUnderTest.onDestroy()
        
        // Assert
        verify(mockAudioEngine).stop()
        verify(mockAudioManager).abandonAudioFocus(any())
        verify(mockMediaSession).release()
    }
    
    @Test
    fun `quando perde foco temporário, deve pausar a reprodução`() {
        // Arrange
        val audioFocusListener = serviceUnderTest.getAudioFocusListener()
        
        // Act
        audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        
        // Assert
        verify(mockAudioEngine).pause()
    }
    
    @Test
    fun `quando perde foco permanente, deve parar a reprodução`() {
        // Arrange
        val audioFocusListener = serviceUnderTest.getAudioFocusListener()
        
        // Act
        audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        
        // Assert
        verify(mockAudioEngine).stop()
    }
    
    @Test
    fun `quando recupera foco, deve retomar a reprodução`() {
        // Arrange
        val audioFocusListener = serviceUnderTest.getAudioFocusListener()
        
        // Act
        audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        
        // Assert
        verify(mockAudioEngine).resume()
    }
}

/**
 * Versão testável do MediaPlayerService que permite injeção de mocks
 * e expõe métodos/propriedades protegidos para testes.
 */
class TestableMediaPlayerService : MediaPlayerService() {
    
    val isServiceStoppedExposed: Boolean
        get() = isServiceStopped
    
    // Expõe o listener de foco de áudio para testes
    fun getAudioFocusListener(): AudioManager.OnAudioFocusChangeListener = mAudioFocusListener
    
    // Configura as dependências mockadas para testes
    fun setupTestDependencies(
        audioManagerMock: AudioManager,
        mediaSessionMock: MediaSessionCompat,
        audioEngineMock: AudioPlaybackEngine,
        notificationManagerMock: MediaPlayerNotificationManager
    ) {
        audioManager = audioManagerMock
        mediaSession = mediaSessionMock
        audioEngine = audioEngineMock
        notificationManager = notificationManagerMock
        isServiceStopped = false
    }
    
    // Substitui o método para evitar acesso real ao sistema
    override fun getSystemService(name: String): Any? {
        return when (name) {
            Context.AUDIO_SERVICE -> audioManager
            else -> null
        }
    }
    
    // Substitui para evitar inicialização real
    override fun initServiceIfRequired() {
        // Não faz nada, pois as dependências já estão configuradas em setupTestDependencies
    }

    override fun sendBroadcast(intent: Intent?) {
        // Não faz nada
    }
}
