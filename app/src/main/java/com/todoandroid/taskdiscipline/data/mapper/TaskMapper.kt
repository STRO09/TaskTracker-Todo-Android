package com.todoandroid.taskdiscipline.data.mapper

import com.todoandroid.taskdiscipline.data.local.TaskEntity
import com.todoandroid.taskdiscipline.domain.model.Task
import com.todoandroid.taskdiscipline.domain.model.TaskStatus

fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        title = title,
        status = TaskStatus.valueOf(status),
        createdAt = createdAt,
        pendingSince = pendingSince,
        activeSince = activeSince,
        completedAt = completedAt,
        lastPendingReminderLevel = lastPendingReminderLevel,
        lastActiveReminderLevel = lastActiveReminderLevel
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        status = status.name,
        createdAt = createdAt,
        pendingSince = pendingSince,
        activeSince = activeSince,
        completedAt = completedAt,
        lastPendingReminderLevel = lastPendingReminderLevel,
        lastActiveReminderLevel = lastActiveReminderLevel
    )
}
