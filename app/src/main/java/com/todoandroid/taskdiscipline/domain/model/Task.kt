package com.todoandroid.taskdiscipline.domain.model

import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val status: TaskStatus = TaskStatus.PENDING,
    val createdAt: Long,
    val pendingSince: Long,
    val activeSince: Long? = null,
    val completedAt: Long? = null,
    val lastPendingReminderLevel: Int = 0,
    val lastActiveReminderLevel: Int = 0
)
