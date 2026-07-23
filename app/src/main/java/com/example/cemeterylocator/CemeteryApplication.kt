package com.example.cemeterylocator

import android.app.Application
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration

class CemeteryApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // osmdroid requires an explicit user agent (some tile servers reject
        // requests without one) and a writable cache dir for downloaded tiles.
        Configuration.getInstance().load(
            this,
            PreferenceManager.getDefaultSharedPreferences(this)
        )
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidTileCache = cacheDir.resolve("osmdroid/tiles")
        Configuration.getInstance().osmdroidBasePath = cacheDir.resolve("osmdroid")
    }
}
