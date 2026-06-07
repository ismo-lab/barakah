package dev.barakah.app.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import dev.barakah.app.ui.BarakahViewModel
import dev.barakah.app.util.localize
import kotlinx.coroutines.launch

@Composable
fun TasbihScreen(
    viewModel: BarakahViewModel,
    modifier: Modifier = Modifier
) {
    val count by viewModel.tasbihCount.collectAsState()
    val target by viewModel.tasbihTarget.collectAsState()
    val activeDhikr by viewModel.selectedDhikr.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isAr = appLanguage == "ar"
    
    val haptic = LocalView.current
    val view = LocalView.current
    val enableTasbihHaptics by viewModel.enableTasbihHaptics.collectAsState()
    val useWesternNumbersInArabic by viewModel.useWesternNumbersInArabic.collectAsState()

    var showDhikrDropdown by remember { mutableStateOf(false) }

    // Pulsate click scale feedback
    val scope = rememberCoroutineScope()
    var isTapped by remember { mutableStateOf(false) }
    val blobScale by animateFloatAsState(
        targetValue = if (isTapped) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "blobClickScale",
        finishedListener = { isTapped = false }
    )

    fun getTranslatedDhikrTitle(englishTitle: String): String {
        return if (isAr) {
            when (englishTitle) {
                "Subhanallah (Glory be to Allah)" -> "سبحان الله (تسبيح)"
                "Alhamdulillah (Praise be to Allah)" -> "الحمد لله (تحميد)"
                "Allahu Akbar (Allah is the Greatest)" -> "الله أكبر (تكبير)"
                "La ilaha illallah (There is no god but Allah)" -> "لا إله إلا الله (توحيد)"
                "Astaghfirullah (I seek forgiveness from Allah)" -> "أستغفر الله (استغفار)"
                "La hawla wa la quwwata illa billah" -> "لا حول ولا قوة إلا بالله"
                else -> englishTitle
            }
        } else {
            englishTitle
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left column: Selectors and Controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header & Dhikr selector
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isAr) "مسبحة الذكر" else "Tasbih Counter",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Dhikr dropdown trigger
                        Card(
                            onClick = { showDhikrDropdown = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dhikr_dropdown_trigger"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (isAr) {
                                        Text(
                                            text = activeDhikr.second,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = FontFamily.Serif,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Start
                                        )
                                    } else {
                                        Text(
                                            text = getTranslatedDhikrTitle(activeDhikr.first),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Start
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = activeDhikr.second,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontFamily = FontFamily.Serif,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isAr) "اختر الذكر" else "Select Dhikr",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Target Select & Reset Controls
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Options Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val targets = listOf(33, 99, 100, 0)
                            targets.forEach { t ->
                                val isSelected = target == t
                                val label = if (t == 0) (if (isAr) "مفتوح" else "∞ Loop") else t.toString().localize(isAr, useWesternNumbersInArabic)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.setTasbihTarget(t) }
                                        .padding(vertical = 6.dp)
                                        .testTag("target_option_$t"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // RESET BUTTON
                        Button(
                            onClick = { viewModel.resetTasbih() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("reset_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = if (isAr) "إعادة ضبط العداد" else "Reset Counter",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAr) "إعادة ضبط العداد" else "Reset Progress",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Right column: Large clicking blob
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        onClick = {
                            isTapped = true
                            viewModel.incrementTasbih()
                        },
                        modifier = Modifier
                            .fillMaxHeight(0.9f)
                            .aspectRatio(1f)
                            .scale(blobScale)
                            .testTag("tap_zone_blob"),
                        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 80.dp, bottomEnd = 80.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val counterText = if (target > 0) "${count}/${target}" else "$count"
                                Text(
                                    text = counterText.localize(isAr, useWesternNumbersInArabic),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.sp,
                                        lineHeight = 36.sp,
                                        textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isAr) "اضغط للتسبيح" else "TAP TO COUNT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxHeight()
                    .widthIn(max = 680.dp)
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 24.dp,
                        bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() + 24.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
        // 1. TOP HEADER & DHIKR SELECTOR
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isAr) "مسبحة الذكر" else "Tasbih Counter",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (isAr) "حافظ على أورادك اليومية وأذكارك بسهولة" else "Keep track of your acts of remembrance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dhikr selector triggers dropdown sheet (Fully rounded shape)
            Card(
                onClick = { showDhikrDropdown = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dhikr_dropdown_trigger"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isAr) {
                            Text(
                                text = activeDhikr.second,
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = getTranslatedDhikrTitle(activeDhikr.first),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeDhikr.second,
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isAr) "اختر الذكر" else "Select Dhikr",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 2. LARGE TACTILE TAP ZONE (Pulsating Capsule Shape)
        Card(
            onClick = {
                isTapped = true
                viewModel.incrementTasbih()
            },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .scale(blobScale)
                .testTag("tap_zone_blob"),
            shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 80.dp, bottomEnd = 80.dp), // Fluid asymmetrical capsule
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val counterText = if (target > 0) "${count}/${target}" else "$count"
                    Text(
                        text = counterText.localize(isAr, useWesternNumbersInArabic),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp,
                            lineHeight = 52.sp,
                            textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) "اضغط في أي مكان للتسبيح" else "TAP ANYWHERE TO COUNT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // 3. TARGET OPTIONS BAR & RESET CONTROLS
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Target Selection Flags
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val targets = listOf(33, 99, 100, 0) // 0 represents "Infinite"
                targets.forEach { t ->
                    val isSelected = target == t
                    val label = if (t == 0) (if (isAr) "مفتوح" else "∞ Loop") else t.toString().localize(isAr, useWesternNumbersInArabic)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewModel.setTasbihTarget(t) }
                            .padding(vertical = 10.dp)
                            .testTag("target_option_$t"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // RESET BUTTON
            Button(
                onClick = { viewModel.resetTasbih() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("reset_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (isAr) "إعادة ضبط العداد" else "Reset Counter"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAr) "إعادة ضبط العداد" else "Reset Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    }

    // LIST DIALOG FOR DHIKR PHRASES
    if (showDhikrDropdown) {
        AlertDialog(
            onDismissRequest = { showDhikrDropdown = false },
            title = {
                Text(
                    text = if (isAr) "اختر الورد / الذكر" else "Choose Dhikr/Dua Phrase",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                val dialogMaxHeight = if (isLandscape) 140.dp else 350.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = dialogMaxHeight)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.commonDhikrList.size) { idx ->
                            val dhikr = viewModel.commonDhikrList[idx]
                            val isSelected = activeDhikr.first == dhikr.first
                            
                            Card(
                                onClick = {
                                    viewModel.selectDhikr(dhikr)
                                    showDhikrDropdown = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dhikr_select_${dhikr.first}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (isAr) {
                                            Text(
                                                text = dhikr.second,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = FontFamily.Serif,
                                                color = MaterialTheme.colorScheme.primary,
                                                textAlign = TextAlign.Start,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            Text(
                                                text = getTranslatedDhikrTitle(dhikr.first),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Start,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = dhikr.second,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = FontFamily.Serif,
                                                color = MaterialTheme.colorScheme.primary,
                                                textAlign = TextAlign.Start,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDhikrDropdown = false }) {
                    Text(if (isAr) "إغلاق" else "Close")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
    }
}
