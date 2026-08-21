package org.openui.sms.contacts.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

private val AvatarColorsArray = arrayOf(
    Color(0xFF007AFF),
    Color(0xFF34C759),
    Color(0xFFFF9500),
    Color(0xFFAF52DE),
    Color(0xFFFF2D55),
    Color(0xFF5856D6),
    Color(0xFF00C7BE),
    Color(0xFFFF3B30)
)

@Immutable
data class ContactItem(
    val id: Long,
    val name: String,
    val number: String,
    val email: String? = null,
    val isFavorite: Boolean = false,
    val photoUri: String? = null,
    val phoneType: String = "",
    val accountType: String = "",
    val initials: String = computeInitials(name)
)

private fun computeInitials(name: String): String {
    return name
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .take(2)
        .joinToString("")
}

fun getAvatarColor(name: String): Color {
    val hash = name.fold(0) { acc, char -> acc + char.code }
    return AvatarColorsArray[kotlin.math.abs(hash) % AvatarColorsArray.size]
}
