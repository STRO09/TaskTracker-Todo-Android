package com.todoandroid.taskdiscipline.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.todoandroid.taskdiscipline.MainActivity
import com.todoandroid.taskdiscipline.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_PENDING = "pending_reminders_channel"
        const val CHANNEL_ACTIVE = "active_reminders_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val pendingChannel = NotificationChannel(
                CHANNEL_PENDING,
                "Pending Task Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for neglected pending tasks"
            }

            val activeChannel = NotificationChannel(
                CHANNEL_ACTIVE,
                "Active Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for prolonged active tasks"
            }

            manager.createNotificationChannel(pendingChannel)
            manager.createNotificationChannel(activeChannel)
        }
    }

    fun sendPendingReminder(taskId: String, taskTitle: String, days: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TARGET_TASK_ID", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_PENDING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Pending Task Reminder")
            .setContentText("'$taskTitle' has been pending for $days day${if (days > 1) "s" else ""}.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(taskId.hashCode(), notification)
    }

    fun sendActiveReminder(taskId: String, taskTitle: String, days: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TARGET_TASK_ID", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode() + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ACTIVE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Active Task Reminder")
            .setContentText("'$taskTitle' has been active for $days day${if (days > 1) "s" else ""}.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(taskId.hashCode() + 1000, notification)
    }
}
