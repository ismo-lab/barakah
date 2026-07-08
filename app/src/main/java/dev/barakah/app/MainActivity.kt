package dev.barakah.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.NavHostController
import dev.barakah.app.ui.BarakahViewModel
import dev.barakah.app.ui.screens.*
import dev.barakah.app.ui.theme.BarakahTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

class MainActivity : ComponentActivity() {
    private var activityViewModel: BarakahViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BarakahViewModel = viewModel()
            activityViewModel = viewModel
            val themeMode by viewModel.appTheme.collectAsState()
            val useDynamicColor by viewModel.useDynamicColor.collectAsState()
            val amoledDark by viewModel.amoledDark.collectAsState()
            val appLanguage by viewModel.appLanguage.collectAsState()
 
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemDark
            }
 
            val layoutDirection = if (appLanguage == "ar") {
                androidx.compose.ui.unit.LayoutDirection.Rtl
            } else {
                androidx.compose.ui.unit.LayoutDirection.Ltr
            }
 
            BarakahTheme(
                darkTheme = darkTheme,
                amoledDark = amoledDark,
                dynamicColor = useDynamicColor
            ) {
                CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
                ) {
                    val isFirstRun by viewModel.isFirstRun.collectAsState()
                    val navController = rememberNavController()

                    val showGpsPrompt by viewModel.showGpsSettingsPrompt.collectAsState()
                    val isAr = appLanguage == "ar"

                    if (showGpsPrompt) {
                        AlertDialog(
                            onDismissRequest = { viewModel.setShowGpsSettingsPrompt(false) },
                            title = {
                                Text(
                                    text = if (isAr) "تفعيل خدمات الموقع" else "Enable Location Services",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = if (isAr) 
                                        "لتحديد موقعك تلقائيًا والحصول على مواقيت صلاة دقيقة، يرجى تفعيل خدمات الموقع (GPS) على جهازك." 
                                    else 
                                        "To auto-detect your location and get accurate prayer times, please enable your device's location services (GPS).",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.setShowGpsSettingsPrompt(false)
                                        try {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                            startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                ) {
                                    Text(if (isAr) "تفعيل" else "Enable")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { viewModel.setShowGpsSettingsPrompt(false) }
                                ) {
                                    Text(if (isAr) "إلغاء" else "Cancel")
                                }
                            }
                        )
                    }
                    
                    if (isFirstRun) {
                        WelcomeScreen(viewModel)
                    } else {
                        MainNavigationContainer(viewModel, navController)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activityViewModel?.refreshLocationIfAuto()
    }

    override fun onPause() {
        activityViewModel?.stopLocationUpdates()
        super.onPause()
    }
}

@Composable
fun MainNavigationContainer(
    viewModel: BarakahViewModel,
    navController: NavHostController
) {
    val currentLang by viewModel.appLanguage.collectAsState()
    val isAr = currentLang == "ar"
    
    var screenKeys by remember { mutableStateOf(mapOf<String, Int>()) }

    val navItems = listOf(
        NavigationTabItem(
            route = "home",
            label = if (isAr) "الرئيسية" else "Home",
            selectedIcon = Icons.Default.Home,
            unselectedIcon = Icons.Outlined.Home,
            tag = "tab_home"
        ),
        NavigationTabItem(
            route = "quran",
            label = if (isAr) "القرآن" else "Quran",
            selectedIcon = Icons.Default.MenuBook,
            unselectedIcon = Icons.Outlined.MenuBook,
            tag = "tab_quran"
        ),
        NavigationTabItem(
            route = "qiblah",
            label = if (isAr) "القبلة" else "Qiblah",
            selectedIcon = Icons.Default.Explore,
            unselectedIcon = Icons.Outlined.Explore,
            tag = "tab_qiblah"
        ),
        NavigationTabItem(
            route = "tasbih",
            label = if (isAr) "التسبيح" else "Tasbih",
            selectedIcon = Icons.Default.ControlPoint,
            unselectedIcon = Icons.Outlined.ControlPoint,
            tag = "tab_tasbih"
        ),
        NavigationTabItem(
            route = "duas",
            label = if (isAr) "الأدعية" else "Duas",
            selectedIcon = Icons.Default.Star,
            unselectedIcon = Icons.Outlined.Star,
            tag = "tab_duas"
        )
    )

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()

    val shouldHideNavigation = showSettingsDialog || currentDestination?.route == "others"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = if (shouldHideNavigation) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        bottomBar = {
            if (!isLandscape && !shouldHideNavigation) {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar")
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentDestination?.route == item.route && !(item.route == "home" && showSettingsDialog)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                viewModel.setShowSettingsDialog(false)
                                if (!isSelected) {
                                    screenKeys = screenKeys.toMutableMap().apply { this[item.route] = (this[item.route] ?: 0) + 1 }
                                    viewModel.resetScreenState(item.route)
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = false }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                }
                            },
                            label = { Text(text = item.label, style = MaterialTheme.typography.labelSmall) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            modifier = Modifier.testTag(item.tag)
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
            if (isLandscape && !shouldHideNavigation) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("side_navigation_rail")
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    navItems.forEachIndexed { index, item ->
                        val isSelected = currentDestination?.route == item.route && !(item.route == "home" && showSettingsDialog)
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                viewModel.setShowSettingsDialog(false)
                                if (!isSelected) {
                                    screenKeys = screenKeys.toMutableMap().apply { this[item.route] = (this[item.route] ?: 0) + 1 }
                                    viewModel.resetScreenState(item.route)
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = false }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                }
                            },
                            label = { Text(text = item.label, style = MaterialTheme.typography.labelSmall) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            modifier = Modifier.testTag(item.tag)
                        )
                        if (index < navItems.size - 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            val contentModifier = if (isLandscape && !shouldHideNavigation) {
                Modifier.weight(1f).fillMaxSize()
            } else {
                Modifier.fillMaxSize()
            }

            Box(modifier = contentModifier) {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { fadeIn(animationSpec = tween(250)) },
                    exitTransition = { fadeOut(animationSpec = tween(250)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(250)) },
                    popExitTransition = { fadeOut(animationSpec = tween(250)) }
                ) {
                    composable("home") { 
                        androidx.compose.runtime.key(screenKeys["home"]) {
                            HomeScreen(viewModel = viewModel, navController = navController)
                        } 
                    }
                    composable("quran") { 
                        androidx.compose.runtime.key(screenKeys["quran"]) {
                            QuranScreen(viewModel = viewModel)
                        } 
                    }
                    composable("qiblah") { 
                        androidx.compose.runtime.key(screenKeys["qiblah"]) {
                            QiblahScreen(viewModel = viewModel)
                        } 
                    }
                    composable("tasbih") { 
                        androidx.compose.runtime.key(screenKeys["tasbih"]) {
                            TasbihScreen(viewModel = viewModel)
                        } 
                    }
                    composable("duas") { 
                        androidx.compose.runtime.key(screenKeys["duas"]) {
                            SupplicationsScreen(viewModel = viewModel)
                        } 
                    }
                    composable("others") { 
                        androidx.compose.runtime.key(screenKeys["others"]) {
                            OthersScreen(viewModel = viewModel, navController = navController)
                        } 
                    }
                }
            }
        }
    }
}

data class NavigationTabItem(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val tag: String
)
