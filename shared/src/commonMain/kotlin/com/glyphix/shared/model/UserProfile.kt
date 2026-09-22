package com.glyphix.shared.model

import com.glyphix.shared.util.currentTimeMillis

data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val profilePictureUrl: String? = null,
    val totalVisualizedTime: Long = 0,
    val createdAt: Long = currentTimeMillis()
)
