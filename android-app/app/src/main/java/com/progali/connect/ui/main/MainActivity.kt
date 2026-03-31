package com.progali.connect.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.progali.connect.ui.device.DetailsScreen
import com.progali.connect.ui.device.DeviceScreen
import com.progali.connect.ui.device.DeviceViewModel
import com.progali.connect.ui.scan.ScanScreen
import com.progali.connect.ui.theme.ProgaliConnectTheme
import com.progali.connect.ui.welcome.WelcomeScreen
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

    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(
                onNavigateToScan = {
                    // Navegación simple: welcome permanece en el back stack
                    // → el botón atrás desde scan vuelve aquí
                    navController.navigate("scan")
                }
            )
        }
        composable("scan") {
            ScanScreen(
                onDeviceConnected = {
                    navController.navigate("device_config") {
                        popUpTo("scan") { inclusive = false }
                    }
                }
            )
        }
        composable("device_config") {
            DeviceScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { navController.navigate("device_details") }
            )
        }
        composable("device_details") {
            val parentEntry = remember(it) { navController.getBackStackEntry("device_config") }
            val viewModel: DeviceViewModel = hiltViewModel(parentEntry)
            DetailsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
