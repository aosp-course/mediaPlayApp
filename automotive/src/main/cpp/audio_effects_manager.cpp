#include <jni.h>
#include <string>
#include <vector>
#include <cmath>
#include <android/log.h>

#define LOG_TAG "AudioEffectsManager"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Função para reforço de Bass usando filtro passa-baixa (agora com ByteArray)
std::vector<jbyte> lowPassFilter(const std::vector<jbyte>& input, float strength) {
    std::vector<jbyte> output(input.size());
    float alpha = strength; // Controle de força do filtro (0 < alpha < 1)

    float previousFilteredSample = static_cast<float>(input[0]);
    for (size_t i = 0; i < input.size(); ++i) {
        float filteredSample = alpha * static_cast<float>(input[i]) + (1.0f - alpha) * previousFilteredSample;
        output[i] = static_cast<jbyte>(filteredSample);
        previousFilteredSample = filteredSample;
    }

    return output;
}

// Função para reforço de Midrange usando filtro passa-banda (agora com ByteArray)
std::vector<jbyte> bandPassFilter(const std::vector<jbyte>& input, float strength, int sampleRate) {
    std::vector<jbyte> output(input.size());
    const float lowCutoff = 400.0;
    const float highCutoff = 2500.0;

    float rcLow = 1.0f / (2 * M_PI * lowCutoff);
    float dt = 1.0f / sampleRate;
    float alphaLow = dt / (rcLow + dt);

    float rcHigh = 1.0f / (2 * M_PI * highCutoff);
    float alphaHigh = rcHigh / (rcHigh + dt);

    float lowPass = 0.0f, highPass = 0.0f;
    for (size_t i = 0; i < input.size(); ++i) {
        lowPass += alphaLow * (static_cast<float>(input[i]) - lowPass);
        highPass += alphaHigh * (lowPass - highPass);
        output[i] = static_cast<jbyte>(highPass * strength);
    }

    return output;
}

// Método JNI para aplicar BassBoost (agora utilizando ByteArray)
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_mediaplayerapp_AudioEqualizer_applyBassBoostEffect(JNIEnv *env, jobject thiz, jbyteArray audioData, jfloat bassStrength) {
    jsize length = env->GetArrayLength(audioData);
    jbyte* audioDataArray = env->GetByteArrayElements(audioData, nullptr);

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
Java_com_example_mediaplayerapp_AudioEqualizer_applyMidrangeBoostEffect(JNIEnv *env, jobject thiz, jbyteArray audioData, jfloat midrangeStrength) {
    jsize length = env->GetArrayLength(audioData);
    jbyte* audioDataArray = env->GetByteArrayElements(audioData, nullptr);

    std::vector<jbyte> input(audioDataArray, audioDataArray + length);
    int sampleRate = 44100; // Taxa de amostragem comum

    // Aplicar efeito MidrangeBoost
    auto output = bandPassFilter(input, midrangeStrength, sampleRate);

    jbyteArray result = env->NewByteArray(length);
    env->SetByteArrayRegion(result, 0, length, output.data());

    env->ReleaseByteArrayElements(audioData, audioDataArray, 0);

    return result;
}

