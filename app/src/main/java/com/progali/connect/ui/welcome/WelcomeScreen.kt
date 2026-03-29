package com.progali.connect.ui.welcome

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(onNavigateToScan: () -> Unit) {
    val context = LocalContext.current
    var showButton by remember { mutableStateOf(false) }

    // Leemos la duración real del GIF desde los assets
    val gifDurationMs = remember {
        try {
            context.assets.open("progali_animation.gif").use { stream ->
                @Suppress("DEPRECATION")
                android.graphics.Movie.decodeStream(stream)?.duration()?.toLong() ?: 3000L
            }
        } catch (e: Exception) {
            3000L
        }
    }

    // El botón aparece exactamente cuando termina el GIF
    LaunchedEffect(Unit) {
        delay(gifDurationMs)
        showButton = true
    }

    // Fade-in suave del botón al aparecer
    val buttonAlpha by animateFloatAsState(
        targetValue = if (showButton) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "buttonFade"
    )

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animación GIF — ocupa todo el espacio disponible, se reproduce una sola vez
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/progali_animation.gif")
                .setParameter("coil#repeat_count", 0, memoryCacheKey = null)
                .build(),
            imageLoader = imageLoader,
            contentDescription = "Progali animation",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Zona del botón: altura fija reservada para evitar saltos de layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .alpha(buttonAlpha),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onNavigateToScan,
                enabled = showButton,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                Text(
                    "Configurar dispositivo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
