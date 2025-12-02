package com.example.mediaplayerapp.cansimulator

import android.util.Log
import java.util.Random

/**
 * @brief Simula um sensor de veículo, gerando dados aleatórios com base no tipo de sensor.
 *
 * Esta classe fornece uma simulação básica para diferentes tipos de sensores veiculares,
 * como velocidade, temperatura externa e nível de combustível. Ela gera dados aleatórios
 * dentro de faixas realistas para cada tipo de sensor e inclui uma função para simular
 * a calibração do sensor.
 */
class VehicleSensorSimulator(private val sensorName: String) {
    private val TAG = "VehicleSensorSimulator"
    private val random = Random()

    /**
     * @brief Simula a leitura de dados de um sensor específico do veículo.
     *
     * O valor gerado depende do `sensorName` configurado na inicialização da classe.
     * - "Velocidade": Gera um valor entre $$0$$ e $$200$$ (km/h).
     * - "Temperatura Externa": Gera um valor entre $$-10$$ e $$39$$ ($$^\circ C$$).
     * - "Nível Combustível": Gera um valor entre $$0$$ e $$100$$ ($$\%$$).
     * - Outros: Gera um valor genérico entre $$0$$ e $$100$$.
     *
     * Os dados lidos são registrados no logcat.
     *
     * @return Um valor inteiro que simula a leitura atual do sensor.
     */
    fun readSensorData(): Int {
        val data = when (sensorName) {
            "Velocidade" -> random.nextInt(201) // 0-200 km/h
            "Temperatura Externa" -> random.nextInt(50) - 10 // -10 a 39 °C
            "Nível Combustível" -> random.nextInt(101) // 0-100 %
            else -> random.nextInt(101) // Valor genérico 0-100
        }
        Log.d(TAG, "[$sensorName] Lendo dados do sensor: $data")
        return data
    }

    /**
     * @brief Simula uma operação de calibração para o sensor.
     *
     * Este método simula um processo de calibração, introduzindo um atraso de $$1$$ segundo
     * para representar o tempo necessário para a operação. Mensagens de início e fim
     * da calibração são registradas no logcat.
     */
    fun calibrateSensor() {
        Log.i(TAG, "[$sensorName] Calibrando sensor...")
        // Simula um processo de calibração que levaria tempo
        Thread.sleep(1000) // Simula um atraso de 1 segundo
        Log.i(TAG, "[$sensorName] Sensor calibrado.")
    }
}
