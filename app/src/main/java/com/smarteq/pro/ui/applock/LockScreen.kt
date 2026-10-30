package com.smarteq.pro.ui.applock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.smarteq.pro.R
import com.smarteq.pro.data.repository.SettingsRepository
import com.smarteq.pro.ui.theme.BrandPurple
import com.smarteq.pro.ui.theme.BrandTeal

@Composable
fun LockScreen(
    appLockManager: AppLockManager,
    settingsRepository: SettingsRepository,
) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val preferBiometric by settingsRepository.appLockUseBiometric.collectAsState(initial = true)
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val hasPin = appLockManager.hasPinConfigured()
    val biometricAvailable = activity != null && appLockManager.canUseBiometrics(activity)
    val useBiometric = preferBiometric && biometricAvailable && (!hasPin || preferBiometric)

    fun tryBiometric() {
        activity?.let {
            appLockManager.promptBiometric(it, onSuccess = {}, onError = { })
        }
    }

    LaunchedEffect(useBiometric) {
        if (useBiometric && !hasPin) tryBiometric()
        else if (useBiometric && hasPin && preferBiometric) tryBiometric()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(BrandPurple, BrandTeal))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(44.dp))
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.lock_prompt_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        if (hasPin && !preferBiometric) {
            OutlinedTextField(
                value = pinInput,
                onValueChange = { pinInput = it.filter(Char::isDigit).take(8); pinError = false },
                label = { Text("Enter PIN") },
                singleLine = true,
                isError = pinError,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
            if (pinError) {
                Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                if (appLockManager.verifyPin(pinInput)) {
                    appLockManager.unlock()
                } else {
                    pinError = true
                }
            }) { Text(stringResource(R.string.lock_unlock_button)) }
        } else {
            Button(onClick = { tryBiometric() }) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  " + stringResource(R.string.lock_unlock_button))
            }
        }
    }
}
