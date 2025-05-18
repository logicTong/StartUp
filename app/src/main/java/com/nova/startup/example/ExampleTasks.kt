/** 
 * Storage initialization task (no dependencies, runs on background thread)
 */
class StorageInitTask : StartupTask(
    runOnMainThread = false
) {
    override fun execute() {
        // Simulate database initialization
        Thread.sleep(500)
        println("[StorageInitTask] Storage component initialized")
    }
}

/** 
 * Configuration loading task (depends on StorageInitTask, runs on main thread)
 */
class ConfigLoadTask : StartupTask(
    runOnMainThread = true,
    dependsOn = listOf(StorageInitTask::class.java)
) {
    override fun execute() {
        // Simulate loading local configuration
        Thread.sleep(300)
        println("[ConfigLoadTask] Configuration loaded")
    }
}

/** 
 * Network initialization task (depends on ConfigLoadTask, 
 * runs on background thread with timeout handling)
 */
class NetworkInitTask : StartupTask(
    runOnMainThread = false,
    dependsOn = listOf(ConfigLoadTask::class.java)
) {

    override val timeout =3000L

    override fun execute() {
        // Simulate network library initialization (long-running operation)
        Thread.sleep(1500)
        println("[NetworkInitTask] Network component initialized")
    }
}

/** 
 * Logging component initialization task 
 * (executes in parallel with NetworkInitTask)
 */
class LogInitTask : StartupTask(
    runOnMainThread = false,
    dependsOn = listOf(StorageInitTask::class.java)
) {
    override fun execute() {
        // Simulate logging library initialization
        Thread.sleep(400)
        println("[LogInitTask] Logging component initialized")
    }
}