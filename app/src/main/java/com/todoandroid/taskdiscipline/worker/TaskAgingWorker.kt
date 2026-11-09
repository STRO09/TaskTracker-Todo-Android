package com.todoandroid.taskdiscipline.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.todoandroid.taskdiscipline.data.local.TaskDatabase
import com.todoandroid.taskdiscipline.data.local.UserPreferencesRepository
import com.todoandroid.taskdiscipline.data.mapper.toDomain
import com.todoandroid.taskdiscipline.data.repository.TaskRepositoryImpl
import com.todoandroid.taskdiscipline.domain.workflow.SystemTimeProvider

class TaskAgingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = TaskDatabase.getInstance(applicationContext)
        val userPrefs = UserPreferencesRepository(applicationContext)
        val repository = TaskRepositoryImpl(database.taskDao(), userPrefs)
        val notificationHelper = NotificationHelper(applicationContext)
        val now = SystemTimeProvider().currentTimeMillis()

        // 1. Pending tasks aging reminders (1 day, 3 days, 7 days)
        val pendingTasks = repository.getPendingTasks()
        for (task in pendingTasks) {
            val ageDays = ((now - task.pendingSince) / (1000 * 60 * 60 * 24.0)).toInt()
            when {
                ageDays >= 7 && task.lastPendingReminderLevel < 3 -> {
                    notificationHelper.sendPendingReminder(task.id, task.title, 7)
                    repository.updateTask(task.copy(lastPendingReminderLevel = 3))
                }
                ageDays >= 3 && task.lastPendingReminderLevel < 2 -> {
                    notificationHelper.sendPendingReminder(task.id, task.title, 3)
                    repository.updateTask(task.copy(lastPendingReminderLevel = 2))
                }
                ageDays >= 1 && task.lastPendingReminderLevel < 1 -> {
                    notificationHelper.sendPendingReminder(task.id, task.title, 1)
                    repository.updateTask(task.copy(lastPendingReminderLevel = 1))
                }
            }
        }

        // 2. Active tasks aging reminders (2 days, 5 days)
        val activeTasks = repository.getActiveTasks()
        for (task in activeTasks) {
            val activeSince = task.activeSince ?: continue
            val ageDays = ((now - activeSince) / (1000 * 60 * 60 * 24.0)).toInt()
            when {
                ageDays >= 5 && task.lastActiveReminderLevel < 2 -> {
                    notificationHelper.sendActiveReminder(task.id, task.title, 5)
                    repository.updateTask(task.copy(lastActiveReminderLevel = 2))
                }
                ageDays >= 2 && task.lastActiveReminderLevel < 1 -> {
                    notificationHelper.sendActiveReminder(task.id, task.title, 2)
                    repository.updateTask(task.copy(lastActiveReminderLevel = 1))
                }
            }
        }

        return Result.success()
    }
}
