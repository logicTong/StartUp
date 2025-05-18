package com.nova.startup.example

import StartupTask
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.nova.startup.R
import com.nova.startup.core.StartupScheduler
import com.nova.startup.core.TaskCompleteNotifier
import com.nova.startup.core.TaskCompleteObserver

class MainActivity : ComponentActivity() {

    private lateinit var taskStatusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 TextView
        taskStatusTextView = findViewById(R.id.taskStatusTextView)

        // 注册任务完成通知监听器
        TaskCompleteNotifier.addObserver(object : TaskCompleteObserver {
            override fun onTaskComplete(scheduler: StartupScheduler, task: StartupTask) {
                taskStatusTextView.post {
                    // 在任务完成时更新 TextView 的文本
                    taskStatusTextView.text =
                        "${taskStatusTextView.text} \n complete task =${task.taskId}"
                }

            }

            override fun onAllTaskComplete(scheduler: StartupScheduler) {
                taskStatusTextView.post {
                    val info = printStartupStats(scheduler)
                    taskStatusTextView.text = "${taskStatusTextView.text} \n\n\n\n$info"
                }

            }
        })

    }


    private fun printStartupStats(scheduler: StartupScheduler): String {
        val timeline = scheduler.getExecutionTimeline()
        val totalTime = timeline.maxOf { it.endTime } - timeline.minOf { it.startTime }

        val sb = StringBuilder()
        sb.append("\n\n\n\n===== Startup Task Performance Report =====")
        sb.append("\nTotal duration: ${totalTime}ms")
        sb.append("\nExecution details:")

        timeline.forEach { info ->
            sb.append("\nTask: ${info.taskId.padEnd(20)} | Thread: ${info.threadName.padEnd(20)} | Duration: ${info.duration}ms")
        }
        sb.append("\n===============================")
        return sb.toString()
    }
}

