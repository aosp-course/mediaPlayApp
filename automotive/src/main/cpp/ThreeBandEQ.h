#ifndef THREE_BAND_EQ_H
#define THREE_BAND_EQ_H

#include "BiquadFilters.h"
#include <vector>
#include <cstdint>
#include <algorithm> // For std::transform, std::clamp
#include <cmath>     // For pow

// Helper to convert dB to linear gain
float dbToLinear(float db) {
    return std::pow(10.0f, db / 20.0f);
}

class ThreeBandEQ {
public:
    ThreeBandEQ() = default;

    void setup(double sampleRate, double lowMidCutoffHz, double midHighCutoffHz) {
        m_sampleRate = sampleRate;
        m_lowMidCutoff = lowMidCutoffHz;
        m_midHighCutoff = midHighCutoffHz;

        // For a 2nd Order Linkwitz-Riley, Q is 0.707 for each of the two cascaded 1st order Butterworth
        // or if using a single biquad per stage, Q is 0.5 for each of the 2nd order stages
        // For simplicity here, we'll use two biquads for a 4th order LR-like effect (steeper slope)
        // by cascading two 2nd-order Butterworth filters (Q=0.707).
        // If you want a true 2nd order LR (12dB/octave), use Q=0.5 and only one filter per path.
        // Here, Q=0.70707 makes each biquad a 2nd order Butterworth. Cascading two gives a 4th order Linkwitz-Riley.
        double q = 0.70710678118; // Q for Butterworth (1/sqrt(2))

        // Low band path (LPF at lowMidCutoff)
        lpf1_low.setCoefficients(FilterType::LOWPASS, sampleRate, lowMidCutoffHz, q);
        lpf2_low.setCoefficients(FilterType::LOWPASS, sampleRate, lowMidCutoffHz, q);

        // High band path (HPF at midHighCutoff)
        hpf1_high.setCoefficients(FilterType::HIGHPASS, sampleRate, midHighCutoffHz, q);
        hpf2_high.setCoefficients(FilterType::HIGHPASS, sampleRate, midHighCutoffHz, q);

        // For mid band, we need signal components between lowMidCutoff and midHighCutoff
        // We can take the full signal, subtract the low, and subtract the high.
        // Or, more directly for LR crossovers:
        // Mid = LPF(midHighCutoff) - LPF(lowMidCutoff)
        // which is equivalent to HPF(lowMidCutoff) applied to LPF(midHighCutoff) output.
        lpf1_mid.setCoefficients(FilterType::LOWPASS, sampleRate, midHighCutoffHz, q);
        lpf2_mid.setCoefficients(FilterType::LOWPASS, sampleRate, midHighCutoffHz, q);

        hpf1_mid.setCoefficients(FilterType::HIGHPASS, sampleRate, lowMidCutoffHz, q);
        hpf2_mid.setCoefficients(FilterType::HIGHPASS, sampleRate, lowMidCutoffHz, q);
    }

    // Process a block of 16-bit PCM audio samples
    // Gains are in dB
    void processBlock(std::vector<int16_t>& samples, float gainLowDb, float gainMidDb, float gainHighDb) {
        if (m_sampleRate == 0) return; // Not initialized

        float linGainLow = dbToLinear(gainLowDb);
        float linGainMid = dbToLinear(gainMidDb);
        float linGainHigh = dbToLinear(gainHighDb);

        std::vector<float> floatSamples(samples.size());

        // Convert int16_t to float [-1.0, 1.0]
        std::transform(samples.begin(), samples.end(), floatSamples.begin(),
                       [](int16_t s) { return static_cast<float>(s) / 32768.0f; });

        for (size_t i = 0; i < floatSamples.size(); ++i) {
            float inputSample = floatSamples[i];

            // --- Low Band ---
            float lowSignal = lpf2_low.process(lpf1_low.process(inputSample));

            // --- High Band ---
            float highSignal = hpf2_high.process(hpf1_high.process(inputSample));

            // --- Mid Band ---
            // Pass through LPF (cutoff at midHigh) then HPF (cutoff at lowMid)
            float midSignalPath = hpf2_mid.process(hpf1_mid.process(
                    lpf2_mid.process(lpf1_mid.process(inputSample))
            ));
            // An alternative for mid-band in an LR crossover setup:
            // If using true LR crossovers, mid = original - low - high.
            // However, since we are directly filtering, the above bandpass approach is more common.
            // For a simpler crossover:
            // float midSignal = inputSample - lowSignal - highSignal; // This works if LPF and HPF are perfect complementary.
            // With the bandpass approach:
            float midSignal = midSignalPath;


            // Apply gains
            lowSignal *= linGainLow;
            midSignal *= linGainMid;
            highSignal *= linGainHigh;

            // Sum bands
            float outputSample = lowSignal + midSignal + highSignal;

            // Store back (clipping will happen during conversion to int16_t)
            floatSamples[i] = outputSample;
        }

        // Convert float back to int16_t with clipping
        std::transform(floatSamples.begin(), floatSamples.end(), samples.begin(),
                       [](float s) {
                           float clamped = std::max(-1.0f, std::min(1.0f, s)); // Ensure in [-1,1]
                           return static_cast<int16_t>(clamped * 32767.0f);   // Scale and cast
                       });
    }

    void resetFilters() {
        lpf1_low.reset(); lpf2_low.reset();
        hpf1_high.reset(); hpf2_high.reset();
        lpf1_mid.reset(); lpf2_mid.reset();
        hpf1_mid.reset(); hpf2_mid.reset();
    }

private:
    double m_sampleRate = 0;
    double m_lowMidCutoff = 0;
    double m_midHighCutoff = 0;

    // Filters for 4th order Linkwitz-Riley like crossover
    // Low Band path
    BiquadFilter lpf1_low, lpf2_low;
    // High Band path
    BiquadFilter hpf1_high, hpf2_high;
    // Mid Band path (LPF up to midHigh, then HPF from lowMid)
    BiquadFilter lpf1_mid, lpf2_mid; // LPF part of mid band (up to midHighCutoff)
    BiquadFilter hpf1_mid, hpf2_mid; // HPF part of mid band (from lowMidCutoff)
};

#endif // THREE_BAND_EQ_H
