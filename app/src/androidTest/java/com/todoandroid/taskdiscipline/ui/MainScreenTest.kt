package com.todoandroid.taskdiscipline.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.todoandroid.taskdiscipline.domain.model.Task
import com.todoandroid.taskdiscipline.domain.model.TaskStatus
import com.todoandroid.taskdiscipline.domain.repository.TaskRepository
import com.todoandroid.taskdiscipline.ui.main.MainScreen
import com.todoandroid.taskdiscipline.ui.main.MainViewModel
import com.todoandroid.taskdiscipline.ui.theme.TodoAndroidTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

class FakeTaskRepository : TaskRepository {
    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private val wipLimitFlow = MutableStateFlow(2)

    override fun observeAllTasks(): Flow<List<Task>> = tasksFlow
    override fun observeActiveTaskCount(): Flow<Int> = MutableStateFlow(tasksFlow.value.count { it.status == TaskStatus.ACTIVE })
    override fun observeWipLimit(): Flow<Int> = wipLimitFlow
    override suspend fun getTaskById(id: String): Task? = tasksFlow.value.find { it.id == id }
    override suspend fun getActiveTaskCount(): Int = tasksFlow.value.count { it.status == TaskStatus.ACTIVE }

    override suspend fun insertTask(task: Task) {
        tasksFlow.value = tasksFlow.value + task
    }

    override suspend fun updateTask(task: Task) {
        tasksFlow.value = tasksFlow.value.map { if (it.id == task.id) task else it }
    }

    override suspend fun deleteTask(id: String) {
        tasksFlow.value = tasksFlow.value.filterNot { it.id == id }
    }

    override suspend fun setWipLimit(limit: Int) {
        wipLimitFlow.value = limit
    }

    override suspend fun deleteCompletedTasksOlderThan(thresholdTimestamp: Long): Int = 0
    override suspend fun getPendingTasks(): List<Task> = tasksFlow.value.filter { it.status == TaskStatus.PENDING }
    override suspend fun getActiveTasks(): List<Task> = tasksFlow.value.filter { it.status == TaskStatus.ACTIVE }
}

@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testBoardHeadersDisplayed() {
        val fakeRepo = FakeTaskRepository()
        composeTestRule.setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val viewModel = MainViewModel(fakeRepo, context)
            TodoAndroidTheme {
                MainScreen(viewModel = viewModel, onOpenSettings = {})
            }
        }

        composeTestRule.onNodeWithText("PENDING").assertIsDisplayed()
        composeTestRule.onNodeWithText("ACTIVE").assertIsDisplayed()
        composeTestRule.onNodeWithText("COMPLETED").assertIsDisplayed()
    }
}
