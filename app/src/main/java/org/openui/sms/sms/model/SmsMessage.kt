package org.openui.sms.sms.model

import androidx.compose.runtime.Immutable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val timeFormatCache = ThreadLocal.withInitial { SimpleDateFormat("HH:mm", Locale.getDefault()) }
private val dayHeaderFormatSameYearCache = ThreadLocal.withInitial { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
private val dayHeaderFormatOtherYearCache = ThreadLocal.withInitial { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }
private val convDateFormatCache = ThreadLocal.withInitial { SimpleDateFormat("dd.MM", Locale.getDefault()) }

private fun formatMessageTime(ts: Long): String = (timeFormatCache.get() ?: SimpleDateFormat("HH:mm", Locale.getDefault())).format(Date(ts))

private fun formatDayHeader(ts: Long): String {
    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { timeInMillis = ts }
    val sameYear = now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
    val dayDiff = now.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR)

    return when {
        sameYear && dayDiff == 0 -> "Today"
        sameYear && dayDiff == 1 -> "Yesterday"
        sameYear -> (dayHeaderFormatSameYearCache.get() ?: SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())).format(Date(ts)).replaceFirstChar { it.uppercase() }
        else -> (dayHeaderFormatOtherYearCache.get() ?: SimpleDateFormat("d MMMM yyyy", Locale.getDefault())).format(Date(ts))
    }
}

private fun formatConversationDate(ts: Long): String = (convDateFormatCache.get() ?: SimpleDateFormat("dd.MM", Locale.getDefault())).format(Date(ts))

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

@Immutable
data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val body: String,
    val timestamp: Long,
    val boxType: MessageBoxType,
    val isRead: Boolean,
    val displayName: String = contactName ?: address,
    val title: String = displayName,
    val formattedDate: String = formatMessageTime(timestamp),
    val formattedDayHeader: String = formatDayHeader(timestamp)
)

@Immutable
data class SmsConversation(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val snippet: String,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val messageCount: Int,
    val hardcodedDate: String? = null,
    val displayName: String = contactName ?: address,
    val title: String = displayName,
    val formattedDate: String = hardcodedDate ?: formatConversationDate(lastTimestamp)
)
