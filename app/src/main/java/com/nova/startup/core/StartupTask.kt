/**
 * Base class for startup tasks
 * @param runOnMainThread Whether to execute on main thread
 * @param dependsOn List of dependent task classes (current task executes after all dependencies complete)
 */
abstract class StartupTask(
    val runOnMainThread: Boolean = false,
    val dependsOn: List<Class<in StartupTask>> = emptyList()
) {
    /** Unique task identifier (default uses class name) */
    open val taskId: String get() = javaClass.simpleName

    /** Execution logic (runs on scheduler-specified thread) */
    abstract fun execute()

    /** Task timeout duration (default 20 seconds) */
    open val timeout: Long = 20_000L

    /** Whether to allow continuing execution after timeout (default true) */
    open val allowContinueOnTimeout: Boolean = true
}