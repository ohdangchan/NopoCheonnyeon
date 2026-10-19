package com.nopo.cheonnyeon

import android.app.Application
import android.os.StrictMode
import com.nopo.cheonnyeon.app.AppContainer

class NopoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build())
        }
        container = AppContainer()
    }
}
