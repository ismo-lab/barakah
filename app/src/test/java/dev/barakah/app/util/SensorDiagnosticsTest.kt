package dev.barakah.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The diagnostics report is what gets read back when the compass misbehaves on a phone we cannot
 * attach a debugger to, so its verdicts have to be right: they are the only thing standing
 * between "it does not work" and a repair.
 */
class SensorDiagnosticsTest {

    @Test
    fun `magnitude is the length of the vector and refuses junk`() {
        assertEquals(5f, SensorDiagnostics.magnitudeOf(floatArrayOf(3f, 4f, 0f))!!, 0.001f)
        assertNull(SensorDiagnostics.magnitudeOf(null))
        assertNull(SensorDiagnostics.magnitudeOf(floatArrayOf(1f, 0f)))
        assertNull(SensorDiagnostics.magnitudeOf(floatArrayOf(Float.NaN, 0f, 0f)))
        assertNull(SensorDiagnostics.magnitudeOf(floatArrayOf(0f, Float.POSITIVE_INFINITY, 0f)))
    }

    @Test
    fun `magnetic field verdicts split dead weak healthy and interfered`() {
        assertTrue(SensorDiagnostics.magneticFieldVerdict(null).contains("no reading"))
        assertTrue(SensorDiagnostics.magneticFieldVerdict(0f).contains("dead reading"))
        assertTrue(SensorDiagnostics.magneticFieldVerdict(2f).contains("implausibly weak"))
        assertTrue(SensorDiagnostics.magneticFieldVerdict(45f).contains("healthy"))
        assertTrue(SensorDiagnostics.magneticFieldVerdict(500f).contains("very strong"))
    }

    @Test
    fun `acceleration verdicts split free fall healthy and moving`() {
        assertTrue(SensorDiagnostics.accelerationVerdict(null).contains("no reading"))
        assertTrue(SensorDiagnostics.accelerationVerdict(0.2f).contains("free-fall"))
        assertTrue(SensorDiagnostics.accelerationVerdict(9.81f).contains("healthy"))
        assertTrue(SensorDiagnostics.accelerationVerdict(30f).contains("outside"))
    }

    @Test
    fun `rate label reports events per second and copes with silence`() {
        assertEquals("no events", SensorDiagnostics.rateLabel(0L, 0L, 1000L))
        assertEquals("50 events", SensorDiagnostics.rateLabel(50L, 1000L, 1000L))
        assertTrue(SensorDiagnostics.rateLabel(50L, 1000L, 2000L).endsWith("50.0/s"))
        assertTrue(SensorDiagnostics.rateLabel(120L, 0L, 1000L).endsWith("120.0/s"))
    }

    @Test
    fun `accuracy labels name what android reported`() {
        assertEquals("HIGH(3)", SensorDiagnostics.accuracyName(3))
        assertEquals("MEDIUM(2)", SensorDiagnostics.accuracyName(2))
        assertEquals("LOW(1)", SensorDiagnostics.accuracyName(1))
        assertEquals("UNRELIABLE(0)", SensorDiagnostics.accuracyName(0))
        assertEquals("never reported", SensorDiagnostics.accuracyName(-1))
    }

    @Test
    fun `sensor labels name the sensors the compass depends on`() {
        assertEquals("magnetic field", SensorDiagnostics.label(android.hardware.Sensor.TYPE_MAGNETIC_FIELD))
        assertEquals("rotation vector", SensorDiagnostics.label(android.hardware.Sensor.TYPE_ROTATION_VECTOR))
        assertEquals("accelerometer", SensorDiagnostics.label(android.hardware.Sensor.TYPE_ACCELEROMETER))
    }
}
