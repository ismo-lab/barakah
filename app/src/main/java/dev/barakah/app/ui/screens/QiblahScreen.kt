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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import dev.barakah.app.ui.BarakahViewModel
import dev.barakah.app.util.CompassStatus
import dev.barakah.app.util.QiblaManager
import dev.barakah.app.util.localize
import android.view.HapticFeedbackConstants
import kotlin.math.abs

/** Beyond this tilt the azimuth stops being meaningful, so the dial asks for a flat phone. */
private const val TILT_WARNING_DEGREES = 25f

@Composable
fun QiblahScreen(
    viewModel: BarakahViewModel,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val hapticEnabled by viewModel.enableTasbihHaptics.collectAsStateWithLifecycle()
    val useWesternNumbersInArabic by viewModel.useWesternNumbersInArabic.collectAsStateWithLifecycle()

    DisposableEffect(viewModel) {
        viewModel.startQiblaTracking()
        onDispose {
            viewModel.stopQiblaTracking()
        }
    }

    val compassAzimuth by viewModel.compassAzimuth.collectAsStateWithLifecycle()
    val compassStatus by viewModel.compassStatus.collectAsStateWithLifecycle()
    val compassTilt by viewModel.compassTilt.collectAsStateWithLifecycle()
    val qiblaBearing by viewModel.qiblaBearing.collectAsStateWithLifecycle()
    val locationLabel by viewModel.locationLabel.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isAr = appLanguage == "ar"

    // Whether the needle is allowed to be believed. With no live reading, pointing at a
    // direction would be a lie, so the dial is dimmed and the readout shows a dash instead.
    val hasLiveHeading = compassStatus.hasLiveHeading

    // Smooth heading rotation interpolation.
    // The needle always travels the short way round: animating the raw 0..360 value makes it
    // sweep all the way back whenever the heading crosses North, which reads as a stuck dial.
    var unwrappedHeading by remember { mutableStateOf(compassAzimuth) }
    LaunchedEffect(compassAzimuth) {
        unwrappedHeading += QiblaManager.shortestAngleDelta(unwrappedHeading, compassAzimuth)
    }
    val animatedHeading by animateFloatAsState(
        targetValue = unwrappedHeading,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "compassRotation"
    )

    // Compute relative pointer rotation (Mecca Bearing - Phone Heading)
    val relativeAngle = QiblaManager.normalizeDegrees(qiblaBearing.toFloat() - animatedHeading)

    // Check if phone is perfectly pointed to Kaaba (with hysteresis loop to prevent jitter)
    val exactDiff = abs(compassAzimuth - qiblaBearing.toFloat())
    val shortestDiff = if (exactDiff > 180f) 360f - exactDiff else exactDiff

    var isAligned by remember { mutableStateOf(false) }

    LaunchedEffect(shortestDiff, hasLiveHeading) {
        if (!hasLiveHeading) {
            isAligned = false
        } else if (shortestDiff < 2.5f && !isAligned) {
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

    // Explain the dial whenever it is not actually tracking, so a compass that cannot move is
    // never just silently frozen on screen.
    val statusMessage = when {
        compassStatus == CompassStatus.NO_SENSOR ->
            if (isAr) "لا يحتوي هذا الهاتف على مستشعر بوصلة، لذا لا يمكن للقرص تتبع الاتجاه."
            else "This phone has no compass sensor, so the dial cannot track your direction."
        compassStatus == CompassStatus.UNCALIBRATED ->
            if (isAr) "البوصلة تحتاج إلى معايرة: ابتعد عن المعادن والأغطية المغناطيسية ثم حرّك الهاتف على شكل الرقم 8."
            else "Compass needs calibrating: move away from metal and magnetic cases, then wave the phone in a figure 8."
        compassStatus == CompassStatus.STALLED ->
            if (isAr) "توقّف مستشعر البوصلة عن الإرسال. اخرج من هذه الشاشة ثم أعد فتحها."
            else "The compass sensor stopped reporting. Leave this screen and open it again."
        compassStatus == CompassStatus.WAITING ->
            if (isAr) "في انتظار قراءة البوصلة…"
            else "Waiting for the compass sensor…"
        compassTilt > TILT_WARNING_DEGREES ->
            if (isAr) "أمسك الهاتف بشكل مسطح للحصول على دقة أعلى."
            else "Hold your phone flat for highest precision."
        else ->
            if (isAr) "يرجى وضع الهاتف بشكل مسطح والابتعاد عن المعادن أو الأغطية المغناطيسية لدقة أعلى."
            else "Hold your phone flat, away from metal objects or magnetic cases for highest precision."
    }
    val statusIsProblem = compassStatus == CompassStatus.NO_SENSOR ||
        compassStatus == CompassStatus.STALLED ||
        compassStatus == CompassStatus.UNCALIBRATED
    val statusColor = if (statusIsProblem) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    }
    val headingText = if (hasLiveHeading) {
        "${compassAzimuth.toInt()}°".localize(isAr, useWesternNumbersInArabic)
    } else {
        "—"
    }

    val glowScale by animateFloatAsState(
        targetValue = if (isAligned) 1.08f else 1f,
        animationSpec = tween(300),
        label = "alignmentGlow"
    )

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isShortScreen = configuration.screenHeightDp < 620

    val compassSize = if (isLandscape || isShortScreen) 210.dp else 280.dp
    val glowSize = if (isLandscape || isShortScreen) 200.dp else 270.dp
    val dialRingSize = if (isLandscape || isShortScreen) 180.dp else 250.dp
    val canvasSize = if (isLandscape || isShortScreen) 170.dp else 240.dp
    val pointerSize = if (isLandscape || isShortScreen) 140.dp else 210.dp
    val innerCoreSize = if (isLandscape || isShortScreen) 56.dp else 76.dp
    val coreIconSize = if (isLandscape || isShortScreen) 26.dp else 36.dp

    // The two layouts draw the same dial, at the smaller landscape size or the full one.
    val dialStyle = if (isLandscape) QiblahDialStyle.Compact else QiblahDialStyle.Full

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Panel: Rotate compass
            QiblahDial(
                compassSize = compassSize,
                glowSize = glowSize,
                dialRingSize = dialRingSize,
                canvasSize = canvasSize,
                pointerSize = pointerSize,
                innerCoreSize = innerCoreSize,
                coreIconSize = coreIconSize,
                style = dialStyle,
                isAligned = isAligned,
                relativeAngle = relativeAngle,
                animatedHeading = animatedHeading,
                alignmentColor = alignmentColor,
                hasLiveHeading = hasLiveHeading,
                isAr = isAr,
                useWesternNumbersInArabic = useWesternNumbersInArabic,
                shortestDiff = shortestDiff,
                statusMessage = statusMessage,
                modifier = Modifier.weight(0.45f)
            )

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
                QiblahLocationRow(
                    text = if (isAr) "موقعك الحالي: $locationLabel" else "Location: $locationLabel",
                    textStyle = MaterialTheme.typography.bodySmall,
                    iconSize = 16.dp,
                    spacerWidth = 8.dp,
                    contentPadding = 10.dp,
                    shape = 12.dp,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                // Alignment Status Bubble
                QiblahAlignedBanner(
                    visible = isAligned,
                    isAr = isAr,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = 10.dp,
                    textPadding = PaddingValues(6.dp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Stats details Card
                QiblahStatsCard(
                    isAr = isAr,
                    headingText = headingText,
                    qiblaBearing = qiblaBearing,
                    statusMessage = statusMessage,
                    statusColor = statusColor,
                    useWesternNumbersInArabic = useWesternNumbersInArabic,
                    labelStyle = MaterialTheme.typography.labelSmall,
                    valueStyle = MaterialTheme.typography.titleMedium,
                    statusStyle = MaterialTheme.typography.labelSmall,
                    shape = 16.dp,
                    containerAlpha = 0.3f,
                    contentPadding = 12.dp,
                    rowSpacing = 8.dp
                )
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
                    .then(if (isShortScreen) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        // No window inset: the Scaffold content already starts below the status bar.
                        top = 24.dp,
                        // The Scaffold content already stops above the bottom nav bar; the window inset
                        // was counted twice and left a black band above the bar.
                        bottom = 24.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isShortScreen) Arrangement.Top else Arrangement.SpaceBetween
            ) {
            // 1. TOP HEADER BANNER
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (isAr) "بوصلة القبلة" else "Qiblah Compass",
                    style = if (isShortScreen) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isShortScreen) {
                    Text(
                        text = if (isAr) "تحديد اتجاه الكعبة المشرفة بدقة متناهية" else "Points accurately towards the Holy Kaaba in Mecca",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(if (isShortScreen) 6.dp else 12.dp))

                // Info panel
                QiblahLocationRow(
                    text = if (isAr) "حسب موقعك في: $locationLabel" else "Relative to: $locationLabel",
                    textStyle = MaterialTheme.typography.labelLarge,
                    iconSize = 18.dp,
                    spacerWidth = 10.dp,
                    contentPadding = 12.dp,
                    shape = 16.dp,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            // 2. LIVE ROTATING COMPASS DIAL CARD
            // The dial stays circular: a short window scrolls the column instead of clamping the dial,
            // which squashed it into an ellipse with its ticks clipped.
            QiblahDial(
                compassSize = compassSize,
                glowSize = glowSize,
                dialRingSize = dialRingSize,
                canvasSize = canvasSize,
                pointerSize = pointerSize,
                innerCoreSize = innerCoreSize,
                coreIconSize = coreIconSize,
                style = dialStyle,
                isAligned = isAligned,
                relativeAngle = relativeAngle,
                animatedHeading = animatedHeading,
                alignmentColor = alignmentColor,
                hasLiveHeading = hasLiveHeading,
                isAr = isAr,
                useWesternNumbersInArabic = useWesternNumbersInArabic,
                shortestDiff = shortestDiff,
                statusMessage = statusMessage
            )

            // 3. BOTTOM ALIGN STATUS PANEL
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                QiblahAlignedBanner(
                    visible = isAligned,
                    isAr = isAr,
                    textStyle = MaterialTheme.typography.titleMedium,
                    shape = 12.dp,
                    textPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                QiblahStatsCard(
                    isAr = isAr,
                    headingText = headingText,
                    qiblaBearing = qiblaBearing,
                    statusMessage = statusMessage,
                    statusColor = statusColor,
                    useWesternNumbersInArabic = useWesternNumbersInArabic,
                    labelStyle = MaterialTheme.typography.labelMedium,
                    valueStyle = MaterialTheme.typography.titleLarge,
                    statusStyle = MaterialTheme.typography.bodySmall,
                    shape = 20.dp,
                    containerAlpha = 0.4f,
                    contentPadding = 16.dp,
                    rowSpacing = 12.dp
                )
            }
        }
        }
    }
}

/**
 * The drawing details of the dial. The landscape layout draws it smaller, with slightly finer ticks
 * and a shorter pointer, so the numbers that differ between the two live here instead of being
 * copied into the drawing code twice.
 */
private class QiblahDialStyle(
    val tickInsetShort: Dp,
    val tickInsetLong: Dp,
    val tickInsetSemiMajor: Dp,
    val majorStroke: Dp,
    val semiMajorStroke: Dp,
    val pointerLengthInset: Dp,
    val arrowHalfWidth: Dp,
    val arrowTipHeight: Dp,
    val arrowInnerHeight: Dp,
    val arrowInnerHalfWidth: Dp,
    val coreBorder: Dp,
    val coreElevation: Dp
) {
    companion object {
        /** The smaller dial, which is the one landscape uses. */
        val Compact = QiblahDialStyle(
            tickInsetShort = 8.dp,
            tickInsetLong = 16.dp,
            tickInsetSemiMajor = 11.dp,
            majorStroke = 2.5.dp,
            semiMajorStroke = 1.5.dp,
            pointerLengthInset = 16.dp,
            arrowHalfWidth = 10.dp,
            arrowTipHeight = 18.dp,
            arrowInnerHeight = 14.dp,
            arrowInnerHalfWidth = 3.dp,
            coreBorder = 2.dp,
            coreElevation = 4.dp
        )

        /** The full size dial, which is the one portrait uses. */
        val Full = QiblahDialStyle(
            tickInsetShort = 10.dp,
            tickInsetLong = 20.dp,
            tickInsetSemiMajor = 14.dp,
            majorStroke = 3.dp,
            semiMajorStroke = 2.dp,
            pointerLengthInset = 24.dp,
            arrowHalfWidth = 14.dp,
            arrowTipHeight = 24.dp,
            arrowInnerHeight = 18.dp,
            arrowInnerHalfWidth = 4.dp,
            coreBorder = 3.dp,
            coreElevation = 6.dp
        )
    }
}

/**
 * The rotating dial: the glow that appears when aligned, the tick ring, the pointer towards Mecca
 * and the Kaaba core. Both layouts show exactly this, so it is drawn here once and only the sizes,
 * the alignment state and the wording are handed in.
 */
@Composable
private fun QiblahDial(
    compassSize: Dp,
    glowSize: Dp,
    dialRingSize: Dp,
    canvasSize: Dp,
    pointerSize: Dp,
    innerCoreSize: Dp,
    coreIconSize: Dp,
    style: QiblahDialStyle,
    isAligned: Boolean,
    relativeAngle: Float,
    animatedHeading: Float,
    alignmentColor: Color,
    hasLiveHeading: Boolean,
    isAr: Boolean,
    useWesternNumbersInArabic: Boolean,
    shortestDiff: Float,
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(compassSize)
            .testTag("compass_dial_outer")
            .semantics {
                val angleFormatted = shortestDiff.toInt().toString().localize(isAr, useWesternNumbersInArabic)
                contentDescription = when {
                    !hasLiveHeading -> statusMessage
                    isAligned -> if (isAr) "تم محاذاة اتجاه القبلة بنجاح" else "Qibla is perfectly aligned!"
                    else -> if (isAr) "القبلة تبعد $angleFormatted درجة" else "Qibla is $angleFormatted degrees away"
                }
            },
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
            val innerRadiusShort = outerRadius - style.tickInsetShort.toPx()
            val innerRadiusLong = outerRadius - style.tickInsetLong.toPx()

            for (deg in 0 until 360 step 15) {
                val angleRad = Math.toRadians(deg.toDouble())
                val isMajor = deg % 90 == 0
                val isSemiMajor = deg % 45 == 0 && !isMajor

                val r1 = outerRadius
                val r2 = if (isMajor) innerRadiusLong else if (isSemiMajor) (outerRadius - style.tickInsetSemiMajor.toPx()) else innerRadiusShort

                val strokeW = if (isMajor) style.majorStroke.toPx() else if (isSemiMajor) style.semiMajorStroke.toPx() else 1.dp.toPx()
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
                .alpha(if (hasLiveHeading) 1f else 0.25f)
                .testTag("compass_pointer"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val pointerLen = size.width / 2f - style.pointerLengthInset.toPx()

                val arrowPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x, center.y - pointerLen)
                    lineTo(center.x - style.arrowHalfWidth.toPx(), center.y - pointerLen + style.arrowTipHeight.toPx())
                    lineTo(center.x - style.arrowInnerHalfWidth.toPx(), center.y - pointerLen + style.arrowInnerHeight.toPx())
                    lineTo(center.x - style.arrowInnerHalfWidth.toPx(), center.y)
                    lineTo(center.x + style.arrowInnerHalfWidth.toPx(), center.y)
                    lineTo(center.x + style.arrowInnerHalfWidth.toPx(), center.y - pointerLen + style.arrowInnerHeight.toPx())
                    lineTo(center.x + style.arrowHalfWidth.toPx(), center.y - pointerLen + style.arrowTipHeight.toPx())
                    close()
                }
                drawPath(arrowPath, secondaryColor)
            }
        }

        // Inner core
        Surface(
            modifier = Modifier
                .size(innerCoreSize)
                .border(style.coreBorder, alignmentColor, CircleShape),
            shape = CircleShape,
            color = if (isAligned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            tonalElevation = style.coreElevation
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
}

/** The row that names the place the bearing is relative to. */
@Composable
private fun QiblahLocationRow(
    text: String,
    textStyle: TextStyle,
    iconSize: Dp,
    spacerWidth: Dp,
    contentPadding: Dp,
    shape: Dp,
    horizontalArrangement: Arrangement.Horizontal,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(shape),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = horizontalArrangement
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(spacerWidth))
            Text(
                text = text,
                style = textStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The banner that slides in once the phone is pointed at the Kaaba. */
@Composable
private fun QiblahAlignedBanner(
    visible: Boolean,
    isAr: Boolean,
    textStyle: TextStyle,
    shape: Dp,
    textPadding: PaddingValues,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            shape = RoundedCornerShape(shape),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = modifier
        ) {
            Text(
                text = if (isAr) "🕌 تم محاذاة القبلة بدقة!" else "🕌 PERFECTLY ALIGNED!",
                style = textStyle,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = textAlign,
                modifier = Modifier.padding(textPadding)
            )
        }
    }
}

/** The phone's heading, the bearing to Mecca, and the status line under them. */
@Composable
private fun QiblahStatsCard(
    isAr: Boolean,
    headingText: String,
    qiblaBearing: Double,
    statusMessage: String,
    statusColor: Color,
    useWesternNumbersInArabic: Boolean,
    labelStyle: TextStyle,
    valueStyle: TextStyle,
    statusStyle: TextStyle,
    shape: Dp,
    containerAlpha: Float,
    contentPadding: Dp,
    rowSpacing: Dp
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(shape),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = containerAlpha)
        )
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isAr) "اتجاه هاتفك" else "Your Heading",
                        style = labelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        headingText,
                        style = valueStyle,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isAr) "اتجاه القبلة" else "Mecca Target",
                        style = labelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${qiblaBearing.toInt()}°".localize(isAr, useWesternNumbersInArabic),
                        style = valueStyle,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(rowSpacing))
            Text(
                text = statusMessage,
                style = statusStyle,
                textAlign = TextAlign.Center,
                color = statusColor
            )
        }
    }
}
