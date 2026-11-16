package org.openui.sms.sms.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.openui.sms.R
import org.openui.sms.data.AppSettings
import org.openui.sms.sms.model.MessageBoxType
import org.openui.sms.sms.model.SmsMessage
import org.openui.sms.ui.theme.AppTheme
import org.openui.sms.ui.theme.OpenUIAvatarGrey

private val EmojisList = listOf("😊", "👍", "❤️", "😂", "🔥", "🙏", "🎉", "👋", "🥳", "💡", "📱", "🤣", "🙌", "⭐")

private sealed interface ThreadListItem {
    val key: String
    data class DateHeader(val text: String, override val key: String) : ThreadListItem
    data class MessageBubbleItem(val message: SmsMessage, override val key: String) : ThreadListItem
}

@Composable
fun SmsThreadScreen(
    title: String,
    messages: List<SmsMessage>,
    onBackClick: () -> Unit,
    onSendClick: (String) -> Unit,
    onDeleteThreadClick: () -> Unit,
    onDeleteMessageClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val appSettings = remember(context) { AppSettings.getInstance(context) }
    val wallpaperUri by appSettings.wallpaperUri.collectAsState()
    val isWallpaperActive = wallpaperUri != null

    val prefs = remember(context) { context.getSharedPreferences("sms_drafts", Context.MODE_PRIVATE) }
    val draftKey = remember(title) { "draft_${title.hashCode()}" }

    var inputText by remember(title) { mutableStateOf(prefs.getString(draftKey, "") ?: "") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val contactPhotoUri = remember(messages) {
        messages.firstOrNull { !it.photoUri.isNullOrBlank() }?.photoUri
    }

    val initial = remember(title) {
        val trimmed = title.trim()
        val firstChar = trimmed.firstOrNull()
        if (firstChar != null && (firstChar.isLetter() || firstChar in 'А'..'Я' || firstChar == 'Ё')) {
            firstChar.uppercaseChar().toString()
        } else {
            ""
        }
    }

    val threadItems = remember(messages) {
        val itemsList = mutableListOf<ThreadListItem>()
        messages.forEachIndexed { index, message ->
            val showHeader = index == 0 || message.formattedDayHeader != messages[index - 1].formattedDayHeader
            if (showHeader) {
                itemsList.add(ThreadListItem.DateHeader(message.formattedDayHeader, "header_${message.timestamp}_$index"))
            }
            itemsList.add(ThreadListItem.MessageBubbleItem(message, "msg_${message.id}_$index"))
        }
        itemsList
    }

    LaunchedEffect(inputText) {
        if (inputText.isBlank()) {
            prefs.edit().remove(draftKey).apply()
        } else {
            prefs.edit().putString(draftKey, inputText).apply()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isWallpaperActive) Color.Transparent else colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isWallpaperActive) colors.surface.copy(alpha = 0.88f) else colors.surface)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = colors.textPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !contactPhotoUri.isNullOrBlank() -> Color.Transparent
                                colors.isExpressive -> MaterialTheme.colorScheme.primaryContainer
                                else -> OpenUIAvatarGrey
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!contactPhotoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = contactPhotoUri,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (colors.isExpressive && initial.isNotBlank()) {
                        Text(
                            text = initial,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = if (colors.isExpressive) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                Color.White.copy(alpha = 0.9f)
                            },
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onDeleteThreadClick) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "top_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(
                items = threadItems,
                key = { it.key },
                contentType = {
                    when (it) {
                        is ThreadListItem.DateHeader -> "date_header"
                        is ThreadListItem.MessageBubbleItem -> "message_bubble"
                    }
                }
            ) { item ->
                when (item) {
                    is ThreadListItem.DateHeader -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.text,
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    is ThreadListItem.MessageBubbleItem -> {
                        MessageBubbleRow(
                            message = item.message,
                            colors = colors,
                            onForwardClick = { body ->
                                inputText = body
                            },
                            onDeleteClick = {
                                onDeleteMessageClick(item.message.id)
                            }
                        )
                    }
                }
            }

            item(key = "bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (showEmojiPicker) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(vertical = 8.dp)
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(EmojisList) { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { inputText += emoji }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isWallpaperActive) colors.surface.copy(alpha = 0.90f) else colors.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.contacts_message),
                        color = colors.textSecondary,
                        fontSize = 15.sp
                    )
                },
                singleLine = false,
                maxLines = 4,
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showEmojiPicker = !showEmojiPicker },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SentimentSatisfied,
                                contentDescription = null,
                                tint = if (showEmojiPicker) colors.primaryBrand else colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedBorderColor = colors.card,
                    unfocusedBorderColor = colors.card,
                    focusedContainerColor = colors.card,
                    unfocusedContainerColor = colors.card
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank()) colors.primaryBrand else colors.card
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            prefs.edit().remove(draftKey).apply()
                            onSendClick(inputText)
                            inputText = ""
                            showEmojiPicker = false
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.new_sms_send),
                        tint = if (inputText.isNotBlank()) colors.onAccent else colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubbleRow(
    message: SmsMessage,
    colors: org.openui.sms.ui.theme.AppCustomColors,
    onForwardClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val isOutgoing = remember(message.boxType) {
        message.boxType == MessageBoxType.SENT || message.boxType == MessageBoxType.OUTBOX
    }

    val bubbleColor = if (isOutgoing) colors.primaryBrand else colors.incomingBubble
    val textColor = if (isOutgoing) colors.onAccent else colors.textPrimary

    val largeCorner = if (colors.isExpressive) 24.dp else 18.dp
    val smallCorner = if (colors.isExpressive) 6.dp else 4.dp
    val shape = if (isOutgoing) {
        RoundedCornerShape(topStart = largeCorner, topEnd = largeCorner, bottomStart = largeCorner, bottomEnd = smallCorner)
    } else {
        RoundedCornerShape(topStart = largeCorner, topEnd = largeCorner, bottomStart = smallCorner, bottomEnd = largeCorner)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isOutgoing) {
                Text(
                    text = message.formattedDate,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(shape)
                    .background(bubbleColor)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showMenu = true }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.body,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(colors.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.thread_copy), color = colors.textPrimary) },
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("SMS", message.body)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, context.getString(R.string.thread_copied), Toast.LENGTH_SHORT).show()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.thread_forward), color = colors.textPrimary) },
                        onClick = {
                            onForwardClick(message.body)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.thread_delete), color = colors.actionRed) },
                        onClick = {
                            onDeleteClick()
                            showMenu = false
                        }
                    )
                }
            }

            if (!isOutgoing) {
                Text(
                    text = message.formattedDate,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

