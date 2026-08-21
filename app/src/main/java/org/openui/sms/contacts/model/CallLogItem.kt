package org.openui.sms.contacts.model

import androidx.compose.runtime.Immutable

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    UNKNOWN
}

@Immutable
data class CallLogItem(
    val id: Long,
    val number: String,
    val type: CallType,
    val timestamp: Long,
    val durationSeconds: Long
)
