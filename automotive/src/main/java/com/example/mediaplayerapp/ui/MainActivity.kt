package com.example.mediaplayerapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.mediaplayerapp.ui.ui.main.MainFragment
import com.example.mediaplayerapp.R
import androidx.activity.viewModels
import com.example.mediaplayerapp.ui.ui.main.MainViewModel

class MainActivity : AppCompatActivity() {

    val mViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }
}