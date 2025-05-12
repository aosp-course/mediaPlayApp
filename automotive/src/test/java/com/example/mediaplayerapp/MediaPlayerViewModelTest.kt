package com.example.mediaplayerapp


import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.test.core.app.ApplicationProvider
import com.example.mediaplayerapp.viewmodel.MediaPlayerViewModel
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.FileDescriptor

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MediaPlayerViewModelTest {

    private lateinit var viewModel: MediaPlayerViewModel
    private lateinit var context: Context
    private lateinit var resources: Resources
    private lateinit var mediaMetadataRetriever: MediaMetadataRetriever
    private lateinit var afd: AssetFileDescriptor
    private lateinit var fileDescriptor: FileDescriptor
    private lateinit var mockBitmap: Bitmap // Mock para o Bitmap retornado
    // Mock estático para BitmapFactory
    private lateinit var mockedBitmapFactory: MockedStatic<BitmapFactory>

    @Before
    fun setup() {
        context = mock()
        resources = mock()
        afd = mock()
        mediaMetadataRetriever = mock()
        fileDescriptor = mock()
        mockBitmap = mock()

        viewModel = MediaPlayerViewModel()

        `when`(context.resources).thenReturn(resources)
        `when`(resources.openRawResourceFd(anyInt())).thenReturn(afd)
        `when`(afd.fileDescriptor).thenReturn(fileDescriptor)
        `when`(afd.startOffset).thenReturn(0L)
        `when`(afd.length).thenReturn(100L)

        // Injeta o mock do MediaMetadataRetriever usando Reflection
        viewModel.retriever = mediaMetadataRetriever

        // Configura o mock estático para BitmapFactory
        mockedBitmapFactory = mockStatic(BitmapFactory::class.java)
        // Define o comportamento do decodeByteArray quando bytes são fornecidos
        mockedBitmapFactory.`when`<Bitmap> {
            BitmapFactory.decodeByteArray(any(), anyInt(), anyInt())
        }.thenReturn(mockBitmap)
    }

    @After
    fun tearDown() {
        mockedBitmapFactory.close()
    }

    @Test
    fun `getMusicName should return correct music name`() {
        // Arrange
        val mockTitle = "Test Music Title"
        val mockArtist = "Test Artist"

        `when`(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)).thenReturn(mockTitle)
        `when`(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)).thenReturn(mockArtist)

        // Act
        val musicName = viewModel.getMusicName(context)

        // Assert
        assertEquals("$mockArtist - $mockTitle", musicName)

        // Verify
        verify(mediaMetadataRetriever).setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        verify(mediaMetadataRetriever).extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        verify(mediaMetadataRetriever).extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        verify(context.resources).openRawResourceFd(anyInt())
    }

    @Test
    fun `getAlbumCover should return Bitmap when embeddedPicture returns bytes`() {
        // Arrange
        val mockArtBytes = byteArrayOf(1, 2, 3, 4) // Exemplo de bytes

        // Configura o mock do retriever para retornar bytes
        `when`(mediaMetadataRetriever.embeddedPicture).thenReturn(mockArtBytes)

        // Act
        val resultBitmap = viewModel.getAlbumCover(context)

        // Assert
        assertEquals(mockBitmap, resultBitmap) // Verifica se retornou o Bitmap mockado

        // Verify
        verify(context.resources).openRawResourceFd(anyInt())
        verify(mediaMetadataRetriever).setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        verify(mediaMetadataRetriever).embeddedPicture
        verify(mediaMetadataRetriever).release() // Verifica se release foi chamado
        verify(afd).close() // Verifica se close foi chamado no afd

        // Verifica se BitmapFactory.decodeByteArray foi chamado com os bytes corretos
        mockedBitmapFactory.verify {
            BitmapFactory.decodeByteArray(mockArtBytes, 0, mockArtBytes.size)
        }
    }


    @Test
    fun `playMusic starts MediaPlayerService with correct action and resourceId`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowApplication = shadowOf(context.applicationContext as android.app.Application)

        // When
        viewModel.playMusic(context) // Call the public method

        // Then
        val startedIntent = shadowApplication.nextStartedService
        assertEquals("com.example.ACTION_PLAY", startedIntent.action)
        //We need to get the resourceId from the musicList
        val resourceId = viewModel.javaClass.getDeclaredField("musicList").apply { isAccessible = true }.get(viewModel) as List<Int>
        assertEquals(resourceId[0], startedIntent.getIntExtra("resourceId", -1))
    }

    @Test
    fun `playMusic starts MediaPlayerService with correct action`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowApplication = shadowOf(context.applicationContext as android.app.Application)

        // When
        viewModel.playMusic(context)

        // Then
        val startedIntent = shadowApplication.nextStartedService
        assertEquals("com.example.ACTION_PLAY", startedIntent.action)
    }

    @Test
    fun `pauseMusic starts MediaPlayerService with correct action`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowApplication = shadowOf(context.applicationContext as android.app.Application)

        // When
        viewModel.pauseMusic(context)

        // Then
        val startedIntent = shadowApplication.nextStartedService
        assertEquals("com.example.ACTION_PAUSE", startedIntent.action)
    }

    @Test
    fun `stopMusic starts MediaPlayerService with correct action`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowApplication = shadowOf(context.applicationContext as android.app.Application)

        // When
        viewModel.stopMusic(context)

        // Then
        val startedIntent = shadowApplication.nextStartedService
        assertEquals("com.example.ACTION_STOP", startedIntent.action)
    }

    @Test
    fun `skipMusic to next track`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowApplication = shadowOf(context.applicationContext as android.app.Application)
        val initialIndex = viewModel.javaClass.getDeclaredField("currentMusicIndex").apply { isAccessible = true }.get(viewModel) as Int
        val musicList = viewModel.javaClass.getDeclaredField("musicList").apply { isAccessible = true }.get(viewModel) as List<Int>

        // When
        viewModel.skipMusic(context, true)

        // Then
        val startedIntent = shadowApplication.nextStartedService
        assertEquals("com.example.ACTION_PLAY", startedIntent.action)
        val newIndex = viewModel.javaClass.getDeclaredField("currentMusicIndex").apply { isAccessible = true }.get(viewModel) as Int
        assertEquals((initialIndex + 1) % musicList.size, newIndex)
        assertEquals(musicList[newIndex], startedIntent.getIntExtra("resourceId", -1))
    }

    @Test
    fun `skipMusic to previous track`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowApplication = shadowOf(context.applicationContext as android.app.Application)
        val initialIndex = viewModel.javaClass.getDeclaredField("currentMusicIndex").apply { isAccessible = true }.get(viewModel) as Int
        val musicList = viewModel.javaClass.getDeclaredField("musicList").apply { isAccessible = true }.get(viewModel) as List<Int>

        // When
        viewModel.skipMusic(context, false)

        // Then
        val startedIntent = shadowApplication.nextStartedService
        assertEquals("com.example.ACTION_PLAY", startedIntent.action)
        val newIndex = viewModel.javaClass.getDeclaredField("currentMusicIndex").apply { isAccessible = true }.get(viewModel) as Int
        assertEquals((initialIndex - 1 + musicList.size) % musicList.size, newIndex)
        assertEquals(musicList[newIndex], startedIntent.getIntExtra("resourceId", -1))
    }

    @Test
    fun `setFavoriteMusic toggles isFavorited from false to true`() {
        // Given
        viewModel.isFavorited = false

        // When
        viewModel.setFavoriteMusic()

        // Then
        assertTrue(viewModel.isFavorited)
    }

    @Test
    fun `setFavoriteMusic toggles isFavorited from true to false`() {
        // Given
        viewModel.isFavorited = true

        // When
        viewModel.setFavoriteMusic()

        // Then
        assertFalse(viewModel.isFavorited)
    }
}