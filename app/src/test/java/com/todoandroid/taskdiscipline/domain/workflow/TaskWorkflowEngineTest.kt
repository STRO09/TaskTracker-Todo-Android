package com.todoandroid.taskdiscipline.domain.workflow

import com.todoandroid.taskdiscipline.domain.model.Task
import com.todoandroid.taskdiscipline.domain.model.TaskAgeCategory
import com.todoandroid.taskdiscipline.domain.model.TaskStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskWorkflowEngineTest {

    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var workflowEngine: TaskWorkflowEngine

    @Before
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider(nowMillis = 1_000_000_000_000L)
        workflowEngine = TaskWorkflowEngine(fakeTimeProvider)
    }

    // 1. New task starts as PENDING
    @Test
    fun test1_newTaskStartsWithPendingStatus() {
        val task = workflowEngine.createPendingTask("Test Task")
        assertEquals(TaskStatus.PENDING, task.status)
        assertEquals(fakeTimeProvider.currentTimeMillis(), task.createdAt)
        assertEquals(fakeTimeProvider.currentTimeMillis(), task.pendingSince)
        assertNull(task.activeSince)
        assertNull(task.completedAt)
    }

    // 2. PENDING → ACTIVE succeeds
    @Test
    fun test2_pendingToActiveSucceeds() {
        val pendingTask = workflowEngine.createPendingTask("Pending Task")
        val (activatedTask, result) = workflowEngine.activateTask(
            task = pendingTask,
            currentActiveCount = 0,
            wipLimit = 2
        )
        assertTrue(result is TransitionResult.Allowed)
        assertNotNull(activatedTask)
        assertEquals(TaskStatus.ACTIVE, activatedTask!!.status)
        assertEquals(fakeTimeProvider.currentTimeMillis(), activatedTask.activeSince)
    }

    // 3. ACTIVE → COMPLETED succeeds
    @Test
    fun test3_activeToCompletedSucceeds() {
        val pendingTask = workflowEngine.createPendingTask("Task")
        val (activeTask, _) = workflowEngine.activateTask(pendingTask, 0, 2)
        assertNotNull(activeTask)

        fakeTimeProvider.advanceTimeByHours(1)
        val (completedTask, result) = workflowEngine.completeTask(activeTask!!)

        assertTrue(result is TransitionResult.Allowed)
        assertNotNull(completedTask)
        assertEquals(TaskStatus.COMPLETED, completedTask!!.status)
        assertEquals(fakeTimeProvider.currentTimeMillis(), completedTask.completedAt)
    }

    // 4. ACTIVE → PENDING fails
    @Test
    fun test4_activeToPendingFails() {
        val activeTask = Task(
            title = "Active",
            status = TaskStatus.ACTIVE,
            createdAt = 1000L,
            pendingSince = 1000L,
            activeSince = 2000L
        )
        val result = workflowEngine.validateTransition(activeTask, TaskStatus.PENDING, 1, 2)
        assertTrue(result is TransitionResult.Forbidden)
        assertEquals("Active tasks cannot be moved back to PENDING.", (result as TransitionResult.Forbidden).reason)
    }

    // 5. PENDING → COMPLETED fails
    @Test
    fun test5_pendingToCompletedFails() {
        val pendingTask = workflowEngine.createPendingTask("Pending")
        val result = workflowEngine.validateTransition(pendingTask, TaskStatus.COMPLETED, 0, 2)
        assertTrue(result is TransitionResult.Forbidden)
    }

    // 6. COMPLETED → ACTIVE fails
    @Test
    fun test6_completedToActiveFails() {
        val completedTask = Task(
            title = "Completed",
            status = TaskStatus.COMPLETED,
            createdAt = 1000L,
            pendingSince = 1000L,
            activeSince = 2000L,
            completedAt = 3000L
        )
        val result = workflowEngine.validateTransition(completedTask, TaskStatus.ACTIVE, 0, 2)
        assertTrue(result is TransitionResult.Forbidden)
    }

    // 7. COMPLETED → PENDING fails
    @Test
    fun test7_completedToPendingFails() {
        val completedTask = Task(
            title = "Completed",
            status = TaskStatus.COMPLETED,
            createdAt = 1000L,
            pendingSince = 1000L,
            activeSince = 2000L,
            completedAt = 3000L
        )
        val result = workflowEngine.validateTransition(completedTask, TaskStatus.PENDING, 0, 2)
        assertTrue(result is TransitionResult.Forbidden)
    }

    // 8. WIP limit of 1 is enforced
    @Test
    fun test8_wipLimitOf1Enforced() {
        val task1 = workflowEngine.createPendingTask("Task 1")
        val task2 = workflowEngine.createPendingTask("Task 2")

        val (_, result1) = workflowEngine.activateTask(task1, currentActiveCount = 0, wipLimit = 1)
        assertTrue(result1 is TransitionResult.Allowed)

        val (_, result2) = workflowEngine.activateTask(task2, currentActiveCount = 1, wipLimit = 1)
        assertTrue(result2 is TransitionResult.Forbidden)
    }

    // 9. WIP limit of 2 is enforced
    @Test
    fun test9_wipLimitOf2Enforced() {
        val task1 = workflowEngine.createPendingTask("Task 1")

        val (_, result1) = workflowEngine.activateTask(task1, currentActiveCount = 1, wipLimit = 2)
        assertTrue(result1 is TransitionResult.Allowed)

        val (_, result2) = workflowEngine.activateTask(task1, currentActiveCount = 2, wipLimit = 2)
        assertTrue(result2 is TransitionResult.Forbidden)
    }

    // 10. WIP limit of 3 is enforced
    @Test
    fun test10_wipLimitOf3Enforced() {
        val task1 = workflowEngine.createPendingTask("Task 1")

        val (_, result1) = workflowEngine.activateTask(task1, currentActiveCount = 2, wipLimit = 3)
        assertTrue(result1 is TransitionResult.Allowed)

        val (_, result2) = workflowEngine.activateTask(task1, currentActiveCount = 3, wipLimit = 3)
        assertTrue(result2 is TransitionResult.Forbidden)
    }

    // 11. Cannot exceed configured WIP limit
    @Test
    fun test11_cannotExceedConfiguredWipLimit() {
        val task = workflowEngine.createPendingTask("Task")
        val result = workflowEngine.validateTransition(task, TaskStatus.ACTIVE, currentActiveCount = 2, wipLimit = 2)
        assertTrue(result is TransitionResult.Forbidden)
    }

    // 12. Pending tasks sort oldest-first
    @Test
    fun test12_pendingTasksSortOldestFirst() {
        val baseTime = fakeTimeProvider.currentTimeMillis()
        val taskOldest = Task(title = "Oldest", status = TaskStatus.PENDING, createdAt = baseTime, pendingSince = baseTime)
        val taskMiddle = Task(title = "Middle", status = TaskStatus.PENDING, createdAt = baseTime + 100, pendingSince = baseTime + 100)
        val taskNewest = Task(title = "Newest", status = TaskStatus.PENDING, createdAt = baseTime + 200, pendingSince = baseTime + 200)

        val unsorted = listOf(taskMiddle, taskNewest, taskOldest)
        val sorted = workflowEngine.sortPendingTasks(unsorted)

        assertEquals(listOf(taskOldest, taskMiddle, taskNewest), sorted)
    }

    // 13. Active tasks sort oldest-first
    @Test
    fun test13_activeTasksSortOldestFirst() {
        val baseTime = fakeTimeProvider.currentTimeMillis()
        val taskActiveOld = Task(title = "Active 1", status = TaskStatus.ACTIVE, createdAt = baseTime, pendingSince = baseTime, activeSince = baseTime)
        val taskActiveNew = Task(title = "Active 2", status = TaskStatus.ACTIVE, createdAt = baseTime, pendingSince = baseTime, activeSince = baseTime + 500)

        val unsorted = listOf(taskActiveNew, taskActiveOld)
        val sorted = workflowEngine.sortActiveTasks(unsorted)

        assertEquals(listOf(taskActiveOld, taskActiveNew), sorted)
    }

    // 14. Completed tasks sort newest-first
    @Test
    fun test14_completedTasksSortNewestFirst() {
        val baseTime = fakeTimeProvider.currentTimeMillis()
        val taskCompOld = Task(title = "Comp Old", status = TaskStatus.COMPLETED, createdAt = baseTime, pendingSince = baseTime, completedAt = baseTime)
        val taskCompNew = Task(title = "Comp New", status = TaskStatus.COMPLETED, createdAt = baseTime, pendingSince = baseTime, completedAt = baseTime + 1000)

        val unsorted = listOf(taskCompOld, taskCompNew)
        val sorted = workflowEngine.sortCompletedTasks(unsorted)

        assertEquals(listOf(taskCompNew, taskCompOld), sorted)
    }

    // 15. Completed task is not deleted before 72 hours
    @Test
    fun test15_completedTaskNotDeletedBefore72Hours() {
        val completionTime = fakeTimeProvider.currentTimeMillis()
        fakeTimeProvider.advanceTimeByHours(71) // 71 hours elapsed

        val isEligible = workflowEngine.isCompletedTaskEligibleForDeletion(completionTime)
        assertFalse(isEligible)
    }

    // 16. Completed task becomes eligible for deletion after 72 hours
    @Test
    fun test16_completedTaskEligibleForDeletionAfter72Hours() {
        val completionTime = fakeTimeProvider.currentTimeMillis()
        fakeTimeProvider.advanceTimeByHours(72) // Exactly 72 hours

        val isEligible = workflowEngine.isCompletedTaskEligibleForDeletion(completionTime)
        assertTrue(isEligible)
    }

    // 17. Pending aging thresholds work correctly
    @Test
    fun test17_pendingAgingThresholds() {
        val pendingSince = fakeTimeProvider.currentTimeMillis()

        // 0-1 day -> NORMAL
        assertEquals(TaskAgeCategory.NORMAL, workflowEngine.getPendingAgeCategory(pendingSince))

        // 1.5 days -> SLIGHTLY_EMPHASIZED
        fakeTimeProvider.advanceTimeByDays(1)
        fakeTimeProvider.advanceTimeByHours(12)
        assertEquals(TaskAgeCategory.SLIGHTLY_EMPHASIZED, workflowEngine.getPendingAgeCategory(pendingSince))

        // 4 days -> WARNING
        fakeTimeProvider.advanceTimeByDays(3)
        assertEquals(TaskAgeCategory.WARNING, workflowEngine.getPendingAgeCategory(pendingSince))

        // 8 days -> STRONG_WARNING
        fakeTimeProvider.advanceTimeByDays(4)
        assertEquals(TaskAgeCategory.STRONG_WARNING, workflowEngine.getPendingAgeCategory(pendingSince))
    }

    // 18. Active aging thresholds work correctly
    @Test
    fun test18_activeAgingThresholds() {
        val activeSince = fakeTimeProvider.currentTimeMillis()

        // 1 day -> NORMAL
        fakeTimeProvider.advanceTimeByDays(1)
        assertEquals(TaskAgeCategory.NORMAL, workflowEngine.getActiveAgeCategory(activeSince))

        // 3 days -> WARNING
        fakeTimeProvider.advanceTimeByDays(2)
        assertEquals(TaskAgeCategory.WARNING, workflowEngine.getActiveAgeCategory(activeSince))

        // 6 days -> STRONG_WARNING
        fakeTimeProvider.advanceTimeByDays(3)
        assertEquals(TaskAgeCategory.STRONG_WARNING, workflowEngine.getActiveAgeCategory(activeSince))
    }
}
