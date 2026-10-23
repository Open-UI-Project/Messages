package org.openui.sms.contacts.data

import android.content.Context
import android.content.SharedPreferences

class ContactPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("open_sms_contact_prefs", Context.MODE_PRIVATE)

    fun getFavoriteNumbers(): Set<String> {
        return prefs.getStringSet("favorites", emptySet()) ?: emptySet()
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
        prefs.edit().putStringSet("favorites", current).apply()
        return nowFav
    }

    fun getBlockedNumbers(): Set<String> {
        return prefs.getStringSet("blocked", emptySet()) ?: emptySet()
    }

    fun isBlocked(number: String): Boolean {
        if (number.isBlank()) return false
        val clean = cleanNumber(number)
        return getBlockedNumbers().any { cleanNumber(it) == clean }
    }

    fun toggleBlocked(number: String): Boolean {
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
        prefs.edit().putStringSet("blocked", current).apply()
        return nowBlocked
    }

    fun getTrashNumbers(): Set<String> {
        return prefs.getStringSet("trash_numbers", emptySet()) ?: emptySet()
    }

    fun moveToTrash(number: String) {
        if (number.isBlank()) return
        val clean = cleanNumber(number)
        val current = getTrashNumbers().toMutableSet()
        current.add(clean)
        prefs.edit().putStringSet("trash_numbers", current).apply()
    }

    fun restoreFromTrash(number: String) {
        if (number.isBlank()) return
        val clean = cleanNumber(number)
        val current = getTrashNumbers().toMutableSet()
        current.removeAll { cleanNumber(it) == clean }
        prefs.edit().putStringSet("trash_numbers", current).apply()
    }

    fun isDeleted(number: String): Boolean {
        if (number.isBlank()) return false
        val clean = cleanNumber(number)
        return getTrashNumbers().any { cleanNumber(it) == clean }
    }

    fun clearTrash() {
        prefs.edit().remove("trash_numbers").apply()
    }

    private fun cleanNumber(number: String): String {
        return number.replace("[^0-9+]".toRegex(), "")
    }
}
