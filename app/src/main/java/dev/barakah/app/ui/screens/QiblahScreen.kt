package dev.barakah.app.ui.screens

import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.abs
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.barakah.app.ui.BarakahViewModel
import dev.barakah.app.util.QiblaManager
import android.view.HapticFeedbackConstants
import kotlin.math.abs

@Composable
fun QiblahScreen(
    viewModel: BarakahViewModel,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val hapticEnabled by viewModel.enableTasbihHaptics.collectAsState()

    DisposableEffect(viewModel) {
        viewModel.startQiblaTracking()
        onDispose {
            viewModel.stopQiblaTracking()
        }
    }

    val compassAzimuth by viewModel.compassAzimuth.collectAsState()
    val qiblaBearing by viewModel.qiblaBearing.collectAsState()
    val locationLabel by viewModel.locationLabel.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isAr = appLanguage == "ar"

    // Smooth heading rotation interpolation
    val animatedHeading by animateFloatAsState(
        targetValue = compassAzimuth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "compassRotation"
    )

    // Compute relative pointer rotation (Mecca Bearing - Phone Heading)
    val relativeAngle = (qiblaBearing.toFloat() - animatedHeading + 360f) % 360f

    // Check if phone is perfectly pointed to Kaaba (with hysteresis loop to prevent jitter)
    val exactDiff = abs(compassAzimuth - qiblaBearing.toFloat())
    val shortestDiff = if (exactDiff > 180f) 360f - exactDiff else exactDiff

    var isAligned by remember { mutableStateOf(false) }

    LaunchedEffect(shortestDiff) {
        if (shortestDiff < 2.5f && !isAligned) {
            isAligned = true
        } else if (shortestDiff > 8.0f && isAligned) {
            isAligned = false
        }
    }

    LaunchedEffect(isAligned) {
        if (isAligned && hapticEnabled) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            } catch (e: Exception) {}
        }
    }

    val alignmentColor = if (isAligned) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    val glowScale by animateFloatAsState(
        targetValue = if (isAligned) 1.08f else 1f,
        animationSpec = tween(300),
        label = "alignmentGlow"
    )

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val compassSize = if (isLandscape) 210.dp else 280.dp
    val glowSize = if (isLandscape) 200.dp else 270.dp
    val dialRingSize = if (isLandscape) 180.dp else 250.dp
    val canvasSize = if (isLandscape) 170.dp else 240.dp
    val pointerSize = if (isLandscape) 140.dp else 210.dp
    val innerCoreSize = if (isLandscape) 56.dp else 76.dp
    val coreIconSize = if (isLandscape) 26.dp else 36.dp

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Panel: Rotate compass
            Box(
                modifier = Modifier
                    .size(compassSize)
                    .testTag("compass_dial_outer")
                    .weight(0.45f),
                contentAlignment = Alignment.Center
            ) {
                // Background Radial Glow Aura when aligned
                if (isAligned) {
                    Box(
                        modifier = Modifier
                            .size(glowSize)
                            .rotate(relativeAngle)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // Outer ring
                Box(
                    modifier = Modifier
                        .size(dialRingSize)
                        .border(2.dp, alignmentColor.copy(alpha = 0.4f), CircleShape)
                )

                // Compass Rose
                val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary

                Canvas(
                    modifier = Modifier
                        .size(canvasSize)
                        .rotate(-animatedHeading)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val outerRadius = size.width / 2f
                    val innerRadiusShort = outerRadius - 8.dp.toPx()
                    val innerRadiusLong = outerRadius - 16.dp.toPx()

                    for (deg in 0 until 360 step 15) {
                        val angleRad = Math.toRadians(deg.toDouble())
                        val isMajor = deg % 90 == 0
                        val isSemiMajor = deg % 45 == 0 && !isMajor
                        
                        val r1 = outerRadius
                        val r2 = if (isMajor) innerRadiusLong else if (isSemiMajor) (outerRadius - 11.dp.toPx()) else innerRadiusShort

                        val strokeW = if (isMajor) 2.5.dp.toPx() else if (isSemiMajor) 1.5.dp.toPx() else 1.dp.toPx()
                        val color = if (isMajor) primaryColor else if (isSemiMajor) secondaryColor else tickColor

                        val startX = center.x + r1 * sin(angleRad).toFloat()
                        val startY = center.y - r1 * cos(angleRad).toFloat()

                        val endX = center.x + r2 * sin(angleRad).toFloat()
                        val endY = center.y - r2 * cos(angleRad).toFloat()

                        drawLine(
                            color = color,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Live Pointer
                Box(
                    modifier = Modifier
                        .size(pointerSize)
                        .rotate(relativeAngle)
                        .testTag("compass_pointer"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val pointerLen = size.width / 2f - 16.dp.toPx()

                        val arrowPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(center.x, center.y - pointerLen)
                            lineTo(center.x - 10.dp.toPx(), center.y - pointerLen + 18.dp.toPx())
                            lineTo(center.x - 3.dp.toPx(), center.y - pointerLen + 14.dp.toPx())
                            lineTo(center.x - 3.dp.toPx(), center.y)
                            lineTo(center.x + 3.dp.toPx(), center.y)
                            lineTo(center.x + 3.dp.toPx(), center.y - pointerLen + 14.dp.toPx())
                            lineTo(center.x + 10.dp.toPx(), center.y - pointerLen + 18.dp.toPx())
                            close()
                        }
                        drawPath(arrowPath, secondaryColor)
                    }
                }

                // Inner core
                Surface(
                    modifier = Modifier
                        .size(innerCoreSize)
                        .border(2.dp, alignmentColor, CircleShape),
                    shape = CircleShape,
                    color = if (isAligned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = alignmentColor,
                            modifier = Modifier.size(coreIconSize)
                        )
                    }
                }
            }

            // Right Panel: Info & Settings
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = if (isAr) "بوصلة القبلة" else "Qiblah Compass",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                // Location Bar
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "موقعك الحالي: $locationLabel" else "Location: $locationLabel",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Alignment Status Bubble
                AnimatedVisibility(
                    visible = isAligned,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isAr) "🕌 تم محاذاة القبلة بدقة!" else "🕌 PERFECTLY ALIGNED!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                // Stats details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isAr) "اتجاه هاتفك" else "Your Heading",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${compassAzimuth.toInt()}°",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isAr) "اتجاه القبلة" else "Mecca Target",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${qiblaBearing.toInt()}°",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isAr) "ضع الهاتف بشكل مسطح للحصول على دقة أعلى." else "Hold your phone flat for highest precision.",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 680.dp)
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 24.dp,
                        bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() + 24.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
            // 1. TOP HEADER BANNER
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (isAr) "بوصلة القبلة" else "Qiblah Compass",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (isAr) "تحديد اتجاه الكعبة المشرفة بدقة متناهية" else "Points accurately towards the Holy Kaaba in Mecca",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // Info panel
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isAr) "حسب موقعك في: $locationLabel" else "Relative to: $locationLabel",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 2. LIVE ROTATING COMPASS DIAL CARD
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .testTag("compass_dial_outer"),
                contentAlignment = Alignment.Center
            ) {
                // Background Radial Glow Aura when aligned
                if (isAligned) {
                    Box(
                        modifier = Modifier
                            .size(270.dp)
                            .rotate(relativeAngle)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // Outer ring
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .border(2.dp, alignmentColor.copy(alpha = 0.4f), CircleShape)
                )

                // Compass Rose / Geometric markings drawing
                val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary

                Canvas(
                    modifier = Modifier
                        .size(240.dp)
                        .rotate(-animatedHeading) // Orient dial opposite to heading so true North stays Up
                ) {
                    // Draw 360 degree ticks
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val outerRadius = size.width / 2f
                    val innerRadiusShort = outerRadius - 10.dp.toPx()
                    val innerRadiusLong = outerRadius - 20.dp.toPx()

                    for (deg in 0 until 360 step 15) {
                        val angleRad = Math.toRadians(deg.toDouble())
                        val isMajor = deg % 90 == 0
                        val isSemiMajor = deg % 45 == 0 && !isMajor
                        
                        val r1 = outerRadius
                        val r2 = if (isMajor) innerRadiusLong else if (isSemiMajor) (outerRadius - 14.dp.toPx()) else innerRadiusShort

                        val strokeW = if (isMajor) 3.dp.toPx() else if (isSemiMajor) 2.dp.toPx() else 1.dp.toPx()
                        val color = if (isMajor) primaryColor else if (isSemiMajor) secondaryColor else tickColor

                        val startX = center.x + r1 * sin(angleRad).toFloat()
                        val startY = center.y - r1 * cos(angleRad).toFloat()

                        val endX = center.x + r2 * sin(angleRad).toFloat()
                        val endY = center.y - r2 * cos(angleRad).toFloat()

                        drawLine(
                            color = color,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Live Rotating Pointer pointing to Mecca
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .rotate(relativeAngle) // Relative rotation pointing Mecca
                        .testTag("compass_pointer"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val pointerLen = size.width / 2f - 24.dp.toPx()

                        // Draw golden Mecca Arrow pointer
                        val arrowPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(center.x, center.y - pointerLen)
                            lineTo(center.x - 14.dp.toPx(), center.y - pointerLen + 24.dp.toPx())
                            lineTo(center.x - 4.dp.toPx(), center.y - pointerLen + 18.dp.toPx())
                            lineTo(center.x - 4.dp.toPx(), center.y)
                            lineTo(center.x + 4.dp.toPx(), center.y)
                            lineTo(center.x + 4.dp.toPx(), center.y - pointerLen + 18.dp.toPx())
                            lineTo(center.x + 14.dp.toPx(), center.y - pointerLen + 24.dp.toPx())
                            close()
                         }
                         drawPath(arrowPath, secondaryColor)
                    }
                }

                // Beautiful Static Inner Core with Kaaba Icon representation
                Surface(
                    modifier = Modifier
                        .size(76.dp)
                        .border(3.dp, alignmentColor, CircleShape),
                    shape = CircleShape,
                    color = if (isAligned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = alignmentColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // 3. BOTTOM ALIGN STATUS PANEL
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                AnimatedVisibility(
                    visible = isAligned,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = if (isAr) "🕌 تم محاذاة القبلة بدقة!" else "🕌 PERFECTLY ALIGNED!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isAr) "اتجاه هاتفك" else "Your Heading",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${compassAzimuth.toInt()}°",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isAr) "اتجاه القبلة" else "Mecca Target",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${qiblaBearing.toInt()}°",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isAr) "يرجى وضع الهاتف بشكل مسطح والابتعاد عن المعادن أو الأغطية المغناطيسية لدقة أعلى." else "Hold your phone flat, away from metal objects or magnetic cases for highest precision.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        }
    }
}
