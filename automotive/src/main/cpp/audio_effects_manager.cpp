#include <jni.h>
#include <string>
#include <vector>
#include <cmath>
#include <android/log.h>

#define LOG_TAG "AudioEffectsManager"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Função para reforço de Bass usando filtro passa-baixa
std::vector<short> lowPassFilter(const std::vector<short>& input, float strength) {
    std::vector<short> output(input.size());
    float alpha = strength; // Controle de força do filtro (0 < alpha < 1)

    float previousFilteredSample = input[0];
    for (size_t i = 0; i < input.size(); ++i) {
        float filteredSample = alpha * input[i] + (1.0f - alpha) * previousFilteredSample;
        output[i] = static_cast<short>(filteredSample);
        previousFilteredSample = filteredSample;
    }

    return output;
}

// Função para reforço de Midrange usando filtro passa-banda
std::vector<short> bandPassFilter(const std::vector<short>& input, float strength, int sampleRate) {
    std::vector<short> output(input.size());
    const float lowCutoff = 400.0;
    const float highCutoff = 2500.0;

    float rcLow = 1.0 / (2 * M_PI * lowCutoff);
    float dt = 1.0 / sampleRate;
    float alphaLow = dt / (rcLow + dt);

    float rcHigh = 1.0 / (2 * M_PI * highCutoff);
    float alphaHigh = rcHigh / (rcHigh + dt);

    float lowPass = 0.0, highPass = 0.0;
    for (size_t i = 0; i < input.size(); ++i) {
        lowPass += alphaLow * (input[i] - lowPass);
        highPass += alphaHigh * (lowPass - highPass);
        output[i] = static_cast<short>(highPass * strength);
    }

    return output;
}

// Método JNI para aplicar BassBoost
extern "C" JNIEXPORT jshortArray JNICALL
Java_com_example_audioeffects_AudioEffectsManager_applyBassBoostEffect(JNIEnv *env, jobject thiz, jshortArray audioData, jfloat bassStrength) {
    jsize length = env->GetArrayLength(audioData);
    jshort* audioDataArray = env->GetShortArrayElements(audioData, nullptr);

    std::vector<short> input(audioDataArray, audioDataArray + length);

    // Aplicar efeito BassBoost
    auto output = lowPassFilter(input, bassStrength);

    jshortArray result = env->NewShortArray(length);
    env->SetShortArrayRegion(result, 0, length, output.data());

    env->ReleaseShortArrayElements(audioData, audioDataArray, 0);

    return result;
}

// Método JNI para aplicar MidrangeBoost
extern "C" JNIEXPORT jshortArray JNICALL
Java_com_example_audioeffects_AudioEffectsManager_applyMidrangeBoostEffect(JNIEnv *env, jobject thiz, jshortArray audioData, jfloat midrangeStrength) {
    jsize length = env->GetArrayLength(audioData);
    jshort* audioDataArray = env->GetShortArrayElements(audioData, nullptr);

    std::vector<short> input(audioDataArray, audioDataArray + length);
    int sampleRate = 44100; // Taxa de amostragem comum

    // Aplicar efeito MidrangeBoost
    auto output = bandPassFilter(input, midrangeStrength, sampleRate);

    jshortArray result = env->NewShortArray(length);
    env->SetShortArrayRegion(result, 0, length, output.data());

    env->ReleaseShortArrayElements(audioData, audioDataArray, 0);

    return result;
}