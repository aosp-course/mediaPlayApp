package com.example.mediaplayerapp.ui.equalizer

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import com.example.mediaplayerapp.R
import androidx.navigation.fragment.findNavController
import com.example.mediaplayerapp.viewmodel.EqualizerViewModel


/**
 * A simple [Fragment] subclass.
 * Use the [EqualizerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EqualizerFragment : Fragment() {

    private val mViewModel by activityViewModels<EqualizerViewModel>()
    private val TAG = "EqualizerFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val seekListener: SeekBar.OnSeekBarChangeListener = object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Log.i(TAG, "progress change progress: $progress")
                when (seekBar.id) {
                    R.id.seekBar1 -> {
                        mViewModel.setBass(progress)
                    }

                    R.id.seekBar2 -> {
                        mViewModel.setMid(progress)
                    }

                    R.id.seekBar3 -> {
                        mViewModel.setTreble(progress)
                    }

                    else -> {
                        Log.i(TAG, "unknown bar percent: $progress")
                    }
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                //Do nothing
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                //Do nothing
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_equalizer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val backButton: ImageView = view.findViewById(R.id.backButton)

        backButton.setOnClickListener{
            findNavController().navigate(R.id.action_equalizerFragment_to_mainFragment)
        }

    }

}