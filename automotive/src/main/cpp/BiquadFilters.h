#ifndef BIQUAD_FILTER_H
#define BIQUAD_FILTER_H

#include <cmath>
#include <vector>

// Enum para os dois tipos de filtro que vão ser usados: passa-baixa e passa-alta
enum class FilterType {
    LOWPASS,
    HIGHPASS
};

class BiquadFilter {
public:
    BiquadFilter() = default;

    void setCoefficients(FilterType type, double sampleRate, double cutoffFreq, double q) {
        // Calcula os coeficients do filtro com base na frequência de corte de cada filtro, a taxa de amostragem e o fator de qualidade Q do filtro
        // a frequência de corte é a frequência mínima que o filtro passa-alta irá permitir; ou a frequência máxima que o filtro passa-baixa irá permitir
        double norm;
        double K = std::tan(M_PI * cutoffFreq / sampleRate);
        double K2 = K * K;

        if (type == FilterType::LOWPASS) {
            norm = 1.0 / (1.0 + K / q + K2);
            b0 = K2 * norm;
            b1 = 2.0 * b0;
            b2 = b0;
            a1 = 2.0 * (K2 - 1.0) * norm;
            a2 = (1.0 - K / q + K2) * norm;
        } else if (type == FilterType::HIGHPASS) {
            norm = 1.0 / (1.0 + K / q + K2);
            b0 = 1.0 * norm;
            b1 = -2.0 * b0;
            b2 = b0;
            a1 = 2.0 * (K2 - 1.0) * norm;
            a2 = (1.0 - K / q + K2) * norm;
        }

        reset(); // limpa os valores anteriores de x1, x2, y1 e y2
    }


    float process(float input) {
        // para um filtro IIR Biquad, o processamento do áudio é feito de forma recursiva: b0, b1, b2, a1 e a2 são os
        // zeros e raízes da resposta ao impulso do sistema, e y1, y2, x1 e x2 são os valores passados de saída e
        // entrada do sistema que precisam ser armazenados para poder processar os próximos valores
        float output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;

        // limitando o valor da saída para que não ultrapasse o valor de 1 a -1, que depois será convertido
        // para os valores máximos de 8 bits que representa o áudio
        output = std::max(-1.0f, std::min(1.0f, output));


        // armazena os valores passados para o próximo cálculo
        x2 = x1;
        x1 = input;
        y2 = y1;
        y1 = output;

        return output;
    }

    void reset() {
        x1 = x2 = y1 = y2 = 0.0f;
    }

private:
    // coeficientes do filtro
    double b0 = 1.0, b1 = 0.0, b2 = 0.0;
    double a1 = 0.0, a2 = 0.0;

    // variáveis de estado
    float x1 = 0.0f, x2 = 0.0f;
    float y1 = 0.0f, y2 = 0.0f;
};

#endif // BIQUAD_FILTER_H
