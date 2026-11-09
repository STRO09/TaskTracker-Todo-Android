package com.todoandroid.taskdiscipline.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoandroid.taskdiscipline.domain.model.Task
import com.todoandroid.taskdiscipline.domain.model.TaskStatus
import com.todoandroid.taskdiscipline.domain.model.TaskAgeCategory
import com.todoandroid.taskdiscipline.domain.workflow.TransitionResult
import com.todoandroid.taskdiscipline.ui.main.components.CreateTaskDialog
import com.todoandroid.taskdiscipline.ui.main.components.DragOverlay
import com.todoandroid.taskdiscipline.ui.main.components.TaskCard
import com.todoandroid.taskdiscipline.ui.main.dragdrop.rememberDragDropState
import com.todoandroid.taskdiscipline.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val dragDropState = rememberDragDropState(viewModel.workflowEngine)

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "TASK DISCIPLINE",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                )
                            )
                            Text(
                                text = "PENDING → ACTIVE → COMPLETED",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showCreateTaskDialog() },
                    icon = { Icon(Icons.Default.Add, contentDescription = "New Task") },
                    text = { Text("New Task") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Section 1: PENDING
                DroppableTaskSection(
                    status = TaskStatus.PENDING,
                    title = "PENDING",
                    subtitle = "Oldest first (${uiState.pendingTasks.size})",
                    icon = Icons.Default.HourglassEmpty,
                    badgeText = "${uiState.pendingTasks.size} Tasks",
                    headerBg = PendingHeaderBg,
                    badgeBg = PendingBadgeColor,
                    tasks = uiState.pendingTasks,
                    dragDropState = dragDropState,
                    viewModel = viewModel
                )

                // Section 2: ACTIVE (WIP Enforced)
                DroppableTaskSection(
                    status = TaskStatus.ACTIVE,
                    title = "ACTIVE",
                    subtitle = "WIP Limit: ${uiState.activeCount} / ${uiState.wipLimit}",
                    icon = Icons.Default.PlayArrow,
                    badgeText = "${uiState.activeCount} / ${uiState.wipLimit} WIP",
                    headerBg = ActiveHeaderBg,
                    badgeBg = ActiveBadgeColor,
                    tasks = uiState.activeTasks,
                    dragDropState = dragDropState,
                    viewModel = viewModel
                )

                // Section 3: COMPLETED
                DroppableTaskSection(
                    status = TaskStatus.COMPLETED,
                    title = "COMPLETED",
                    subtitle = "Auto-deletes after 72h (${uiState.completedTasks.size})",
                    icon = Icons.Default.CheckCircle,
                    badgeText = "${uiState.completedTasks.size} Tasks",
                    headerBg = CompletedHeaderBg,
                    badgeBg = CompletedBadgeColor,
                    tasks = uiState.completedTasks,
                    dragDropState = dragDropState,
                    viewModel = viewModel
                )

                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
            }
        }

        // Global Drag Preview Overlay
        if (dragDropState.isDragging) {
            DragOverlay(dragDropState = dragDropState)
        }

        // Create Task Modal Dialog
        if (uiState.isCreateTaskDialogVisible) {
            CreateTaskDialog(
                onDismissRequest = { viewModel.hideCreateTaskDialog() },
                onConfirm = { title -> viewModel.createNewTask(title) }
            )
        }
    }
}

@Composable
fun DroppableTaskSection(
    status: TaskStatus,
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String,
    headerBg: Color,
    badgeBg: Color,
    tasks: List<Task>,
    dragDropState: com.todoandroid.taskdiscipline.ui.main.dragdrop.DragDropState,
    viewModel: MainViewModel
) {
    val isHoveredTarget = dragDropState.hoveredTargetStatus == status
    val validation = dragDropState.hoveredValidation

    val sectionBorderColor = when {
        isHoveredTarget && validation is TransitionResult.Allowed -> CompletedBadgeColor
        isHoveredTarget && validation is TransitionResult.Forbidden -> AgingCritical
        else -> Color.Transparent
    }

    val sectionBg = when {
        isHoveredTarget && validation is TransitionResult.Allowed -> CompletedBadgeColor.copy(alpha = 0.08f)
        isHoveredTarget && validation is TransitionResult.Forbidden -> AgingCritical.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(sectionBg)
            .border(
                width = if (isHoveredTarget) 2.dp else 0.dp,
                color = sectionBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp)
            .onGloballyPositioned { coordinates ->
                dragDropState.registerTargetBounds(status, coordinates.boundsInRoot())
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        TaskSectionHeader(
            title = title,
            subtitle = subtitle,
            icon = icon,
            badgeText = badgeText,
            headerBg = headerBg,
            badgeBg = badgeBg
        )

        // Task List or Placeholder
        if (tasks.isEmpty()) {
            SectionPlaceholder(
                text = when (status) {
                    TaskStatus.PENDING -> "No pending tasks. Tap '+ New Task' to add."
                    TaskStatus.ACTIVE -> "No active tasks. Tap 'Activate' on a pending task."
                    TaskStatus.COMPLETED -> "No completed tasks in the last 72 hours."
                },
                borderColor = badgeBg.copy(alpha = 0.3f)
            )
        } else {
            tasks.forEach { task ->
                val ageCategory = when (task.status) {
                    TaskStatus.PENDING -> viewModel.getPendingAgeCategory(task.pendingSince)
                    TaskStatus.ACTIVE -> viewModel.getActiveAgeCategory(task.activeSince)
                    TaskStatus.COMPLETED -> TaskAgeCategory.NORMAL
                }
                val ageLabel = viewModel.formatAgeLabel(
                    when (task.status) {
                        TaskStatus.PENDING -> task.pendingSince
                        TaskStatus.ACTIVE -> task.activeSince
                        TaskStatus.COMPLETED -> task.completedAt
                    }
                )

                val uiState by viewModel.uiState.collectAsState()

                TaskCard(
                    task = task,
                    ageCategory = ageCategory,
                    formattedAge = ageLabel,
                    onActivateClick = { viewModel.activateTask(task.id) },
                    onCompleteClick = { viewModel.completeTask(task.id) },
                    onDragStart = { startPos -> dragDropState.onDragStart(task, startPos) },
                    onDragDelta = { delta ->
                        dragDropState.onDragDelta(delta, uiState.activeCount, uiState.wipLimit)
                    },
                    onDragEnd = {
                        dragDropState.onDragEnd(
                            currentActiveCount = uiState.activeCount,
                            wipLimit = uiState.wipLimit,
                            onDropSuccess = { targetTask, targetStatus ->
                                viewModel.handleTaskDrop(targetTask, targetStatus)
                            },
                            onDropForbidden = { reason ->
                                viewModel.showUserMessage(reason)
                            }
                        )
                    },
                    isDraggingThisCard = dragDropState.draggedTask?.id == task.id
                )
            }
        }
    }
}

@Composable
fun TaskSectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String,
    headerBg: Color,
    badgeBg: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(headerBg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = badgeBg,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(badgeBg)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
fun SectionPlaceholder(
    text: String,
    borderColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

