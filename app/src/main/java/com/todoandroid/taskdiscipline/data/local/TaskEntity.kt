package com.todoandroid.taskdiscipline.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["status", "pendingSince"]),
        Index(value = ["status", "completedAt"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val status: String,
    val createdAt: Long,
    val pendingSince: Long,
    val activeSince: Long?,
    val completedAt: Long?,
    val lastPendingReminderLevel: Int = 0,
    val lastActiveReminderLevel: Int = 0
)
