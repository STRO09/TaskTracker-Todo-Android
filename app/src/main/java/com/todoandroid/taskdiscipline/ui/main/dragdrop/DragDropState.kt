package com.todoandroid.taskdiscipline.ui.main.dragdrop

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.todoandroid.taskdiscipline.domain.model.Task
import com.todoandroid.taskdiscipline.domain.model.TaskStatus
import com.todoandroid.taskdiscipline.domain.workflow.TaskWorkflowEngine
import com.todoandroid.taskdiscipline.domain.workflow.TransitionResult

class DragDropState(
    private val workflowEngine: TaskWorkflowEngine
) {
    var draggedTask by mutableStateOf<Task?>(null)
        private set

    var dragPosition by mutableStateOf(Offset.Zero)
        private set

    var dragOffset by mutableStateOf(Offset.Zero)
        private set

    var hoveredTargetStatus by mutableStateOf<TaskStatus?>(null)
        private set

    var hoveredValidation by mutableStateOf<TransitionResult?>(null)
        private set

    private val targetBounds = mutableStateMapOf<TaskStatus, Rect>()

    val isDragging: Boolean get() = draggedTask != null

    fun registerTargetBounds(status: TaskStatus, bounds: Rect) {
        targetBounds[status] = bounds
    }

    fun onDragStart(task: Task, initialPosition: Offset) {
        draggedTask = task
        dragPosition = initialPosition
        dragOffset = Offset.Zero
        updateHoverTarget(initialPosition)
    }

    fun onDragDelta(delta: Offset, currentActiveCount: Int, wipLimit: Int) {
        dragOffset += delta
        val currentTouchPos = dragPosition + dragOffset
        updateHoverTarget(currentTouchPos)

        val task = draggedTask
        val target = hoveredTargetStatus
        if (task != null && target != null) {
            hoveredValidation = workflowEngine.validateTransition(
                task = task,
                targetStatus = target,
                currentActiveCount = currentActiveCount,
                wipLimit = wipLimit
            )
        } else {
            hoveredValidation = null
        }
    }

    private fun updateHoverTarget(position: Offset) {
        val match = targetBounds.entries.find { (_, bounds) ->
            bounds.contains(position)
        }
        hoveredTargetStatus = match?.key
    }

    fun onDragEnd(
        currentActiveCount: Int,
        wipLimit: Int,
        onDropSuccess: (Task, TaskStatus) -> Unit,
        onDropForbidden: (String) -> Unit
    ) {
        val task = draggedTask
        val target = hoveredTargetStatus

        if (task != null && target != null) {
            val validation = workflowEngine.validateTransition(
                task = task,
                targetStatus = target,
                currentActiveCount = currentActiveCount,
                wipLimit = wipLimit
            )
            if (validation is TransitionResult.Allowed) {
                onDropSuccess(task, target)
            } else if (validation is TransitionResult.Forbidden) {
                onDropForbidden(validation.reason)
            }
        }

        reset()
    }

    fun reset() {
        draggedTask = null
        dragPosition = Offset.Zero
        dragOffset = Offset.Zero
        hoveredTargetStatus = null
        hoveredValidation = null
    }
}

@Composable
fun rememberDragDropState(
    workflowEngine: TaskWorkflowEngine = remember { TaskWorkflowEngine() }
): DragDropState {
    return remember { DragDropState(workflowEngine) }
}
