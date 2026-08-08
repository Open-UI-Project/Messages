package org.openui.sms.sms.model

sealed interface SmsSendResult {
    data object Success : SmsSendResult
    data class Error(val message: String, val cause: Throwable? = null) : SmsSendResult
}
