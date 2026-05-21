package com.nextthing.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.isActive as coroutineIsActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var color: Color,
    var alpha: Float,
    var life: Float
)

@Composable
fun ParticleExplosionEffect(
    isActive: Boolean,
    originX: Float,
    originY: Float,
    baseColor: Color,
    particleCount: Int = 200,
    durationMs: Long = 1400L,
    onFinished: () -> Unit
) {
    if (!isActive) return

    val particles = remember {
        mutableStateListOf<Particle>().apply {
            for (i in 0 until particleCount) {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val speed = 6f + Random.nextFloat() * 22f
                val r = 1.5f + Random.nextFloat() * 5f
                val brightnessShift = 0.7f + Random.nextFloat() * 0.6f
                val particleColor = Color(
                    red = (baseColor.red * brightnessShift).coerceIn(0f, 1f),
                    green = (baseColor.green * brightnessShift).coerceIn(0f, 1f),
                    blue = (baseColor.blue * brightnessShift).coerceIn(0f, 1f),
                    alpha = 1f
                )
                add(
                    Particle(
                        x = originX,
                        y = originY,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        radius = r,
                        color = particleColor,
                        alpha = 1f,
                        life = 1f
                    )
                )
            }
        }
    }

    val frameCount = remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val startTime = withFrameMillis { it }
        while (coroutineIsActive) {
            val currentTime = withFrameMillis { it }
            val elapsed = currentTime - startTime
            val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)

            if (progress >= 1f) {
                onFinished()
                break
            }

            val gravity = 0.06f
            val friction = 0.96f

            for (i in particles.indices) {
                val p = particles[i]
                particles[i] = p.copy(
                    x = p.x + p.vx,
                    y = p.y + p.vy + gravity,
                    vx = p.vx * friction,
                    vy = p.vy + gravity,
                    life = 1f - progress,
                    alpha = ((1f - progress) * 1.2f).coerceIn(0f, 1f),
                    radius = p.radius * 0.993f
                )
            }
            frameCount.intValue++
        }
    }

    // Force recomposition on each frame
    frameCount.intValue

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { clip = false }
    ) {
        for (p in particles) {
            if (p.alpha > 0.01f) {
                drawCircle(
                    color = p.color.copy(alpha = p.alpha),
                    radius = p.radius,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}
