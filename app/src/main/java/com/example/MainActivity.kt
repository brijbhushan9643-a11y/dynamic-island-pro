package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.CustomizationStudioScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ModulesScreen
import com.example.ui.screens.SetupAndPermissionsScreen
import com.example.ui.theme.DynamicIslandProTheme
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedObsidian
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.MainViewModel

enum class AppDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("dashboard", R.string.nav_dashboard, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    STUDIO("studio", R.string.nav_studio, Icons.Filled.Palette, Icons.Outlined.Palette),
    MODULES("modules", R.string.nav_modules, Icons.Filled.Tune, Icons.Outlined.Tune),
    SETUP("setup", R.string.nav_setup, Icons.Filled.Security, Icons.Outlined.Security)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DynamicIslandProTheme {
                DynamicIslandProRoot()
            }
        }
    }
}

@Composable
fun DynamicIslandProRoot(
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissions()
    }

    val navigateTo: (AppDestination) -> Unit = { dest ->
        navController.navigate(dest.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        val useNavRail = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = VoidBlack,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        tonalElevation = 0.dp
                    ) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_${dest.route}"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElevatedObsidian,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        contentColor = TextPrimary
                    ) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_${dest.route}"),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElevatedObsidian,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppDestination.DASHBOARD.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AppDestination.DASHBOARD.route) {
                        DashboardScreen(
                            uiState = uiState,
                            onToggleIsland = viewModel::toggleDynamicIsland,
                            onTogglePreviewExpand = viewModel::togglePreviewExpanded,
                            onSelectPreviewMode = viewModel::setPreviewMode,
                            onToggleFeature = viewModel::toggleFeature,
                            onNavigateToStudio = { navigateTo(AppDestination.STUDIO) },
                            onNavigateToModules = { navigateTo(AppDestination.MODULES) },
                            onNavigateToSetup = { navigateTo(AppDestination.SETUP) },
                            onPlayPause = viewModel::toggleMediaPlayPause,
                            onSkipNext = viewModel::skipToNextMedia,
                            onSkipPrev = viewModel::skipToPreviousMedia,
                            onOpenMediaApp = viewModel::openActiveMediaApp,
                            onOpenNotification = viewModel::openNotification,
                            onDismissNotification = viewModel::dismissNotification,
                            onSetMediaVolume = viewModel::setMediaVolume,
                            onSetBrightness = { viewModel.setSystemBrightness(it) }
                        )
                    }
                    composable(AppDestination.STUDIO.route) {
                        CustomizationStudioScreen(
                            uiState = uiState,
                            onTogglePreviewExpand = viewModel::togglePreviewExpanded,
                            onApplyThemePreset = viewModel::applyThemePreset,
                            onUpdateDimensions = viewModel::updateDimensions,
                            onUpdateAppearance = viewModel::updateAppearance,
                            onUpdateAnimation = viewModel::updateAnimation,
                            onResetDefaults = viewModel::resetStudioDefaults
                        )
                    }
                    composable(AppDestination.MODULES.route) {
                        ModulesScreen(
                            uiState = uiState,
                            onPlayPause = viewModel::toggleMediaPlayPause,
                            onSkipNext = viewModel::skipToNextMedia,
                            onSkipPrev = viewModel::skipToPreviousMedia,
                            onOpenMediaApp = viewModel::openActiveMediaApp,
                            onSetMediaVolume = viewModel::setMediaVolume,
                            onSetBrightness = { viewModel.setSystemBrightness(it) },
                            onOpenNotification = viewModel::openNotification,
                            onDismissNotification = viewModel::dismissNotification,
                            onToggleMonitoredPackage = viewModel::toggleMonitoredPackage,
                            onNavigateToSetup = { navigateTo(AppDestination.SETUP) }
                        )
                    }
                    composable(AppDestination.SETUP.route) {
                        SetupAndPermissionsScreen(
                            uiState = uiState,
                            onRefreshPermissions = viewModel::refreshPermissions,
                            onToggleFeature = viewModel::toggleFeature
                        )
                    }
                }
            }
        }
    }
}
