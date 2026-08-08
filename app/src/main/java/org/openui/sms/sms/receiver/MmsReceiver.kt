package org.openui.sms.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class MmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION &&
            action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) {
            return
        }
        try {
            Log.d(TAG, "MMS WAP PUSH received: $action")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling MMS", e)
        }
    }

    companion object {
        private const val TAG = "MmsReceiver"
    }
}
