package com.glyphix.shared.model

import com.glyphix.shared.util.currentTimeMillis

data class LeaderboardEntry(
    val userId: String = "",
    val name: String = "Anonymous",
    val profilePictureUrl: String? = null,
    val totalTimeMs: Long = 0,
    val lastUpdated: Long = currentTimeMillis()
)
