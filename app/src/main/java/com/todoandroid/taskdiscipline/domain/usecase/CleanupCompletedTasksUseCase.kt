package com.todoandroid.taskdiscipline.domain.usecase

import com.todoandroid.taskdiscipline.domain.repository.TaskRepository
import com.todoandroid.taskdiscipline.domain.workflow.TaskWorkflowEngine

class CleanupCompletedTasksUseCase(
    private val taskRepository: TaskRepository,
    private val workflowEngine: TaskWorkflowEngine = TaskWorkflowEngine()
) {
    suspend operator fun invoke(): Int {
        val threshold = workflowEngine.getRetentionThresholdTimestamp()
        return taskRepository.deleteCompletedTasksOlderThan(threshold)
    }
}
