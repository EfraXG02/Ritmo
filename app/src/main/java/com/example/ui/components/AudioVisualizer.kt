package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet

@Composable
fun AudioVisualizer(
    levels: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 2.dp,
    height: Dp = 32.dp
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(NeonPink, NeonViolet, NeonCyan)
    )

    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        levels.forEach { level ->
            val animatedHeight by animateFloatAsState(
                targetValue = if (isPlaying) level.coerceIn(0.1f, 1.0f) else 0.12f,
                animationSpec = spring(stiffness = 800f),
                label = "bar_height"
            )

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(fraction = animatedHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(gradient)
            )
        }
    }
}
