// SPDX-License-Identifier: GPL-3.0-only
package com.glyphix.app.music

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import com.eygraber.uri.toKmpUriOrNull
import com.glyphix.app.logic.AutoPresetEngine
import com.glyphix.app.service.AudioCaptureService
import com.maxrave.domain.data.model.intent.GenericIntent
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import com.maxrave.media3.audio.GlyphixPcmBridge
import com.maxrave.media3.di.startService
import com.maxrave.simpmusic.App
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.maxrave.domain.mediaservice.handler.ToastType
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin

class NativeMusicFeature : MusicFeature {
    private val handler: MediaPlayerHandler by lazy { getKoin().get() }
    private val viewModel: SharedViewModel by lazy { getKoin().get() }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var metadataJob: Job? = null
    private var owner: ComponentActivity? = null
    private var activityModule: org.koin.core.module.Module? = null
    private var binding: ServiceConnection? = null

    override fun attach(activity: ComponentActivity) {
        owner = activity
        activityModule = module { single<ComponentActivity> { activity } }.also { loadKoinModules(it) }
        GlyphixPcmBridge.sink = { samples, rate ->
            AudioCaptureService.sInstance?.submitIntegratedMusicPcm(samples, rate)
        }
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                viewModel.isServiceRunning = true
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                viewModel.isServiceRunning = false
            }
        }
        binding = connection
        handler.pushPlayerError = { error -> viewModel.makeToast(error.message ?: error.errorCodeName) }
        handler.showToast = { type ->
            viewModel.makeToast(when (type) {
                is ToastType.ExplicitContent -> "Explicit content is blocked by your music settings."
                is ToastType.PlayerError -> type.error
                is ToastType.SponsorBlockSkip -> "Skipped ${type.category}"
            })
        }
        if (metadataJob == null) {
            // Process lifetime collector: track changes still update presets with the screen off.
            metadataJob = applicationScope.launch {
                handler.nowPlayingState.collect { track ->
                    if (AudioCaptureService.sInstance?.captureSource == AudioCaptureService.CaptureSource.SIMPMUSIC) {
                        val metadata = track.mediaItem.metadata
                        AutoPresetEngine.getInstance().updateMediaMetadata(metadata.title, metadata.artist, metadata.albumTitle, null)
                    }
                }
            }
        }
        startService(activity, connection)
        viewModel.runSmartDownloadsIfDue()
        onIntent(activity.intent)
    }

    override fun detach(activity: ComponentActivity) {
        if (owner !== activity) return
        binding?.let { activity.unbindService(it) }
        binding = null
        activityModule?.let { unloadKoinModules(it) }
        activityModule = null
        owner = null
        // The callback only references the service singleton. Background playback keeps PCM sync.
    }

    override fun onIntent(intent: Intent) {
        val data = intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri() ?: return
        // Spotify remains owned by Glyphix's existing OAuth integration.
        if (data.scheme == "glyphix") return
        viewModel.setIntent(GenericIntent(intent.action, data.toKmpUriOrNull(), intent.type))
    }

    @Composable
    override fun Screen(modifier: Modifier) {
        Box(modifier.fillMaxSize()) {
            App(viewModel = viewModel, inheritHostTheme = true)
        }
    }

    @Composable
    override fun Player(modifier: Modifier, onOpenMusic: () -> Unit) {
        val track by handler.nowPlayingState.collectAsStateWithLifecycle()
        val controls by handler.controlState.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        if (track.mediaItem.mediaId.isBlank()) return
        Surface(modifier.clickable(onClick = onOpenMusic), shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track.mediaItem.metadata.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall)
                    Text(track.mediaItem.metadata.artist.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall)
                }
                IconButton(enabled = controls.isPreviousAvailable, onClick = {
                    scope.launch { handler.onPlayerEvent(PlayerEvent.Previous) }
                }) { Icon(Icons.Default.SkipPrevious, "Previous track") }
                IconButton(onClick = { scope.launch { handler.onPlayerEvent(PlayerEvent.PlayPause) } }) {
                    Icon(if (controls.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (controls.isPlaying) "Pause" else "Play")
                }
                IconButton(enabled = controls.isNextAvailable, onClick = {
                    scope.launch { handler.onPlayerEvent(PlayerEvent.Next) }
                }) { Icon(Icons.Default.SkipNext, "Next track") }
            }
        }
    }

}
