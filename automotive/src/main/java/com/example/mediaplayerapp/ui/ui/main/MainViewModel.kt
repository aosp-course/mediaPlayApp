package com.example.mediaplayerapp.ui.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.provider.MediaStore.Audio.Media
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.mediaplayerapp.MediaPlayerService
import com.example.mediaplayerapp.R


class MainViewModel : ViewModel() {
    val TAG = "MediaPlayerViewModel"
    var isPaused = true
    var isFavorited = false

    // Lista de IDs dos recursos de áudio
    private val musicList = listOf(
        R.raw.circusoffreaks,
        R.raw.gothamlicious,
        R.raw.igotastickarrbryanteoh,
        R.raw.newherointown,
        R.raw.theicegiants
    )

    private var currentMusicIndex = 0

    fun getMusicName(context: Context): String {
        val retriever = MediaMetadataRetriever()
        val afd = context.resources.openRawResourceFd(musicList[currentMusicIndex])
        retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        return "$artist - $title"
    }

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

    private fun playMusic(context: Context, resourceId: Int) {
        var serviceIntent = Intent(context, MediaPlayerService::class.java).apply {
            action = "com.example.ACTION_PLAY"
            putExtra("resourceId", resourceId)
        }
        context.startService(serviceIntent)
        isPaused = false
    }

    fun playMusic(context: Context) {
        Log.i(TAG, "playMusic")
        playMusic(context, musicList[currentMusicIndex])
        isPaused = false
    }

    fun pauseMusic(context: Context) {
        Log.i(TAG, "pauseMusic")
        var serviceIntent = Intent(context, MediaPlayerService::class.java).apply {
            action = "com.example.ACTION_PAUSE"
        }
        context.startService(serviceIntent)
        isPaused = true
    }

    fun stopMusic(context: Context) {
        Log.i(TAG, "stopMusic")
        val serviceIntent = Intent(context, MediaPlayerService::class.java).apply {
            action = "com.example.ACTION_STOP"
        }
        context.startService(serviceIntent)
        isPaused = true
    }

    fun skipMusic(context: Context, isNext: Boolean) {
        Log.i(TAG, "skipMusic - isNext: " + isNext)
        var path: String
        if (isNext) {
            currentMusicIndex = (currentMusicIndex + 1) % musicList.size
        } else {
            currentMusicIndex = (currentMusicIndex - 1 + musicList.size) % musicList.size
        }
        playMusic(context, musicList[currentMusicIndex])
    }

    fun setFavoriteMusic() {
        //seta musica atual como favorita
        isFavorited = !isFavorited
    }

}
