package com.example.localconnect.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

object PermissionUtils {
    private const val PREFS_NAME = "app_prefs"
    private const val LOCATION_ASKED = "location_permission_asked"
    private const val LOCATION_GRANTED = "location_permission_granted"
    private const val CAMERA_ASKED = "camera_permission_asked"
    private const val CAMERA_GRANTED = "camera_permission_granted"

    // Location Permission Methods
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

    // Camera Permission Methods
    fun wasCameraPermissionAsked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(CAMERA_ASKED, false)
    }

    fun isCameraPermissionGranted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(CAMERA_GRANTED, false)
    }

    fun saveCameraPermissionResult(context: Context, granted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(CAMERA_ASKED, true)
            .putBoolean(CAMERA_GRANTED, granted)
            .apply()
    }

    // Runtime Permission Checks
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCoarseLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Combined permission checks
    fun hasAnyLocationPermission(context: Context): Boolean {
        return hasLocationPermission(context) || hasCoarseLocationPermission(context)
    }

    // Map-specific permission checks (alias for location permissions)
    fun hasMapPermissions(context: Context): Boolean {
        return hasAnyLocationPermission(context)
    }

    // Get required permissions arrays
    fun getLocationPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    // Map permissions (same as location permissions)
    fun getMapPermissions(): Array<String> {
        return getLocationPermissions()
    }

    // Batch permission result handling
    fun handleLocationPermissionResult(context: Context, permissions: Map<String, Boolean>) {
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val anyLocationGranted = fineLocationGranted || coarseLocationGranted

        saveLocationPermissionResult(context, anyLocationGranted)
    }

    // Map permission result handler (alias for location permission handler)
    fun handleMapPermissionResult(context: Context, permissions: Map<String, Boolean>) {
        handleLocationPermissionResult(context, permissions)
    }

    fun handleCameraPermissionResult(context: Context, permissions: Map<String, Boolean>) {
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        saveCameraPermissionResult(context, cameraGranted)
    }

    /**
     * Get the last known location from the device.
     * Checks both GPS and Network providers.
     * Returns null if no location is available or permissions are not granted.
     */
    @Suppress("DEPRECATION")
    fun getLastKnownLocation(context: Context): Location? {
        if (!hasAnyLocationPermission(context)) {
            return null
        }

        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if location services are enabled on the device.
     */
    fun isLocationServiceEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}
