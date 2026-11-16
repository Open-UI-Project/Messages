package org.openui.sms.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Minimal MMS broadcast endpoint.
 *
 * Open UI Messages is an SMS-only client: MMS/PDU handling is intentionally not
 * implemented. This receiver exists only because Android requires a default SMS
 * application to declare a WAP PUSH receiver; incoming MMS is ignored.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // MMS is not supported by design.
    }
}
