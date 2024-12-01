package com.example.wirelesstransferandroid.tools

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import com.example.wirelesstransferandroid.R

object NotificationSender {
    val fileShareChannel = NotificationChannel("FileShare", "FileShare", NotificationManager.IMPORTANCE_HIGH)

    fun sendNotification(context: Context, title: String, content: String, channel: NotificationChannel) {
        var drawable = R.drawable.ic_launcher_foreground
        var notifyId = 0
        when (channel.id) {
            "FileShare" -> {
                drawable = R.drawable.file_sharing_icon
                notifyId = 1
            }
        }

        val builder = Notification.Builder(context, channel.id)
        builder
            .setSmallIcon(drawable)
            .setContentTitle(title)
            .setContentText(content)
        val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        manager.notify(notifyId, builder.build())
    }
}