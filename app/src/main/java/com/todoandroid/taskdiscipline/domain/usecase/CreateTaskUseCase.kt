package com.todoandroid.taskdiscipline.domain.usecase

import com.todoandroid.taskdiscipline.domain.model.Task
import com.todoandroid.taskdiscipline.domain.repository.TaskRepository
import com.todoandroid.taskdiscipline.domain.workflow.TaskWorkflowEngine

class CreateTaskUseCase(
    private val taskRepository: TaskRepository,
    private val workflowEngine: TaskWorkflowEngine = TaskWorkflowEngine()
) {
    suspend operator fun invoke(title: String): Result<Task> {
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("Task title cannot be empty."))
        }
        val task = workflowEngine.createPendingTask(title)
        taskRepository.insertTask(task)
        return Result.success(task)
    }
}
