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

        // Simulate post-startup completion print stats (actual should listen to all task completion events)
        Handler(Looper.getMainLooper()).postDelayed({
            printStartupStats()
        }, 3000)
    }

    private fun printStartupStats() {
        val timeline = scheduler.getExecutionTimeline()
        val totalTime = timeline.maxOf { it.endTime } - timeline.minOf { it.startTime }

        println("\n===== Startup Task Performance Report =====")
        println("Total duration: ${totalTime}ms")
        println("Execution details:")

        timeline.forEach { info ->
            println("Task: ${info.taskId.padEnd(15)} | Thread: ${info.threadName.padEnd(15)} | Duration: ${info.duration}ms")
        }
        println("===============================")
    }

    override fun onTerminate() {
        super.onTerminate()
        scheduler.shutdown()
    }
}