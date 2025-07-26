package com.example.mediaplayerapp.cansimulator

/**
 * @brief Representa uma mensagem do barramento CAN.
 *
 * @param id O identificador da mensagem CAN (ID).
 * @param data Os dados contidos na mensagem CAN, representados como um array de bytes.
 */
data class CanMessage(val id: Int, val data: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (javaClass != other?.javaClass)
            return false
        other as CanMessage
        if (id != other.id)
            return false
        if (!data.contentEquals(other.data))
            return false
        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + data.contentHashCode()
        return result
    }
}
