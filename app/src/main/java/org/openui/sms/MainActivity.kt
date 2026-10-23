package org.openui.sms

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.view.Display
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import org.openui.sms.sms.data.DefaultSmsRepository
import org.openui.sms.sms.model.SmsSendResult
import org.openui.sms.sms.ui.SmsViewModel
import org.openui.sms.sms.ui.components.AboutDialog
import org.openui.sms.sms.ui.components.SettingsDialog
import org.openui.sms.sms.ui.components.CreateGroupDialog
import org.openui.sms.sms.ui.components.NewSmsDialog
import org.openui.sms.sms.ui.components.SmsConversationItem
import org.openui.sms.sms.ui.components.SmsFab
import org.openui.sms.sms.ui.components.SmsIslandNavigationBar
import org.openui.sms.sms.ui.components.SmsTopBar
import org.openui.sms.sms.ui.model.SmsTab
import org.openui.sms.contacts.ui.SmsContactsScreen
import org.openui.sms.sms.ui.screen.SmsThreadScreen
import org.openui.sms.data.AppSettings
import org.openui.sms.ui.theme.MyApplicationTheme
import org.openui.sms.ui.theme.OpenUIBlack
import org.openui.sms.ui.theme.OpenUICardBackground
import org.openui.sms.ui.theme.OpenUIOrangeBadge
import org.openui.sms.ui.theme.OpenUISurface
import org.openui.sms.ui.theme.OpenUITextSecondary
import android.net.Uri
import android.widget.VideoView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage

import androidx.activity.viewModels

class MainActivity : ComponentActivity() {

    private val viewModel: SmsViewModel by viewModels {
        SmsViewModel.provideFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableHighRefreshRate()

        setContent {
            MyApplicationTheme {
                SmsAppScreen(viewModel = viewModel)
            }
        }
    }

    private fun enableHighRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val currentDisplay = display ?: (getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)?.getDisplay(Display.DEFAULT_DISPLAY)
                val modes = currentDisplay?.supportedModes
                val highestRefreshRateMode = modes?.maxByOrNull { it.refreshRate }
                if (highestRefreshRateMode != null) {
                    val params = window.attributes
                    params.preferredDisplayModeId = highestRefreshRateMode.modeId
                    params.preferredRefreshRate = highestRefreshRateMode.refreshRate
                    window.attributes = params
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val modes = windowManager.defaultDisplay?.supportedModes
                val highestRefreshRateMode = modes?.maxByOrNull { it.refreshRate }
                if (highestRefreshRateMode != null) {
                    val params = window.attributes
                    params.preferredDisplayModeId = highestRefreshRateMode.modeId
                    params.preferredRefreshRate = highestRefreshRateMode.refreshRate
                    window.attributes = params
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndUpdateState()
        viewModel.checkDefaultSmsApp(this)
    }

    private fun checkPermissionsAndUpdateState() {
        val readSmsGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updatePermissionStatus(readSmsGranted)
    }
}

@Composable
fun SmsAppScreen(viewModel: SmsViewModel) {
    val context = LocalContext.current
    val allConversations by viewModel.conversations.collectAsState()
    val filteredConversations by viewModel.filteredConversations.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val selectedGroupName by viewModel.selectedGroupName.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val activeMessages by viewModel.activeThreadMessages.collectAsState()
    val sendResult by viewModel.sendResult.collectAsState()

    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedThreadId by viewModel.selectedThreadId.collectAsState()
    val activeRecipient by viewModel.activeRecipient.collectAsState()
    val activeThreadTitle by viewModel.activeThreadTitle.collectAsState()

    var isContactDetailOpen by remember { mutableStateOf(false) }

    var showNewSmsDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showMarkAllAsReadDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val appSettings = remember(context) { AppSettings.getInstance(context) }
    val wallpaperUri by appSettings.wallpaperUri.collectAsState()
    val wallpaperType by appSettings.wallpaperType.collectAsState()

    val permissions = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.CALL_PHONE
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val smsGranted = results[Manifest.permission.READ_SMS] == true
        viewModel.updatePermissionStatus(smsGranted)
    }

    LaunchedEffect(sendResult) {
        sendResult?.let { result ->
            when (result) {
                is SmsSendResult.Success -> {
                    Toast.makeText(context, context.getString(R.string.msg_sent), Toast.LENGTH_SHORT).show()
                    viewModel.clearSendResult()
                }
                is SmsSendResult.Error -> {
                    Toast.makeText(context, context.getString(R.string.msg_error, result.message), Toast.LENGTH_LONG).show()
                    viewModel.clearSendResult()
                }
            }
        }
    }

    if (showNewSmsDialog) {
        NewSmsDialog(
            onDismiss = { showNewSmsDialog = false },
            onSend = { address, text ->
                viewModel.sendSms(address, text)
                showNewSmsDialog = false
            }
        )
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            conversations = allConversations,
            onDismiss = { showCreateGroupDialog = false },
            onCreateGroup = { groupName, selectedIds ->
                viewModel.createGroup(groupName, selectedIds)
                showCreateGroupDialog = false
            }
        )
    }

    if (showMarkAllAsReadDialog) {
        AlertDialog(
            onDismissRequest = { showMarkAllAsReadDialog = false },
            containerColor = OpenUISurface,
            title = {
                Text(
                    text = stringResource(R.string.msg_read_all_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.msg_read_all_desc),
                    color = OpenUITextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markAllAsRead()
                        showMarkAllAsReadDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenUIOrangeBadge),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.group_create_btn), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkAllAsReadDialog = false }) {
                    Text(stringResource(R.string.group_cancel), color = OpenUITextSecondary)
                }
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            onDismissRequest = { showAboutDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismissRequest = { showSettingsDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (wallpaperUri != null) {
            if (wallpaperType == AppSettings.TYPE_VIDEO) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(wallpaperUri))
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                mp.setVolume(0f, 0f)
                                start()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = wallpaperUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OpenUIBlack.copy(alpha = 0.65f))
            )
        }

        AnimatedContent(
            targetState = selectedThreadId,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(300))).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 3 },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(180))
                    )
                } else {
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth / 3 },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(300))).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(180))
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            label = "MainScreenToThreadTransition"
        ) { threadId ->
            if (threadId != null) {
                BackHandler {
                    viewModel.selectThread(null)
                    if (searchQuery.isNotBlank()) {
                        viewModel.updateSearchQuery("")
                    }
                }
            SmsThreadScreen(
                title = activeThreadTitle,
                messages = activeMessages,
                onBackClick = {
                    viewModel.selectThread(null)
                    if (searchQuery.isNotBlank()) {
                        viewModel.updateSearchQuery("")
                    }
                },
                onSendClick = { body ->
                    viewModel.sendSms(activeRecipient, body)
                },
                onDeleteThreadClick = {
                    viewModel.deleteConversation(threadId)
                    viewModel.selectThread(null)
                    if (searchQuery.isNotBlank()) {
                        viewModel.updateSearchQuery("")
                    }
                },
                onDeleteMessageClick = { messageId ->
                    viewModel.deleteMessage(messageId)
                }
            )
        } else {
            val isDefaultSmsApp by viewModel.isDefaultSmsApp.collectAsState()

            val defaultSmsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                viewModel.checkDefaultSmsApp(context)
            }

            val requestDefaultSms: () -> Unit = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = context.getSystemService(RoleManager::class.java)
                    if (roleManager?.isRoleAvailable(RoleManager.ROLE_SMS) == true) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                        defaultSmsLauncher.launch(intent)
                    } else {
                        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                            putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                        }
                        defaultSmsLauncher.launch(intent)
                    }
                } else {
                    val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                        putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                    }
                    defaultSmsLauncher.launch(intent)
                }
            }

            val permissionStatus by viewModel.permissionStatus.collectAsState()
            val contacts by viewModel.contacts.collectAsState()

            LaunchedEffect(permissionStatus, isDefaultSmsApp) {
                if (permissionStatus == org.openui.sms.sms.ui.PermissionStatus.GRANTED && !isDefaultSmsApp) {
                    requestDefaultSms()
                }
            }

            BackHandler(enabled = searchQuery.isNotBlank()) {
                viewModel.updateSearchQuery("")
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (wallpaperUri != null) Color.Transparent else OpenUIBlack)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState == SmsTab.CONTACTS) {
                            (slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(280, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(280))).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -fullWidth / 3 },
                                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(180))
                            )
                        } else {
                            (slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                                animationSpec = tween(280, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(280))).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(180))
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "TabContentTransition"
                ) { currentTab ->
                    when (currentTab) {
                        SmsTab.CONVERSATIONS -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                SmsTopBar(
                                    groups = groups,
                                    selectedGroup = selectedGroupName,
                                    searchQuery = searchQuery,
                                    onGroupSelect = { viewModel.selectGroup(it) },
                                    onCreateGroupClick = { showCreateGroupDialog = true },
                                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                    onMarkAllAsReadClick = { showMarkAllAsReadDialog = true },
                                    onMakeDefaultAppClick = { requestDefaultSms() },
                                    onSettingsClick = { showSettingsDialog = true },
                                    onAboutClick = { showAboutDialog = true }
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                        .background(if (wallpaperUri != null) OpenUICardBackground.copy(alpha = 0.85f) else OpenUICardBackground)
                                ) {
                                if (permissionStatus == org.openui.sms.sms.ui.PermissionStatus.DENIED) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.perm_required_title),
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = stringResource(R.string.perm_required_desc),
                                            color = OpenUITextSecondary,
                                            fontSize = 14.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = { permissionLauncher.launch(permissions) },
                                            colors = ButtonDefaults.buttonColors(containerColor = OpenUIOrangeBadge),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(stringResource(R.string.perm_grant_btn), color = Color.White, fontWeight = FontWeight.SemiBold)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        TextButton(
                                            onClick = {
                                                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            }
                                        ) {
                                            Text(stringResource(R.string.perm_settings_btn), color = OpenUITextSecondary)
                                        }
                                    }
                                } else if (isLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = OpenUIOrangeBadge)
                                    }
                                } else if (filteredConversations.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (searchQuery.isNotBlank()) stringResource(R.string.nothing_found) else stringResource(R.string.no_messages),
                                            color = Color.White,
                                            fontSize = 18.sp
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp)
                                    ) {
                                        items(
                                            items = filteredConversations,
                                            key = { it.threadId },
                                            contentType = { "conversation_item" }
                                        ) { conversation ->
                                            SmsConversationItem(
                                                conversation = conversation,
                                                onClick = {
                                                    viewModel.selectThread(
                                                        conversation.threadId,
                                                        conversation.address,
                                                        conversation.title
                                                    )
                                                    if (conversation.unreadCount > 0) {
                                                        viewModel.markThreadAsRead(conversation.threadId)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SmsTab.CONTACTS -> {
                            SmsContactsScreen(
                                contacts = contacts,
                                onContactClick = { number, name, _ ->
                                    val existing = allConversations.find { 
                                        it.address == number || (number.length >= 7 && it.address.endsWith(number.takeLast(7))) 
                                    }
                                    if (existing != null) {
                                        viewModel.selectThread(
                                            existing.threadId,
                                            existing.address,
                                            name ?: existing.title
                                        )
                                    } else {
                                        viewModel.selectThread(-1L, number, name ?: number)
                                    }
                                },
                                onRequestPermissions = {
                                    permissionLauncher.launch(permissions)
                                },
                                onRefresh = {
                                    viewModel.loadContacts()
                                },
                                onFetchCallLogs = { number ->
                                    viewModel.getCallLogsForNumber(number)
                                },
                                onDetailStateChanged = { isOpen ->
                                    isContactDetailOpen = isOpen
                                }
                            )
                        }
                    }
                }

                if (!isContactDetailOpen) {
                    SmsIslandNavigationBar(
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.setSelectedTab(it) },
                        totalUnreadCount = allConversations.sumOf { it.unreadCount },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(end = 20.dp, bottom = 18.dp)
                    ) {
                        SmsFab(onClick = { showNewSmsDialog = true })
                    }
                }
            }
        }
    }
}
}
