package com.todoandroid.taskdiscipline.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.todoandroid.taskdiscipline.data.local.TaskDatabase
import com.todoandroid.taskdiscipline.data.local.UserPreferencesRepository
import com.todoandroid.taskdiscipline.data.repository.TaskRepositoryImpl
import com.todoandroid.taskdiscipline.domain.usecase.CleanupCompletedTasksUseCase

class CompletedTaskCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = TaskDatabase.getInstance(applicationContext)
        val userPrefs = UserPreferencesRepository(applicationContext)
        val repository = TaskRepositoryImpl(database.taskDao(), userPrefs)
        val cleanupUseCase = CleanupCompletedTasksUseCase(repository)

        cleanupUseCase()
        return Result.success()
    }
}
