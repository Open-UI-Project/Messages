package org.openui.sms.sms.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MessageBoxType(val code: Int) {
    INBOX(1),
    SENT(2),
    DRAFT(3),
    OUTBOX(4),
    FAILED(5),
    UNKNOWN(0);

    companion object {
        fun fromCode(code: Int): MessageBoxType =
            entries.find { it.code == code } ?: UNKNOWN
    }
}

data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val body: String,
    val timestamp: Long,
    val boxType: MessageBoxType,
    val isRead: Boolean
) {
    val displayName: String
        get() = contactName ?: address

    val title: String
        get() = displayName

    val formattedDate: String
        get() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

    val formattedDayHeader: String
        get() {
            val now = java.util.Calendar.getInstance()
            val msgCal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
            val sameYear = now.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR)
            val dayDiff = now.get(java.util.Calendar.DAY_OF_YEAR) - msgCal.get(java.util.Calendar.DAY_OF_YEAR)
            
            return when {
                sameYear && dayDiff == 0 -> "Today"
                sameYear && dayDiff == 1 -> "Yesterday"
                sameYear -> SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(timestamp)).replaceFirstChar { it.uppercase() }
                else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
}

data class SmsConversation(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val snippet: String,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val messageCount: Int,
    val hardcodedDate: String? = null
) {
    val displayName: String
        get() = contactName ?: address

    val title: String
        get() = displayName

    val formattedDate: String
        get() = hardcodedDate ?: SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(lastTimestamp))
}
