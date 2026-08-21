package org.openui.sms.contacts.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import org.openui.sms.R
import org.openui.sms.ui.theme.AppTheme
import kotlinx.coroutines.launch
import org.openui.sms.contacts.data.ContactManager
import org.openui.sms.contacts.data.ContactPreferences
import org.openui.sms.contacts.model.CallLogItem
import org.openui.sms.contacts.model.CallType
import org.openui.sms.contacts.model.ContactItem
import org.openui.sms.contacts.model.getAvatarColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val CardShape20 = RoundedCornerShape(20.dp)
private val BottomToolbarShape = RoundedCornerShape(32.dp)

@Composable
fun ContactCallHistoryScreen(
    contact: ContactItem,
    onBackClick: () -> Unit,
    onFetchCallLogs: suspend (String) -> List<CallLogItem>,
    onDeleteContact: (ContactItem) -> Unit,
    onContactUpdated: () -> Unit = {},
    onSmsClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val prefs = remember(context) { ContactPreferences(context) }
    val coroutineScope = rememberCoroutineScope()

    var currentContact by remember { mutableStateOf(contact) }
    var isFavorite by remember { mutableStateOf(prefs.isFavorite(currentContact.number)) }
    var isBlocked by remember { mutableStateOf(prefs.isBlocked(currentContact.number)) }
    var customBgUri by remember { mutableStateOf(prefs.getCustomBackground(currentContact.number)) }
    var socialMedia by remember { mutableStateOf(prefs.getSocialMedia(currentContact.number)) }

    var callLogs by remember { mutableStateOf<List<CallLogItem>?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }

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
                    Toast.makeText(context, context.getString(R.string.contacts_call_failed), Toast.LENGTH_SHORT).show()
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

    LaunchedEffect(currentContact.number) {
        callLogs = onFetchCallLogs(currentContact.number)
    }

    BackHandler {
        onBackClick()
    }

    val avatarColor = remember(currentContact.name) { getAvatarColor(currentContact.name) }

    val groupedCallLogs = remember(callLogs, context) {
        val logs = callLogs ?: emptyList()
        logs.groupBy { item -> formatDateHeader(context, item.timestamp) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (customBgUri != null) {
            AsyncImage(
                model = customBgUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (colors.isDark) Color.Black.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.75f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.contacts_close),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { showEditDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.contacts_edit_contact),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentContact.photoUri != null) {
                        AsyncImage(
                            model = currentContact.photoUri,
                            contentDescription = currentContact.name,
                            modifier = Modifier.size(80.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = currentContact.initials.ifBlank { "#" },
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentContact.name,
                    color = colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = currentContact.number,
                    color = colors.textMuted,
                    fontSize = 14.sp
                )

                if (!currentContact.email.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentContact.email!!,
                        color = colors.primaryBrand,
                        fontSize = 13.sp
                    )
                }

                if (socialMedia.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = socialMedia,
                        color = colors.dialerGreen,
                        fontSize = 12.sp
                    )
                }

                if (isBlocked) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.contacts_in_blacklist),
                        color = colors.actionRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { initiateCall(currentContact.number) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.cardElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = stringResource(R.string.contacts_call),
                            tint = colors.dialerGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    IconButton(
                        onClick = {
                            if (onSmsClick != null) {
                                onSmsClick(currentContact.number)
                            } else {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${currentContact.number}"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.cardElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubble,
                            contentDescription = stringResource(R.string.contacts_message),
                            tint = Color(0xFF2A7FF6),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.divider, thickness = 0.5.dp)

            when {
                callLogs == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.dialerGreen)
                    }
                }
                callLogs!!.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.contacts_no_call_history),
                            color = colors.textMuted,
                            fontSize = 15.sp
                        )
                    }
                }
                else -> {
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
                                    color = colors.textMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp, start = 8.dp)
                                )
                            }

                            item(key = "group_$header") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(CardShape20)
                                        .background(colors.card)
                                ) {
                                    logs.forEachIndexed { index, item ->
                                        CallLogEntryRow(
                                            item = item,
                                            onClick = { initiateCall(currentContact.number) }
                                        )
                                        if (index < logs.size - 1) {
                                            HorizontalDivider(
                                                color = colors.divider,
                                                thickness = 0.5.dp,
                                                modifier = Modifier.padding(start = 60.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "footer_spacer") {
                            Spacer(modifier = Modifier.height(110.dp))
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(BottomToolbarShape)
                    .background(colors.surface.copy(alpha = 0.95f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            isFavorite = prefs.toggleFavorite(currentContact.number)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Outlined.StarBorder,
                        contentDescription = stringResource(R.string.contacts_favorite_btn),
                        tint = if (isFavorite) Color(0xFFFFB800) else colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.contacts_favorite_btn),
                        color = colors.textPrimary,
                        fontSize = 11.sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${currentContact.name}: ${currentContact.number}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.contacts_share)))
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.contacts_share),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.contacts_share),
                        color = colors.textPrimary,
                        fontSize = 11.sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            showBlockDialog = true
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = if (isBlocked) stringResource(R.string.contacts_unblock_btn) else stringResource(R.string.contacts_block_btn),
                        tint = if (isBlocked) colors.actionRed else colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isBlocked) stringResource(R.string.contacts_unblock_btn) else stringResource(R.string.contacts_block_btn),
                        color = if (isBlocked) colors.actionRed else colors.textPrimary,
                        fontSize = 11.sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            prefs.moveToTrash(currentContact)
                            onDeleteContact(currentContact)
                            onBackClick()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.contacts_delete),
                        tint = colors.actionRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.contacts_delete),
                        color = colors.actionRed,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (showEditDialog) {
            EditContactDialog(
                contact = currentContact,
                currentSocialLink = socialMedia,
                currentBackgroundUri = customBgUri,
                onDismiss = { showEditDialog = false },
                onSave = { name, number, email, photoUri, social, newBgUri ->
                    showEditDialog = false
                    coroutineScope.launch {
                        val success = ContactManager.updateContact(
                            context = context,
                            contactId = currentContact.id,
                            name = name,
                            phone = number,
                            email = email,
                            photoUri = photoUri
                        )
                        if (success) {
                            prefs.setSocialMedia(number, social)
                            socialMedia = social
                            prefs.setCustomBackground(number, newBgUri)
                            customBgUri = newBgUri
                            currentContact = currentContact.copy(
                                name = name,
                                number = number,
                                email = email,
                                photoUri = photoUri?.toString() ?: currentContact.photoUri
                            )
                            Toast.makeText(context, context.getString(R.string.contacts_updated_success), Toast.LENGTH_SHORT).show()
                            onContactUpdated()
                        } else {
                            Toast.makeText(context, context.getString(R.string.contacts_updated_error), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        if (showBlockDialog) {
            AlertDialog(
                onDismissRequest = { showBlockDialog = false },
                containerColor = colors.surface,
                title = {
                    Text(
                        text = if (isBlocked) stringResource(R.string.contacts_unblock_dialog_title) else stringResource(R.string.contacts_block_dialog_title),
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (isBlocked)
                            stringResource(R.string.contacts_unblock_dialog_desc, currentContact.name)
                        else
                            stringResource(R.string.contacts_block_dialog_desc, currentContact.name, currentContact.number),
                        color = colors.textMuted,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isBlocked = prefs.toggleBlock(currentContact.number)
                            showBlockDialog = false
                            val msg = if (isBlocked) context.getString(R.string.contacts_blocked_success) else context.getString(R.string.contacts_unblocked_success)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(
                            text = if (isBlocked) stringResource(R.string.contacts_unblock_btn) else stringResource(R.string.contacts_block_btn),
                            color = if (isBlocked) colors.dialerGreen else colors.actionRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBlockDialog = false }) {
                        Text(stringResource(R.string.contacts_cancel), color = colors.textMuted)
                    }
                }
            )
        }
    }
}

@Immutable
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
    val colors = AppTheme.colors
    val context = LocalContext.current
    val timeFormatted = remember(item.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.timestamp))
    }

    val style = remember(item.type, item.durationSeconds, context, colors) {
        val durText = formatDuration(context, item.durationSeconds)
        when (item.type) {
            CallType.OUTGOING -> CallRowStyle(
                icon = Icons.AutoMirrored.Rounded.CallMade,
                tint = colors.dialerGreen,
                text = context.getString(R.string.call_outgoing, durText)
            )
            CallType.INCOMING -> CallRowStyle(
                icon = Icons.AutoMirrored.Rounded.CallReceived,
                tint = colors.textPrimary,
                text = context.getString(R.string.call_incoming, durText)
            )
            CallType.MISSED -> CallRowStyle(
                icon = Icons.AutoMirrored.Rounded.CallReceived,
                tint = colors.actionRed,
                text = context.getString(R.string.call_missed)
            )
            CallType.REJECTED -> CallRowStyle(
                icon = Icons.AutoMirrored.Rounded.CallReceived,
                tint = colors.actionRed,
                text = context.getString(R.string.call_rejected)
            )
            CallType.UNKNOWN -> CallRowStyle(
                icon = Icons.AutoMirrored.Rounded.CallReceived,
                tint = colors.textMuted,
                text = context.getString(R.string.call_unknown, durText)
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
            color = colors.textMuted,
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
                color = if (item.type == CallType.MISSED || item.type == CallType.REJECTED) colors.actionRed else colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatDuration(context: Context, durationSeconds: Long): String {
    if (durationSeconds <= 0) return context.getString(R.string.call_duration_zero)
    val mins = durationSeconds / 60
    val secs = durationSeconds % 60
    return when {
        mins > 0 && secs > 0 -> context.getString(R.string.call_duration_min_sec, mins, secs)
        mins > 0 -> context.getString(R.string.call_duration_min, mins)
        else -> context.getString(R.string.call_duration_sec, secs)
    }
}

private fun formatDateHeader(context: Context, timestamp: Long): String {
    val cal = Calendar.getInstance()
    val today = Calendar.getInstance()
    cal.timeInMillis = timestamp

    val formatTime = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())

    return when {
        isSameDay(cal, today) -> context.getString(R.string.call_today)
        isYesterday(cal, today) -> context.getString(R.string.call_yesterday)
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
