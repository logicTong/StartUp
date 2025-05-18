package com.nova.startup.core

import StartupTask
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * In the Observer Pattern, notify all observers that the task is completed.
 */
object TaskCompleteNotifier {

    private val observers = mutableListOf<TaskCompleteObserver>()
    private var isNotifiedAllCompletion = false

    @Synchronized
    fun addObserver(observer: TaskCompleteObserver) {
        observers.add(observer)
    }

    @Synchronized
    fun notifyTaskComplete(scheduler: StartupScheduler, task: StartupTask) {
        observers.forEach {
            it.onTaskComplete(scheduler, task)
        }
    }

    @Synchronized
    fun notifyAllTaskComplete(scheduler: StartupScheduler) {
        if (isNotifiedAllCompletion) {
            return
        }
        isNotifiedAllCompletion = true
        observers.forEach {
            it.onAllTaskComplete(scheduler)
        }
        clearObservers()
    }

    @Synchronized
    fun clearObservers() {
        observers.clear()
    }

}

interface TaskCompleteObserver {

    fun onTaskComplete(scheduler: StartupScheduler, task: StartupTask) {
        if (task.taskStatus != StartupTask.TaskStatus.SUCCEEDED) {
            Log.e(
                "TaskCompleteObserver", "onTaskComplete: ${task.taskId} FAIL!!",
                IllegalStateException("${task.taskId} FAIL, task status is ${task.taskStatus}")
            )
        }
    }

    fun onAllTaskComplete(scheduler: StartupScheduler)

}