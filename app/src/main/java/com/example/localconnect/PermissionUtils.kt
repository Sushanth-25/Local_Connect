package com.example.localconnect

import android.content.Context

object PermissionUtils {
    private const val PREFS_NAME = "app_prefs"
    private const val LOCATION_ASKED = "location_permission_asked"
    private const val LOCATION_GRANTED = "location_permission_granted"

    fun wasLocationPermissionAsked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(LOCATION_ASKED, false)
    }

    fun isLocationPermissionGranted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(LOCATION_GRANTED, false)
    }

    fun saveLocationPermissionResult(context: Context, granted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(LOCATION_ASKED, true)
            .putBoolean(LOCATION_GRANTED, granted)
            .apply()
    }
}
