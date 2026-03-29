package com.progali.connect.data.ble

data class DeviceInfo(
    val uid: String? = null,
    val mac: String? = null,
    val mcu: String? = null,
    val radar: String? = null,
    val wifiStatus: String? = null,
    val ssid: String? = null,
    val pwd: String? = null,
    val server: String? = null
)
