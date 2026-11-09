package com.todoandroid.taskdiscipline.domain.usecase

import com.todoandroid.taskdiscipline.domain.repository.TaskRepository

class SetWipLimitUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(limit: Int) {
        val validLimit = limit.coerceIn(1, 3)
        taskRepository.setWipLimit(validLimit)
    }
}
