package com.example.mediaplayerapp.cansimulator

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * @brief Simula um barramento CAN (Controller Area Network) para o veículo.
 *
 * Esta classe simula o comportamento de um barramento CAN automotivo, permitindo
 * o envio e recebimento de mensagens CAN. Ela utiliza Coroutines do Kotlin para
 * processamento assíncrono e canais para comunicação de mensagens.
 *
 * As mensagens CAN recebidas são processadas e emitidas através de um `SharedFlow`
 * para que outros componentes da aplicação possam observá-las.
 */
class VehicleCanBusSimulator {
    /**
     * @brief Tag para logging, usada para identificar as mensagens de log desta classe.
     */
    private val TAG = "VehicleCanBusSimulator"

    /**
     * @brief Canal de comunicação para enviar e receber mensagens CAN internamente.
     *
     * Este `Channel` atua como uma fila de mensagens, onde as mensagens são enviadas
     * para processamento assíncrono.
     */
    private val messageChannel = Channel<CanMessage>()

    /**
     * @brief Escopo de corrotinas utilizado para gerenciar as operações assíncronas da classe.
     *
     * O `Dispatchers.Default` é usado para tarefas que consomem CPU e operações de E/S.
     */
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * @brief Um `MutableSharedFlow` interno para emitir mensagens CAN processadas.
     *
     * Este flow permite que múltiplos coletores (observers) recebam as mensagens
     * CAN que foram processadas pelo simulador.
     */
    private val _canMessageFlow = MutableSharedFlow<CanMessage>()

    /**
     * @brief Um `SharedFlow` público que expõe as mensagens CAN processadas.
     *
     * Outras partes da aplicação podem coletar este flow para reagir a mensagens
     * específicas do barramento CAN.
     */
    val canMessageFlow = _canMessageFlow.asSharedFlow()

    /**
     * @brief Bloco de inicialização da classe.
     *
     * Lança uma corrotina no `scope` que consome mensagens do `messageChannel`.
     * Cada mensagem recebida é logada e emitida para o `_canMessageFlow`.
     * Atualmente, processa mensagens com ID `0x123` para extrair o volume mestre.
     */
    init {
        scope.launch {
            for (message in messageChannel) {
                Log.d(
                    TAG, "Mensagem CAN recebida (ID: 0x%X, Dados: %s)".format(
                        message.id,
                        message.data.joinToString { "%02X".format(it) }))
                _canMessageFlow.emit(message)
                when (message.id) {
                    0x123 -> {
                        if (message.data.isNotEmpty()) {
                            val volume = message.data[0].toInt() and 0xFF
                            Log.i(TAG, "Processando mensagem CAN: Novo Volume Mestre: $volume")
                        }
                    }
                }
            }
        }
    }

    /**
     * @brief Envia uma mensagem CAN para o simulador.
     *
     * Esta função lança uma corrotina para enviar a `CanMessage` fornecida
     * para o `messageChannel` para processamento assíncrono.
     *
     * @param message A `CanMessage` a ser enviada.
     */
    fun sendMessage(message: CanMessage) {
        scope.launch {
            Log.i(TAG, "Enviando mensagem CAN (ID: 0x%X, Dados: %s)".format(message.id,
                message.data.joinToString { "%02X".format(it) }))
            messageChannel.send(message)
        }
    }

    /**
     * @brief Para o simulador CAN.
     *
     * Cancela todas as corrotinas no `scope` e fecha o `messageChannel`,
     * liberando os recursos e impedindo o processamento de novas mensagens.
     */
    fun stopSimulator() {
        scope.cancel()
        messageChannel.close()
        Log.d(TAG, "Simulador CAN parado.")
    }
}