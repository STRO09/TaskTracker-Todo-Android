package com.todoandroid.taskdiscipline.data.repository

import com.todoandroid.taskdiscipline.data.local.TaskDao
import com.todoandroid.taskdiscipline.data.local.UserPreferencesRepository
import com.todoandroid.taskdiscipline.data.mapper.toDomain
import com.todoandroid.taskdiscipline.data.mapper.toEntity
import com.todoandroid.taskdiscipline.domain.model.Task
import com.todoandroid.taskdiscipline.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : TaskRepository {

    override fun observeAllTasks(): Flow<List<Task>> {
        return taskDao.observeAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeActiveTaskCount(): Flow<Int> {
        return taskDao.observeActiveTaskCount()
    }

    override fun observeWipLimit(): Flow<Int> {
        return userPreferencesRepository.wipLimitFlow
    }

    override suspend fun getTaskById(id: String): Task? {
        return taskDao.getTaskById(id)?.toDomain()
    }

    override suspend fun getActiveTaskCount(): Int {
        return taskDao.getActiveTaskCount()
    }

    override suspend fun insertTask(task: Task) {
        taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(id: String) {
        taskDao.deleteTask(id)
    }

    override suspend fun setWipLimit(limit: Int) {
        userPreferencesRepository.setWipLimit(limit)
    }

    override suspend fun deleteCompletedTasksOlderThan(thresholdTimestamp: Long): Int {
        return taskDao.deleteCompletedTasksOlderThan(thresholdTimestamp)
    }

    override suspend fun getPendingTasks(): List<Task> {
        return taskDao.getPendingTasks().map { it.toDomain() }
    }

    override suspend fun getActiveTasks(): List<Task> {
        return taskDao.getActiveTasks().map { it.toDomain() }
    }
}
