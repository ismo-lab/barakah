package dev.barakah.app.ui.screens

import dev.barakah.app.data.UiStrings

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.barakah.app.data.CityData
import dev.barakah.app.data.LocationDefaults
import dev.barakah.app.ui.BarakahViewModel
import dev.barakah.app.util.localize
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WelcomeScreen(viewModel: BarakahViewModel) {
    // The onboarding step and the two permission flags are kept across a configuration change:
    // without this, rotating mid-setup would send the user back to step one and re-ask for a
    // permission they had already answered.
    var step by rememberSaveable { mutableIntStateOf(1) } // 1: Language, 2: Calculations, 3: Notifications, 4: Location
    val currentLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isShortScreen = configuration.screenHeightDp < 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // safeDrawing, not status/navigation bars separately: it also covers a landscape display
            // cutout, which sits on a side edge and had content drawn under the camera.
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(if (isShortScreen) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(if (isShortScreen) 8.dp else 32.dp))
            
            // Header
            Text(
                text = if (isAr) "مرحباً بك في بركة" else UiStrings.of("Welcome to Barakah"),
                style = if (isShortScreen) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            if (!isShortScreen) {
                Text(
                    text = if (isAr) "تطبيقك الإسلامي المجاني ومفتوح المصدر" else UiStrings.of("Your free and open source Islam app"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(32.dp))
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            when (step) {
                1 -> LanguageStep(viewModel, onNext = { step = 2 })
                2 -> CalculationStep(viewModel, onNext = { step = 3 })
                3 -> NotificationsStep(viewModel, onNext = { step = 4 })
                4 -> LocationStep(viewModel, onFinish = { viewModel.setFirstRunComplete() })
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LanguageStep(viewModel: BarakahViewModel, onNext: () -> Unit) {
    val currentLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else null
    
    LaunchedEffect(notificationPermissionState?.status?.isGranted) {
        if (notificationPermissionState?.status?.isGranted == true && permissionRequested) {
            onNext()
        }
    }
    
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isShortScreen = configuration.screenHeightDp < 600
    val scrollState = androidx.compose.foundation.rememberScrollState()
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (isShortScreen) Modifier.verticalScroll(scrollState) else Modifier
    ) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            modifier = Modifier.size(if (isShortScreen) 40.dp else 80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(if (isShortScreen) 12.dp else 24.dp))
        Text(
            text = "اختر لغة التطبيق / Select Language",
            style = if (isShortScreen) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(if (isShortScreen) 16.dp else 32.dp))

        AppLanguagePicker(
            selectedTag = currentLang,
            isAr = currentLang == "ar",
            onSelect = { viewModel.setAppLanguage(it) }
        )

        if (isShortScreen) {
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Button(
            onClick = {
                if (notificationPermissionState != null && !notificationPermissionState.status.isGranted && !permissionRequested) {
                    notificationPermissionState.launchPermissionRequest()
                    permissionRequested = true
                } else {
                    onNext()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (currentLang == "ar") "التالي" else UiStrings.of("Next"),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
 fun LocationStep(viewModel: BarakahViewModel, onFinish: () -> Unit) {
     val currentLang by viewModel.appLanguage.collectAsStateWithLifecycle()
     val isAr = currentLang == "ar"
     var query by rememberSaveable { mutableStateOf("") }
     var filteredCities by remember { mutableStateOf(emptyList<dev.barakah.app.data.CitySuggestion>()) }
     var gpsPermissionRequested by rememberSaveable { mutableStateOf(false) }
     val context = androidx.compose.ui.platform.LocalContext.current

     LaunchedEffect(query, currentLang) {
         kotlinx.coroutines.delay(250)
         filteredCities = CityData.search(context, query, languageTag = currentLang)
             .filter { it.name.isNotBlank() && it.displayName(currentLang).isNotBlank() }
             .distinctBy { it.id }
     }
     
     val locationPermissionState = rememberPermissionState(
         android.Manifest.permission.ACCESS_FINE_LOCATION
     )

     LaunchedEffect(locationPermissionState.status.isGranted) {
         if (locationPermissionState.status.isGranted && gpsPermissionRequested) {
             viewModel.setLocationMethod("auto")
             onFinish()
         }
     }

     val configuration = androidx.compose.ui.platform.LocalConfiguration.current
     val isShortScreen = configuration.screenHeightDp < 600

     Column(horizontalAlignment = Alignment.CenterHorizontally) {
         Icon(
             imageVector = Icons.Default.LocationOn,
             contentDescription = null,
             modifier = Modifier.size(if (isShortScreen) 40.dp else 80.dp),
             tint = MaterialTheme.colorScheme.primary
         )
         Spacer(modifier = Modifier.height(if (isShortScreen) 12.dp else 24.dp))
         Text(
             text = if (isAr) "تحديد الموقع" else UiStrings.of("Set Your Location"),
             style = if (isShortScreen) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
             fontWeight = FontWeight.Bold
         )
         if (!isShortScreen) {
             Text(
                 text = if (isAr) "لحساب مواقيت الصلاة واتجاه القبلة بدقة" else UiStrings.of("For accurate prayer times and Qibla"),
                 style = MaterialTheme.typography.bodyMedium,
                 color = MaterialTheme.colorScheme.outline
             )
         }
         
         Spacer(modifier = Modifier.height(if (isShortScreen) 12.dp else 32.dp))

         // GPS Button
         Button(
             onClick = {
                 if (!locationPermissionState.status.isGranted && !gpsPermissionRequested) {
                     locationPermissionState.launchPermissionRequest()
                     gpsPermissionRequested = true
                 } else {
                     viewModel.setLocationMethod("auto")
                     onFinish()
                 }
             },
             modifier = Modifier.fillMaxWidth().height(56.dp),
             shape = RoundedCornerShape(16.dp),
             colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
         ) {
             Icon(Icons.Default.GpsFixed, contentDescription = null)
             Spacer(modifier = Modifier.width(12.dp))
             Text(text = if (isAr) "استخدام الموقع التلقائي (GPS)" else UiStrings.of("Use GPS (Recommended)"))
         }

         Spacer(modifier = Modifier.height(10.dp))

         // Default / Filler Button
         OutlinedButton(
             onClick = {
                 viewModel.setLocationMethod("manual")
                 val defaultLabel = LocationDefaults.unsetLabel(isAr)
                 viewModel.updateLocation(LocationDefaults.LATITUDE, LocationDefaults.LONGITUDE, defaultLabel)
                 onFinish()
             },
             modifier = Modifier.fillMaxWidth().height(56.dp),
             shape = RoundedCornerShape(16.dp)
         ) {
             Icon(Icons.Default.LocationOn, contentDescription = null)
             Spacer(modifier = Modifier.width(12.dp))
             Text(text = if (isAr) "تحديد الموقع لاحقاً (بدون تحديد)" else UiStrings.of("Set Location Later (Not Set)"))
         }

         Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = if (isAr) "أو اختر مدينة يدوياً:" else UiStrings.of("Or select a city manually:"), style = MaterialTheme.typography.labelMedium)
        
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(if (isAr) "ابحث عن مدينتك..." else UiStrings.of("Search for city...")) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredCities, key = { it.id }) { city ->
                ElevatedCard(
                    onClick = {
                        viewModel.setLocationMethod("manual")
                        viewModel.updateLocation(city.lat, city.lng, city.displayName(currentLang), timeZoneId = city.timeZoneId, countryCode = city.countryCode)
                        onFinish()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(city.displayName(currentLang), fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(city.countryName) },
                        leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
        
        if (query.isEmpty()) {
            Text(
                text = if (isAr) "يمكنك دائماً تغيير هذه الإعدادات لاحقاً" else UiStrings.of("You can always change these later in settings"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun NotificationsStep(viewModel: BarakahViewModel, onNext: () -> Unit) {
    val currentLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"

    val notifyMorningAdhkar by viewModel.notifyMorningAdhkar.collectAsStateWithLifecycle()
    val notifyEveningAdhkar by viewModel.notifyEveningAdhkar.collectAsStateWithLifecycle()
    val notifyBeforeAdhan by viewModel.notifyBeforeAdhan.collectAsStateWithLifecycle()
    val notifyOccasions by viewModel.notifyOccasions.collectAsStateWithLifecycle()
    val notifyFasting by viewModel.notifyFasting.collectAsStateWithLifecycle()
    val notifyJumuah by viewModel.notifyJumuah.collectAsStateWithLifecycle()
    val notifySuhur by viewModel.notifySuhur.collectAsStateWithLifecycle()
    val notifyIftar by viewModel.notifyIftar.collectAsStateWithLifecycle()
    val showNawafil by viewModel.showNawafil.collectAsStateWithLifecycle()
    val notifyNawafil by viewModel.notifyNawafil.collectAsStateWithLifecycle()
    val enableTasbihHaptics by viewModel.enableTasbihHaptics.collectAsStateWithLifecycle()
    val morningAdhkarOffset by viewModel.morningAdhkarOffset.collectAsStateWithLifecycle()
    val eveningAdhkarOffset by viewModel.eveningAdhkarOffset.collectAsStateWithLifecycle()
    val useWesternNumbersInArabic by viewModel.useWesternNumbersInArabic.collectAsStateWithLifecycle()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isShortScreen = configuration.screenHeightDp < 600

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight()) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(if (isShortScreen) 36.dp else 64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(if (isShortScreen) 6.dp else 12.dp))
        Text(
            text = if (isAr) "تخصيص التنبيهات" else UiStrings.of("Configure Notifications"),
            style = if (isShortScreen) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (!isShortScreen) {
            Text(
                text = if (isAr) "اختر التنبيهات التي ترغب في تلقيها يومياً" else UiStrings.of("Select which reminders you want to receive"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Notifications toggles card list - scrollable
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                NotificationToggleCard(
                    title = (if (isAr) "تنبيه قبل الأذان بـ ١٥ دقيقة" else UiStrings.of("Pre-Adhan Reminders")).localize(isAr, useWesternNumbersInArabic),
                    desc = (if (isAr) "تنبيه تمهيدي قبل أذان كل صلاة مفروضة بـ ١٥ دقيقة" else UiStrings.of("Notify 15 mins before every fard prayer time")).localize(isAr, useWesternNumbersInArabic),
                    checked = notifyBeforeAdhan,
                    onCheckedChange = { viewModel.setNotifyBeforeAdhan(it) }
                )
            }
            item {
                NotificationToggleCard(
                    title = if (isAr) "تنبيهات أذكار الصباح" else UiStrings.of("Morning Adhkar Notifications"),
                    desc = if (isAr) "تنبيه يومي لقراءة الأذكار (وقت قابل للتخصيص)" else UiStrings.of("Daily reminder to recite Adhkar (customizable timing)"),
                    checked = notifyMorningAdhkar,
                    onCheckedChange = { viewModel.setNotifyMorningAdhkar(it) }
                )
            }
            if (notifyMorningAdhkar) {
                item {
                    dev.barakah.app.ui.screens.AdhkarOffsetSelector(
                        currentOffset = morningAdhkarOffset,
                        onOffsetSelected = { viewModel.setMorningAdhkarOffset(it) },
                        isAr = isAr,
                        isMorning = true,
                        useWesternNumbersInArabic = useWesternNumbersInArabic
                    )
                }
            }
            item {
                NotificationToggleCard(
                    title = if (isAr) "تنبيهات أذكار المساء" else UiStrings.of("Evening Adhkar Notifications"),
                    desc = if (isAr) "تنبيه يومي لقراءة الأذكار (وقت قابل للتخصيص)" else UiStrings.of("Daily reminder to recite Adhkar (customizable timing)"),
                    checked = notifyEveningAdhkar,
                    onCheckedChange = { viewModel.setNotifyEveningAdhkar(it) }
                )
            }
            if (notifyEveningAdhkar) {
                item {
                    dev.barakah.app.ui.screens.AdhkarOffsetSelector(
                        currentOffset = eveningAdhkarOffset,
                        onOffsetSelected = { viewModel.setEveningAdhkarOffset(it) },
                        isAr = isAr,
                        isMorning = false,
                        useWesternNumbersInArabic = useWesternNumbersInArabic
                    )
                }
            }

            item {
                NotificationToggleCard(
                    title = if (isAr) "عرض النوافل وقيام الليل" else UiStrings.of("Show Voluntary Prayers"),
                    desc = if (isAr) "تضمين الضحى وقيام الليل والوتر والتهجد لجدول الصلوات" else UiStrings.of("Include Duha, Qiyam-ul-Layl, Witr & Tahajjud in schedule"),
                    checked = showNawafil,
                    onCheckedChange = { viewModel.setShowNawafil(it) }
                )
            }
            if (showNawafil) {
                item {
                    NotificationToggleCard(
                        title = if (isAr) "تنبيهات صلوات النوافل" else UiStrings.of("Nawafil Notifications"),
                        desc = if (isAr) "تنبيهات للضحى والوتر والتهجد وقيام الليل" else UiStrings.of("Notifications for Duha, Witr, Tahajjud & Qiyam-ul-Layl"),
                        checked = notifyNawafil,
                        onCheckedChange = { viewModel.setNotifyNawafil(it) }
                    )
                }
            }
            item {
                NotificationToggleCard(
                    title = if (isAr) "تنبيه يوم الجمعة المبارك" else UiStrings.of("Friday Jumu'ah Reminder"),
                    desc = if (isAr) "تنبيه خاص لصلاة الجمعة وقراءة سورة الكهف والصلوات" else UiStrings.of("Special notification for Friday prayer and Surah Al-Kahf"),
                    checked = notifyJumuah,
                    onCheckedChange = { viewModel.setNotifyJumuah(it) }
                )
            }
            item {
                NotificationToggleCard(
                    title = if (isAr) "تنبيه صيام الإذن والخميس" else UiStrings.of("Mon/Thu Fasting Reminders"),
                    desc = if (isAr) "تذكير بصيام يومي الإثنين والخميس في الليلة السابقة" else UiStrings.of("Reminder to fast on Mondays and Thursdays the night before"),
                    checked = notifyFasting,
                    onCheckedChange = { viewModel.setNotifyFasting(it) }
                )
            }
            item {
                NotificationToggleCard(
                    title = if (isAr) "تنبيهات الأعياد والمناسبات" else UiStrings.of("Eid & Occasion Alerts"),
                    desc = if (isAr) "تذكير المناسبات الإسلامية المهمة قبل حلولها بليلة" else UiStrings.of("Alert of key Islamic calendar events a day before"),
                    checked = notifyOccasions,
                    onCheckedChange = { viewModel.setNotifyOccasions(it) }
                )
            }
            item {
                NotificationToggleCard(
                    title = if (isAr) "تنبيه السحور" else UiStrings.of("Suhur Reminder"),
                    desc = if (isAr) "تنبيه بوقت السحور الفضيل قبل أذان الكاذب للفجر" else UiStrings.of("Special reminder notification for Suhur before Fajr"),
                    checked = notifySuhur,
                    onCheckedChange = { viewModel.setNotifySuhur(it) }
                )
            }
            item {
                NotificationToggleCard(
                    title = if (isAr) "تنبيه الإفطار" else UiStrings.of("Iftar Reminder"),
                    desc = if (isAr) "تنبيه مخصص وقت الإفطار المبارك عند أذان المغرب" else UiStrings.of("Special notification for Iftar at Maghrib time"),
                    checked = notifyIftar,
                    onCheckedChange = { viewModel.setNotifyIftar(it) }
                )
            }
            item {
                NotificationToggleCard(
                    title = if (isAr) "الاهتزاز والتفاعل اللمسي" else UiStrings.of("Global Haptic Feedback"),
                    desc = if (isAr) "تشغيل الاهتزاز والتفاعل اللمسي في السبحة والأدعية والقبلة" else UiStrings.of("Enable vibrations in Tasbih subhah, Dua counters, and Qiblah alignment"),
                    checked = enableTasbihHaptics,
                    onCheckedChange = { viewModel.setEnableTasbihHaptics(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (isAr) "التالي" else UiStrings.of("Next"),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun NotificationToggleCard(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }
            if (content != null) {
                content()
            }
        }
    }
}

@Composable
fun CalculationStep(viewModel: BarakahViewModel, onNext: () -> Unit) {
    val currentLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isAr = currentLang == "ar"
    
    val asrMethod by viewModel.asrMethod.collectAsStateWithLifecycle()
    val ishaMethod by viewModel.ishaMethod.collectAsStateWithLifecycle()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isShortScreen = configuration.screenHeightDp < 600

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight()
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            modifier = Modifier.size(if (isShortScreen) 36.dp else 64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(if (isShortScreen) 6.dp else 12.dp))
        Text(
            text = if (isAr) "المذهب والحساب الفقهي" else UiStrings.of("Juristic Calculations"),
            style = if (isShortScreen) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (!isShortScreen) {
            Text(
                text = if (isAr) "تسمح هذه الإعدادات بتخصيص حساب صلاة العصر والعشاء" else UiStrings.of("These settings customize calculations for Asr and Isha prayers"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Spacer(modifier = Modifier.height(10.dp))
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Asr Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isAr) "صلاة العصر: حساب الجمهور أم الحنفي" else UiStrings.of("Asr Prayer: Standard vs Hanafi School"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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
                                Text(
                                    text = if (isAr) "الجمهور (شافعي...)" else UiStrings.of("Standard (Shafi'i)"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (asrMethod == "standard") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
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
                                Text(
                                    text = if (isAr) "المذهب الحنفي" else UiStrings.of("Hanafi School"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (asrMethod == "hanafi") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Isha Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isAr) "صلاة العشاء: التقدير الفلكي أم الحنفي" else UiStrings.of("Isha Prayer: Standard Angle vs Hanafi Angle (18°)"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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
                                Text(
                                    text = if (isAr) "الحساب فلكياً" else UiStrings.of("Standard Angle"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ishaMethod == "standard") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
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
                                Text(
                                    text = if (isAr) "حنفي (18 درجة)" else UiStrings.of("Hanafi Angle (18°)"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ishaMethod == "hanafi") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (isAr) "التالي" else UiStrings.of("Next"),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}
