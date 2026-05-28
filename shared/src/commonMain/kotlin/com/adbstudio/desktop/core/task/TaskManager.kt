package com.adbstudio.desktop.core.task

/**
 * State of a long-running ADB task (§6).
 */
sealed interface TaskState {
    data object Queued : TaskState
    data object Running : TaskState
    data class Progress(val percent: Float, val message: String = "") : TaskState
    data object Completed : TaskState
    data class Failed(val error: com.adbstudio.desktop.core.error.AppError) : TaskState
}

/**
 * Represents a long-running ADB operation (install, pull, push, screenrecord, bugreport).
 * Created by [TaskManager.submit].
 */
data class AdbTask(
    val id: String,
    val description: String,
    val serial: String,
    val block: suspend () -> Unit,
)

/**
 * Handle to observe and control a running task.
 */
interface TaskHandle {
    val id: String
    val description: String
    val state: kotlinx.coroutines.flow.StateFlow<TaskState>
    fun cancel()
}

/**
 * Central manager for long-running ADB operations (§6).
 *
 * - Every long-running ADB operation goes through TaskManager.
 * - Tasks auto-cancel when the target device disconnects.
 * - UI shows task progress in a status bar or notification center.
 * - Caps concurrent tasks per device (default 3).
 */
interface TaskManager {
    /** Submit a new task. Returns a handle for observation/cancellation. */
    suspend fun submit(task: AdbTask): TaskHandle

    /** All active tasks for a device, or all devices if serial is null. */
    fun activeTasks(serial: String?): kotlinx.coroutines.flow.Flow<List<TaskHandle>>

    /** Observe a specific task's state. */
    fun observe(taskId: String): kotlinx.coroutines.flow.StateFlow<TaskState>?
}
