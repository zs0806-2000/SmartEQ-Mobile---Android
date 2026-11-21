package com.smarteq.pro.ui.equalizer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smarteq.pro.R
import com.smarteq.pro.data.model.AudioOutputDevice
import com.smarteq.pro.data.model.AudioOutputDeviceType
import com.smarteq.pro.data.model.BandCount
import com.smarteq.pro.data.model.BandFrequencies
import com.smarteq.pro.data.model.EqEngineMode
import com.smarteq.pro.ui.components.EqBandFader
import com.smarteq.pro.ui.components.EqKnob
import com.smarteq.pro.ui.theme.bandColor

@Composable
fun EqualizerScreen(
    padding: PaddingValues,
    onNavigateToHelp: (String) -> Unit,
    viewModel: EqualizerViewModel,
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Active Audio Output Device & Low-Latency Card
        AudioOutputDeviceCard(
            device = state.outputDevice,
            lowLatencyActive = state.isLowLatencyActive,
            onToggleLowLatency = viewModel::setLowLatencyMode,
        )

        // Engine Status & Global/Per-App Switcher
        EngineStatusCard(
            engineEnabled = state.engineEnabled,
            mode = state.engineMode,
            status = state.engineStatus,
            onToggleEnabled = viewModel::setEngineEnabled,
            onSetMode = viewModel::setEngineMode,
            onHelp = { onNavigateToHelp("eq_mode") },
        )

        // 15/10/5-band Equalizer Sliders
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.testTag("equalizer_bands_card"),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.eq_title), style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        listOf(BandCount.FIVE, BandCount.TEN, BandCount.FIFTEEN).forEach { count ->
                            FilterChip(
                                selected = state.eq.bandCount == count,
                                onClick = { viewModel.setBandCount(count) },
                                label = { Text(count.count.toString()) },
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .testTag("band_count_${count.count}"),
                            )
                        }
                        IconButton(
                            onClick = { viewModel.resetToFlat() },
                            modifier = Modifier.testTag("reset_flat_button"),
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = stringResource(R.string.eq_reset))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                val labels = BandFrequencies.labelsForCount(state.eq.bandCount)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.eq.bandGainsDb.forEachIndexed { i, gainDb ->
                        EqBandFader(
                            value = gainDb,
                            min = -12f,
                            max = 12f,
                            label = labels.getOrElse(i) { "" },
                            color = bandColor(i, state.eq.bandCount.count),
                            onChange = { viewModel.setBandGain(i, it) },
                            modifier = Modifier.testTag("fader_band_$i"),
                        )
                    }
                }
            }
        }

        // Dynamics Processing Card
        DynamicsCard(
            dynamics = state.eq.dynamics,
            outputTrimDb = state.eq.outputTrimDb,
            onDynamicsChange = viewModel::setDynamics,
            onOutputTrimChange = viewModel::setOutputTrim,
            onHelp = { onNavigateToHelp("dynamics") },
        )
    }
}

@Composable
private fun AudioOutputDeviceCard(
    device: AudioOutputDevice,
    lowLatencyActive: Boolean,
    onToggleLowLatency: (Boolean) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (device.type) {
                        AudioOutputDeviceType.BLUETOOTH_A2DP,
                        AudioOutputDeviceType.BLUETOOTH_LE -> Icons.Default.Headphones
                        AudioOutputDeviceType.USB_AUDIO -> Icons.Default.Usb
                        AudioOutputDeviceType.WIRED_HEADPHONES -> Icons.Default.Headphones
                        else -> Icons.Default.Speaker
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = if (device.isBluetooth) "Bluetooth Device Connected" else "Audio Output Sink Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Offline badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Offline Ready",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Low-Latency Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (lowLatencyActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Low-Latency Bluetooth Mode",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = if (lowLatencyActive) "Faster, coarser processing. Doesn't change Bluetooth's own delay." else "Standard processing (best sound)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = lowLatencyActive,
                    onCheckedChange = onToggleLowLatency,
                    modifier = Modifier.testTag("low_latency_switch"),
                )
            }
        }
    }
}

@Composable
private fun EngineStatusCard(
    engineEnabled: Boolean,
    mode: EqEngineMode,
    status: com.smarteq.pro.data.model.EqEngineStatus,
    onToggleEnabled: (Boolean) -> Unit,
    onSetMode: (EqEngineMode) -> Unit,
    onHelp: () -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        if (engineEnabled) stringResource(R.string.eq_engine_on) else stringResource(R.string.eq_engine_off),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        statusText(status),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = engineEnabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("engine_enabled_switch"),
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mode:", style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = onHelp) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Help: mode", modifier = Modifier.size(16.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                FilterChip(
                    selected = mode == EqEngineMode.UNIVERSAL_GLOBAL,
                    onClick = { onSetMode(EqEngineMode.UNIVERSAL_GLOBAL) },
                    label = { Text(stringResource(R.string.eq_mode_universal)) },
                    modifier = Modifier.testTag("mode_chip_universal"),
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = mode == EqEngineMode.DEFAULT_PER_SESSION,
                    onClick = { onSetMode(EqEngineMode.DEFAULT_PER_SESSION) },
                    label = { Text(stringResource(R.string.eq_mode_default)) },
                    modifier = Modifier.testTag("mode_chip_default"),
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = mode == EqEngineMode.LEGACY_GLOBAL,
                    onClick = { onSetMode(EqEngineMode.LEGACY_GLOBAL) },
                    label = { Text(stringResource(R.string.eq_mode_legacy)) },
                    modifier = Modifier.testTag("mode_chip_legacy"),
                )
            }
        }
    }
}

@Composable
private fun statusText(status: com.smarteq.pro.data.model.EqEngineStatus): String {
    val activeLabel = status.activeSessions.firstOrNull()?.sourceAppLabel
    return when {
        status.mode == EqEngineMode.UNIVERSAL_GLOBAL && status.isRunning ->
            if (activeLabel != null) stringResource(R.string.eq_status_processing, activeLabel)
            else stringResource(R.string.eq_status_universal_active)
        status.mode == EqEngineMode.LEGACY_GLOBAL && status.isRunning ->
            stringResource(R.string.eq_status_legacy_active)
        activeLabel != null -> stringResource(R.string.eq_status_processing, activeLabel)
        else -> stringResource(R.string.eq_status_idle)
    }
}

@Composable
private fun DynamicsCard(
    dynamics: com.smarteq.pro.data.model.DynamicsSettings,
    outputTrimDb: Float,
    onDynamicsChange: ((com.smarteq.pro.data.model.DynamicsSettings) -> com.smarteq.pro.data.model.DynamicsSettings) -> Unit,
    onOutputTrimChange: (Float) -> Unit,
    onHelp: () -> Unit,
) {
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dynamics & Tone", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onHelp) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Help: dynamics", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                items(1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        EqKnob(
                            value = dynamics.bassDb,
                            min = -12f,
                            max = 12f,
                            label = "Bass",
                            unit = "dB",
                            onChange = { v -> onDynamicsChange { d -> d.copy(bassDb = v) } },
                        )
                        EqKnob(
                            value = dynamics.trebleDb,
                            min = -12f,
                            max = 12f,
                            label = "Treble",
                            unit = "dB",
                            onChange = { v -> onDynamicsChange { d -> d.copy(trebleDb = v) } },
                        )
                        EqKnob(
                            value = dynamics.compThresholdDb,
                            min = -60f,
                            max = 0f,
                            label = "Comp Thr",
                            unit = "dB",
                            onChange = { v -> onDynamicsChange { d -> d.copy(compThresholdDb = v) } },
                        )
                        EqKnob(
                            value = dynamics.compRatio,
                            min = 1f,
                            max = 20f,
                            label = "Comp Rat",
                            unit = ":1",
                            onChange = { v -> onDynamicsChange { d -> d.copy(compRatio = v) } },
                        )
                        EqKnob(
                            value = dynamics.compAttackSec,
                            min = 0f,
                            max = 1f,
                            label = "Attack",
                            unit = "s",
                            onChange = { v -> onDynamicsChange { d -> d.copy(compAttackSec = v) } },
                        )
                        EqKnob(
                            value = dynamics.compReleaseSec,
                            min = 0f,
                            max = 1f,
                            label = "Release",
                            unit = "s",
                            onChange = { v -> onDynamicsChange { d -> d.copy(compReleaseSec = v) } },
                        )
                        EqKnob(
                            value = dynamics.compKneeDb,
                            min = 0f,
                            max = 40f,
                            label = "Knee",
                            unit = "dB",
                            onChange = { v -> onDynamicsChange { d -> d.copy(compKneeDb = v) } },
                        )
                        EqKnob(
                            value = outputTrimDb,
                            min = -12f,
                            max = 0f,
                            label = "Trim",
                            unit = "dB",
                            onChange = onOutputTrimChange,
                        )
                    }
                }
            }
        }
    }
}
