package com.todoandroid.taskdiscipline.domain.usecase

import com.todoandroid.taskdiscipline.domain.repository.TaskRepository
import com.todoandroid.taskdiscipline.domain.workflow.TaskWorkflowEngine
import com.todoandroid.taskdiscipline.domain.workflow.TransitionResult

class CompleteTaskUseCase(
    private val taskRepository: TaskRepository,
    private val workflowEngine: TaskWorkflowEngine = TaskWorkflowEngine()
) {
    suspend operator fun invoke(taskId: String): TransitionResult {
        val task = taskRepository.getTaskById(taskId)
            ?: return TransitionResult.Forbidden("Task not found.")

        val (completedTask, result) = workflowEngine.completeTask(task)

        if (result is TransitionResult.Allowed && completedTask != null) {
            taskRepository.updateTask(completedTask)
        }
        return result
    }
}
