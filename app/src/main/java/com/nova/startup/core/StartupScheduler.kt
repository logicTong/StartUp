package com.nova.startup.core


import StartupTask
import StartupTask.TaskStatus
import TaskTimeInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Startup task scheduler with dependency management and parallel execution
 * @param mainHandler Handler for main thread operations
 * @param workerPool Thread pool for background tasks
 */
class StartupScheduler(
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val workerPool: ExecutorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors() * 2
    )
) {
    private val TAG = "StartupScheduler"

    private val pendingTasks = mutableListOf<StartupTask>()
    private val taskMap = mutableMapOf<String, StartupTask>()
    private val dependencyGraph = ConcurrentHashMap<String, MutableSet<String>>()
    private val reverseDependencyGraph = ConcurrentHashMap<String, MutableSet<String>>()
    private val taskTimeRecords = ConcurrentHashMap<String, TaskTimeInfo>()
    private val isRegistered = AtomicBoolean(false)


    /**
     * Get all task time records
     * @return List of TaskTimeInfo objects
     */
    fun getTaskTimeRecords(): List<TaskTimeInfo> = taskTimeRecords.values.toList()

    /**
     * Get execution timeline sorted by start time
     * @return Chronologically ordered list of tasks
     */
    fun getExecutionTimeline(): List<TaskTimeInfo> = taskTimeRecords.values
        .sortedBy { it.startTime }

    /**
     * Register startup tasks
     * @param tasks Tasks to register
     * @throws IllegalArgumentException When circular dependencies are detected
     */
    fun registerTasks(vararg tasks: StartupTask) {
        if (isRegistered.getAndSet(true)) {
            Log.e(
                TAG,
                "registerTasks: ",
                IllegalStateException("Tasks have already been registered")
            )
            return
        }
        pendingTasks.addAll(tasks)
        tasks.forEach { task ->
            taskMap[task.taskId] = task
        }
        buildDependencyGraph()
        checkForCircularDependencies()
    }

    /** Start executing all registered tasks */
    fun start() {
        val entryTasks = findEntryTasks()
        entryTasks.forEach { executeTask(it) }
    }

    private fun buildDependencyGraph() {
        dependencyGraph.clear()
        reverseDependencyGraph.clear()
        taskMap.forEach { (taskId, task) ->
            val dependencies = ConcurrentHashMap.newKeySet<String>().apply {
                addAll(task.dependsOn.map { it.simpleName })
            }
            dependencyGraph[taskId] = dependencies
            dependencies.forEach { depId ->
                reverseDependencyGraph.getOrPut(depId) { mutableSetOf() }.add(taskId)
            }
        }
    }

    private fun checkForCircularDependencies() {
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()

        fun hasCycle(node: String): Boolean {
            if (node in recursionStack) return true
            if (node in visited) return false

            visited.add(node)
            recursionStack.add(node)

            dependencyGraph[node]?.forEach { neighbor ->
                if (hasCycle(neighbor)) return true
            }

            recursionStack.remove(node)
            return false
        }

        taskMap.keys.forEach {
            if (hasCycle(it)) {
                throw IllegalArgumentException("Circular dependency detected: Task chain contains a cycle")
            }
        }
    }

    private fun findEntryTasks(): List<String> {
        return taskMap.keys.filter { taskId ->
            dependencyGraph[taskId]?.isEmpty() ?: true
        }
    }

    private fun executeTask(taskId: String) {
        val task = taskMap[taskId] ?: return
        if (task.taskStatus != TaskStatus.PENDING) return

        task.taskStatus = TaskStatus.RUNNING

        val executor = if (task.runOnMainThread) {
            { mainHandler.post { executeTaskInternal(task, taskId) } }
        } else {
            { workerPool.submit { executeTaskInternal(task, taskId) } }
        }

        executor.invoke()
    }

    private fun executeTaskInternal(task: StartupTask, taskId: String) {
        val startTime = System.currentTimeMillis()
        val threadName = Thread.currentThread().name
        var endTime = startTime
        var isTimeout = false

        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
//                isTimeout = true
//                endTime = System.currentTimeMillis()
//                taskMap[taskId]?.taskStatus = TaskStatus.TIMEOUT
//                recordTaskTime(taskId, startTime, endTime, threadName)
//                handleTaskCompletion(taskId, isTimeout)
            }
        }, task.timeout)

        try {
            task.execute()
            if (!isTimeout) {
                endTime = System.currentTimeMillis()
                taskMap[taskId]?.taskStatus = TaskStatus.SUCCEEDED
            }
        } catch (e: Exception) {
            endTime = System.currentTimeMillis()
            taskMap[taskId]?.taskStatus = TaskStatus.FAILED
            e.printStackTrace()
        } finally {
            if (!isTimeout) {
                recordTaskTime(taskId, startTime, endTime, threadName)
                handleTaskCompletion(taskId, isTimeout)
            }
            timer.cancel()
        }
    }

    private fun recordTaskTime(
        taskId: String,
        startTime: Long,
        endTime: Long,
        threadName: String
    ) {
        taskTimeRecords[taskId] = TaskTimeInfo(
            taskId = taskId,
            startTime = startTime,
            endTime = endTime,
            threadName = threadName
        )
    }

    private fun handleTaskCompletion(taskId: String, isTimeout: Boolean) {
        reverseDependencyGraph[taskId]?.forEach { dependentTaskId ->
            val dependencies = dependencyGraph[dependentTaskId] ?: return@forEach
            dependencies.remove(taskId)
            if (dependencies.isEmpty()) {
                val dependentTask = taskMap[dependentTaskId] ?: return@forEach
                if (!isTimeout || dependentTask.allowContinueOnTimeout) {
                    executeTask(dependentTaskId)
                }
            }
        }
        synchronized(StartupScheduler::class) {
            pendingTasks.remove(taskMap[taskId])
            TaskCompleteNotifier.notifyTaskComplete(this, taskMap[taskId]!!)
            if (pendingTasks.isEmpty()) {
                TaskCompleteNotifier.notifyAllTaskComplete(this)
                mainHandler.postDelayed({ shutdown() }, 600)
            }
        }
    }


    /** Close scheduler (call on app exit) */
    fun shutdown() {
        if (workerPool.isShutdown) {
            return
        }
        workerPool.shutdown()
        try {
            if (!workerPool.awaitTermination(1, TimeUnit.SECONDS)) {
                workerPool.shutdownNow()
            }
        } catch (e: InterruptedException) {
            workerPool.shutdownNow()
        }
    }
}
    