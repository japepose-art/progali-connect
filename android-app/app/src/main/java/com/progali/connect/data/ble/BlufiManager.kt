package com.progali.connect.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import blufi.espressif.BlufiCallback
import blufi.espressif.BlufiClient
import blufi.espressif.BlufiClientImpl
import blufi.espressif.params.BlufiConfigureParams
import blufi.espressif.response.BlufiStatusResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BlufiManager(private val context: Context) {

    private var blufiClient: BlufiClient? = null

    private val _connectionState = MutableStateFlow<BlufiConnectionState>(BlufiConnectionState.Disconnected)
    val connectionState: StateFlow<BlufiConnectionState> = _connectionState.asStateFlow()

    private val callback = object : BlufiCallback() {
        override fun onGattConnectionChange(client: BlufiClient?, status: Int, newState: Int) {
            Log.d("BlufiManager", "Gatt connection change: $newState, status: $status")
            if (newState == 2) { // Connected
                _connectionState.value = BlufiConnectionState.Connected
            } else {
                _connectionState.value = BlufiConnectionState.Disconnected
            }
        }

        override fun onConfigureResult(client: BlufiClient?, status: Int) {
            Log.d("BlufiManager", "Configuration result: $status")
        }

        override fun onDeviceStatusResponse(client: BlufiClient?, status: Int, response: BlufiStatusResponse?) {
            Log.d("BlufiManager", "Device status response: $status")
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        close()
        _connectionState.value = BlufiConnectionState.Connecting
        blufiClient = BlufiClientImpl(context, device)
        blufiClient?.setCallback(callback)
        blufiClient?.connect()
    }

    fun disconnect() {
        blufiClient?.disconnect()
    }

    fun close() {
        blufiClient?.close()
        blufiClient = null
        _connectionState.value = BlufiConnectionState.Disconnected
    }
}

sealed class BlufiConnectionState {
    object Disconnected : BlufiConnectionState()
    object Connecting : BlufiConnectionState()
    object Connected : BlufiConnectionState()
    data class Error(val message: String) : BlufiConnectionState()
}
