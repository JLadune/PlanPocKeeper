package com.example.planpockeeper.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.example.planpockeeper.ui.theme.Fond
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

data class Bubble(
    var position: Offset,
    var velocity: Offset,
    val radius: Float,
    val color: Color
)

@Composable
fun BubblesBackground() {
    val bubbles = remember { mutableStateListOf<Bubble>() }

    val colors = listOf(
        Color(0xFFD48A98),
        Color(0xFFB15E6C),
        Color(0xFF9FD6CE)
    )

    val spawnRate = 0.5f
    val acceleration = Offset(0f, -500f)
    val friction = 0.2f

    var canvasSize by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        var lastTime = 0L

        while (true) {
            withFrameNanos { now ->
                if (lastTime == 0L) lastTime = now
                val dt = (now - lastTime) / 1_000_000_000f
                lastTime = now

                val width = canvasSize.x
                val height = canvasSize.y

                if (width == 0f || height == 0f) return@withFrameNanos

                val spawnPoint = Offset(width / 2f, height + 50f)

                if (Random.nextFloat() < spawnRate) {
                    val angle = Random.nextFloat() * (PI/2).toFloat() + (PI/4).toFloat()
                    val speed = Random.nextFloat() * 300f + 800f

                    val velocity = Offset(
                        cos(angle) * speed,
                        -abs(sin(angle) * speed)
                    )

                    bubbles.add(
                        Bubble(
                            position = spawnPoint,
                            velocity = velocity,
                            radius = Random.nextFloat() * 50f + 30f,
                            color = colors.random()
                        )
                    )
                }

                bubbles.forEach { bubble ->
                    bubble.velocity += acceleration * dt
                    bubble.velocity -= bubble.velocity * friction * dt
                    bubble.position += bubble.velocity * dt
                }

                bubbles.removeAll { it.position.y < -100f }
            }
        }
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        canvasSize = Offset(size.width, size.height)

        bubbles.forEach { bubble ->

            // Main translucent body
            drawCircle(
                color = bubble.color.copy(alpha = 0.25f),
                radius = bubble.radius,
                center = bubble.position
            )

            // Soft highlight (top-left)
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = bubble.radius * 0.35f,
                center = bubble.position + Offset(
                    x = -bubble.radius * 0.3f,
                    y = -bubble.radius * 0.3f
                )
            )

            // Specular rim (thin bright edge)
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = bubble.radius,
                center = bubble.position,
                style = Stroke(width = bubble.radius * 0.12f)
            )

            // Slight color shift for iridescence
            drawCircle(
                color = bubble.color.copy(alpha = 0.15f)
                    .copy(
                        red = (bubble.color.red * 1.1f).coerceAtMost(1f),
                        blue = (bubble.color.blue * 1.2f).coerceAtMost(1f)
                    ),
                radius = bubble.radius * 0.9f,
                center = bubble.position
            )
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3000)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Fond),
        contentAlignment = Alignment.Center
    ) {
        BubblesBackground()

        val context = LocalContext.current
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/logo.svg")
                .decoderFactory(SvgDecoder.Factory())
                .build(),
            contentDescription = "Logo",
            modifier = Modifier.size(200.dp)
        )
    }
}