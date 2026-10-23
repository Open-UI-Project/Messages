package org.openui.sms.sms.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import org.openui.sms.MainActivity
import java.util.concurrent.ConcurrentHashMap

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private val recentMessages = ConcurrentHashMap<String, Long>()
        private const val DEDUPLICATION_WINDOW_MS = 6000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Telephony.Sms.Intents.SMS_DELIVER_ACTION &&
            action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: "SMS"
        val body = messages.joinToString("") { it.displayMessageBody ?: "" }
        val timestamp = messages[0].timestampMillis

        val now = System.currentTimeMillis()
        val deduplicationKey = "${sender.trim()}:${body.trim()}"
        val lastReceived = recentMessages[deduplicationKey]

        if (recentMessages.size > 100) {
            recentMessages.entries.removeIf { now - it.value > 60_000L }
        }

        if (lastReceived != null && (now - lastReceived) < DEDUPLICATION_WINDOW_MS) {
            return
        }
        recentMessages[deduplicationKey] = now

        val isDefault = context.packageName == Telephony.Sms.getDefaultSmsPackage(context)

        if (action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            saveMessageToProvider(context, sender, body, timestamp)
            showNotification(context, sender, body)
        } else if (action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            if (!isDefault) {
                showNotification(context, sender, body)
            }
        }
    }

    private fun saveMessageToProvider(context: Context, sender: String, body: String, timestamp: Long) {
        try {
            val now = if (timestamp > 0) timestamp else System.currentTimeMillis()

            try {
                val checkCursor = context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf(Telephony.Sms._ID),
                    "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.BODY} = ? AND ${Telephony.Sms.DATE} >= ?",
                    arrayOf(sender, body, (now - 15_000L).toString()),
                    null
                )
                val exists = (checkCursor?.count ?: 0) > 0
                checkCursor?.close()
                if (exists) {
                    return
                }
            } catch (_: Exception) {
            }

            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, sender)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, now)
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
                "SMS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
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

        val notifId = (sender.hashCode() * 31 + body.hashCode()) and 0x7FFFFFFF
        notificationManager.notify(notifId, notification)
    }
}
