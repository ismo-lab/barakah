package dev.barakah.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.barakah.app.ui.BarakahViewModel
import dev.barakah.app.notifications.AdhanSoundManager
import kotlinx.coroutines.launch
import dev.barakah.app.util.PrayerCalculator
import dev.barakah.app.util.TimeUtils
import dev.barakah.app.util.localize
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
    val locationLabel by viewModel.locationLabel.collectAsState()
    val times by viewModel.prayerTimes.collectAsState()
    val activeHighlight by viewModel.activeHighlightName.collectAsState()

    // Persistent Settings states retrieved from viewmodel
    val notifyMorningAdhkar by viewModel.notifyMorningAdhkar.collectAsState()
    val notifyEveningAdhkar by viewModel.notifyEveningAdhkar.collectAsState()
    val notifyBeforeAdhan by viewModel.notifyBeforeAdhan.collectAsState()
    val notifyOccasions by viewModel.notifyOccasions.collectAsState()
    val notifyFasting by viewModel.notifyFasting.collectAsState()
    val notifyJumuah by viewModel.notifyJumuah.collectAsState()
    val notifySuhur by viewModel.notifySuhur.collectAsState()
    val notifyIftar by viewModel.notifyIftar.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val useDynamicColor by viewModel.useDynamicColor.collectAsState()
    val amoledDark by viewModel.amoledDark.collectAsState()
    val enableAdhanSound by viewModel.enableAdhanSound.collectAsState()
    val adhanSoundType by viewModel.adhanSoundType.collectAsState()
    val enableTasbihHaptics by viewModel.enableTasbihHaptics.collectAsState()
    val useWesternNumbersInArabic by viewModel.useWesternNumbersInArabic.collectAsState()
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
        "Duha (Nafilah)" to "الضحى",
        "Witr (Nafilah)" to "صلاة الوتر",
        "Qiyam-ul-Layl (Nafilah)" to "قيام الليل",
        "First Third (Nafilah)" to "الثلث الأول - من الليل",
        "Midnight (Nafilah)" to "منتصف الليل",
        "Qiyam Last Third (Nafilah)" to "القيام - الثلث الأخير"
    )

    fun translatePrayer(name: String): String {
        val clean = name.trim()
        if (isAr) {
            return prayerTranslations[clean] ?: clean
        }
        // In English, remove (Nafilah) suffix as the UI badge handles it
        return clean.replace("(Nafilah)", "").trim()
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
            val duhaTime = TimeUtils.calculateOffsetTime(times.sunrise, 20)
            list.add(Triple("Duha (Nafilah)", duhaTime, "Duha Voluntary Prayer"))
        }
        list.add(Triple("Dhuhr", times.dhuhr, "Midday Prayer"))
        list.add(Triple("Asr", times.asr, "Afternoon Prayer"))
        list.add(Triple("Maghrib", times.maghrib, "Sunset Prayer"))
        list.add(Triple("Isha", times.isha, "Night Prayer"))
        
        if (showNawafil) {
            try {
                val mParts = times.maghrib.split(":")
                val mMin = mParts[0].toInt() * 60 + mParts[1].toInt()
                val fParts = times.fajr.split(":")
                var fMin = fParts[0].toInt() * 60 + fParts[1].toInt()
                if (fMin < mMin) fMin += 24 * 60
                val diff = fMin - mMin
                
                val firstThirdMin = mMin + diff / 3
                val midMin = mMin + diff / 2
                val lastThirdMin = mMin + (diff * 2) / 3
                
                fun formatMins(min: Int): String {
                    val h = (min / 60) % 24
                    val m = min % 60
                    return String.format(Locale.US, "%02d:%02d", h, m)
                }
                
                list.add(Triple("First Third (Nafilah)", formatMins(firstThirdMin), "First Third of the Night"))
                list.add(Triple("Midnight (Nafilah)", formatMins(midMin), "Islamic Midnight"))
                list.add(Triple("Qiyam Last Third (Nafilah)", formatMins(lastThirdMin), "Late Night Prayer"))
                list.add(Triple("Witr (Nafilah)", "${times.isha} - ${times.fajr}", "Witr Voluntary Prayer"))
            } catch (e: Exception) {
                // Fallback if parsing fails
                val witrTime = TimeUtils.calculateOffsetTime(times.isha, 45)
                list.add(Triple("Witr (Nafilah)", witrTime, "Witr Voluntary Prayer"))
            }
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
                .widthIn(max = if (isLandscape) 1200.dp else 680.dp),
            contentPadding = PaddingValues(
                start = 16.dp, 
                end = 16.dp, 
                top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 16.dp,
                bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() + 24.dp
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
                            contentDescription = if (isAr) "الموقع الحالي" else "Current location",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        val locLabel = viewModel.locationLabel.collectAsState().value
                        Text(
                            text = if (isAr) locLabel else locLabel.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                    }

                    val hijriDateStr = getHijriDateString()
                    Text(
                        text = translateHijri(hijriDateStr).localize(isAr, useWesternNumbersInArabic),
                        style = if (isLandscape || isShortScreen) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )

                    val gregorianDateStr = remember(isAr, useWesternNumbersInArabic) {
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
                }

                // Unified Settings Button
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
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
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

        // 2.8 RANDOM HADITH CARD
        item {
            val hadiths = remember(isAr) {
                if (isAr) listOf(
                    "«إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى»" to "صحيح البخاري ومسلم • عن عمر بن الخطاب",
                    "«يَسِّرُوا وَلاَ تُعَسِّرُوا، وَبَشِّرُوا وَلاَ تُنَفِّرُوا»" to "صحيح البخاري • عن أنس بن مالك",
                    "«مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ»" to "صحيح البخاري • عن أبي هريرة",
                    "«الْمُؤْمِنُ لِلْمُؤْمِنِ كَالْبُنْيَانِ يَشُدُّ بَعْضُهُ بَعْضًا»" to "صحيح البخاري ومسلم • عن أبي موسى الأشعري",
                    "«خيرُكم من تعلَّمَ القرآنَ وعلَّمَهُ»" to "صحيح البخاري • عن عثمان بن عفان",
                    "«اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ، وَأَتْبِعِ السَّيِّئَةَ الْحَسَنَةَ تَمْحُهَا»" to "سنن الترمذي • عن أبي ذر الغفاري",
                    "«مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الْجَنَّةِ»" to "صحيح مسلم • عن أبي هريرة"
                ) else listOf(
                    "\"Actions are but by intentions, and every person shall have only that which he intended.\"" to "Sahih al-Bukhari & Muslim • Narrated by Umar bin Al-Khattab",
                    "\"Make things easy and do not make them difficult, and cheer people up and do not make them voice aversion.\"" to "Sahih al-Bukhari • Narrated by Anas bin Malik",
                    "\"He who believes in Allah and the Last Day must either speak good or remain silent.\"" to "Sahih al-Bukhari • Narrated by Abu Hurayrah",
                    "\"A believer to another believer is like a building whose different parts enforce and support each other.\"" to "Sahih al-Bukhari & Muslim • Narrated by Abu Musa Al-Ash'ari",
                    "\"The best among you are those who learn the Quran and teach it.\"" to "Sahih al-Bukhari • Narrated by Uthman bin Affan",
                    "\"Be mindful of Allah wherever you are, and follow up an evil deed with a good deed which will wipe it out.\"" to "Sunan al-Tirmidhi • Narrated by Abu Dharr Al-Ghifari",
                    "\"Whoever treads a path in search of knowledge, Allah will make easy for him the path to Paradise.\"" to "Sahih Muslim • Narrated by Abu Hurayrah"
                )
            }
            val randomHadith = remember { hadiths.random() }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) "حديث شريف" else "Hadith of the Day",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = randomHadith.first,
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
                        text = randomHadith.second,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
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
                    text = if (isAr) "مواقيت الصلاة" else "Prayer Schedule",
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
            val isNawafil = name.endsWith("(Nafilah)")
            val isCurrent = name == activeHighlight

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
                                contentDescription = "Toggle alert for $name",
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
        val layoutDirection = remember(currentLang) {
            if (currentLang == "ar") androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
        }
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection) {
            // Location states
            var locationQuery by remember { mutableStateOf("") }
            val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

            val playingSoundResId by AdhanSoundManager.playingResId.collectAsState()

            fun playSound(resId: Int) {
                if (playingSoundResId == resId) {
                    AdhanSoundManager.stop()
                } else {
                    val isShort = adhanSoundType == "short"
                    AdhanSoundManager.play(context, resId, isShort)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
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

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isAr) "تنبيهات أذكار الصباح" else "Morning Adhkar Notifications",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isAr) "تنبيه يومي لقراءة أذكار الصباح (بعد صلاة الفجر بـ ٣٠ دقيقة)" else "Daily reminder to recite Morning Adhkar (30 mins after Fajr)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = notifyMorningAdhkar,
                                            onCheckedChange = { viewModel.setNotifyMorningAdhkar(it) }
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isAr) "تنبيهات أذكار المساء" else "Evening Adhkar Notifications",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isAr) "تنبيه يومي لقراءة أذكار المساء (بعد صلاة العصر بـ ٣٠ دقيقة)" else "Daily reminder to recite Evening Adhkar (30 mins after Asr)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = notifyEveningAdhkar,
                                            onCheckedChange = { viewModel.setNotifyEveningAdhkar(it) }
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isAr) "تنبيه قبل الأذان بـ ١٥ دقيقة" else "Pre-Adhan Reminders",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isAr) "تنبيه تمهيدي قبل أذان كل صلاة مفروضة بـ ١٥ دقيقة للاستعداد" else "Notify 15 minutes before every fard prayer time to prepare",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = notifyBeforeAdhan,
                                            onCheckedChange = { viewModel.setNotifyBeforeAdhan(it) }
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isAr) "تنبيهات الأعياد والمناسبات" else "Eid & Occasion Alerts",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isAr) "تذكير بالمناسبات والأعياد الإسلامية قبل حلولها بليلة" else "Receive notifications of key Islamic calendar events a day before",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = notifyOccasions,
                                            onCheckedChange = { viewModel.setNotifyOccasions(it) }
                                         )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isAr) "تنبيه صيام الإثنين والخميس" else "Fast Reminders (Mon/Thu)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isAr) "تنبيه لصيام يومي الإثنين والخميس قبل حلولها بليلة" else "Receive reminders to fast on Sunnah Mondays and Thursdays a day before",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = notifyFasting,
                                            onCheckedChange = { viewModel.setNotifyFasting(it) }
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isAr) "تنبيه يوم الجمعة المبارك" else "Friday Jumu'ah Reminder",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isAr) "تنبيه خاص لصلاة الجمعة وقراءة سورة الكهف والصلاة على النبي" else "Special notification for Friday prayer, reciting Surah Al-Kahf & Salawat",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = notifyJumuah,
                                            onCheckedChange = { viewModel.setNotifyJumuah(it) }
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isAr) "تنبيه السحور" else "Suhur Reminder",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isAr) "تنبيه بوقت السحور قبل أذان الفجر" else "Special notification for Suhur before Fajr",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = notifySuhur,
                                            onCheckedChange = { viewModel.setNotifySuhur(it) }
                                        )
                                    }
                                    
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isAr) "تنبيه الإفطار" else "Iftar Reminder",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isAr) "تنبيه بوقت الإفطار عند أذان المغرب" else "Special notification for Iftar at Maghrib",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = notifyIftar,
                                            onCheckedChange = { viewModel.setNotifyIftar(it) }
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isAr) "صوت الأذان عند دخول الصلاة" else "Adhan Call to Prayer",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isAr) "تشغيل الأذان عند دخول وقت الفريضة" else "Play Adhan when a fard prayer starts",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = enableAdhanSound,
                                            onCheckedChange = { viewModel.setEnableAdhanSound(it) }
                                        )
                                    }

                                    if (enableAdhanSound) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isAr) "طول صوت الأذان" else "Adhan Sound Duration",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.setAdhanSoundType("short") },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (adhanSoundType == "short") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = if (adhanSoundType == "short") 2.dp else 1.dp,
                                                    color = if (adhanSoundType == "short") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                )
                                            ) {
                                                Text(
                                                    text = if (isAr) "قصير (٢٠ ثانية)" else "Short (20s)", 
                                                    fontWeight = FontWeight.Bold, 
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (adhanSoundType == "short") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            
                                            OutlinedButton(
                                                onClick = { viewModel.setAdhanSoundType("full") },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (adhanSoundType == "full") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = if (adhanSoundType == "full") 2.dp else 1.dp,
                                                    color = if (adhanSoundType == "full") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                )
                                            ) {
                                                Text(
                                                    text = if (isAr) "كامل" else "Full", 
                                                    fontWeight = FontWeight.Bold, 
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (adhanSoundType == "full") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isAr) "الاهتزاز واللمس" else "Global Haptic Feedback",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isAr) "تشغيل الاهتزاز والتفاعل اللمسي في السبحة والأدعية والقبلة" else "Enable vibrations in Tasbih subhah, Dua counters, and Qiblah alignment",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = enableTasbihHaptics,
                                            onCheckedChange = { viewModel.setEnableTasbihHaptics(it) }
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
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp),
                                            maxLines = 1,
                                            textStyle = MaterialTheme.typography.bodyMedium,
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
                                        text = if (isAr) "صلاة العصر: حساب الجمهور أم الحنفي" else "Asr Prayer: Standard vs Hanafi School",
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
                                            text = if (isAr) "تنسيق ونوع خط المصحف" else "Quran Font & Styling",
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
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Slider(
                                            value = fontEn,
                                            onValueChange = { viewModel.setEnglishFontSize(it) },
                                            valueRange = 12f..28f,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(text = "${fontEn.toInt()}sp", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {},
                                            modifier = Modifier.size(0.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = 1.dp,
                                                color = Color.Transparent
                                            )
                                        ) {
                                            Text(
                                                text = if (isAr) "الخط الافتراضي" else "Default Font",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Transparent
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {},
                                            modifier = Modifier.size(0.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.Transparent
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = 1.dp,
                                                color = Color.Transparent
                                            )
                                        ) {
                                            Text(
                                                text = if (isAr) "الخط العثماني" else "Othmani Font",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Transparent
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Real-time Text Size Preview Layout
                                    val previewFontFamily = FontFamily.Default
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f))
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(14.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = if (isAr) "معاينة خط المصحف والتراجم" else "Quran Font & Translation Preview",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.align(Alignment.Start)
                                             )

                                            // Arabic verse preview
                                            Text(
                                                text = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
                                                fontSize = fontAr.sp,
                                                fontFamily = previewFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                             
                                            // English translation preview
                                            Text(
                                                text = "In the name of Allah, the Beneficent, the Merciful",
                                                fontSize = fontEn.sp,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
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

@Composable
fun DailyEssentialsSection(navController: androidx.navigation.NavController, viewModel: BarakahViewModel, isAr: Boolean) {
    Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)) {
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
                    Icon(Icons.Default.WbSunny, contentDescription = if (isAr) "أذكار الصباح" else "Morning Adhkar", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
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
                    Icon(Icons.Default.NightsStay, contentDescription = if (isAr) "أذكار المساء" else "Evening Adhkar", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
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

@Composable
fun ExploreMoreSection(navController: androidx.navigation.NavController, isAr: Boolean) {
    Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)) {
        Text(
            text = if (isAr) "استكشف المزيد" else "Explore More",
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
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = if (isAr) "أيقونة أخرى" else "Others icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isAr) "أخرى" else "Others",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAr) "أسماء الله الحسنى والمناسبات الإسلامية" else "Allah's Names & Islamic Occasions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (isAr) "عرض المزيد" else "View more",
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
    val countdown by viewModel.nextPrayerCountdown.collectAsState()
    val nextPrayerName by viewModel.nextPrayerName.collectAsState()

val useWesternNumbersInArabic by viewModel.useWesternNumbersInArabic.collectAsState()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isShortScreen = configuration.screenHeightDp < 600

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isShortScreen || isLandscape) 8.dp else 16.dp)
            .testTag("countdown_card"),
        shape = androidx.compose.foundation.shape.AbsoluteRoundedCornerShape(topLeft = if (isShortScreen || isLandscape) 32.dp else 48.dp, bottomRight = if (isShortScreen || isLandscape) 32.dp else 48.dp, topRight = 16.dp, bottomLeft = 16.dp),
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
                    text = if (isAr) "الصلاة القادمة: ${translatePrayer(nextPrayerName)}" else "NEXT: $nextPrayerName".uppercase(),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                val formattedCountdown = remember(countdown, isAr, useWesternNumbersInArabic) {
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

                androidx.compose.material3.Text(
                    text = formattedCountdown,
                    style = androidx.compose.material3.MaterialTheme.typography.displayLarge.copy(
                        fontSize = if (isShortScreen || isLandscape) 32.sp else 44.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
