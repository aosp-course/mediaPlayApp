package com.example.mediaplayerapp.ui.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

    fun loadMusicFiles() {
        // aqui não precisa fazer nada pq os arquivos são carregados na lista acima ai
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
        //adicionar logica pra criar lista de musicas e tocar atual
        playMusic(context, musicList[currentMusicIndex])
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
    }

    fun skipMusic(context: Context, isNext: Boolean) {
        Log.i(TAG, "skipMusic - isNext: "+isNext)
        var path: String
        if(isNext) {
            //adicionar logica pra criar lista de musicas e tocar a proxima da lista
            currentMusicIndex = (currentMusicIndex + 1) % musicList.size
        } else {
            //adicionar logica pra criar lista de musicas e tocar a anterior da lista
            currentMusicIndex = (currentMusicIndex - 1 + musicList.size) % musicList.size
        }
        playMusic(context, musicList[currentMusicIndex])
    }

    fun setFavoriteMusic() {
        //seta musica atual como favorita
        isFavorited = !isFavorited
    }

}
