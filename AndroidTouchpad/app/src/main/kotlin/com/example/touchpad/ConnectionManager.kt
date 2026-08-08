package com.example.touchpad

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.*
import java.io.*
import java.net.Socket

interface ConnectionCallback {
    fun onConnected()
    fun onDisconnected()
    fun onError(message: String)
}

class ConnectionManager(private val context: Context) {
    private var socket: Socket? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var scope = CoroutineScope(Dispatchers.IO + Job())
    private var callback: ConnectionCallback? = null
    var isConnected = false

    fun setCallback(cb: ConnectionCallback) {
        callback = cb
    }

    fun connectViaWiFi(host: String, port: Int) {
        scope.launch {
            try {
                socket = Socket(host, port)
                outputStream = socket?.getOutputStream()
                isConnected = true

                sendMessage(TouchpadMessage.Connect)

                withContext(Dispatchers.Main) {
                    callback?.onConnected()
                }
            } catch (e: Exception) {
                isConnected = false
                withContext(Dispatchers.Main) {
                    callback?.onError("Wi-Fi помилка: ${e.message}")
                }
            }
        }
    }

    fun connectViaBluetooth(deviceAddress: String) {
        scope.launch {
            try {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

                val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()

                outputStream = bluetoothSocket?.getOutputStream()
                isConnected = true

                sendMessage(TouchpadMessage.Connect)

                withContext(Dispatchers.Main) {
                    callback?.onConnected()
                }
            } catch (e: Exception) {
                isConnected = false
                withContext(Dispatchers.Main) {
                    callback?.onError("Bluetooth помилка: ${e.message}")
                }
            }
        }
    }

    fun sendMessage(message: TouchpadMessage) {
        if (!isConnected) return

        scope.launch {
            try {
                val data = MessageProtocol.serialize(message)
                outputStream?.write(data)
                outputStream?.flush()
            } catch (e: Exception) {
                isConnected = false
                withContext(Dispatchers.Main) {
                    callback?.onError("Помилка надсилання: ${e.message}")
                    disconnect()
                }
            }
        }
    }

    fun disconnect() {
        isConnected = false
        scope.launch {
            try {
                sendMessage(TouchpadMessage.Disconnect)
                Thread.sleep(100)

                socket?.close()
                bluetoothSocket?.close()
                outputStream?.close()

                withContext(Dispatchers.Main) {
                    callback?.onDisconnected()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onDisconnected()
                }
            }
        }
    }

    fun destroy() {
        disconnect()
        scope.cancel()
    }
}