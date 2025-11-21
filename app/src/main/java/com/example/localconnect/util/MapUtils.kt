package com.example.localconnect.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

object MapUtils {
    /**
     * Opens the specified location in Google Maps app or falls back to browser
     * @param context Android context
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @param locationName Optional name/label for the location
     */
    fun openLocationInMaps(
        context: Context,
        latitude: Double,
        longitude: Double,
        locationName: String? = null
    ) {
        val label = locationName?.let { "($it)" } ?: ""
        val uri = "geo:$latitude,$longitude?q=$latitude,$longitude$label".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to browser if Google Maps is not installed
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude".toUri()
            )
            context.startActivity(browserIntent)
        }
    }
}
