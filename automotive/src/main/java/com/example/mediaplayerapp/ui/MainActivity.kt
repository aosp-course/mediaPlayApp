package com.example.mediaplayerapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mediaplayerapp.R
import androidx.activity.viewModels
import com.example.mediaplayerapp.viewmodel.MediaPlayerViewModel

class MainActivity : AppCompatActivity() {

    val mViewModel by viewModels<MediaPlayerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}