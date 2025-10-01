package com.localconnect.data.model

enum class PostCategory(val displayName: String) {
    GENERAL("General"),
    HEALTH("Health"),
    ROADS("Roads"),
    INFRASTRUCTURE("Infrastructure"),
    LOST_AND_FOUND("Lost & Found"),
    EVENTS("Events"),
    WASTE("Waste"),
    WATER_SUPPLY("Water Supply"),
    SPORTS("Sports"),
    CULTURE("Culture"),
    EMERGENCY("Emergency");

    companion object {
        fun fromDisplayName(displayName: String): PostCategory? {
            return values().find { it.displayName == displayName }
        }
    }
}
