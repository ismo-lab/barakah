package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.PrayerCalculator
import com.example.util.QiblaManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class BarakahViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = AppRepository(db)

    // Hardware managers
    private val sensorManager = QiblaManager(application)
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator ?: (application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
    } else {
        application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val prefs = application.getSharedPreferences("barakah_prefs", Context.MODE_PRIVATE)

    // First Run state
    private val _isFirstRun = MutableStateFlow(prefs.getBoolean("is_first_run", true))
    val isFirstRun: StateFlow<Boolean> = _isFirstRun

    // Language state: "ar" or "en"
    private val _appLanguage = MutableStateFlow(prefs.getString("app_lang", "ar") ?: "ar")
    val appLanguage: StateFlow<String> = _appLanguage

    // Theme state: "dark", "light", "system"
    private val _appTheme = MutableStateFlow(prefs.getString("app_theme", "system") ?: "system")
    val appTheme: StateFlow<String> = _appTheme

    // Dynamic color state: boolean (default true)
    private val _useDynamicColor = MutableStateFlow(prefs.getBoolean("use_dynamic_color", true))
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor

    // AMOLED Dark state: boolean (default true)
    private val _amoledDark = MutableStateFlow(prefs.getBoolean("amoled_dark", true))
    val amoledDark: StateFlow<Boolean> = _amoledDark

    // Font states:
    private val _arabicFontSize = MutableStateFlow(prefs.getFloat("arabic_font_size", 24f))
    val arabicFontSize: StateFlow<Float> = _arabicFontSize

    private val _englishFontSize = MutableStateFlow(prefs.getFloat("english_font_size", 16f))
    val englishFontSize: StateFlow<Float> = _englishFontSize

    // Selected location method: "auto" or "manual"
    private val _locationMethod = MutableStateFlow(prefs.getString("location_method", "manual") ?: "manual")
    val locationMethod: StateFlow<String> = _locationMethod

    // Adhan settings states
    private val _enableAdhanSound = MutableStateFlow(prefs.getBoolean("enable_adhan_sound", false))
    val enableAdhanSound: StateFlow<Boolean> = _enableAdhanSound

    private val _adhanSoundType = MutableStateFlow(prefs.getString("adhan_sound_type", "short") ?: "short")
    val adhanSoundType: StateFlow<String> = _adhanSoundType

    // Nawafil state flow
    private val _showNawafil = MutableStateFlow(prefs.getBoolean("show_nawafil", false))
    val showNawafil: StateFlow<Boolean> = _showNawafil

    // Settings dialog visibility
    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog

    fun setShowSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    // Juristic / School states
    private val _asrMethod = MutableStateFlow(prefs.getString("asr_method", "standard") ?: "standard")
    val asrMethod: StateFlow<String> = _asrMethod

    private val _ishaMethod = MutableStateFlow(prefs.getString("isha_method", "standard") ?: "standard")
    val ishaMethod: StateFlow<String> = _ishaMethod

    // Manual adjustments
    val adjFajr = MutableStateFlow(prefs.getInt("adj_fajr", 0))
    val adjSunrise = MutableStateFlow(prefs.getInt("adj_sunrise", 0))
    val adjDhuhr = MutableStateFlow(prefs.getInt("adj_dhuhr", 0))
    val adjAsr = MutableStateFlow(prefs.getInt("adj_asr", 0))
    val adjMaghrib = MutableStateFlow(prefs.getInt("adj_maghrib", 0))
    val adjIsha = MutableStateFlow(prefs.getInt("adj_isha", 0))

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
            prefs.getFloat("loc_lat", 21.4225f).toDouble(),
            prefs.getFloat("loc_lng", 39.8262f).toDouble()
        )
    )
    val currentLocation: StateFlow<Pair<Double, Double>> = _currentLocation

    private val _locationLabel = MutableStateFlow(prefs.getString("loc_label", "Mecca, KSA") ?: "Mecca, KSA")
    val locationLabel: StateFlow<String> = _locationLabel

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    private val locationClient = LocationServices.getFusedLocationProviderClient(application)

    // Manual cities fallback config
    data class City(val name: String, val lat: Double, val lng: Double)
    val manualCities = listOf(
        City("Mecca, KSA", 21.4225, 39.8262),
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
                PrayerCalculator.CalculationMethod.valueOf(prefs.getString("calc_method", "MWL") ?: "MWL")
            } catch(e: Exception) {
                PrayerCalculator.CalculationMethod.MWL
            }
        }
    )
    val calculationMethod: StateFlow<PrayerCalculator.CalculationMethod> = _calculationMethod

    // 2. PRAYER TIMES STATE initialized dynamically with stored values and proper TZ offset from launch
    private val _prayerTimes = MutableStateFlow(
        run {
            val lat = prefs.getFloat("loc_lat", 21.4225f).toDouble()
            val lng = prefs.getFloat("loc_lng", 39.8262f).toDouble()
            val m = try {
                PrayerCalculator.CalculationMethod.valueOf(prefs.getString("calc_method", "MWL") ?: "MWL")
            } catch(e: Exception) {
                PrayerCalculator.CalculationMethod.MWL
            }
            val asrM = prefs.getString("asr_method", "standard") ?: "standard"
            val ishaM = prefs.getString("isha_method", "standard") ?: "standard"
            val adjF = prefs.getInt("adj_fajr", 0)
            val adjS = prefs.getInt("adj_sunrise", 0)
            val adjD = prefs.getInt("adj_dhuhr", 0)
            val adjA = prefs.getInt("adj_asr", 0)
            val adjM = prefs.getInt("adj_maghrib", 0)
            val adjI = prefs.getInt("adj_isha", 0)
            val cal = Calendar.getInstance()
            val tz = TimeZone.getDefault()
            val offsetHours = tz.getOffset(cal.timeInMillis) / 3600000.0
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

    val alertSettings: StateFlow<List<com.example.data.PrayerAlertSetting>> = repository.allAlertSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6. QURAN RESUME STATE
    private val _lastReadingState = MutableStateFlow<LastReadingState?>(null)
    val lastReadingState: StateFlow<LastReadingState?> = _lastReadingState

    // 7. QIBLAH / COMPASS STATE
    val compassAzimuth = sensorManager.compassHeading
    private val _qiblaBearing = MutableStateFlow(0.0)
    val qiblaBearing: StateFlow<Double> = _qiblaBearing

    // Haptic Alignment Lock
    private var lastAlignmentPulseTime = 0L

    // Coroutines ticks
    private var countdownJob: Job? = null

    init {
        // Initialize parsed seconds
        updateParsedSeconds(_prayerTimes.value)

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

        // Start Qibla orientation tracking
        sensorManager.start()
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
    }

    private var lastGpsOffMsgTime = 0L
    private var lastGpsSuccessMsgTime = 0L

    // LOCATION ACTIONS
    @SuppressLint("MissingPermission")
    fun requestGPSLocation() {
        try {
            val isGpsEnabled = isLocationEnabled()
            if (!isGpsEnabled) {
                val now = System.currentTimeMillis()
                if (now - lastGpsOffMsgTime > 30000) { // Only once every 30 seconds
                    lastGpsOffMsgTime = now
                    viewModelScope.launch {
                        val msg = if (_appLanguage.value == "ar") "يرجى تفعيل خدمة تحديد الموقع (GPS)" else "Please turn on your device GPS / Location services"
                        _eventFlow.emit(msg)
                    }
                }
            }
            locationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        val now = System.currentTimeMillis()
                        val diffLat = abs(loc.latitude - _currentLocation.value.first)
                        val diffLng = abs(loc.longitude - _currentLocation.value.second)
                        
                        updateLocation(loc.latitude, loc.longitude, "GPS (Auto)")
                        
                        // Show success message only if it's a significant change or once in a while
                        if (now - lastGpsSuccessMsgTime > 60000 || diffLat > 0.001 || diffLng > 0.001) {
                            lastGpsSuccessMsgTime = now
                            viewModelScope.launch {
                                val msg = if (_appLanguage.value == "ar") "تم تحديد موقعك تلقائياً بنجاح" else "GPS location detected successfully"
                                _eventFlow.emit(msg)
                            }
                        }
                    } else {
                        // try last known location as fallback
                        locationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                updateLocation(lastLoc.latitude, lastLoc.longitude, "GPS (Auto)")
                                viewModelScope.launch {
                                    val msg = if (_appLanguage.value == "ar") "تم تحديد موقعك تلقائياً بنجاح" else "GPS location detected successfully"
                                    _eventFlow.emit(msg)
                                }
                            } else {
                                // Fallback ONLY to Mecca as requested
                                val label = if (_appLanguage.value == "ar") "مكة المكرمة (تلقائي)" else "Mecca (GPS Fallback)"
                                updateLocation(21.4225, 39.8262, label)
                                viewModelScope.launch {
                                    val msg = if (_appLanguage.value == "ar") "تعذر تحديد الموقع الجغرافي، تم استخدام مكة المكرمة كافتراضي" else "Could not determine GPS location, falling back to Mecca"
                                    _eventFlow.emit(msg)
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    // Fallback ONLY to Mecca
                    val label = if (_appLanguage.value == "ar") "مكة المكرمة (تلقائي)" else "Mecca (GPS Fallback)"
                    updateLocation(21.4225, 39.8262, label)
                    viewModelScope.launch {
                        val msg = if (_appLanguage.value == "ar") "فشل تحديد الموقع الجغرافي، تم استخدام مكة المكرمة كافتراضي" else "GPS location failed, falling back to Mecca"
                        _eventFlow.emit(msg)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback ONLY to Mecca
            val label = if (_appLanguage.value == "ar") "مكة المكرمة (تلقائي)" else "Mecca (GPS Fallback)"
            updateLocation(21.4225, 39.8262, label)
            viewModelScope.launch {
                val msg = if (_appLanguage.value == "ar") "حدث خطأ أثناء تحديد الموقع، تم استخدام مكة المكرمة كافتراضي" else "GPS error occurred, falling back to Mecca"
                _eventFlow.emit(msg)
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        return try {
            val locationManager = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }

    fun updateLocation(lat: Double, lng: Double, label: String, isSuccessFeedback: Boolean = false) {
        _currentLocation.value = Pair(lat, lng)
        _locationLabel.value = label
        _qiblaBearing.value = QiblaManager.calculateQiblaBearing(lat, lng)
        prefs.edit().putFloat("loc_lat", lat.toFloat()).putFloat("loc_lng", lng.toFloat()).putString("loc_label", label).apply()
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
        prefs.edit().putString("app_lang", lang).apply()
    }

    fun setAppTheme(theme: String) {
        _appTheme.value = theme
        prefs.edit().putString("app_theme", theme).apply()
    }

    fun setUseDynamicColor(use: Boolean) {
        _useDynamicColor.value = use
        prefs.edit().putBoolean("use_dynamic_color", use).apply()
    }

    fun setAmoledDark(enable: Boolean) {
        _amoledDark.value = enable
        prefs.edit().putBoolean("amoled_dark", enable).apply()
    }

    fun setArabicFontSize(size: Float) {
        _arabicFontSize.value = size
        prefs.edit().putFloat("arabic_font_size", size).apply()
    }

    fun setEnglishFontSize(size: Float) {
        _englishFontSize.value = size
        prefs.edit().putFloat("english_font_size", size).apply()
    }

    fun setLocationMethod(method: String) {
        _locationMethod.value = method
        prefs.edit().putString("location_method", method).apply()
        if (method == "auto") {
            lastGpsOffMsgTime = 0 // Reset to allow immediate feedback when switching to auto
            requestGPSLocation()
        }
    }

    fun setEnableAdhanSound(enable: Boolean) {
        _enableAdhanSound.value = enable
        prefs.edit().putBoolean("enable_adhan_sound", enable).apply()
    }

    fun setAdhanSoundType(type: String) {
        _adhanSoundType.value = type
        prefs.edit().putString("adhan_sound_type", type).apply()
    }

    fun setFirstRunComplete() {
        _isFirstRun.value = false
        prefs.edit().putBoolean("is_first_run", false).apply()
    }

    fun saveLastReading(surahId: Int, ayahNumber: Int, surahName: String) {
        viewModelScope.launch {
            val state = LastReadingState(id = 1, surahId = surahId, ayahNumber = ayahNumber, surahName = surahName)
            repository.saveLastReadingState(state)
            _lastReadingState.value = state
        }
    }

    fun triggerGPSManual() {
        lastGpsOffMsgTime = 0
        lastGpsSuccessMsgTime = 0
        requestGPSLocation()
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
                ?: com.example.data.PrayerAlertSetting(prayerName, isEnabled)
            repository.saveAlertSetting(setting.copy(isEnabled = isEnabled))
            try { com.example.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
        }
    }

    fun selectMethod(method: PrayerCalculator.CalculationMethod) {
        _calculationMethod.value = method
        recalculatePrayerTimes()
    }

    private fun recalculatePrayerTimes() {
        val calendar = Calendar.getInstance()
        val tz = TimeZone.getDefault()
        val offsetHours = tz.getOffset(calendar.timeInMillis) / 3600000.0

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
        try { com.example.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setShowNawafil(value: Boolean) {
        _showNawafil.value = value
        prefs.edit().putBoolean("show_nawafil", value).apply()
        recalculatePrayerTimes()
    }

    fun setAsrMethod(value: String) {
        _asrMethod.value = value
        prefs.edit().putString("asr_method", value).apply()
        recalculatePrayerTimes()
    }

    fun setIshaMethod(value: String) {
        _ishaMethod.value = value
        prefs.edit().putString("isha_method", value).apply()
        recalculatePrayerTimes()
    }

    fun setAdjFajr(value: Int) {
        adjFajr.value = value
        prefs.edit().putInt("adj_fajr", value).apply()
        recalculatePrayerTimes()
    }

    fun setAdjSunrise(value: Int) {
        adjSunrise.value = value
        prefs.edit().putInt("adj_sunrise", value).apply()
        recalculatePrayerTimes()
    }

    fun setAdjDhuhr(value: Int) {
        adjDhuhr.value = value
        prefs.edit().putInt("adj_dhuhr", value).apply()
        recalculatePrayerTimes()
    }

    fun setAdjAsr(value: Int) {
        adjAsr.value = value
        prefs.edit().putInt("adj_asr", value).apply()
        recalculatePrayerTimes()
    }

    fun setAdjMaghrib(value: Int) {
        adjMaghrib.value = value
        prefs.edit().putInt("adj_maghrib", value).apply()
        recalculatePrayerTimes()
    }

    fun setAdjIsha(value: Int) {
        adjIsha.value = value
        prefs.edit().putInt("adj_isha", value).apply()
        recalculatePrayerTimes()
    }

    fun resetToDefaults() {
        setAppTheme("system")
        setUseDynamicColor(true)
        setAmoledDark(true)
        setArabicFontSize(24f)
        setEnglishFontSize(16f)
        setLocationMethod("manual")
        updateLocation(21.4225, 39.8262, "Mecca, KSA", false)
        setAsrMethod("standard")
        setIshaMethod("standard")
        setShowNawafil(false)
        setEnableAdhanSound(false)
        setAdhanSoundType("short")
        setAdjFajr(0)
        setAdjSunrise(0)
        setAdjDhuhr(0)
        setAdjAsr(0)
        setAdjMaghrib(0)
        setAdjIsha(0)
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
            // Long vibration on target hit
            _tasbihCount.value = 0 // Auto reset on target hit
            triggerVibration(250)
        } else {
            // Soft ripple vibration
            triggerVibration(30)
        }

        // Persist
        viewModelScope.launch {
            repository.saveTasbihState(TasbihState(_selectedDhikr.value.first, _tasbihCount.value, target))
        }
    }

    fun resetTasbih() {
        _tasbihCount.value = 0
        triggerVibration(80)
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
    fun updateDuaSearch(query: String) {
        _duaSearchQuery.value = query
    }

    fun selectDuaCategory(category: String) {
        _selectedDuaCategory.value = category
    }

    // QIBLA LOCK FEEDBACK
    private fun checkQiblaAlignment(compassAzimuth: Float) {
        val target = _qiblaBearing.value.toFloat()
        val diff = abs(compassAzimuth - target)
        // Normalize range
        val normDiff = if (diff > 180f) 360f - diff else diff

        if (normDiff < 4.0) { // Aligned within 4 degrees
            val now = System.currentTimeMillis()
            if (now - lastAlignmentPulseTime > 1500) { // Once every 1.5 seconds maximum
                lastAlignmentPulseTime = now
                triggerVibration(70)
            }
        }
    }

    private fun triggerVibration(duration: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // COUNTDOWN & ACTIVE PRAYER TICK
    private fun startCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                calculateNextPrayer()
                delay(1000)
            }
        }
    }

    private fun calculateNextPrayer() {
        val now = Calendar.getInstance()
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

        var diff = if (isNextDay) {
            (24 * 3600 - currentTimeInSec) + nextSec
        } else {
            nextSec - currentTimeInSec
        }
        if (diff < 0) diff = 0

        val h = diff / 3600
        val m = (diff % 3600) / 60
        val s = diff % 60

        _nextPrayerCountdown.value = String.format("%02d:%02d:%02d", h, m, s)
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stop()
        countdownJob?.cancel()
    }
}
