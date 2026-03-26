package com.progali.connect.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.progali.connect.ui.device.DeviceScreen
import com.progali.connect.ui.scan.ScanScreen
import com.progali.connect.ui.theme.ProgaliConnectTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProgaliConnectTheme {
                ProgaliAppNavigation()
            }
        }
    }
}

@Composable
fun ProgaliAppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "scan") {
        composable("scan") {
            ScanScreen(
                onDeviceConnected = {
                    navController.navigate("device_config") {
                        // Evitamos que el usuario pueda volver atrás al escaneo con el botón back físico
                        // si ya estamos en proceso de configuración
                        popUpTo("scan") { inclusive = false }
                    }
                }
            )
        }
        composable("device_config") {
            DeviceScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
