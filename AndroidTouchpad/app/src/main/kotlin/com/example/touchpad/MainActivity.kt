package com.example.touchpad

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), ConnectionCallback {

    private lateinit var statusText: TextView
    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private lateinit var touchpadView: TouchpadView
    private lateinit var connectionManager: ConnectionManager

    private val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            Toast.makeText(this, "Дозволи надано", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth недоступний без дозволів", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        hostInput = findViewById(R.id.hostInput)
        portInput = findViewById(R.id.portInput)
        touchpadView = findViewById(R.id.touchpadView)

        connectionManager = ConnectionManager(this)
        connectionManager.setCallback(this)

        touchpadView.onTouchCallback = { message ->
            connectionManager.sendMessage(message)
        }

        findViewById<Button>(R.id.connectWifiBtn).setOnClickListener {
            val host = hostInput.text.toString().trim()
            val port = portInput.text.toString().toIntOrNull()

            if (host.isEmpty() || port == null) {
                Toast.makeText(this, "Введіть коректні IP та порт", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            connectionManager.connectViaWiFi(host, port)
        }

        findViewById<Button>(R.id.connectBluetoothBtn).setOnClickListener {
            ensureBluetoothPermissions {
                showPairedDevicesAndConnect()
            }
        }

        findViewById<Button>(R.id.disconnectBtn).setOnClickListener {
            connectionManager.disconnect()
        }
    }

    private fun ensureBluetoothPermissions(onGranted: () -> Unit) {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            onGranted()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun showPairedDevicesAndConnect() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            Toast.makeText(this, "Bluetooth не підтримується", Toast.LENGTH_SHORT).show()
            return
        }
        if (!adapter.isEnabled) {
            Toast.makeText(this, "Увімкніть Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        val paired = try {
            adapter.bondedDevices
        } catch (e: SecurityException) {
            Toast.makeText(this, "Немає дозволу на Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if (paired.isNullOrEmpty()) {
            Toast.makeText(this, "Немає спарених пристроїв. Спаруйте ПК у налаштуваннях Bluetooth.", Toast.LENGTH_LONG).show()
            return
        }

        // Спрощено: підключаємось до першого спареного пристрою.
        val device = paired.first()
        connectionManager.connectViaBluetooth(device.address)
    }

    override fun onConnected() {
        statusText.text = "✅ Підключено"
        statusText.setTextColor(0xFF388E3C.toInt())
    }

    override fun onDisconnected() {
        statusText.text = "❌ Не підключено"
        statusText.setTextColor(0xFFD32F2F.toInt())
    }

    override fun onError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        onDisconnected()
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionManager.destroy()
    }
}