package com.example.touchpad

import java.io.Serializable

sealed class TouchpadMessage : Serializable {
    data class MouseMove(val x: Float, val y: Float) : TouchpadMessage()
    data class MouseClick(val button: Int) : TouchpadMessage()
    data class MouseScroll(val deltaY: Float) : TouchpadMessage()
    object Connect : TouchpadMessage()
    object Disconnect : TouchpadMessage()
}

object MessageProtocol {
    fun serialize(message: TouchpadMessage): ByteArray {
        return when (message) {
            is TouchpadMessage.MouseMove -> {
                val x = message.x.toInt()
                val y = message.y.toInt()
                byteArrayOf(
                    0x01,
                    (x shr 8).toByte(),
                    x.toByte(),
                    (y shr 8).toByte(),
                    y.toByte()
                )
            }
            is TouchpadMessage.MouseClick -> {
                byteArrayOf(0x02, message.button.toByte())
            }
            is TouchpadMessage.MouseScroll -> {
                byteArrayOf(0x03, message.deltaY.toInt().toByte())
            }
            TouchpadMessage.Connect -> byteArrayOf(0x10)
            TouchpadMessage.Disconnect -> byteArrayOf(0x11)
        }
    }
}