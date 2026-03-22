package com.rmb.app_deteccion_rostro_ia

import android.bluetooth.*
import java.io.OutputStream
import java.util.*

class BluetoothManager {

    private val deviceName = "ESP32_OJOS_IA"

    private val uuid: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    fun connect(): Boolean {

        val adapter = BluetoothAdapter.getDefaultAdapter()

        val device = adapter.bondedDevices.firstOrNull {
            it.name == deviceName
        } ?: return false

        socket = device.createRfcommSocketToServiceRecord(uuid)

        adapter.cancelDiscovery()
        socket!!.connect()

        outputStream = socket!!.outputStream

        return true
    }

    fun sendData(data: String) {
        outputStream?.write(data.toByteArray())
    }
}