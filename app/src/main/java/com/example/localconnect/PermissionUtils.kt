package com.example.localconnect

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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

    // Combined permission checks for different features
    fun hasMapPermissions(context: Context): Boolean {
        return hasLocationPermission(context) || hasCoarseLocationPermission(context)
    }

    fun hasPostCreationPermissions(context: Context): Boolean {
        return hasCameraPermission(context) && hasLocationPermission(context)
    }

    // Get required permissions arrays
    fun getLocationPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    fun getPostCreationPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun getMapPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    // Batch permission result handling
    fun handleLocationPermissionResult(context: Context, permissions: Map<String, Boolean>) {
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val anyLocationGranted = fineLocationGranted || coarseLocationGranted

        saveLocationPermissionResult(context, anyLocationGranted)
    }

    fun handleCameraPermissionResult(context: Context, permissions: Map<String, Boolean>) {
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        saveCameraPermissionResult(context, cameraGranted)
    }

    fun handleMapPermissionResult(context: Context, permissions: Map<String, Boolean>) {
        handleLocationPermissionResult(context, permissions)
    }

    fun handlePostCreationPermissionResult(context: Context, permissions: Map<String, Boolean>) {
        handleLocationPermissionResult(context, permissions)
        handleCameraPermissionResult(context, permissions)
    }
}
