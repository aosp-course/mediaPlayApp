// com.example.mediaplayerapp.ui.equalizer.EqualizerFragment.kt
package com.example.mediaplayerapp.ui.equalizer

import android.content.Intent // Adicionar
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
import com.example.mediaplayerapp.MediaPlayerService // Adicionar
import com.example.mediaplayerapp.viewmodel.EqualizerViewModel

class EqualizerFragment : Fragment() {

    private val equalizerViewModel by activityViewModels<EqualizerViewModel>() // Renomeado para clareza
    private val TAG = "EqualizerFragment"

    // Listener para os SeekBars
    private val seekListener: SeekBar.OnSeekBarChangeListener = object :
        SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) { // Apenas processa se a mudança foi feita pelo usuário
                Log.d(TAG, "SeekBar ${seekBar.id} progress changed to: $progress by user")
                when (seekBar.id) {
                    R.id.seekBar1 -> equalizerViewModel.setBass(progress)
                    R.id.seekBar2 -> equalizerViewModel.setMid(progress)
                    R.id.seekBar3 -> equalizerViewModel.setTreble(progress)
                }
                // Após atualizar o ViewModel, envia os novos valores para o serviço
                sendEqSettingsToService()
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            // Não é necessário fazer nada aqui por enquanto
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            // Também não é necessário fazer nada aqui para atualizações em tempo real
            // Se quisesse enviar apenas ao soltar, faria aqui.
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_equalizer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState) // Chamar super

        val backButton: ImageView = view.findViewById(R.id.backButton)
        val seekBarBass: SeekBar = view.findViewById(R.id.seekBar1)
        val seekBarMid: SeekBar = view.findViewById(R.id.seekBar2)
        val seekBarTreble: SeekBar = view.findViewById(R.id.seekBar3)

        // Configurar listeners
        seekBarBass.setOnSeekBarChangeListener(seekListener)
        seekBarMid.setOnSeekBarChangeListener(seekListener)
        seekBarTreble.setOnSeekBarChangeListener(seekListener)

        // Configurar valores iniciais dos SeekBars a partir do ViewModel
        // Usar observe para manter a UI sincronizada se os valores mudarem por outra fonte
        equalizerViewModel.bass.observe(viewLifecycleOwner) { bassValue ->
            if (seekBarBass.progress != bassValue) seekBarBass.progress = bassValue
        }
        equalizerViewModel.mid.observe(viewLifecycleOwner) { midValue ->
            if (seekBarMid.progress != midValue) seekBarMid.progress = midValue
        }
        equalizerViewModel.treble.observe(viewLifecycleOwner) { trebleValue ->
            if (seekBarTreble.progress != trebleValue) seekBarTreble.progress = trebleValue
        }

        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_equalizerFragment_to_mainFragment)
        }
    }

    /**
     * Envia os valores atuais do equalizador para o MediaPlayerService.
     */
    private fun sendEqSettingsToService() {
        val bassDb = equalizerViewModel.getBassDb()
        val midDb = equalizerViewModel.getMidDb()
        val trebleDb = equalizerViewModel.getTrebleDb()

        Log.d(TAG, "Enviando EQ para serviço: Bass=${bassDb}dB, Mid=${midDb}dB, Treble=${trebleDb}dB")

        val serviceIntent = Intent(activity, MediaPlayerService::class.java).apply {
            action = MediaPlayerService.ACTION_UPDATE_EQ // Nova ação
            putExtra(MediaPlayerService.EXTRA_BASS_DB, bassDb)
            putExtra(MediaPlayerService.EXTRA_MID_DB, midDb)
            putExtra(MediaPlayerService.EXTRA_TREBLE_DB, trebleDb)
        }
        activity?.startService(serviceIntent)
    }
}
