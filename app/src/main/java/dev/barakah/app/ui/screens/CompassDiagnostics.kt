package dev.barakah.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.barakah.app.util.CompassStatus
import dev.barakah.app.util.SensorDiagnostics
import kotlinx.coroutines.delay

/**
 * A quiet link under the dial. When the compass misbehaves this is what turns "it does not work"
 * into facts that can be read off the screen or copied out of the app.
 */
@Composable
fun CompassDiagnosticsLink(isAr: Boolean, onOpen: () -> Unit) {
    TextButton(onClick = onOpen) {
        Text(
            text = if (isAr) "تشخيص البوصلة" else "Compass diagnostics",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Live sensor report, for phones we cannot attach a debugger to. The probe listens only while
 * this dialog is open, and Copy puts the whole report on the clipboard so it can be pasted
 * anywhere - into a note, an email, or a bug report.
 */
@Composable
fun CompassDiagnosticsDialog(
    isAr: Boolean,
    status: CompassStatus,
    heading: Float,
    tiltDegrees: Float,
    qiblaBearing: Double,
    locationLabel: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val probe = remember { SensorDiagnostics(context) }
    val clipboardManager = LocalClipboardManager.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var report by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    // The heading changes many times a second, so it is read through updated state rather than
    // used as an effect key: the report only needs to be rebuilt about once a second.
    val readout by rememberUpdatedState(
        CompassReadout(status, heading, tiltDegrees, qiblaBearing, locationLabel)
    )

    DisposableEffect(probe) {
        probe.start()
        onDispose { probe.stop() }
    }

    LaunchedEffect(probe) {
        while (true) {
            val current = readout
            report = probe.buildReport(
                status = current.status,
                heading = current.heading,
                tiltDegrees = current.tiltDegrees,
                qiblaBearing = current.qiblaBearing,
                locationLabel = current.locationLabel
            )
            delay(1000)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isAr) "تشخيص البوصلة" else "Compass diagnostics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isLandscape) 160.dp else 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = report,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clipboardManager.setText(AnnotatedString(report))
                copied = true
            }) {
                Text(
                    text = when {
                        copied && isAr -> "تم النسخ"
                        copied -> "Copied"
                        isAr -> "نسخ"
                        else -> "Copy"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isAr) "إغلاق" else "Close")
            }
        }
    )
}

private data class CompassReadout(
    val status: CompassStatus,
    val heading: Float,
    val tiltDegrees: Float,
    val qiblaBearing: Double,
    val locationLabel: String
)
