#ifndef THREE_BAND_EQ_H
#define THREE_BAND_EQ_H

#include "BiquadFilters.h"
#include <vector>
#include <cstdint>
#include <algorithm>
#include <cmath>

// converte um ganho em DB em um valor linear de ganho
float dbToLinear(float db) {
    return std::pow(10.0f, db / 20.0f);
}

class ThreeBandEQ {
public:
    ThreeBandEQ() = default;

    void setup(double sampleRate, double lowMidCutoffHz, double midHighCutoffHz) {
        // a implementacao abaixo do equalizador utiliza 8 filtros BiQuad de resposta ao impulso infinita (IIR) do tipo Butterworth
        // em cascata (um filtro após o outro). os 8 filtros são utilizados da seguinte forma:

        // 1 - um filtro passa baixa de 4a ordem, utilizando 2 filtros passa baixa de 2a ordem em cascata
        // 2 - um filtro passa alta de 4a ordem, utilizando 2 filtros passa alta de 2a ordem em cascata
        // 3 - um filtro passa faixa de 4a ordem, utilizando 2 filtros passa baixa e 2 filtros passa alta em cascata

        // com esses 3 filtros, nós conseguimos processar um sinal de áudio dividindo ele em 3 regiões: a região de
        // frequências graves (Bass), frequências médias (Midrange) e frequências agudas (Treble)
        // para cada uma dessas faixas de frequência, conseguimos determinar um ganho positivo ou negativo
        // para que a equalização aumente ou diminua o volume do áudio para aquela determinada frequência

        double q = 0.70710678118; // fator de qualidade Q para um filtro Butterworth (1/sqrt(2))

        // filtro passa baixa
        lpf1_low.setCoefficients(FilterType::LOWPASS, sampleRate, lowMidCutoffHz, q);
        lpf2_low.setCoefficients(FilterType::LOWPASS, sampleRate, lowMidCutoffHz, q);

        // filtro passa alta
        hpf1_high.setCoefficients(FilterType::HIGHPASS, sampleRate, midHighCutoffHz, q);
        hpf2_high.setCoefficients(FilterType::HIGHPASS, sampleRate, midHighCutoffHz, q);

        // filtro passa faixa
        lpf1_mid.setCoefficients(FilterType::LOWPASS, sampleRate, midHighCutoffHz, q);
        lpf2_mid.setCoefficients(FilterType::LOWPASS, sampleRate, midHighCutoffHz, q);

        hpf1_mid.setCoefficients(FilterType::HIGHPASS, sampleRate, lowMidCutoffHz, q);
        hpf2_mid.setCoefficients(FilterType::HIGHPASS, sampleRate, lowMidCutoffHz, q);
    }

    void processBlock(std::vector<int16_t>& samples, float gainLowDb, float gainMidDb, float gainHighDb) {
        float linGainLow = dbToLinear(gainLowDb);
        float linGainMid = dbToLinear(gainMidDb);
        float linGainHigh = dbToLinear(gainHighDb);

        // cria o vetor de saida
        std::vector<float> floatSamples(samples.size());

        // o bloco de áudio a ser processado está em 16-bit PCM

        // convertendo int16_t para float [-1.0, 1.0]
        std::transform(samples.begin(), samples.end(), floatSamples.begin(),
                       [](int16_t s) { return static_cast<float>(s) / 32768.0f; });

        for (size_t i = 0; i < floatSamples.size(); ++i) {
            float inputSample = floatSamples[i];

            // frequências graves do sinal
            float lowSignal = lpf2_low.process(lpf1_low.process(inputSample));

            // frequências altas do sinal
            float highSignal = hpf2_high.process(hpf1_high.process(inputSample));

            // frequências médias do sinal: filtra as altas e filtra as baixas, deixando as médias
            float midSignal = hpf2_mid.process(hpf1_mid.process(
                    lpf2_mid.process(lpf1_mid.process(inputSample))
            ));

            // multiplica as amplitudes dos sinais pelo ganho de cada um deles
            lowSignal *= linGainLow;
            midSignal *= linGainMid;
            highSignal *= linGainHigh;

            // soma os 3 sinais
            float outputSample = lowSignal + midSignal + highSignal;

            // guarda o valor no vetor de saida
            floatSamples[i] = outputSample;
        }

        // converte de float de volta para uint_16t
        std::transform(floatSamples.begin(), floatSamples.end(), samples.begin(),
                       [](float s) {
                           float clamped = std::max(-1.0f, std::min(1.0f, s)); // garante que os valores nao ultrapassam -1 ou 1
                           return static_cast<int16_t>(clamped * 32767.0f);   // converte da escola de -1 a 1 para o valor maximo de 16 bits
                       });
    }

private:
    // banda de frequências baixas
    BiquadFilter lpf1_low, lpf2_low;
    // banda de frequências altas
    BiquadFilter hpf1_high, hpf2_high;
    // banda de frequências médias
    BiquadFilter lpf1_mid, lpf2_mid; // filtro passa baixa usado pras frequências médias
    BiquadFilter hpf1_mid, hpf2_mid; // filtro passa alta usado pras frequ~encias médias
};

#endif // THREE_BAND_EQ_H
