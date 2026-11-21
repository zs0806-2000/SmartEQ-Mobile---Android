package com.smarteq.pro.ui.settings

import com.smarteq.pro.ui.components.LiquidCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import com.smarteq.pro.ui.theme.AppFont
import com.smarteq.pro.ui.theme.ThemeMode
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.smarteq.pro.R
import com.smarteq.pro.ui.applock.AppLockManager

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    onNavigateToHelp: (String) -> Unit,
    pendingSpotifyRedirect: android.net.Uri?,
    onSpotifyRedirectConsumed: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(pendingSpotifyRedirect) {
        pendingSpotifyRedirect?.let {
            viewModel.handleSpotifyRedirect(it)
            onSpotifyRedirectConsumed()
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
        SettingsSection(title = "Appearance") {
            AppearanceBlock(viewModel, onHelp = { onNavigateToHelp("appearance") })
        }

        SettingsSection(title = stringResource(R.string.settings_section_connections)) {
            GeminiSettingsBlock(state, viewModel, onHelp = { onNavigateToHelp("gemini") })
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            SpotifySettingsBlock(state, viewModel, onHelp = { onNavigateToHelp("spotify") })
        }

        SettingsSection(title = stringResource(R.string.settings_section_processing)) {
            SettingsSwitchRow(
                label = stringResource(R.string.bluetooth_low_latency_title),
                checked = state.bluetoothLowLatencyEnabled,
                onCheckedChange = viewModel::setBluetoothLowLatency,
            )
            Text(
                stringResource(R.string.bluetooth_low_latency_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            SettingsSwitchRow(
                label = stringResource(R.string.settings_resume_on_boot),
                checked = state.resumeOnBoot,
                onCheckedChange = viewModel::setResumeOnBoot,
            )
            SettingsHelpRow(title = "How system-wide processing works", onClick = { onNavigateToHelp("system_wide") })
        }

        SettingsSection(title = stringResource(R.string.settings_section_security)) {
            AppLockSettingsBlock(state, viewModel, onHelp = { onNavigateToHelp("app_lock") })
        }

        SettingsSection(title = stringResource(R.string.settings_section_about)) {
            SettingsHelpRow(title = "Help & feature guide", onClick = { onNavigateToHelp("index") })
            Text(
                "SmartEQ Pro v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    LiquidCard(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SettingsHelpRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onClick) {
            Icon(Icons.Default.HelpOutline, contentDescription = "Open help: $title")
        }
    }
}

@Composable
private fun GeminiSettingsBlock(state: SettingsUiState, viewModel: SettingsViewModel, onHelp: () -> Unit) {
    var keyInput by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.settings_gemini_key_label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onHelp) { Icon(Icons.Default.HelpOutline, contentDescription = "Help: Gemini") }
    }
    Text(
        stringResource(R.string.settings_gemini_key_hint),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))

    if (state.geminiKeyConfigured) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(end = 8.dp))
            Text("API key configured", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { viewModel.clearGeminiApiKey() }) { Text("Remove") }
        }
        Spacer(Modifier.height(8.dp))
        SettingsSwitchRow(
            label = stringResource(R.string.settings_gemini_enabled),
            checked = state.geminiNetworkEnabled,
            onCheckedChange = viewModel::setGeminiNetworkEnabled,
        )
    } else {
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("Paste your Gemini API key") },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Toggle visibility")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(
            elevation = androidx.compose.material3.ButtonDefaults.filledTonalButtonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp), onClick = { viewModel.setGeminiApiKey(keyInput); viewModel.setGeminiNetworkEnabled(true); keyInput = "" },
            enabled = keyInput.isNotBlank(),
        ) { Text("Save key") }
    }
}

@Composable
private fun SpotifySettingsBlock(state: SettingsUiState, viewModel: SettingsViewModel, onHelp: () -> Unit) {
    var clientIdInput by remember { mutableStateOf("") }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Spotify", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onHelp) { Icon(Icons.Default.HelpOutline, contentDescription = "Help: Spotify") }
    }
    Spacer(Modifier.height(4.dp))

    if (!state.spotifyClientIdConfigured) {
        Text(
            "Enter your own Spotify Developer Client ID to enable this (see SPOTIFY_SETUP.md). No client secret is ever needed or stored.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = clientIdInput,
            onValueChange = { clientIdInput = it },
            label = { Text("Spotify Client ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(elevation = androidx.compose.material3.ButtonDefaults.filledTonalButtonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp), onClick = { viewModel.setSpotifyClientId(clientIdInput) }, enabled = clientIdInput.isNotBlank()) {
            Text("Save Client ID")
        }
    } else if (!state.spotifyConnected) {
        FilledTonalButton(elevation = androidx.compose.material3.ButtonDefaults.filledTonalButtonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp), onClick = { viewModel.beginSpotifyLogin() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_spotify_connect))
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Connected", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { viewModel.disconnectSpotify() }) { Text(stringResource(R.string.settings_spotify_disconnect)) }
        }
        Spacer(Modifier.height(8.dp))
        SettingsSwitchRow(
            label = stringResource(R.string.settings_spotify_enabled),
            checked = state.spotifyNetworkEnabled,
            onCheckedChange = viewModel::setSpotifyNetworkEnabled,
        )
    }
}

@Composable
private fun AppLockSettingsBlock(state: SettingsUiState, viewModel: SettingsViewModel, onHelp: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appLockManager = (context.applicationContext as com.smarteq.pro.SmartEqApplication).container.appLockManager
    var showSetPinDialog by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.settings_applock_enable), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onHelp) { Icon(Icons.Default.HelpOutline, contentDescription = "Help: App Lock") }
    }
    SettingsSwitchRow(
        label = "Enabled",
        checked = state.appLockEnabled,
        onCheckedChange = viewModel::setAppLockEnabled,
    )

    if (state.appLockEnabled) {
        Spacer(Modifier.height(8.dp))
        Text("Unlock method", style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = state.appLockUseBiometric, onClick = { viewModel.setAppLockUseBiometric(true) })
            Text("Biometric / device PIN", modifier = Modifier.padding(end = 16.dp))
            RadioButton(selected = !state.appLockUseBiometric, onClick = { viewModel.setAppLockUseBiometric(false) })
            Text("In-app PIN")
        }

        if (!state.appLockUseBiometric) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.appLockPinConfigured) "PIN is set" else "No PIN set yet",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { showSetPinDialog = true }) {
                    Text(if (state.appLockPinConfigured) "Change PIN" else "Set PIN")
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.settings_applock_timeout), style = MaterialTheme.typography.labelMedium)
        Row {
            listOf(0 to "Immediately", 60 to "1 min", 300 to "5 min").forEach { (seconds, label) ->
                FilledTonalButton(
                    elevation = androidx.compose.material3.ButtonDefaults.filledTonalButtonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp), onClick = { viewModel.setAppLockTimeoutSeconds(seconds) },
                    modifier = Modifier.padding(end = 8.dp),
                ) { Text(label) }
            }
        }
    }

    if (showSetPinDialog) {
        SetPinDialog(
            onDismiss = { showSetPinDialog = false },
            onConfirm = { pin -> appLockManager.setPin(pin); showSetPinDialog = false },
        )
    }
}

@Composable
private fun SetPinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val mismatch = confirmPin.isNotEmpty() && pin != confirmPin

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set App Lock PIN") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                    label = { Text("New PIN (4-8 digits)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it.filter(Char::isDigit).take(8) },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    isError = mismatch,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                if (mismatch) Text("PINs don't match", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(pin) },
                enabled = pin.length >= 4 && pin == confirmPin,
            ) { Text("Save") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AppearanceBlock(viewModel: SettingsViewModel, onHelp: () -> Unit) {
    val font by viewModel.appFont.collectAsState()
    val mode by viewModel.themeMode.collectAsState()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Theme", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        IconButton(onClick = onHelp) {
            Icon(Icons.Default.HelpOutline, contentDescription = "Help: appearance")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { m ->
            FilterChip(
                elevation = FilterChipDefaults.filterChipElevation(elevation = 2.dp),
                selected = m == mode,
                onClick = { viewModel.setThemeMode(m) },
                label = { Text(m.label) },
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    Text("Font", style = MaterialTheme.typography.titleSmall)
    Text(
        "All fonts are built into the app; switching never downloads anything.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    Column(Modifier.selectableGroup()) {
        AppFont.entries.forEach { f ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .selectable(selected = f == font, role = Role.RadioButton, onClick = { viewModel.setAppFont(f) })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = f == font, onClick = null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(f.label, fontFamily = f.heading, fontWeight = f.headingWeight, fontSize = 18.sp)
                    Text(
                        "Equalizer  +3.5 dB  1k  16k",
                        fontFamily = f.body,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

