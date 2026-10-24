package com.neko.weather.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.neko.weather.model.CityLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Locale
import kotlin.coroutines.resume

class DeviceLocationService(private val context: Context) {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun locate(): CityLocation = reverseGeocode(currentLocation())

    /** 单次取当前坐标：优先新鲜的缓存位置，否则请求一次定位（15 秒超时）。 */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location {
        check(hasPermission()) { "请先允许位置权限" }
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> throw IllegalStateException("系统定位服务未开启")
        }
        val cached = manager.getLastKnownLocation(provider)
        return if (cached != null && System.currentTimeMillis() - cached.time < 30 * 60 * 1000L) {
            cached
        } else {
            withTimeout(15_000) { requestSingleLocation(provider) }
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private suspend fun requestSingleLocation(provider: String): Location = suspendCancellableCoroutine { continuation ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                manager.removeUpdates(this)
                if (continuation.isActive) continuation.resume(location)
            }

            override fun onProviderDisabled(provider: String) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        continuation.invokeOnCancellation { manager.removeUpdates(listener) }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(location: Location): CityLocation = withContext(Dispatchers.IO) {
        val address = runCatching {
            Geocoder(context, Locale.SIMPLIFIED_CHINESE)
                .getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
        }.getOrNull()
        CityLocation(
            id = "current-${location.latitude.toFloat()}-${location.longitude.toFloat()}",
            name = address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: "当前位置",
            admin = address?.adminArea.orEmpty(),
            country = address?.countryName.orEmpty(),
            latitude = location.latitude,
            longitude = location.longitude,
            isCurrentLocation = true,
        )
    }
}
