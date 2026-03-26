package com.progali.connect.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import blufi.espressif.BlufiCallback
import blufi.espressif.BlufiClient
import blufi.espressif.params.BlufiConfigureParams
import blufi.espressif.response.BlufiStatusResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BlufiManager(private val context: Context) {

    private var blufiClient: BlufiClient? = null

    private val _connectionState = MutableStateFlow<BlufiConnectionState>(BlufiConnectionState.Disconnected)
    val connectionState: StateFlow<BlufiConnectionState> = _connectionState.asStateFlow()

    private val _deviceStatus = MutableStateFlow<BlufiStatusResponse?>(null)
    val deviceStatus: StateFlow<BlufiStatusResponse?> = _deviceStatus.asStateFlow()

    // Flujos para los datos personalizados del servidor
    private val _serverDomain = MutableStateFlow<String?>(null)
    val serverDomain: StateFlow<String?> = _serverDomain.asStateFlow()

    private val _serverPort = MutableStateFlow<String?>(null)
    val serverPort: StateFlow<String?> = _serverPort.asStateFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = BlufiConnectionState.Connected
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = BlufiConnectionState.Disconnected
                resetData()
            }
        }
    }

    private val blufiCallback = object : BlufiCallback() {
        override fun onGattPrepared(client: BlufiClient?, status: Int, gatt: BluetoothGatt?) {
            Log.d("BlufiManager", "Gatt prepared. Consultando datos...")
            client?.requestDeviceStatus()
            // Comandos personalizados para obtener servidor y puerto (asumimos GET_SERVER y GET_PORT)
            postCustomData("GET_SERVER".toByteArray())
            postCustomData("GET_PORT".toByteArray())
        }

        override fun onDeviceStatusResponse(client: BlufiClient?, status: Int, response: BlufiStatusResponse?) {
            if (status == STATUS_SUCCESS) _deviceStatus.value = response
        }

        override fun onReceiveCustomData(client: BlufiClient?, status: Int, data: ByteArray?) {
            if (status == STATUS_SUCCESS && data != null) {
                val response = String(data)
                Log.d("BlufiManager", "Dato personalizado recibido: $response")
                
                // Procesamos la respuesta (esto depende del formato exacto del firmware)
                when {
                    response.startsWith("SERVER:") -> _serverDomain.value = response.substringAfter("SERVER:")
                    response.startsWith("PORT:") -> _serverPort.value = response.substringAfter("PORT:")
                }
            }
        }

        override fun onError(client: BlufiClient?, errCode: Int) {
            _connectionState.value = BlufiConnectionState.Error("Error Blufi: $errCode")
        }
    }

    private fun resetData() {
        _deviceStatus.value = null
        _serverDomain.value = null
        _serverPort.value = null
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        close()
        _connectionState.value = BlufiConnectionState.Connecting
        blufiClient = BlufiClient(context, device)
        blufiClient?.setGattCallback(gattCallback)
        blufiClient?.setBlufiCallback(blufiCallback)
        blufiClient?.connect()
    }

    fun configureWifi(params: BlufiConfigureParams) = blufiClient?.configure(params)
    
    fun postCustomData(data: ByteArray) = blufiClient?.postCustomData(data)

    fun requestDeviceStatus() = blufiClient?.requestDeviceStatus()

    fun requestCloseConnection() = blufiClient?.requestCloseConnection()

    fun close() {
        blufiClient?.close()
        blufiClient = null
        _connectionState.value = BlufiConnectionState.Disconnected
    }
}

sealed class BlufiConnectionState {
    object Disconnected : BlufiConnectionState() { override fun toString() = "Desconectado" }
    object Connecting : BlufiConnectionState() { override fun toString() = "Conectando..." }
    object Connected : BlufiConnectionState() { override fun toString() = "Conectado" }
    data class Error(val message: String) : BlufiConnectionState()
}
