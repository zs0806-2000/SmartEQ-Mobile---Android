package com.smarteq.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * A vertical fader for a single EQ band. Drag anywhere on the track;
 * one-line semantics description makes it usable with TalkBack.
 * Mirrors VerticalSlider in the original web app's App.tsx.
 */
@Composable
fun EqBandFader(
    value: Float,
    min: Float,
    max: Float,
    label: String,
    color: Color,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackHeightDp = 176.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = if (value > 0) "+${formatDb(value)}" else formatDb(value),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
        Box(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp)
                .height(trackHeightDp)
                .width(28.dp)
                .semantics { contentDescription = "$label band, ${formatDb(value)} decibels" }
                .pointerInput(min, max) {
                    detectVerticalDragGestures { change, _ ->
                        val trackPx = size.height.toFloat()
                        val relativeY = (change.position.y).coerceIn(0f, trackPx)
                        val percentFromBottom = 1f - (relativeY / trackPx)
                        val newValue = min + percentFromBottom * (max - min)
                        onChange(newValue.coerceIn(min, max).roundToTenth())
                    }
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Track
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(trackHeightDp)
                    .clip(RoundedCornerShape(3.dp))
                    .align(Alignment.Center)
                    .then(Modifier)
                    .background(Color.Black.copy(alpha = 0.35f))
            )
            val percent = ((value - min) / (max - min)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(trackHeightDp * percent)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
            Box(
                modifier = Modifier
                    .padding(bottom = (trackHeightDp * percent) - 6.dp)
                    .width(24.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * A rotary knob control for scalar dynamics parameters. Mirrors Knob in
 * the original web app; drag vertically to adjust, matching the same
 * interaction model as most native Android EQ apps use for knobs.
 */
@Composable
fun EqKnob(
    value: Float,
    min: Float,
    max: Float,
    label: String,
    unit: String = "",
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val percent = ((value - min) / (max - min)).coerceIn(0f, 1f)
    val angleDeg = -135f + percent * 270f

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
                .semantics { contentDescription = "$label knob, ${formatKnobValue(value)}$unit" }
                .pointerInput(min, max) {
                    detectDragGestures { change, dragAmount ->
                        val sensitivity = (max - min) / 200f
                        val newValue = value - dragAmount.y * sensitivity
                        onChange(newValue.coerceIn(min, max))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .rotate(angleDeg),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(width = 4.dp, height = 10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "${formatKnobValue(value)}$unit",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun formatDb(value: Float): String {
    val rounded = (value * 10).roundToInt() / 10f
    return if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString() else rounded.toString()
}

private fun formatKnobValue(value: Float): String {
    val rounded = (value * 1000).roundToInt() / 1000f
    return if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString() else rounded.toString()
}

private fun Float.roundToTenth(): Float = (this * 10).roundToInt() / 10f
