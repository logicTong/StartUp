package com.nova.startup.example

import ConfigLoadTask
import LogInitTask
import NetworkInitTask
import StorageInitTask
import android.app.Application
import android.os.Handler
import android.os.Looper
import com.nova.startup.core.StartupScheduler

/** Application entry class */
class MyApplication : Application() {
    private lateinit var scheduler: StartupScheduler

    override fun onCreate() {
        super.onCreate()
        scheduler = StartupScheduler()

        // Register all startup tasks
        scheduler.registerTasks(
            StorageInitTask(),
            ConfigLoadTask(),
            NetworkInitTask(),
            LogInitTask()
        )

        // Start scheduling
        scheduler.start()

    }



    override fun onTerminate() {
        super.onTerminate()
        scheduler.shutdown()
    }
}