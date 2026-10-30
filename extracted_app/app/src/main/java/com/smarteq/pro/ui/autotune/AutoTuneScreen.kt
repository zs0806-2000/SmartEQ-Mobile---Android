package com.smarteq.pro.ui.autotune

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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smarteq.pro.R
import com.smarteq.pro.data.model.AutoEqProfile

@Composable
fun AutoTuneScreen(
    padding: PaddingValues,
    onNavigateToHelp: (String) -> Unit,
    viewModel: AutoTuneViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var manualArtist by remember { mutableStateOf("") }
    var manualTrack by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.loadLocalAudioFile(it, it.lastPathSegment ?: "audio_track")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.autotune_title), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { onNavigateToHelp("gemini") }) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Help: AI tuning", modifier = Modifier.size(18.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.autotune_toggle_label), style = MaterialTheme.typography.labelMedium)
                        Switch(checked = state.autoTuneEnabled, onCheckedChange = viewModel::setAutoTuneEnabled)
                    }
                }

                if (!state.geminiAvailable) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Using the built-in local tuning library (no API key configured — see Settings).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Current track card
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        val track = state.currentTrack
                        if (track != null) {
                            Text(track.title, style = MaterialTheme.typography.titleMedium)
                            Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                            track.album?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        } else {
                            Text(stringResource(R.string.autotune_no_track), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.local_audio_pick))
                }

                Spacer(Modifier.height(12.dp))
                Text("Manual artist search", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = manualArtist,
                    onValueChange = { manualArtist = it },
                    label = { Text("Artist name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualTrack,
                    onValueChange = { manualTrack = it },
                    label = { Text("Track title (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = { viewModel.setManualArtist(manualArtist.trim(), manualTrack.trim().ifBlank { null }) },
                    enabled = manualArtist.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Set Active Artist") }

                Spacer(Modifier.height(16.dp))
                Text("Quick Artist Test Profiles", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.quickArtists) { artist ->
                        SuggestionChip(onClick = { viewModel.setManualArtist(artist, null) }, label = { Text(artist) })
                    }
                }
            }
        }

        AutoEqProfileCard(
            selected = state.selectedAutoEqProfile,
            profiles = viewModel.autoEqProfiles,
            onSelect = viewModel::selectAutoEqProfile,
        )

        FilledTonalButton(
            onClick = { viewModel.triggerAutoTune() },
            enabled = !state.isTuning,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isTuning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.autotune_calculating))
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.autotune_trigger))
            }
        }

        state.activeResult?.let { result ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Active Profile: ${result.artist} → ${result.outputDeviceName}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(result.explanation, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (result.source == com.smarteq.pro.data.model.TuningSource.GEMINI_AI) "Generated by Gemini AI" else "From local tuning library",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoEqProfileCard(
    selected: AutoEqProfile,
    profiles: List<AutoEqProfile>,
    onSelect: (AutoEqProfile) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp)) {
            Text("Audio Output Source (AutoEQ)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = "${selected.brand} ${selected.model}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Headphone / Speaker profile") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                androidx.compose.material3.ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    profiles.forEach { profile ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Column {
                                    Text("${profile.brand} ${profile.model}", style = MaterialTheme.typography.bodyMedium)
                                    Text(profile.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = { onSelect(profile); expanded = false },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(selected.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
