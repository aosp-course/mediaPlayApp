package com.example.mediaplayerapp.ui.car

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.example.mediaplayerapp.R
import com.example.mediaplayerapp.cansimulator.CanMessage
import com.example.mediaplayerapp.cansimulator.VehicleCanBusSimulator
import com.example.mediaplayerapp.cansimulator.VehicleSensorSimulator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class CarFragment : Fragment() {
    private val TAG: String = "CarFragment"
    private val vehicleCanBusSimulator = VehicleCanBusSimulator()
    private val vehicleSensorSimulator = VehicleSensorSimulator("Speed Sensor")
    private val activityScope = CoroutineScope(Dispatchers.Main)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_car, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val backButton: ImageView = view.findViewById(R.id.backButton)
        val btReadSpeed: Button = view.findViewById(R.id.btnReadSpeed)
        val btSendCanVolume: Button = view.findViewById(R.id.btnSendCanVolume)
        val speedLabel: TextView = view.findViewById(R.id.tvSpeedLabel)
        val canVolumeLabel: TextView = view.findViewById(R.id.tvCanVolumeLabel)
        /**
         * @brief Configura o listener de clique para o botão de voltar.
         *
         * Ao ser clicado, este botão utiliza o `NavController` para navegar de volta
         * para a tela anterior na pilha de navegação.
         */
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        /**
         * @brief Configura o listener de clique para o botão de leitura de velocidade.
         *
         * Ao ser clicado, este botão:
         * 1. Lê os dados simulados de velocidade do `vehicleSensorSimulator`.
         * 2. Atualiza o texto do `speedLabel` para exibir a velocidade atual.
         * 3. Registra a velocidade lida no logcat para fins de depuração.
         */
        btReadSpeed.setOnClickListener {
            val currentSpeed = vehicleSensorSimulator.readSensorData()
            speedLabel.text = "Velocidade Atual: $currentSpeed km/h"
            Log.d(TAG, "Velocidade lida: $currentSpeed km/h")
        }

        /**
         * @brief Configura o listener de clique para o botão de envio de volume CAN.
         *
         * Ao ser clicado, este botão:
         * 1. Gera um valor de volume aleatório entre $$0$$ e $$100$$.
         * 2. Cria uma `CanMessage` com o ID `0x123` e o volume gerado como dado.
         * 3. Envia esta mensagem CAN para o `vehicleCanBusSimulator` para simulação.
         */
        btSendCanVolume.setOnClickListener {
            val randomVolume = (0..100).random()
            val message = CanMessage(id = 0x123, data = byteArrayOf(randomVolume.toByte()))
            vehicleCanBusSimulator.sendMessage(message)
            Log.d(TAG, "Volume enviado: $randomVolume")
        }

        /**
         * @brief Inicia a coleta de mensagens CAN do simulador.
         *
         * Lança uma corrotina no `activityScope` para coletar mensagens do `canMessageFlow`
         * do `vehicleCanBusSimulator`.
         * Se a mensagem tiver o ID `0x123` e contiver dados, extrai o valor do volume
         * (primeiro byte) e atualiza o `canVolumeLabel` na UI.
         */
        activityScope.launch {
            vehicleCanBusSimulator.canMessageFlow.collect {
                message -> if (message.id == 0x123 && message.data.isNotEmpty()) {
                    val volume = message.data[0].toInt() and 0xFF
                    canVolumeLabel.text = "Volume CAN: $volume"
                }
            }
        }
    }
}
