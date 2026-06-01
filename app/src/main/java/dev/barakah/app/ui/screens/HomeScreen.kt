package dev.barakah.app.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.*
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
import dev.barakah.app.ui.BarakahViewModel
import dev.barakah.app.util.PrayerCalculator
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*

fun formatDisplayTime(context: android.content.Context, timeStr: String, isAr: Boolean = false): String {
    return try {
        val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        val parts = timeStr.trim().split(":")
        val h = parts[0].toInt()
        val m = parts[1].split(" ")[0].trim().toInt()
        
        if (is24Hour) {
            String.format(Locale.getDefault(), "%02d:%02d", h, m)
        } else {
            val hour12 = if (h % 12 == 0) 12 else h % 12
            val amPm = if (h < 12) {
                if (isAr) "ص" else "AM"
            } else {
                if (isAr) "م" else "PM"
            }
            String.format(Locale.getDefault(), "%02d:%02d %s", hour12, m, amPm)
        }
    } catch (e: Exception) {
        timeStr
    }
}

fun calculateOffsetTime(timeStr: String, offsetMinutes: Int): String {
    return try {
        val parts = timeStr.trim().split(":")
        val h = parts[0].toInt()
        val m = parts[1].split(" ")[0].trim().toInt()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            add(Calendar.MINUTE, offsetMinutes)
        }
        val nh = cal.get(Calendar.HOUR_OF_DAY)
        val nm = cal.get(Calendar.MINUTE)
        String.format(Locale.US, "%02d:%02d", nh, nm)
    } catch (e: Exception) {
        timeStr
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BarakahViewModel,
    navController: androidx.navigation.NavHostController,
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
    val enableAdhanSound by viewModel.enableAdhanSound.collectAsState()
    val adhanSoundType by viewModel.adhanSoundType.collectAsState()
    val fontAr by viewModel.arabicFontSize.collectAsState()
    val fontEn by viewModel.englishFontSize.collectAsState()
    val locationMethod by viewModel.locationMethod.collectAsState()

    val showNawafil by viewModel.showNawafil.collectAsState()
    val asrMethod by viewModel.asrMethod.collectAsState()
    val ishaMethod by viewModel.ishaMethod.collectAsState()
    val adjFajr by viewModel.adjFajr.collectAsState()
    val adjSunrise by viewModel.adjSunrise.collectAsState()
    val adjDhuhr by viewModel.adjDhuhr.collectAsState()
    val adjAsr by viewModel.adjAsr.collectAsState()
    val adjMaghrib by viewModel.adjMaghrib.collectAsState()
    val adjIsha by viewModel.adjIsha.collectAsState()

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

    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()

    androidx.activity.compose.BackHandler(enabled = showSettingsDialog) {
        viewModel.setShowSettingsDialog(false)
    }

    // Translations maps
    val prayerTranslations = mapOf(
        "Fajr" to "الفجر",
        "Sunrise" to "الشروق",
        "Dhuhr" to "الظهر",
        "Asr" to "العصر",
        "Maghrib" to "المغرب",
        "Isha" to "العشاء",
        "Sunrise / Duha" to "الشروق / الضحى",
        "Isha (Last Night)" to "عشاء الليلة الماضية",
        "Tahajjud (Nafilah)" to "التهجد",
        "Duha (Nafilah)" to "صلاة الضحى",
        "Witr (Nafilah)" to "الوتر",
        "Qiyam-ul-Layl (Nafilah)" to "قيام الليل"
    )

    fun translatePrayer(name: String): String {
        if (!isAr) return name
        val clean = name.trim()
        return prayerTranslations[clean] ?: clean
    }

    val subtitleTranslations = mapOf(
        "Duha Voluntary Prayer" to "صلاة الضحى – ركعتين نافلة",
        "Tahajjud Night Prayer" to "صلاة التهجد – قيام الليل نافلة",
        "Witr Voluntary Prayer" to "صلاة الوتر – ركعة أو ثلاث خاتمة الصلاة",
        "Late Night Prayer" to "صلاة قيام الليل – تفضل في أول الليل أو وسطه"
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

    val schedule = remember(times, showNawafil) {
        val list = mutableListOf(
            Triple("Fajr", times.fajr, "Dawn Prayer"),
            Triple("Sunrise", times.sunrise, "Sunrise Shuruq")
        )
        if (showNawafil) {
            val duhaTime = calculateOffsetTime(times.sunrise, 20)
            list.add(Triple("Duha (Nafilah)", duhaTime, "Duha Voluntary Prayer"))
        }
        list.add(Triple("Dhuhr", times.dhuhr, "Midday Prayer"))
        list.add(Triple("Asr", times.asr, "Afternoon Prayer"))
        list.add(Triple("Maghrib", times.maghrib, "Sunset Prayer"))
        list.add(Triple("Isha", times.isha, "Night Prayer"))
        if (showNawafil) {
            val witrTime = calculateOffsetTime(times.isha, 45)
            list.add(Triple("Witr (Nafilah)", witrTime, "Witr Voluntary Prayer"))

            val tahajjudTime = calculateOffsetTime(times.fajr, -90)
            list.add(0, Triple("Tahajjud (Nafilah)", tahajjudTime, "Tahajjud Night Prayer"))

            val qiyamTime = calculateOffsetTime(times.fajr, -150)
            list.add(0, Triple("Qiyam-ul-Layl (Nafilah)", qiyamTime, "Late Night Prayer"))
        }
        list
    }

    if (!showSettingsDialog) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(
                start = 16.dp, 
                end = 16.dp, 
                top = 12.dp,
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
                        @Suppress("DEPRECATION")
                        val locale = if (isAr) Locale("ar") else Locale.getDefault()
                        SimpleDateFormat("EEE, d MMM", locale).format(Date())
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
                        .clickable { viewModel.setShowSettingsDialog(true) }
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

        // 2.5 DAILY ESSENTIALS SECTION
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)) {
                Text(
                    text = if (isAr) "أذكار اليوم" else "Daily Essentials",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val morningCat = if (isAr) "أَذْكَارُ الصَّبَاحِ" else "In the morning and evening"
                    val eveningCat = if (isAr) "أَذْكَارُ المَسَاءِ" else "In the morning and evening"
                    
                    // Morning Card
                    Card(
                        onClick = {
                            viewModel.selectDuaCategory(morningCat)
                            navController.navigate("duas") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isAr) "أذكار الصباح" else "Morning Adhkar",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Evening Card
                    Card(
                        onClick = {
                            viewModel.selectDuaCategory(eveningCat)
                            navController.navigate("duas") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.NightsStay, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isAr) "أذكار المساء" else "Evening Adhkar",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 2.7 INSPIRATIONAL QUOTE CARD
        item {
            val inspirations = remember(isAr) {
                if (isAr) listOf(
                    "«لَئِن شَكَرْتُمْ لَأَزِيدَنَّكُمْ»" to "سورة إبراهيم، ٧",
                    "«فَإِنِّي قَرِيبٌ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ»" to "سورة البقرة، ١٨٦",
                    "«وَبَشِّرِ الصَّابِرينَ»" to "سورة البقرة، ١٥٥",
                    "«لَا تَدْرِي لَعَلَّ اللَّهَ يُحْدِثُ بَعْدَ ذَلِكَ أَمْرًا»" to "سورة الطلاق، ١",
                    "«سَيَجْعَلُ اللَّهُ بَعْدَ عُسْرٍ يُسْرًا»" to "سورة الطلاق، ٧"
                ) else listOf(
                    "\"If you are grateful, I will surely increase you.\"" to "Surah Ibrahim, 7",
                    "\"Indeed, I am near. I respond to the invocation of the supplicant.\"" to "Surah Al-Baqarah, 186",
                    "\"And give good tidings to the patient.\"" to "Surah Al-Baqarah, 155",
                    "\"You know not, perhaps Allah will bring about thereafter a [new] matter.\"" to "Surah At-Talaq, 1",
                    "\"Allah will bring about, after hardship, ease.\"" to "Surah At-Talaq, 7"
                )
            }
            val quote = remember { inspirations.random() }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = quote.first,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            lineHeight = 28.sp
                        ),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = quote.second,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
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

        // 4. VERTICAL TIMELINE OF THE PRAYERS (including Nawafil if enabled)
        items(schedule.size) { index ->
            val item = schedule[index]
            val name = item.first
            val time = item.second
            val subtitle = item.third
            val isNawafil = name.endsWith("(Nafilah)")
            val isCurrent = !isNawafil && (name == activePrayer || (name == "Sunrise" && activePrayer == "Sunrise / Duha"))

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
                    } else if (isNawafil) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
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
                } else if (isNawafil) {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = translatePrayer(name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                            if (isNawafil) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isAr) "نافلة" else "Nafilah",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = formatDisplayTime(context, time, isAr),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )

                        val isAlertEnabled = alertSettings.find { it.prayerName == name }?.isEnabled != false
                        IconButton(
                            onClick = {
                                if (!isAlertEnabled && notifPermissionState?.status?.isGranted == false) {
                                    notifPermissionState.launchPermissionRequest()
                                }
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
    }
    }

    // Unified Settings Section (Language, Theme, Location auto/manual with search query, Sizing)
    if (showSettingsDialog) {
        val layoutDirection = remember(currentLang) {
            if (currentLang == "ar") androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
        }
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection) {
            var locationQuery by remember { mutableStateOf("") }
            val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.setShowSettingsDialog(false) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "الضبط والإعدادات" else "Settings & Preferences",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        // Card 1: Language & Theme Preferences
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isAr) "اللغة ومظهر التطبيق" else "Language & Theme",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Text(
                                        text = if (isAr) "لغة التطبيق" else "App Language",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.setAppLanguage("ar") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (currentLang == "ar") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (currentLang == "ar") 2.dp else 1.dp,
                                                color = if (currentLang == "ar") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
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
                                                width = if (currentLang == "en") 2.dp else 1.dp,
                                                color = if (currentLang == "en") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Text("English", fontWeight = FontWeight.Bold, color = if (currentLang == "en") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    Text(
                                        text = if (isAr) "الوضع العام" else "Dark / Light Mode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("light" to (if (isAr) "مضيء" else "Light"), "dark" to (if (isAr) "مظلم" else "Dark"), "system" to (if (isAr) "تلقائي" else "System")).forEach { (mode, flag) ->
                                            OutlinedButton(
                                                onClick = { viewModel.setAppTheme(mode) },
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 4.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (appTheme == mode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = if (appTheme == mode) 2.dp else 1.dp,
                                                    color = if (appTheme == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                )
                                            ) {
                                                Text(flag, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = if (appTheme == mode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isAr) "مظهر AMOLED أسود بالكامل" else "AMOLED Black Theme",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Switch(
                                            checked = amoledDark,
                                            onCheckedChange = { viewModel.setAmoledDark(it) }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isAr) "الألوان الديناميكية من النظام" else "Dynamic Device Colors",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Switch(
                                            checked = useDynamicColor,
                                            onCheckedChange = { viewModel.setUseDynamicColor(it) }
                                        )
                                    }
                                }
                            }
                        }

                        // Card 2: Alerts & Voluntary Prayers (Nawafil)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isAr) "التنبيهات والصلوات المستحبة" else "Notifications & Voluntary",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isAr) "عرض النوافل وقيام الليل" else "Show Voluntary Prayers",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isAr) "تضمين الضحى وقيام الليل والوتر والتهجد لجدول الصلوات" else "Include Duha, Qiyam-ul-Layl, Witr & Tahajjud in schedule",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = showNawafil,
                                            onCheckedChange = { viewModel.setShowNawafil(it) }
                                        )
                                    }
                                }
                            }
                        }

                        // Card 3: Location Settings
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isAr) "تحديد الموقع الجغرافي" else "Location Settings",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Text(
                                        text = if (isAr) "الموقع الحالي: $locationLabel" else "Selected: $locationLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )

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
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (locationMethod == "auto") 2.dp else 1.dp,
                                                color = if (locationMethod == "auto") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
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
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (locationMethod == "manual") 2.dp else 1.dp,
                                                color = if (locationMethod == "manual") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isAr) "يدوي بالبحث" else "Manual Search", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    if (locationMethod == "manual") {
                                        OutlinedTextField(
                                            value = locationQuery,
                                            onValueChange = { locationQuery = it },
                                            placeholder = { Text(if (isAr) "ابحث عن مدينة..." else "Search city name...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        val suggestions = if (locationQuery.isBlank()) {
                                            emptyList()
                                        } else {
                                            dev.barakah.app.data.CityData.cities.filter { city ->
                                                city.name.contains(locationQuery, ignoreCase = true) ||
                                                city.nameAr.contains(locationQuery, ignoreCase = true) ||
                                                city.country.contains(locationQuery, ignoreCase = true) ||
                                                city.countryAr.contains(locationQuery, ignoreCase = true)
                                            }.take(5)
                                        }

                                        if (suggestions.isNotEmpty()) {
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
                                                                viewModel.setShowSettingsDialog(false)
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
                            }
                        }

                        // Card 4: Juristic & School Options (Shafi'i vs Hanafi)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isAr) "المذهب والحساب الفقهي" else "Juristic Calculations",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Text(
                                        text = if (isAr) "صلاة العصر: وقت المذهب الشافعي أم الحنفي" else "Asr Prayer: Shafi'i vs Hanafi School",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.setAsrMethod("standard") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (asrMethod == "standard") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (asrMethod == "standard") 2.dp else 1.dp,
                                                color = if (asrMethod == "standard") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Text(if (isAr) "الجمهور (شافعي، مالكي...)" else "Standard (Shafi'i)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.setAsrMethod("hanafi") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (asrMethod == "hanafi") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (asrMethod == "hanafi") 2.dp else 1.dp,
                                                color = if (asrMethod == "hanafi") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Text(if (isAr) "حنفي" else "Hanafi", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    Text(
                                        text = if (isAr) "صلاة العشاء: التقدير الفلكي أم الحنفي (زاوية 18°)" else "Isha Prayer: Standard Angle vs Hanafi Angle (18°)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.setIshaMethod("standard") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (ishaMethod == "standard") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (ishaMethod == "standard") 2.dp else 1.dp,
                                                color = if (ishaMethod == "standard") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Text(if (isAr) "طريقة الحساب فلكياً" else "Calculation", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.setIshaMethod("hanafi") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (ishaMethod == "hanafi") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (ishaMethod == "hanafi") 2.dp else 1.dp,
                                                color = if (ishaMethod == "hanafi") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Text(if (isAr) "حنفي (18 درجة)" else "Hanafi Angle (18°)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Card 5: Manual Adjustments
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isAr) "تعديل الدقائق يدويًّا" else "Manual Adjustments",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Text(
                                        text = if (isAr) "أضف أو نقص دقائق مخصصة لكل صلاة حسب مدينتك بدقة" else "Add or deduct minutes manually for each prayer calculation",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    val adjList = listOf(
                                        Quadruple("Fajr", if (isAr) "الفجر" else "Fajr", adjFajr) { v: Int -> viewModel.setAdjFajr(v) },
                                        Quadruple("Sunrise", if (isAr) "الشروق" else "Sunrise", adjSunrise) { v: Int -> viewModel.setAdjSunrise(v) },
                                        Quadruple("Dhuhr", if (isAr) "الظهر" else "Dhuhr", adjDhuhr) { v: Int -> viewModel.setAdjDhuhr(v) },
                                        Quadruple("Asr", if (isAr) "العصر" else "Asr", adjAsr) { v: Int -> viewModel.setAdjAsr(v) },
                                        Quadruple("Maghrib", if (isAr) "المغرب" else "Maghrib", adjMaghrib) { v: Int -> viewModel.setAdjMaghrib(v) },
                                        Quadruple("Isha", if (isAr) "العشاء" else "Isha", adjIsha) { v: Int -> viewModel.setAdjIsha(v) }
                                    )

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        adjList.forEach { adjItem ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = adjItem.second,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.width(80.dp)
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = { adjItem.fourth(adjItem.third - 1) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Remove, contentDescription = "Deduct minute", modifier = Modifier.size(16.dp))
                                                    }
                                                    val prefix = if (adjItem.third > 0) "+" else ""
                                                    Text(
                                                        text = "$prefix${adjItem.third} m",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.width(50.dp)
                                                    )
                                                    IconButton(
                                                        onClick = { adjItem.fourth(adjItem.third + 1) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = "Add minute", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Card 6: Quran Font Formatting
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isAr) "تنسيق خط المصاحف والسور" else "Quran Font Scale",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = if (isAr) "الخط العربي: " else "Arabic: ", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(100.dp))
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
                                        Text(text = if (isAr) "الترجمة الإنجليزية: " else "English: ", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(100.dp))
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
                        }

                        // Card 8: About App
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isAr) "عن التطبيق" else "About this App",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Text(
                                        text = if (isAr) "هذا التطبيق مجاني بالكامل ومفتوح المصدر (FOSS)، خالي من الإعلانات إلى الأبد، ويحترم خصوصيتك. لقد قمت ببرمجة هذا التطبيق كعمل صالح للتقرب إلى الله، وأرجو من كل من يستخدمه أن يدعو لي بالخير. شكرًا لكم وجزاكم الله خيرًا."
                                                else "This application is fully Free and Open Source Software (FOSS), forever ad-free, and respects your privacy. I made this app for a good deed, and I hope whoever uses it will do dua for me. Thank you and Jazakum Allah Khair.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )

                                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                                    OutlinedButton(
                                        onClick = { uriHandler.openUri("https://github.com/ismo-lab/barakah") },
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isAr) "رمز المصدر (GitHub)" else "Source Code (GitHub)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(
                                onClick = { viewModel.resetToDefaults() },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Settings",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isAr) "إعادة الضبط الافتراضي" else "Reset to Defaults",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
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

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
