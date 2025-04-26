package com.example.mediaplayerapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mediaplayerapp.R
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.example.mediaplayerapp.ui.ui.main.MainViewModel

class MainActivity : AppCompatActivity() {

    val mViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}