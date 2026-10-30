package com.smarteq.pro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.smarteq.pro.data.repository.SpotifyRepository
import com.smarteq.pro.ui.applock.AppLockManager
import com.smarteq.pro.ui.navigation.SmartEqApp
import com.smarteq.pro.ui.theme.SmartEqTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appLockManager: AppLockManager
    @Inject lateinit var spotifyRepository: SpotifyRepository

    private var pendingSpotifyRedirect by mutableStateOf<android.net.Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            SmartEqTheme {
                SmartEqApp(
                    appLockManager = appLockManager,
                    pendingSpotifyRedirect = pendingSpotifyRedirect,
                    onSpotifyRedirectConsumed = { pendingSpotifyRedirect = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "smarteqpro" && data.host == "spotify-callback") {
            pendingSpotifyRedirect = data
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { appLockManager.onAppForegrounded() }
    }

    override fun onStop() {
        appLockManager.onAppBackgrounded()
        super.onStop()
    }
}
