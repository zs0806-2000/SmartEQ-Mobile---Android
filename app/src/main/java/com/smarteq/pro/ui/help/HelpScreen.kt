package com.smarteq.pro.ui.help

import com.smarteq.pro.ui.components.LiquidCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smarteq.pro.R

private data class HelpEntry(val titleRes: Int, val bodyRes: Int)

private val HELP_ENTRIES = mapOf(
    "appearance" to HelpEntry(R.string.help_appearance_title, R.string.help_appearance_body),
    "adaptive" to HelpEntry(R.string.help_adaptive_title, R.string.help_adaptive_body),
    "eq_mode" to HelpEntry(R.string.help_eq_mode_title, R.string.help_eq_mode_body),
    "gemini" to HelpEntry(R.string.help_gemini_title, R.string.help_gemini_body),
    "spotify" to HelpEntry(R.string.help_spotify_title, R.string.help_spotify_body),
    "system_wide" to HelpEntry(R.string.help_system_wide_title, R.string.help_system_wide_body),
    "app_lock" to HelpEntry(R.string.help_app_lock_title, R.string.help_app_lock_body),
    "dynamics" to HelpEntry(R.string.help_dynamics_title, R.string.help_dynamics_body),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(topic: String?, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val entries = if (topic == "index" || topic == null) HELP_ENTRIES.values.toList()
                else listOfNotNull(HELP_ENTRIES[topic])

            entries.forEach { entry ->
                LiquidCard(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text(stringResource(entry.titleRes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(entry.bodyRes), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
