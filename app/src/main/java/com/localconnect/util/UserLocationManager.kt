package com.localconnect.util

import android.content.Context
import android.content.SharedPreferences

object UserLocationManager {
    private const val PREFS_NAME = "user_location_prefs"
    private const val KEY_LATITUDE = "user_latitude"
    private const val KEY_LONGITUDE = "user_longitude"
    private const val KEY_LOCATION_NAME = "user_location_name"
    private const val KEY_LAST_UPDATED = "location_last_updated"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveUserLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        locationName: String? = null
    ) {
        getPrefs(context).edit()
            .putFloat(KEY_LATITUDE, latitude.toFloat())
            .putFloat(KEY_LONGITUDE, longitude.toFloat())
            .putString(KEY_LOCATION_NAME, locationName)
            .putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
            .apply()
    }

    fun getUserLatitude(context: Context): Double? {
        val prefs = getPrefs(context)
        val lat = prefs.getFloat(KEY_LATITUDE, Float.NaN)
        return if (lat.isNaN()) null else lat.toDouble()
    }

    fun getUserLongitude(context: Context): Double? {
        val prefs = getPrefs(context)
        val lon = prefs.getFloat(KEY_LONGITUDE, Float.NaN)
        return if (lon.isNaN()) null else lon.toDouble()
    }

    fun getUserLocationName(context: Context): String? {
        return getPrefs(context).getString(KEY_LOCATION_NAME, null)
    }

    fun getLastLocationUpdate(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_UPDATED, 0)
    }

    fun hasUserLocation(context: Context): Boolean {
        return getUserLatitude(context) != null && getUserLongitude(context) != null
    }

    fun clearUserLocation(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_LATITUDE)
            .remove(KEY_LONGITUDE)
            .remove(KEY_LOCATION_NAME)
            .remove(KEY_LAST_UPDATED)
            .apply()
    }

    data class UserLocation(
        val latitude: Double,
        val longitude: Double,
        val locationName: String?,
        val lastUpdated: Long
    )

    fun getUserLocation(context: Context): UserLocation? {
        val lat = getUserLatitude(context)
        val lon = getUserLongitude(context)

        return if (lat != null && lon != null) {
            UserLocation(
                latitude = lat,
                longitude = lon,
                locationName = getUserLocationName(context),
                lastUpdated = getLastLocationUpdate(context)
            )
        } else {
            null
        }
    }
}
