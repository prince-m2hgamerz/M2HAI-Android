package com.m2h.m2haichatbot.utils

import android.content.Context
import kotlin.system.exitProcess

class CrashManager(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Log to Telegram
        TelegramLogger.logCrash(throwable)

        // Give it a moment to send
        Thread.sleep(2000)

        // Call original handler or kill process
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable)
        } else {
            exitProcess(1)
        }
    }

    companion object {
        fun initialize(context: Context) {
            CrashManager(context)
        }
    }
}
