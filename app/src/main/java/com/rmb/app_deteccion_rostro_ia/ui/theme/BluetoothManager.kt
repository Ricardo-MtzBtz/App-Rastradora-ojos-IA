package com.rmb.app_deteccion_rostro_ia

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.OutputStream
import java.util.*

class BluetoothManager {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val deviceName = "ESP32_ROBOT"

    fun connect(): Boolean {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Log.d("BT","Bluetooth no disponible")
            return false
        }

        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter!!.bondedDevices

        for (device in pairedDevices) {

            if (device.name == deviceName) {

                try {

                    val uuid = device.uuids[0].uuid

                    bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)

                    bluetoothSocket!!.connect()

                    outputStream = bluetoothSocket!!.outputStream

                    Log.d("BT","ESP32 conectado")

                    return true

                } catch (e: Exception) {

                    Log.d("BT","Error conectando")

                    return false
                }
            }
        }

        return false
    }

    fun sendCoordinates(x: Int, y: Int) {

        try {

            val data = "$x,$y\n"

            outputStream?.write(data.toByteArray())

        } catch (e: Exception) {

            Log.d("BT","Error enviando datos")

        }

    }
}