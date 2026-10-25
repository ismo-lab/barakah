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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import dev.barakah.app.util.PrayerCalculator
import dev.barakah.app.util.TimeUtils
import dev.barakah.app.util.localize
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*

/**
 * The settings screen.
 *
 * It lives in its own file because it is a screen: 1200 lines with their own state, their own
 * sub-lists and their own scrolling, which until now sat in the middle of HomeScreen and made that
 * function unreadable. Nothing about it changed in the move.
 *
 * It reads the settings it edits from the view model itself, the way the other screens here do,
 * rather than taking thirty of them as parameters.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeSettingsSection(
    viewModel: BarakahViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val currentLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"

    val locationLabel by viewModel.locationLabel.collectAsStateWithLifecycle()
    val locationMethod by viewModel.locationMethod.collectAsStateWithLifecycle()
    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val useDynamicColor by viewModel.useDynamicColor.collectAsStateWithLifecycle()
    val amoledDark by viewModel.amoledDark.collectAsStateWithLifecycle()
    val useWesternNumbersInArabic by viewModel.useWesternNumbersInArabic.collectAsStateWithLifecycle()
    val enableTasbihHaptics by viewModel.enableTasbihHaptics.collectAsStateWithLifecycle()

    val asrMethod by viewModel.asrMethod.collectAsStateWithLifecycle()
    val ishaMethod by viewModel.ishaMethod.collectAsStateWithLifecycle()
    val showNawafil by viewModel.showNawafil.collectAsStateWithLifecycle()
    val adjFajr by viewModel.adjFajr.collectAsStateWithLifecycle()
    val adjDhuhr by viewModel.adjDhuhr.collectAsStateWithLifecycle()
    val adjAsr by viewModel.adjAsr.collectAsStateWithLifecycle()
    val adjMaghrib by viewModel.adjMaghrib.collectAsStateWithLifecycle()
    val adjIsha by viewModel.adjIsha.collectAsStateWithLifecycle()
val adjGlobal by viewModel.adjGlobal.collectAsStateWithLifecycle()
val prayerTimes by viewModel.prayerTimes.collectAsStateWithLifecycle()
    val hijriAdjustment by viewModel.hijriAdjustment.collectAsStateWithLifecycle()
    val hijriAdjustmentIsManual by viewModel.hijriAdjustmentIsManual.collectAsStateWithLifecycle()

    val adhanAudioEnabled by viewModel.adhanAudioEnabled.collectAsStateWithLifecycle()
    val adhanAudioMode by viewModel.adhanAudioMode.collectAsStateWithLifecycle()
    val adhanAudioStream by viewModel.adhanAudioStream.collectAsStateWithLifecycle()
    val notifyBeforeAdhan by viewModel.notifyBeforeAdhan.collectAsStateWithLifecycle()
    val notifyOccasions by viewModel.notifyOccasions.collectAsStateWithLifecycle()
    val notifyFasting by viewModel.notifyFasting.collectAsStateWithLifecycle()
    val notifyJumuah by viewModel.notifyJumuah.collectAsStateWithLifecycle()
    val notifySuhur by viewModel.notifySuhur.collectAsStateWithLifecycle()
    val notifyIftar by viewModel.notifyIftar.collectAsStateWithLifecycle()
    val notifyNawafil by viewModel.notifyNawafil.collectAsStateWithLifecycle()
    val notifyMorningAdhkar by viewModel.notifyMorningAdhkar.collectAsStateWithLifecycle()
    val notifyEveningAdhkar by viewModel.notifyEveningAdhkar.collectAsStateWithLifecycle()
    val notifyDailyVerse by viewModel.notifyDailyVerse.collectAsStateWithLifecycle()
    val notifyDailyHadith by viewModel.notifyDailyHadith.collectAsStateWithLifecycle()
    val morningAdhkarOffset by viewModel.morningAdhkarOffset.collectAsStateWithLifecycle()
    val eveningAdhkarOffset by viewModel.eveningAdhkarOffset.collectAsStateWithLifecycle()
    val layoutDirection = remember(currentLang) {
        if (dev.barakah.app.data.UiLanguages.isRtl(currentLang)) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
    }
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection) {
        // Location states
        // Kept across a configuration change so a rotation in the middle of searching for a city
        // does not wipe the query; the suggestion list is rebuilt by the effect below either way.
        var locationQuery by rememberSaveable { mutableStateOf("") }
        var searchSubmission by rememberSaveable { mutableIntStateOf(0) }
        var citySuggestions by remember { mutableStateOf(emptyList<dev.barakah.app.data.CitySuggestion>()) }
        LaunchedEffect(locationQuery, searchSubmission, currentLang) {
            val query = locationQuery.trim()
            citySuggestions = emptyList()
            if (query.length < 2) return@LaunchedEffect
            delay(250)
            val results = dev.barakah.app.data.CityData.search(
                context,
                query,
                languageTag = currentLang
            )
            if (locationQuery.trim() == query) {
                citySuggestions = results
                    .filter { it.name.isNotBlank() && it.displayName(currentLang).isNotBlank() }
                    .distinctBy { it.id }
            }
        }
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

        Box(
            // No navigation bar inset: the Scaffold content already ends above the bottom nav bar,
            // so asking for it again left a dead black band above the bar.
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    // No bottom padding here: out here it would end the scroll area short of the nav
                    // bar and leave a visible band. The list keeps that spacing inside itself instead.
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onDismiss() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = UiStrings.of("Back"),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAr) "الضبط والإعدادات" else UiStrings.of("Settings & Preferences"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
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
                                        text = if (isAr) "اللغة ومظهر التطبيق" else UiStrings.of("Language & Theme"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                AppLanguagePicker(
                                    selectedTag = currentLang,
                                    isAr = isAr,
                                    onSelect = { viewModel.setAppLanguage(it) }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                Text(
                                    text = if (isAr) "الوضع العام" else UiStrings.of("Dark / Light Mode"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("light" to (if (isAr) "مضيء" else UiStrings.of("Light")), "dark" to (if (isAr) "مظلم" else UiStrings.of("Dark")), "system" to (if (isAr) "تلقائي" else UiStrings.of("System"))).forEach { (mode, flag) ->
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
                                        text = if (isAr) "مظهر AMOLED أسود بالكامل" else UiStrings.of("AMOLED Black Theme"),
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
                                        text = if (isAr) "الألوان الديناميكية من النظام" else UiStrings.of("Dynamic Device Colors"),
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
                                        text = if (isAr) "التنبيهات والصلوات المستحبة" else UiStrings.of("Notifications & Voluntary"),
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
                                            text = if (isAr) "صوت الأذان" else UiStrings.of("Adhan Sound"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isAr) "تشغيل الأذان عند دخول وقت الصلاة" else UiStrings.of("Play the Adhan when a prayer time begins"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = adhanAudioEnabled,
                                        onCheckedChange = { viewModel.setAdhanAudioEnabled(it) }
                                    )
                                }

                                if (adhanAudioEnabled) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { viewModel.setAdhanAudioMode("full") },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = adhanAudioMode == "full",
                                                onClick = { viewModel.setAdhanAudioMode("full") }
                                            )
                                            Text(
                                                text = if (isAr) "الأذان كاملًا" else UiStrings.of("Full Adhan"),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { viewModel.setAdhanAudioMode("short") },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = adhanAudioMode == "short",
                                                onClick = { viewModel.setAdhanAudioMode("short") }
                                            )
                                            Text(
                                                text = if (isAr) "أول ١٥ ثانية فقط" else UiStrings.of("First 15 seconds"),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        Text(
                                            text = if (isAr) "مستوى الصوت المتحكّم به" else UiStrings.of("Volume controlled by"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { viewModel.setAdhanAudioStream("notification") },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = adhanAudioStream == "notification",
                                                onClick = { viewModel.setAdhanAudioStream("notification") }
                                            )
                                            Column {
                                                Text(
                                                    text = if (isAr) "صوت التنبيهات (موصى به)" else UiStrings.of("Notification volume (recommended)"),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = if (isAr) "يُكتم في الوضع الصامت وعدم الإزعاج" else UiStrings.of("Muted in silent mode and Do Not Disturb"),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { viewModel.setAdhanAudioStream("alarm") },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = adhanAudioStream == "alarm",
                                                onClick = { viewModel.setAdhanAudioStream("alarm") }
                                            )
                                            Column {
                                                Text(
                                                    text = if (isAr) "صوت المنبّه" else UiStrings.of("Alarm volume"),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = if (isAr) "يعمل حتى في الوضع الصامت وعدم الإزعاج" else UiStrings.of("Plays even in silent mode and Do Not Disturb"),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
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
                                            text = if (isAr) "عرض النوافل وقيام الليل" else UiStrings.of("Show Voluntary Prayers"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isAr) "تضمين الضحى وقيام الليل والوتر والتهجد لجدول الصلوات" else UiStrings.of("Include Duha, Qiyam-ul-Layl, Witr & Tahajjud in schedule"),
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

                                if (showNawafil) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isAr) "تنبيهات صلوات النوافل" else UiStrings.of("Nawafil Notifications"),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isAr) "تنبيهات للضحى والوتر والتهجد وقيام الليل" else UiStrings.of("Notifications for Duha, Witr, Tahajjud & Qiyam-ul-Layl"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = notifyNawafil,
                                            onCheckedChange = { viewModel.setNotifyNawafil(it) }
                                        )
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
                                            text = if (isAr) "تنبيهات أذكار الصباح" else UiStrings.of("Morning Adhkar Notifications"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isAr) "تنبيه يومي لقراءة أذكار الصباح" else UiStrings.of("Daily reminder to recite Morning Adhkar"),
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

                                if (notifyMorningAdhkar) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AdhkarOffsetSelector(
                                        currentOffset = morningAdhkarOffset,
                                        onOffsetSelected = { viewModel.setMorningAdhkarOffset(it) },
                                        isAr = isAr,
                                        isMorning = true,
                                        useWesternNumbersInArabic = useWesternNumbersInArabic
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
                                            text = if (isAr) "تنبيهات أذكار المساء" else UiStrings.of("Evening Adhkar Notifications"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isAr) "تنبيه يومي لقراءة أذكار المساء" else UiStrings.of("Daily reminder to recite Evening Adhkar"),
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

                                if (notifyEveningAdhkar) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AdhkarOffsetSelector(
                                        currentOffset = eveningAdhkarOffset,
                                        onOffsetSelected = { viewModel.setEveningAdhkarOffset(it) },
                                        isAr = isAr,
                                        isMorning = false,
                                        useWesternNumbersInArabic = useWesternNumbersInArabic
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                listOf(
                                    Triple(UiStrings.of("Today's Verse"), notifyDailyVerse, viewModel::setNotifyDailyVerse),
                                    Triple(UiStrings.of("Today's Hadith"), notifyDailyHadith, viewModel::setNotifyDailyHadith)
                                ).forEach { (label, checked, onChecked) ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                        Switch(checked = checked, onCheckedChange = onChecked)
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
                                            text = (if (isAr) "تنبيه قبل الأذان بـ ١٥ دقيقة" else UiStrings.of("Pre-Adhan Reminders")).localize(isAr, useWesternNumbersInArabic),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = (if (isAr) "تنبيه تمهيدي قبل أذان كل صلاة مفروضة بـ ١٥ دقيقة للاستعداد" else UiStrings.of("Notify 15 minutes before every fard prayer time to prepare")).localize(isAr, useWesternNumbersInArabic),
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
                                            text = if (isAr) "تنبيهات الأعياد والمناسبات" else UiStrings.of("Eid & Occasion Alerts"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isAr) "تذكير بالمناسبات والأعياد الإسلامية قبل حلولها بليلة" else UiStrings.of("Receive notifications of key Islamic calendar events a day before"),
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
                                            text = if (isAr) "تنبيه صيام الإثنين والخميس" else UiStrings.of("Fast Reminders (Mon/Thu)"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isAr) "تنبيه لصيام يومي الإثنين والخميس قبل حلولها بليلة" else UiStrings.of("Receive reminders to fast on Sunnah Mondays and Thursdays a day before"),
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
                                            text = if (isAr) "تنبيه يوم الجمعة المبارك" else UiStrings.of("Friday Jumu'ah Reminder"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isAr) "تنبيه خاص لصلاة الجمعة وقراءة سورة الكهف والصلاة على النبي" else UiStrings.of("Special notification for Friday prayer, reciting Surah Al-Kahf & Salawat"),
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
                                            text = if (isAr) "تنبيه السحور" else UiStrings.of("Suhur Reminder"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isAr) "تنبيه بوقت السحور قبل أذان الفجر" else UiStrings.of("Special notification for Suhur before Fajr"),
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
                                            text = if (isAr) "تنبيه الإفطار" else UiStrings.of("Iftar Reminder"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isAr) "تنبيه بوقت الإفطار عند أذان المغرب" else UiStrings.of("Special notification for Iftar at Maghrib"),
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
                                            text = if (isAr) "الاهتزاز واللمس" else UiStrings.of("Global Haptic Feedback"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isAr) "تشغيل الاهتزاز والتفاعل اللمسي في السبحة والأدعية والقبلة" else UiStrings.of("Enable vibrations in Tasbih subhah, Dua counters, and Qiblah alignment"),
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
                                        text = if (isAr) "تحديد الموقع الجغرافي" else UiStrings.of("Location Settings"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = if (isAr) "الموقع الحالي: $locationLabel" else UiStrings.format("Selected: %s", locationLabel),
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
                                            // One request only: switching to auto already fetches,
                                            // so doing both fetched twice and reported twice.
                                            if (locationPermissionState.status.isGranted) {
                                                viewModel.setLocationMethod("auto")
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
                                        Text(if (isAr) "تلقائي (GPS)" else UiStrings.of("Auto (GPS)"), style = MaterialTheme.typography.bodySmall)
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
                                        Text(if (isAr) "يدوي بالبحث" else UiStrings.of("Manual Search"), style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                if (locationMethod == "manual") {
                                    OutlinedTextField(
                                        value = locationQuery,
                                        onValueChange = {
                                            locationQuery = it
                                            citySuggestions = emptyList()
                                        },
                                        placeholder = { Text(if (isAr) "ابحث عن مدينة..." else UiStrings.of("Search city name...")) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        maxLines = 1,
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                searchSubmission++
                                                keyboardController?.hide()
                                            }
                                        ),
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    val suggestions = citySuggestions

                                    if (suggestions.isNotEmpty()) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)
                                        ) {
                                            suggestions.forEach { city ->
                                                key(city.id) {
                                                    val displayName = "${city.displayName(currentLang)}، ${city.countryName}"
                                                    Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                                        .clickable {
                                                            viewModel.updateLocation(city.lat, city.lng, city.displayName(currentLang), isSuccessFeedback = true, timeZoneId = city.timeZoneId, countryCode = city.countryCode)
                                                            locationQuery = ""
                                                            keyboardController?.hide()
                                                            focusManager.clearFocus()
                                                            onDismiss()
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
                                        text = if (isAr) "المذهب والحساب الفقهي" else UiStrings.of("Juristic Calculations"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = if (isAr) "صلاة العصر: حساب الجمهور أم الحنفي" else UiStrings.of("Asr Prayer: Standard vs Hanafi School"),
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
                                        Text(if (isAr) "الجمهور (شافعي، مالكي...)" else UiStrings.of("Standard (Shafi'i)"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                                        Text(if (isAr) "حنفي" else UiStrings.of("Hanafi"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                Text(
                                    text = if (isAr) "صلاة العشاء: التقدير الفلكي أم الحنفي (زاوية 18°)" else UiStrings.of("Isha Prayer: Standard Angle vs Hanafi Angle (18°)"),
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
                                        Text(if (isAr) "طريقة الحساب فلكياً" else UiStrings.of("Calculation"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                                        Text(if (isAr) "حنفي (18 درجة)" else UiStrings.of("Hanafi Angle (18°)"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                                        text = if (isAr) "تعديل الدقائق يدويًّا" else UiStrings.of("Manual Adjustments"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = if (isAr) "أضف أو نقص دقائق مخصصة لكل صلاة حسب مدينتك بدقة" else UiStrings.of("Add or deduct minutes manually for each prayer calculation"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isAr) "توقيت الهاتف" else UiStrings.of("Phone clock"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isAr) "إن كان الهاتف ما زال على التوقيت القديم، أزح كل الصلوات بنفس المقدار."
                                            else UiStrings.of("Shift every prayer at once if the phone still uses the old time zone rules."),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { if (adjGlobal > -180) viewModel.setAdjGlobal(adjGlobal - 60) },
                                            enabled = adjGlobal > -180,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Remove,
                                                contentDescription = if (isAr) "ساعة أقل" else UiStrings.of("An hour earlier"),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        val globalPrefix = if (adjGlobal > 0) "+" else ""
                                        Text(
                                            text = if (adjGlobal == 0) "0" else "$globalPrefix${adjGlobal / 60} " + (if (isAr) "س" else UiStrings.of("h")),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.width(50.dp)
                                        )
                                        IconButton(
                                            onClick = { if (adjGlobal < 180) viewModel.setAdjGlobal(adjGlobal + 60) },
                                            enabled = adjGlobal < 180,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = if (isAr) "ساعة أكثر" else UiStrings.of("An hour later"),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                if (adjGlobal != 0) {
                                    Text(
                                        text = if (isAr) {
                                            if (adjGlobal > 0) "أُزيحت كل الصلوات ${kotlin.math.abs(adjGlobal)} دقيقة متأخرًا."
                                            else "أُزيحت كل الصلوات ${kotlin.math.abs(adjGlobal)} دقيقة مبكرًا."
                                        } else {
                                            if (adjGlobal > 0) UiStrings.format("All prayers moved %s min later.", kotlin.math.abs(adjGlobal))
                                            else UiStrings.format("All prayers moved %s min earlier.", kotlin.math.abs(adjGlobal))
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                val adjList = listOf(
                                    Quadruple("Fajr", if (isAr) "الفجر" else UiStrings.of("Fajr"), adjFajr) { v: Int -> viewModel.setAdjFajr(v) },
                                    Quadruple("Dhuhr", if (isAr) "الظهر" else UiStrings.of("Dhuhr"), adjDhuhr) { v: Int -> viewModel.setAdjDhuhr(v) },
                                    Quadruple("Asr", if (isAr) "العصر" else UiStrings.of("Asr"), adjAsr) { v: Int -> viewModel.setAdjAsr(v) },
                                    Quadruple("Maghrib", if (isAr) "المغرب" else UiStrings.of("Maghrib"), adjMaghrib) { v: Int -> viewModel.setAdjMaghrib(v) },
                                    Quadruple("Isha", if (isAr) "العشاء" else UiStrings.of("Isha"), adjIsha) { v: Int -> viewModel.setAdjIsha(v) }
                                )

                                var fineTuningOpen by rememberSaveable { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { fineTuningOpen = !fineTuningOpen }
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isAr) "ضبط دقيق لكل صلاة" else UiStrings.of("Fine tuning per prayer"),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = if (fineTuningOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                AnimatedVisibility(visible = fineTuningOpen) {
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
                                            Text(
                                                text = when (adjItem.first) {
                                                    "Fajr" -> prayerTimes.fajr
                                                    "Dhuhr" -> prayerTimes.dhuhr
                                                    "Asr" -> prayerTimes.asr
                                                    "Maghrib" -> prayerTimes.maghrib
                                                    else -> prayerTimes.isha
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { if (adjItem.third > -99) adjItem.fourth(adjItem.third - 1) },
                                                    enabled = adjItem.third > -99,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = UiStrings.of("Deduct minute"), modifier = Modifier.size(16.dp))
                                                }
                                                val prefix = if (adjItem.third > 0) "+" else ""
                                                Text(
                                                    text = ("$prefix${adjItem.third} " + (if (isAr) "د" else UiStrings.of("m"))).localize(isAr, useWesternNumbersInArabic),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.width(50.dp)
                                                )
                                                IconButton(
                                                    onClick = { if (adjItem.third < 99) adjItem.fourth(adjItem.third + 1) },
                                                    enabled = adjItem.third < 99,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = UiStrings.of("Add minute"), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                                }
                            }
                        }
                    }

                    // Card: Hijri date adjustment
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAr) "التاريخ الهجري" else UiStrings.of("Hijri Date"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = if (isAr) "التاريخ محسوب على أم القرى، ويُزاح تلقائياً في البلدان التي تعتمد الرؤية المحلية. عدّله هنا عند الحاجة، واضغط الرقم للرجوع إلى التلقائي." else UiStrings.of("The date is calculated from Umm al-Qura, shifted automatically for countries that follow a local sighting. Override it here if needed; tap the number to go back to automatic."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

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
                                        text = if (isAr) "الإزاحة" else UiStrings.of("Adjustment"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.setHijriAdjustment(hijriAdjustment - 1) },
                                            enabled = hijriAdjustment > -2,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = if (isAr) "يوم أقل" else UiStrings.of("One day earlier"), modifier = Modifier.size(16.dp))
                                        }
                                        val adjustmentLabel = when {
                                            !hijriAdjustmentIsManual -> if (isAr) "تلقائي" else UiStrings.of("Auto")
                                            hijriAdjustment == 0 -> if (isAr) "محسوب" else UiStrings.of("Calculated")
                                            hijriAdjustment > 0 -> "+$hijriAdjustment " + (if (isAr) "يوم" else UiStrings.of("d"))
                                            else -> "$hijriAdjustment " + (if (isAr) "يوم" else UiStrings.of("d"))
                                        }
                                        Text(
                                            text = adjustmentLabel.localize(isAr, useWesternNumbersInArabic),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .width(70.dp)
                                                .clickable(enabled = hijriAdjustmentIsManual) {
                                                    viewModel.resetHijriAdjustment()
                                                }
                                        )
                                        IconButton(
                                            onClick = { viewModel.setHijriAdjustment(hijriAdjustment + 1) },
                                            enabled = hijriAdjustment < 2,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = if (isAr) "يوم أكثر" else UiStrings.of("One day later"), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // Show what the shift does, rather than leaving the arrows as guesswork.
                                val previewDate = dev.barakah.app.util.HijriCalendarHelper
                                    .getTodayHijriDateString(isAr, hijriAdjustment)
                                Text(
                                    text = ((if (isAr) "اليوم: " else UiStrings.of("Today: ")) + previewDate)
                                        .localize(isAr, useWesternNumbersInArabic),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End
                                )
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isAr) "عن التطبيق" else UiStrings.of("About this App"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = "v${dev.barakah.app.BuildConfig.VERSION_NAME} (${dev.barakah.app.BuildConfig.VERSION_CODE})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = if (isAr) "هذا التطبيق مجاني بالكامل ومفتوح المصدر (FOSS)، خالي من الإعلانات إلى الأبد، ويحترم خصوصيتك. لقد قمت ببرمجة هذا التطبيق كعمل صالح للتقرب إلى الله، وأرجو من كل من يستخدمه أن يدعو لي بالخير. شكرًا لكم وجزاكم الله خيرًا."
                                            else UiStrings.of("This application is fully Free and Open Source Software (FOSS), forever ad-free, and respects your privacy. I made this app for a good deed, and I hope whoever uses it will do dua for me. Thank you and Jazakum Allah Khair."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )

                                Text(
                                    text = if (isAr) "بيانات المدن: GeoNames cities500، بترخيص CC BY 4.0." else UiStrings.of("City data: GeoNames cities500, licensed under CC BY 4.0."),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = if (isAr) {
                                        "ترجمات القرآن: وزارة الشؤون الدينية الإندونيسية (QuranEnc)، محمد جناغرهي، أبو بكر زكريا، مسلم شاهين، فريق Islamhouse.com الفارسي، أبوبكر محمود جومي، عبدالله محمد بسميه، علي محسن البرواني (Tanzil)."
                                    } else {
                                        UiStrings.of("Quran translations: Indonesian Ministry of Religious Affairs (QuranEnc), Muhammad Junagarhi, Abu Bakr Zakaria, Muslim Shahin, Islamhouse.com Persian Team, Abubakar Mahmood Jummi, Abdullah Muhammad Basmeih, Ali Muhsin Al Barwani (Tanzil).")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = if (isAr) {
                                        "التفسير: الجلالين (QUL وAltafsir)، والمختصر عبر Tarteel QUL، وتفسير أبو بكر زكريا بالبنغالية، والسعدي بالأردية (QuranEnc، الإصدار 1.0.0)."
                                    } else {
                                        UiStrings.of("Tafsir: Tafsir al-Jalalayn (English, altafsir.com), Al-Mukhtasar (Tarteel QUL), Tafsir Abu Bakr Zakaria (Bengali), and As-Saadi Urdu (QuranEnc, V1.0.0).")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = if (isAr) {
                                        "ترجمات الأدعية: MuslimData بترخيص Apache 2.0 (الروسية والفارسية والكردية السورانية)."
                                    } else {
                                        UiStrings.of("Supplication translations: MuslimData, Apache License 2.0 (Russian, Persian, Sorani Kurdish).")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        text = if (isAr) "رمز المصدر (GitHub)" else UiStrings.of("Source Code (GitHub)"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(
                                    onClick = { uriHandler.openUri("https://tanzil.net/trans/") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isAr) "شروط ترجمات Tanzil" else UiStrings.of("Tanzil translation terms"),
                                        style = MaterialTheme.typography.labelMedium
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
                                contentDescription = UiStrings.of("Reset Settings"),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "إعادة الضبط الافتراضي" else UiStrings.of("Reset to Defaults"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
