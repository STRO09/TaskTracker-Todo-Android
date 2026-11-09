package com.todoandroid.taskdiscipline.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoandroid.taskdiscipline.domain.model.Task
import com.todoandroid.taskdiscipline.domain.workflow.TransitionResult
import com.todoandroid.taskdiscipline.ui.main.dragdrop.DragDropState
import com.todoandroid.taskdiscipline.ui.theme.AgingCritical
import com.todoandroid.taskdiscipline.ui.theme.CompletedBadgeColor

@Composable
fun DragOverlay(
    dragDropState: DragDropState
) {
    val task = dragDropState.draggedTask ?: return
    val validation = dragDropState.hoveredValidation
    val hoverTarget = dragDropState.hoveredTargetStatus

    val position = dragDropState.dragPosition + dragDropState.dragOffset

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = position.x
                translationY = position.y
            }
    ) {
        Column(
            modifier = Modifier.width(300.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Target Validation Pill
            if (hoverTarget != null && validation != null) {
                val (pillBg, pillText, label) = when (validation) {
                    is TransitionResult.Allowed -> Triple(
                        CompletedBadgeColor,
                        Color.White,
                        "Drop to move to ${hoverTarget.name}"
                    )
                    is TransitionResult.Forbidden -> Triple(
                        AgingCritical,
                        Color.White,
                        validation.reason
                    )
                }

                Box(
                    modifier = Modifier
                        .background(pillBg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = pillText
                        )
                    )
                }
            }

            // Dragged Task Ghost Card
            Card(
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        when (validation) {
                            is TransitionResult.Allowed -> CompletedBadgeColor
                            is TransitionResult.Forbidden -> AgingCritical
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ),
                    width = 2.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = 1.05f
                        scaleY = 1.05f
                        alpha = 0.92f
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dragging from ${task.status.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
