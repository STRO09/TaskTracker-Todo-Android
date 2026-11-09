package com.todoandroid.taskdiscipline.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.todoandroid.taskdiscipline.domain.usecase.SetWipLimitUseCase
import com.todoandroid.taskdiscipline.domain.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val taskRepository: TaskRepository,
    private val setWipLimitUseCase: SetWipLimitUseCase = SetWipLimitUseCase(taskRepository)
) : ViewModel() {

    val wipLimit: StateFlow<Int> = taskRepository.observeWipLimit()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 2
        )

    fun onWipLimitSelected(limit: Int) {
        viewModelScope.launch {
            setWipLimitUseCase(limit)
        }
    }
}

class SettingsViewModelFactory(
    private val taskRepository: TaskRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(taskRepository) as T
    }
}
