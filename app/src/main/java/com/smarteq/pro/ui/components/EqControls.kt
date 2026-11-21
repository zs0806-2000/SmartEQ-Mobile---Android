package com.smarteq.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import com.smarteq.pro.ui.theme.LocalPeach
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
 * A vertical fader for a single EQ band. Drag anywhere on the track, or use TalkBack's
 * adjust gestures (semantics progress range). The fill grows up or down from the 0 dB
 * centre line; the knob carries a soft shadow and a ring in the band's colour.
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
    val trackHeight = 176.dp
    val knob = 22.dp
    val currentValue by rememberUpdatedState(value)
    val t = LocalPeach.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = if (value > 0) "+${formatDb(value)}" else formatDb(value),
            style = MaterialTheme.typography.labelMedium,
            color = if (value == 0f) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Box(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .height(trackHeight + knob)
                .width(32.dp)
                .semantics {
                    contentDescription = "$label band"
                    stateDescription = "${formatDb(value)} decibels"
                    progressBarRangeInfo = ProgressBarRangeInfo(value, min..max, steps = 0)
                    setProgress { target ->
                        onChange(target.coerceIn(min, max).roundToTenth()); true
                    }
                }
                .pointerInput(min, max) {
                    detectVerticalDragGestures { change, _ ->
                        val pad = knob.toPx() / 2f
                        val trackPx = size.height - 2 * pad
                        val y = (change.position.y - pad).coerceIn(0f, trackPx)
                        val newValue = min + (1f - y / trackPx) * (max - min)
                        onChange(newValue.coerceIn(min, max).roundToTenth())
                    }
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            val frac = ((currentValue - min) / (max - min)).coerceIn(0f, 1f)
            val knobTop = trackHeight * (1f - frac)          // knob centre offset from track top
            val centre = trackHeight / 2
            val fillTop = if (knobTop < centre) knobTop else centre
            val fillHeight = if (knobTop < centre) centre - knobTop else knobTop - centre
            // Track
            Box(
                Modifier
                    .padding(top = knob / 2)
                    .width(6.dp)
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(t.soft)
            )
            // 0 dB line
            Box(
                Modifier
                    .padding(top = knob / 2 + centre)
                    .width(14.dp)
                    .height(1.dp)
                    .background(t.line)
            )
            // Fill from the centre line, with a faint glow in the band colour
            if (fillHeight > 0.dp) {
                Box(
                    Modifier
                        .padding(top = knob / 2 + fillTop)
                        .width(6.dp)
                        .height(fillHeight)
                        .shadow(6.dp, CircleShape, ambientColor = color, spotColor = color)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            // Knob
            Box(
                Modifier
                    .padding(top = knobTop)
                    .size(knob)
                    .shadow(5.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.4f))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground)
                    .border(3.dp, color, CircleShape)
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
    val latestValue by rememberUpdatedState(value)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.35f))
                .clip(CircleShape)
                .background(LocalPeach.current.soft)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
                .semantics { contentDescription = "$label knob, ${formatKnobValue(value)}$unit" }
                .pointerInput(min, max) {
                    detectDragGestures { change, dragAmount ->
                        val sensitivity = (max - min) / 200f
                        val newValue = latestValue - dragAmount.y * sensitivity
                        onChange(newValue.coerceIn(min, max))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(LocalPeach.current.card)
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
