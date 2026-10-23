// SPDX-License-Identifier: GPL-3.0-only
package com.glyphix.app.music

import android.app.Application
import android.os.Build
import com.glyphix.app.BuildConfig
import com.maxrave.common.AppIdentity
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.simpmusic.di.viewModelModule
import com.maxrave.simpmusic.BuildKonfig
import com.maxrave.simpmusic.service.backup.AutoBackupScheduler
import com.maxrave.domain.manager.DataStoreManager
import org.simpmusic.lastfm.configLastfm
import org.simpmusic.crashlytics.configCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform.getKoin
import com.maxrave.simpmusic.utils.VersionManager
import multiplatform.network.cmptoast.AppContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** Initialises the native FOSS music stack once, in the same process as Glyphix. */
class GlyphixMusicApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override fun onCreate() {
        super.onCreate()
        AppContext.set(applicationContext)
        startKoin {
            androidContext(this@GlyphixMusicApplication)
            loadAllModules(AppIdentity(
                applicationId = BuildConfig.APPLICATION_ID,
                versionName = BuildConfig.VERSION_NAME,
                platform = "Android ${Build.VERSION.RELEASE}",
                isDevBuild = BuildConfig.DEBUG,
            ))
            modules(viewModelModule)
        }
        configLastfm(BuildKonfig.lastfmApiKey, BuildKonfig.lastfmSecret)
        // A DSN is optional and must be supplied explicitly; no new reporting runs by default.
        if (BuildKonfig.sentryDsn.isNotBlank()) configCrashlytics(this, BuildKonfig.sentryDsn)
        applicationScope.launch {
            AutoBackupScheduler(this@GlyphixMusicApplication, getKoin().get<DataStoreManager>()).observeAndSchedule()
        }
        VersionManager.initialize(BuildConfig.VERSION_NAME)
        MusicIntegration.install(NativeMusicFeature())
    }
}
