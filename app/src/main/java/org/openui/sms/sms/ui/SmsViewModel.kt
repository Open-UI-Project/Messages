package org.openui.sms.sms.ui

import android.content.Context
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import org.openui.sms.sms.data.DefaultSmsRepository
import org.openui.sms.sms.data.SmsRepository
import org.openui.sms.sms.model.ContactItem
import org.openui.sms.sms.model.SmsConversation
import org.openui.sms.sms.model.SmsMessage
import org.openui.sms.sms.model.SmsSendResult
import org.openui.sms.sms.ui.model.SmsTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PermissionStatus {
    UNKNOWN, GRANTED, DENIED
}

data class SmsGroup(
    val name: String,
    val threadIds: List<Long>
)

class SmsViewModel(private val repository: SmsRepository) : ViewModel() {

    private val _permissionStatus = MutableStateFlow(PermissionStatus.UNKNOWN)
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    private val _isDefaultSmsApp = MutableStateFlow(true)
    val isDefaultSmsApp: StateFlow<Boolean> = _isDefaultSmsApp.asStateFlow()

    private val _selectedThreadId = MutableStateFlow<Long?>(null)
    val selectedThreadId: StateFlow<Long?> = _selectedThreadId.asStateFlow()

    private val _selectedTab = MutableStateFlow(SmsTab.CONVERSATIONS)
    val selectedTab: StateFlow<SmsTab> = _selectedTab.asStateFlow()

    private val _activeRecipient = MutableStateFlow("")
    val activeRecipient: StateFlow<String> = _activeRecipient.asStateFlow()

    private val _activeThreadTitle = MutableStateFlow("")
    val activeThreadTitle: StateFlow<String> = _activeThreadTitle.asStateFlow()

    fun setSelectedTab(tab: SmsTab) {
        _selectedTab.value = tab
    }

    private val _sendResult = MutableStateFlow<SmsSendResult?>(null)
    val sendResult: StateFlow<SmsSendResult?> = _sendResult.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _groups = MutableStateFlow<List<SmsGroup>>(emptyList())
    val groups: StateFlow<List<SmsGroup>> = _groups.asStateFlow()

    private val _selectedGroupName = MutableStateFlow("All")
    val selectedGroupName: StateFlow<String> = _selectedGroupName.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts: StateFlow<List<ContactItem>> = _contacts.asStateFlow()

    fun loadContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getAllContacts()
            _contacts.value = list
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: StateFlow<List<SmsConversation>> = _permissionStatus
        .flatMapLatest { status ->
            if (status == PermissionStatus.GRANTED) {
                repository.getConversations().onEach { _isLoading.value = false }
            } else {
                _isLoading.value = false
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredConversations: StateFlow<List<SmsConversation>> = combine(
        conversations,
        _searchQuery,
        _groups,
        _selectedGroupName
    ) { list, query, groupList, activeGroup ->
        var filtered = list

        if (activeGroup == "All" || activeGroup == "All" || activeGroup == "Все") {
            val allGroupThreadIds = groupList.flatMap { it.threadIds }.toSet()
            filtered = filtered.filter { it.threadId !in allGroupThreadIds }
        } else {
            val currentGroup = groupList.find { it.name == activeGroup }
            val groupThreadIds = currentGroup?.threadIds?.toSet() ?: emptySet()
            filtered = filtered.filter { it.threadId in groupThreadIds }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter {
                it.title.lowercase().contains(q) ||
                it.snippet.lowercase().contains(q) ||
                it.address.lowercase().contains(q)
            }
        }

        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeThreadMessages: StateFlow<List<SmsMessage>> = _selectedThreadId
        .flatMapLatest { threadId ->
            if (threadId != null && _permissionStatus.value == PermissionStatus.GRANTED) {
                repository.getMessagesForThread(threadId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectGroup(groupName: String) {
        _selectedGroupName.value = groupName
    }

    fun createGroup(name: String, selectedThreadIds: List<Long>) {
        if (name.isBlank()) return
        val current = _groups.value.toMutableList()
        current.removeAll { it.name.equals(name, ignoreCase = true) }
        current.add(SmsGroup(name.trim(), selectedThreadIds))
        _groups.value = current
        _selectedGroupName.value = name.trim()
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun updatePermissionStatus(granted: Boolean) {
        _permissionStatus.value = if (granted) {
            loadContacts()
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }

    fun checkDefaultSmsApp(context: Context) {
        val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)
        _isDefaultSmsApp.value = (defaultSmsPackage == context.packageName)
    }

    fun selectThread(threadId: Long?, recipient: String = "", title: String = "") {
        _selectedThreadId.value = threadId
        if (recipient.isNotBlank()) _activeRecipient.value = recipient
        if (title.isNotBlank()) _activeThreadTitle.value = title
        if (threadId != null) {
            markThreadAsRead(threadId)
        }
    }

    fun sendSms(recipient: String, body: String) {
        if (recipient.isBlank() || body.isBlank()) return
        viewModelScope.launch {
            val result = repository.sendSms(recipient, body)
            _sendResult.value = if (result.isSuccess) {
                SmsSendResult.Success
            } else {
                SmsSendResult.Error(
                    result.exceptionOrNull()?.message ?: "Failed to send SMS",
                    result.exceptionOrNull()
                )
            }
        }
    }

    fun clearSendResult() {
        _sendResult.value = null
    }

    fun markThreadAsRead(threadId: Long) {
        viewModelScope.launch {
            repository.markThreadAsRead(threadId)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun deleteConversation(threadId: Long) {
        viewModelScope.launch {
            if (_selectedThreadId.value == threadId) {
                _selectedThreadId.value = null
            }
            repository.deleteConversation(threadId)
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val repository = DefaultSmsRepository(context.applicationContext)
                    return SmsViewModel(repository) as T
                }
            }
    }
}
