package dev.barakah.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.barakah.app.data.QuranData
import dev.barakah.app.R
import dev.barakah.app.util.PrayerCalculator
import dev.barakah.app.data.Surah
import dev.barakah.app.ui.BarakahViewModel
import dev.barakah.app.util.localize
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun QuranScreen(
    viewModel: BarakahViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val bookmarks by viewModel.bookmarks.collectAsState()
    
    // Filters: 0 -> All Surahs, 1 -> Bookmarks Only
    var activeFilterTab by remember { mutableStateOf(0) }
    
    // In Detail Reading view target
    var selectedSurahForReading by remember { mutableStateOf<Surah?>(null) }
    
    // Dialog for individual verse Tafseer
    var showVerseTafseerDialog by remember { mutableStateOf<Pair<dev.barakah.app.data.Verse, String>?>(null) }
    
    BackHandler(enabled = selectedSurahForReading != null) {
        selectedSurahForReading = null
    }
    
    // Resume state
    val lastReadingState by viewModel.lastReadingState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Fonts collected directly from centralized viewModel
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()
    val englishFontSize by viewModel.englishFontSize.collectAsState()
    val quranFontFamily = FontFamily.Default
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isAr = appLanguage == "ar"
    val useWesternNumbersInArabic by viewModel.useWesternNumbersInArabic.collectAsState()

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    val allSurahs by QuranData.surahsFlow.collectAsState()

    // Filter surahs list
    val filteredSurahs = remember(searchQuery, activeFilterTab, bookmarks, allSurahs) {
        allSurahs.filter { surah ->
            val matchesQuery = surah.name.contains(searchQuery, ignoreCase = true) ||
                    surah.translation.contains(searchQuery, ignoreCase = true) ||
                    surah.arabic.contains(searchQuery) ||
                    surah.id.toString() == searchQuery
            val matchesFilter = if (activeFilterTab == 1) {
                bookmarks.any { b -> b.surahId == surah.id }
            } else {
                true
            }
            matchesQuery && matchesFilter
        }
    }

    AnimatedContent(
        targetState = selectedSurahForReading,
        transitionSpec = {
            if (targetState != null) {
                // Opening reading view
                (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(400))).togetherWith(
                    slideOutHorizontally { width -> -width / 4 } + fadeOut(animationSpec = tween(400))
                )
            } else {
                // Closing reading view
                (slideInHorizontally { width -> -width / 4 } + fadeIn(animationSpec = tween(400))).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(400))
                )
            }.using(SizeTransform(clip = false))
        },
        label = "QuranReadingTransition"
    ) { currentSurah ->
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        val lastReadBanner: @Composable () -> Unit = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        lastReadingState?.let {
                            selectedSurahForReading = QuranData.getSurahById(it.surahId)
                        } ?: run {
                            selectedSurahForReading = QuranData.getSurahById(1)
                        }
                    }
                    .testTag("last_read_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Outlined.Book,
                            contentDescription = if (isAr) "أيقونة آخر قراءة" else "Last read icon",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isAr) "آخر ما قرأت / متابعة" else "LAST READ / RESUME",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = lastReadingState?.let { 
                                    val s = QuranData.getSurahById(it.surahId)
                                    val surahName = if (isAr) s?.arabic else s?.name
                                    val ayahLabel = if (isAr) "آية" else "Ayah"
                                    val ayahNum = it.ayahNumber.toString()
                                    "$surahName - $ayahLabel $ayahNum".localize(isAr, useWesternNumbersInArabic)
                                } ?: (if (isAr) "سورة الفاتحة" else "Surah Al-Fatihah"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = if (isAr) "متابعة القراءة" else "Resume reading",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        if (currentSurah == null) {
            // MAIN QURAN CATALOG LIST
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // 1. TOP HEADER BANNER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .padding(
                            top = if (isLandscape) {
                                WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 8.dp
                            } else {
                                WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 24.dp
                            }, 
                            start = if (isLandscape) 16.dp else 24.dp, 
                            end = if (isLandscape) 16.dp else 24.dp, 
                            bottom = if (isLandscape) 8.dp else 24.dp
                        )
                ) {
                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isAr) "القرآن الكريم" else "The Holy Quran",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = if (isAr) "القرآن الكريم" else "The Holy Quran",
                                style = MaterialTheme.typography.headlineLarge,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = if (isAr) "اقرأ وتدبر واجمع عظيم الأجور والبركات" else "Read, reflect, and gather countless blessings",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            lastReadBanner()
                        }
                    }
                }

                // 2. SEARCH BAR & TAB TOGGLE FLAGS
                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { 
                                Text(
                                    text = if (isAr) "ابحث عن السور..." else "Search Surah...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                ) 
                            },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = if (isAr) "بحث" else "Search icon") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = if (isAr) "مسح البحث" else "Clear search")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(0.5f)
                                .heightIn(min = 52.dp)
                                .testTag("quran_search_bar"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )

                        // Custom Tab Toggle
                        Row(
                            modifier = Modifier
                                .weight(0.5f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(2.dp)
                        ) {
                            val items = listOf(
                                if (isAr) "كل السور" else "All Surahs",
                                if (isAr) "المحفوظات (${bookmarks.size})" else "Bookmarks (${bookmarks.size})"
                            )
                            items.forEachIndexed { index, title ->
                                val selected = activeFilterTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { activeFilterTab = index }
                                        .padding(vertical = 4.dp)
                                        .testTag("quran_filter_tab_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { 
                                Text(
                                    text = if (isAr) "ابحث عن اسم السورة..." else "Search Surah name, number, meaning...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                ) 
                            },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = if (isAr) "بحث" else "Search icon") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = if (isAr) "مسح البحث" else "Clear search")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .testTag("quran_search_bar"),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Tab Toggle (All Surahs vs Bookmarked Surahs)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp)
                        ) {
                            val items = listOf(
                                if (isAr) "كل السور" else "All Surahs",
                                if (isAr) "المحفوظات (${bookmarks.size})" else "Bookmarks (${bookmarks.size})"
                            )
                            items.forEachIndexed { index, title ->
                                val selected = activeFilterTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { activeFilterTab = index }
                                        .padding(vertical = 10.dp)
                                        .testTag("quran_filter_tab_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    state = listState
                ) {
                    if (isLandscape) {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                lastReadBanner()
                            }
                        }
                    }
                    items(filteredSurahs) { surah ->
                        val isBookmarked = bookmarks.any { b -> b.surahId == surah.id }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSurahForReading = QuranData.getSurahById(surah.id)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("surah_item_${surah.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                             ) {
                                // Surah Number index
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .drawBehindSquareOrnament(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = surah.id.toString().localize(isAr, useWesternNumbersInArabic),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = if (isAr) surah.arabic else surah.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val typeTranslated = if (surah.type == "Meccan") {
                                            if (isAr) "مكية" else "Meccan"
                                        } else {
                                            if (isAr) "مدنية" else "Medinan"
                                        }
                                        val versesCountStr = surah.versesCount.toString().localize(isAr, useWesternNumbersInArabic)
                                        Text(
                                            text = "$typeTranslated • $versesCountStr ${if (isAr) "آية" else "Verses"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isAr) {
                                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 12.dp)) {
                                        Text(
                                            text = surah.arabic,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontFamily = quranFontFamily
                                        )
                                        Text(
                                            text = surah.translation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.toggleBookmark(surah) },
                                    modifier = Modifier.testTag("bookmark_toggle_${surah.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = if (isAr) {
                                            if (isBookmarked) "إزالة الإشارة المرجعية" else "إضافة إشارة مرجعية"
                                        } else {
                                            if (isBookmarked) "Remove bookmark" else "Add bookmark"
                                        },
                                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        } else {
            // DETAIL ACTIVE READING VIEW
            val context = androidx.compose.ui.platform.LocalContext.current
            var surahWithVerses by remember(currentSurah.id) { mutableStateOf(currentSurah) }
            
            LaunchedEffect(currentSurah.id) {
                val verses = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    QuranData.loadVersesForSurah(context, currentSurah.id)
                }
                surahWithVerses = currentSurah.copy(versesList = verses)
            }
            
            val surah = surahWithVerses
            val isBookmarked = bookmarks.any { b -> b.surahId == surah.id }
            val readerListState = androidx.compose.runtime.key(surah.id) { androidx.compose.foundation.lazy.rememberLazyListState() }

            LaunchedEffect(surah.id, surah.versesList.isNotEmpty()) {
                if (surah.versesList.isNotEmpty()) {
                    val lastRead = lastReadingState
                    if (lastRead != null && lastRead.surahId == surah.id) {
                        val targetIndex = if (surah.id != 9) {
                            lastRead.ayahNumber
                        } else {
                            lastRead.ayahNumber - 1
                        }
                        if (targetIndex >= 0 && targetIndex < surah.versesList.size + (if (surah.id != 9) 1 else 0)) {
                            readerListState.scrollToItem(targetIndex)
                        }
                    }
                }
            }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // 1. TOP HEADER Reading Actions
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 8.dp,
                                bottom = 4.dp,
                                start = 8.dp,
                                end = 8.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { selectedSurahForReading = null },
                            modifier = Modifier.testTag("back_to_catalog")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (isAr) "رجوع" else "Back")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isAr) surah.arabic else surah.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { viewModel.toggleBookmark(surah) }) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isAr) {
                                    if (isBookmarked) "إزالة الإشارة المرجعية للسورة" else "حفظ إشارة مرجعية للسورة"
                                } else {
                                    if (isBookmarked) "Remove surah bookmark" else "Bookmark surah"
                                },
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. VERSES LIST READER
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    state = readerListState
                ) {
                    if (surah.id != 9) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontSize = (arabicFontSize + 2).sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        fontFamily = quranFontFamily,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    items(surah.versesList) { verse ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .combinedClickable(
                                    onClick = {
                                        viewModel.saveLastReading(surah.id, verse.index, surah.arabic)
                                    },
                                    onLongClick = {
                                        viewModel.triggerVibration(40)
                                        val taf = if (isAr) {
                                            QuranData.loadTafseer(context, surah.id, verse.index)
                                        } else {
                                            QuranData.loadEnglishTafseer(context, surah.id, verse.index)
                                        }
                                        showVerseTafseerDialog = Pair(verse, taf)
                                    }
                                )
                                .testTag("verse_block_${verse.index}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = verse.index.toString().localize(isAr, useWesternNumbersInArabic),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = verse.arabic,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = arabicFontSize.sp,
                                    fontFamily = quranFontFamily,
                                    textDirection = TextDirection.Rtl,
                                    lineHeight = (arabicFontSize * 1.5).sp,
                                    textAlign = TextAlign.Right
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )

                            if (!isAr) {
                                Text(
                                    text = verse.english,
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontSize = englishFontSize.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        lineHeight = (englishFontSize * 1.35).sp,
                                        textAlign = TextAlign.Left
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVerseTafseerDialog != null) {
        val (verse, tafseer) = showVerseTafseerDialog!!
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        AlertDialog(
            onDismissRequest = { showVerseTafseerDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = if (isAr) "معلومات التفسير" else "Tafseer information",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                val verseNum = verse.index.toString().localize(isAr, useWesternNumbersInArabic)
                Text(
                    text = if (isAr) "تفسير الآية $verseNum" else "Tafseer of Ayah ${verse.index}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                val dialogMaxHeight = if (isLandscape) 140.dp else 420.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = dialogMaxHeight)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = verse.arabic,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = (arabicFontSize - 2).sp,
                            fontFamily = quranFontFamily,
                            textDirection = TextDirection.Rtl,
                            textAlign = TextAlign.Right,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = tafseer,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = (englishFontSize + 1).sp,
                                    fontFamily = FontFamily.SansSerif,
                                    textDirection = if (isAr) TextDirection.Rtl else TextDirection.Ltr,
                                    textAlign = if (isAr) TextAlign.Right else TextAlign.Left,
                                    lineHeight = (englishFontSize + 7).sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isAr) "المصدر: التفسير الميسر" else "Source: Tafsir al-Jalalayn",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                textAlign = if (isAr) TextAlign.Right else TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(tafseer))
                        showVerseTafseerDialog = null
                    }
                ) {
                    Text(if (isAr) "نسخ التفسير" else "Copy Tafseer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVerseTafseerDialog = null }) {
                    Text(if (isAr) "إغلاق" else "Close")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }
}

fun Modifier.drawBehindSquareOrnament(color: Color) = this.drawBehind {
    val size = this.size
    val radius = size.minDimension / 2f
    val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
    val path = androidx.compose.ui.graphics.Path()
    for (step in 0..7) {
        val angleRad = Math.toRadians((step * 45).toDouble())
        val x = center.x + radius * Math.cos(angleRad).toFloat()
        val y = center.y + radius * Math.sin(angleRad).toFloat()
        if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
