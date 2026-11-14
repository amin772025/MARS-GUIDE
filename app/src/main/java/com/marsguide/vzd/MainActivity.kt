package com.marsguide.vzd

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.marsguide.vzd.ui.components.*
import com.marsguide.vzd.ui.screens.*
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.ui.viewmodel.MarsGuideViewModel

sealed class Screen(val route: String) {
    object Explore : Screen("explore")
    object Map : Screen("map")
    object RoutePlanner : Screen("route_planner?startId={startId}&destId={destId}") {
        fun createRoute(startId: String? = null, destId: String? = null): String {
            val s = startId ?: ""
            val d = destId ?: ""
            return "route_planner?startId=$s&destId=$d"
        }
    }
    object Missions : Screen("missions")
    object Saved : Screen("saved")
    object About : Screen("about")
    object GeminiChat : Screen("gemini_chat")
    object Weather : Screen("weather")
    object AudioGuide : Screen("audio_guide")
    object LocationAnalysis : Screen("location_analysis/{locationId}") {
        fun createRoute(locationId: String) = "location_analysis/$locationId"
    }
}

data class NavItem(
    val screen: Screen,
    val titleResId: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val prefs = getSharedPreferences("MarsGuideSettings", Context.MODE_PRIVATE)
        val initialSkipBoot = prefs.getBoolean("skip_boot_sequence", false)
        val initialHasSeenTour = prefs.getBoolean("has_seen_tour", false)
        val soundEnabled = prefs.getBoolean("sound_effects_enabled", true)
        com.marsguide.vzd.util.SoundManager.init(this, soundEnabled)
        if (initialSkipBoot) {
            com.marsguide.vzd.util.SoundManager.setGlobalMute(false)
        }

        setContent {
            MarsGuideTheme {
                val viewModel: MarsGuideViewModel = viewModel()
                var showBootSequence by remember { mutableStateOf(!initialSkipBoot) }
                var showGuidedTour by remember { mutableStateOf(!initialHasSeenTour && !showBootSequence) }
                var isSearchOpen by remember { mutableStateOf(false) }

                val navController = rememberNavController()

                Box(modifier = Modifier.fillMaxSize()) {
                    MarsGuideAppContent(
                        viewModel = viewModel,
                        navController = navController,
                        onOpenSearch = { isSearchOpen = true }
                    )

                    // Global Search Dialog
                    if (isSearchOpen) {
                        SearchDialog(
                            viewModel = viewModel,
                            onDismiss = { isSearchOpen = false },
                            onNavigateToLocation = { locId ->
                                navController.navigate(Screen.LocationAnalysis.createRoute(locId))
                            },
                            onNavigateToMissions = {
                                navController.navigate(Screen.Missions.route)
                            },
                            onSelectCoordinateOnMap = {
                                navController.navigate(Screen.Map.route)
                            }
                        )
                    }

                    // First-time Guided Tour Walkthrough
                    if (showGuidedTour) {
                        GuidedTourDialog(
                            onDismiss = {
                                prefs.edit().putBoolean("has_seen_tour", true).apply()
                                showGuidedTour = false
                            }
                        )
                    }

                    // Startup Boot Sequence Overlay
                    if (showBootSequence) {
                        BootSequenceOverlay(
                            onComplete = {
                                showBootSequence = false
                                if (!prefs.getBoolean("has_seen_tour", false)) {
                                    showGuidedTour = true
                                }
                            },
                            onSkipForever = {
                                prefs.edit().putBoolean("skip_boot_sequence", true).apply()
                                showBootSequence = false
                                if (!prefs.getBoolean("has_seen_tour", false)) {
                                    showGuidedTour = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        com.marsguide.vzd.util.SoundManager.onPause()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("MarsGuideSettings", Context.MODE_PRIVATE)
        val soundEnabled = prefs.getBoolean("sound_effects_enabled", true)
        com.marsguide.vzd.util.SoundManager.setEnabled(soundEnabled)
        com.marsguide.vzd.util.SoundManager.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        com.marsguide.vzd.util.SoundManager.release()
    }
}

@Composable
fun MarsGuideAppContent(
    viewModel: MarsGuideViewModel,
    navController: androidx.navigation.NavHostController,
    onOpenSearch: () -> Unit
) {
    val navItems = listOf(
        NavItem(
            screen = Screen.Explore,
            titleResId = R.string.nav_explore,
            label = stringResource(R.string.nav_command).uppercase(),
            selectedIcon = Icons.Filled.Dashboard,
            unselectedIcon = Icons.Outlined.Dashboard,
            testTag = "nav_explore"
        ),
        NavItem(
            screen = Screen.Map,
            titleResId = R.string.nav_map,
            label = stringResource(R.string.nav_map).uppercase(),
            selectedIcon = Icons.Filled.Map,
            unselectedIcon = Icons.Outlined.Map,
            testTag = "nav_map"
        ),
        NavItem(
            screen = Screen.RoutePlanner,
            titleResId = R.string.nav_planner,
            label = stringResource(R.string.nav_planner).uppercase(),
            selectedIcon = Icons.Filled.Route,
            unselectedIcon = Icons.Outlined.Route,
            testTag = "nav_planner"
        ),
        NavItem(
            screen = Screen.Missions,
            titleResId = R.string.nav_missions,
            label = stringResource(R.string.nav_missions).uppercase(),
            selectedIcon = Icons.Filled.RocketLaunch,
            unselectedIcon = Icons.Outlined.RocketLaunch,
            testTag = "nav_missions"
        ),
        NavItem(
            screen = Screen.Saved,
            titleResId = R.string.nav_saved,
            label = stringResource(R.string.nav_archive).uppercase(),
            selectedIcon = Icons.Filled.Bookmark,
            unselectedIcon = Icons.Outlined.BookmarkBorder,
            testTag = "nav_saved"
        ),
        NavItem(
            screen = Screen.About,
            titleResId = R.string.nav_about,
            label = stringResource(R.string.nav_sources).uppercase(),
            selectedIcon = Icons.Filled.Info,
            unselectedIcon = Icons.Outlined.Info,
            testTag = "nav_about"
        ),
        NavItem(
            screen = Screen.GeminiChat,
            titleResId = R.string.nav_explore,
            label = "KNOWLEDGE",
            selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
            unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
            testTag = "nav_knowledge"
        ),
        NavItem(
            screen = Screen.Weather,
            titleResId = R.string.nav_explore,
            label = "WEATHER",
            selectedIcon = Icons.Filled.Thermostat,
            unselectedIcon = Icons.Outlined.Thermostat,
            testTag = "nav_weather"
        ),
        NavItem(
            screen = Screen.AudioGuide,
            titleResId = R.string.nav_explore,
            label = "AUDIO TOUR",
            selectedIcon = Icons.AutoMirrored.Filled.VolumeUp,
            unselectedIcon = Icons.AutoMirrored.Outlined.VolumeUp,
            testTag = "nav_audio"
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom navigation bar on primary tabs
    val showBottomBar = currentRoute != null && !currentRoute.startsWith("location_analysis")

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
        containerColor = SpaceBlack,
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(androidx.compose.foundation.BorderStroke(1.dp, SpaceBorderDark))
                        .testTag("bottom_navigation_bar"),
                    color = SpaceDark,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .heightIn(min = 60.dp)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEach { item ->
                            val isSelected = currentRoute == item.screen.route ||
                                    (item.screen is Screen.RoutePlanner && currentRoute?.startsWith("route_planner") == true)

                            Column(
                                modifier = Modifier
                                    .widthIn(min = 68.dp)
                                    .heightIn(min = 50.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MarsOrangeBright.copy(alpha = 0.14f) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) MarsOrangeBright.copy(alpha = 0.55f) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        if (currentRoute != item.screen.route) {
                                            com.marsguide.vzd.util.SoundManager.playQuindarOn()
                                            com.marsguide.vzd.util.SoundManager.playNavigationClick()
                                            navController.navigate(item.screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag(item.testTag),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) MarsOrangeBright else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MarsOrangeBright else Color(0xFF94A3B8),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp,
                                    maxLines = 1,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Explore.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(androidx.compose.animation.core.tween(280)) },
            exitTransition = { fadeOut(androidx.compose.animation.core.tween(220)) },
            popEnterTransition = { fadeIn(androidx.compose.animation.core.tween(280)) },
            popExitTransition = { fadeOut(androidx.compose.animation.core.tween(220)) }
        ) {
            // 1. Explore Screen
            composable(Screen.Explore.route) {
                ExploreScreen(
                    viewModel = viewModel,
                    onNavigateToMap = {
                        navController.navigate(Screen.Map.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLocation = { locId ->
                        navController.navigate(Screen.LocationAnalysis.createRoute(locId))
                    },
                    onNavigateToPlanner = {
                        navController.navigate(Screen.RoutePlanner.createRoute())
                    },
                    onNavigateToMissions = {
                        navController.navigate(Screen.Missions.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenSearch = onOpenSearch
                )
            }

            // 2. Map Screen
            composable(Screen.Map.route) {
                MapScreen(
                    viewModel = viewModel,
                    onNavigateToLocation = { locId ->
                        navController.navigate(Screen.LocationAnalysis.createRoute(locId))
                    },
                    onNavigateToPlannerWithStart = { locId ->
                        navController.navigate(Screen.RoutePlanner.createRoute(startId = locId))
                    },
                    onSwitchToViewport = {
                        navController.navigate(Screen.Explore.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // 3. Route Planner Screen
            composable(
                route = Screen.RoutePlanner.route,
                arguments = listOf(
                    navArgument("startId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("destId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val startId = backStackEntry.arguments?.getString("startId")?.takeIf { it.isNotBlank() }
                val destId = backStackEntry.arguments?.getString("destId")?.takeIf { it.isNotBlank() }
                RoutePlannerScreen(
                    viewModel = viewModel,
                    initialStartId = startId,
                    initialDestId = destId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 4. Missions Screen
            composable(Screen.Missions.route) {
                MissionsScreen(
                    viewModel = viewModel,
                    onNavigateToLocationByName = { locName ->
                        viewModel.locations.value.find { it.name.contains(locName, ignoreCase = true) }?.let { loc ->
                            viewModel.selectLocation(loc)
                            navController.navigate(Screen.LocationAnalysis.createRoute(loc.id))
                        }
                    }
                )
            }

            // 5. Saved Screen
            composable(Screen.Saved.route) {
                SavedScreen(
                    viewModel = viewModel,
                    onNavigateToLocation = { locId ->
                        navController.navigate(Screen.LocationAnalysis.createRoute(locId))
                    }
                )
            }

            // 6. About Screen
            composable(Screen.About.route) {
                AboutSourcesScreen()
            }

            // 7. Knowledge Terminal Screen
            composable(Screen.GeminiChat.route) {
                MarsKnowledgeTerminalScreen()
            }

            // 8. Weather Screen
            composable(Screen.Weather.route) {
                MarsWeatherScreen(viewModel = viewModel)
            }

            // 9. Audio Guide Screen
            composable(Screen.AudioGuide.route) {
                LandmarksAudioGuideScreen(viewModel = viewModel)
            }

            // 7. Location Analysis Screen
            composable(
                route = Screen.LocationAnalysis.route,
                arguments = listOf(navArgument("locationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val locationId = backStackEntry.arguments?.getString("locationId") ?: ""
                LocationAnalysisScreen(
                    locationId = locationId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlanner = { locId ->
                        navController.navigate(Screen.RoutePlanner.createRoute(startId = locId))
                    }
                )
            }
        }
    }
}
