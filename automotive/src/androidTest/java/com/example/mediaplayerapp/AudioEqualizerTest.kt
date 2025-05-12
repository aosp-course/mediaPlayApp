package com.example.mediaplayerapp
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AudioEqualizerTest {
    @Test
    fun testApplyBassMidTreble_realCall() {
        val equalizer = AudioEqualizer()
        val inputAudio = ByteArray(1024) { 0 }  // silent audio data
        val result = equalizer.applyBassMidTreble(inputAudio, 5f, 3f, 7f, 44100)

        // Verificando se o tamanho do output retornado é igual ao esperado:
        assertEquals(inputAudio.size, result.size)
    }
}