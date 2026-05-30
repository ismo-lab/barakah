package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BarakahViewModel
import com.example.util.PrayerCalculator
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BarakahViewModel,
    modifier: Modifier = Modifier
) {
    val location by viewModel.currentLocation.collectAsState()
    val locationLabel by viewModel.locationLabel.collectAsState()
    val times by viewModel.prayerTimes.collectAsState()
    val activePrayer by viewModel.activePrayerName.collectAsState()
    val nextPrayerName by viewModel.nextPrayerName.collectAsState()
    val countdown by viewModel.nextPrayerCountdown.collectAsState()

    // Persistent Settings states retrieved from viewmodel
    val currentLang by viewModel.appLanguage.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val useDynamicColor by viewModel.useDynamicColor.collectAsState()
    val amoledDark by viewModel.amoledDark.collectAsState()
    val fontAr by viewModel.arabicFontSize.collectAsState()
    val fontEn by viewModel.englishFontSize.collectAsState()
    val locationMethod by viewModel.locationMethod.collectAsState()

    val isAr = currentLang == "ar"

    // Toast event flow observer
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(currentLang) {
        viewModel.eventFlow.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Location Permission
    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )
    val notifPermissionState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState("android.permission.POST_NOTIFICATIONS")
    } else null

    // Alert settings
    val alertSettings by viewModel.alertSettings.collectAsState()
    
    val prayers = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
    LaunchedEffect(Unit) {
        if (notifPermissionState?.status?.isGranted == false) {
            notifPermissionState.launchPermissionRequest()
        }
    }

    var showSettingsDialog by remember { mutableStateOf(false) }

    // Translations maps
    val prayerTranslations = mapOf(
        "Fajr" to "الفجر",
        "Sunrise" to "الشروق",
        "Dhuhr" to "الظهر",
        "Asr" to "العصر",
        "Maghrib" to "المغرب",
        "Isha" to "العشاء",
        "Sunrise / Duha" to "الشروق / الضحى",
        "Isha (Last Night)" to "عشاء الليلة الماضية"
    )

    fun translatePrayer(name: String): String {
        if (!isAr) return name
        val clean = name.trim()
        return prayerTranslations[clean] ?: clean
    }

    val subtitleTranslations = mapOf(
        "Dawn Prayer" to "صلاة الفجر – أول النهار",
        "Sunrise Shuruq" to "شروق الشمس – وقت الضحى",
        "Midday Prayer" to "صلاة الظهر – زوال الشمس",
        "Afternoon Prayer" to "صلاة العصر – منتصف المساء",
        "Sunset Prayer" to "صلاة المغرب – أول الليل",
        "Night Prayer" to "صلاة العشاء – عمق الليل"
    )

    fun translateSubtitle(sub: String): String {
        if (!isAr) return sub
        return subtitleTranslations[sub] ?: sub
    }

    fun translateHijri(hijriStr: String): String {
        if (!isAr) return hijriStr
        return hijriStr
            .replace("Dhul-Hijjah", "ذو الحجة")
            .replace("Dhul-Qi'dah", "ذو القعدة")
            .replace("Muharram", "المحرم")
            .replace("AH", "هـ")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp, 
            end = 16.dp, 
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
            bottom = 12.dp
        )
    ) {
        // 1. LOCATION SELECTION HEADER (Bold Typography style)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 12.dp)
                    .testTag("location_card"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isAr) locationLabel else locationLabel.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                    }

                    val hijriDateStr = getHijriDateString()
                    Text(
                        text = translateHijri(hijriDateStr),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )

                    val gregorianDateStr = remember(isAr) {
                        SimpleDateFormat("EEE, d MMM", if (isAr) Locale("ar") else Locale.getDefault()).format(Date())
                    }
                    Text(
                        text = gregorianDateStr,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Unified Settings Button replaces individual GPS & manual selectors
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { showSettingsDialog = true }
                        .testTag("settings_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = if (isAr) "الإعدادات" else "Settings",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 2. DYNAMIC EXPRESSIVE COUNTDOWN CARD (Bold Asymmetrical "rounded-[48px_16px_48px_16px]")
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("countdown_card"),
                shape = AbsoluteRoundedCornerShape(topLeft = 48.dp, bottomRight = 48.dp, topRight = 16.dp, bottomLeft = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = if (isAr) "الصلاة القادمة: ${translatePrayer(nextPrayerName)}" else "NEXT: $nextPrayerName".uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Text(
                            text = countdown,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-2.5).sp,
                                lineHeight = 56.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }

        // 3. SEPARATOR / ACTIVE WINDOW TITLE - Only says Prayer Schedule and removes "Active" indicator text
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isAr) "مواقيت الصلاة" else "Prayer Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 4. VERTICAL TIMELINE OF THE 5 DAILY PRAYERS
        val schedule = listOf(
            Triple("Fajr", times.fajr, "Dawn Prayer"),
            Triple("Sunrise", times.sunrise, "Sunrise Shuruq"),
            Triple("Dhuhr", times.dhuhr, "Midday Prayer"),
            Triple("Asr", times.asr, "Afternoon Prayer"),
            Triple("Maghrib", times.maghrib, "Sunset Prayer"),
            Triple("Isha", times.isha, "Night Prayer")
        )

        items(schedule.size) { index ->
            val item = schedule[index]
            val name = item.first
            val time = item.second
            val subtitle = item.third
            val isCurrent = (name == activePrayer || (name == "Sunrise" && activePrayer == "Sunrise / Duha"))

            // Elegant high contrast timeline cards as requested
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("prayer_item_$name"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    },
                    contentColor = if (isCurrent) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                ),
                border = if (isCurrent) {
                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = translatePrayer(name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = translateSubtitle(subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = time,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )

                        val isAlertEnabled = alertSettings.find { it.prayerName == name }?.isEnabled != false
                        IconButton(
                            onClick = {
                                viewModel.togglePrayerAlert(name, !isAlertEnabled)
                            }
                        ) {
                            Icon(
                                imageVector = if (isAlertEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
                                contentDescription = "Toggle alert for $name",
                                tint = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Unified Settings Dialog (Language, Theme, Location auto/manual with search query, Sizing)
    if (showSettingsDialog) {
        var locationQuery by remember { mutableStateOf("") }
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = if (isAr) "الضبط والإعدادات" else "Settings & Preferences",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (isAr) TextAlign.Right else TextAlign.Left
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Language preference
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (isAr) "لغة التطبيق" else "App Language",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = if (isAr) TextAlign.Right else TextAlign.Left
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.setAppLanguage("ar") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (currentLang == "ar") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = if (currentLang == "ar") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Text("العربية", fontWeight = FontWeight.Bold, color = if (currentLang == "ar") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.setAppLanguage("en") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (currentLang == "en") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = if (currentLang == "en") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Text("English", fontWeight = FontWeight.Bold, color = if (currentLang == "en") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }

                        // Divider
                        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }

                        // 2. Theme Preferences
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (isAr) "مظهر التطبيق" else "Theme Preference",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = if (isAr) TextAlign.Right else TextAlign.Left
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.setAppTheme("light") },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (appTheme == "light") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = if (appTheme == "light") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Text(if (isAr) "مضيء" else "Light", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = if (appTheme == "light") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.setAppTheme("dark") },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (appTheme == "dark") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = if (appTheme == "dark") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Text(if (isAr) "مظلم" else "Dark", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = if (appTheme == "dark") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.setAppTheme("system") },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (appTheme == "system") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = if (appTheme == "system") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Text(if (isAr) "تلقائي" else "Auto", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = if (appTheme == "system") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isAr) "مظهر AMOLED أسود بالكامل" else "AMOLED Black Theme",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Switch(
                                        checked = amoledDark,
                                        onCheckedChange = { viewModel.setAmoledDark(it) }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isAr) "الألوان الديناميكية من النظام" else "Dynamic Device Colors",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Switch(
                                        checked = useDynamicColor,
                                        onCheckedChange = { viewModel.setUseDynamicColor(it) }
                                    )
                                }
                            }
                        }

                        // Divider
                        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }

                        // 3. Location Select preferences (select either automatically or manually through search)
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (isAr) "تحديد الموقع الجغرافي" else "Location Settings",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = if (isAr) TextAlign.Right else TextAlign.Left
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isAr) "الموقع الحالي: $locationLabel" else "Selected: $locationLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    textAlign = if (isAr) TextAlign.Right else TextAlign.Left
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.setLocationMethod("auto")
                                            if (locationPermissionState.status.isGranted) {
                                                viewModel.triggerGPSManual()
                                            } else {
                                                locationPermissionState.launchPermissionRequest()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (locationMethod == "auto") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        )
                                    ) {
                                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isAr) "تلقائي (GPS)" else "Auto (GPS)", style = MaterialTheme.typography.bodySmall)
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.setLocationMethod("manual") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (locationMethod == "manual") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        )
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isAr) "يدوي بالبحث" else "Manual Search", style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                if (locationMethod == "manual") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = locationQuery,
                                        onValueChange = { locationQuery = it },
                                        placeholder = { Text(if (isAr) "ابحث عن مدينة..." else "Search city name...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                                    )

                                    val suggestions = if (locationQuery.isBlank()) {
                                        emptyList()
                                    } else {
                                        com.example.data.CityData.cities.filter { city ->
                                            city.name.contains(locationQuery, ignoreCase = true) ||
                                            city.nameAr.contains(locationQuery, ignoreCase = true) ||
                                            city.country.contains(locationQuery, ignoreCase = true) ||
                                            city.countryAr.contains(locationQuery, ignoreCase = true)
                                        }.take(5)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)
                                    ) {
                                        suggestions.forEach { city ->
                                            val displayName = if (isAr) "${city.nameAr}، ${city.countryAr}" else "${city.name}, ${city.country}"
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                                    .clickable {
                                                        viewModel.updateLocation(city.lat, city.lng, if (isAr) city.nameAr else city.name, isSuccessFeedback = true)
                                                        locationQuery = ""
                                                        keyboardController?.hide()
                                                        focusManager.clearFocus()
                                                        showSettingsDialog = false
                                                    }
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(displayName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Divider
                        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }

                        // 4. Custom font sizing of Quran (Moved here from Quran screen!)
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (isAr) "حجم خط آيات السور والمصاحف" else "Quran Text Formatting",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = if (isAr) TextAlign.Right else TextAlign.Left
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = if (isAr) "الخط العربي: " else "Arabic font: ", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(100.dp))
                                    Slider(
                                        value = fontAr,
                                        onValueChange = { viewModel.setArabicFontSize(it) },
                                        valueRange = 18f..40f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(text = "${fontAr.toInt()}sp", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = if (isAr) "الترجمة الإنجليزية: " else "English font: ", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(100.dp))
                                    Slider(
                                        value = fontEn,
                                        onValueChange = { viewModel.setEnglishFontSize(it) },
                                        valueRange = 12f..28f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(text = "${fontEn.toInt()}sp", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                                }
                            }
                        }

                        // Divider
                        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }

                        // 5. About app
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Text(
                                    text = if (isAr) "عن التطبيق" else "About this App",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = if (isAr) TextAlign.Right else TextAlign.Left
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isAr) "هذا التطبيق مجاني بالكامل ومفتوح المصدر (FOSS)، خالي من الإعلانات إلى الأبد، ويحترم خصوصيتك. لقد قمت ببرمجة هذا التطبيق كعمل صالح للتقرب إلى الله، وأرجو من كل من يستخدمه أن يدعو لي بالخير. شكرًا لكم وجزاكم الله خيرًا."
                                            else "This application is fully Free and Open Source Software (FOSS), forever ad-free, and respects your privacy. I made this app for a good deed, and I hope whoever uses it will do dua for me. Thank you and Jazakum Allah Khair.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = if (isAr) TextAlign.Right else TextAlign.Left,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text(if (isAr) "إغلاق" else "Close", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

private fun getHijriDateString(): String {
    val today = Calendar.getInstance()
    val day = today.get(Calendar.DAY_OF_MONTH)
    val month = today.get(Calendar.MONTH) + 1
    val year = today.get(Calendar.YEAR)
    
    // Exact approximate offset for Islamic calendar calculation context in May 2026
    if (year == 2026 && month == 5) {
        val hijriDay = day - 18 + 1
        return if (hijriDay > 0) "$hijriDay Dhul-Hijjah 1447 AH" else "${30 + hijriDay} Dhul-Qi'dah 1447 AH"
    } else if (year == 2026 && month == 6) {
        val hijriDay = day + 13
        return if (hijriDay <= 30) "$hijriDay Dhul-Hijjah 1447 AH" else "${hijriDay - 30} Muharram 1448 AH"
    }
    return "13 Dhul-Hijjah 1447 AH"
}
