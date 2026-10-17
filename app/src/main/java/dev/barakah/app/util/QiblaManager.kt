package dev.barakah.app.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.SystemClock
import android.view.Display
import android.view.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

/**
 * How much the current compass reading can be trusted, so the dial can explain itself instead of
 * sitting on a stale value with no hint of what is wrong.
 */
enum class CompassStatus {
    /** The device exposes neither a rotation vector nor an accelerometer + magnetometer pair. */
    NO_SENSOR,

    /** Sensors are registered but no valid reading has arrived yet. */
    WAITING,

    /** A heading is available and the magnetometer is properly calibrated. */
    OK,

    /** A heading is available but the magnetometer asks to be recalibrated (metal, magnets, drift). */
    UNCALIBRATED,

    /** Readings used to arrive but have stopped, which would otherwise look like a frozen needle. */
    STALLED;

    /** True while [QiblaManager.compassHeading] is actually following the device. */
    val hasLiveHeading: Boolean get() = this == OK || this == UNCALIBRATED
}

class QiblaManager(context: Context) : SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val displayManager = appContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

    private val rotationVector = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _compassHeading = MutableStateFlow(0f)
    val compassHeading: StateFlow<Float> = _compassHeading

    private val _status = MutableStateFlow(CompassStatus.WAITING)
    val status: StateFlow<CompassStatus> = _status

    /** Degrees the phone is tilted away from lying flat: 0 = flat, 90 = held upright. */
    private val _tiltDegrees = MutableStateFlow(0f)
    val tiltDegrees: StateFlow<Float> = _tiltDegrees

    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private val rMatrix = FloatArray(9)
    private val iMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var hasGravity = false
    private var hasGeomagnetic = false
    private var tracking = false
    private var lastReadingAt = 0L
    private var magneticAccuracyUnreliable = false

    fun start() {
        if (tracking) return
        val manager = sensorManager ?: run {
            _status.value = CompassStatus.NO_SENSOR
            return
        }

        tracking = true
        lastReadingAt = 0L
        _status.value = CompassStatus.WAITING

        // The platform rotation vector is already fused and calibrated by the sensor hub, so it
        // gives a usable heading with no filtering of ours. Only devices without it fall back to
        // the raw accelerometer + magnetometer pair, which has to be smoothed by hand.
        if (rotationVector != null &&
            manager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)
        ) {
            return
        }

        val accelerometerRegistered = accelerometer != null &&
            manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        val magnetometerRegistered = magnetometer != null &&
            manager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
        if (!accelerometerRegistered || !magnetometerRegistered) {
            // Half a sensor pair cannot produce a heading; say so instead of never reporting.
            manager.unregisterListener(this)
            _status.value = CompassStatus.NO_SENSOR
        }
    }

    fun stop() {
        tracking = false
        sensorManager?.unregisterListener(this)
    }

    /**
     * Called while the compass screen is open. Readings arrive many times a second, so a gap of
     * several seconds means the sensor has stopped reporting and the dial is showing a stale value.
     */
    fun checkForStall(thresholdMillis: Long = STALL_TIMEOUT_MS) {
        if (!tracking || !_status.value.hasLiveHeading) return
        if (lastReadingAt == 0L || SystemClock.elapsedRealtime() - lastReadingAt > thresholdMillis) {
            _status.value = CompassStatus.STALLED
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                if (!isUsableSample(event.values)) return
                // Void in the platform SDK, so the matrix it writes is judged where it is used.
                SensorManager.getRotationMatrixFromVector(rMatrix, event.values)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                if (!isUsableSample(event.values)) return
                smoothInto(gravityValues, event.values, hasGravity)
                hasGravity = true
                if (!fillRotationMatrix()) return
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                if (!isUsableSample(event.values, MIN_MAGNETIC_FIELD)) return
                smoothInto(geomagneticValues, event.values, hasGeomagnetic)
                hasGeomagnetic = true
                if (!fillRotationMatrix()) return
            }

            else -> return
        }

        publishHeading()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        when (sensor?.type) {
            Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_ROTATION_VECTOR -> Unit
            else -> return
        }

        magneticAccuracyUnreliable = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
        if (!tracking) return
        _status.value = when {
            magneticAccuracyUnreliable -> CompassStatus.UNCALIBRATED
            lastReadingAt > 0L -> CompassStatus.OK
            else -> CompassStatus.WAITING
        }
    }

    private fun fillRotationMatrix(): Boolean {
        if (!hasGravity || !hasGeomagnetic) return false
        return SensorManager.getRotationMatrix(rMatrix, iMatrix, gravityValues, geomagneticValues)
    }

    /**
     * Exponential smoothing of a raw sensor vector. [seeded] marks the first accepted sample,
     * which is copied as-is so the filters start from a real measurement.
     */
    private fun smoothInto(target: FloatArray, sample: FloatArray, seeded: Boolean) {
        if (!seeded) {
            System.arraycopy(sample, 0, target, 0, minOf(sample.size, target.size))
            return
        }
        for (axis in target.indices) {
            if (axis < sample.size) {
                target[axis] += SMOOTHING_ALPHA * (sample[axis] - target[axis])
            }
        }
    }

    private fun publishHeading() {
        // Read the heading from the top edge of the screen whatever way the phone is held, so
        // the dial stays correct in landscape as well.
        SensorManager.remapCoordinateSystem(rMatrix, remapAxisX(), remapAxisY(), remappedMatrix)
        SensorManager.getOrientation(remappedMatrix, orientationAngles)

        val rawAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        // Judged before normalising: normalizeDegrees turns a non-finite value into 0, which must
        // not be published as a heading someone could pray towards.
        if (!rawAzimuth.isFinite()) return

        _tiltDegrees.value = tiltDegreesFromMatrix(remappedMatrix)
        lastReadingAt = SystemClock.elapsedRealtime()
        _compassHeading.value = normalizeDegrees(rawAzimuth)
        _status.value = if (magneticAccuracyUnreliable) CompassStatus.UNCALIBRATED else CompassStatus.OK
    }

    private fun remapAxisX(): Int = when (displayRotation()) {
        Surface.ROTATION_90 -> SensorManager.AXIS_Y
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X
        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y
        else -> SensorManager.AXIS_X
    }

    private fun remapAxisY(): Int = when (displayRotation()) {
        Surface.ROTATION_90 -> SensorManager.AXIS_MINUS_X
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_Y
        Surface.ROTATION_270 -> SensorManager.AXIS_X
        else -> SensorManager.AXIS_Y
    }

    @Suppress("DEPRECATION") // Display#getRotation is the only read available down to minSdk 24.
    private fun displayRotation(): Int =
        displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0

    companion object {
        const val MECCA_LAT = 21.4225
        const val MECCA_LNG = 39.8262

        /** How long the compass may go without a reading before it is called stalled. */
        const val STALL_TIMEOUT_MS = 3000L

        private const val SMOOTHING_ALPHA = 0.12f
        private const val MIN_ACCELERATION = 0.001f
        private const val MIN_MAGNETIC_FIELD = 0.5f

        /**
         * Computes Great Circle bearing from current lat/lng to Mecca (True North referenced)
         */
        fun calculateQiblaBearing(lat: Double, lng: Double): Double {
            val phiCurrent = Math.toRadians(lat)
            val lambdaCurrent = Math.toRadians(lng)

            val phiMecca = Math.toRadians(MECCA_LAT)
            val lambdaMecca = Math.toRadians(MECCA_LNG)

            val deltaLambda = lambdaMecca - lambdaCurrent

            val y = sin(deltaLambda) * cos(phiMecca)
            val x = cos(phiCurrent) * sin(phiMecca) - sin(phiCurrent) * cos(phiMecca) * cos(deltaLambda)

            val bearing = Math.toDegrees(atan2(y, x))
            return (bearing + 360.0) % 360.0
        }

        /**
         * True when a raw sensor sample can be trusted into the pipeline. Drivers do hand out
         * NaN, infinities or an all-zero "sensor not ready" vector, and because the accelerometer
         * and magnetometer vectors above are smoothed recursively, a single NaN would turn the
         * stored vector into NaN permanently - which freezes the heading for the whole session.
         */
        fun isUsableSample(values: FloatArray, minMagnitude: Float = MIN_ACCELERATION): Boolean {
            if (values.size < 3) return false
            var squaredMagnitude = 0f
            for (axis in 0 until 3) {
                val value = values[axis]
                if (!value.isFinite()) return false
                squaredMagnitude += value * value
            }
            return squaredMagnitude >= minMagnitude * minMagnitude
        }

        /** Wraps degrees into 0..360, turning a non-finite value into 0 rather than poisoning the dial. */
        fun normalizeDegrees(degrees: Float): Float {
            if (!degrees.isFinite()) return 0f
            val wrapped = degrees % 360f
            return if (wrapped < 0f) wrapped + 360f else wrapped
        }

        /** Signed shortest rotation from [from] to [to], in -180..180, so needles never take the long way round. */
        fun shortestAngleDelta(from: Float, to: Float): Float {
            if (!from.isFinite() || !to.isFinite()) return 0f
            val delta = normalizeDegrees(to) - normalizeDegrees(from)
            return when {
                delta > 180f -> delta - 360f
                delta < -180f -> delta + 360f
                else -> delta
            }
        }

        /**
         * Degrees between the screen and the horizon: 0 when the phone lies flat, 90 when it is
         * held upright, where the azimuth is no longer meaningful.
         */
        fun tiltDegreesFromMatrix(rotationMatrix: FloatArray): Float {
            if (rotationMatrix.size < 9) return 0f
            val screenNormalZ = rotationMatrix[8].coerceIn(-1f, 1f)
            return Math.toDegrees(acos(abs(screenNormalZ).toDouble())).toFloat()
        }
    }
}
