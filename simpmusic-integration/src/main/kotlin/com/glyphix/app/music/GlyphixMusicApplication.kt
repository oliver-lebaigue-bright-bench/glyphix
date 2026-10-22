// SPDX-License-Identifier: GPL-3.0-only
package com.glyphix.app.music

import android.app.Application
import android.os.Build
import com.glyphix.app.BuildConfig
import com.maxrave.common.AppIdentity
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.simpmusic.di.viewModelModule
import com.maxrave.simpmusic.utils.VersionManager
import multiplatform.network.cmptoast.AppContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** Initialises the native FOSS music stack once, in the same process as Glyphix. */
class GlyphixMusicApplication : Application() {
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
        VersionManager.initialize()
        MusicIntegration.install(NativeMusicFeature())
    }
}
