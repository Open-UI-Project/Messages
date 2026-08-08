package org.openui.sms.sms.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log

class SmsStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            ACTION_SMS_SENT -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.d(TAG, "SMS successfully sent")
                    }
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                        Log.w(TAG, "SMS send failed: Generic failure")
                    }
                    SmsManager.RESULT_ERROR_NO_SERVICE -> {
                        Log.w(TAG, "SMS send failed: No service")
                    }
                    SmsManager.RESULT_ERROR_NULL_PDU -> {
                        Log.w(TAG, "SMS send failed: Null PDU")
                    }
                    SmsManager.RESULT_ERROR_RADIO_OFF -> {
                        Log.w(TAG, "SMS send failed: Radio off")
                    }
                    else -> {
                        Log.w(TAG, "SMS send failed with code: $resultCode")
                    }
                }
            }
            ACTION_SMS_DELIVERED -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.d(TAG, "SMS successfully delivered")
                    }
                    Activity.RESULT_CANCELED -> {
                        Log.w(TAG, "SMS delivery cancelled")
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "SmsStatusReceiver"
        const val ACTION_SMS_SENT = "org.openui.sms.sms.ACTION_SMS_SENT"
        const val ACTION_SMS_DELIVERED = "org.openui.sms.sms.ACTION_SMS_DELIVERED"
    }
}
