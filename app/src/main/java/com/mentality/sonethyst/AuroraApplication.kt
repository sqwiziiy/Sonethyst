package com.mentality.sonethyst

import android.app.Application
import com.mentality.sonethyst.data.AppContainer

class AuroraApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
