package com.example.localconnect.util

import com.example.localconnect.data.model.Post
import kotlin.math.*

object LocationUtils {
    private const val EARTH_RADIUS_KM = 6371.0
    private const val COMMUNITY_RADIUS_KM = 30.0

    /**
     * Calculate distance between two points using Haversine formula
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in kilometers
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_KM * c
    }

    /**
     * Parse latitude from location string format "lat, lon"
     */
    fun parseLatitudeFromLocation(location: String?): Double? {
        if (location.isNullOrBlank()) return null
        return try {
            val parts = location.split(",")
            if (parts.size >= 2) {
                parts[0].trim().toDouble()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse longitude from location string format "lat, lon"
     */
    fun parseLongitudeFromLocation(location: String?): Double? {
        if (location.isNullOrBlank()) return null
        return try {
            val parts = location.split(",")
            if (parts.size >= 2) {
                parts[1].trim().toDouble()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if a post is within community radius (30km) from user's location
     */
    fun isWithinCommunityRadius(
        userLat: Double?,
        userLon: Double?,
        postLocation: String?
    ): Boolean {
        if (userLat == null || userLon == null || postLocation.isNullOrBlank()) {
            return true // If no coordinates, include in results
        }

        val postLat = parseLatitudeFromLocation(postLocation)
        val postLon = parseLongitudeFromLocation(postLocation)

        if (postLat == null || postLon == null) {
            return true // Include posts without valid coordinates
        }

        val distance = calculateDistance(userLat, userLon, postLat, postLon)
        return distance <= COMMUNITY_RADIUS_KM
    }

    /**
     * Check if a post is within community radius (30km) from user's location
     * Uses Post's direct latitude/longitude fields (new approach)
     */
    fun isPostNearUser(
        userLat: Double?,
        userLon: Double?,
        post: Post
    ): Boolean {
        if (userLat == null || userLon == null) {
            return true // If no user coordinates, include in results
        }

        val distance = calculateDistance(userLat, userLon, post.latitude, post.longitude)
        return distance <= COMMUNITY_RADIUS_KM
    }

    /**
     * Filter posts based on user's location and community radius
     * IMPORTANT: This filters posts to only show those within 30km of the user
     */
    fun filterPostsByLocation(
        posts: List<Post>,
        userLat: Double?,
        userLon: Double?
    ): List<Post> {
        if (userLat == null || userLon == null) {
            println("LocationUtils: No user location provided, returning empty list for safety")
            return emptyList() // Don't show posts if user location is not available
        }

        return posts.filter { post ->
            // Calculate actual distance using required coordinates
            val distance = calculateDistance(userLat, userLon, post.latitude, post.longitude)
            val isWithinRadius = distance <= COMMUNITY_RADIUS_KM

            println("LocationUtils: Post ${post.postId} - Location: ${post.locationName}, Distance: ${"%.2f".format(distance)}km, Within 30km: $isWithinRadius")

            return@filter isWithinRadius
        }
    }

    /**
     * Get distance string for display
     */
    fun getDistanceString(
        userLat: Double?,
        userLon: Double?,
        postLocation: String?
    ): String {
        if (userLat == null || userLon == null || postLocation.isNullOrBlank()) {
            return ""
        }

        val postLat = parseLatitudeFromLocation(postLocation)
        val postLon = parseLongitudeFromLocation(postLocation)

        if (postLat == null || postLon == null) {
            return ""
        }

        val distance = calculateDistance(userLat, userLon, postLat, postLon)
        return when {
            distance < 1.0 -> "${(distance * 1000).roundToInt()}m away"
            distance < 10.0 -> "${"%.1f".format(distance)}km away"
            else -> "${distance.roundToInt()}km away"
        }
    }

    /**
     * Get distance string for display
     * Uses Post's direct latitude/longitude fields (new approach)
     */
    fun getDistanceString(
        userLat: Double?,
        userLon: Double?,
        post: Post
    ): String {
        if (userLat == null || userLon == null) {
            return ""
        }

        val distance = calculateDistance(userLat, userLon, post.latitude, post.longitude)
        return when {
            distance < 1.0 -> "${(distance * 1000).roundToInt()}m away"
            distance < 10.0 -> "${"%.1f".format(distance)}km away"
            else -> "${distance.roundToInt()}km away"
        }
    }

    /**
     * Check if coordinates are valid
     */
    fun areValidCoordinates(lat: Double?, lon: Double?): Boolean {
        if (lat == null || lon == null) return false
        return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180
    }

    /**
     * Get approximate location name from coordinates (placeholder - you'd integrate with geocoding service)
     */
    fun getLocationName(lat: Double?, lon: Double?): String {
        if (!areValidCoordinates(lat, lon)) return "Unknown location"
        // This is a placeholder - in real implementation, you'd use a geocoding service
        return "Lat: ${"%.4f".format(lat)}, Lon: ${"%.4f".format(lon)}"
    }
}
