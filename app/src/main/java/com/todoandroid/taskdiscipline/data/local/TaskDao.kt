package com.todoandroid.taskdiscipline.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'ACTIVE'")
    suspend fun getActiveTaskCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'ACTIVE'")
    fun observeActiveTaskCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("DELETE FROM tasks WHERE status = 'COMPLETED' AND completedAt IS NOT NULL AND completedAt <= :thresholdTimestamp")
    suspend fun deleteCompletedTasksOlderThan(thresholdTimestamp: Long): Int

    @Query("SELECT * FROM tasks WHERE status = 'PENDING'")
    suspend fun getPendingTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = 'ACTIVE'")
    suspend fun getActiveTasks(): List<TaskEntity>
}
