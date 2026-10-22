package com.glyphix.app.music

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Native music implementation is installed by the integration build's Application. */
interface MusicFeature {
    fun attach(activity: androidx.activity.ComponentActivity, onOpenMusic: () -> Unit) {}
    fun detach(activity: androidx.activity.ComponentActivity) {}
    fun onIntent(intent: android.content.Intent) {}
    fun onSourceSelected() {}
    @Composable fun Screen(modifier: Modifier)
    @Composable fun Player(modifier: Modifier, onOpenMusic: () -> Unit)
}

object MusicIntegration {
    var feature: MusicFeature? = null
        private set
    fun install(feature: MusicFeature) { check(this.feature == null); this.feature = feature }
    val available: Boolean get() = feature != null
}
