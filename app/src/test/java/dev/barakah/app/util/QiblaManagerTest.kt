package dev.barakah.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The compass is what tells a user which way to face, so the guards that keep it honest are
 * worth pinning down: a sensor reading that is not a number must never reach the filters (one
 * NaN used to freeze the dial for the whole session), angles must wrap predictably, the needle
 * must always take the short way round the circle, and a phone that cannot produce a heading
 * must say so rather than show a stale one.
 */
class QiblaManagerTest {

    @Test
    fun `nan and infinite samples are rejected`() {
        assertFalse(QiblaManager.isUsableSample(floatArrayOf(Float.NaN, 0f, 9.8f)))
        assertFalse(QiblaManager.isUsableSample(floatArrayOf(0f, Float.POSITIVE_INFINITY, 0f)))
        assertFalse(QiblaManager.isUsableSample(floatArrayOf(Float.NEGATIVE_INFINITY, 0f, 0f)))
    }

    @Test
    fun `zeroed and truncated samples are rejected`() {
        assertFalse(QiblaManager.isUsableSample(floatArrayOf(0f, 0f, 0f)))
        assertFalse(QiblaManager.isUsableSample(floatArrayOf(0f, 0f)))
    }

    @Test
    fun `real readings are accepted`() {
        assertTrue(QiblaManager.isUsableSample(floatArrayOf(0.1f, 9.7f, 1.2f)))
        assertTrue(QiblaManager.isUsableSample(floatArrayOf(21f, -4f, 38f), minMagnitude = 0.5f))
        // A magnetometer reporting a fraction of a microtesla is not usable.
        assertFalse(QiblaManager.isUsableSample(floatArrayOf(0.1f, 0f, 0f), minMagnitude = 0.5f))
    }

    @Test
    fun `degrees wrap into a single turn`() {
        assertEquals(0f, QiblaManager.normalizeDegrees(0f), 0.001f)
        assertEquals(350f, QiblaManager.normalizeDegrees(-10f), 0.001f)
        assertEquals(10f, QiblaManager.normalizeDegrees(370f), 0.001f)
        assertEquals(1f, QiblaManager.normalizeDegrees(721f), 0.001f)
        // A non-finite heading must read as 0 instead of filling the dial with NaN.
        assertEquals(0f, QiblaManager.normalizeDegrees(Float.NaN), 0.001f)
    }

    @Test
    fun `needle deltas always take the short way round`() {
        assertEquals(20f, QiblaManager.shortestAngleDelta(350f, 10f), 0.001f)
        assertEquals(-20f, QiblaManager.shortestAngleDelta(10f, 350f), 0.001f)
        assertEquals(0.7f, QiblaManager.shortestAngleDelta(359.5f, 0.2f), 0.001f)
        assertEquals(90f, QiblaManager.shortestAngleDelta(0f, 90f), 0.001f)
        assertEquals(0f, QiblaManager.shortestAngleDelta(Float.NaN, 90f), 0.001f)
    }

    @Test
    fun `tilt is zero when flat and ninety degrees when upright`() {
        val flat = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        assertEquals(0f, QiblaManager.tiltDegreesFromMatrix(flat), 0.5f)

        // Screen facing the horizon: the screen normal no longer points at the sky.
        val upright = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
        assertEquals(90f, QiblaManager.tiltDegreesFromMatrix(upright), 0.5f)

        // Face down on a table is still a phone lying flat.
        val faceDown = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, -1f)
        assertEquals(0f, QiblaManager.tiltDegreesFromMatrix(faceDown), 0.5f)
    }

    @Test
    fun `only a live heading is advertised as usable`() {
        assertTrue(CompassStatus.OK.hasLiveHeading)
        assertTrue(CompassStatus.UNCALIBRATED.hasLiveHeading)
        assertFalse(CompassStatus.WAITING.hasLiveHeading)
        assertFalse(CompassStatus.NO_SENSOR.hasLiveHeading)
        assertFalse(CompassStatus.STALLED.hasLiveHeading)
    }

    @Test
    fun `qibla bearing still points at mecca`() {
        // From Jakarta, Mecca lies to the west-north-west.
        assertEquals(295.2, QiblaManager.calculateQiblaBearing(-6.2088, 106.8456), 1.0)
        // Standing at the Kaaba there is no meaningful bearing, but it must stay finite.
        val atMecca = QiblaManager.calculateQiblaBearing(QiblaManager.MECCA_LAT, QiblaManager.MECCA_LNG)
        assertTrue(atMecca.isFinite())
    }
}
