package com.example.localconnect.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "userId") val userId: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "profileImage") val profileImage: String = "",
    @Json(name = "bio") val bio: String = "",
    @Json(name = "createdAt") val createdAt: Long = 0L
)

