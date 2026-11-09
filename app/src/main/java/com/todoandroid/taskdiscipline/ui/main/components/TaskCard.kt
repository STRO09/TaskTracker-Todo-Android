package com.todoandroid.taskdiscipline.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoandroid.taskdiscipline.domain.model.Task
import com.todoandroid.taskdiscipline.domain.model.TaskAgeCategory
import com.todoandroid.taskdiscipline.domain.model.TaskStatus
import com.todoandroid.taskdiscipline.ui.theme.*

@Composable
fun TaskCard(
    task: Task,
    ageCategory: TaskAgeCategory,
    formattedAge: String,
    onActivateClick: () -> Unit = {},
    onCompleteClick: () -> Unit = {},
    onDragStart: (Offset) -> Unit = {},
    onDragDelta: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    isDraggingThisCard: Boolean = false,
    modifier: Modifier = Modifier
) {
    val borderColor = when (ageCategory) {
        TaskAgeCategory.STRONG_WARNING -> AgingCritical
        TaskAgeCategory.WARNING -> AgingWarning
        TaskAgeCategory.SLIGHTLY_EMPHASIZED -> AgingSlight
        TaskAgeCategory.NORMAL -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    val cardBg = when (ageCategory) {
        TaskAgeCategory.STRONG_WARNING -> AgingCritical.copy(alpha = 0.12f)
        TaskAgeCategory.WARNING -> AgingWarning.copy(alpha = 0.08f)
        TaskAgeCategory.SLIGHTLY_EMPHASIZED -> AgingSlight.copy(alpha = 0.05f)
        TaskAgeCategory.NORMAL -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    var rootPosition = Offset.Zero

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                rootPosition = coordinates.positionInRoot()
            }
            .pointerInput(task.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart(rootPosition) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(borderColor),
            width = if (ageCategory == TaskAgeCategory.STRONG_WARNING || ageCategory == TaskAgeCategory.WARNING) 2.dp else 1.dp
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDraggingThisCard) 8.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Age Badge Chip
                AgeChip(category = ageCategory, label = formattedAge)
            }

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when (task.status) {
                            TaskStatus.PENDING -> "Pending: $formattedAge"
                            TaskStatus.ACTIVE -> "Active: $formattedAge"
                            TaskStatus.COMPLETED -> "Completed: $formattedAge"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (task.status) {
                    TaskStatus.PENDING -> {
                        Button(
                            onClick = onActivateClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ActiveBadgeColor,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Activate", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    TaskStatus.ACTIVE -> {
                        Button(
                            onClick = onCompleteClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CompletedBadgeColor,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Mark as Completed", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    TaskStatus.COMPLETED -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CompletedBadgeColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "COMPLETED",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CompletedBadgeColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgeChip(category: TaskAgeCategory, label: String) {
    val (bgColor, textColor) = when (category) {
        TaskAgeCategory.STRONG_WARNING -> AgingCritical to Color.White
        TaskAgeCategory.WARNING -> AgingWarning to Color.White
        TaskAgeCategory.SLIGHTLY_EMPHASIZED -> AgingSlight to Color.Black
        TaskAgeCategory.NORMAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}
