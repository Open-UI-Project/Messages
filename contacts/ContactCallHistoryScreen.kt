package org.openui.sms.sms.ui.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openui.sms.sms.data.ContactPreferences
import org.openui.sms.sms.model.CallLogItem
import org.openui.sms.sms.model.CallType
import org.openui.sms.sms.model.ContactItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ContactCallHistoryScreen(
    contact: ContactItem,
    onBackClick: () -> Unit,
    onFetchCallLogs: suspend (String) -> List<CallLogItem>,
    onDeleteContact: (ContactItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) { ContactPreferences(context) }

    var isFavorite by remember { mutableStateOf(prefs.isFavorite(contact.number)) }
    var isBlocked by remember { mutableStateOf(prefs.isBlocked(contact.number)) }
    var callLogs by remember { mutableStateOf<List<CallLogItem>?>(null) }

    var pendingCallNumber by remember { mutableStateOf<String?>(null) }

    val makeDirectCall: (String) -> Unit = remember(context) {
        { phoneNumber ->
            try {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
            } catch (e: Exception) {
                try {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(dialIntent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Не удалось совершить вызов", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val numberToCall = pendingCallNumber
        pendingCallNumber = null
        if (isGranted && !numberToCall.isNullOrBlank()) {
            makeDirectCall(numberToCall)
        } else if (!numberToCall.isNullOrBlank()) {
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numberToCall")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
            } catch (_: Exception) {}
        }
    }

    val initiateCall: (String) -> Unit = remember(context) {
        { phoneNumber ->
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                makeDirectCall(phoneNumber)
            } else {
                pendingCallNumber = phoneNumber
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }
    }

    LaunchedEffect(contact.number) {
        callLogs = onFetchCallLogs(contact.number)
    }

    BackHandler {
        onBackClick()
    }

    val screenBg = Color(0xFF000000)
    val cardBg = Color(0xFF1E1E22)
    val textMuted = Color(0xFF8E8E93)
    val dividerColor = Color(0xFF292930)
    val greenColor = Color(0xFF2E8B57)
    val redColor = Color(0xFFFF3B30)

    val groupedCallLogs = remember(callLogs) {
        val logs = callLogs ?: emptyList()
        logs.groupBy { item -> formatDateHeader(item.timestamp) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = contact.name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = contact.number,
                        color = textMuted,
                        fontSize = 13.sp
                    )
                }

                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Информация",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

            // Content
            if (callLogs == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = greenColor)
                }
            } else if (callLogs!!.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "История вызовов отсутствует",
                        color = textMuted,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedCallLogs.forEach { (header, logs) ->
                        item(key = "header_$header") {
                            Text(
                                text = header,
                                color = textMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 8.dp)
                            )
                        }

                        item(key = "group_$header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(cardBg)
                            ) {
                                logs.forEachIndexed { index, item ->
                                    CallLogEntryRow(
                                        item = item,
                                        onClick = { initiateCall(contact.number) }
                                    )
                                    if (index < logs.size - 1) {
                                        HorizontalDivider(
                                            color = dividerColor,
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(start = 60.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(110.dp))
                    }
                }
            }
        }

        // Bottom Action Pill Toolbar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xCC202026))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorite
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            isFavorite = prefs.toggleFavorite(contact.number)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Избранное",
                        tint = if (isFavorite) Color(0xFFFFB800) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Избранное",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

                // Share
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${contact.name}: ${contact.number}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Поделиться контактом"))
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Поделиться",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Поделиться",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

                // Block / Unblock
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            isBlocked = prefs.toggleBlocked(contact.number)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = if (isBlocked) "Разблокировать" else "Заблокировать",
                        tint = if (isBlocked) redColor else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isBlocked) "Разблокиров." else "Заблокиров.",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

                // Delete (Trash)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            prefs.moveToTrash(contact.number)
                            onDeleteContact(contact)
                            onBackClick()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Удалить",
                        tint = redColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Удалить",
                        color = redColor,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private data class CallRowStyle(
    val icon: ImageVector,
    val tint: Color,
    val text: String
)

@Composable
private fun CallLogEntryRow(
    item: CallLogItem,
    onClick: () -> Unit
) {
    val greenColor = Color(0xFF2E8B57)
    val redColor = Color(0xFFFF3B30)
    val textMuted = Color(0xFF8E8E93)

    val timeFormatted = remember(item.timestamp) {
        SimpleDateFormat("HH:mm", Locale("ru")).format(Date(item.timestamp))
    }

    val style = remember(item.type, item.durationSeconds) {
        when (item.type) {
            CallType.OUTGOING -> CallRowStyle(
                icon = Icons.AutoMirrored.Rounded.CallMade,
                tint = greenColor,
                text = "Исходящий вызов, ${formatDuration(item.durationSeconds)}"
            )
            CallType.INCOMING -> CallRowStyle(
                icon = Icons.AutoMirrored.Rounded.CallReceived,
                tint = Color.White,
                text = "Входящий вызов, ${formatDuration(item.durationSeconds)}"
            )
            CallType.MISSED -> CallRowStyle(
                icon = Icons.AutoMirrored.Rounded.CallReceived,
                tint = redColor,
                text = "Пропущенный вызов"
            )
            CallType.REJECTED -> CallRowStyle(
                icon = Icons.AutoMirrored.Rounded.CallReceived,
                tint = redColor,
                text = "Отклоненный вызов"
            )
            CallType.UNKNOWN -> CallRowStyle(
                icon = Icons.AutoMirrored.Rounded.CallReceived,
                tint = textMuted,
                text = "Вызов, ${formatDuration(item.durationSeconds)}"
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = timeFormatted,
            color = textMuted,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.tint,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = style.text,
                color = if (item.type == CallType.MISSED || item.type == CallType.REJECTED) redColor else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatDuration(durationSeconds: Long): String {
    if (durationSeconds <= 0) return "0 сек."
    val mins = durationSeconds / 60
    val secs = durationSeconds % 60
    return when {
        mins > 0 && secs > 0 -> "$mins мин. $secs сек."
        mins > 0 -> "$mins мин."
        else -> "$secs сек."
    }
}

private fun formatDateHeader(timestamp: Long): String {
    val cal = Calendar.getInstance()
    val today = Calendar.getInstance()
    cal.timeInMillis = timestamp

    val formatTime = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))

    return when {
        isSameDay(cal, today) -> "Сегодня"
        isYesterday(cal, today) -> "Вчера"
        else -> formatTime.format(Date(timestamp))
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
    val yesterday = cal2.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    return isSameDay(cal1, yesterday)
}
