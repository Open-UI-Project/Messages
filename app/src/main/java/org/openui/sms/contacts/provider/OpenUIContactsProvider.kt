package org.openui.sms.contacts.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle

/**
 * OpenUIContactsProvider allows shared ecosystem state between Open UI Contacts (org.openui.contacts)
 * and Open UI SMS (org.openui.sms), when installed on the same device and signed with the same key.
 */
class OpenUIContactsProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "org.openui.sms.contacts.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")

        const val PREFS_NAME = "open_sms_contact_prefs"

        const val METHOD_GET_PREF = "get_pref"
        const val METHOD_SET_PREF = "set_pref"
        const val METHOD_REMOVE_PREF = "remove_pref"
        const val METHOD_GET_ALL = "get_all"

        const val KEY_PREF_KEY = "key"
        const val KEY_PREF_VALUE = "value"
        const val KEY_PREF_TYPE = "type" // string, string_set, etc.
    }

    private var prefs: SharedPreferences? = null

    override fun onCreate(): Boolean {
        context?.let {
            prefs = it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(arrayOf("key", "value"))
        prefs?.all?.forEach { (k, v) ->
            cursor.addRow(arrayOf(k, v?.toString() ?: ""))
        }
        return cursor
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val p = prefs ?: return null
        val response = Bundle()
        when (method) {
            METHOD_GET_PREF -> {
                val key = arg ?: return null
                if (p.contains(key)) {
                    val all = p.all
                    val value = all[key]
                    if (value is Set<*>) {
                        response.putStringArrayList(KEY_PREF_VALUE, ArrayList(value.map { it.toString() }))
                        response.putString(KEY_PREF_TYPE, "string_set")
                    } else {
                        response.putString(KEY_PREF_VALUE, value?.toString())
                        response.putString(KEY_PREF_TYPE, "string")
                    }
                }
                return response
            }
            METHOD_SET_PREF -> {
                val key = arg ?: return null
                val type = extras?.getString(KEY_PREF_TYPE) ?: "string"
                val editor = p.edit()
                if (type == "string_set") {
                    val list = extras?.getStringArrayList(KEY_PREF_VALUE) ?: arrayListOf()
                    editor.putStringSet(key, list.toSet())
                } else {
                    val value = extras?.getString(KEY_PREF_VALUE)
                    if (value != null) {
                        editor.putString(key, value)
                    }
                }
                editor.apply()
                context?.contentResolver?.notifyChange(CONTENT_URI, null)
                response.putBoolean("success", true)
                return response
            }
            METHOD_REMOVE_PREF -> {
                val key = arg ?: return null
                p.edit().remove(key).apply()
                context?.contentResolver?.notifyChange(CONTENT_URI, null)
                response.putBoolean("success", true)
                return response
            }
        }
        return super.call(method, arg, extras)
    }

    override fun getType(uri: Uri): String? = "vnd.android.cursor.dir/org.openui.contacts"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
