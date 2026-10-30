package com.smarteq.pro.ui.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smarteq.pro.R
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
    viewModel: EqualizerViewModel = hiltViewModel(),
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
        EngineStatusCard(
            engineEnabled = state.engineEnabled,
            mode = state.engineMode,
            status = state.engineStatus,
            onToggleEnabled = viewModel::setEngineEnabled,
            onSetMode = viewModel::setEngineMode,
            onHelp = { onNavigateToHelp("eq_mode") },
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
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
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                        IconButton(onClick = { viewModel.resetToFlat() }) {
                            Icon(Icons.Default.RestartAlt, contentDescription = stringResource(R.string.eq_reset))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                val labels = BandFrequencies.labelsForCount(state.eq.bandCount)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    state.eq.bandGainsDb.forEachIndexed { index, gain ->
                        EqBandFader(
                            value = gain,
                            min = -12f,
                            max = 12f,
                            label = labels.getOrElse(index) { "" },
                            color = bandColor(index, state.eq.bandGainsDb.size),
                            onChange = { viewModel.setBandGain(index, it) },
                        )
                    }
                }
            }
        }

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
private fun EngineStatusCard(
    engineEnabled: Boolean,
    mode: EqEngineMode,
    status: com.smarteq.pro.data.model.EqEngineStatus,
    onToggleEnabled: (Boolean) -> Unit,
    onSetMode: (EqEngineMode) -> Unit,
    onHelp: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = if (engineEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (engineEnabled) stringResource(R.string.eq_engine_on) else stringResource(R.string.eq_engine_off),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = onHelp) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Help: processing mode", modifier = Modifier.size(18.dp))
                    }
                }
                Switch(checked = engineEnabled, onCheckedChange = onToggleEnabled)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = statusText(status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                FilterChip(
                    selected = mode == EqEngineMode.UNIVERSAL_GLOBAL,
                    onClick = { onSetMode(EqEngineMode.UNIVERSAL_GLOBAL) },
                    label = { Text(stringResource(R.string.eq_mode_universal)) },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = mode == EqEngineMode.DEFAULT_PER_SESSION,
                    onClick = { onSetMode(EqEngineMode.DEFAULT_PER_SESSION) },
                    label = { Text(stringResource(R.string.eq_mode_default)) },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = mode == EqEngineMode.LEGACY_GLOBAL,
                    onClick = { onSetMode(EqEngineMode.LEGACY_GLOBAL) },
                    label = { Text(stringResource(R.string.eq_mode_legacy)) },
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
                        EqKnob(dynamics.bassDb, -12f, 12f, "Bass", "dB") { onDynamicsChange { d -> d.copy(bassDb = it) } }
                        EqKnob(dynamics.trebleDb, -12f, 12f, "Treble", "dB") { onDynamicsChange { d -> d.copy(trebleDb = it) } }
                        EqKnob(dynamics.compThresholdDb, -60f, 0f, "Comp Thr", "dB") { onDynamicsChange { d -> d.copy(compThresholdDb = it) } }
                        EqKnob(dynamics.compRatio, 1f, 20f, "Comp Rat", ":1") { onDynamicsChange { d -> d.copy(compRatio = it) } }
                        EqKnob(dynamics.compAttackSec, 0f, 1f, "Attack", "s") { onDynamicsChange { d -> d.copy(compAttackSec = it) } }
                        EqKnob(dynamics.compReleaseSec, 0f, 1f, "Release", "s") { onDynamicsChange { d -> d.copy(compReleaseSec = it) } }
                        EqKnob(dynamics.compKneeDb, 0f, 40f, "Knee", "dB") { onDynamicsChange { d -> d.copy(compKneeDb = it) } }
                        EqKnob(outputTrimDb, -12f, 0f, "Trim", "dB", onOutputTrimChange)
                    }
                }
            }
        }
    }
}
