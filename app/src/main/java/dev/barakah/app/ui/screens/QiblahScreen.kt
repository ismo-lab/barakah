package dev.barakah.app.ui.screens

import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.abs
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

    // Check if phone is perfectly pointed to Kaaba (within 4 degrees threshold)
    val diff = abs(compassAzimuth - qiblaBearing.toFloat())
    val isAligned = (if (diff > 180f) 360f - diff else diff) < 4.0f

    LaunchedEffect(isAligned) {
        if (isAligned && hapticEnabled) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                start = 20.dp, 
                end = 20.dp, 
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp,
                bottom = 20.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. TOP HEADER BANNER
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isAr) "بوصلة القبلة" else "Qiblah Compass",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isAr) "تحديد اتجاه الكعبة المشرفة بدقة متناهية" else "Points accurately towards the Holy Kaaba in Mecca",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
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
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. LIVE ROTATING COMPASS DIAL CARD
        Box(
            modifier = Modifier
                .size(280.dp)
                .testTag("compass_dial_outer")
                .align(Alignment.CenterHorizontally),
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

                // Draw North, East, South, West labels
                val fontScale = 14.sp.toPx()
                // Removed North accent pointer per request to show only qiblah arrow
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
            modifier = Modifier.fillMaxWidth()
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
