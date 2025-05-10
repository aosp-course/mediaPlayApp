package com.example.mediaplayerapp

class AudioEqualizer {
    companion object {
        // Carrega a biblioteca nativa
        init {
            System.loadLibrary("audioeffects-lib")
        }
    }

    /**
     * Aplica efeitos de equalização de três bandas no áudio.
     *
     * @param audioData As amostras de áudio PCM a serem processadas.
     * @param bassStrength A intensidade do efeito de graves em dB.
     * @param midrangeStrength A intensidade do efeito de médios em dB.
     * @param trebleStrength A intensidade do efeito de agudos em dB.
     * @param sampleRate A taxa de amostragem do áudio em Hz.  // <<< NOVO PARÂMETRO
     * @return Retorna o áudio processado como um array de bytes, ou o original em caso de erro.
     */
    external fun applyBassMidTreble(
        audioData: ByteArray,
        bassStrength: Float,
        midrangeStrength: Float,
        trebleStrength: Float,
        sampleRate: Int // <<< NOVO PARÂMETRO
    ): ByteArray // A função JNI deve retornar jbyteArray
}