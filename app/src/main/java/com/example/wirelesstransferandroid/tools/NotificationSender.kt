package com.example.wirelesstransferandroid.tools

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import com.example.wirelesstransferandroid.R

object NotificationSender {
    val fileShareChannel = NotificationChannel("FileShare", "FileShare", NotificationManager.IMPORTANCE_HIGH)
    var smallIcon = R.mipmap.ic_launcher_round

    fun getNotifyId(channel: NotificationChannel): Int {
        when (channel.id) {
            "FileShare" -> return 1
        }
        return 0
    }

    fun sendNotification(context: Context, title: String, content: String, channel: NotificationChannel) {
        var notifyId = getNotifyId(channel)

        val builder = Notification.Builder(context, channel.id)
        builder
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(content)
        val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        manager.notify(notifyId, builder.build())
    }

    fun sendProgressNotification(context: Context, title: String, content: String, progress: Int, indeterminate: Boolean, channel: NotificationChannel) {
        var notifyId = getNotifyId(channel)

        val builder = Notification.Builder(context, channel.id)
        builder
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(content)
            .setProgress(100, progress, indeterminate)
        val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        manager.notify(notifyId, builder.build())
    }

    fun dismissNotification(context: Context, channel: NotificationChannel) {
        var notifyId = getNotifyId(channel)
        val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notifyId)
    }
}