#include <jni.h>
#include <string>
#include <vector>
#include <cmath>
#include <android/log.h>
#include "filters.h"

#define LOG_TAG "AudioEffectsManager"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

float history[FILTER_TAPS] = {0};

// Função para reforço de Bass usando filtro passa-baixa (agora com ByteArray)
std::vector<jbyte> lowPassFilter(const std::vector<jbyte> &input, float strength) {
    std::vector<jbyte> output(input.size());
    float alpha = strength; // Controle de força do filtro (0 < alpha < 1)

    float previousFilteredSample = static_cast<float>(input[0]);
    for (size_t i = 0; i < input.size(); ++i) {
        //float filteredSample = alpha * static_cast<float>(input[i]) + (1.0f - alpha) * previousFilteredSample;
        //output[i] = static_cast<jbyte>(filteredSample);
        //previousFilteredSample = filteredSample;
        output[i] = input[i];
    }

    return output;
}

// Função para reforço de Midrange usando filtro passa-banda (agora com ByteArray)
std::vector<jbyte> bandPassFilter(const std::vector<jbyte> &input, float strength, int sampleRate) {
    std::vector<jbyte> output(input.size());
/*
    for (int n = 0; n < input.size(); n++) {
        // Shift history buffer
        for (int i = FILTER_TAPS - 1; i > 0; i--) {
            history[i] = history[i - 1];
        }
        history[0] = (float)input[n];  // Convert to float for filtering

        // Apply FIR filter
        float acc = 0.0f;
        for (int i = 0; i < FILTER_TAPS; i++) {
            acc += filter_coeffs[i] * history[i];
        }

        // Clamp and store in output array
        int32_t y = lroundf(acc);
        if (y > 127) y = 127;
        if (y < -128) y = -128;
        output[n] = (jbyte)y;
    }
*/

    for (auto i = 0u; i < input.size(); ++i) {
        double acc = 0.0;
        acc = input[i] * filter_coeffs[0];
        for (auto j = 1u; j < FILTER_TAPS; ++j) {
            acc += input[i + j] * filter_coeffs[j];
        }
        if (acc > 127) acc = 127;
        if (acc < -128) acc = -128;
        output[i] = (jbyte) acc;
    }

    /*
        for (auto i = 0u; i < input.size(); ++i) {
        for (int b = FILTER_TAPS - 1; b > 0; b--) {
            history[b] = history[b - 1];
        }
        history[0] = (double)input[i];  // Convert to float for filtering

        // Apply FIR filter
        double acc = 0.0f;
        for (int j = 0; j < FILTER_TAPS; j++) {
            acc += filter_coeffs[j] * history[j];
        }

        if (acc > 127) acc = 127;
        if (acc < -128) acc = -128;
        output[i] = (jbyte)acc;
    }
         */
    return output;
}

// Método JNI para aplicar BassBoost (agora utilizando ByteArray)
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_mediaplayerapp_AudioEqualizer_applyBassBoostEffect(JNIEnv *env, jobject thiz,
                                                                    jbyteArray audioData,
                                                                    jfloat bassStrength) {
    jsize length = env->GetArrayLength(audioData);
    jbyte *audioDataArray = env->GetByteArrayElements(audioData, nullptr);

    std::vector<jbyte> input(audioDataArray, audioDataArray + length);

    // Aplicar efeito BassBoost
    auto output = lowPassFilter(input, bassStrength);

    jbyteArray result = env->NewByteArray(length);
    env->SetByteArrayRegion(result, 0, length, output.data());

    env->ReleaseByteArrayElements(audioData, audioDataArray, 0);

    return result;
}

// Método JNI para aplicar MidrangeBoost (agora utilizando ByteArray)
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_mediaplayerapp_AudioEqualizer_applyMidrangeBoostEffect(JNIEnv *env, jobject thiz,
                                                                        jbyteArray audioData,
                                                                        jfloat midrangeStrength) {
    jsize length = env->GetArrayLength(audioData);
    jbyte *audioDataArray = env->GetByteArrayElements(audioData, nullptr);

    std::vector<jbyte> input(audioDataArray, audioDataArray + length);
    int sampleRate = 44100; // Taxa de amostragem comum

    // Aplicar efeito MidrangeBoost
    auto output = bandPassFilter(input, midrangeStrength, sampleRate);

    jbyteArray result = env->NewByteArray(length);
    env->SetByteArrayRegion(result, 0, length, output.data());

    env->ReleaseByteArrayElements(audioData, audioDataArray, 0);

    return result;
}

