package com.todoandroid.taskdiscipline.domain.workflow

sealed interface TransitionResult {
    data object Allowed : TransitionResult
    data class Forbidden(val reason: String) : TransitionResult
}
