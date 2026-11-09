package com.todoandroid.taskdiscipline.domain.repository

import com.todoandroid.taskdiscipline.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeAllTasks(): Flow<List<Task>>
    fun observeActiveTaskCount(): Flow<Int>
    fun observeWipLimit(): Flow<Int>
    suspend fun getTaskById(id: String): Task?
    suspend fun getActiveTaskCount(): Int
    suspend fun insertTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(id: String)
    suspend fun setWipLimit(limit: Int)
    suspend fun deleteCompletedTasksOlderThan(thresholdTimestamp: Long): Int
    suspend fun getPendingTasks(): List<Task>
    suspend fun getActiveTasks(): List<Task>
}
