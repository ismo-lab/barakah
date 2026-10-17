package dev.barakah.app.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.SystemClock
import android.view.Display
import android.view.Surface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

/**
 * Writes down what the device's motion sensors actually do, so that "the compass does not move"
 * can be diagnosed on a phone with no developer tools attached: which sensors exist, whether
 * events arrive at all, what the raw values are, what accuracy Android reports for them, and what
 * each heading calculation makes of them.
 *
 * This is a probe, not part of the compass. It registers its own listeners only while the
 * diagnostics view is open, unregisters them when it closes, and never touches [QiblaManager].
 */
class SensorDiagnostics(context: Context) : SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val displayManager = appContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val eventLog = ArrayDeque<String>()
    private val sensors = LinkedHashMap<Int, Tracked>()
    private val stats = LinkedHashMap<Int, Stats>()

    // Raw, unsmoothed vectors kept only to offer a second opinion on the heading.
    private val lastAccelerometer = FloatArray(3)
    private val lastMagneticField = FloatArray(3)
    private var hasAccelerometer = false
    private var hasMagneticField = false
    private val rMatrix = FloatArray(9)
    private val iMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var running = false
    private var rejectedSamples = 0L
    private val reportedRejections = mutableSetOf<Int>()

    private class Tracked(val sensor: Sensor?, val registered: Boolean)

    private class Stats {
        var events = 0L
        var firstEventAt = 0L
        var lastEventAt = 0L
        var latest: FloatArray? = null
        var accuracy = -1
    }

    fun start() {
        if (running) return
        val manager = sensorManager ?: run {
            note("sensor service UNAVAILABLE")
            return
        }

        running = true
        rejectedSamples = 0L
        reportedRejections.clear()
        note("probe started")

        for (type in MONITORED_TYPES) {
            val sensor = manager.getDefaultSensor(type)
            val registered = sensor != null &&
                manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            sensors[type] = Tracked(sensor, registered)
            stats.getOrPut(type) { Stats() }
            note(
                when {
                    sensor == null -> "${label(type)}: NOT PRESENT on this device"
                    registered -> "${label(type)}: registered ok"
                    else -> "${label(type)}: present but registerListener refused it"
                }
            )
        }
    }

    fun stop() {
        if (!running) return
        running = false
        sensorManager?.unregisterListener(this)
        note("probe stopped")
    }

    override fun onSensorChanged(event: SensorEvent) {
        val type = event.sensor.type
        val stats = stats.getOrPut(type) { Stats() }
        val now = SystemClock.elapsedRealtime()

        if (stats.events == 0L) {
            stats.firstEventAt = now
            note("${label(type)}: first sample ${formatVector(event.values)}")
        }
        stats.events++
        stats.lastEventAt = now

        if (!isUsable(event.values)) {
            rejectedSamples++
            if (reportedRejections.add(type)) {
                note("${label(type)}: UNUSABLE sample ${formatVector(event.values)} (not a number, or empty)")
            }
            return
        }

        stats.latest = event.values.copyOf(minOf(3, event.values.size))
        when (type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, lastAccelerometer, 0, 3)
                hasAccelerometer = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, lastMagneticField, 0, 3)
                hasMagneticField = true
            }
            else -> Unit
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        val type = sensor?.type ?: return
        val stats = stats.getOrPut(type) { Stats() }
        if (stats.accuracy != accuracy) {
            note("${label(type)}: accuracy now ${accuracyName(accuracy)}")
        }
        stats.accuracy = accuracy
    }

    /** The copy-ready report. Safe to call repeatedly; it re-reads the live state each time. */
    fun buildReport(
        status: CompassStatus,
        heading: Float,
        tiltDegrees: Float,
        qiblaBearing: Double,
        locationLabel: String
    ): String {
        val now = SystemClock.elapsedRealtime()
        val lines = mutableListOf<String>()

        lines += "Barakah compass diagnostics"
        lines += "generated      : ${timeFormat.format(Date())}"
        lines += "app            : v${dev.barakah.app.BuildConfig.VERSION_NAME} (${dev.barakah.app.BuildConfig.VERSION_CODE})"
        lines += "device         : ${Build.MANUFACTURER} ${Build.MODEL}"
        lines += "android        : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        lines += "screen         : rotation ${displayRotation()}, heading axes ${remapAxesLabel()}"
        lines += ""
        lines += "compass status : ${status.name} (live heading: ${status.hasLiveHeading})"
        lines += "app heading    : ${formatDegrees(heading)}, tilt ${formatDegrees(tiltDegrees)} (0 = flat on a table)"
        lines += "qibla bearing  : ${formatDegrees(qiblaBearing.toFloat())} from $locationLabel"
        lines += ""
        lines += "sensor service : " + if (sensorManager == null) {
            "UNAVAILABLE"
        } else {
            "available, ${sensorManager.getSensorList(Sensor.TYPE_ALL).size} sensors reported"
        }
        lines += "probe          : ${if (running) "listening" else "stopped"}, $rejectedSamples unusable samples seen"
        lines += ""
        lines += "sensors this app looks for:"
        for (type in MONITORED_TYPES) {
            lines += "  ${describeSensor(type)}"
        }
        lines += ""
        lines += "live readings (probe, raw and unfiltered):"
        for (type in MONITORED_TYPES) {
            lines += "  ${describeLive(type, now)}"
        }
        lines += ""
        lines += "heading by each path (device frame, no smoothing):"
        lines += "  accelerometer + magnetometer -> ${azimuthFromAccelerometerAndMagneticField()?.let(::formatDegrees) ?: "no heading"}"
        lines += "  rotation vector              -> ${azimuthFromRotationVector()?.let(::formatDegrees) ?: "no heading"}"
        lines += ""
        lines += "what the raw numbers mean:"
        lines += "  magnetic field : ${magneticFieldVerdict(magnitudeOf(if (hasMagneticField) lastMagneticField else null))}"
        lines += "  acceleration   : ${accelerationVerdict(magnitudeOf(if (hasAccelerometer) lastAccelerometer else null))}"
        lines += ""
        lines += "event log (newest last):"
        for (entry in eventLog) {
            lines += "  $entry"
        }
        return lines.joinToString("\n")
    }

    private fun describeSensor(type: Int): String {
        val tracked = sensors[type] ?: return "${label(type)}: not probed yet"
        val sensor = tracked.sensor ?: return "${label(type)}: NOT PRESENT"
        return buildString {
            append(label(type))
            append(": ")
            append(if (tracked.registered) "registered" else "REGISTRATION REFUSED")
            append(", name=").append(sensor.name)
            append(", vendor=").append(sensor.vendor)
            append(", v=").append(sensor.version)
            append(", range=").append(formatFloat(sensor.maximumRange))
            append(", res=").append(formatFloat(sensor.resolution))
            append(", minDelay=").append(sensor.minDelay).append("us")
            append(", power=").append(formatFloat(sensor.power)).append("mA")
        }
    }

    private fun describeLive(type: Int, now: Long): String {
        val stats = stats[type] ?: return "${label(type)}: no data"
        val values = stats.latest
        return buildString {
            append(label(type))
            append(": ").append(rateLabel(stats.events, stats.firstEventAt, now))
            append(", last ").append(if (stats.lastEventAt == 0L) "never" else "${now - stats.lastEventAt}ms ago")
            append(", values ").append(if (values == null) "none" else formatVector(values))
            magnitudeOf(values)?.let { append(", |v|=").append(formatFloat(it)) }
            append(", accuracy=").append(accuracyName(stats.accuracy))
        }
    }

    /** Device-frame azimuth straight from the accelerometer and magnetometer samples. */
    private fun azimuthFromAccelerometerAndMagneticField(): Float? {
        if (!hasAccelerometer || !hasMagneticField) return null
        if (!SensorManager.getRotationMatrix(rMatrix, iMatrix, lastAccelerometer, lastMagneticField)) {
            return null
        }
        return azimuthFromMatrix()
    }

    /** Device-frame azimuth straight from the rotation vector samples. */
    private fun azimuthFromRotationVector(): Float? {
        val values = stats[Sensor.TYPE_ROTATION_VECTOR]?.latest ?: return null
        if (values.size < 3) return null
        // Void in the platform SDK, so the matrix is judged by the heading it produces.
        SensorManager.getRotationMatrixFromVector(rMatrix, values)
        return azimuthFromMatrix()
    }

    private fun azimuthFromMatrix(): Float? {
        SensorManager.getOrientation(rMatrix, orientationAngles)
        val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        return if (azimuth.isFinite()) QiblaManager.normalizeDegrees(azimuth) else null
    }

    private fun note(message: String) {
        eventLog.addLast("${timeFormat.format(Date())}  $message")
        while (eventLog.size > MAX_LOG_LINES) {
            eventLog.removeFirst()
        }
    }

    private fun formatVector(values: FloatArray): String =
        values.take(3).joinToString(prefix = "(", postfix = ")", separator = ", ") { formatFloat(it) }

    private fun formatFloat(value: Float): String = String.format(Locale.US, "%.2f", value)

    private fun formatDegrees(value: Float): String = "${String.format(Locale.US, "%.1f", value)} deg"

    @Suppress("DEPRECATION") // Display#getRotation is the only read available down to minSdk 24.
    private fun displayRotation(): Int =
        displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0

    private fun remapAxesLabel(): String = when (displayRotation()) {
        Surface.ROTATION_90 -> "AXIS_Y / AXIS_MINUS_X"
        Surface.ROTATION_180 -> "AXIS_MINUS_X / AXIS_MINUS_Y"
        Surface.ROTATION_270 -> "AXIS_MINUS_Y / AXIS_X"
        else -> "AXIS_X / AXIS_Y"
    }

    companion object {
        private const val MAX_LOG_LINES = 60

        private val MONITORED_TYPES = intArrayOf(
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_GYROSCOPE
        )

        fun label(type: Int): String = when (type) {
            Sensor.TYPE_ROTATION_VECTOR -> "rotation vector"
            Sensor.TYPE_GAME_ROTATION_VECTOR -> "game rotation vector"
            Sensor.TYPE_ACCELEROMETER -> "accelerometer"
            Sensor.TYPE_MAGNETIC_FIELD -> "magnetic field"
            Sensor.TYPE_GYROSCOPE -> "gyroscope"
            else -> "sensor type $type"
        }

        fun accuracyName(accuracy: Int): String = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "HIGH(3)"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "MEDIUM(2)"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "LOW(1)"
            SensorManager.SENSOR_STATUS_UNRELIABLE -> "UNRELIABLE(0)"
            else -> "never reported"
        }

        fun rateLabel(events: Long, firstEventAtMs: Long, nowMs: Long): String {
            if (events <= 0L) return "no events"
            val window = nowMs - firstEventAtMs
            if (window <= 0L) return "$events events"
            val perSecond = events.toFloat() * 1000f / window
            return "$events events, ${String.format(Locale.US, "%.1f", perSecond)}/s"
        }

        fun magnitudeOf(values: FloatArray?): Float? {
            if (values == null || values.size < 3) return null
            var squared = 0f
            for (axis in 0 until 3) {
                val value = values[axis]
                if (!value.isFinite()) return null
                squared += value * value
            }
            return sqrt(squared)
        }

        /** Earth's field is roughly 25-65 uT; anything far outside that deserves a human look. */
        fun magneticFieldVerdict(magnitudeMicroTesla: Float?): String = when {
            magnitudeMicroTesla == null -> "no reading at all"
            magnitudeMicroTesla < 0.5f -> "dead reading (0 uT): the sensor is blocked or failing"
            magnitudeMicroTesla < 5f -> "implausibly weak field: likely blocked (Android 'Sensors off'?) or bad hardware"
            magnitudeMicroTesla <= 80f -> "earth-like field, sensor looks healthy"
            else -> "very strong field: magnets, metal or a magnetic case nearby"
        }

        fun accelerationVerdict(magnitudeMetresPerSecondSquared: Float?): String = when {
            magnitudeMetresPerSecondSquared == null -> "no reading at all"
            magnitudeMetresPerSecondSquared < 1f -> "near free-fall: the device is moving, or the sensor is dead"
            magnitudeMetresPerSecondSquared in 8f..12f -> "about 1g, sensor looks healthy"
            else -> "outside the usual 1g: the device may be moving"
        }

        private fun isUsable(values: FloatArray): Boolean {
            if (values.size < 3) return false
            for (axis in 0 until 3) {
                if (!values[axis].isFinite()) return false
            }
            return true
        }
    }
}
