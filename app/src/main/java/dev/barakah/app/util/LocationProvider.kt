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
import android.os.Looper
import androidx.core.content.ContextCompat

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

    /** The freshest fix the system has kept around, or null when there is none. */
    @SuppressLint("MissingPermission")
    fun bestLastKnown(): Location? {
        if (!hasPermission()) return null
        val manager = manager ?: return null
        return providers()
            .mapNotNull { provider ->
                try {
                    manager.getLastKnownLocation(provider)
                } catch (e: Exception) {
                    null
                }
            }
            .maxByOrNull { it.time }
    }

    /**
     * A fresh fix when the platform can produce one quickly, otherwise the best cached one.
     * On Android 11 and up that is [LocationManager.getCurrentLocation]; on older releases
     * the cached fix is used, and the update loop keeps it current.
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
                e.printStackTrace()
            }
        }
        onResult(bestLastKnown())
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
            e.printStackTrace()
        }
    }

    fun stopUpdates() {
        val listener = updatesListener ?: return
        try {
            manager?.removeUpdates(listener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        updatesListener = null
    }
}
