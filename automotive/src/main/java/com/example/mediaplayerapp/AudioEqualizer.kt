package com.example.mediaplayerapp

class AudioEqualizer {
    companion object {
        // Carrega a biblioteca nativa
        init {
            System.loadLibrary("audioeffects-lib")
        }
    }

    /**
     * Aplica o efeito de reforço de graves no áudio.
     *
     * @param audioData As amostras de áudio a serem processadas.
     * @param bassStrength A intensidade do efeito de reforço de graves.
     * @return Retorna o áudio processado como um array de amostras.
     */
    external fun applyBassBoostEffect(audioData: ByteArray, bassStrength: Float): ByteArray

    /**
     * Aplica o efeito de reforço de médios no áudio.
     *
     * @param audioData As amostras de áudio a serem processadas.
     * @param midrangeStrength A intensidade do efeito de reforço de médios.
     * @return Retorna o áudio processado como um array de amostras.
     */
    external fun applyMidrangeBoostEffect(audioData: ByteArray, midrangeStrength: Float): ByteArray
}