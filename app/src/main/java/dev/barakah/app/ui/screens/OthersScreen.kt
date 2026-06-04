package dev.barakah.app.ui.screens

import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.barakah.app.data.AllahName
import dev.barakah.app.data.AllahNamesData
import dev.barakah.app.ui.BarakahViewModel
import java.util.Calendar

data class IslamicOccasion(
    val id: Int,
    val nameEn: String,
    val nameAr: String,
    val hijriDateEn: String,
    val hijriDateAr: String,
    val descEn: String,
    val descAr: String,
    val hMonth: Int,
    val hDay: Int,
    val iconEn: String = "🌟"
)

fun getMiladiDateString(item: IslamicOccasion, isAr: Boolean): String {
    return if (item.hDay == -1) {
        dev.barakah.app.util.HijriCalendarHelper.getGregorianEquivalentRange(item.hMonth, 21, 30, isAr)
    } else {
        dev.barakah.app.util.HijriCalendarHelper.getGregorianEquivalent(item.hMonth, item.hDay, isAr)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OthersScreen(
    viewModel: BarakahViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isAr = appLanguage == "ar"
    
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator }
    val isHapticEnabled by viewModel.enableTasbihHaptics.collectAsState()

    fun triggerVibration() {
        if (!isHapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (_: Exception) {}
    }

    // Occasions list
    val occasions = remember {
        listOf(
            IslamicOccasion(
                id = 1,
                nameAr = "رأس السنة الهجرية",
                nameEn = "Hijri New Year",
                hijriDateAr = "١ محرم",
                hijriDateEn = "1 Muharram",
                descAr = "بداية العام الهجري الجديد وتوثيق للهجرة النبوية المباركة من مكة المكرمة إلى المدينة المنورة.",
                descEn = "The first of Muharram marks the beginning of the Islamic New Year, commemorating the Prophet's Hijra from Mecca to Medina.",
                hMonth = 1,
                hDay = 1
            ),
            IslamicOccasion(
                id = 2,
                nameAr = "يوم عاشوراء",
                nameEn = "Day of Ashura",
                hijriDateAr = "١٠ محرم",
                hijriDateEn = "10 Muharram",
                descAr = "يوم نجّى الله فيه موسى عليه السلام من فرعون وملئه، ويُستحب صيام هذا اليوم شكراً لله تعالى.",
                descEn = "The day Allah saved Prophet Musa (Moses) from Pharaoh. Fasting on this day is recommended as an act of gratitude.",
                hMonth = 1,
                hDay = 10
            ),
            IslamicOccasion(
                id = 3,
                nameAr = "المولد النبوي الشريف",
                nameEn = "Mawlid al-Nabi",
                hijriDateAr = "١٢ ربيع الأول",
                hijriDateEn = "12 Rabi' al-Awwal",
                descAr = "ذكرى مولد الحبيب المصطفى محمد صلى الله عليه وسلم، مبعث النور والهدى للبشرية جمعاء.",
                descEn = "Commemorating the birth of Prophet Muhammad (peace be upon him), sent as a mercy to all creation.",
                hMonth = 3,
                hDay = 12
            ),
            IslamicOccasion(
                id = 4,
                nameAr = "ليلة الإسراء والمعراج",
                nameEn = "Isra' and Mi'raj",
                hijriDateAr = "٢٧ رجب",
                hijriDateEn = "27 Rajab",
                descAr = "الرحلة الإعجازية للنبي صلى الله عليه وسلم من المسجد الحرام للمسجد الأقصى وعروجه للسماوات العلا.",
                descEn = "The miraculous night journey of Prophet Muhammad from Mecca to Jerusalem, and his ascension to the Heavens.",
                hMonth = 7,
                hDay = 27
            ),
            IslamicOccasion(
                id = 5,
                nameAr = "بداية شهر رمضان المبارك",
                nameEn = "Ramadan First Day",
                hijriDateAr = "١ رمضان",
                hijriDateEn = "1 Ramadan",
                descAr = "بداية شهر الصوم والقرآن والاعتكاف، أعظم شهور السنة وفيه ليلة القدر المباركة.",
                descEn = "The start of the holy month of fasting, spiritual reflection, and devotion, containing the Night of Power.",
                hMonth = 9,
                hDay = 1
            ),
            IslamicOccasion(
                id = 6,
                nameAr = "ليلة النصف من شعبان",
                nameEn = "Mid-Sha'ban Night",
                hijriDateAr = "١٥ شعبان",
                hijriDateEn = "15 Sha'ban",
                descAr = "ليلة مباركة يرجى فيها مغفرة الذنوب وبسط الرحمة وقبول الطاعات.",
                descEn = "A sacred night in the month of Sha'ban on which forgiveness and divine blessings are widely bestowed.",
                hMonth = 8,
                hDay = 15
            ),
            IslamicOccasion(
                id = 7,
                nameAr = "ليلة القدر",
                nameEn = "Laylat al-Qadr",
                hijriDateAr = "العشر الأواخر من رمضان",
                hijriDateEn = "The last 10 days of Ramadan",
                descAr = "ليلة عظيمة هي خير من ألف شهر، نزل فيها القرآن الكريم وتُتحرى في العشر الأواخر من رمضان.",
                descEn = "The Night of Decree, better than a thousand months, during which the Holy Quran was revealed, sought in the last ten days.",
                hMonth = 9,
                hDay = -1
            ),
            IslamicOccasion(
                id = 8,
                nameAr = "عيد الفطر المبارك",
                nameEn = "Eid al-Fitr",
                hijriDateAr = "١ شوال",
                hijriDateEn = "1 Shawwal",
                descAr = "جائزة الصائمين وعيد بهيج يعقُب شهر رمضان تملأ فيه الفرحة البيوت والقلوب.",
                descEn = "Festival of breaking the fast, marking the end of Ramadan, filled with joy, gratitude, and charity.",
                hMonth = 10,
                hDay = 1
            ),
            IslamicOccasion(
                id = 9,
                nameAr = "بداية أشهر الحج المباركة",
                nameEn = "Start of Hajj Season",
                hijriDateAr = "١ ذو القعدة",
                hijriDateEn = "1 Dhul-Qi'dah",
                descAr = "بداية الأشهر الحرم التي يُتأهب فيها لزيارة بيت الله العتيق وأداء فريضة الحج المعظمة.",
                descEn = "The beginning of the sacred Hajj months, preparing the pilgrims for the journey of a lifetime to Mecca.",
                hMonth = 11,
                hDay = 1
            ),
            IslamicOccasion(
                id = 10,
                nameAr = "يوم عرفة",
                nameEn = "Day of Arafah",
                hijriDateAr = "٩ ذو الحجة",
                hijriDateEn = "9 Dhul-Hijjah",
                descAr = "أعظم أيام الدهر وركن الحج الأعظم، ويُستحب صيام لغير الحاج تكفيراً لذنوب سنتين.",
                descEn = "The most blessed day of the year, the pinnacle of the Hajj pilgrimage. Fasting on this day expiates sins for two years.",
                hMonth = 12,
                hDay = 9
            ),
            IslamicOccasion(
                id = 11,
                nameAr = "عيد الأضحى المبارك",
                nameEn = "Eid al-Adha",
                hijriDateAr = "١٠ ذو الحجة",
                hijriDateEn = "10 Dhul-Hijjah",
                descAr = "عيد النحر الكافي والذكرى المباركة لفداء إسماعيل عليه السلام، وتوزيع الأضاحي وصلة الأرحام.",
                descEn = "Festival of Sacrifice, commemorating Prophet Ibrahim's obedience, characterized by charity and community sharing.",
                hMonth = 12,
                hDay = 10
            )
        )
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf(
        if (isAr) "أسماء الله الحسنى" else "Names of Allah",
        if (isAr) "المناسبات والأعياد" else "Occasions & Eids"
    )

    var selectedNameForDetail by remember { mutableStateOf<AllahName?>(null) }
    var selectedOccasionForDetail by remember { mutableStateOf<IslamicOccasion?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isAr) "أخرى" else "Others",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { 
                            triggerVibration()
                            navController.popBackStack() 
                        },
                        modifier = Modifier.testTag("others_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isAr) "رجوع" else "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Elegant M3 Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().testTag("others_tab_row")
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            triggerVibration()
                            selectedTabIndex = index
                        },
                        text = { 
                            Text(
                                text = title, 
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        modifier = Modifier.testTag("others_tab_$index")
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "others_tab_animation",
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> NamesOfAllahPane(
                        isAr = isAr,
                        names = AllahNamesData.names,
                        onNameClick = { name ->
                            triggerVibration()
                            selectedNameForDetail = name
                        }
                    )
                    1 -> CalendarAndOccasionsPane(
                        isAr = isAr,
                        occasions = occasions,
                        onOccasionClick = { occasion ->
                            triggerVibration()
                            selectedOccasionForDetail = occasion
                        }
                    )
                }
            }
        }
    }

    // Detail Dialogs for Allah's Name
    if (selectedNameForDetail != null) {
        val name = selectedNameForDetail!!
        AlertDialog(
            onDismissRequest = { selectedNameForDetail = null },
            confirmButton = {
                TextButton(onClick = { 
                    triggerVibration()
                    selectedNameForDetail = null 
                }) {
                    Text(if (isAr) "إغلاق" else "Close")
                }
            },
            title = {
                Text(
                    text = name.arabic,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = if (isLandscape) 140.dp else 420.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = name.transliteration,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = name.englishMeaning,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isAr) "الشرح والدلالة" else "Meaning & Significance",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isAr) name.arabicMeaning else name.englishMeaningDetail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        )
    }

    // Detail Dialogs for occasion
    if (selectedOccasionForDetail != null) {
        val holiday = selectedOccasionForDetail!!
        AlertDialog(
            onDismissRequest = { selectedOccasionForDetail = null },
            confirmButton = {
                TextButton(onClick = { 
                    triggerVibration()
                    selectedOccasionForDetail = null 
                }) {
                    Text(if (isAr) "تم" else "Dismiss")
                }
            },
            title = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isAr) holiday.nameAr else holiday.nameEn,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val miladi = getMiladiDateString(holiday, isAr)
                    val formattedDate = if (miladi.isNotEmpty()) {
                        if (isAr) {
                            "التاريخ الهجري: ${holiday.hijriDateAr} • الموافق $miladi م"
                        } else {
                            "Hijri: ${holiday.hijriDateEn} • Equal: $miladi AD"
                        }
                    } else {
                        if (isAr) "التاريخ الهجري: ${holiday.hijriDateAr}" else "Date: ${holiday.hijriDateEn}"
                    }
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = if (isLandscape) 140.dp else 420.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    ) {
                        Text(
                            text = if (isAr) holiday.descAr else holiday.descEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(16.dp),
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun NamesOfAllahPane(
    isAr: Boolean,
    names: List<AllahName>,
    onNameClick: (AllahName) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        
        // Introductory banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAr) "إن لله تسعة وتسعين اسماً" else "99 Names of Allah",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isAr) "من أحصاها دخل الجنة. اضغط على أي اسم لقراءة شرحه ودلالاته." 
                               else "Whosoever memorizes and acts upon them will enter Paradise. Tap any name to read its meaning.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grid for responsive layouts
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .testTag("allah_names_grid")
        ) {
            items(names, key = { it.id }) { name ->
                AllahNameCard(
                    name = name,
                    isAr = isAr,
                    onCardClick = { onNameClick(name) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AllahNameCard(
    name: AllahName,
    isAr: Boolean,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onCardClick() }
            .testTag("allah_name_card_${name.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Index tag
            Text(
                text = "#${name.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Main Arabic text
            Text(
                text = name.arabic,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // English transliteration
            Text(
                text = name.transliteration,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Primary english meaning
            Text(
                text = name.englishMeaning.split("/")[0].trim(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CalendarAndOccasionsPane(
    isAr: Boolean,
    occasions: List<IslamicOccasion>,
    onOccasionClick: (IslamicOccasion) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                text = if (isAr) "أبرز المناسبات والأعياد" else "Major Occasions & Eids",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        // List occasions
        items(occasions, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOccasionClick(item) }
                    .testTag("occasion_card_${item.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.iconEn,
                            fontSize = 20.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAr) item.nameAr else item.nameEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val miladi = getMiladiDateString(item, isAr)
                        val displayDate = if (miladi.isNotEmpty()) {
                            if (isAr) "${item.hijriDateAr} • الموافق $miladi م" else "${item.hijriDateEn} • Eq: $miladi"
                        } else {
                            if (isAr) item.hijriDateAr else item.hijriDateEn
                        }
                        Text(
                            text = displayDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
