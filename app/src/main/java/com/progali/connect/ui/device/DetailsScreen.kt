package com.progali.connect.ui.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.progali.connect.data.ble.DeviceInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    viewModel: DeviceViewModel,
    onNavigateBack: () -> Unit
) {
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val rawResponse by viewModel.lastRawResponse.collectAsState()
    var customCommand by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.requestDeviceInfo()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del Dispositivo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (deviceInfo == null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Sin respuesta del dispositivo.", style = MaterialTheme.typography.bodyMedium)

                        if (rawResponse != null) {
                            HorizontalDivider()
                            Text("Última respuesta recibida:", style = MaterialTheme.typography.labelMedium)
                            Text(rawResponse!!, style = MaterialTheme.typography.bodySmall)
                        }

                        HorizontalDivider()

                        Text("Probar comando manual:", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = customCommand,
                            onValueChange = { customCommand = it },
                            label = { Text("Comando (ej: DETAILS, INFO, STATUS)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.requestDeviceInfo() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reintentar")
                            }
                            Button(
                                onClick = {
                                    if (customCommand.isNotBlank()) {
                                        viewModel.sendRawCommand(customCommand)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = customCommand.isNotBlank()
                            ) {
                                Text("Enviar")
                            }
                        }
                    }
                }
            } else {
                InfoCard(info = deviceInfo!!)
            }
        }
    }
}

@Composable
private fun InfoCard(info: DeviceInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Identificación", style = MaterialTheme.typography.titleMedium)
            InfoRow("UID", info.uid)
            InfoRow("MAC", info.mac)

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text("Firmware", style = MaterialTheme.typography.titleMedium)
            InfoRow("MCU", info.mcu)
            InfoRow("Radar", info.radar)

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text("Red Wi-Fi", style = MaterialTheme.typography.titleMedium)
            InfoRow("Estado", info.wifiStatus)
            InfoRow("SSID", info.ssid)
            InfoRow("Contraseña", info.pwd)

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text("Servidor", style = MaterialTheme.typography.titleMedium)
            InfoRow("Dirección", info.server)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value ?: "—",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
