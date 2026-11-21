package com.smarteq.pro.ui.autotune

import com.smarteq.pro.ui.components.LiquidCard
import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.smarteq.pro.audio.adaptive.AdaptivePhase
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Controls for adaptive per-track tuning. Each permission is asked for only when the user
 * switches on the option that needs it, with the reason shown right next to the switch.
 */
@Composable
fun AdaptiveTuningCard(
    state: AdaptiveUiState,
    onEnabled: (Boolean) -> Unit,
    onUseTrackInfo: (Boolean) -> Unit,
    onListen: (Boolean) -> Unit,
    onStrength: (Float) -> Unit,
    onLoudness: (Boolean) -> Unit,
    onResetLearning: () -> Unit,
    onHelp: () -> Unit,
) {
    val context = LocalContext.current
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onListen(granted)
    }

    LiquidCard(shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Adapt to every track", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onHelp) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Help: adaptive tuning", modifier = Modifier.size(18.dp))
                }
                Switch(checked = state.enabled, onCheckedChange = onEnabled)
            }
            Text(
                "Re-tunes the sound for each song, on top of your own EQ. Your sliders are never changed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!state.enabled) return@Column

            Spacer(Modifier.height(12.dp))
            OptionRow(
                title = "Use track info",
                body = "Notices when a new song starts in any app, including Spotify — no login needed. " +
                    "Android calls this notification access; SmartEQ Pro reads only the song title, artist and app name.",
                checked = state.useTrackInfo && state.notificationAccessGranted,
                onChange = { on ->
                    if (on && !state.notificationAccessGranted) {
                        // Android only allows this to be granted on its own settings screen.
                        runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    }
                    onUseTrackInfo(on)
                },
            )
            OptionRow(
                title = "Analyse the sound",
                body = "Measures each song's tonal balance and gently corrects it. Android files this under " +
                    "Microphone; the microphone is never turned on and no audio is recorded or saved.",
                checked = state.listen && state.micGranted,
                onChange = { on ->
                    if (on && !state.micGranted) micLauncher.launch(Manifest.permission.RECORD_AUDIO) else onListen(on)
                },
            )

            Spacer(Modifier.height(8.dp))
            var strength by remember(state.strength) { mutableFloatStateOf(state.strength) }
            Text("Strength: ${(strength * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = strength,
                onValueChange = { strength = it },
                onValueChangeFinished = { onStrength(strength) },
                valueRange = 0f..1f,
                steps = 9,
                modifier = Modifier.semantics { contentDescription = "Adaptive tuning strength" },
            )
            OptionRow(
                title = "Even out volume between songs",
                body = "Experimental. Turns loud songs down (up to 6 dB) and quiet ones up a little (up to 3 dB).",
                checked = state.loudness,
                onChange = onLoudness,
                enabledWhen = state.listen && state.micGranted,
            )

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            StatusBlock(state)
            TextButton(onClick = onResetLearning) { Text("Forget what it learned") }
        }
    }
}

@Composable
private fun OptionRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit, enabledWhen: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked && enabledWhen, onCheckedChange = onChange, enabled = enabledWhen)
    }
}

@Composable
private fun StatusBlock(state: AdaptiveUiState) {
    val s = state.status
    val phaseText = when (s.phase) {
        AdaptivePhase.OFF -> "Off"
        AdaptivePhase.WAITING_FOR_MUSIC -> "Waiting for music"
        AdaptivePhase.ANALYZING -> "Listening to this song…"
        AdaptivePhase.APPLIED -> "Tuned for this song"
        AdaptivePhase.TRACK_INFO_ONLY -> "Using track info only"
        AdaptivePhase.UNAVAILABLE -> "Not running"
    }
    Text(phaseText, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    s.trackLabel?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    if (s.outputLabel.isNotBlank()) {
        Text("Output: ${s.outputLabel} · ${s.tracksLearned} songs learned", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    s.note?.let {
        Spacer(Modifier.height(4.dp))
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(8.dp))
    CorrectionBars(s.correction15)
    if (s.loudnessGainDb != 0f) {
        Text("Volume match: ${"%+.1f".format(s.loudnessGainDb)} dB", style = MaterialTheme.typography.labelSmall)
    }
}

/** Small bar view of the adaptive layer: bars up are boosts, down are cuts (±4 dB scale). */
@Composable
private fun CorrectionBars(values: List<Float>) {
    val pos = MaterialTheme.colorScheme.primary
    val neg = MaterialTheme.colorScheme.tertiary
    val axis = MaterialTheme.colorScheme.outlineVariant
    val description = values.joinToString { "%+.1f".format(it) }
    Canvas(
        Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = "Adaptive correction per band, in decibels: $description" }
    ) {
        val n = values.size.coerceAtLeast(1)
        val slot = size.width / n
        val mid = size.height / 2f
        drawLine(axis, Offset(0f, mid), Offset(size.width, mid), strokeWidth = 1f)
        values.forEachIndexed { i, v ->
            val h = (abs(v) / 4f).coerceAtMost(1f) * (mid - 2f)
            val top = if (v >= 0) mid - h else mid
            drawRect(
                color = if (v >= 0) pos else neg,
                topLeft = Offset(i * slot + slot * 0.2f, top),
                size = Size(slot * 0.6f, h.coerceAtLeast(1f)),
            )
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("25 Hz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("1 kHz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("16 kHz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
