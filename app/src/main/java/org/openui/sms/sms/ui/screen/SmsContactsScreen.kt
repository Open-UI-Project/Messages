package org.openui.sms.sms.ui.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import org.openui.sms.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.openui.sms.sms.model.ContactItem
import org.openui.sms.sms.model.getAvatarColor

val DialerGreen = Color(0xFF2E8B57)
val PureBlackBackground = Color(0xFF000000)

@Composable
fun SmsContactsScreen(
    contacts: List<ContactItem>,
    onContactClick: (number: String, name: String?, photoUri: String?) -> Unit,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var expandedContactId by remember { mutableStateOf<Long?>(null) }
    var selectedInfoContact by remember { mutableStateOf<ContactItem?>(null) }

    val hasContactsPermission = remember(context) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    val filteredSortedContacts = remember(contacts, searchQuery) {
        val filtered = if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.number.contains(searchQuery)
            }
        }
        filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    val groupedContacts = remember(filteredSortedContacts) {
        filteredSortedContacts.groupBy { contact ->
            val firstChar = contact.name.trim().firstOrNull()?.uppercaseChar()
            if (firstChar != null && (firstChar.isLetter() || firstChar in 'А'..'Я' || firstChar == 'Ё')) {
                firstChar.toString()
            } else {
                "#"
            }
        }
    }

    val screenBg = PureBlackBackground
    val cardBg = Color(0xFF1E1E22)
    val iconCircleBg = Color(0xFF303036)
    val dividerColor = Color(0xFF292930)
    val textMuted = Color(0xFF8E8E93)

    BackHandler(enabled = isSearchVisible) {
        isSearchVisible = false
        searchQuery = ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.contacts_title),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        type = ContactsContract.Contacts.CONTENT_TYPE
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.contacts_add_contact),
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.contacts_search),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box {
                IconButton(onClick = { isMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.contacts_menu),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false },
                    modifier = Modifier.background(Color(0xFF222226))
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.contacts_refresh), color = Color.White, fontSize = 14.sp) },
                        onClick = {
                            isMenuExpanded = false
                            onRefresh()
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isSearchVisible || searchQuery.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.contacts_search_placeholder), color = textMuted, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.contacts_search),
                        tint = textMuted
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Rounded.Close, contentDescription = stringResource(R.string.contacts_clear), tint = textMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DialerGreen,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        cond_block@ when {
            !hasContactsPermission -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(cardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = DialerGreen,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.contacts_need_access),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.contacts_need_access_desc),
                        color = textMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onRequestPermissions,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DialerGreen)
                    ) {
                        Text(
                            text = stringResource(R.string.contacts_grant_access),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            filteredSortedContacts.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Contacts,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isBlank()) stringResource(R.string.contacts_no_contacts) else stringResource(R.string.contacts_nothing_found),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cardBg)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.contacts_refresh), color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (searchQuery.isBlank()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(cardBg)
                                    .clickable { }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFB800)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = stringResource(R.string.contacts_favorite),
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(cardBg)
                                    .clickable { }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(iconCircleBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Group,
                                        contentDescription = null,
                                        tint = textMuted,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = stringResource(R.string.contacts_groups),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    groupedContacts.forEach { (letter, contactsInGroup) ->
                        item(key = "letter_header_$letter") {
                            Text(
                                text = letter,
                                color = textMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp)
                            )
                        }
                        item(key = "group_$letter") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(cardBg)
                                    .animateContentSize(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                            ) {
                                contactsInGroup.forEachIndexed { index, contact ->
                                    val isExpanded = expandedContactId == contact.id
                                    ContactRowItem(
                                        contact = contact,
                                        isExpanded = isExpanded,
                                        onRowClick = {
                                            expandedContactId = if (isExpanded) null else contact.id
                                        },
                                        onCallClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.number}")).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        onSmsClick = {
                                            onContactClick(contact.number, contact.name, contact.photoUri)
                                        },
                                        onInfoClick = {
                                            selectedInfoContact = contact
                                        }
                                    )
                                    if (index < contactsInGroup.size - 1) {
                                        HorizontalDivider(
                                            color = dividerColor,
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(start = 68.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }

        selectedInfoContact?.let { contact ->
            AlertDialog(
                onDismissRequest = { selectedInfoContact = null },
                containerColor = Color(0xFF222226),
                title = { Text(text = contact.name, fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column {
                        Text(text = stringResource(R.string.contacts_mobile_phone), fontSize = 13.sp, color = textMuted)
                        Text(text = contact.number, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedInfoContact = null
                            onContactClick(contact.number, contact.name, contact.photoUri)
                        }
                    ) {
                        Text(stringResource(R.string.contacts_write_sms), color = DialerGreen)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedInfoContact = null }) {
                        Text(stringResource(R.string.contacts_close), color = textMuted)
                    }
                }
            )
        }
    }
}

@Composable
private fun ContactRowItem(
    contact: ContactItem,
    isExpanded: Boolean,
    onRowClick: () -> Unit,
    onCallClick: () -> Unit,
    onSmsClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarColor = remember(contact.name) { getAvatarColor(contact.name) }
    val initial = remember(contact.name) {
        contact.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "#"
    }
    val buttonBg = Color(0xFF2A2A30)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(color = Color.White.copy(alpha = 0.1f)),
                onClick = onRowClick
            )
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("contact_item_${contact.id}")
    ) {
        if (!isExpanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = contact.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.name,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.contacts_mobile_prefix, contact.number),
                            color = Color(0xFFA8A8A8),
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Call
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(buttonBg)
                            .clickable(onClick = onCallClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Phone,
                            contentDescription = stringResource(R.string.contacts_call),
                            tint = DialerGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    // SMS
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(buttonBg)
                            .clickable(onClick = onSmsClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubble,
                            contentDescription = stringResource(R.string.contacts_message),
                            tint = Color(0xFF2A7FF6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Info
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(buttonBg)
                            .clickable(onClick = onInfoClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                            contentDescription = stringResource(R.string.contacts_info),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
