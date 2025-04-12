package com.example.mediaplayerapp.ui.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mediaplayerapp.R

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    lateinit var playPauseButton: ImageView

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra("status")
            status?.let {
                updateUIOnMediaPlayerStatusChanged(it)
            }
        }
    }

    private fun updateUIOnMediaPlayerStatusChanged(mediaPlayerStatus: String) {
        var drawableId: Int
        if(mediaPlayerStatus == "com.example.ACTION_PLAY") {
            drawableId = R.drawable.pause_circle
        } else {
            drawableId = R.drawable.play_circle
        }
        playPauseButton.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                drawableId
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        )[MainViewModel::class.java]
        viewModel.loadMusicFiles()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playPauseButton = view.findViewById(R.id.playPause_button)
        val stopButton: ImageView = view.findViewById(R.id.stop_button)
        val skipNextButton: ImageView = view.findViewById(R.id.skip_next_button)
        val skipBackButton: ImageView = view.findViewById(R.id.skip_back_button)

        playPauseButton.setOnClickListener {
            if (viewModel.isPaused) {
                viewModel.playMusic(requireContext())
            } else {
                viewModel.pauseMusic(requireContext())
            }
        }

        stopButton.setOnClickListener {
            viewModel.stopMusic(requireContext())
        }

        skipNextButton.setOnClickListener {
            viewModel.skipMusic(requireContext(), true)
        }

        skipBackButton.setOnClickListener {
            viewModel.skipMusic(requireContext(), false)
        }

        val starButton: ImageView = view.findViewById(R.id.star_button)

        starButton.setOnClickListener {
            if (viewModel.isFavorited) {
                starButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                starButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gold))
            }
            viewModel.setFavoriteMusic()
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("com.example.MEDIA_PLAYER_STATUS")
        requireContext().registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(receiver)
    }
}
