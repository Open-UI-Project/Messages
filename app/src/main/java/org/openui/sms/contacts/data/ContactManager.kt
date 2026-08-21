package org.openui.sms.contacts.data

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openui.sms.contacts.model.ContactItem
import java.io.ByteArrayOutputStream

@Immutable
data class SimAccountInfo(
    val simId: Int,
    val displayName: String,
    val carrierName: String,
    val slotIndex: Int,
    val accountName: String?,
    val accountType: String?
)

object ContactManager {

    fun getAvailableSimAccounts(context: Context): List<SimAccountInfo> {
        val simList = mutableListOf<SimAccountInfo>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return simList
        }
        try {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            if (subManager != null) {
                val activeSubs: List<SubscriptionInfo>? = subManager.activeSubscriptionInfoList
                if (!activeSubs.isNullOrEmpty()) {
                    for (sub in activeSubs) {
                        val carrier = sub.carrierName?.toString()?.trim() ?: ""
                        val display = sub.displayName?.toString()?.trim() ?: ""
                        
                        val isCarrierGeneric = carrier.equals("Android", ignoreCase = true) || carrier.equals("T-Mobile", ignoreCase = true)
                        val isDisplayGeneric = display.equals("Android", ignoreCase = true) || display.equals("T-Mobile", ignoreCase = true)
                        
                        val cleanCarrier = if (isCarrierGeneric) "" else carrier
                        val cleanDisplay = if (isDisplayGeneric) "" else display

                        val realName = when {
                            cleanDisplay.isNotBlank() && cleanCarrier.isNotBlank() && cleanDisplay != cleanCarrier -> "$cleanDisplay ($cleanCarrier)"
                            cleanCarrier.isNotBlank() -> cleanCarrier
                            cleanDisplay.isNotBlank() -> cleanDisplay
                            else -> "SIM ${sub.simSlotIndex + 1}"
                        }

                        simList.add(
                            SimAccountInfo(
                                simId = sub.subscriptionId,
                                displayName = realName,
                                carrierName = cleanCarrier,
                                slotIndex = sub.simSlotIndex,
                                accountName = "SIM_${sub.subscriptionId}",
                                accountType = "com.android.sim"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return simList
    }

    suspend fun saveContact(
        context: Context,
        name: String,
        phone: String,
        email: String? = null,
        photoUri: Uri? = null,
        storageTarget: String = "DEVICE"
    ): Boolean = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext false
        }
        return@withContext try {
            val ops = ArrayList<ContentProviderOperation>()

            var accountType: String? = null
            var accountName: String? = null

            if (storageTarget.startsWith("SIM")) {
                accountType = "com.android.sim"
                accountName = storageTarget
            }

            val rawContactInsertIndex = ops.size
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                    .build()
            )

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build()
            )

            if (!email.isNullOrBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                        .build()
                )
            }

            if (photoUri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(photoUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        val photoBytes = stream.toByteArray()
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                                .build()
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateContact(
        context: Context,
        contactId: Long,
        name: String,
        phone: String,
        email: String? = null,
        photoUri: Uri? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext false
        }
        return@withContext try {
            val ops = ArrayList<ContentProviderOperation>()

            ops.add(
                ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            ops.add(
                ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    )
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                    .build()
            )

            if (!email.isNullOrBlank()) {
                val emailUri = ContactsContract.Data.CONTENT_URI
                val emailCursor = context.contentResolver.query(
                    emailUri,
                    arrayOf(ContactsContract.Data._ID),
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE),
                    null
                )
                val hasEmail = (emailCursor?.count ?: 0) > 0
                emailCursor?.close()

                if (hasEmail) {
                    ops.add(
                        ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection(
                                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                                arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            )
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                            .build()
                    )
                } else {
                    val rawContactId = getRawContactId(context, contactId)
                    if (rawContactId != null) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                                .build()
                        )
                    }
                }
            }

            if (photoUri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(photoUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        val photoBytes = stream.toByteArray()

                        val rawContactId = getRawContactId(context, contactId)
                        if (rawContactId != null) {
                            ops.add(
                                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                                    .withSelection(
                                        "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                                        arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                    )
                                    .build()
                            )
                            ops.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                                    .build()
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getRawContactId(context: Context, contactId: Long): Long? {
        val cursor = context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )
        return cursor?.use {
            if (it.moveToFirst()) it.getLong(0) else null
        }
    }

    fun getAllContacts(context: Context): List<ContactItem> = fetchContacts(context)

    fun fetchContacts(context: Context): List<ContactItem> {
        val list = mutableListOf<ContactItem>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return list
        }
        val contentResolver = context.contentResolver
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.ACCOUNT_TYPE_AND_DATA_SET
        )

        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val typeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val accountTypeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.ACCOUNT_TYPE_AND_DATA_SET)

                val seenNumbers = mutableSetOf<String>()

                while (cursor.moveToNext()) {
                    val id = if (idIndex >= 0) cursor.getLong(idIndex) else 0L
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
                    val rawNumber = if (numberIndex >= 0) cursor.getString(numberIndex) ?: "" else ""
                    val photoUri = if (photoIndex >= 0) cursor.getString(photoIndex) else null
                    val type = if (typeIndex >= 0) cursor.getInt(typeIndex).toString() else ""
                    val accountType = if (accountTypeIndex >= 0) cursor.getString(accountTypeIndex) ?: "" else ""

                    val cleanNum = rawNumber.replace("[^0-9+]".toRegex(), "")
                    if (cleanNum.isNotBlank() && seenNumbers.add(cleanNum)) {
                        val email = fetchEmailForContact(context, id)
                        list.add(
                            ContactItem(
                                id = id,
                                name = name.ifBlank { rawNumber },
                                number = rawNumber,
                                email = email,
                                photoUri = photoUri,
                                phoneType = type,
                                accountType = accountType
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }

    private fun fetchEmailForContact(context: Context, contactId: Long): String? {
        return try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                null
            )?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteContactGlobally(
        context: Context,
        contactId: Long,
        name: String,
        phone: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext false
        }
        var success = false
        val cleanPhone = phone.replace("[^0-9+]".toRegex(), "")

        try {
            if (contactId > 0) {
                val contactUri = android.content.ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
                val deletedRows = context.contentResolver.delete(contactUri, null, null)
                if (deletedRows > 0) success = true

                val rawOps = ArrayList<ContentProviderOperation>()
                rawOps.add(
                    ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                        .withSelection("${ContactsContract.RawContacts.CONTACT_ID} = ?", arrayOf(contactId.toString()))
                        .build()
                )
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, rawOps)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (cleanPhone.isNotEmpty()) {
                val dataUri = ContactsContract.Data.CONTENT_URI
                val cursor = context.contentResolver.query(
                    dataUri,
                    arrayOf(ContactsContract.Data.RAW_CONTACT_ID),
                    "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?",
                    arrayOf("%$cleanPhone%"),
                    null
                )
                cursor?.use {
                    while (it.moveToNext()) {
                        val rawId = it.getLong(0)
                        val rawUri = android.content.ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawId)
                        val rows = context.contentResolver.delete(rawUri, null, null)
                        if (rows > 0) success = true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val simUris = listOf(
                Uri.parse("content://icc/adn"),
                Uri.parse("content://sim/adn"),
                Uri.parse("content://icc/adn/subId/1"),
                Uri.parse("content://icc/adn/subId/2")
            )
            val cleanName = name.replace("'", "''")
            val cleanNum = cleanPhone.replace("'", "''")
            for (simUri in simUris) {
                try {
                    val simWhere = "tag='$cleanName' AND number='$cleanNum'"
                    val simDeleted = context.contentResolver.delete(simUri, simWhere, null)
                    if (simDeleted > 0) success = true
                } catch (_: Exception) {}

                try {
                    val simWhere2 = "number='$cleanNum'"
                    val simDeleted2 = context.contentResolver.delete(simUri, simWhere2, null)
                    if (simDeleted2 > 0) success = true
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        success
    }
}
