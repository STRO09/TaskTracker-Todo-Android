package com.todoandroid.taskdiscipline.domain.usecase

import com.todoandroid.taskdiscipline.domain.model.Task
import com.todoandroid.taskdiscipline.domain.repository.TaskRepository
import com.todoandroid.taskdiscipline.domain.workflow.TaskWorkflowEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SortedBoardTasks(
    val pendingTasks: List<Task>,
    val activeTasks: List<Task>,
    val completedTasks: List<Task>
)

class GetTasksUseCase(
    private val taskRepository: TaskRepository,
    private val workflowEngine: TaskWorkflowEngine = TaskWorkflowEngine()
) {
    operator fun invoke(): Flow<SortedBoardTasks> {
        return taskRepository.observeAllTasks().map { tasks ->
            SortedBoardTasks(
                pendingTasks = workflowEngine.sortPendingTasks(tasks),
                activeTasks = workflowEngine.sortActiveTasks(tasks),
                completedTasks = workflowEngine.sortCompletedTasks(tasks)
            )
        }
    }
}
