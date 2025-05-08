#ifndef BIQUAD_FILTER_H
#define BIQUAD_FILTER_H

#include <cmath> // For M_PI, cos, sin, sqrt
#include <vector>

// Enum for filter types
enum class FilterType {
    LOWPASS,
    HIGHPASS
};

class BiquadFilter {
public:
    BiquadFilter() = default;

    void setCoefficients(FilterType type, double sampleRate, double cutoffFreq, double q) {
        // Normalize coefficients by a0 (which is calculated but then used to divide others)
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
        // For other types like bandpass, peak, notch, shelves, add more here
        // See RBJ Audio EQ Cookbook for formulas

        reset(); // Reset state when coefficients change
    }

    // Process a single sample
    float process(float input) {
        float output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;

        // Clamp output to prevent extreme values if filter becomes unstable
        // This is a basic safeguard, proper stability checks are more involved.
        output = std::max(-1.0f, std::min(1.0f, output));


        // Shift delay lines
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
    // Coefficients
    double b0 = 1.0, b1 = 0.0, b2 = 0.0;
    double a1 = 0.0, a2 = 0.0; // a0 is implicitly 1 after normalization

    // State variables (delay lines)
    float x1 = 0.0f, x2 = 0.0f; // Input delay
    float y1 = 0.0f, y2 = 0.0f; // Output delay
};

#endif // BIQUAD_FILTER_H
