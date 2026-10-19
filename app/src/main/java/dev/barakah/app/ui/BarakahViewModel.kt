package dev.barakah.app.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.GlanceAppWidgetManager
import dev.barakah.app.data.*
import dev.barakah.app.widget.PrayerWidget
import dev.barakah.app.widget.PrayerRemainingWidget
import dev.barakah.app.widget.NawafilWidget
import dev.barakah.app.widget.refreshWidgetInstances
import dev.barakah.app.util.AppNightMode
import dev.barakah.app.util.HijriCalendarHelper
import dev.barakah.app.util.LocationProvider
import dev.barakah.app.util.PrayerCalculator
import dev.barakah.app.util.QiblaManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import dev.barakah.app.R

private const val TAG = "BarakahViewModel"

class BarakahViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    /** The data layer is an implementation detail; the screens only see StateFlows. */
    private val repository = AppRepository(db)

    // Hardware managers
    private val sensorManager = QiblaManager(application)
    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator ?: (application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    } else {
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val prefs = application.getSharedPreferences(AppPrefs.FILE, Context.MODE_PRIVATE)

    // First Run state
    private val _isFirstRun = MutableStateFlow(prefs.getBoolean(AppPrefs.IS_FIRST_RUN, true))
    val isFirstRun: StateFlow<Boolean> = _isFirstRun

    // Language state: "ar" or "en"
    private val _appLanguage = MutableStateFlow(prefs.getString(AppPrefs.APP_LANG, "ar") ?: "ar")
    val appLanguage: StateFlow<String> = _appLanguage

    // Theme state: "dark", "light", "system"
    private val _appTheme = MutableStateFlow(prefs.getString(AppPrefs.APP_THEME, "system") ?: "system")
    val appTheme: StateFlow<String> = _appTheme

    // Dynamic color state: boolean (default true)
    private val _useDynamicColor = MutableStateFlow(prefs.getBoolean(AppPrefs.USE_DYNAMIC_COLOR, true))
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor

    // AMOLED Dark state: boolean (default true)
    private val _amoledDark = MutableStateFlow(prefs.getBoolean(AppPrefs.AMOLED_DARK, true))
    val amoledDark: StateFlow<Boolean> = _amoledDark

    // Font states:
    private val _arabicFontSize = MutableStateFlow(prefs.getFloat(AppPrefs.ARABIC_FONT_SIZE, 24f))
    val arabicFontSize: StateFlow<Float> = _arabicFontSize

    private val _englishFontSize = MutableStateFlow(prefs.getFloat(AppPrefs.ENGLISH_FONT_SIZE, 16f))
    val englishFontSize: StateFlow<Float> = _englishFontSize

    // Face used for Quran text only: "system" (default) or "amiri" (Uthmani script)
    private val _quranFont = MutableStateFlow(prefs.getString(AppPrefs.QURAN_FONT, "system") ?: "system")
    val quranFont: StateFlow<String> = _quranFont

    // Selected location method: "auto" or "manual"
    private val _locationMethod = MutableStateFlow(prefs.getString(AppPrefs.LOCATION_METHOD, "manual") ?: "manual")
    val locationMethod: StateFlow<String> = _locationMethod

    private val _showGpsSettingsPrompt = MutableStateFlow(false)
    val showGpsSettingsPrompt: StateFlow<Boolean> = _showGpsSettingsPrompt

    fun setShowGpsSettingsPrompt(show: Boolean) {
        _showGpsSettingsPrompt.value = show
    }

    fun refreshLocationIfAuto() {
        if (locationMethod.value == "auto" && isLocationEnabled()) {
            requestGPSLocation()
            startLocationUpdates()
        }
    }

    private val widgetRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val widgetRefreshMutex = Mutex()

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        locationProvider.startUpdates(minTimeMs = 300_000L, minDistanceM = 200f) { location ->
            val now = System.currentTimeMillis()
            val diffLat = abs(location.latitude - _currentLocation.value.first)
            val diffLng = abs(location.longitude - _currentLocation.value.second)

            if (diffLat > 0.001 || diffLng > 0.001) {
                viewModelScope.launch {
                    val (resolvedName, countryCode) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        getPlaceFromLocation(location.latitude, location.longitude)
                    }
                    val defaultLabel = if (_appLanguage.value == "ar") "تلقائي (GPS)" else "GPS (Auto)"
                    val label = resolvedName ?: defaultLabel

                    updateLocation(location.latitude, location.longitude, label, countryCode = countryCode)

                    if (now - lastGpsSuccessMsgTime > 60000) {
                        lastGpsSuccessMsgTime = now
                        val msg = if (_appLanguage.value == "ar") "تم تحديث موقعك تلقائياً بنجاح ($label)" else "GPS location updated successfully ($label)"
                        _eventFlow.emit(msg)
                    }
                }
            }
        }
    }

    fun stopLocationUpdates() {
        locationProvider.stopUpdates()
    }

    // Nawafil state flow
    private val _showNawafil = MutableStateFlow(prefs.getBoolean(AppPrefs.SHOW_NAWAFIL, false))
    val showNawafil: StateFlow<Boolean> = _showNawafil

    private val _notifyNawafil = MutableStateFlow(prefs.getBoolean(AppPrefs.NOTIFY_NAWAFIL, true))
    val notifyNawafil: StateFlow<Boolean> = _notifyNawafil

    private val _adhanAudioEnabled = MutableStateFlow(prefs.getBoolean(AppPrefs.ADHAN_AUDIO_ENABLED, false))
    val adhanAudioEnabled: StateFlow<Boolean> = _adhanAudioEnabled

    private val _adhanAudioMode = MutableStateFlow(prefs.getString(AppPrefs.ADHAN_AUDIO_MODE, "short") ?: "short")
    val adhanAudioMode: StateFlow<String> = _adhanAudioMode

    /**
     * Which system volume stream the Adhan plays on: "notification" (default) or "alarm".
     *
     * "notification" routes to STREAM_NOTIFICATION so the notification slider controls it,
     * but the Adhan is then suppressed by silent mode and Do Not Disturb. "alarm" routes to
     * STREAM_ALARM, which ignores both, at the cost of a harder-to-reach volume slider.
     */
    private val _adhanAudioStream = MutableStateFlow(prefs.getString(AppPrefs.ADHAN_AUDIO_STREAM, "notification") ?: "notification")
    val adhanAudioStream: StateFlow<String> = _adhanAudioStream

    // Morning and Evening Adhkar Notifications state flow
    private val _notifyMorningAdhkar = MutableStateFlow(prefs.getBoolean(AppPrefs.NOTIFY_MORNING_ADHKAR, true))
    val notifyMorningAdhkar: StateFlow<Boolean> = _notifyMorningAdhkar

    private val _notifyEveningAdhkar = MutableStateFlow(prefs.getBoolean(AppPrefs.NOTIFY_EVENING_ADHKAR, true))
    val notifyEveningAdhkar: StateFlow<Boolean> = _notifyEveningAdhkar

    // Custom offsets (in minutes) for Morning and Evening Adhkar reminders
    private val _morningAdhkarOffset = MutableStateFlow(prefs.getInt(AppPrefs.MORNING_ADHKAR_OFFSET, 30))
    val morningAdhkarOffset: StateFlow<Int> = _morningAdhkarOffset

    private val _eveningAdhkarOffset = MutableStateFlow(prefs.getInt(AppPrefs.EVENING_ADHKAR_OFFSET, 30))
    val eveningAdhkarOffset: StateFlow<Int> = _eveningAdhkarOffset

    // Pre-Adhan notifications (15-min before), Occasions, and Fasting notifications state flows
    private val _notifyBeforeAdhan = MutableStateFlow(prefs.getBoolean(AppPrefs.NOTIFY_BEFORE_ADHAN, true))
    val notifyBeforeAdhan: StateFlow<Boolean> = _notifyBeforeAdhan

    private val _notifyOccasions = MutableStateFlow(prefs.getBoolean(AppPrefs.NOTIFY_OCCASIONS, true))
    val notifyOccasions: StateFlow<Boolean> = _notifyOccasions

    private val _notifyFasting = MutableStateFlow(prefs.getBoolean(AppPrefs.NOTIFY_FASTING, true))
    val notifyFasting: StateFlow<Boolean> = _notifyFasting

    private val _notifyJumuah = MutableStateFlow(prefs.getBoolean(AppPrefs.NOTIFY_JUMUAH, true))
    val notifyJumuah: StateFlow<Boolean> = _notifyJumuah

    private val _notifySuhur = MutableStateFlow(prefs.getBoolean(AppPrefs.NOTIFY_SUHUR, false))
    val notifySuhur: StateFlow<Boolean> = _notifySuhur

    private val _notifyIftar = MutableStateFlow(prefs.getBoolean(AppPrefs.NOTIFY_IFTAR, false))
    val notifyIftar: StateFlow<Boolean> = _notifyIftar

    // Tasbih haptic feedback state: boolean (default true)
    private val _enableTasbihHaptics = MutableStateFlow(prefs.getBoolean(AppPrefs.ENABLE_TASBIH_HAPTICS, true))
    val enableTasbihHaptics: StateFlow<Boolean> = _enableTasbihHaptics

    // Digit style: Western vs Eastern Arabic numbers when in Arabic locale
    private val _useWesternNumbersInArabic = MutableStateFlow(prefs.getBoolean(AppPrefs.USE_WESTERN_NUMBERS_IN_ARABIC, false))
    val useWesternNumbersInArabic: StateFlow<Boolean> = _useWesternNumbersInArabic

    // Settings dialog visibility
    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog

    fun setShowSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    // Juristic / School states
    private val _asrMethod = MutableStateFlow(prefs.getString(AppPrefs.ASR_METHOD, "standard") ?: "standard")
    val asrMethod: StateFlow<String> = _asrMethod

    private val _ishaMethod = MutableStateFlow(prefs.getString(AppPrefs.ISHA_METHOD, "standard") ?: "standard")
    val ishaMethod: StateFlow<String> = _ishaMethod

    // Manual adjustments
    val adjFajr = MutableStateFlow(prefs.getInt(AppPrefs.ADJ_FAJR, 0))
    val adjSunrise = MutableStateFlow(prefs.getInt(AppPrefs.ADJ_SUNRISE, 0))
    val adjDhuhr = MutableStateFlow(prefs.getInt(AppPrefs.ADJ_DHUHR, 0))
    val adjAsr = MutableStateFlow(prefs.getInt(AppPrefs.ADJ_ASR, 0))
    val adjMaghrib = MutableStateFlow(prefs.getInt(AppPrefs.ADJ_MAGHRIB, 0))
    val adjIsha = MutableStateFlow(prefs.getInt(AppPrefs.ADJ_ISHA, 0))

    /**
     * Where the Hijri shift comes from. Countries that wait for a local sighting usually start the
     * month a day after the Umm al-Qura table this app computes from, so their default is -1. A
     * value the user picks is kept; clearing it goes back to the country default.
     */
    private fun countryHijriAdjustment(): Int =
        HijriCalendarHelper.countryAdjustmentDays(prefs.getString(AppPrefs.LOC_COUNTRY, null))

    private fun savedHijriAdjustment(): Int =
        if (prefs.contains(AppPrefs.HIJRI_ADJUSTMENT)) {
            prefs.getInt(AppPrefs.HIJRI_ADJUSTMENT, 0)
        } else {
            countryHijriAdjustment()
        }

    /** Days to shift the calculated Hijri date by, so it can follow the local moon sighting. */
    val hijriAdjustment = MutableStateFlow(savedHijriAdjustment())

    /** False while [hijriAdjustment] is the country default rather than a choice the user made. */
    val hijriAdjustmentIsManual = MutableStateFlow(prefs.contains(AppPrefs.HIJRI_ADJUSTMENT))

    // Cached parsed prayer seconds to eliminate 1s loop allocations
    private var CachedFajrSec = 0
    private var CachedSunriseSec = 0
    private var CachedDhuhrSec = 0
    private var CachedAsrSec = 0
    private var CachedMaghribSec = 0
    private var CachedIshaSec = 0

    // 1. LOCATION STATE
    private val _currentLocation = MutableStateFlow<Pair<Double, Double>>(
        Pair(
            prefs.getFloat(AppPrefs.LOC_LAT, LocationDefaults.LATITUDE.toFloat()).toDouble(),
            prefs.getFloat(AppPrefs.LOC_LNG, LocationDefaults.LONGITUDE.toFloat()).toDouble()
        )
    )
    val currentLocation: StateFlow<Pair<Double, Double>> = _currentLocation

    private val _locationLabel = MutableStateFlow(
        prefs.getString(AppPrefs.LOC_LABEL, null)
            ?.takeIf { !LocationDefaults.isPlaceholder(it) }
            ?: LocationDefaults.unsetLabel(prefs.getString(AppPrefs.APP_LANG, "ar") == "ar")
    )
    val locationLabel: StateFlow<String> = _locationLabel

    private val _isLocationSet = MutableStateFlow(LocationDefaults.isSet(_locationLabel.value))
    val isLocationSet: StateFlow<Boolean> = _isLocationSet

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    private val locationProvider = LocationProvider(application)

    // Manual cities fallback config
    data class City(val name: String, val lat: Double, val lng: Double)
    val manualCities = listOf(
        City(LocationDefaults.MECCA_LABEL, LocationDefaults.LATITUDE, LocationDefaults.LONGITUDE),
        City("London, UK", 51.5074, -0.1278),
        City("Cairo, Egypt", 30.0444, 31.2357),
        City("New York, USA", 40.7128, -74.0060),
        City("Jakarta, Indonesia", -6.2088, 106.8456),
        City("Istanbul, Turkey", 41.0082, 28.9784),
        City("Kuala Lumpur, Malaysia", 3.1390, 101.6869),
        City("Toronto, Canada", 43.6532, -79.3832)
    )

    private val _calculationMethod = MutableStateFlow(
        run {
            try {
                PrayerCalculator.CalculationMethod.valueOf(prefs.getString(AppPrefs.CALC_METHOD, "MWL") ?: "MWL")
            } catch(e: Exception) {
                PrayerCalculator.CalculationMethod.MWL
            }
        }
    )
    val calculationMethod: StateFlow<PrayerCalculator.CalculationMethod> = _calculationMethod

    // 2. PRAYER TIMES STATE initialized dynamically with stored values and proper TZ offset from launch
    private val _prayerTimes = MutableStateFlow(
        run {
            val lat = prefs.getFloat(AppPrefs.LOC_LAT, LocationDefaults.LATITUDE.toFloat()).toDouble()
            val lng = prefs.getFloat(AppPrefs.LOC_LNG, LocationDefaults.LONGITUDE.toFloat()).toDouble()
            val m = try {
                PrayerCalculator.CalculationMethod.valueOf(prefs.getString(AppPrefs.CALC_METHOD, "MWL") ?: "MWL")
            } catch(e: Exception) {
                PrayerCalculator.CalculationMethod.MWL
            }
            val asrM = prefs.getString(AppPrefs.ASR_METHOD, "standard") ?: "standard"
            val ishaM = prefs.getString(AppPrefs.ISHA_METHOD, "standard") ?: "standard"
            val adjF = prefs.getInt(AppPrefs.ADJ_FAJR, 0)
            val adjS = prefs.getInt(AppPrefs.ADJ_SUNRISE, 0)
            val adjD = prefs.getInt(AppPrefs.ADJ_DHUHR, 0)
            val adjA = prefs.getInt(AppPrefs.ADJ_ASR, 0)
            val adjM = prefs.getInt(AppPrefs.ADJ_MAGHRIB, 0)
            val adjI = prefs.getInt(AppPrefs.ADJ_ISHA, 0)
            val isAutoLocation = prefs.getString(AppPrefs.LOCATION_METHOD, "manual") == "auto"
            val timeZoneId = PrayerCalculator.resolveTimeZoneId(
                if (isAutoLocation) TimeZone.getDefault().id else prefs.getString(AppPrefs.LOC_TIMEZONE, null)
            )
            val cal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId))
            val offsetHours = PrayerCalculator.getTimeZoneOffset(timeZoneId, cal)
            PrayerCalculator.calculate(
                latitude = lat,
                longitude = lng,
                timezoneOffset = offsetHours,
                calendar = cal,
                method = m,
                asrMethod = asrM,
                ishaMethod = ishaM,
                adjFajr = adjF,
                adjSunrise = adjS,
                adjDhuhr = adjD,
                adjAsr = adjA,
                adjMaghrib = adjM,
                adjIsha = adjI
            )
        }
    )
    val prayerTimes: StateFlow<PrayerCalculator.PrayerTimes> = _prayerTimes

    // Next Prayer Indicator
    private val _nextPrayerName = MutableStateFlow("Fajr")
    val nextPrayerName: StateFlow<String> = _nextPrayerName

    private val _nextPrayerCountdown = MutableStateFlow("00:00:00")
    val nextPrayerCountdown: StateFlow<String> = _nextPrayerCountdown

    private val _activePrayerName = MutableStateFlow("Dhuhr")
    val activePrayerName: StateFlow<String> = _activePrayerName

    private val _activeHighlightName = MutableStateFlow("Dhuhr")
    val activeHighlightName: StateFlow<String> = _activeHighlightName

    // 3. PERSISTENT TASBIH COUNTER STATE
    val commonDhikrList = listOf(
        Pair("Subhanallah (Glory be to Allah)", "سُبْحَانَ اللَّهِ"),
        Pair("Alhamdulillah (Praise be to Allah)", "الْحَمْدُ لِلَّهِ"),
        Pair("Allahu Akbar (Allah is the Greatest)", "اللَّهُ أَكْبَرُ"),
        Pair("La ilaha illallah (There is no god but Allah)", "لَا إِلَهَ إِلَّا اللَّهُ"),
        Pair("Astaghfirullah (I seek forgiveness from Allah)", "أَسْتَغْفِرُ اللَّهِ"),
        Pair("La hawla wa la quwwata illa billah", "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ")
    )

    private val _selectedDhikr = MutableStateFlow(commonDhikrList[0])
    val selectedDhikr: StateFlow<Pair<String, String>> = _selectedDhikr

    private val _tasbihCount = MutableStateFlow(0)
    val tasbihCount: StateFlow<Int> = _tasbihCount

    private val _tasbihTarget = MutableStateFlow(33) // Default: 33. Can be 33, 99, 100 or 0 (Infinite)
    val tasbihTarget: StateFlow<Int> = _tasbihTarget

    // 4. BOOKMARKS
    val bookmarks: StateFlow<List<SurahBookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. DUAS STATE
    private val _duaSearchQuery = MutableStateFlow("")
    val duaSearchQuery: StateFlow<String> = _duaSearchQuery

    private val _selectedDuaCategory = MutableStateFlow("Menu")
    val selectedDuaCategory: StateFlow<String> = _selectedDuaCategory

    private val _filteredDuas = MutableStateFlow<List<Dua>>(emptyList())
    val filteredDuas: StateFlow<List<Dua>> = _filteredDuas

    val duaBookmarks: StateFlow<List<DuaBookmark>> = repository.allDuaBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alertSettings: StateFlow<List<dev.barakah.app.data.PrayerAlertSetting>> = repository.allAlertSettings
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6. QURAN RESUME STATE
    private val _lastReadingState = MutableStateFlow<LastReadingState?>(null)
    val lastReadingState: StateFlow<LastReadingState?> = _lastReadingState

    // 7. QIBLAH / COMPASS STATE
    val compassAzimuth = sensorManager.compassHeading
    val compassStatus = sensorManager.status
    val compassTilt = sensorManager.tiltDegrees
    private val _qiblaBearing = MutableStateFlow(0.0)
    val qiblaBearing: StateFlow<Double> = _qiblaBearing

    // Coroutines ticks
    private var countdownJob: Job? = null
    private var lastCheckedDayOfYear = -1
    private var lastCheckedTimeZoneId = ""
    private var lastCheckedZoneOffsetMillis = Int.MIN_VALUE

    private val gpsStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == android.location.LocationManager.PROVIDERS_CHANGED_ACTION) {
                refreshLocationIfAuto()
            }
        }
    }

    init {
        try {
            val filter = IntentFilter(android.location.LocationManager.PROVIDERS_CHANGED_ACTION)
            application.registerReceiver(gpsStatusReceiver, filter)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to watch for location services being switched on or off", e)
        }

        // Guarantee app default language is arabic when first open
        if (!prefs.contains(AppPrefs.APP_LANG)) {
            prefs.edit { putString(AppPrefs.APP_LANG, "ar") }
        }

        if (!_isFirstRun.value && _locationMethod.value != "auto" && _isLocationSet.value && !prefs.contains(AppPrefs.LOC_TIMEZONE)) {
            viewModelScope.launch {
                val latitude = _currentLocation.value.first
                val longitude = _currentLocation.value.second
                val migratedTimeZone = CityData.nearestTimeZoneId(application, latitude, longitude)
                if (migratedTimeZone != null && !prefs.contains(AppPrefs.LOC_TIMEZONE)) {
                    updateLocation(latitude, longitude, _locationLabel.value, timeZoneId = migratedTimeZone)
                }
            }
        }

        // Initialize parsed seconds and observe for subsequent dynamic updates
        viewModelScope.launch {
            _prayerTimes.collect { times ->
                updateParsedSeconds(times)
            }
        }

        // Initialize databases & values asynchronously in the background
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            QuranData.load(application)
            DuaData.load(application)
            _filteredDuas.value = if (_appLanguage.value == "ar") DuaData.duasAr else DuaData.duasEn
        }
        viewModelScope.launch {
            repository.initDefaultAlertSettings()
            
            // Load persistent state of selected Dhikr if exists
            val initialDhikrId = _selectedDhikr.value.first
            val persistedState = repository.getTasbihState(initialDhikrId)
            if (persistedState != null) {
                _tasbihCount.value = persistedState.count
                _tasbihTarget.value = persistedState.target
            }
        }

        _qiblaBearing.value = QiblaManager.calculateQiblaBearing(
            _currentLocation.value.first,
            _currentLocation.value.second
        )

        // Observe orientation azimuth for perfect Kaaba alignment vibro pulse
        viewModelScope.launch {
            compassAzimuth.collect { azimuth ->
                checkQiblaAlignment(azimuth)
            }
        }

        // Periodically tick next prayer calculation
        startCountdownTimer()

        // Load reading state
        viewModelScope.launch {
            _lastReadingState.value = repository.getLastReadingState()
        }

        // Combine Dua search, category, and language
        viewModelScope.launch {
            combine(_duaSearchQuery, _selectedDuaCategory, _appLanguage, duaBookmarks) { query, category, lang, bookmarks ->
                val favIds = bookmarks.map { it.duaId }.toSet()
                val currentDuas = if (lang == "ar") DuaData.duasAr else DuaData.duasEn
                val searchResult = currentDuas.filter { dua ->
                    val title = if (lang == "ar") dua.titleAr else dua.titleEn
                    val matchesQuery = query.isEmpty() || title.contains(query, ignoreCase = true) ||
                            dua.arabic.contains(query) ||
                            dua.transliteration.contains(query, ignoreCase = true) ||
                            dua.translation.contains(query, ignoreCase = true)
                    
                    val isFavCat = (category == "Favorites" || category == "المفضلة" || category == "Bookmarks" || category == "الإشارات المرجعية" || category == "الأذكار المحفوظة")
                    val matchesCategory = (isFavCat && favIds.contains(dua.id)) ||
                            (!isFavCat && (category == "All" || category == "الكل" || category == "Menu" ||
                            dua.categoryAr == category || dua.categoryEn == category))
                    
                    matchesQuery && matchesCategory
                }
                _filteredDuas.value = searchResult
            }.collect {}
        }
        updateWidgetsGlobally()
        dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(application)
        if (locationMethod.value == "auto") {
            startLocationUpdates()
        }
    }

    private var lastGpsFailMsgTime = 0L
    private var lastGpsSuccessMsgTime = 0L

    // LOCATION ACTIONS
    @SuppressLint("MissingPermission")
    fun requestGPSLocation() {
        try {
            val context = getApplication<Application>()
            val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!fineGranted && !coarseGranted) {
                fallBackToPlaceholderIfUnset()
                val now = System.currentTimeMillis()
                if (now - lastGpsFailMsgTime > 30000) {
                    lastGpsFailMsgTime = now
                    viewModelScope.launch {
                        val msg = if (_appLanguage.value == "ar") "لم يتم منح صلاحية تحديد الموقع، يرجى تحديد موقعك" else "Location permission not granted, please select your location"
                        _eventFlow.emit(msg)
                    }
                }
                return
            }

            // With location services off there is nothing that can answer the request, so asking
            // would only put a second failure on top of this prompt. Whatever location is already
            // stored is left alone.
            if (!isLocationEnabled()) {
                _showGpsSettingsPrompt.value = true
                return
            }
            locationProvider.currentLocation { loc ->
                if (loc != null) {
                    viewModelScope.launch {
                        val (resolvedName, countryCode) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            getPlaceFromLocation(loc.latitude, loc.longitude)
                        }
                        val defaultLabel = if (_appLanguage.value == "ar") "تلقائي (GPS)" else "GPS (Auto)"
                        val label = resolvedName ?: defaultLabel

                        updateLocation(loc.latitude, loc.longitude, label, countryCode = countryCode)

                        val now = System.currentTimeMillis()
                        val diffLat = abs(loc.latitude - _currentLocation.value.first)
                        val diffLng = abs(loc.longitude - _currentLocation.value.second)

                        // Show success message only if it's a significant change or once in a while
                        if (now - lastGpsSuccessMsgTime > 60000 || diffLat > 0.001 || diffLng > 0.001) {
                            lastGpsSuccessMsgTime = now
                            val msg = if (_appLanguage.value == "ar") "تم تحديد موقعك تلقائياً بنجاح ($label)" else "GPS location detected successfully ($label)"
                            _eventFlow.emit(msg)
                        }
                    }
                } else {
                    // No fix: keep a location that already worked, and report the failure at most
                    // once every 30 seconds.
                    fallBackToPlaceholderIfUnset()
                    val now = System.currentTimeMillis()
                    if (now - lastGpsFailMsgTime > 30000) {
                        lastGpsFailMsgTime = now
                        viewModelScope.launch {
                            val msg = if (_appLanguage.value == "ar") "تعذر تحديد الموقع الجغرافي، يرجى اختياره يدوياً" else "Could not determine GPS location, please select manually"
                            _eventFlow.emit(msg)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to get a GPS fix", e)
            fallBackToPlaceholderIfUnset()
            viewModelScope.launch {
                val msg = if (_appLanguage.value == "ar") "حدث خطأ أثناء تحديد الموقع، يرجى اختياره يدوياً" else "GPS error occurred, please select manually"
                _eventFlow.emit(msg)
            }
        }
    }

    /**
     * What a failed fix leaves behind: an existing location untouched, and the placeholder only
     * when nothing was ever chosen. Blanking a location that already worked is how a phone that
     * was briefly offline lost its city.
     */
    private fun fallBackToPlaceholderIfUnset() {
        if (_isLocationSet.value) return
        updateLocation(
            LocationDefaults.LATITUDE,
            LocationDefaults.LONGITUDE,
            LocationDefaults.unsetLabel(_appLanguage.value == "ar")
        )
    }

    private fun isLocationEnabled(): Boolean = locationProvider.isEnabled()

    private fun getPlaceFromLocation(lat: Double, lng: Double): Pair<String?, String?> {
        return try {
            val context = getApplication<Application>()
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val name = address.locality ?: address.subAdminArea ?: address.adminArea ?: address.countryName
                name to address.countryCode
            } else {
                null to null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to name the location from its coordinates", e)
            null to null
        }
    }

    fun updateLocation(lat: Double, lng: Double, label: String, isSuccessFeedback: Boolean = false, timeZoneId: String? = null, countryCode: String? = null) {
        _currentLocation.value = Pair(lat, lng)
        _locationLabel.value = label
        _isLocationSet.value = LocationDefaults.isSet(label)
        _qiblaBearing.value = QiblaManager.calculateQiblaBearing(lat, lng)
        val selectedTimeZoneId = timeZoneId ?: if (_locationMethod.value == "auto") {
            TimeZone.getDefault().id
        } else {
            prefs.getString(AppPrefs.LOC_TIMEZONE, null)
        }
        val resolvedTimeZoneId = PrayerCalculator.resolveTimeZoneId(selectedTimeZoneId)
        prefs.edit {
            putFloat(AppPrefs.LOC_LAT, lat.toFloat())
            putFloat(AppPrefs.LOC_LNG, lng.toFloat())
            putString(AppPrefs.LOC_LABEL, label)
            putString(AppPrefs.LOC_TIMEZONE, resolvedTimeZoneId)
            if (countryCode != null) putString(AppPrefs.LOC_COUNTRY, countryCode.uppercase())
        }
        if (!hijriAdjustmentIsManual.value) {
            hijriAdjustment.value = countryHijriAdjustment()
        }
        recalculatePrayerTimes()
        if (isSuccessFeedback) {
            viewModelScope.launch {
                val msg = if (_appLanguage.value == "ar") "تم تغيير الموقع بنجاح إلى $label" else "Location updated successfully to $label"
                _eventFlow.emit(msg)
            }
        }
    }

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit { putString(AppPrefs.APP_LANG, lang) }
        _filteredDuas.value = if (lang == "ar") DuaData.duasAr else DuaData.duasEn
        updateWidgetsGlobally()
    }

    /**
     * Retry widget delivery after the activity leaves the foreground. Some launchers
     * do not visibly apply a RemoteViews update until they become the active host again.
     */
    fun refreshWidgetsWhenAppStops() {
        updateWidgetsGlobally()
    }

    private fun updateWidgetsGlobally() {
        val context = getApplication<Application>().applicationContext
        widgetRefreshScope.launch {
            widgetRefreshMutex.withLock {
                val manager = GlanceAppWidgetManager(context)
                val prayerWidget = PrayerWidget()
                val remainingWidget = PrayerRemainingWidget()
                val nawafilWidget = NawafilWidget()

                try {
                    refreshWidgetInstances(
                        context,
                        prayerWidget,
                        manager.getGlanceIds(PrayerWidget::class.java)
                    )
                } catch (e: Exception) {
                    android.util.Log.e("BarakahViewModel", "Error updating PrayerWidget", e)
                }
                try {
                    refreshWidgetInstances(
                        context,
                        remainingWidget,
                        manager.getGlanceIds(PrayerRemainingWidget::class.java)
                    )
                } catch (e: Exception) {
                    android.util.Log.e("BarakahViewModel", "Error updating PrayerRemainingWidget", e)
                }
                try {
                    refreshWidgetInstances(
                        context,
                        nawafilWidget,
                        manager.getGlanceIds(NawafilWidget::class.java)
                    )
                } catch (e: Exception) {
                    android.util.Log.e("BarakahViewModel", "Error updating NawafilWidget", e)
                }
            }
        }
    }

    fun setAppTheme(theme: String) {
        _appTheme.value = theme
        prefs.edit { putString(AppPrefs.APP_THEME, theme) }
        // Keeps the system's idea of the app's theme in step, which is what the splash screen uses.
        AppNightMode.apply(getApplication(), theme)
    }

    fun setUseDynamicColor(use: Boolean) {
        _useDynamicColor.value = use
        prefs.edit { putBoolean(AppPrefs.USE_DYNAMIC_COLOR, use) }
    }

    fun setAmoledDark(enable: Boolean) {
        _amoledDark.value = enable
        prefs.edit { putBoolean(AppPrefs.AMOLED_DARK, enable) }
    }

    fun setArabicFontSize(size: Float) {
        _arabicFontSize.value = size
        prefs.edit { putFloat(AppPrefs.ARABIC_FONT_SIZE, size) }
    }

    fun setEnglishFontSize(size: Float) {
        _englishFontSize.value = size
        prefs.edit { putFloat(AppPrefs.ENGLISH_FONT_SIZE, size) }
    }

    fun setQuranFont(font: String) {
        _quranFont.value = font
        prefs.edit { putString(AppPrefs.QURAN_FONT, font) }
    }

    fun setLocationMethod(method: String) {
        _locationMethod.value = method
        prefs.edit { putString(AppPrefs.LOCATION_METHOD, method) }
        if (method == "auto") {
            // An explicit switch to auto is answered the first time, rather than being swallowed by
            // the throttle on an earlier attempt.
            lastGpsFailMsgTime = 0
            requestGPSLocation()
            startLocationUpdates()
        } else {
            stopLocationUpdates()
        }
    }

    fun setEnableTasbihHaptics(enable: Boolean) {
        _enableTasbihHaptics.value = enable
        prefs.edit { putBoolean(AppPrefs.ENABLE_TASBIH_HAPTICS, enable) }
    }

    fun setUseWesternNumbersInArabic(enable: Boolean) {
        _useWesternNumbersInArabic.value = enable
        prefs.edit { putBoolean(AppPrefs.USE_WESTERN_NUMBERS_IN_ARABIC, enable) }
        updateWidgetsGlobally()
    }

    fun setFirstRunComplete() {
        _isFirstRun.value = false
        prefs.edit { putBoolean(AppPrefs.IS_FIRST_RUN, false) }
    }

    fun saveLastReading(surahId: Int, ayahNumber: Int, surahName: String) {
        viewModelScope.launch {
            val state = LastReadingState(id = 1, surahId = surahId, ayahNumber = ayahNumber, surahName = surahName)
            repository.saveLastReadingState(state)
            _lastReadingState.value = state
        }
    }

    fun toggleDuaBookmark(duaId: String) {
        viewModelScope.launch {
            val bookmarks = duaBookmarks.value
            if (bookmarks.any { it.duaId == duaId }) {
                repository.deleteDuaBookmark(duaId)
            } else {
                repository.saveDuaBookmark(DuaBookmark(duaId))
            }
        }
    }

    fun togglePrayerAlert(prayerName: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val setting = alertSettings.value.find { it.prayerName == prayerName } 
                ?: dev.barakah.app.data.PrayerAlertSetting(prayerName, isEnabled)
            repository.saveAlertSetting(setting.copy(isEnabled = isEnabled))
            try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
        }
    }

    fun selectMethod(method: PrayerCalculator.CalculationMethod) {
        _calculationMethod.value = method
        prefs.edit { putString(AppPrefs.CALC_METHOD, method.name) }
        recalculatePrayerTimes()
    }

    private fun recalculatePrayerTimes() {
        val timeZoneId = currentTimeZoneId()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId))
        val offsetHours = PrayerCalculator.getTimeZoneOffset(timeZoneId, calendar)

        val newTimes = PrayerCalculator.calculate(
            latitude = _currentLocation.value.first,
            longitude = _currentLocation.value.second,
            timezoneOffset = offsetHours,
            calendar = calendar,
            method = _calculationMethod.value,
            asrMethod = _asrMethod.value,
            ishaMethod = _ishaMethod.value,
            adjFajr = adjFajr.value,
            adjSunrise = adjSunrise.value,
            adjDhuhr = adjDhuhr.value,
            adjAsr = adjAsr.value,
            adjMaghrib = adjMaghrib.value,
            adjIsha = adjIsha.value
        )
        _prayerTimes.value = newTimes
        updateParsedSeconds(newTimes)
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
        updateWidgetsGlobally()
    }

    private fun currentTimeZoneId(): String {
        val selectedTimeZoneId = if (_locationMethod.value == "auto") {
            TimeZone.getDefault().id
        } else {
            prefs.getString(AppPrefs.LOC_TIMEZONE, null)
        }
        return PrayerCalculator.resolveTimeZoneId(selectedTimeZoneId)
    }

    fun setShowNawafil(value: Boolean) {
        _showNawafil.value = value
        prefs.edit { putBoolean(AppPrefs.SHOW_NAWAFIL, value) }
        recalculatePrayerTimes()
    }

    fun setNotifyNawafil(value: Boolean) {
        _notifyNawafil.value = value
        prefs.edit { putBoolean(AppPrefs.NOTIFY_NAWAFIL, value) }
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setAdhanAudioEnabled(value: Boolean) {
        _adhanAudioEnabled.value = value
        prefs.edit { putBoolean(AppPrefs.ADHAN_AUDIO_ENABLED, value) }
    }

    fun setAdhanAudioMode(value: String) {
        if (value != "full" && value != "short") return
        _adhanAudioMode.value = value
        prefs.edit { putString(AppPrefs.ADHAN_AUDIO_MODE, value) }
    }

    fun setAdhanAudioStream(value: String) {
        if (value != "notification" && value != "alarm") return
        _adhanAudioStream.value = value
        prefs.edit { putString(AppPrefs.ADHAN_AUDIO_STREAM, value) }
    }

    fun setNotifyMorningAdhkar(value: Boolean) {
        _notifyMorningAdhkar.value = value
        prefs.edit { putBoolean(AppPrefs.NOTIFY_MORNING_ADHKAR, value) }
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setMorningAdhkarOffset(value: Int) {
        _morningAdhkarOffset.value = value
        prefs.edit { putInt(AppPrefs.MORNING_ADHKAR_OFFSET, value) }
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setNotifyEveningAdhkar(value: Boolean) {
        _notifyEveningAdhkar.value = value
        prefs.edit { putBoolean(AppPrefs.NOTIFY_EVENING_ADHKAR, value) }
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setEveningAdhkarOffset(value: Int) {
        _eveningAdhkarOffset.value = value
        prefs.edit { putInt(AppPrefs.EVENING_ADHKAR_OFFSET, value) }
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setNotifyBeforeAdhan(value: Boolean) {
        _notifyBeforeAdhan.value = value
        prefs.edit { putBoolean(AppPrefs.NOTIFY_BEFORE_ADHAN, value) }
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setNotifyOccasions(value: Boolean) {
        _notifyOccasions.value = value
        prefs.edit { putBoolean(AppPrefs.NOTIFY_OCCASIONS, value) }
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setNotifyFasting(value: Boolean) {
        _notifyFasting.value = value
        prefs.edit { putBoolean(AppPrefs.NOTIFY_FASTING, value) }
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setNotifyJumuah(value: Boolean) {
        _notifyJumuah.value = value
        prefs.edit { putBoolean(AppPrefs.NOTIFY_JUMUAH, value) }
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setNotifySuhur(value: Boolean) {
        _notifySuhur.value = value
        prefs.edit { putBoolean(AppPrefs.NOTIFY_SUHUR, value) }
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setNotifyIftar(value: Boolean) {
        _notifyIftar.value = value
        prefs.edit { putBoolean(AppPrefs.NOTIFY_IFTAR, value) }
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setAsrMethod(value: String) {
        _asrMethod.value = value
        prefs.edit { putString(AppPrefs.ASR_METHOD, value) }
        recalculatePrayerTimes()
    }

    fun setIshaMethod(value: String) {
        _ishaMethod.value = value
        prefs.edit { putString(AppPrefs.ISHA_METHOD, value) }
        recalculatePrayerTimes()
    }



    fun setAdjFajr(value: Int) {
        adjFajr.value = value
        prefs.edit { putInt(AppPrefs.ADJ_FAJR, value) }
        recalculatePrayerTimes()
    }

    fun setAdjSunrise(value: Int) {
        adjSunrise.value = value
        prefs.edit { putInt(AppPrefs.ADJ_SUNRISE, value) }
        recalculatePrayerTimes()
    }

    fun setAdjDhuhr(value: Int) {
        adjDhuhr.value = value
        prefs.edit { putInt(AppPrefs.ADJ_DHUHR, value) }
        recalculatePrayerTimes()
    }

    fun setAdjAsr(value: Int) {
        adjAsr.value = value
        prefs.edit { putInt(AppPrefs.ADJ_ASR, value) }
        recalculatePrayerTimes()
    }

    fun setAdjMaghrib(value: Int) {
        adjMaghrib.value = value
        prefs.edit { putInt(AppPrefs.ADJ_MAGHRIB, value) }
        recalculatePrayerTimes()
    }

    fun setAdjIsha(value: Int) {
        adjIsha.value = value
        prefs.edit { putInt(AppPrefs.ADJ_ISHA, value) }
        recalculatePrayerTimes()
    }

    /**
     * Shifts the Hijri date by [value] days. Prayer times are unaffected, so nothing has to be
     * rescheduled; the screens that show the date observe [hijriAdjustment] and redraw.
     */
    fun setHijriAdjustment(value: Int) {
        hijriAdjustment.value = value
        hijriAdjustmentIsManual.value = true
        prefs.edit { putInt(AppPrefs.HIJRI_ADJUSTMENT, value) }
    }

    /** Drops the manual shift and goes back to the saved location's country default. */
    fun resetHijriAdjustment() {
        prefs.edit { remove(AppPrefs.HIJRI_ADJUSTMENT) }
        hijriAdjustmentIsManual.value = false
        hijriAdjustment.value = countryHijriAdjustment()
    }

    fun resetToDefaults() {
        setAppTheme("system")
        setUseDynamicColor(true)
        setAmoledDark(true)
        setArabicFontSize(24f)
        setEnglishFontSize(16f)
        setLocationMethod("manual")
        val defaultLabel = LocationDefaults.unsetLabel(prefs.getString(AppPrefs.APP_LANG, "ar") == "ar")
        updateLocation(LocationDefaults.LATITUDE, LocationDefaults.LONGITUDE, defaultLabel, false)
        setAsrMethod("standard")
        setIshaMethod("standard")
        setShowNawafil(false)
        setEnableTasbihHaptics(true)
        setAdjFajr(0)
        setAdjSunrise(0)
        setAdjDhuhr(0)
        setAdjAsr(0)
        setAdjMaghrib(0)
        setAdjIsha(0)
        setHijriAdjustment(0)
    }



    private fun updateParsedSeconds(times: PrayerCalculator.PrayerTimes) {
        fun parseTimeToSec(timeStr: String): Int {
            return try {
                val parts = timeStr.split(":")
                val h = parts[0].toInt()
                val m = parts[1].split(" ")[0].trim().toInt()
                h * 3600 + m * 60
            } catch (e: Exception) {
                0
            }
        }
        CachedFajrSec = parseTimeToSec(times.fajr)
        CachedSunriseSec = parseTimeToSec(times.sunrise)
        CachedDhuhrSec = parseTimeToSec(times.dhuhr)
        CachedAsrSec = parseTimeToSec(times.asr)
        CachedMaghribSec = parseTimeToSec(times.maghrib)
        CachedIshaSec = parseTimeToSec(times.isha)
    }

    // COUNTER ACTIONS
    fun selectDhikr(dhikr: Pair<String, String>) {
        // Save current first
        val oldDhikrId = _selectedDhikr.value.first
        val oldCount = _tasbihCount.value
        val oldTarget = _tasbihTarget.value
        viewModelScope.launch {
            repository.saveTasbihState(TasbihState(oldDhikrId, oldCount, oldTarget))

            // Swap
            _selectedDhikr.value = dhikr

            // Load new
            val newState = repository.getTasbihState(dhikr.first)
            if (newState != null) {
                _tasbihCount.value = newState.count
                _tasbihTarget.value = newState.target
            } else {
                _tasbihCount.value = 0
                _tasbihTarget.value = 33 // Default fallback
            }
        }
    }

    fun incrementTasbih() {
        val target = _tasbihTarget.value
        val oldVal = _tasbihCount.value
        val newVal = oldVal + 1
        _tasbihCount.value = newVal

        // Trigger vibration
        if (target in 1..newVal) {
            _tasbihCount.value = 0 // Auto reset on target hit
            if (_enableTasbihHaptics.value) {
                // Long vibration on target hit
                triggerVibration(250)
            }
        } else {
            if (_enableTasbihHaptics.value) {
                // Soft ripple vibration
                triggerVibration(30)
            }
        }

        // Persist
        viewModelScope.launch {
            repository.saveTasbihState(TasbihState(_selectedDhikr.value.first, _tasbihCount.value, target))
        }
    }

    fun resetTasbih() {
        _tasbihCount.value = 0
        if (_enableTasbihHaptics.value) {
            triggerVibration(80)
        }
        viewModelScope.launch {
            repository.saveTasbihState(TasbihState(_selectedDhikr.value.first, 0, _tasbihTarget.value))
        }
    }

    fun setTasbihTarget(target: Int) {
        _tasbihTarget.value = target
        triggerVibration(100)
        viewModelScope.launch {
            repository.saveTasbihState(TasbihState(_selectedDhikr.value.first, _tasbihCount.value, target))
        }
    }

    // BOOKMARKS ACTIONS
    fun toggleBookmark(surah: Surah) {
        viewModelScope.launch {
            val exists = repository.getBookmarkBySurah(surah.id)
            if (exists != null) {
                repository.deleteBookmark(surah.id)
            } else {
                repository.saveBookmark(
                    SurahBookmark(
                        surahId = surah.id,
                        surahName = surah.arabic,
                        transliteration = surah.name
                    )
                )
            }
        }
    }

    // SUPPLICATIONS ACTIONS
    fun resetScreenState(route: String) {
        when (route) {
            "duas" -> {
                _duaSearchQuery.value = ""
                _selectedDuaCategory.value = "Menu"
            }
        }
    }

    fun updateDuaSearch(query: String) {
        _duaSearchQuery.value = query
    }

    fun selectDuaCategory(category: String) {
        _selectedDuaCategory.value = category
    }

    // QIBLA LOCK FEEDBACK
    private var isQiblaScreenActive = false
    private var qiblaWatchdogJob: Job? = null

    fun startQiblaTracking() {
        isQiblaScreenActive = true
        sensorManager.start()
        // Sensors report many times a second; if they go quiet while the dial is on screen,
        // the manager flags it so the UI can say why the needle stopped moving.
        qiblaWatchdogJob?.cancel()
        qiblaWatchdogJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                sensorManager.checkForStall()
            }
        }
    }

    fun stopQiblaTracking() {
        isQiblaScreenActive = false
        qiblaWatchdogJob?.cancel()
        qiblaWatchdogJob = null
        sensorManager.stop()
    }

    private val _isQiblaAligned = MutableStateFlow(false)

    private fun checkQiblaAlignment(compassAzimuth: Float) {
        if (!isQiblaScreenActive) return
        if (!_enableTasbihHaptics.value) return
        val target = _qiblaBearing.value.toFloat()
        val diff = abs(compassAzimuth - target)
        // Normalize range
        val normDiff = if (diff > 180f) 360f - diff else diff

        if (normDiff < 4.0) { // Aligned within 4 degrees
            if (!_isQiblaAligned.value) {
                _isQiblaAligned.value = true
                triggerVibration(100)
            }
        } else {
            _isQiblaAligned.value = false
        }
    }

    fun triggerVibration(duration: Long) {
        if (!_enableTasbihHaptics.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to vibrate the device", e)
        }
    }

    // COUNTDOWN & ACTIVE PRAYER TICK
    private fun startCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                calculateNextPrayer()
                // Align delay exactly to the start of the next wall-clock second to prevent drift, skipping, or double ticks
                val nextSecondDelay = 1000 - (System.currentTimeMillis() % 1000)
                delay(nextSecondDelay)
            }
        }
    }

    private fun calculateNextPrayer() {
        val tzId = currentTimeZoneId()
        val now = Calendar.getInstance(TimeZone.getTimeZone(tzId))
        val dayOfYear = now.get(Calendar.DAY_OF_YEAR)
        val zoneOffsetMillis = now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET)

        // Detect day-change or timezone/DST transitions and trigger live recalculation. The zone id
        // alone is not enough: a daylight saving change keeps the id and the day the same and only
        // moves the offset, so the times would stay an hour out - and the countdown with them - for
        // the rest of the day.
        if ((lastCheckedDayOfYear != -1 && lastCheckedDayOfYear != dayOfYear) ||
            (lastCheckedTimeZoneId.isNotEmpty() && lastCheckedTimeZoneId != tzId) ||
            (lastCheckedZoneOffsetMillis != Int.MIN_VALUE && lastCheckedZoneOffsetMillis != zoneOffsetMillis)) {
            recalculatePrayerTimes()
        }
        lastCheckedDayOfYear = dayOfYear
        lastCheckedTimeZoneId = tzId
        lastCheckedZoneOffsetMillis = zoneOffsetMillis

        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentSecond = now.get(Calendar.SECOND)
        val currentTimeInSec = currentHour * 3600 + currentMinute * 60 + currentSecond

        val fajrSec = CachedFajrSec
        val sunriseSec = CachedSunriseSec
        val dhuhrSec = CachedDhuhrSec
        val asrSec = CachedAsrSec
        val maghribSec = CachedMaghribSec
        val ishaSec = CachedIshaSec

        // Find active and next prayer
        val active: String
        val nextName: String
        val nextSec: Int
        var isNextDay = false

        when {
            currentTimeInSec < fajrSec -> {
                active = "Isha"
                nextName = "Fajr"
                nextSec = fajrSec
            }
            currentTimeInSec < sunriseSec -> {
                active = "Fajr"
                nextName = "Sunrise"
                nextSec = sunriseSec
            }
            currentTimeInSec < dhuhrSec -> {
                active = "Sunrise / Duha"
                nextName = "Dhuhr"
                nextSec = dhuhrSec
            }
            currentTimeInSec < asrSec -> {
                active = "Dhuhr"
                nextName = "Asr"
                nextSec = asrSec
            }
            currentTimeInSec < maghribSec -> {
                active = "Asr"
                nextName = "Maghrib"
                nextSec = maghribSec
            }
            currentTimeInSec < ishaSec -> {
                active = "Maghrib"
                nextName = "Isha"
                nextSec = ishaSec
            }
            else -> {
                active = "Isha"
                nextName = "Fajr"
                nextSec = fajrSec
                isNextDay = true
            }
        }

        _activePrayerName.value = active
        _nextPrayerName.value = nextName

        // Compute exact timeline active item (consistent across show_nawafil state to keep highlighting active)
        val highlight: String
        if (_showNawafil.value) {
            fun relativeSec(s: Int): Int {
                val normalized = (s % 86400 + 86400) % 86400
                return if (normalized < fajrSec) normalized + 86400 else normalized
            }

            val t = if (currentTimeInSec < fajrSec) currentTimeInSec + 24 * 3600 else currentTimeInSec

            val rSunrise = sunriseSec
            val rDuha = sunriseSec + 20 * 60
            val rDhuhr = dhuhrSec
            val rAsr = asrSec
            val rMaghrib = maghribSec
            val rIsha = ishaSec

            var adjustedFajr = fajrSec
            if (adjustedFajr < maghribSec) {
                adjustedFajr += 24 * 3600
            }
            val nightDuration = adjustedFajr - maghribSec
            val firstThirdSec = maghribSec + nightDuration / 3
            val midnightSec = maghribSec + nightDuration / 2
            val lastThirdSec = maghribSec + (nightDuration * 2) / 3
            val witrSec = ishaSec + 30 * 60

            val rWitr = relativeSec(witrSec)
            val rFirstThird = relativeSec(firstThirdSec)
            val rMidnight = relativeSec(midnightSec)
            val rLastThird = relativeSec(lastThirdSec)
            val rNextFajr = fajrSec + 24 * 3600

            when {
                t < rSunrise -> highlight = "Fajr"
                t < rDuha -> highlight = "Sunrise"
                t < rDhuhr -> highlight = "Duha (Nafilah)"
                t < rAsr -> highlight = "Dhuhr"
                t < rMaghrib -> highlight = "Asr"
                t < rIsha -> highlight = "Maghrib"
                t < rWitr -> highlight = "Isha"
                t < rFirstThird -> highlight = "Witr (Nafilah)"
                t < rMidnight -> highlight = "First Third (Nafilah)"
                t < rLastThird -> highlight = "Midnight (Nafilah)"
                t < rNextFajr -> highlight = "Qiyam Last Third (Nafilah)"
                else -> highlight = "Fajr"
            }
        } else {
            when {
                currentTimeInSec < fajrSec -> highlight = "Isha"
                currentTimeInSec < sunriseSec -> highlight = "Fajr"
                currentTimeInSec < dhuhrSec -> highlight = "Sunrise"
                currentTimeInSec < asrSec -> highlight = "Dhuhr"
                currentTimeInSec < maghribSec -> highlight = "Asr"
                currentTimeInSec < ishaSec -> highlight = "Maghrib"
                else -> highlight = "Isha"
            }
        }
        _activeHighlightName.value = highlight

        var diff = if (isNextDay) {
            (24 * 3600 - currentTimeInSec) + nextSec
        } else {
            nextSec - currentTimeInSec
        }
        if (diff < 0) diff = 0

        val h = diff / 3600
        val m = (diff % 3600) / 60
        val s = diff % 60

        _nextPrayerCountdown.value = java.lang.String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stop()
        qiblaWatchdogJob?.cancel()
        countdownJob?.cancel()
        widgetRefreshScope.cancel()
        stopLocationUpdates()
        try {
            getApplication<Application>().unregisterReceiver(gpsStatusReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to stop watching for location services", e)
        }
    }
}
