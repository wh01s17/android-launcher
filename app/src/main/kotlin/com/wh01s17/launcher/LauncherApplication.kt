package com.wh01s17.launcher

import android.app.Application
import com.wh01s17.launcher.di.AppContainer

class LauncherApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
