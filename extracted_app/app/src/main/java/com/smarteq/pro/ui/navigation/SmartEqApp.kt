package com.smarteq.pro.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smarteq.pro.data.repository.SpotifyRepository
import com.smarteq.pro.ui.applock.AppLockManager
import com.smarteq.pro.ui.applock.LockScreen
import com.smarteq.pro.ui.autotune.AutoTuneScreen
import com.smarteq.pro.ui.equalizer.EqualizerScreen
import com.smarteq.pro.ui.help.HelpScreen
import com.smarteq.pro.ui.onboarding.OnboardingScreen
import com.smarteq.pro.ui.onboarding.OnboardingViewModel
import com.smarteq.pro.ui.presets.PresetsScreen
import com.smarteq.pro.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

sealed class TopLevelDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Equalizer : TopLevelDestination("equalizer", "Equalizer", Icons.Default.Equalizer)
    object AutoTune : TopLevelDestination("autotune", "Auto-Tune", Icons.Default.AutoAwesome)
    object Presets : TopLevelDestination("presets", "Presets", Icons.Default.BookmarkBorder)
    object Settings : TopLevelDestination("settings", "Settings", Icons.Default.Settings)
}

private val bottomNavDestinations = listOf(
    TopLevelDestination.Equalizer,
    TopLevelDestination.AutoTune,
    TopLevelDestination.Presets,
    TopLevelDestination.Settings,
)

@Composable
fun SmartEqApp(
    appLockManager: AppLockManager,
    pendingSpotifyRedirect: android.net.Uri?,
    onSpotifyRedirectConsumed: () -> Unit,
) {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingComplete by onboardingViewModel.onboardingComplete.collectAsState(initial = true)
    val isLocked by appLockManager.isLocked.collectAsState()

    when {
        !onboardingComplete -> OnboardingScreen(onFinished = { onboardingViewModel.markComplete() })
        isLocked -> LockScreen(appLockManager = appLockManager)
        else -> MainScaffold(pendingSpotifyRedirect, onSpotifyRedirectConsumed)
    }
}

@Composable
private fun MainScaffold(
    pendingSpotifyRedirect: android.net.Uri?,
    onSpotifyRedirectConsumed: () -> Unit,
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                bottomNavDestinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Equalizer.route,
            modifier = Modifier
                .fillMaxSize()
                .let { it }
        ) {
            composable(TopLevelDestination.Equalizer.route) {
                EqualizerScreen(
                    padding = paddingValues,
                    onNavigateToHelp = { topic -> navController.navigate("help/$topic") },
                )
            }
            composable(TopLevelDestination.AutoTune.route) {
                AutoTuneScreen(
                    padding = paddingValues,
                    onNavigateToHelp = { topic -> navController.navigate("help/$topic") },
                )
            }
            composable(TopLevelDestination.Presets.route) {
                PresetsScreen(padding = paddingValues)
            }
            composable(TopLevelDestination.Settings.route) {
                SettingsScreen(
                    padding = paddingValues,
                    onNavigateToHelp = { topic -> navController.navigate("help/$topic") },
                    pendingSpotifyRedirect = pendingSpotifyRedirect,
                    onSpotifyRedirectConsumed = onSpotifyRedirectConsumed,
                )
            }
            composable("help/{topic}") { backStackEntry ->
                HelpScreen(
                    topic = backStackEntry.arguments?.getString("topic"),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
