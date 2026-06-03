package dev.barakah.app.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

class QiblaManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _compassHeading = MutableStateFlow(0f)
    val compassHeading: StateFlow<Float> = _compassHeading

    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    fun start() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val alpha = 0.12f
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            if (!hasGravity) {
                System.arraycopy(event.values, 0, gravityValues, 0, minOf(event.values.size, gravityValues.size))
                hasGravity = true
            } else {
                for (j in 0 until 3) {
                    gravityValues[j] = gravityValues[j] + alpha * (event.values[j] - gravityValues[j])
                }
            }
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            if (!hasGeomagnetic) {
                System.arraycopy(event.values, 0, geomagneticValues, 0, minOf(event.values.size, geomagneticValues.size))
                hasGeomagnetic = true
            } else {
                for (j in 0 until 3) {
                    geomagneticValues[j] = geomagneticValues[j] + alpha * (event.values[j] - geomagneticValues[j])
                }
            }
        }

        if (hasGravity && hasGeomagnetic) {
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, gravityValues, geomagneticValues)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                // azimuth is orientation[0] in radians. Convert to degrees.
                var azimuthDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                azimuthDegrees = (azimuthDegrees + 360) % 360
                
                // Let's also do a subtle low-pass or immediate value with a small deadband / check to avoid noise
                _compassHeading.value = azimuthDegrees
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Unused
    }

    companion object {
        const val MECCA_LAT = 21.4225
        const val MECCA_LNG = 39.8262

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
    }
}
