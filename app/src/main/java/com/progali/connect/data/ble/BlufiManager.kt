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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BlufiManager(private val context: Context) {

    private var blufiClient: BlufiClient? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectionState = MutableStateFlow<BlufiConnectionState>(BlufiConnectionState.Disconnected)
    val connectionState: StateFlow<BlufiConnectionState> = _connectionState.asStateFlow()

    private val _deviceStatus = MutableStateFlow<BlufiStatusResponse?>(null)
    val deviceStatus: StateFlow<BlufiStatusResponse?> = _deviceStatus.asStateFlow()

    private val _serverDomain = MutableStateFlow<String?>(null)
    val serverDomain: StateFlow<String?> = _serverDomain.asStateFlow()

    private val _serverPort = MutableStateFlow<String?>(null)
    val serverPort: StateFlow<String?> = _serverPort.asStateFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d("BlufiManager", "GATT State: $newState")
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = BlufiConnectionState.Disconnected
                resetData()
            }
        }
    }

    private val blufiCallback = object : BlufiCallback() {
        override fun onGattPrepared(client: BlufiClient?, status: Int, gatt: BluetoothGatt?) {
            Log.d("BlufiManager", "Canal listo. Iniciando negociación de seguridad...")
            // PASO 1: Iniciar seguridad (Obligatorio para datos personalizados)
            client?.negotiateSecurity()
        }

        override fun onNegotiateSecurityResult(client: BlufiClient?, status: Int) {
            if (status == STATUS_SUCCESS) {
                _connectionState.value = BlufiConnectionState.Connected
                Log.i("BlufiManager", "Seguridad establecida. Consultando servidor...")
                scope.launch {
                    delay(500)
                    client?.requestDeviceStatus()
                    delay(1500) // Mayor retardo para dar tiempo al equipo
                    Log.d("BlufiManager", "Pidiendo Servidor...")
                    postCustomData("GET_SERVER\r\n".toByteArray())
                    delay(1500)
                    Log.d("BlufiManager", "Pidiendo Puerto...")
                    postCustomData("GET_PORT\r\n".toByteArray())
                }
            } else {
                Log.e("BlufiManager", "Error en negociación de seguridad: $status")
            }
        }

        override fun onDeviceStatusResponse(client: BlufiClient?, status: Int, response: BlufiStatusResponse?) {
            if (status == STATUS_SUCCESS) _deviceStatus.value = response
        }

        override fun onReceiveCustomData(client: BlufiClient?, status: Int, data: ByteArray?) {
            if (status == STATUS_SUCCESS && data != null) {
                val response = String(data).trim()
                Log.i("BlufiManager", "DATO RECIBIDO RAW: '$response'")
                
                when {
                    response.startsWith("SERVER:", ignoreCase = true) ->
                        _serverDomain.value = response.substringAfter(":").trim()
                    response.startsWith("PORT:", ignoreCase = true) ->
                        _serverPort.value = response.substringAfter(":").trim()
                    response.all { it.isDigit() } ->
                        _serverPort.value = response
                    response.isNotEmpty() ->
                        _serverDomain.value = response
                }
            }
        }

        override fun onError(client: BlufiClient?, errCode: Int) {
            Log.e("BlufiManager", "Error Blufi: $errCode")
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
