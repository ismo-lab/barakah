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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("BarakahCrash", "CRASH in ${thread.name}", throwable)
            // Just let it die
            kotlin.system.exitProcess(1)
        }
        enableEdgeToEdge()
        setContent {
            val viewModel: BarakahViewModel = viewModel()
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
                    
                    if (isFirstRun) {
                        WelcomeScreen(viewModel)
                    } else {
                        MainNavigationContainer(viewModel, navController)
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavigationContainer(
    viewModel: BarakahViewModel,
    navController: NavHostController
) {
    val currentLang by viewModel.appLanguage.collectAsState()
    val isAr = currentLang == "ar"

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
            selectedIcon = Icons.Default.Favorite,
            unselectedIcon = Icons.Outlined.FavoriteBorder,
            tag = "tab_duas"
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = currentDestination?.route == item.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            viewModel.setShowSettingsDialog(false)
                            if (!isSelected) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(250)) },
            exitTransition = { fadeOut(animationSpec = tween(250)) },
            popEnterTransition = { fadeIn(animationSpec = tween(250)) },
            popExitTransition = { fadeOut(animationSpec = tween(250)) }
        ) {
            composable("home") { HomeScreen(viewModel = viewModel) }
            composable("quran") { QuranScreen(viewModel = viewModel) }
            composable("qiblah") { QiblahScreen(viewModel = viewModel) }
            composable("tasbih") { TasbihScreen(viewModel = viewModel) }
            composable("duas") { SupplicationsScreen(viewModel = viewModel) }
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
