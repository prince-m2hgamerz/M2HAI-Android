package com.m2h.m2haichatbot.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.m2h.m2haichatbot.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme
    val logoScale = remember { Animatable(0.76f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoRotation = remember { Animatable(-8f) }
    val textOffset = remember { Animatable(18f) }
    val textAlpha = remember { Animatable(0f) }
    val infinite = rememberInfiniteTransition(label = "splash-motion")
    val orbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )
    val glow by infinite.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val background = if (dark) {
        listOf(Color(0xFF070707), Color(0xFF121210), Color(0xFF1A1512))
    } else {
        listOf(Color(0xFFFFFCF7), Color(0xFFF8F1E9), Color(0xFFFFE9DF))
    }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, animationSpec = tween(260))
        logoScale.animateTo(1.08f, animationSpec = tween(520, easing = FastOutSlowInEasing))
        logoRotation.animateTo(0f, animationSpec = tween(620, easing = FastOutSlowInEasing))
        logoScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        textAlpha.animateTo(1f, animationSpec = tween(260))
        textOffset.animateTo(0f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        delay(560)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(background)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorScheme.primary.copy(alpha = if (dark) 0.24f else 0.18f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.62f
                ),
                radius = size.minDimension * 0.62f,
                center = center
            )
        }

        Box(
            modifier = Modifier
                .size(210.dp)
                .scale(glow)
                .clip(CircleShape)
                .background(colorScheme.primary.copy(alpha = if (dark) 0.08f else 0.10f))
        )

        Canvas(modifier = Modifier.size(224.dp).rotate(orbit)) {
            drawArc(
                color = colorScheme.primary.copy(alpha = 0.65f),
                startAngle = 16f,
                sweepAngle = 86f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = colorScheme.tertiary.copy(alpha = 0.48f),
                startAngle = 184f,
                sweepAngle = 54f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(logoAlpha.value)
        ) {
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                colorScheme.surface.copy(alpha = if (dark) 0.92f else 0.98f),
                                colorScheme.surfaceVariant.copy(alpha = if (dark) 0.72f else 0.86f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "M2HAI Logo",
                    modifier = Modifier
                        .size(118.dp)
                        .rotate(logoRotation.value)
                        .scale(logoScale.value)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = textOffset.value.dp)
                    .alpha(textAlpha.value)
            ) {
                Text(
                    text = "M2HAI",
                    color = colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AI workspace",
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
