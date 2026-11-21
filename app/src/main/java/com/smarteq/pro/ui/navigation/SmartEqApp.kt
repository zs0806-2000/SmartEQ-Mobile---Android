package com.smarteq.pro.ui.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.smarteq.pro.ui.components.liquidBackground
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smarteq.pro.SmartEqApplication
import com.smarteq.pro.ui.applock.AppLockManager
import com.smarteq.pro.ui.applock.LockScreen
import com.smarteq.pro.ui.autotune.AutoTuneScreen
import com.smarteq.pro.ui.autotune.AutoTuneViewModel
import com.smarteq.pro.ui.equalizer.EqualizerScreen
import com.smarteq.pro.ui.equalizer.EqualizerViewModel
import com.smarteq.pro.ui.help.HelpScreen
import com.smarteq.pro.ui.onboarding.OnboardingScreen
import com.smarteq.pro.ui.onboarding.OnboardingViewModel
import com.smarteq.pro.ui.presets.PresetsScreen
import com.smarteq.pro.ui.presets.PresetsViewModel
import com.smarteq.pro.ui.settings.SettingsScreen
import com.smarteq.pro.ui.settings.SettingsViewModel

sealed class TopLevelDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val testTag: String) {
    object Equalizer : TopLevelDestination("equalizer", "Equalizer", Icons.Default.Equalizer, "nav_equalizer")
    object AutoTune : TopLevelDestination("autotune", "Auto-Tune", Icons.Default.AutoAwesome, "nav_autotune")
    object Presets : TopLevelDestination("presets", "Presets", Icons.Default.BookmarkBorder, "nav_presets")
    object Settings : TopLevelDestination("settings", "Settings", Icons.Default.Settings, "nav_settings")
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
    val context = LocalContext.current
    val app = context.applicationContext as SmartEqApplication
    val onboardingViewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.provideFactory(app.container))
    val onboardingComplete by onboardingViewModel.onboardingComplete.collectAsState(initial = true)
    val isLocked by appLockManager.isLocked.collectAsState()

    when {
        !onboardingComplete -> OnboardingScreen(onFinished = { onboardingViewModel.markComplete() })
        isLocked -> LockScreen(appLockManager = appLockManager, settingsRepository = app.container.settingsRepository)
        else -> MainScaffold(app, pendingSpotifyRedirect, onSpotifyRedirectConsumed)
    }
}

@Composable
private fun MainScaffold(
    app: SmartEqApplication,
    pendingSpotifyRedirect: android.net.Uri?,
    onSpotifyRedirectConsumed: () -> Unit,
) {
    val navController = rememberNavController()

    val equalizerViewModel: EqualizerViewModel = viewModel(factory = EqualizerViewModel.provideFactory(app.container))
    val autoTuneViewModel: AutoTuneViewModel = viewModel(factory = AutoTuneViewModel.provideFactory(app.container))
    val presetsViewModel: PresetsViewModel = viewModel(factory = PresetsViewModel.provideFactory(app.container))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.provideFactory(app.container))

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar").liquidBackground(variant = 2),
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                tonalElevation = 0.dp,
            ) {
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
                        modifier = Modifier.testTag(destination.testTag),
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Equalizer.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(TopLevelDestination.Equalizer.route) {
                EqualizerScreen(
                    padding = paddingValues,
                    onNavigateToHelp = { topic -> navController.navigate("help/$topic") },
                    viewModel = equalizerViewModel,
                )
            }
            composable(TopLevelDestination.AutoTune.route) {
                AutoTuneScreen(
                    padding = paddingValues,
                    onNavigateToHelp = { topic -> navController.navigate("help/$topic") },
                    viewModel = autoTuneViewModel,
                )
            }
            composable(TopLevelDestination.Presets.route) {
                PresetsScreen(
                    padding = paddingValues,
                    viewModel = presetsViewModel,
                )
            }
            composable(TopLevelDestination.Settings.route) {
                SettingsScreen(
                    padding = paddingValues,
                    onNavigateToHelp = { topic -> navController.navigate("help/$topic") },
                    pendingSpotifyRedirect = pendingSpotifyRedirect,
                    onSpotifyRedirectConsumed = onSpotifyRedirectConsumed,
                    viewModel = settingsViewModel,
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
