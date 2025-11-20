package com.example.localconnect.data.model

import com.google.firebase.firestore.PropertyName

data class Staff(
    @PropertyName("uid") val uid: String = "",
    @PropertyName("email") val email: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("approved") val approved: Boolean = false,
    @PropertyName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @PropertyName("createdBy") val createdBy: String = "",
    @PropertyName("role") val role: String = "staff", // staff, admin, moderator
    @PropertyName("department") val department: String? = null
) {
    constructor() : this("", "", "", false, 0L, "", "staff", null)
}

