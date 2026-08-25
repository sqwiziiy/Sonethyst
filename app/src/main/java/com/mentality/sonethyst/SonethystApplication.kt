package com.mentality.sonethyst

import android.app.Application
import com.mentality.sonethyst.data.AppContainer
import java.io.File

class SonethystApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        migrateLegacySettings()
        container = AppContainer(this)
    }

    private fun migrateLegacySettings() {
        runCatching {
            val dataStoreDir = File(filesDir, "datastore")
            val legacy = File(dataStoreDir, "aurora_settings.preferences_pb")
            val current = File(dataStoreDir, "sonethyst_settings.preferences_pb")

            if (legacy.exists() && !current.exists()) {
                dataStoreDir.mkdirs()

                if (!legacy.renameTo(current)) {
                    legacy.copyTo(current, overwrite = false)
                    legacy.delete()
                }
            }
        }
    }

}
