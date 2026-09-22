package com.glyphix.shared.model

import com.glyphix.shared.util.currentTimeMillis

data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = currentTimeMillis(),
    val style: String = "INFO", // INFO, URGENT, FEATURE, UPDATE, RELEASE
    val link: String? = null,
    val linkText: String? = null,
    val apkUrl: String? = null,
    val version: String? = null
)
