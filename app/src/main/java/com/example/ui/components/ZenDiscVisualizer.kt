package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.PastelRose
import com.example.ui.theme.SageLight
import com.example.ui.theme.SagePrimary
import com.example.ui.theme.SageSoft

@Composable
fun ZenDiscVisualizer(
    isPlaying: Boolean,
    isBuffering: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "disc_anim")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val activeScale = if (isPlaying) pulseScale else 1f
    val activeRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier
            .size(240.dp)
            .scale(activeScale),
        contentAlignment = Alignment.Center
    ) {
        // Subtle ambient ring glow
        Box(
            modifier = Modifier
                .size(236.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SageSoft.copy(alpha = 0.5f),
                            PastelRose.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main Disc with soft shadow
        Box(
            modifier = Modifier
                .size(200.dp)
                .shadow(elevation = 8.dp, shape = CircleShape, spotColor = SagePrimary.copy(alpha = 0.2f))
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // Grooves drawing
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(activeRotation)
            ) {
                val center = this.center
                val strokeColor = Color(0xFFF0ECE4)

                drawCircle(
                    color = strokeColor,
                    radius = size.minDimension * 0.42f,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = strokeColor,
                    radius = size.minDimension * 0.35f,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = strokeColor,
                    radius = size.minDimension * 0.28f,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Inner Center Label (Pastel Zen Badge)
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SageLight, PastelPeach)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.Radio,
                    contentDescription = "Visualizer Icon",
                    tint = SagePrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
