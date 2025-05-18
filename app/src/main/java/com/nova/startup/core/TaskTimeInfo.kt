/**
 * Data class for task timing information
 * @param taskId Unique task identifier
 * @param startTime Task start timestamp (millis)
 * @param endTime Task completion timestamp (millis)
 * @param threadName Name of the execution thread
 */
data class TaskTimeInfo(
    val taskId: String,
    val startTime: Long,
    val endTime: Long,
    val threadName: String
) {
    /** Task execution duration in milliseconds */
    val duration: Long get() = endTime - startTime
}