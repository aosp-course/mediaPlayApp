package com.example.mediaplayerapp.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mediaplayerapp.MediaPlayerService
import com.example.mediaplayerapp.R
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * @class MainViewModel
 * @brief ViewModel responsável por gerenciar a lógica de reprodução de música.
 *
 * O MainViewModel fornece funcionalidades para controlar a reprodução de música,
 * como tocar, pausar, parar, e pular faixas, além de manipular favoritos e
 * recuperar metadados da música.
 */
class MediaPlayerViewModel : ViewModel() {

    /// Tag utilizada para logs.
    val TAG = "MediaPlayerViewModel"

    /// Indica se a música está pausada.
    private val _isPaused = MutableLiveData<Boolean>(true)
    val isPaused: LiveData<Boolean> = _isPaused

    /// Indica se a música atual está marcada como favorita.
    var isFavorited = false

    lateinit var retriever: MediaMetadataRetriever

    /// Lista de IDs dos recursos de áudio.
    private val musicList = listOf(
        R.raw.circusoffreaks,
        R.raw.gothamlicious,
        R.raw.igotastickarrbryanteoh,
        R.raw.newherointown,
        R.raw.theicegiants
    )

    /// Índice da música atualmente em reprodução.
    private var currentMusicIndex = 0

    init {
        retriever = MediaMetadataRetriever()
    }

    /**
     * @brief Obtém o nome da música atual.
     * @param context Contexto utilizado para acessar recursos.
     * @return Nome e artista da música atual.
     */
    fun getMusicName(context: Context): String {
        val afd = context.resources.openRawResourceFd(musicList[currentMusicIndex])
        retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown music"
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown artist"
        return "$artist - $title"
    }


    /**
     * @brief Obtém a capa do álbum da música atual.
     * @param context Contexto utilizado para acessar recursos.
     * @return Bitmap da capa do álbum ou null se não existir.
     */
    fun getAlbumCover(context: Context): Bitmap? {
        val retriever = MediaMetadataRetriever()
        val afd = context.resources.openRawResourceFd(musicList[currentMusicIndex])
        retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        val artBytes = retriever.embeddedPicture
        retriever.release()
        afd.close()

        return if (artBytes != null) {
            BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
        } else {
            null
        }
    }

    /**
     * @brief Inicia a reprodução da música especificada por resourceId.
     * @param context Contexto utilizado para iniciar o serviço.
     * @param resourceId ID do recurso da música a ser reproduzida.
     */
    private fun playMusic(context: Context, resourceId: Int) {
        var serviceIntent = Intent(context, MediaPlayerService::class.java).apply {
            action = MediaPlayerService.ACTION_PLAY
            putExtra("resourceId", resourceId)
        }
        context.startService(serviceIntent)
        _isPaused.value = false
    }

    /**
     * @brief Inicia a reprodução da música atual.
     * @param context Contexto utilizado para iniciar o serviço.
     */
    fun playMusic(context: Context) {
        Log.i(TAG, "playMusic")
        playMusic(context, musicList[currentMusicIndex])
        _isPaused.value = false
    }

    /**
     * @brief Pausa a reprodução da música atual.
     * @param context Contexto utilizado para iniciar o serviço.
     */
    fun pauseMusic(context: Context) {
        Log.i(TAG, "pauseMusic")
        var serviceIntent = Intent(context, MediaPlayerService::class.java).apply {
            action = MediaPlayerService.ACTION_PAUSE
        }
        context.startService(serviceIntent)
        _isPaused.value = true
    }

    /**
     * @brief Para a reprodução da música atual.
     * @param context Contexto utilizado para iniciar o serviço.
     */
    fun stopMusic(context: Context) {
        Log.i(TAG, "stopMusic")
        val serviceIntent = Intent(context, MediaPlayerService::class.java).apply {
            action = MediaPlayerService.ACTION_STOP
        }
        context.startService(serviceIntent)
        _isPaused.value = true
    }

    /**
     * @brief Pula para a próxima ou anterior música na lista.
     * @param context Contexto utilizado para iniciar o serviço.
     * @param isNext Indica se deve pular para a próxima música (true) ou anterior (false).
     */
    fun skipMusic(context: Context, isNext: Boolean) {
        Log.i(TAG, "skipMusic - isNext: " + isNext)
        if (isNext) {
            currentMusicIndex = (currentMusicIndex + 1) % musicList.size
        } else {
            currentMusicIndex = (currentMusicIndex - 1 + musicList.size) % musicList.size
        }
        playMusic(context, musicList[currentMusicIndex])
    }

    /**
     * @brief Marca ou desmarca a música atual como favorita.
     */
    fun setFavoriteMusic() {
        // Seta música atual como favorita
        isFavorited = !isFavorited
    }
}
