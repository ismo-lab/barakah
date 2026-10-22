package dev.barakah.app

import dev.barakah.app.data.UiLanguages
import dev.barakah.app.data.UiStrings

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import dev.barakah.app.util.AppNightMode
import dev.barakah.app.ui.currentWindowWidth
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

/**
 * The navigation bar scrims androidx.activity uses by default (it keeps them internal). They only
 * matter below API 29, where the navigation bar cannot be transparent. The dark one is also what
 * pre-API-26 devices need, since their navigation icons are always light.
 */
private val NAV_BAR_LIGHT_SCRIM = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val NAV_BAR_DARK_SCRIM = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    private var activityViewModel: BarakahViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // The splash is drawn by the system before this activity exists, so the app's theme goes to
        // the system on the way in as well as whenever the setting changes.
        AppNightMode.applyStored(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BarakahViewModel = viewModel()
            activityViewModel = viewModel
            val themeMode by viewModel.appTheme.collectAsStateWithLifecycle()
            val useDynamicColor by viewModel.useDynamicColor.collectAsStateWithLifecycle()
            val amoledDark by viewModel.amoledDark.collectAsStateWithLifecycle()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
 
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemDark
            }
 
            // The system bar icons follow the app's theme, not the phone's. By default
            // enableEdgeToEdge() reads the system's light/dark mode, so a dark app on a light phone
            // gets dark status bar icons that disappear into our own dark background (and the light
            // app on a dark phone the reverse). The call in onCreate sets edge-to-edge up before the
            // first frame; this re-applies the style whenever the in-app theme changes.
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        NAV_BAR_LIGHT_SCRIM,
                        NAV_BAR_DARK_SCRIM
                    ) { darkTheme }
                )
            }

            val layoutDirection = if (UiLanguages.isRtl(appLanguage)) {
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
                    val isFirstRun by viewModel.isFirstRun.collectAsStateWithLifecycle()
                    val navController = rememberNavController()

                    val showGpsPrompt by viewModel.showGpsSettingsPrompt.collectAsStateWithLifecycle()
                    val isAr = appLanguage == "ar"

                    if (showGpsPrompt) {
                        AlertDialog(
                            onDismissRequest = { viewModel.setShowGpsSettingsPrompt(false) },
                            title = {
                                Text(
                                    text = if (isAr) "تفعيل خدمات الموقع" else UiStrings.of("Enable Location Services"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = if (isAr) 
                                        "لتحديد موقعك تلقائيًا والحصول على مواقيت صلاة دقيقة، يرجى تفعيل خدمات الموقع (GPS) على جهازك." 
                                    else 
                                        UiStrings.of("To auto-detect your location and get accurate prayer times, please enable your device's location services (GPS)."),
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
                                            Log.e(TAG, "Unable to open the location settings screen", e)
                                        }
                                    }
                                ) {
                                    Text(if (isAr) "تفعيل" else UiStrings.of("Enable"))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { viewModel.setShowGpsSettingsPrompt(false) }
                                ) {
                                    Text(if (isAr) "إلغاء" else UiStrings.of("Cancel"))
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

    override fun onStop() {
        super.onStop()
        activityViewModel?.refreshWidgetsWhenAppStops()
    }
}

@Composable
fun MainNavigationContainer(
    viewModel: BarakahViewModel,
    navController: NavHostController
) {
    val currentLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"
    
    var screenKeys by remember { mutableStateOf(mapOf<String, Int>()) }

    val navItems = listOf(
        NavigationTabItem(
            route = "home",
            label = if (isAr) "الرئيسية" else UiStrings.of("Home"),
            selectedIcon = Icons.Default.Home,
            unselectedIcon = Icons.Outlined.Home,
            tag = "tab_home"
        ),
        NavigationTabItem(
            route = "quran",
            label = if (isAr) "القرآن" else UiStrings.of("Quran"),
            selectedIcon = Icons.Default.MenuBook,
            unselectedIcon = Icons.Outlined.MenuBook,
            tag = "tab_quran"
        ),
        NavigationTabItem(
            route = "qiblah",
            label = if (isAr) "القبلة" else UiStrings.of("Qiblah"),
            selectedIcon = Icons.Default.Explore,
            unselectedIcon = Icons.Outlined.Explore,
            tag = "tab_qiblah"
        ),
        NavigationTabItem(
            route = "tasbih",
            label = if (isAr) "التسبيح" else UiStrings.of("Tasbih"),
            selectedIcon = Icons.Default.ControlPoint,
            unselectedIcon = Icons.Outlined.ControlPoint,
            tag = "tab_tasbih"
        ),
        NavigationTabItem(
            route = "duas",
            label = if (isAr) "الأدعية" else UiStrings.of("Duas"),
            selectedIcon = Icons.Default.Star,
            unselectedIcon = Icons.Outlined.Star,
            tag = "tab_duas"
        )
    )

    val windowWidth = currentWindowWidth()
    // Five labelled items do not fit a landscape or split-screen window at full spacing, and the rail
    // cannot centre itself in one either: it used to cut the last item off the bottom edge. A short
    // window drops the centring spacers, tightens the spacing and lets the rail scroll instead.
    val shortRail = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 560
    val railItemSpacing = if (shortRail) 4.dp else 16.dp
    val railScroll = rememberScrollState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()

    // Settings replaces the rail with the bottom bar: it is an overlay that applies its own insets.
    // Others used to do the same, which left it on a bottom bar in a wide window while every other
    // screen had the rail. It is an ordinary screen, so it keeps the rail.
    val showNavigationRail = windowWidth.showsNavigationRail && !showSettingsDialog

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // safeDrawing rather than the Scaffold default (systemBars): on a phone held in landscape the
        // display cutout, the camera, is on a side edge, and systemBars does not cover it, so every
        // screen laid its content under the camera. The whole app is inset once, here; the screens
        // must not ask again. Settings is the exception: it draws its own top inset while the bottom
        // bar is still on screen, so it takes the horizontal sides only.
        contentWindowInsets = if (showSettingsDialog) {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
        } else {
            WindowInsets.safeDrawing
        },
        bottomBar = {
            if (!showNavigationRail) {
                ShortNavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentDestination?.route == item.route && !(item.route == "home" && showSettingsDialog)
                        ShortNavigationBarItem(
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
            if (showNavigationRail) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .then(if (shortRail) Modifier.verticalScroll(railScroll) else Modifier)
                        .testTag("side_navigation_rail")
                ) {
                    if (!shortRail) Spacer(modifier = Modifier.weight(1f))
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
                            Spacer(modifier = Modifier.height(railItemSpacing))
                        }
                    }
                    if (!shortRail) Spacer(modifier = Modifier.weight(1f))
                }
            }

            val contentModifier = if (showNavigationRail) {
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
