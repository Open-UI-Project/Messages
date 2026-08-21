package org.openui.sms.sms.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.ContentValues
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import org.openui.sms.MainActivity
import org.openui.sms.R

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION || action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (!messages.isNullOrEmpty()) {
                val sender = messages[0].displayOriginatingAddress ?: "SMS"
                val body = messages.joinToString("") { it.displayMessageBody ?: "" }
                val timestamp = messages[0].timestampMillis

                if (action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
                    saveMessageToProvider(context, sender, body, timestamp)
                    showNotification(context, sender, body)
                } else {
                    val isDefault = context.packageName == Telephony.Sms.getDefaultSmsPackage(context)
                    if (!isDefault) {
                        showNotification(context, sender, body)
                    }
                }
            }
        }
    }

    private fun saveMessageToProvider(context: Context, sender: String, body: String, timestamp: Long) {
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, sender)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, if (timestamp > 0) timestamp else System.currentTimeMillis())
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            }
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showNotification(context: Context, sender: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "sms_notifications_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SMS Уведомления",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для входящих SMS-сообщений"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(sender)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
