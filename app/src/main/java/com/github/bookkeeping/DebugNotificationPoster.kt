package com.github.bookkeeping

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object DebugNotificationPoster {
    private const val CHANNEL_ID = "bookkeeping_test_transactions"

    fun canPost(context: Context): Boolean {
        return Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    fun postWeChatPayment(context: Context): Boolean {
        if (!canPost(context)) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_permission_title),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(context.getString(R.string.test_notification_title))
            .setContentText(context.getString(R.string.test_notification_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.test_notification_big_text)))
            .setAutoCancel(true)
            .build()
        return postNotification(context, notification)
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(context: Context, notification: android.app.Notification): Boolean {
        if (!canPost(context)) return false
        return runCatching {
            // 用时间戳作通知 ID，避免多条测试通知因固定 ID 互相覆盖。
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        }.isSuccess
    }
}
