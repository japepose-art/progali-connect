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

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    private val _lastRawResponse = MutableStateFlow<String?>(null)
    val lastRawResponse: StateFlow<String?> = _lastRawResponse.asStateFlow()

    private val _wifiNetworks = MutableStateFlow<List<blufi.espressif.response.BlufiScanResult>>(emptyList())
    val wifiNetworks: StateFlow<List<blufi.espressif.response.BlufiScanResult>> = _wifiNetworks.asStateFlow()

    private val _isWifiScanning = MutableStateFlow(false)
    val isWifiScanning: StateFlow<Boolean> = _isWifiScanning.asStateFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d("BlufiManager", "GATT State: $newState")
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = BlufiConnectionState.Disconnected
                resetData()
            }
        }
    }

    private val _wifiConfigureResult = MutableStateFlow<WifiConfigureResult?>(null)
    val wifiConfigureResult: StateFlow<WifiConfigureResult?> = _wifiConfigureResult.asStateFlow()

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
                    delay(1000)
                    Log.d("BlufiManager", "Pidiendo detalles del dispositivo...")
                    postCustomData("10:".toByteArray())
                }
            } else {
                Log.e("BlufiManager", "Error en negociación de seguridad: $status")
            }
        }

        override fun onPostConfigureParams(client: BlufiClient?, status: Int) {
            if (status == STATUS_SUCCESS) {
                Log.i("BlufiManager", "Wi-Fi configurado vía BLUFI correctamente")
                _wifiConfigureResult.value = WifiConfigureResult.Success
                scope.launch {
                    delay(1500)
                    client?.requestDeviceStatus()
                }
            } else {
                Log.e("BlufiManager", "Error al configurar Wi-Fi vía BLUFI: $status")
                _wifiConfigureResult.value = WifiConfigureResult.Error(status)
            }
        }

        override fun onDeviceStatusResponse(client: BlufiClient?, status: Int, response: BlufiStatusResponse?) {
            if (status == STATUS_SUCCESS) _deviceStatus.value = response
        }

        override fun onReceiveCustomData(client: BlufiClient?, status: Int, data: ByteArray?) {
            if (status == STATUS_SUCCESS && data != null) {
                val response = String(data).trim()
                Log.i("BlufiManager", "DATO RECIBIDO RAW: '$response'")
                _lastRawResponse.value = response
                
                when {
                    response.startsWith("UID:", ignoreCase = true) ->
                        _deviceInfo.value = parseDeviceInfo(response)
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

        override fun onDeviceScanResult(
            client: BlufiClient?,
            status: Int,
            results: List<blufi.espressif.response.BlufiScanResult>?
        ) {
            _isWifiScanning.value = false
            if (status == STATUS_SUCCESS && results != null) {
                _wifiNetworks.value = results.filter { it.ssid?.isNotBlank() == true }
                Log.i("BlufiManager", "Redes encontradas: ${results.size}")
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
        _deviceInfo.value = null
        _lastRawResponse.value = null
        _wifiNetworks.value = emptyList()
        _isWifiScanning.value = false
        _wifiConfigureResult.value = null
    }

    private fun parseDeviceInfo(raw: String): DeviceInfo {
        var uid: String? = null
        var mac: String? = null
        var mcu: String? = null
        var radar: String? = null
        var wifiStatus: String? = null
        var ssid: String? = null
        var pwd: String? = null
        var server: String? = null

        raw.lines().forEach { line ->
            val l = line.trim()
            when {
                l.startsWith("UID:", ignoreCase = true) -> {
                    // Format: "UID:AD8A613B9E3F, MAC:EC-DA-3B-65-EA-28"
                    val rest = l.substringAfter(":")
                    if (rest.contains("MAC:", ignoreCase = true)) {
                        uid = rest.substringBefore(",").trim()
                        mac = rest.substringAfter("MAC:").trim()
                    } else {
                        uid = rest.trimEnd(',').trim()
                    }
                }
                l.startsWith("MAC:", ignoreCase = true) ->
                    mac = l.substringAfter(":").trim()
                l.startsWith("MCU:", ignoreCase = true) ->
                    mcu = l.substringAfter(":").trim()
                l.startsWith("Radar:", ignoreCase = true) ->
                    radar = l.substringAfter(":").trim()
                l.startsWith("WiFi:", ignoreCase = true) -> {
                    val rest = l.substringAfter(":").trim()
                    wifiStatus = rest.substringBefore(",").trim()
                    if (rest.contains("ssid:", ignoreCase = true))
                        ssid = rest.substringAfter("ssid:").substringBefore(",").trim()
                    if (rest.contains("pwd:", ignoreCase = true))
                        pwd = rest.substringAfter("pwd:").substringBefore(",").trim()
                }
                l.startsWith("Server:", ignoreCase = true) ->
                    server = l.substringAfter(":").trim()
            }
        }

        return DeviceInfo(uid, mac, mcu, radar, wifiStatus, ssid, pwd, server)
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
    fun requestDeviceInfo() = blufiClient?.postCustomData("10:".toByteArray())
    fun requestWifiScan() {
        _isWifiScanning.value = true
        _wifiNetworks.value = emptyList()
        blufiClient?.requestDeviceWifiScan()
    }
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

sealed class WifiConfigureResult {
    object Success : WifiConfigureResult()
    data class Error(val code: Int) : WifiConfigureResult()
}
