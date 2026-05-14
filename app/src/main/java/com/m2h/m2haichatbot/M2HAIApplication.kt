package com.m2h.m2haichatbot

import android.app.Application
import com.m2h.m2haichatbot.utils.CrashManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class M2HAIApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashManager.initialize(this)
    }
}
