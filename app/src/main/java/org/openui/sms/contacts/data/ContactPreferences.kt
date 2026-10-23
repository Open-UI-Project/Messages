package org.openui.sms.contacts.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject
import org.openui.sms.contacts.model.ContactItem

data class TrashContact(
    val id: Long,
    val name: String,
    val number: String,
    val email: String? = null,
    val photoUri: String? = null,
    val accountType: String? = null,
    val deletedTimestamp: Long = System.currentTimeMillis()
)

class ContactPreferences(private val context: Context) {
    private val localPrefs: SharedPreferences = context.getSharedPreferences("open_sms_contact_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val CONTACTS_APP_PKG = "org.openui.contacts"
        private const val CONTACTS_PROVIDER_URI = "content://org.openui.contacts.provider"
        private const val SMS_PROVIDER_URI = "content://org.openui.sms.contacts.provider"

        private const val METHOD_GET_PREF = "get_pref"
        private const val METHOD_SET_PREF = "set_pref"
        private const val METHOD_REMOVE_PREF = "remove_pref"

        private const val KEY_PREF_VALUE = "value"
        private const val KEY_PREF_TYPE = "type"
    }

    private fun getSharedString(key: String, defaultValue: String?): String? {
        try {
            val uri = Uri.parse(CONTACTS_PROVIDER_URI)
            val res = context.contentResolver.call(uri, METHOD_GET_PREF, key, null)
            if (res != null && res.containsKey(KEY_PREF_VALUE)) {
                return res.getString(KEY_PREF_VALUE) ?: defaultValue
            }
        } catch (_: Exception) {
        }

        try {
            val otherContext = context.createPackageContext(CONTACTS_APP_PKG, Context.CONTEXT_IGNORE_SECURITY)
            val otherPrefs = otherContext.getSharedPreferences("open_sms_contact_prefs", Context.MODE_PRIVATE)
            if (otherPrefs.contains(key)) {
                return otherPrefs.getString(key, defaultValue)
            }
        } catch (_: Exception) {
        }

        return localPrefs.getString(key, defaultValue)
    }

    private fun putSharedString(key: String, value: String?) {
        if (value == null) {
            localPrefs.edit().remove(key).apply()
        } else {
            localPrefs.edit().putString(key, value).apply()
        }

        try {
            val uri = Uri.parse(CONTACTS_PROVIDER_URI)
            val extras = Bundle().apply {
                putString(KEY_PREF_TYPE, "string")
                putString(KEY_PREF_VALUE, value)
            }
            if (value == null) {
                context.contentResolver.call(uri, METHOD_REMOVE_PREF, key, null)
            } else {
                context.contentResolver.call(uri, METHOD_SET_PREF, key, extras)
            }
        } catch (_: Exception) {
        }

        try {
            val otherContext = context.createPackageContext(CONTACTS_APP_PKG, Context.CONTEXT_IGNORE_SECURITY)
            val otherPrefs = otherContext.getSharedPreferences("open_sms_contact_prefs", Context.MODE_PRIVATE)
            if (value == null) {
                otherPrefs.edit().remove(key).apply()
            } else {
                otherPrefs.edit().putString(key, value).apply()
            }
        } catch (_: Exception) {
        }
    }

    private fun getSharedStringSet(key: String): Set<String> {
        try {
            val uri = Uri.parse(CONTACTS_PROVIDER_URI)
            val res = context.contentResolver.call(uri, METHOD_GET_PREF, key, null)
            if (res != null && res.containsKey(KEY_PREF_VALUE)) {
                val list = res.getStringArrayList(KEY_PREF_VALUE)
                if (list != null) return list.toSet()
            }
        } catch (_: Exception) {
        }

        try {
            val otherContext = context.createPackageContext(CONTACTS_APP_PKG, Context.CONTEXT_IGNORE_SECURITY)
            val otherPrefs = otherContext.getSharedPreferences("open_sms_contact_prefs", Context.MODE_PRIVATE)
            if (otherPrefs.contains(key)) {
                return otherPrefs.getStringSet(key, emptySet()) ?: emptySet()
            }
        } catch (_: Exception) {
        }

        return localPrefs.getStringSet(key, emptySet()) ?: emptySet()
    }

    private fun putSharedStringSet(key: String, set: Set<String>) {
        localPrefs.edit().putStringSet(key, set).apply()

        try {
            val uri = Uri.parse(CONTACTS_PROVIDER_URI)
            val extras = Bundle().apply {
                putString(KEY_PREF_TYPE, "string_set")
                putStringArrayList(KEY_PREF_VALUE, ArrayList(set))
            }
            context.contentResolver.call(uri, METHOD_SET_PREF, key, extras)
        } catch (_: Exception) {
        }

        try {
            val otherContext = context.createPackageContext(CONTACTS_APP_PKG, Context.CONTEXT_IGNORE_SECURITY)
            val otherPrefs = otherContext.getSharedPreferences("open_sms_contact_prefs", Context.MODE_PRIVATE)
            otherPrefs.edit().putStringSet(key, set).apply()
        } catch (_: Exception) {
        }
    }

    fun getFavoriteNumbers(): Set<String> {
        return getSharedStringSet("favorites")
    }

    fun isFavorite(number: String): Boolean {
        if (number.isBlank()) return false
        val clean = cleanNumber(number)
        return getFavoriteNumbers().any { cleanNumber(it) == clean }
    }

    fun toggleFavorite(number: String): Boolean {
        if (number.isBlank()) return false
        val clean = cleanNumber(number)
        val current = getFavoriteNumbers().toMutableSet()
        val existing = current.find { cleanNumber(it) == clean }
        val nowFav: Boolean
        if (existing != null) {
            current.remove(existing)
            nowFav = false
        } else {
            current.add(clean)
            nowFav = true
        }
        putSharedStringSet("favorites", current)
        return nowFav
    }

    fun getTrashContacts(): List<TrashContact> {
        val jsonStr = getSharedString("trash_contacts_json", null) ?: return emptyList()
        val list = mutableListOf<TrashContact>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    TrashContact(
                        id = obj.optLong("id", 0L),
                        name = obj.optString("name", ""),
                        number = obj.optString("number", ""),
                        email = if (obj.isNull("email")) null else obj.optString("email"),
                        photoUri = if (obj.isNull("photoUri")) null else obj.optString("photoUri"),
                        accountType = if (obj.isNull("accountType")) null else obj.optString("accountType"),
                        deletedTimestamp = obj.optLong("deletedTimestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun moveToTrash(contact: ContactItem) {
        val current = getTrashContacts().toMutableList()
        val cleanNum = cleanNumber(contact.number)
        current.removeAll { cleanNumber(it.number) == cleanNum || (it.id != 0L && it.id == contact.id) }
        current.add(
            TrashContact(
                id = contact.id,
                name = contact.name,
                number = contact.number,
                email = contact.email,
                photoUri = contact.photoUri,
                accountType = contact.accountType
            )
        )
        saveTrashContacts(current)
    }

    fun moveToTrash(number: String) {
        if (number.isBlank()) return
        val current = getTrashContacts().toMutableList()
        val cleanNum = cleanNumber(number)
        if (current.none { cleanNumber(it.number) == cleanNum }) {
            current.add(
                TrashContact(
                    id = 0L,
                    name = number,
                    number = number
                )
            )
            saveTrashContacts(current)
        }
    }

    fun restoreFromTrash(number: String) {
        val current = getTrashContacts().toMutableList()
        val cleanNum = cleanNumber(number)
        current.removeAll { cleanNumber(it.number) == cleanNum }
        saveTrashContacts(current)
    }

    fun removeFromTrash(number: String) {
        val current = getTrashContacts().toMutableList()
        val cleanNum = cleanNumber(number)
        current.removeAll { cleanNumber(it.number) == cleanNum }
        saveTrashContacts(current)
    }

    fun isDeleted(number: String): Boolean {
        if (number.isBlank()) return false
        val clean = cleanNumber(number)
        return getTrashContacts().any { cleanNumber(it.number) == clean }
    }

    fun isContactInTrash(id: Long, number: String): Boolean {
        val clean = cleanNumber(number)
        return getTrashContacts().any {
            (clean.isNotBlank() && cleanNumber(it.number) == clean) || (id != 0L && it.id == id)
        }
    }

    fun clearTrash() {
        putSharedString("trash_contacts_json", null)
    }

    private fun saveTrashContacts(list: List<TrashContact>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("name", item.name)
                obj.put("number", item.number)
                obj.put("email", item.email)
                obj.put("photoUri", item.photoUri)
                obj.put("accountType", item.accountType)
                obj.put("deletedTimestamp", item.deletedTimestamp)
                arr.put(obj)
            }
            putSharedString("trash_contacts_json", arr.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getBlockedNumbers(): Set<String> {
        return getSharedStringSet("blocked_numbers")
    }

    fun isBlocked(number: String): Boolean {
        if (number.isBlank()) return false
        val clean = cleanNumber(number)
        return getBlockedNumbers().any { cleanNumber(it) == clean }
    }

    fun toggleBlock(number: String): Boolean {
        if (number.isBlank()) return false
        val clean = cleanNumber(number)
        val current = getBlockedNumbers().toMutableSet()
        val existing = current.find { cleanNumber(it) == clean }
        val nowBlocked: Boolean
        if (existing != null) {
            current.remove(existing)
            nowBlocked = false
        } else {
            current.add(clean)
            nowBlocked = true
        }
        putSharedStringSet("blocked_numbers", current)
        return nowBlocked
    }

    fun getStorageTarget(): String {
        return getSharedString("storage_target", "DEVICE") ?: "DEVICE"
    }

    fun setStorageTarget(target: String) {
        putSharedString("storage_target", target)
    }

    fun getCustomBackground(number: String): String? {
        return getSharedString("custom_bg_${cleanNumber(number)}", null)
    }

    fun setCustomBackground(number: String, uriString: String?) {
        val clean = cleanNumber(number)
        putSharedString("custom_bg_$clean", uriString)
    }

    fun getSocialMedia(number: String): String {
        return getSharedString("social_${cleanNumber(number)}", "") ?: ""
    }

    fun setSocialMedia(number: String, social: String) {
        val clean = cleanNumber(number)
        putSharedString("social_$clean", social.ifBlank { null })
    }

    private fun cleanNumber(number: String): String {
        return number.replace("[^0-9+]".toRegex(), "")
    }
}
