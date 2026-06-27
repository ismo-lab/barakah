package dev.barakah.app.ui

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
import androidx.glance.appwidget.updateAll
import dev.barakah.app.data.*
import dev.barakah.app.util.PrayerCalculator
import dev.barakah.app.util.QiblaManager
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
    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator ?: (application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    } else {
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
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

    // Morning and Evening Adhkar Notifications state flow
    private val _notifyMorningAdhkar = MutableStateFlow(prefs.getBoolean("notify_morning_adhkar", true))
    val notifyMorningAdhkar: StateFlow<Boolean> = _notifyMorningAdhkar

    private val _notifyEveningAdhkar = MutableStateFlow(prefs.getBoolean("notify_evening_adhkar", true))
    val notifyEveningAdhkar: StateFlow<Boolean> = _notifyEveningAdhkar

    // Pre-Adhan notifications (15-min before), Occasions, and Fasting notifications state flows
    private val _notifyBeforeAdhan = MutableStateFlow(prefs.getBoolean("notify_before_adhan", true))
    val notifyBeforeAdhan: StateFlow<Boolean> = _notifyBeforeAdhan

    private val _notifyOccasions = MutableStateFlow(prefs.getBoolean("notify_occasions", true))
    val notifyOccasions: StateFlow<Boolean> = _notifyOccasions

    private val _notifyFasting = MutableStateFlow(prefs.getBoolean("notify_fasting", true))
    val notifyFasting: StateFlow<Boolean> = _notifyFasting

    private val _notifyJumuah = MutableStateFlow(prefs.getBoolean("notify_jumuah", true))
    val notifyJumuah: StateFlow<Boolean> = _notifyJumuah

    private val _notifySuhur = MutableStateFlow(prefs.getBoolean("notify_suhur", false))
    val notifySuhur: StateFlow<Boolean> = _notifySuhur

    private val _notifyIftar = MutableStateFlow(prefs.getBoolean("notify_iftar", false))
    val notifyIftar: StateFlow<Boolean> = _notifyIftar

    // Tasbih haptic feedback state: boolean (default true)
    private val _enableTasbihHaptics = MutableStateFlow(prefs.getBoolean("enable_tasbih_haptics", true))
    val enableTasbihHaptics: StateFlow<Boolean> = _enableTasbihHaptics

    // Digit style: Western vs Eastern Arabic numbers when in Arabic locale
    private val _useWesternNumbersInArabic = MutableStateFlow(prefs.getBoolean("use_western_numbers_in_arabic", false))
    val useWesternNumbersInArabic: StateFlow<Boolean> = _useWesternNumbersInArabic

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
            val offsetHours = PrayerCalculator.getEffectiveTimezoneOffset(lat, lng)
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
    private val _qiblaBearing = MutableStateFlow(0.0)
    val qiblaBearing: StateFlow<Double> = _qiblaBearing

    // Coroutines ticks
    private var countdownJob: Job? = null
    private var lastCheckedDayOfYear = -1
    private var lastCheckedTimeZoneId = ""

    init {
        // Guarantee app default language is arabic when first open
        if (!prefs.contains("app_lang")) {
            prefs.edit().putString("app_lang", "ar").apply()
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
    }

    private var lastGpsOffMsgTime = 0L
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
                val label = if (_appLanguage.value == "ar") "مكة المكرمة (تلقائي)" else "Mecca (GPS Fallback)"
                updateLocation(21.4225, 39.8262, label)
                viewModelScope.launch {
                    val msg = if (_appLanguage.value == "ar") "لم يتم منح صلاحية تحديد الموقع، تم استخدام مكة المكرمة كافتراضي" else "Location permission not granted, falling back to Mecca"
                    _eventFlow.emit(msg)
                }
                return
            }

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
                        try {
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
                            }.addOnFailureListener {
                                val label = if (_appLanguage.value == "ar") "مكة المكرمة (تلقائي)" else "Mecca (GPS Fallback)"
                                updateLocation(21.4225, 39.8262, label)
                                viewModelScope.launch {
                                    val msg = if (_appLanguage.value == "ar") "تعذر تحديد الموقع الجغرافي، تم استخدام مكة المكرمة كافتراضي" else "Could not determine GPS location, falling back to Mecca"
                                    _eventFlow.emit(msg)
                                }
                            }
                        } catch (e: Exception) {
                            val label = if (_appLanguage.value == "ar") "مكة المكرمة (تلقائي)" else "Mecca (GPS Fallback)"
                            updateLocation(21.4225, 39.8262, label)
                            viewModelScope.launch {
                                val msg = if (_appLanguage.value == "ar") "تعذر تحديد الموقع الجغرافي، تم استخدام مكة المكرمة كافتراضي" else "Could not determine GPS location, falling back to Mecca"
                                _eventFlow.emit(msg)
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
            val locationManager = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                    locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
        } catch (e: Exception) {
            false
        }
    }

    fun updateLocation(lat: Double, lng: Double, label: String, isSuccessFeedback: Boolean = false) {
        _currentLocation.value = Pair(lat, lng)
        _locationLabel.value = label
        _qiblaBearing.value = QiblaManager.calculateQiblaBearing(lat, lng)
        prefs.edit().putFloat("loc_lat", lat.toFloat()).putFloat("loc_lng", lng.toFloat()).putString("loc_label", label).commit()
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
        prefs.edit().putString("app_lang", lang).commit()
        _filteredDuas.value = if (lang == "ar") DuaData.duasAr else DuaData.duasEn
        updateWidgetsGlobally()
    }

    private fun updateWidgetsGlobally() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                dev.barakah.app.widget.PrayerWidget().updateAll(context)
            } catch (e: Exception) {
                android.util.Log.e("BarakahViewModel", "Error updating PrayerWidget", e)
            }
            try {
                dev.barakah.app.widget.PrayerRemainingWidget().updateAll(context)
            } catch (e: Exception) {
                android.util.Log.e("BarakahViewModel", "Error updating PrayerRemainingWidget", e)
            }
            try {
                dev.barakah.app.widget.NawafilWidget().updateAll(context)
            } catch (e: Exception) {
                android.util.Log.e("BarakahViewModel", "Error updating NawafilWidget", e)
            }

            // Send system broadcasts to immediately force Android's widget framework to invalidate cache and refresh layout
            try {
                val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                
                val prayerIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, dev.barakah.app.widget.PrayerWidgetReceiver::class.java)
                )
                if (prayerIds != null && prayerIds.isNotEmpty()) {
                    val intent = android.content.Intent(context, dev.barakah.app.widget.PrayerWidgetReceiver::class.java).apply {
                        action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, prayerIds)
                    }
                    context.sendBroadcast(intent)
                }

                val remainingIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, dev.barakah.app.widget.PrayerRemainingWidgetReceiver::class.java)
                )
                if (remainingIds != null && remainingIds.isNotEmpty()) {
                    val intent = android.content.Intent(context, dev.barakah.app.widget.PrayerRemainingWidgetReceiver::class.java).apply {
                        action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, remainingIds)
                    }
                    context.sendBroadcast(intent)
                }

                val nawafilIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, dev.barakah.app.widget.NawafilWidgetReceiver::class.java)
                )
                if (nawafilIds != null && nawafilIds.isNotEmpty()) {
                    val intent = android.content.Intent(context, dev.barakah.app.widget.NawafilWidgetReceiver::class.java).apply {
                        action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, nawafilIds)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("BarakahViewModel", "Error sending widget update broadcasts", e)
            }
        }
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

    fun setEnableTasbihHaptics(enable: Boolean) {
        _enableTasbihHaptics.value = enable
        prefs.edit().putBoolean("enable_tasbih_haptics", enable).apply()
    }

    fun setUseWesternNumbersInArabic(enable: Boolean) {
        _useWesternNumbersInArabic.value = enable
        prefs.edit().putBoolean("use_western_numbers_in_arabic", enable).apply()
        updateWidgetsGlobally()
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
                ?: dev.barakah.app.data.PrayerAlertSetting(prayerName, isEnabled)
            repository.saveAlertSetting(setting.copy(isEnabled = isEnabled))
            try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
        }
    }

    fun selectMethod(method: PrayerCalculator.CalculationMethod) {
        _calculationMethod.value = method
        prefs.edit().putString("calc_method", method.name).commit()
        recalculatePrayerTimes()
    }

    private fun recalculatePrayerTimes() {
        val calendar = Calendar.getInstance()
        val offsetHours = PrayerCalculator.getEffectiveTimezoneOffset(_currentLocation.value.first, _currentLocation.value.second)

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

    fun setShowNawafil(value: Boolean) {
        _showNawafil.value = value
        prefs.edit().putBoolean("show_nawafil", value).apply()
        recalculatePrayerTimes()
    }

    fun setNotifyMorningAdhkar(value: Boolean) {
        _notifyMorningAdhkar.value = value
        prefs.edit().putBoolean("notify_morning_adhkar", value).apply()
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setNotifyEveningAdhkar(value: Boolean) {
        _notifyEveningAdhkar.value = value
        prefs.edit().putBoolean("notify_evening_adhkar", value).apply()
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setNotifyBeforeAdhan(value: Boolean) {
        _notifyBeforeAdhan.value = value
        prefs.edit().putBoolean("notify_before_adhan", value).apply()
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setNotifyOccasions(value: Boolean) {
        _notifyOccasions.value = value
        prefs.edit().putBoolean("notify_occasions", value).apply()
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setNotifyFasting(value: Boolean) {
        _notifyFasting.value = value
        prefs.edit().putBoolean("notify_fasting", value).apply()
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setNotifyJumuah(value: Boolean) {
        _notifyJumuah.value = value
        prefs.edit().putBoolean("notify_jumuah", value).apply()
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setNotifySuhur(value: Boolean) {
        _notifySuhur.value = value
        prefs.edit().putBoolean("notify_suhur", value).apply()
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
    }

    fun setNotifyIftar(value: Boolean) {
        _notifyIftar.value = value
        prefs.edit().putBoolean("notify_iftar", value).apply()
        try { dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(getApplication()) } catch(e: Exception) {}
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
        setEnableTasbihHaptics(true)
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

    fun startQiblaTracking() {
        isQiblaScreenActive = true
        sensorManager.start()
    }

    fun stopQiblaTracking() {
        isQiblaScreenActive = false
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
            e.printStackTrace()
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
        val now = Calendar.getInstance()
        val dayOfYear = now.get(Calendar.DAY_OF_YEAR)
        val tzId = TimeZone.getDefault().id
        
        // Detect day-change or timezone/DST transitions and trigger live recalculation
        if ((lastCheckedDayOfYear != -1 && lastCheckedDayOfYear != dayOfYear) ||
            (lastCheckedTimeZoneId.isNotEmpty() && lastCheckedTimeZoneId != tzId)) {
            recalculatePrayerTimes()
        }
        lastCheckedDayOfYear = dayOfYear
        lastCheckedTimeZoneId = tzId

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
        when {
            currentTimeInSec < fajrSec -> highlight = "Isha"
            currentTimeInSec < sunriseSec -> highlight = "Fajr"
            currentTimeInSec < dhuhrSec -> highlight = "Sunrise"
            currentTimeInSec < asrSec -> highlight = "Dhuhr"
            currentTimeInSec < maghribSec -> highlight = "Asr"
            currentTimeInSec < ishaSec -> highlight = "Maghrib"
            else -> highlight = "Isha"
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
        countdownJob?.cancel()
    }
}
