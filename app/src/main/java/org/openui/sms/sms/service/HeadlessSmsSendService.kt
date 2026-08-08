package org.openui.sms.sms.service

import android.app.IntentService
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log

class HeadlessSmsSendService : IntentService(TAG) {

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action != "android.intent.action.RESPOND_VIA_MESSAGE") return

        val extras = intent.extras ?: return
        val message = extras.getString(Intent.EXTRA_TEXT) ?: return
        val uri: Uri? = intent.data
        val recipient = uri?.schemeSpecificPart ?: return

        if (recipient.isNotBlank() && message.isNotBlank()) {
            try {
                val smsManager = getSystemService(SmsManager::class.java)
                smsManager?.sendTextMessage(recipient, null, message, null, null)
                Log.d(TAG, "Sent headless SMS to $recipient")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send headless SMS", e)
            }
        }
    }

    companion object {
        private const val TAG = "HeadlessSmsSendService"
    }
}
