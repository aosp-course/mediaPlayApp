// com.example.mediaplayerapp.viewmodel.EqualizerViewModel.kt
package com.example.mediaplayerapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Log

class EqualizerViewModel : ViewModel() {
    private val TAG = "EqualizerViewModel"

    // Valores dos sliders (ex: 0 a 10, como no XML)
    // Usaremos LiveData para que as mudanças possam ser observadas.
    private val _bass = MutableLiveData<Int>(5) // Valor inicial (ex: meio)
    val bass: LiveData<Int> = _bass

    private val _mid = MutableLiveData<Int>(5)
    val mid: LiveData<Int> = _mid

    private val _treble = MutableLiveData<Int>(5)
    val treble: LiveData<Int> = _treble

    // Função para definir o valor do grave
    fun setBass(value: Int) {
        if (_bass.value != value) {
            _bass.value = value
            //Log.d(TAG, "Bass set to: $value")
        }
    }

    // Função para definir o valor do médio
    fun setMid(value: Int) {
        if (_mid.value != value) {
            _mid.value = value
            //Log.d(TAG, "Mid set to: $value")
        }
    }

    // Função para definir o valor do agudo
    fun setTreble(value: Int) {
        if (_treble.value != value) {
            _treble.value = value
            //Log.d(TAG, "Treble set to: $value")
        }
    }

    /** Os valores do sliders (0-10) são convertidos para valores de ganho em dB
    * que a função C++ espera (ex: -12dB a +12dB).
    * Exemplo de conversão: Se o slider vai de 0 a 10, e o centro (5) é 0dB.
    * E o range total é, por exemplo, 24dB (-12dB a +12dB).
    * Então cada passo do slider é 24dB / 10 = 2.4dB.
    * Valor em dB = (valorSlider - 5) * 2.4
    */

    /**
     * @brief Converte o valor do slider de Grave (Bass) para um ganho em decibéis.
     *
     * A conversão é baseada em um range de slider de 0 a 10, onde o valor 5
     * corresponde a 0dB. Cada passo do slider representa 2.4dB de ganho,
     * cobrindo um range total de -12dB (slider 0) a +12dB (slider 10).
     *
     * @return O ganho calculado para a banda de Grave em decibéis (Float).
     */
    fun getBassDb(): Float {
        // Exemplo: slider 0-10, centro 5 = 0dB, range -12dB a +12dB
        // (valor - ponto_medio) * (range_db_total / range_slider_total)
        val sliderValue = _bass.value ?: 5
        return (sliderValue - 5) * 2.4f // Ajuste 2.4f conforme seu range desejado
    }

    /**
     * @brief Converte o valor do slider de Médio (Mid) para um ganho em decibéis.
     *
     * A conversão é baseada em um range de slider de 0 a 10, onde o valor 5
     * corresponde a 0dB. Cada passo do slider representa 2.4dB de ganho,
     * cobrindo um range total de -12dB (slider 0) a +12dB (slider 10).
     *
     * @return O ganho calculado para a banda de Médio em decibéis (Float).
     */
    fun getMidDb(): Float {
        val sliderValue = _mid.value ?: 5
        return (sliderValue - 5) * 2.4f
    }

    /**
     * @brief Converte o valor do slider de Agudo (Treble) para um ganho em decibéis.
     *
     * A conversão é baseada em um range de slider de 0 a 10, onde o valor 5
     * corresponde a 0dB. Cada passo do slider representa 2.4dB de ganho,
     * cobrindo um range total de -12dB (slider 0) a +12dB (slider 10).
     *
     * @return O ganho calculado para a banda de Agudo em decibéis (Float).
     */
    fun getTrebleDb(): Float {
        val sliderValue = _treble.value ?: 5
        return (sliderValue - 5) * 2.4f
    }
}
