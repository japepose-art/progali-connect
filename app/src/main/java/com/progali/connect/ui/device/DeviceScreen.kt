package com.progali.connect.ui.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.progali.connect.data.ble.BlufiConnectionState
import com.progali.connect.data.ble.WifiConfigureResult
import blufi.espressif.params.BlufiParameter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDetails: () -> Unit = {}
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val wifiNetworks by viewModel.wifiNetworks.collectAsState()
    val isWifiScanning by viewModel.isWifiScanning.collectAsState()
    val wifiConfigureResult by viewModel.wifiConfigureResult.collectAsState()
    val isApplyingAll by viewModel.isApplyingAll.collectAsState()

    var wifiSsid by remember { mutableStateOf("") }
    var wifiPass by remember { mutableStateOf("") }
    var serverDomain by remember { mutableStateOf("") }
    var serverPort by remember { mutableStateOf("") }
    var ssidDropdownExpanded by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        focusManager.clearFocus()
    }

    // Abrir desplegable automáticamente cuando llegan resultados
    LaunchedEffect(wifiNetworks) {
        if (wifiNetworks.isNotEmpty()) ssidDropdownExpanded = true
    }

    LaunchedEffect(connectionState) {
        if (connectionState is BlufiConnectionState.Disconnected) {
            onNavigateBack()
        }
    }

    LaunchedEffect(wifiConfigureResult) {
        when (wifiConfigureResult) {
            is WifiConfigureResult.Success ->
                snackbarHostState.showSnackbar("✓ Credenciales Wi-Fi recibidas por el dispositivo")
            is WifiConfigureResult.Error ->
                snackbarHostState.showSnackbar("✗ Error al enviar Wi-Fi (código ${(wifiConfigureResult as WifiConfigureResult.Error).code})")
            null -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.disconnect() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta de Información
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Información del Dispositivo", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Estado BT: $connectionState")

                    if (deviceStatus != null) {
                        val status = deviceStatus!!
                        val modeText = when (status.opMode) {
                            BlufiParameter.OP_MODE_STA -> "Station (Cliente)"
                            BlufiParameter.OP_MODE_SOFTAP -> "SoftAP"
                            else -> "Modo ${status.opMode}"
                        }
                        val wifiStatusText = if (status.staConnectionStatus == 0) "Conectado" else "Desconectado"

                        Text("Modo Op: $modeText")
                        Text("Estado Wi-Fi: $wifiStatusText")

                        if (!status.staSSID.isNullOrBlank()) {
                            Text("SSID Actual: ${status.staSSID}")
                        }
                    } else {
                        Text("Esperando datos del equipo...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            OutlinedButton(
                onClick = onNavigateToDetails,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Detalles")
            }

            Text("Configuración de Servidor", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = serverDomain,
                onValueChange = { serverDomain = it },
                label = { Text("Dominio o IP") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = serverPort,
                onValueChange = { serverPort = it },
                label = { Text("Puerto") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.configureServer(serverDomain, serverPort)
                    focusManager.clearFocus()
                    scope.launch {
                        snackbarHostState.showSnackbar("✓ Configuración de servidor enviada al dispositivo")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isApplyingAll
            ) {
                Text("Actualizar Servidor")
            }

            HorizontalDivider()

            Text("Configuración Wi-Fi (2.4GHz)", style = MaterialTheme.typography.titleLarge)

            // Campo SSID con botón de escaneo y desplegable
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = wifiSsid,
                    onValueChange = { wifiSsid = it },
                    label = { Text("Nombre de Red (SSID)") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.requestWifiScan()
                            },
                            enabled = !isWifiScanning
                        ) {
                            if (isWifiScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Escanear redes")
                            }
                        }
                    }
                )
                DropdownMenu(
                    expanded = ssidDropdownExpanded,
                    onDismissRequest = { ssidDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    wifiNetworks.forEach { network ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(network.ssid ?: "")
                                    Text(
                                        "RSSI: ${network.rssi} dBm",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                wifiSsid = network.ssid ?: ""
                                ssidDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = wifiPass,
                onValueChange = { wifiPass = it },
                label = { Text("Contraseña") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.configureWifi(wifiSsid, wifiPass)
                    focusManager.clearFocus()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isApplyingAll
            ) {
                Text("Aplicar Wi-Fi")
            }

            HorizontalDivider()

            // Botón principal de provisioning completo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Provisioning Completo",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Aplica Wi-Fi + Servidor y reinicia el dispositivo en secuencia automática.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.applyAllAndReboot(wifiSsid, wifiPass, serverDomain, serverPort)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isApplyingAll && wifiSsid.isNotBlank() && serverDomain.isNotBlank() && serverPort.isNotBlank()
                    ) {
                        if (isApplyingAll) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text("Aplicando configuración...")
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Text("Completar y Reiniciar")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.rebootDevice() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isApplyingAll,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Solo Reiniciar Dispositivo")
            }
        }
    }
}
