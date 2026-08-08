package org.openui.sms.sms.model

import androidx.compose.ui.graphics.Color

data class ContactItem(
    val id: Long,
    val name: String,
    val number: String,
    val isFavorite: Boolean = false,
    val photoUri: String? = null,
    val phoneType: String = "" // Handled in UI
) {
    val initials: String
        get() = name
            .split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .take(2)
            .joinToString("")
}

fun getAvatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFF007AFF), // Синий
        Color(0xFF34C759), // Зеленый
        Color(0xFFFF9500), // Оранжевый
        Color(0xFFAF52DE), // Фиолетовый
        Color(0xFFFF2D55), // Розовый
        Color(0xFF5856D6), // Индиго
        Color(0xFF00C7BE), // Бирюзовый
        Color(0xFFFF3B30)  // Красный
    )
    val hash = name.fold(0) { acc, char -> acc + char.code }
    return colors[kotlin.math.abs(hash) % colors.size]
}
