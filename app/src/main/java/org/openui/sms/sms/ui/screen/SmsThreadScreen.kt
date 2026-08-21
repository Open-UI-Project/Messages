package org.openui.sms.sms.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import org.openui.sms.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openui.sms.sms.model.MessageBoxType
import org.openui.sms.sms.model.SmsMessage
import org.openui.sms.ui.theme.OpenUIBlack
import org.openui.sms.ui.theme.OpenUICardBackground
import org.openui.sms.ui.theme.OpenUIIncomingBubble
import org.openui.sms.ui.theme.OpenUISurface
import org.openui.sms.ui.theme.OpenUITextSecondary

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
    val prefs = remember(context) { context.getSharedPreferences("sms_drafts", Context.MODE_PRIVATE) }
    val draftKey = remember(title) { "draft_${title.hashCode()}" }

    var inputText by remember(title) { mutableStateOf(prefs.getString(draftKey, "") ?: "") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showAttachmentsMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

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

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            inputText = (inputText.trim() + context.getString(R.string.thread_attachment_image)).trim()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            inputText = (inputText.trim() + context.getString(R.string.thread_attachment_camera)).trim()
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
            .background(OpenUIBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OpenUISurface)
                .padding(horizontal = 4.dp, vertical = 10.dp),
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
                        tint = Color.White
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF393939)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onDeleteThreadClick) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = OpenUITextSecondary
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
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.text,
                                color = OpenUITextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    is ThreadListItem.MessageBubbleItem -> {
                        SmsMessageBubble(
                            message = item.message,
                            onDeleteClick = { onDeleteMessageClick(item.message.id) },
                            onForwardClick = { forwardText ->
                                inputText = forwardText
                            }
                        )
                    }
                }
            }

            item(key = "bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(OpenUISurface)
        ) {
            AnimatedVisibility(visible = showEmojiPicker) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OpenUISurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(EmojisList) { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            modifier = Modifier.clickable {
                                inputText += emoji
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showAttachmentsMenu) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OpenUISurface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text(
                        text = stringResource(R.string.thread_attachment_contact_item),
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable {
                            inputText = (inputText.trim() + context.getString(R.string.thread_attachment_contact)).trim()
                            showAttachmentsMenu = false
                        }
                    )
                    Text(
                        text = stringResource(R.string.thread_attachment_location_item),
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable {
                            inputText = (inputText.trim() + context.getString(R.string.thread_attachment_location)).trim()
                            showAttachmentsMenu = false
                        }
                    )
                    Text(
                        text = stringResource(R.string.thread_attachment_file_item),
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable {
                            inputText = (inputText.trim() + context.getString(R.string.thread_attachment_file)).trim()
                            showAttachmentsMenu = false
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OpenUISurface)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        tint = OpenUITextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = { cameraLauncher.launch() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        tint = OpenUITextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = { showAttachmentsMenu = !showAttachmentsMenu },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = if (showAttachmentsMenu) MaterialTheme.colorScheme.primary else OpenUITextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.contacts_message),
                            color = OpenUITextSecondary,
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
                                    tint = if (showEmojiPicker) MaterialTheme.colorScheme.primary else OpenUITextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    inputText = (inputText.trim() + context.getString(R.string.thread_attachment_audio)).trim()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = null,
                                    tint = OpenUITextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OpenUICardBackground,
                        unfocusedBorderColor = OpenUICardBackground,
                        focusedContainerColor = OpenUICardBackground,
                        unfocusedContainerColor = OpenUICardBackground
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary 
                            else OpenUICardBackground
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
                                showAttachmentsMenu = false
                            }
                        },
                        enabled = inputText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private val OutgoingShape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
private val IncomingShape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
private val TimeTextColor = Color(0xFF8E8E93)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SmsMessageBubble(
    message: SmsMessage,
    onDeleteClick: () -> Unit,
    onForwardClick: (String) -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val isOutgoing = remember(message.boxType) {
        message.boxType == MessageBoxType.SENT || message.boxType == MessageBoxType.OUTBOX
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val bubbleColor = remember(isOutgoing, primaryColor) {
        if (isOutgoing) primaryColor else OpenUIIncomingBubble
    }
    
    val shape = if (isOutgoing) OutgoingShape else IncomingShape

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
                    color = TimeTextColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
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
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(OpenUISurface)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.thread_copy), color = Color.White) },
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("SMS", message.body)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, context.getString(R.string.thread_copied), Toast.LENGTH_SHORT).show()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.thread_forward), color = Color.White) },
                        onClick = {
                            onForwardClick(message.body)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.thread_delete), color = Color.Red) },
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
                    color = TimeTextColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}


