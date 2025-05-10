#include <jni.h>
#include <string>
#include <vector>
#include <cmath>        // Para std::abs, std::pow
#include <cstdint>      // Para int16_t
#include <algorithm>    // Para std::transform, std::max, std::min
#include <android/log.h>
#include "ThreeBandEQ.h" // Seu header do EQ

// --- Variáveis Estáticas Globais para o Módulo JNI ---
// Objeto EQ estático. Será criado quando a biblioteca for carregada e persistirá.
static ThreeBandEQ eq;
// Flag para controlar se o EQ já foi configurado com eq.setup().
static bool eqInitialized = false;
// Armazena a última taxa de amostragem usada para configurar o EQ.
// Usado para detectar se a taxa de amostragem mudou e o EQ precisa ser reconfigurado.
static double currentSampleRateForEQ_JNI = 0.0;

// --- Constantes ---
// Frequências de corte para as bandas do EQ.
// Você pode tornar isso configurável no futuro se desejar.
const double LOW_MID_CUTOFF_HZ = 500.0;   // Crossover graves/médios
const double MID_HIGH_CUTOFF_HZ = 5000.0; // Crossover médios/agudos

// --- Macros para Logging ---
#define LOG_TAG_JNI "AudioEffectsManagerJNI" // Tag específica para logs desta JNI
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG_JNI, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG_JNI, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG_JNI, __VA_ARGS__)


// --- Função JNI Principal ---
// Nome da função deve corresponder exatamente a: Java_pacote_Classe_metodo
// Onde 'pacote' é com underscores em vez de pontos.
// Assumindo que sua classe AudioEqualizer.kt está em com.example.mediaplayerapp
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_mediaplayerapp_AudioEqualizer_applyBassMidTreble(
        JNIEnv *env,          // Ponteiro para o ambiente JNI
        jobject thiz,         // Referência ao objeto 'this' Java (a instância de AudioEqualizer)
        jbyteArray audioData, // Array de bytes de entrada com os dados de áudio PCM
        jfloat bassStrength,  // Ganho para graves em dB
        jfloat midStrength,   // Ganho para médios em dB
        jfloat trebleStrength,// Ganho para agudos em dB
        jint javaSampleRate   // Taxa de amostragem do áudio em Hz
) {
    LOGD("JNI applyBassMidTreble chamado. Bass: %.2fdB, Mid: %.2fdB, Treble: %.2fdB, SR: %dHz",
         bassStrength, midStrength, trebleStrength, javaSampleRate);

    // --- PASSO 1: Configurar o Equalizador (se necessário) ---
    double newSampleRate = static_cast<double>(javaSampleRate);

    // Verifica se o EQ precisa ser (re)configurado:
    // 1. Se ainda não foi inicializado (`!eqInitialized`).
    // 2. Se a taxa de amostragem atual (`newSampleRate`) é diferente da última usada (`currentSampleRateForEQ_JNI`).
    //    Usamos std::abs para comparar doubles devido a possíveis imprecisões de ponto flutuante.
    if (!eqInitialized || std::abs(currentSampleRateForEQ_JNI - newSampleRate) > 1e-6) {
        LOGI("Configurando ThreeBandEQ com SampleRate: %.0f Hz", newSampleRate);
        eq.setup(newSampleRate, LOW_MID_CUTOFF_HZ, MID_HIGH_CUTOFF_HZ);
        currentSampleRateForEQ_JNI = newSampleRate; // Atualiza a taxa de amostragem armazenada
        eqInitialized = true;                     // Marca como inicializado
    }

    // --- PASSO 2: Obter e Validar os Dados de Áudio do jbyteArray ---
    // Obtém um ponteiro para os elementos do array de bytes Java.
    // O JNI pode retornar um ponteiro direto para os dados no heap Java ou uma cópia.
    jbyte* audioBytes = env->GetByteArrayElements(audioData, nullptr);
    // Obtém o número total de bytes no array.
    jsize numBytes = env->GetArrayLength(audioData);

    // Se o buffer estiver vazio, não há nada a processar.
    if (numBytes == 0) {
        LOGD("Buffer de áudio de entrada está vazio.");
        if (audioBytes != nullptr) { // Mesmo se vazio, pode ter sido alocado
            env->ReleaseByteArrayElements(audioData, audioBytes, JNI_ABORT); // Libera sem copiar
        }
        return audioData; // Retorna o array original (vazio)
    }

    // Validação: Para PCM de 16 bits, o número de bytes deve ser par.
    if (numBytes % 2 != 0) {
        LOGE("Comprimento dos dados de áudio (%d bytes) deve ser par para PCM de 16 bits.", numBytes);
        if (audioBytes != nullptr) {
            env->ReleaseByteArrayElements(audioData, audioBytes, JNI_ABORT);
        }
        // Lança uma exceção Java para informar o erro ao chamador.
        jclass exClass = env->FindClass("java/lang/IllegalArgumentException");
        if (exClass != nullptr) {
            env->ThrowNew(exClass, "O comprimento dos dados de áudio deve ser par para PCM de 16 bits.");
            env->DeleteLocalRef(exClass); // Boa prática liberar referências locais
        } else {
            LOGE("Não foi possível encontrar a classe java/lang/IllegalArgumentException para lançar exceção.");
        }
        return nullptr; // Retorna null para indicar erro (uma exceção Java já foi lançada).
    }

    // --- PASSO 3: Converter jbyteArray para std::vector<int16_t> ---
    jsize numSamples = numBytes / 2; // Calcula o número de amostras de 16 bits.
    std::vector<int16_t> samples(numSamples); // Cria um vetor para armazenar as amostras.

    // Converte os bytes (jbyte*) para amostras de 16 bits (int16_t).
    // Assume que os dados PCM são little-endian (byte menos significativo primeiro).
    // audioBytes[2*i]   = LSB (Low Byte)
    // audioBytes[2*i+1] = MSB (High Byte)
    for (jsize i = 0; i < numSamples; ++i) {
        // Combina os dois bytes em uma amostra de 16 bits.
        // O cast para unsigned char (ou uint8_t) antes do shift é importante para
        // evitar a extensão de sinal se jbyte (que é signed char) for negativo.
        samples[i] = static_cast<int16_t>(
                (static_cast<unsigned char>(audioBytes[2 * i + 1]) << 8) | // Byte alto (MSB)
                (static_cast<unsigned char>(audioBytes[2 * i]))             // Byte baixo (LSB)
        );
    }

    // --- PASSO 4: Processar o Áudio com o Equalizador ---
    // Chama o método processBlock da instância do EQ, passando o vetor de amostras
    // e os ganhos para cada banda (já em dB, como esperado pela sua classe ThreeBandEQ).
    LOGD("Processando %d amostras com o EQ.", numSamples);
    eq.processBlock(samples, bassStrength, midStrength, trebleStrength);

    // --- PASSO 5: Converter std::vector<int16_t> de volta para jbyteArray ---
    // Escreve as amostras processadas de volta no buffer `audioBytes` (modificando no local).
    for (jsize i = 0; i < numSamples; ++i) {
        audioBytes[2 * i] = static_cast<jbyte>(samples[i] & 0xFF);          // Byte baixo (LSB)
        audioBytes[2 * i + 1] = static_cast<jbyte>((samples[i] >> 8) & 0xFF); // Byte alto (MSB)
    }

    // --- PASSO 6: Liberar Recursos e Retornar ---
    // Libera o buffer `audioBytes`.
    // O modo '0' (zero) significa:
    // 1. Copiar o conteúdo do buffer nativo `audioBytes` de volta para o array Java `audioData`.
    // 2. Liberar a memória do buffer nativo `audioBytes` (se o JNI fez uma cópia).
    if (audioBytes != nullptr) {
        env->ReleaseByteArrayElements(audioData, audioBytes, 0);
    }

    LOGD("Processamento JNI concluído.");
    // Retorna o jbyteArray original, que agora contém os dados de áudio processados.
    return audioData;
}
