package com.meteo.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat

object LocationProvider {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isGpsEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun getLocation(context: Context, onSuccess: (Location) -> Unit, onError: (String) -> Unit) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val recent = lastKnownLocation(manager)
        if (recent != null) {
            onSuccess(recent)
            return
        }

        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> {
                onError("Position indisponible. Activez la localisation dans les réglages.")
                return
            }
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                manager.removeUpdates(this)
                onSuccess(location)
            }

            @Deprecated("Superseded in API 31", ReplaceWith("onLocationChanged(location)"))
            override fun onStatusChanged(p: String?, status: Int, extras: android.os.Bundle?) {}
        }

        try {
            manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            android.os.Handler(context.mainLooper).postDelayed({
                try {
                    manager.removeUpdates(listener)
                } catch (_: Exception) {}
                lastKnownLocation(manager)?.let(onSuccess)
                    ?: onError("Impossible d'obtenir votre position. Réessayez en extérieur.")
            }, 20_000L)
        } catch (e: SecurityException) {
            onError("Permission de localisation refusée.")
        } catch (e: Exception) {
            onError("Erreur de localisation : ${e.message}")
        }
    }

    private fun lastKnownLocation(manager: LocationManager): Location? {
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        return providers.asSequence()
            .filter { manager.isProviderEnabled(it) }
            .mapNotNull { provider ->
                try {
                    manager.getLastKnownLocation(provider)
                } catch (_: SecurityException) {
                    null
                }
            }
            .filter { it.age() < 10 * 60_000L }
            .maxByOrNull { it.time }
    }

    private fun Location.age(): Long = System.currentTimeMillis() - time
}
