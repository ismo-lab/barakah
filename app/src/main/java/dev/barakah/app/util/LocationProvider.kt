package dev.barakah.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thin wrapper around the platform [LocationManager].
 *
 * This replaces Play Services' fused provider: the app only needs a city-level fix, updated
 * every few minutes, which the platform providers already deliver. Staying on AOSP location
 * keeps the app free of Google Play Services.
 *
 * Every call is safe to make without the location permission or on a device with location
 * switched off; those simply produce no location.
 */
class LocationProvider(private val context: Context) {

    private companion object {
        const val TAG = "LocationProvider"

        /** A cached fix older than this counts as no fix at all: the phone may have moved since. */
        const val MAX_CACHED_FIX_AGE_MS = 10 * 60 * 1000L

        /** How long a single update request may take before the cache answers instead. */
        const val SINGLE_FIX_TIMEOUT_MS = 20 * 1000L
    }

    private val manager: LocationManager?
        get() = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /** The network provider is cheaper and plenty for a city; GPS is the fallback. */
    private fun providers(): List<String> {
        val manager = manager ?: return emptyList()
        return listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).filter { provider ->
            try {
                manager.isProviderEnabled(provider)
            } catch (e: Exception) {
                false
            }
        }
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isEnabled(): Boolean = try {
        manager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
            manager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    } catch (e: Exception) {
        false
    }

    /**
     * The freshest fix the system has kept around, or null when there is none recent enough. A fix is
     * only as good as its age - the phone may have crossed a country since it was taken - and a wrong
     * city is worse than asking the user to choose one.
     */
    @SuppressLint("MissingPermission")
    fun bestLastKnown(): Location? {
        if (!hasPermission()) return null
        val manager = manager ?: return null
        val freshest = providers()
            .mapNotNull { provider ->
                try {
                    manager.getLastKnownLocation(provider)
                } catch (e: Exception) {
                    null
                }
            }
            .maxByOrNull { it.time }
        return freshest?.takeIf { System.currentTimeMillis() - it.time <= MAX_CACHED_FIX_AGE_MS }
    }

    /**
     * A fresh fix when the platform can produce one quickly, otherwise the freshest cached one.
     * On Android 11 and up that is [LocationManager.getCurrentLocation]; below it a single update
     * request, because a cached fix is all that arrives without asking for one.
     */
    @SuppressLint("MissingPermission")
    fun currentLocation(onResult: (Location?) -> Unit) {
        if (!hasPermission()) {
            onResult(null)
            return
        }
        val manager = manager
        val provider = providers().firstOrNull()
        if (manager == null || provider == null) {
            onResult(null)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                manager.getCurrentLocation(provider, null, ContextCompat.getMainExecutor(context)) { location ->
                    onResult(location ?: bestLastKnown())
                }
                return
            } catch (e: Exception) {
                Log.e(TAG, "Unable to ask the system for a current location", e)
            }
            onResult(bestLastKnown())
            return
        }

        requestSingleFix(manager, provider, onResult)
    }

    /**
     * One fresh fix, falling back to the cache if the request cannot start or does not answer in
     * time. Either way the listener is removed and the answer is delivered exactly once.
     */
    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun requestSingleFix(manager: LocationManager, provider: String, onResult: (Location?) -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        val delivered = AtomicBoolean(false)
        val deliver: (Location?) -> Unit = { location ->
            if (delivered.compareAndSet(false, true)) {
                handler.removeCallbacksAndMessages(null)
                onResult(location)
            }
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                stopListening(manager, this)
                deliver(location)
            }

            @Deprecated("Part of the LocationListener interface")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }

        handler.postDelayed({
            stopListening(manager, listener)
            deliver(bestLastKnown())
        }, SINGLE_FIX_TIMEOUT_MS)

        try {
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e(TAG, "Unable to request a single location update", e)
            deliver(bestLastKnown())
        }
    }

    private fun stopListening(manager: LocationManager, listener: LocationListener) {
        try {
            manager.removeUpdates(listener)
        } catch (e: Exception) {
            // Already gone; nothing to clean up.
        }
    }

    private var updatesListener: LocationListener? = null

    /**
     * Starts listening for location changes, reusing [minTimeMs] and [minDistanceM] as the
     * system's hints. Calling this twice is a no-op, matching the previous behaviour.
     */
    @SuppressLint("MissingPermission")
    fun startUpdates(minTimeMs: Long, minDistanceM: Float, onLocation: (Location) -> Unit) {
        if (updatesListener != null || !hasPermission()) return
        val manager = manager ?: return
        val provider = providers().firstOrNull() ?: return

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = onLocation(location)

            @Deprecated("Part of the LocationListener interface")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }

        try {
            updatesListener = listener
            manager.requestLocationUpdates(provider, minTimeMs, minDistanceM, listener, Looper.getMainLooper())
        } catch (e: Exception) {
            updatesListener = null
            Log.e(TAG, "Unable to start listening for location updates", e)
        }
    }

    fun stopUpdates() {
        val listener = updatesListener ?: return
        try {
            manager?.removeUpdates(listener)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to stop listening for location updates", e)
        }
        updatesListener = null
    }
}
