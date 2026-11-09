package com.todoandroid.taskdiscipline.domain.workflow

interface TimeProvider {
    fun currentTimeMillis(): Long
}

class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

class FakeTimeProvider(var nowMillis: Long = 1000000000000L) : TimeProvider {
    override fun currentTimeMillis(): Long = nowMillis

    fun advanceTimeByMillis(millis: Long) {
        nowMillis += millis
    }

    fun advanceTimeByDays(days: Long) {
        nowMillis += days * 24 * 60 * 60 * 1000L
    }

    fun advanceTimeByHours(hours: Long) {
        nowMillis += hours * 60 * 60 * 1000L
    }
}
