package com.todoandroid.taskdiscipline.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.todoandroid.taskdiscipline.domain.model.Task
import com.todoandroid.taskdiscipline.domain.model.TaskAgeCategory
import com.todoandroid.taskdiscipline.domain.model.TaskStatus
import com.todoandroid.taskdiscipline.domain.repository.TaskRepository
import com.todoandroid.taskdiscipline.domain.usecase.*
import com.todoandroid.taskdiscipline.domain.workflow.TaskWorkflowEngine
import com.todoandroid.taskdiscipline.domain.workflow.TransitionResult
import com.todoandroid.taskdiscipline.worker.CompletedTaskCleanupWorker
import com.todoandroid.taskdiscipline.worker.TaskAgingWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class MainUiState(
    val pendingTasks: List<Task> = emptyList(),
    val activeTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
    val wipLimit: Int = 2,
    val activeCount: Int = 0,
    val isCreateTaskDialogVisible: Boolean = false,
    val userMessage: String? = null
)

class MainViewModel(
    private val taskRepository: TaskRepository,
    private val context: Context,
    val workflowEngine: TaskWorkflowEngine = TaskWorkflowEngine()
) : ViewModel() {

    private val getTasksUseCase = GetTasksUseCase(taskRepository, workflowEngine)
    private val createTaskUseCase = CreateTaskUseCase(taskRepository, workflowEngine)
    private val activateTaskUseCase = ActivateTaskUseCase(taskRepository, workflowEngine)
    private val completeTaskUseCase = CompleteTaskUseCase(taskRepository, workflowEngine)
    private val cleanupCompletedTasksUseCase = CleanupCompletedTasksUseCase(taskRepository, workflowEngine)

    private val _isCreateDialogVisible = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MainUiState> = combine(
        getTasksUseCase(),
        taskRepository.observeWipLimit(),
        _isCreateDialogVisible,
        _userMessage
    ) { boardTasks, wipLimit, isDialogVisible, message ->
        MainUiState(
            pendingTasks = boardTasks.pendingTasks,
            activeTasks = boardTasks.activeTasks,
            completedTasks = boardTasks.completedTasks,
            wipLimit = wipLimit,
            activeCount = boardTasks.activeTasks.size,
            isCreateTaskDialogVisible = isDialogVisible,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    init {
        // Startup reconciliation: Delete completed tasks older than 72 hours
        viewModelScope.launch {
            cleanupCompletedTasksUseCase()
        }
        // Schedule WorkManager periodic tasks
        scheduleBackgroundWorkers()
    }

    private fun scheduleBackgroundWorkers() {
        val workManager = WorkManager.getInstance(context)

        val agingRequest = PeriodicWorkRequestBuilder<TaskAgingWorker>(6, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(
            "TaskAgingWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            agingRequest
        )

        val cleanupRequest = PeriodicWorkRequestBuilder<CompletedTaskCleanupWorker>(6, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(
            "CompletedTaskCleanupWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }

    fun showCreateTaskDialog() {
        _isCreateDialogVisible.value = true
    }

    fun hideCreateTaskDialog() {
        _isCreateDialogVisible.value = false
    }

    fun createNewTask(title: String) {
        viewModelScope.launch {
            createTaskUseCase(title)
        }
    }

    fun activateTask(taskId: String) {
        viewModelScope.launch {
            val result = activateTaskUseCase(taskId)
            if (result is TransitionResult.Forbidden) {
                _userMessage.value = result.reason
            }
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            val result = completeTaskUseCase(taskId)
            if (result is TransitionResult.Forbidden) {
                _userMessage.value = result.reason
            }
        }
    }

    fun handleTaskDrop(task: Task, targetStatus: TaskStatus) {
        when (targetStatus) {
            TaskStatus.ACTIVE -> activateTask(task.id)
            TaskStatus.COMPLETED -> completeTask(task.id)
            TaskStatus.PENDING -> {
                _userMessage.value = "Cannot move tasks back to PENDING."
            }
        }
    }

    fun showUserMessage(message: String) {
        _userMessage.value = message
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun getPendingAgeCategory(pendingSince: Long): TaskAgeCategory {
        return workflowEngine.getPendingAgeCategory(pendingSince)
    }

    fun getActiveAgeCategory(activeSince: Long?): TaskAgeCategory {
        return workflowEngine.getActiveAgeCategory(activeSince)
    }

    fun formatAgeLabel(sinceTimestamp: Long?): String {
        if (sinceTimestamp == null) return "0m"
        val now = System.currentTimeMillis()
        val ageMillis = (now - sinceTimestamp).coerceAtLeast(0)

        val hours = ageMillis / (1000 * 60 * 60)
        val days = hours / 24

        return when {
            days > 0 -> "${days}d ${hours % 24}h"
            hours > 0 -> "${hours}h"
            else -> "<1h"
        }
    }
}

class MainViewModelFactory(
    private val taskRepository: TaskRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(MainViewModel::class.java)) {
            throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}"
            )
        }

        @Suppress("UNCHECKED_CAST")
        return MainViewModel(taskRepository, context) as T
    }
}