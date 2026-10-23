package org.openui.sms.sms.data

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.collection.LruCache
import org.openui.sms.contacts.data.ContactManager
import org.openui.sms.contacts.model.CallLogItem
import org.openui.sms.contacts.model.CallType
import org.openui.sms.contacts.model.ContactItem
import org.openui.sms.sms.model.MessageBoxType
import org.openui.sms.sms.model.SmsConversation
import org.openui.sms.sms.model.SmsMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.provider.CallLog

interface SmsRepository {
    fun getConversations(): Flow<List<SmsConversation>>
    fun getMessagesForThread(threadId: Long): Flow<List<SmsMessage>>
    suspend fun sendSms(recipient: String, body: String): Result<Unit>
    suspend fun markThreadAsRead(threadId: Long): Boolean
    suspend fun markAllAsRead(): Boolean
    suspend fun deleteMessage(messageId: Long): Boolean
    suspend fun deleteConversation(threadId: Long): Boolean
    suspend fun resolveContactName(phoneNumber: String): String?
    suspend fun getAllContacts(): List<ContactItem>
    suspend fun getCallLogsForNumber(phoneNumber: String): List<CallLogItem>
}

class DefaultSmsRepository(private val context: Context) : SmsRepository {

    private val contentResolver: ContentResolver = context.contentResolver
    private val contactCache = LruCache<String, String>(200)

    override fun getConversations(): Flow<List<SmsConversation>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    trySend(loadConversationsInternal())
                }
            }
        }

        try {
            contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                observer
            )
            trySend(loadConversationsInternal())
        } catch (e: SecurityException) {
            Log.e(TAG, "READ_SMS permission not granted yet", e)
            trySend(emptyList())
        }

        awaitClose {
            try {
                contentResolver.unregisterContentObserver(observer)
            } catch (ignored: Exception) {}
        }
    }
        .conflate()
        .flowOn(Dispatchers.IO)

    override fun getMessagesForThread(threadId: Long): Flow<List<SmsMessage>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    trySend(loadMessagesForThreadInternal(threadId))
                }
            }
        }

        try {
            contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                observer
            )
            trySend(loadMessagesForThreadInternal(threadId))
        } catch (e: SecurityException) {
            Log.e(TAG, "READ_SMS permission not granted yet", e)
            trySend(emptyList())
        }

        awaitClose {
            try {
                contentResolver.unregisterContentObserver(observer)
            } catch (ignored: Exception) {}
        }
    }
        .conflate()
        .flowOn(Dispatchers.IO)

    private fun loadConversationsInternal(): List<SmsConversation> {
        var list = queryConversationsFromUri(Telephony.Sms.CONTENT_URI)
        if (list.isEmpty()) {
            Log.w(TAG, "Telephony.Sms.CONTENT_URI returned 0 items, trying mms-sms/conversations")
            list = queryConversationsFromUri(Uri.parse("content://mms-sms/conversations?simple=true"))
        }
        if (list.isEmpty()) {
            Log.w(TAG, "mms-sms/conversations returned 0 items, trying inbox/sent fallback")
            val inboxList = queryConversationsFromUri(Uri.parse("content://sms/inbox"))
            val sentList = queryConversationsFromUri(Uri.parse("content://sms/sent"))
            list = mergeConversations(inboxList + sentList)
        }
        if (list.isEmpty()) {
            Log.w(TAG, "Trying content://sms fallback")
            list = queryConversationsFromUri(Uri.parse("content://sms"))
        }
        return list
    }

    private fun queryConversationsFromUri(uri: Uri): List<SmsConversation> {
        val conversationsMap = LinkedHashMap<Long, SmsConversationBuilder>()

        try {
            contentResolver.query(
                uri,
                null,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val threadIdIdx = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
                val readIdx = cursor.getColumnIndex(Telephony.Sms.READ)

                while (cursor.moveToNext()) {
                    val rawThreadId = if (threadIdIdx != -1) cursor.getLong(threadIdIdx) else 0L
                    val address = if (addressIdx != -1) cursor.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx != -1) cursor.getString(bodyIdx) ?: "" else ""
                    val timestamp = if (dateIdx != -1) cursor.getLong(dateIdx) else 0L
                    val isRead = if (readIdx != -1) cursor.getInt(readIdx) == 1 else true

                    val safeAddress = if (address.isNotBlank()) address else "Unknown"
                    val threadId = if (rawThreadId > 0) {
                        rawThreadId
                    } else if (safeAddress != "Unknown") {
                        val cleanNum = safeAddress.replace(Regex("[^0-9+]"), "")
                        val key = if (cleanNum.length >= 7) cleanNum.takeLast(7) else cleanNum
                        if (key.isNotBlank()) (key.hashCode().toLong() and 0x7FFFFFFF) else 0L
                    } else {
                        0L
                    }

                    val builder = conversationsMap.getOrPut(threadId) {
                        SmsConversationBuilder(
                            threadId = threadId,
                            address = safeAddress,
                            snippet = body,
                            lastTimestamp = timestamp
                        )
                    }

                    if (timestamp > builder.lastTimestamp) {
                        builder.lastTimestamp = timestamp
                        builder.snippet = body
                    }

                    if ((builder.address == "Unknown" || builder.address.isBlank()) && safeAddress != "Unknown") {
                        builder.address = safeAddress
                    }

                    builder.messageCount++
                    if (!isRead) {
                        builder.unreadCount++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying conversations from $uri", e)
            return emptyList()
        }

        fillMissingAddresses(conversationsMap)

        return conversationsMap.values.map { builder ->
            val contactName = resolveContactNameSync(builder.address)
            SmsConversation(
                threadId = builder.threadId,
                address = builder.address,
                contactName = contactName,
                snippet = builder.snippet,
                unreadCount = builder.unreadCount,
                messageCount = builder.messageCount,
                lastTimestamp = builder.lastTimestamp
            )
        }.sortedByDescending { it.lastTimestamp }
    }

    private fun mergeConversations(items: List<SmsConversation>): List<SmsConversation> {
        val map = LinkedHashMap<Long, SmsConversation>()
        for (item in items) {
            val existing = map[item.threadId]
            if (existing == null) {
                map[item.threadId] = item
            } else {
                val newestTimestamp = maxOf(existing.lastTimestamp, item.lastTimestamp)
                val newestSnippet = if (item.lastTimestamp >= existing.lastTimestamp) item.snippet else existing.snippet
                map[item.threadId] = existing.copy(
                    lastTimestamp = newestTimestamp,
                    snippet = newestSnippet,
                    unreadCount = existing.unreadCount + item.unreadCount,
                    messageCount = existing.messageCount + item.messageCount
                )
            }
        }
        return map.values.sortedByDescending { it.lastTimestamp }
    }

    override suspend fun getCallLogsForNumber(phoneNumber: String): List<CallLogItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<CallLogItem>()
        if (phoneNumber.isBlank()) return@withContext result

        val cleanNum = phoneNumber.replace("[^0-9+]".toRegex(), "")
        val lastDigits = if (cleanNum.length >= 7) cleanNum.takeLast(7) else cleanNum

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        try {
            val selection = "${CallLog.Calls.NUMBER} LIKE ?"
            val selectionArgs = arrayOf("%$lastDigits")

            contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

                while (cursor.moveToNext()) {
                    val id = if (idIdx != -1) cursor.getLong(idIdx) else 0L
                    val number = if (numIdx != -1) cursor.getString(numIdx) ?: phoneNumber else phoneNumber
                    val typeInt = if (typeIdx != -1) cursor.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                    val date = if (dateIdx != -1) cursor.getLong(dateIdx) else 0L
                    val duration = if (durIdx != -1) cursor.getLong(durIdx) else 0L

                    val callType = when (typeInt) {
                        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                        CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                        else -> CallType.UNKNOWN
                    }

                    result.add(
                        CallLogItem(
                            id = id,
                            number = number,
                            type = callType,
                            timestamp = date,
                            durationSeconds = duration
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading call logs for number $phoneNumber", e)
        }
        result
    }

    private fun fillMissingAddresses(conversationsMap: MutableMap<Long, SmsConversationBuilder>) {
        val missingThreads = conversationsMap.filter { it.value.address.isBlank() }.keys
        if (missingThreads.isEmpty()) return

        for (threadId in missingThreads) {
            try {
                contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(Telephony.Sms.ADDRESS),
                    "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.ADDRESS} IS NOT NULL",
                    arrayOf(threadId.toString()),
                    "${Telephony.Sms.DATE} DESC LIMIT 1"
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val addr = cursor.getString(0)
                        if (!addr.isNullOrBlank()) {
                            conversationsMap[threadId]?.address = addr
                        }
                    }
                }
            } catch (ignored: Exception) {}
        }
    }

    private fun loadMessagesForThreadInternal(threadId: Long): List<SmsMessage> {
        val messages = ArrayList<SmsMessage>()
        val urisToTry = listOf(
            Telephony.Sms.CONTENT_URI,
            Uri.parse("content://sms/inbox"),
            Uri.parse("content://sms/sent"),
            Uri.parse("content://sms")
        )

        for (uri in urisToTry) {
            try {
                contentResolver.query(
                    uri,
                    null,
                    "${Telephony.Sms.THREAD_ID} = ?",
                    arrayOf(threadId.toString()),
                    "${Telephony.Sms.DATE} ASC"
                )?.use { cursor ->
                    parseMessagesFromCursor(cursor, threadId, messages)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading messages for thread $threadId from $uri", e)
            }

            if (messages.isNotEmpty()) break
        }

        return messages
    }

    private fun parseMessagesFromCursor(
        cursor: android.database.Cursor,
        threadId: Long,
        messages: MutableList<SmsMessage>
    ) {
        val idIdx = cursor.getColumnIndex(Telephony.Sms._ID)
        val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
        val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
        val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
        val readIdx = cursor.getColumnIndex(Telephony.Sms.READ)
        val typeIdx = cursor.getColumnIndex(Telephony.Sms.TYPE)

        var contactNameCached: String? = null
        var firstAddress = ""
        val seenIds = mutableSetOf<Long>()

        while (cursor.moveToNext()) {
            val id = if (idIdx != -1) cursor.getLong(idIdx) else 0L
            if (id > 0 && !seenIds.add(id)) continue

            val address = if (addressIdx != -1) cursor.getString(addressIdx) ?: "" else ""
            if (contactNameCached == null && address.isNotBlank()) {
                firstAddress = address
                contactNameCached = resolveContactNameSync(address)
            }
            val body = if (bodyIdx != -1) cursor.getString(bodyIdx) ?: "" else ""
            val timestamp = if (dateIdx != -1) cursor.getLong(dateIdx) else 0L
            val isRead = if (readIdx != -1) cursor.getInt(readIdx) == 1 else true
            val typeInt = if (typeIdx != -1) cursor.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX

            val type = when (typeInt) {
                Telephony.Sms.MESSAGE_TYPE_INBOX -> MessageBoxType.INBOX
                Telephony.Sms.MESSAGE_TYPE_SENT -> MessageBoxType.SENT
                Telephony.Sms.MESSAGE_TYPE_OUTBOX -> MessageBoxType.OUTBOX
                Telephony.Sms.MESSAGE_TYPE_FAILED -> MessageBoxType.FAILED
                else -> MessageBoxType.UNKNOWN
            }

            messages.add(
                SmsMessage(
                    id = id,
                    threadId = threadId,
                    address = if (address.isBlank()) firstAddress else address,
                    contactName = contactNameCached,
                    body = body,
                    timestamp = timestamp,
                    isRead = isRead,
                    boxType = type
                )
            )
        }
    }

    override suspend fun sendSms(recipient: String, body: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            if (smsManager == null) {
                return@withContext Result.failure(Exception("SmsManager is not available"))
            }

            val sentIntent = PendingIntent.getBroadcast(
                context, 0, Intent("SMS_SENT"),
                PendingIntent.FLAG_IMMUTABLE
            )

            val deliveredIntent = PendingIntent.getBroadcast(
                context, 0, Intent("SMS_DELIVERED"),
                PendingIntent.FLAG_IMMUTABLE
            )

            smsManager.sendTextMessage(recipient, null, body, sentIntent, deliveredIntent)

            try {
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, recipient)
                    put(Telephony.Sms.BODY, body)
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                    put(Telephony.Sms.READ, 1)
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                }
                contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
                contentResolver.notifyChange(Telephony.Sms.CONTENT_URI, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting sent SMS to provider", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            Result.failure(e)
        }
    }

    override suspend fun markThreadAsRead(threadId: Long): Boolean = withContext(Dispatchers.IO) {
        if (org.openui.sms.data.AppSettings.getInstance(context).isGhostMode()) {
            return@withContext false
        }
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
            }
            val selection = "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0"
            val selectionArgs = arrayOf(threadId.toString())

            val count = contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                selection,
                selectionArgs
            )
            count > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error marking thread as read", e)
            false
        }
    }

    override suspend fun markAllAsRead(): Boolean = withContext(Dispatchers.IO) {
        if (org.openui.sms.data.AppSettings.getInstance(context).isGhostMode()) {
            return@withContext false
        }
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
            }
            val selection = "${Telephony.Sms.READ} = 0"

            val count = contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                selection,
                null
            )
            count >= 0
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all messages as read", e)
            false
        }
    }

    override suspend fun deleteMessage(messageId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, messageId.toString())
            val count = contentResolver.delete(uri, null, null)
            count > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message", e)
            false
        }
    }

    override suspend fun deleteConversation(threadId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val selection = "${Telephony.Sms.THREAD_ID} = ?"
            val selectionArgs = arrayOf(threadId.toString())

            val count = contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                selection,
                selectionArgs
            )
            count > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting conversation", e)
            false
        }
    }

    override suspend fun resolveContactName(phoneNumber: String): String? = withContext(Dispatchers.IO) {
        resolveContactNameSync(phoneNumber)
    }

    private fun resolveContactNameSync(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null

        val cached = contactCache.get(phoneNumber)
        if (cached != null) {
            return if (cached == NO_CONTACT_NAME) null else cached
        }

        var contactName: String? = null
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        contactName = cursor.getString(nameIdx)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Could not resolve contact name for $phoneNumber")
        }

        contactCache.put(phoneNumber, contactName ?: NO_CONTACT_NAME)
        return contactName
    }

    override suspend fun getAllContacts(): List<ContactItem> = withContext(Dispatchers.IO) {
        ContactManager.getAllContacts(context)
    }

    private class SmsConversationBuilder(
        val threadId: Long,
        var address: String,
        var snippet: String,
        var lastTimestamp: Long,
        var unreadCount: Int = 0,
        var messageCount: Int = 0
    )

    companion object {
        private const val TAG = "DefaultSmsRepository"
        private const val NO_CONTACT_NAME = "__NO_NAME__"
    }
}

