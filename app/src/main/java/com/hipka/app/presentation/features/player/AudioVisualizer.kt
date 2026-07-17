package com.hipka.app.presentation.features.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

private const val BAR_COUNT = 28

/**
 * اکولایزر انیمیشنی رسم‌شده با Canvas (بدون GIF/Lottie). این یک ویژوالایزر واقعی صوتی نیست —
 * الگویی رویه‌ای از چند موج سینوسی هم‌پوشان است که در حین پخش حرکت می‌کند و هنگام توقف آرام می‌شود.
 */
@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizerTime")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "visualizerPhase"
    )

    val energy by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.08f,
        animationSpec = tween(durationMillis = 400),
        label = "visualizerEnergy"
    )

    val barColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        drawEqualizerBars(time = time, energy = energy, color = barColor)
    }
}

private fun DrawScope.drawEqualizerBars(time: Float, energy: Float, color: Color) {
    val barWidth = size.width / (BAR_COUNT * 1.6f)
    val gap = barWidth * 0.6f
    val maxBarHeight = size.height
    val centerY = size.height

    for (i in 0 until BAR_COUNT) {
        // ترکیب دو موج سینوسی با فرکانس/فاز متفاوت برای هر میله تا الگو طبیعی‌تر به نظر برسد
        val phase = i * 0.45f
        val wave1 = sin(time * 1.3f + phase)
        val wave2 = sin(time * 2.1f + phase * 1.7f) * 0.5f
        val normalized = ((wave1 + wave2 + 1.5f) / 3f).coerceIn(0.08f, 1f)

        val barHeight = maxBarHeight * normalized * energy
        val x = i * (barWidth + gap)

        drawRoundRect(
            color = color,
            topLeft = Offset(x, centerY - barHeight),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }
}
