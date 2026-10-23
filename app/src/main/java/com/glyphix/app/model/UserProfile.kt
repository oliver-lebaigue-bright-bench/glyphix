package com.glyphix.app.model

import androidx.annotation.Keep

@Keep
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val profilePictureUrl: String? = null,
    val totalVisualizedTime: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
