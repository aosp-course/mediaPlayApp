package com.example.mediaplayerapp.ui.ui.main

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
    private var isPaused = true
    private var isFavorited = false

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
        val playPauseButton: ImageView = view.findViewById(R.id.playPause_button)
        val stopButton: ImageView = view.findViewById(R.id.stop_button)
        val skipNextButton: ImageView = view.findViewById(R.id.skip_next_button)
        val skipBackButton: ImageView = view.findViewById(R.id.skip_back_button)

        playPauseButton.setOnClickListener {
            var drawableId: Int
            if (viewModel.isPaused) {
                viewModel.playMusic(requireContext())
                drawableId = R.drawable.play_circle
            } else {
                viewModel.pauseMusic(requireContext())
                drawableId = R.drawable.pause_circle
            }
            playPauseButton.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    drawableId
                )
            )
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
            if (isFavorited) {
                starButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                starButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gold))
            }
            isFavorited = !isFavorited
        }
    }

}
