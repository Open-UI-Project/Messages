package org.openui.sms.contacts.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.openui.sms.R
import org.openui.sms.contacts.data.ContactPreferences
import org.openui.sms.contacts.model.CallLogItem
import org.openui.sms.contacts.model.ContactItem
import org.openui.sms.contacts.model.getAvatarColor

val DialerGreen = Color(0xFF2E8B57)
val PureBlackBackground = Color(0xFF000000)

@Composable
fun SmsContactsScreen(
    contacts: List<ContactItem>,
    onContactClick: (number: String, name: String?, photoUri: String?) -> Unit,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
    onFetchCallLogs: suspend (String) -> List<CallLogItem>,
    onDetailStateChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) { ContactPreferences(context) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var expandedContactId by remember { mutableStateOf<Long?>(null) }
    var selectedInfoContact by remember { mutableStateOf<ContactItem?>(null) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var showFavoritesOnly by remember { mutableStateOf(false) }

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

    LaunchedEffect(selectedInfoContact) {
        onDetailStateChanged(selectedInfoContact != null)
    }

    val hasContactsPermission = remember(context) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    val activeContacts = remember(contacts, prefs) {
        contacts.filter { !prefs.isDeleted(it.number) }
    }

    val filteredSortedContacts = remember(activeContacts, searchQuery, showFavoritesOnly) {
        var filtered = activeContacts
        if (showFavoritesOnly) {
            filtered = filtered.filter { prefs.isFavorite(it.number) }
        }
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
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

    if (selectedInfoContact != null) {
        ContactCallHistoryScreen(
            contact = selectedInfoContact!!,
            onBackClick = { selectedInfoContact = null },
            onFetchCallLogs = onFetchCallLogs,
            onDeleteContact = { contact ->
                prefs.moveToTrash(contact.number)
                onRefresh()
            }
        )
        return
    }

    val screenBg = PureBlackBackground
    val cardBg = Color(0xFF1E1E22)
    val dividerColor = Color(0xFF292930)
    val textMuted = Color(0xFF8E8E93)

    BackHandler(enabled = isSearchVisible || showFavoritesOnly) {
        if (showFavoritesOnly) {
            showFavoritesOnly = false
        } else {
            isSearchVisible = false
            searchQuery = ""
        }
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
                text = if (showFavoritesOnly) "Избранное" else stringResource(R.string.contacts_title),
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
                    DropdownMenuItem(
                        text = { Text("Корзина", color = Color.White, fontSize = 14.sp) },
                        onClick = {
                            isMenuExpanded = false
                            showTrashDialog = true
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

        when {
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
                        text = if (showFavoritesOnly) "Нет избранных контактов" else if (searchQuery.isBlank()) stringResource(R.string.contacts_no_contacts) else stringResource(R.string.contacts_nothing_found),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (showFavoritesOnly) showFavoritesOnly = false else onRefresh()
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cardBg)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (showFavoritesOnly) "Показать все" else stringResource(R.string.contacts_refresh), color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (searchQuery.isBlank() && !showFavoritesOnly) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(cardBg)
                                    .clickable { showFavoritesOnly = true }
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
                                            initiateCall(contact.number)
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

        if (showTrashDialog) {
            val trashNumbers = remember(prefs) { prefs.getTrashNumbers() }
            val deletedContacts = remember(trashNumbers, contacts) {
                contacts.filter { prefs.isDeleted(it.number) }
            }

            AlertDialog(
                onDismissRequest = { showTrashDialog = false },
                containerColor = Color(0xFF222226),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFFF3B30))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Корзина удаленных", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                text = {
                    if (deletedContacts.isEmpty()) {
                        Text(text = "Корзина пуста", color = textMuted, fontSize = 14.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            deletedContacts.forEach { deleted ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = deleted.name, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                        Text(text = deleted.number, color = textMuted, fontSize = 12.sp)
                                    }
                                    TextButton(
                                        onClick = {
                                            prefs.restoreFromTrash(deleted.number)
                                            showTrashDialog = false
                                            onRefresh()
                                        }
                                    ) {
                                        Text("Восстановить", color = DialerGreen, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (deletedContacts.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                prefs.clearTrash()
                                showTrashDialog = false
                                onRefresh()
                            }
                        ) {
                            Text("Очистить всё", color = Color(0xFFFF3B30))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTrashDialog = false }) {
                        Text(stringResource(R.string.contacts_close), color = textMuted)
                    }
                }
            )
        }
    }
}

private val ContactButtonBg = Color(0xFF2A2A30)
private val SmsBlueIconColor = Color(0xFF2A7FF6)
private val ContactMobileSubtext = Color(0xFFA8A8A8)
private val ContactCircleShape = CircleShape

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
    val initial = contact.initials.ifBlank { "#" }

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
                        .clip(ContactCircleShape)
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
                            color = ContactMobileSubtext,
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(ContactCircleShape)
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
                            .clip(ContactCircleShape)
                            .background(ContactButtonBg)
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
                            .clip(ContactCircleShape)
                            .background(ContactButtonBg)
                            .clickable(onClick = onSmsClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubble,
                            contentDescription = stringResource(R.string.contacts_message),
                            tint = SmsBlueIconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Info
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(ContactCircleShape)
                            .background(ContactButtonBg)
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
