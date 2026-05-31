package dev.barakah.app.ui.screens

import androidx.activity.compose.BackHandler
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.barakah.app.data.Dua
import dev.barakah.app.ui.BarakahViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SupplicationsScreen(
    viewModel: BarakahViewModel,
    modifier: Modifier = Modifier
) {
    val searchPattern by viewModel.duaSearchQuery.collectAsState()
    val activeCategory by viewModel.selectedDuaCategory.collectAsState()
    val filteredDuas by viewModel.filteredDuas.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isAr = appLanguage == "ar"

    val context = LocalContext.current
    val vibrator = remember {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    val categoryList = remember(isAr) {
        val list = mutableListOf<String>()
        list.add(if (isAr) "الأذكار المحفوظة" else "Bookmarks")
        val currentList = if (isAr) dev.barakah.app.data.DuaData.duasAr else dev.barakah.app.data.DuaData.duasEn
        list.addAll(currentList.map { if (isAr) it.categoryAr else it.categoryEn }.distinct())
        list
    }

    val duaBookmarks by viewModel.duaBookmarks.collectAsState()
    val favoriteDuaIds = remember(duaBookmarks) { duaBookmarks.map { it.duaId }.toSet() }
    val duaTapCounts = remember { mutableStateMapOf<String, Int>() }

    val menuScrollState = rememberLazyListState()

    BackHandler(enabled = activeCategory != "Menu" || searchPattern.isNotEmpty()) {
        if (searchPattern.isNotEmpty()) {
            viewModel.updateDuaSearch("")
        } else {
            viewModel.selectDuaCategory("Menu")
        }
    }

    fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(45)
            }
        } catch (_: Exception) {}
    }

    fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
        val clean = category.lowercase()
        return if (clean.contains("bookmark") || clean.contains("favorite") || clean.contains("المفضلة") || clean.contains("مرجعية")) {
            Icons.Default.Bookmark
        } else {
            Icons.Default.MenuBook
        }
    }

    @Composable
    fun getCategoryColor(category: String): Color {
        return MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp, 
                    start = 20.dp, 
                    end = 20.dp, 
                    bottom = 20.dp
                )
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isAr) "حصن المسلم" else "Hisnul Muslim",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    
                    if (activeCategory != "Menu" && searchPattern.isEmpty()) {
                        IconButton(
                            onClick = { viewModel.selectDuaCategory("Menu") },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to categories",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                
                Text(
                    text = if (isAr) "الأدعية والأذكار اليومية الصحيحة" else "Authentic daily supplications",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchPattern,
                    onValueChange = { viewModel.updateDuaSearch(it) },
                    placeholder = { Text(if (isAr) "ابحث في عنوان الذكر أو النص..." else "Search title, keywords...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchPattern.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateDuaSearch("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dua_search_bar"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedContent(
            targetState = (activeCategory == "Menu" && searchPattern.isEmpty()),
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            modifier = Modifier.weight(1f),
            label = "DuaListTransition"
        ) { isMenu ->
            if (isMenu) {
                LazyColumn(
                    state = menuScrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = if (isAr) "حدد تصنيفاً لعرض الأذكار:" else "Select a category:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    items(categoryList) { category ->
                        val color = getCategoryColor(category)
                        val icon = getCategoryIcon(category)
                        
                        Card(
                            onClick = { viewModel.selectDuaCategory(category) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = color.copy(alpha = 0.15f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start
                                )
                                
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    
                    item {
                        Card(
                            onClick = { viewModel.selectDuaCategory("All") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isAr) "عرض جميع الأذكار" else "Show All Supplications",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                if (filteredDuas.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isAr) "لا توجد أذكار مطابقة" else "No Supplications found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredDuas, key = { it.id }) { dua ->
                            val isFavorite = favoriteDuaIds.contains(dua.id)
                            val currentCount = duaTapCounts[dua.id] ?: 0
                            val isDone = currentCount >= dua.targetCount
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dua_card_${dua.id}"),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, 
                                    if (isDone) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    IconButton(
                                        onClick = { viewModel.toggleDuaBookmark(dua.id) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .testTag("fav_${dua.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                            contentDescription = "Bookmark",
                                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                                        Text(
                                            text = if (isAr) dua.titleAr else dua.titleEn,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth(0.85f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        SuggestionChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    text = if (isAr) dua.categoryAr else dua.categoryEn,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                            text = dua.arabic,
                                            style = androidx.compose.ui.text.TextStyle(
                                                fontSize = 22.sp,
                                                fontFamily = FontFamily.Serif,
                                                fontWeight = FontWeight.Bold,
                                                textDirection = TextDirection.Rtl,
                                                lineHeight = 36.sp,
                                                textAlign = TextAlign.Right
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        if (!isAr) {
                                            Text(
                                                text = dua.transliteration,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                    color = MaterialTheme.colorScheme.secondary
                                                ),
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                            )
                                            Text(
                                                text = dua.translation,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                            )
                                        }

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )

                                        Text(
                                            text = if (isAr) "الأثر والفضل النبوي:" else "Benefit / Virtue:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isAr) dua.virtueAr else dua.virtueEn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isDone) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = if (isAr) "التكرار المطلوب" else "Target Count",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                                Text(
                                                    text = "${dua.targetCount} ${if (isAr) "مرات" else "times"}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                if (currentCount > 0) {
                                                    IconButton(
                                                        onClick = {
                                                            duaTapCounts[dua.id] = 0
                                                            triggerVibration()
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Refresh,
                                                            contentDescription = "Reset Count",
                                                            tint = MaterialTheme.colorScheme.outline,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    onClick = {
                                                        val next = if (currentCount < dua.targetCount) currentCount + 1 else 0
                                                        duaTapCounts[dua.id] = next
                                                        triggerVibration()
                                                    },
                                                    shape = CircleShape,
                                                    color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = if (isDone) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(54.dp).testTag("dua_counter_tap_${dua.id}")
                                                ) {
                                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                        if (isDone) {
                                                            Icon(imageVector = Icons.Default.Done, contentDescription = "Completed", modifier = Modifier.size(24.dp))
                                                        } else {
                                                            Text(
                                                                text = java.lang.String.format(java.util.Locale.US, "%d/%d", currentCount, dua.targetCount),
                                                                style = MaterialTheme.typography.bodySmall.copy(
                                                                    fontFamily = FontFamily.Monospace,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    fontSize = 11.sp
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
