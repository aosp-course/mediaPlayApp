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

    fun loadMusicFiles() {

    }

    private fun playMusic(context: Context, path: String) {
        var serviceIntent = Intent(context, MediaPlayerService::class.java).apply {
            action = "com.example.ACTION_PLAY"
            putExtra("path", path)
        }
        context.startService(serviceIntent)
        isPaused = false
    }

    fun playMusic(context: Context) {
        Log.i(TAG, "playMusic")
        //adicionar logica pra criar lista de musicas e tocar atual
        playMusic(context, "/seu/caminho/para/musica")
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
            path = "/seu/caminho/para/proxima/musica"
        } else {
            //adicionar logica pra criar lista de musicas e tocar a anterior da lista
            path = "/seu/caminho/para/anterior/musica"
        }
        playMusic(context, path)
    }

    fun setFavoriteMusic() {
        //seta musica atual como favorita
        isFavorited = !isFavorited
    }

}
