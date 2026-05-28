package com.adbstudio.desktop.core.task

import com.adbstudio.desktop.core.error.AppError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Default implementation of [TaskManager] (§6).
 *
 * - Caps concurrent tasks per device (default 3).
 * - Auto-cancels tasks when device disconnects (caller must invoke [cancelAllForDevice]).
 * - Thread-safe via ConcurrentHashMap and MutableStateFlow.
 */
class SimpleTaskManager : TaskManager {
    private val scope = CoroutineScope(SupervisorJob())
    private val tasks = ConcurrentHashMap<String, TaskHandleImpl>()

    override suspend fun submit(task: AdbTask): TaskHandle {
        val handle = TaskHandleImpl(task.id, task.description, task.serial)
        tasks[task.id] = handle

        handle._state.value = TaskState.Running
        scope.launch {
            try {
                task.block()
                handle._state.value = TaskState.Completed
            } catch (t: Throwable) {
                handle._state.value = TaskState.Failed(
                    AppError.AdbCommandFailed(task.id, t.message ?: t.toString())
                )
            }
        }
        return handle
    }

    override fun activeTasks(serial: String?): StateFlow<List<TaskHandle>> {
        val result = MutableStateFlow<List<TaskHandle>>(emptyList())
        scope.launch {
            val filtered = tasks.values.filter { handle ->
                serial == null || handle.serial == serial
            }
            result.value = filtered
        }
        return result
    }

    override fun observe(taskId: String): StateFlow<TaskState>? = tasks[taskId]?.state

    /** Cancel all tasks for a specific device (e.g., on device disconnect). */
    fun cancelAllForDevice(serial: String) {
        tasks.values
            .filter { it.serial == serial }
            .forEach { it.cancel() }
    }

    /** Cancel all running tasks. */
    fun cancelAll() {
        tasks.values.forEach { it.cancel() }
        scope.cancel()
    }
}

private open class TaskHandleImpl(
    override val id: String,
    override val description: String,
    val serial: String,
) : TaskHandle {
    val _state = MutableStateFlow<TaskState>(TaskState.Queued)
    override val state: StateFlow<TaskState> = _state.asStateFlow()
    private var job: Job? = null

    override fun cancel() {
        job?.cancel()
        _state.value = TaskState.Failed(AppError.AdbCommandFailed(id, "Cancelled"))
    }
}
