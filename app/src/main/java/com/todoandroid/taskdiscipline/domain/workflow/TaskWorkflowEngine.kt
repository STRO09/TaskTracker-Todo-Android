package com.todoandroid.taskdiscipline.domain.workflow

import com.todoandroid.taskdiscipline.domain.model.Task
import com.todoandroid.taskdiscipline.domain.model.TaskAgeCategory
import com.todoandroid.taskdiscipline.domain.model.TaskStatus
import java.util.UUID

class TaskWorkflowEngine(
    private val timeProvider: TimeProvider = SystemTimeProvider()
) {

    fun validateTransition(
        task: Task,
        targetStatus: TaskStatus,
        currentActiveCount: Int,
        wipLimit: Int
    ): TransitionResult {
        if (task.status == targetStatus) {
            return TransitionResult.Forbidden("Task is already in ${targetStatus.name}")
        }

        val clampedWipLimit = wipLimit.coerceIn(1, 3)

        return when (task.status to targetStatus) {
            TaskStatus.PENDING to TaskStatus.ACTIVE -> {
                if (currentActiveCount >= clampedWipLimit) {
                    TransitionResult.Forbidden("Active limit reached ($currentActiveCount/$clampedWipLimit). Finish an active task first.")
                } else {
                    TransitionResult.Allowed
                }
            }
            TaskStatus.ACTIVE to TaskStatus.COMPLETED -> TransitionResult.Allowed

            TaskStatus.PENDING to TaskStatus.COMPLETED ->
                TransitionResult.Forbidden("New/Pending tasks cannot bypass ACTIVE status.")
            TaskStatus.ACTIVE to TaskStatus.PENDING ->
                TransitionResult.Forbidden("Active tasks cannot be moved back to PENDING.")
            TaskStatus.COMPLETED to TaskStatus.ACTIVE ->
                TransitionResult.Forbidden("Completed tasks cannot be re-activated.")
            TaskStatus.COMPLETED to TaskStatus.PENDING ->
                TransitionResult.Forbidden("Completed tasks cannot be restored to PENDING.")
            else ->
                TransitionResult.Forbidden("Invalid workflow transition.")
        }
    }

    fun createPendingTask(title: String): Task {
        val now = timeProvider.currentTimeMillis()
        return Task(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            status = TaskStatus.PENDING,
            createdAt = now,
            pendingSince = now,
            activeSince = null,
            completedAt = null
        )
    }

    fun activateTask(
        task: Task,
        currentActiveCount: Int,
        wipLimit: Int
    ): Pair<Task?, TransitionResult> {
        val validation = validateTransition(task, TaskStatus.ACTIVE, currentActiveCount, wipLimit)
        if (validation is TransitionResult.Forbidden) {
            return Pair(null, validation)
        }
        val now = timeProvider.currentTimeMillis()
        val activatedTask = task.copy(
            status = TaskStatus.ACTIVE,
            activeSince = now
        )
        return Pair(activatedTask, TransitionResult.Allowed)
    }

    fun completeTask(task: Task): Pair<Task?, TransitionResult> {
        val validation = validateTransition(task, TaskStatus.COMPLETED, 0, 3)
        if (validation is TransitionResult.Forbidden) {
            return Pair(null, validation)
        }
        val now = timeProvider.currentTimeMillis()
        val completedTask = task.copy(
            status = TaskStatus.COMPLETED,
            completedAt = now
        )
        return Pair(completedTask, TransitionResult.Allowed)
    }

    fun getPendingAgeCategory(pendingSince: Long): TaskAgeCategory {
        val ageMillis = (timeProvider.currentTimeMillis() - pendingSince).coerceAtLeast(0)
        val ageDays = ageMillis / (1000 * 60 * 60 * 24.0)

        return when {
            ageDays >= 7.0 -> TaskAgeCategory.STRONG_WARNING
            ageDays >= 3.0 -> TaskAgeCategory.WARNING
            ageDays >= 1.0 -> TaskAgeCategory.SLIGHTLY_EMPHASIZED
            else -> TaskAgeCategory.NORMAL
        }
    }

    fun getActiveAgeCategory(activeSince: Long?): TaskAgeCategory {
        if (activeSince == null) return TaskAgeCategory.NORMAL
        val ageMillis = (timeProvider.currentTimeMillis() - activeSince).coerceAtLeast(0)
        val ageDays = ageMillis / (1000 * 60 * 60 * 24.0)

        return when {
            ageDays >= 5.0 -> TaskAgeCategory.STRONG_WARNING
            ageDays >= 2.0 -> TaskAgeCategory.WARNING
            else -> TaskAgeCategory.NORMAL
        }
    }

    fun isCompletedTaskEligibleForDeletion(completedAt: Long?): Boolean {
        if (completedAt == null) return false
        val ageMillis = timeProvider.currentTimeMillis() - completedAt
        val seventyTwoHoursMillis = 72 * 60 * 60 * 1000L
        return ageMillis >= seventyTwoHoursMillis
    }

    fun getRetentionThresholdTimestamp(): Long {
        val seventyTwoHoursMillis = 72 * 60 * 60 * 1000L
        return timeProvider.currentTimeMillis() - seventyTwoHoursMillis
    }

    fun sortPendingTasks(tasks: List<Task>): List<Task> {
        return tasks.filter { it.status == TaskStatus.PENDING }
            .sortedBy { it.pendingSince } // Oldest pending first
    }

    fun sortActiveTasks(tasks: List<Task>): List<Task> {
        return tasks.filter { it.status == TaskStatus.ACTIVE }
            .sortedBy { it.activeSince ?: it.pendingSince } // Oldest active first
    }

    fun sortCompletedTasks(tasks: List<Task>): List<Task> {
        return tasks.filter { it.status == TaskStatus.COMPLETED }
            .sortedByDescending { it.completedAt ?: 0L } // Newest completed first
    }
}
