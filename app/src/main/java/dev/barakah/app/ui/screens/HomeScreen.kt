package dev.barakah.app.ui.screens

import dev.barakah.app.data.UiStrings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.barakah.app.ui.BarakahViewModel
import dev.barakah.app.ui.WindowWidth
import dev.barakah.app.ui.currentWindowWidth
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import dev.barakah.app.util.NawafilTimes
import dev.barakah.app.util.PrayerCalculator
import dev.barakah.app.util.TimeUtils
import dev.barakah.app.util.localize
import dev.barakah.app.ui.theme.QURAN_FONT_AMIRI
import dev.barakah.app.ui.theme.QURAN_FONT_SYSTEM
import dev.barakah.app.ui.theme.quranFontFamilyFor
import dev.barakah.app.ui.theme.quranLineHeightFor
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BarakahViewModel,
    navController: androidx.navigation.NavHostController,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isShortScreen = configuration.screenHeightDp < 600
    val windowWidth = currentWindowWidth()
    val locationLabel by viewModel.locationLabel.collectAsStateWithLifecycle()
    val times by viewModel.prayerTimes.collectAsStateWithLifecycle()
    val isLocationSet by viewModel.isLocationSet.collectAsStateWithLifecycle()
    val activeHighlight by viewModel.activeHighlightName.collectAsStateWithLifecycle()

    // Persistent Settings states retrieved from viewmodel
    val notifyMorningAdhkar by viewModel.notifyMorningAdhkar.collectAsStateWithLifecycle()
    val notifyEveningAdhkar by viewModel.notifyEveningAdhkar.collectAsStateWithLifecycle()
    val morningAdhkarOffset by viewModel.morningAdhkarOffset.collectAsStateWithLifecycle()
    val eveningAdhkarOffset by viewModel.eveningAdhkarOffset.collectAsStateWithLifecycle()
    val notifyBeforeAdhan by viewModel.notifyBeforeAdhan.collectAsStateWithLifecycle()
    val notifyOccasions by viewModel.notifyOccasions.collectAsStateWithLifecycle()
    val notifyFasting by viewModel.notifyFasting.collectAsStateWithLifecycle()
    val notifyJumuah by viewModel.notifyJumuah.collectAsStateWithLifecycle()
    val notifySuhur by viewModel.notifySuhur.collectAsStateWithLifecycle()
    val notifyIftar by viewModel.notifyIftar.collectAsStateWithLifecycle()
    val currentLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val useDynamicColor by viewModel.useDynamicColor.collectAsStateWithLifecycle()
    val amoledDark by viewModel.amoledDark.collectAsStateWithLifecycle()
    val enableTasbihHaptics by viewModel.enableTasbihHaptics.collectAsStateWithLifecycle()
    val useWesternNumbersInArabic by viewModel.useWesternNumbersInArabic.collectAsStateWithLifecycle()
    val quranFont by viewModel.quranFont.collectAsStateWithLifecycle()
    val locationMethod by viewModel.locationMethod.collectAsStateWithLifecycle()

    val showNawafil by viewModel.showNawafil.collectAsStateWithLifecycle()
    val notifyNawafil by viewModel.notifyNawafil.collectAsStateWithLifecycle()
    val adhanAudioEnabled by viewModel.adhanAudioEnabled.collectAsStateWithLifecycle()
    val adhanAudioMode by viewModel.adhanAudioMode.collectAsStateWithLifecycle()
    val adhanAudioStream by viewModel.adhanAudioStream.collectAsStateWithLifecycle()
    val asrMethod by viewModel.asrMethod.collectAsStateWithLifecycle()
    val ishaMethod by viewModel.ishaMethod.collectAsStateWithLifecycle()
    val adjFajr by viewModel.adjFajr.collectAsStateWithLifecycle()
    val adjSunrise by viewModel.adjSunrise.collectAsStateWithLifecycle()
    val adjDhuhr by viewModel.adjDhuhr.collectAsStateWithLifecycle()
    val adjAsr by viewModel.adjAsr.collectAsStateWithLifecycle()
    val adjMaghrib by viewModel.adjMaghrib.collectAsStateWithLifecycle()
    val adjIsha by viewModel.adjIsha.collectAsStateWithLifecycle()
    val hijriAdjustment by viewModel.hijriAdjustment.collectAsStateWithLifecycle()

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
    val alertSettings by viewModel.alertSettings.collectAsStateWithLifecycle()
    
    val prayers = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")

    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()

    // The Gregorian date in the header is a formatted remember, so it needs a key that rolls over at
    // midnight: a screen left open overnight kept showing yesterday's date. This polls rather than
    // sleeping until midnight, because a delay that long does not run while the phone is asleep.
    var gregorianDayKey by remember { mutableIntStateOf(dayKey()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            gregorianDayKey = dayKey()
        }
    }

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
        "Duha (Nafilah)" to "الضحى",
        "Witr (Nafilah)" to "صلاة الوتر",
        "Qiyam-ul-Layl (Nafilah)" to "قيام الليل",
        "First Third (Nafilah)" to "الثلث الأول - من الليل"
    )

    fun translatePrayer(name: String): String {
        val clean = name.trim()
        if (isAr) {
            return prayerTranslations[clean] ?: clean
        }
        // In English, remove (Nafilah) suffix as the UI badge handles it
        return UiStrings.of(clean.replace("(Nafilah)", "").trim())
    }

    val subtitleTranslations = mapOf(
        "Duha Voluntary Prayer" to "صلاة الضحى – ركعتين نافلة",
        "Tahajjud Night Prayer" to "صلاة التهجد – قيام الليل نافلة",
        "Witr Voluntary Prayer" to "صلاة الوتر – ركعة أو ثلاث خاتمة الصلاة",
        "Islamic Midnight" to "منتصف الليل الإسلامي – بداية قيام الليل"
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

    val schedule = remember(times, showNawafil, isLocationSet) {
        val displayTimes = if (isLocationSet) times else {
            PrayerCalculator.PrayerTimes(
                fajr = "--:--",
                sunrise = "--:--",
                dhuhr = "--:--",
                asr = "--:--",
                maghrib = "--:--",
                isha = "--:--"
            )
        }
        val nawafil = if (isLocationSet) NawafilTimes.schedule(times) else null
        val list = mutableListOf(
            Triple("Fajr", displayTimes.fajr, "Dawn Prayer")
        )
        if (showNawafil) {
            list.add(Triple("Sunrise", displayTimes.sunrise, "Sunrise Shuruq"))
            list.add(Triple("Duha (Nafilah)", nawafil?.duha ?: "--:--", "Duha Voluntary Prayer"))
        }
        list.add(Triple("Dhuhr", displayTimes.dhuhr, "Midday Prayer"))
        list.add(Triple("Asr", displayTimes.asr, "Afternoon Prayer"))
        list.add(Triple("Maghrib", displayTimes.maghrib, "Sunset Prayer"))
        list.add(Triple("Isha", displayTimes.isha, "Night Prayer"))

        if (showNawafil) {
            val witr = nawafil?.let { "${it.witr} - ${displayTimes.fajr}" } ?: "--:--"
            list.add(Triple("Witr (Nafilah)", witr, "Witr Voluntary Prayer"))
            list.add(Triple("First Third (Nafilah)", nawafil?.firstThird ?: "--:--", "First Third of the Night"))
            list.add(Triple("Qiyam-ul-Layl (Nafilah)", nawafil?.qiyam ?: "--:--", "Islamic Midnight"))
            list.add(Triple("Tahajjud (Nafilah)", nawafil?.tahajjud ?: "--:--", "Tahajjud Night Prayer"))
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
                .widthIn(
                    max = when {
                        // A tablet gets a dashboard it can read across, not cards stretched to 1280dp.
                        windowWidth == WindowWidth.Expanded -> 960.dp
                        isLandscape -> 1200.dp
                        else -> 680.dp
                    }
                ),
            contentPadding = PaddingValues(
                start = 16.dp, 
                end = 16.dp, 
                // No window inset here: the Scaffold content already starts below the status bar, and
                // adding it again left a second status bar height of empty space above the date.
                top = 16.dp,
                // The Scaffold content already stops above the bottom nav bar; adding the window inset
                // here counted it twice and left a black band above the bar.
                bottom = 24.dp
            )
        ) {
        // 1. LOCATION SELECTION HEADER (Bold Typography style)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = if (isLandscape || isShortScreen) 4.dp else 12.dp)
                    .testTag("location_card"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = if (isAr) "الموقع الحالي" else UiStrings.of("Current location"),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        val locLabel = viewModel.locationLabel.collectAsStateWithLifecycle().value
                        Text(
                            text = if (isAr) locLabel else locLabel.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                    }

                    val hijriDateStr = getHijriDateString(isAr, hijriAdjustment)
                    Text(
                        text = translateHijri(hijriDateStr).localize(isAr, useWesternNumbersInArabic),
                        style = if (isLandscape || isShortScreen) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )

                    val gregorianDateStr = remember(isAr, useWesternNumbersInArabic, gregorianDayKey) {
                        @Suppress("DEPRECATION")
                        val locale = if (isAr) java.util.Locale("ar") else java.util.Locale.US
                        val sdf = SimpleDateFormat("EEE, d MMM", locale)
                        // Force Western digits in base string for .localize() to work consistently
                        sdf.numberFormat = java.text.NumberFormat.getInstance(java.util.Locale.US)
                        sdf.format(Date()).localize(isAr, useWesternNumbersInArabic)
                    }
                    Text(
                        text = gregorianDateStr,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // The Hijri date above is calculated, not sighted, so it can be a day out where
                    // the user is. Point at the setting that fixes that rather than just caveating.
                    Text(
                        text = (if (isAr) {
                            "تاريخ محسوب — قد يختلف بيوم، ويمكن ضبطه من الإعدادات."
                        } else {
                            UiStrings.of("Calculated — may differ by a day; adjustable in Settings.")
                        }).localize(isAr, useWesternNumbersInArabic),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                // Unified Settings Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.setShowSettingsDialog(true) }
                        .testTag("settings_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = if (isAr) "الإعدادات" else UiStrings.of("Settings"),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 2. DYNAMIC EXPRESSIVE COUNTDOWN CARD
        item {
            Box(modifier = Modifier.padding(vertical = if (isLandscape || isShortScreen) 4.dp else 12.dp)) {
                CountdownCardView(
                    viewModel = viewModel,
                    isAr = isAr,
                    translatePrayer = { translatePrayer(it) }
                )
            }
        }

        // Feature Sections - Grid layout if landscape
        if (isLandscape) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        DailyEssentialsSection(navController, viewModel, isAr)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        ExploreMoreSection(navController, isAr)
                    }
                }
            }
        } else {
            item {
                DailyEssentialsSection(navController, viewModel, isAr)
            }
            item {
                ExploreMoreSection(navController, isAr)
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
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                border = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
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

        // Prayer Schedule Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isLandscape) 4.dp else 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isAr) "مواقيت الصلاة" else UiStrings.of("Prayer Schedule"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 4. VERTICAL TIMELINE OF THE PRAYERS (including Nawafil if enabled)
        items(
            count = schedule.size,
            key = { index -> schedule[index].first }
        ) { index ->
            val item = schedule[index]
            val name = item.first
            val time = item.second
            val subtitle = item.third
            val isNawafil = name.endsWith("(Nafilah)") || name == "Sunrise"
            val isCurrent = name == activeHighlight

            // Elegant high contrast timeline cards as requested
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("prayer_item_$name"),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
                    } else if (isNawafil) {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Prayer Name at Start
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = translatePrayer(name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                            val showNafilahBadge = isNawafil && 
                                    name != "Sunrise" && 
                                    !name.contains("First Third") && 
                                    !name.contains("Midnight")
                            if (showNafilahBadge) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isAr) "نافلة" else UiStrings.of("Nafilah"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Combine Time and Notification Icon at the End
                    val formattedTime = remember(time, isAr, useWesternNumbersInArabic) {
                        TimeUtils.parseDisplayTime(context, time, isAr, useWesternNumbersInArabic)
                    }

                    val hasColon = remember(formattedTime) {
                        formattedTime.digits.contains(":") && !formattedTime.digits.contains("-")
                    }
                    val timeParts = remember(formattedTime, hasColon) {
                        if (hasColon) formattedTime.digits.split(":") else emptyList()
                    }
                    val hours = if (timeParts.size >= 2) timeParts[0] else ""
                    val minutes = if (timeParts.size >= 2) timeParts[1] else ""

                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (hasColon) {
                                    if (isAr) {
                                        if (formattedTime.suffix.isNotEmpty()) {
                                            Text(
                                                text = formattedTime.suffix,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.width(28.dp),
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }

                                        Text(
                                            text = hours,
                                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            softWrap = false,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = ":",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.width(8.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = minutes,
                                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            softWrap = false,
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        Text(
                                            text = hours,
                                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            softWrap = false,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = ":",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.width(8.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = minutes,
                                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            softWrap = false,
                                            textAlign = TextAlign.Center
                                        )

                                        if (formattedTime.suffix.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = formattedTime.suffix,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                softWrap = false,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = formattedTime.digits,
                                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        softWrap = false,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }

                        // Alert Icon
                        val isAlertEnabled = alertSettings.find { it.prayerName == name }?.isEnabled != false
                        IconButton(
                            onClick = {
                                if (!isAlertEnabled && notifPermissionState?.status?.isGranted == false) {
                                    notifPermissionState.launchPermissionRequest()
                                }
                                viewModel.togglePrayerAlert(name, !isAlertEnabled)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isAlertEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
                                contentDescription = UiStrings.format("Toggle alert for %s", name),
                                tint = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
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
        HomeSettingsSection(
            viewModel = viewModel,
            onDismiss = { viewModel.setShowSettingsDialog(false) }
        )
    }
}

private fun getHijriDateString(isAr: Boolean = false, adjustmentDays: Int = 0): String {
    return dev.barakah.app.util.HijriCalendarHelper.getTodayHijriDateString(isAr, adjustmentDays)
}

/** The local calendar day, for keying anything that has to roll over at midnight. */
private fun dayKey(): Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
fun DailyEssentialsSection(navController: androidx.navigation.NavController, viewModel: BarakahViewModel, isAr: Boolean) {
    Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)) {
        Text(
            text = if (isAr) "أذكار اليوم" else UiStrings.of("Daily Essentials"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val morningCat = if (isAr) "أَذْكَارُ الصَّبَاحِ" else UiStrings.of("In the morning and evening")
            val eveningCat = if (isAr) "أَذْكَارُ المَسَاءِ" else UiStrings.of("In the morning and evening")
            
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
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.WbSunny, contentDescription = if (isAr) "أذكار الصباح" else UiStrings.of("Morning Adhkar"), tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isAr) "أذكار الصباح" else UiStrings.of("Morning Adhkar"),
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
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.NightsStay, contentDescription = if (isAr) "أذكار المساء" else UiStrings.of("Evening Adhkar"), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isAr) "أذكار المساء" else UiStrings.of("Evening Adhkar"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ExploreMoreSection(navController: androidx.navigation.NavController, isAr: Boolean) {
    Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)) {
        Text(
            text = if (isAr) "استكشف المزيد" else UiStrings.of("Explore More"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            onClick = {
                navController.navigate("others") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("explore_others_card"),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            border = null
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = if (isAr) "أيقونة أخرى" else UiStrings.of("Others icon"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isAr) "أخرى" else UiStrings.of("Others"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAr) "أسماء الله الحسنى والمناسبات الإسلامية" else UiStrings.of("Allah's Names & Islamic Occasions"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (isAr) "عرض المزيد" else UiStrings.of("View more"),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CountdownCardView(
    viewModel: dev.barakah.app.ui.BarakahViewModel,
    isAr: Boolean,
    translatePrayer: (String) -> String
) {
    val countdown by viewModel.nextPrayerCountdown.collectAsStateWithLifecycle()
    val nextPrayerName by viewModel.nextPrayerName.collectAsStateWithLifecycle()
    val isLocationSet by viewModel.isLocationSet.collectAsStateWithLifecycle()
    val useWesternNumbersInArabic by viewModel.useWesternNumbersInArabic.collectAsStateWithLifecycle()

    val labelText = if (!isLocationSet) {
        if (isAr) "الصلاة القادمة: --" else UiStrings.of("NEXT: --")
    } else {
        if (isAr) "الصلاة القادمة: ${translatePrayer(nextPrayerName)}" else
            UiStrings.format("NEXT: %s", translatePrayer(nextPrayerName)).let {
                if (UiStrings.language == "en") it.uppercase() else it
            }
    }

    val countdownText = remember(countdown, isAr, useWesternNumbersInArabic, isLocationSet) {
        if (!isLocationSet) {
            "--:--:--".localize(isAr, useWesternNumbersInArabic)
        } else {
            try {
                val parts = countdown.replace("-", "").split(":")
                if (parts.size == 3) {
                    val locale = java.util.Locale.US
                    val h = parts[0].toInt()
                    val m = parts[1].toInt()
                    val s = parts[2].toInt()
                    val base = java.lang.String.format(locale, "%02d:%02d:%02d", h, m, s)
                    val prefix = if (countdown.startsWith("-")) "-" else ""
                    (prefix + base).localize(isAr, useWesternNumbersInArabic)
                } else {
                    countdown.localize(isAr, useWesternNumbersInArabic)
                }
            } catch (e: Exception) {
                countdown.localize(isAr, useWesternNumbersInArabic)
            }
        }
    }

    CountdownCardContent(labelText, countdownText)
}

/**
 * The countdown card as it is drawn, with the two lines it shows handed in.
 *
 * The countdown changes every second, so an image of this could only ever be compared against a
 * reference if the values come from outside instead of from a clock behind a ViewModel. Splitting
 * the drawing from the reading is what lets it be held still for a screenshot test.
 */
@Composable
fun CountdownCardContent(labelText: String, countdownText: String) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isShortScreen = configuration.screenHeightDp < 600

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isShortScreen || isLandscape) 8.dp else 16.dp)
            .testTag("countdown_card"),
        shape = androidx.compose.foundation.shape.AbsoluteRoundedCornerShape(topLeft = if (isShortScreen || isLandscape) 40.dp else 56.dp, bottomRight = if (isShortScreen || isLandscape) 40.dp else 56.dp, topRight = 24.dp, bottomLeft = 24.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
        )
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(
                    horizontal = 24.dp,
                    vertical = if (isShortScreen || isLandscape) 14.dp else 28.dp
                )
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = androidx.compose.ui.Alignment.Start
            ) {
                androidx.compose.material3.Text(
                    text = labelText,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                androidx.compose.material3.Text(
                    text = countdownText,
                    style = androidx.compose.material3.MaterialTheme.typography.displayLarge.copy(
                        fontSize = if (isShortScreen || isLandscape) 32.sp else 44.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        letterSpacing = 0.sp,
                        lineHeight = if (isShortScreen || isLandscape) 32.sp else 44.sp,
                        textDirection = TextDirection.Ltr,
                        fontFeatureSettings = "tnum"
                    ),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(bottom = 2.dp),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun AdhkarOffsetSelector(
    currentOffset: Int,
    onOffsetSelected: (Int) -> Unit,
    isAr: Boolean,
    isMorning: Boolean,
    useWesternNumbersInArabic: Boolean
) {
    val offsets = listOf(0, 15, 30, 45, 60, 90, 120)
    val labelBase = if (isMorning) {
        if (isAr) "بعد الفجر بـ" else UiStrings.of("after Fajr")
    } else {
        if (isAr) "بعد العصر بـ" else UiStrings.of("after Asr")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = if (isAr) {
                if (currentOffset == 0) "وقت التنبيه: عند الأذان مباشرة" else "وقت التنبيه: $labelBase ${currentOffset.toString().localize(true, useWesternNumbersInArabic)} دقيقة"
            } else {
                if (currentOffset == 0) UiStrings.of("Reminder Time: Exactly at Adhan") else UiStrings.format("Reminder Time: %s mins %s", currentOffset, labelBase)
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(offsets) { offset ->
                val isSelected = currentOffset == offset
                val chipText = if (offset == 0) {
                    if (isAr) "عند الأذان" else UiStrings.of("At Adhan")
                } else if (offset == 30) {
                    if (isAr) "${30.localize(true, useWesternNumbersInArabic)} د (افتراضي)" else UiStrings.of("30m (Default)")
                } else {
                    if (isAr) "${offset.toString().localize(true, useWesternNumbersInArabic)} د" else UiStrings.format("%s m", offset)
                }

                Surface(
                    onClick = { onOffsetSelected(offset) },
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = chipText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
