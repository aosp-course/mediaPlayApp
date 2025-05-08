#include <jni.h>
#include <string>
#include <vector>
#include <cmath>
#include <android/log.h>
#include "ThreeBandEQ.h" // Your EQ header

// Global EQ object or manage its lifecycle appropriately
static ThreeBandEQ eq;
static bool eqInitialized = false;

// Define cutoff frequencies (example values)
const double LOW_MID_CUTOFF_HZ = 500.0;   // e.g., bass/mid crossover
const double MID_HIGH_CUTOFF_HZ = 5000.0; // e.g., mid/treble crossover


#define LOG_TAG "AudioEffectsManager"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Método JNI para aplicar BassBoost (agora utilizando ByteArray)
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_mediaplayerapp_AudioEqualizer_applyBassMidTreble(JNIEnv *env, jobject thiz,
                                                                    jbyteArray audioData,
                                                                    jfloat bassStrength,
                                                                    jfloat midStrength,
                                                                    jfloat trebleStrength) {
    jsize length = env->GetArrayLength(audioData);
    jbyte *audioDataArray = env->GetByteArrayElements(audioData, nullptr);

    std::vector<jbyte> input(audioDataArray, audioDataArray + length);

    eq.setup(static_cast<double>(44100), LOW_MID_CUTOFF_HZ, MID_HIGH_CUTOFF_HZ);
    eqInitialized = true;


    jbyte* audioBytes = env->GetByteArrayElements(audioData, nullptr);
    jsize numBytes = env->GetArrayLength(audioData);

    // Assuming 16-bit PCM, so 2 bytes per sample
    if (numBytes % 2 != 0) {
        env->ReleaseByteArrayElements(audioData, audioBytes, JNI_ABORT); // Release without copy back
        // Or throw an IllegalArgumentException
        jclass exClass = env->FindClass("java/lang/IllegalArgumentException");
        env->ThrowNew(exClass, "Audio data length must be even for 16-bit PCM.");
        return nullptr;
    }

    jsize numSamples = numBytes / 2;
    std::vector<int16_t> samples(numSamples);

    // Convert jbyte* (signed bytes) to int16_t samples (assuming little-endian)
    for (jsize i = 0; i < numSamples; ++i) {
        samples[i] = static_cast<int16_t>( (audioBytes[2 * i + 1] << 8) | (audioBytes[2 * i] & 0xFF) );
    }

    // Process the audio
    // ESSES PARAMETROS PRECISAM SER CONVERTIDOS A PARTIR DO QUE VEIO DO METODO, OS VALORES HARDCODED SÃO SÓ EXEMPLOS
    eq.processBlock(samples, -6, -6, 12);

    // Convert processed int16_t samples back to jbyte*
    for (jsize i = 0; i < numSamples; ++i) {
        audioBytes[2 * i] = static_cast<jbyte>(samples[i] & 0xFF);          // Low byte
        audioBytes[2 * i + 1] = static_cast<jbyte>((samples[i] >> 8) & 0xFF); // High byte
    }

    // Release the byte array elements. JNI_COMMIT ensures changes are copied back if needed.
    // If you want to return a *new* array instead of modifying in-place:
    env->ReleaseByteArrayElements(audioData, audioBytes, 0); // 0 means copy back and free buffer

    // To return a NEW array:
    // jbyteArray processedAudioArray = env->NewByteArray(numBytes);
    // env->SetByteArrayRegion(processedAudioArray, 0, numBytes, audioBytes_after_processing);
    // env->ReleaseByteArrayElements(audioData, audioBytes, JNI_ABORT); // Release original without copy
    // return processedAudioArray;

    return audioData; // Returning the modified input array
}

