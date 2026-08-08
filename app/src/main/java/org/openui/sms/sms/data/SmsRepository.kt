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
import org.openui.sms.sms.model.ContactItem
import org.openui.sms.sms.model.MessageBoxType
import org.openui.sms.sms.model.SmsConversation
import org.openui.sms.sms.model.SmsMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

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
}

class DefaultSmsRepository(private val context: Context) : SmsRepository {

    private val contentResolver: ContentResolver = context.contentResolver
    private val contactCache = LruCache<String, String>(200)

    override fun getConversations(): Flow<List<SmsConversation>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(loadConversationsInternal())
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
                trySend(loadMessagesForThreadInternal(threadId))
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
        val conversationsMap = LinkedHashMap<Long, SmsConversationBuilder>()

        // First attempt fast query using content://sms/conversations
        val conversationsUri = Uri.parse("content://sms/conversations")
        var loadedViaConversationsUri = false

        try {
            val proj = arrayOf(
                "thread_id",
                "msg_count",
                "snippet",
                "address",
                "date",
                "read"
            )
            contentResolver.query(
                conversationsUri,
                proj,
                null,
                null,
                "date DESC"
            )?.use { cursor ->
                val threadIdIdx = cursor.getColumnIndex("thread_id")
                val msgCountIdx = cursor.getColumnIndex("msg_count")
                val snippetIdx = cursor.getColumnIndex("snippet")
                val addressIdx = cursor.getColumnIndex("address")
                val dateIdx = cursor.getColumnIndex("date")
                val readIdx = cursor.getColumnIndex("read")

                if (threadIdIdx != -1 && dateIdx != -1) {
                    while (cursor.moveToNext()) {
                        val threadId = cursor.getLong(threadIdIdx)
                        val address = if (addressIdx != -1) cursor.getString(addressIdx) ?: "Unknown" else "Unknown"
                        val snippet = if (snippetIdx != -1) cursor.getString(snippetIdx) ?: "Unknown" else "Unknown"
                        val timestamp = cursor.getLong(dateIdx)
                        val msgCount = if (msgCountIdx != -1) cursor.getInt(msgCountIdx) else 1
                        val isRead = if (readIdx != -1) cursor.getInt(readIdx) == 1 else true

                        conversationsMap[threadId] = SmsConversationBuilder(
                            threadId = threadId,
                            address = address,
                            snippet = snippet,
                            lastTimestamp = timestamp,
                            messageCount = msgCount,
                            unreadCount = if (!isRead) 1 else 0
                        )
                    }
                    loadedViaConversationsUri = conversationsMap.isNotEmpty()
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Fast conversations query not supported, fallback to Telephony.Sms", e)
        }

        // Fallback if content://sms/conversations isn't populated or available
        if (!loadedViaConversationsUri) {
            val projection = arrayOf(
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ
            )

            try {
                contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${Telephony.Sms.DATE} DESC"
                )?.use { cursor ->
                    val threadIdIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                    val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    val readIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)

                    while (cursor.moveToNext()) {
                        val threadId = cursor.getLong(threadIdIdx)
                        val address = cursor.getString(addressIdx) ?: "Unknown"
                        val body = cursor.getString(bodyIdx) ?: ""
                        val timestamp = cursor.getLong(dateIdx)
                        val isRead = cursor.getInt(readIdx) == 1

                        val builder = conversationsMap.getOrPut(threadId) {
                            SmsConversationBuilder(
                                threadId = threadId,
                                address = address,
                                snippet = body,
                                lastTimestamp = timestamp
                            )
                        }

                        builder.messageCount++
                        if (!isRead) {
                            builder.unreadCount++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading conversations via Telephony.Sms", e)
                return emptyList()
            }
        } else {
            // Fill missing addresses if necessary
            fillMissingAddresses(conversationsMap)
        }

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
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE
        )

        try {
            contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val readIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
                val typeIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)

                var contactNameCached: String? = null
                var firstAddress = ""

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val address = cursor.getString(addressIdx) ?: ""
                    if (contactNameCached == null && address.isNotBlank()) {
                        firstAddress = address
                        contactNameCached = resolveContactNameSync(address)
                    }
                    val body = cursor.getString(bodyIdx) ?: ""
                    val timestamp = cursor.getLong(dateIdx)
                    val isRead = cursor.getInt(readIdx) == 1
                    val typeInt = cursor.getInt(typeIdx)

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
        } catch (e: Exception) {
            Log.e(TAG, "Error loading messages for thread", e)
        }

        return messages
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
        val contactsList = mutableListOf<ContactItem>()
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                val seenNumbers = mutableSetOf<String>()

                while (cursor.moveToNext()) {
                    val id = if (idIdx != -1) cursor.getLong(idIdx) else 0L
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val rawNumber = if (numberIdx != -1) cursor.getString(numberIdx) ?: "Unknown" else "Unknown"
                    val photoUri = if (photoIdx != -1) cursor.getString(photoIdx) else null

                    val cleanNumber = rawNumber.replace("\\s+".toRegex(), "")
                    if (cleanNumber.isNotBlank() && seenNumbers.add(cleanNumber)) {
                        contactsList.add(
                            ContactItem(
                                id = id,
                                name = name,
                                number = cleanNumber,
                                photoUri = photoUri
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching contacts", e)
        }
        contactsList
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

