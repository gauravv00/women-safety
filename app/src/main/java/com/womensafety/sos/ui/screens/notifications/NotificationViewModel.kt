package com.womensafety.sos.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womensafety.sos.data.model.NotificationItem
import com.womensafety.sos.data.model.NotificationType
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.domain.repository.NotificationRepository
import com.womensafety.sos.service.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Success(val items: List<NotificationItem>) : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
}

class NotificationViewModel(
    private val repository: NotificationRepository = ServiceLocator.notificationRepository
) : ViewModel() {

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    val uiState: StateFlow<NotificationsUiState> = repository.getNotifications()
        .map { items ->
            // If any notifications are marked RESOLVED, automatically dismiss ringing emergency alert
            items.filter { it.type == NotificationType.RESOLVED }.forEach { resolvedItem ->
                ServiceLocator.appContext?.let { ctx ->
                    NotificationHelper.cancelEmergencyNotification(ctx, resolvedItem.refId)
                }
            }
            NotificationsUiState.Success(items)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotificationsUiState.Loading
        )

    val unreadCount: StateFlow<Int> = repository.getNotifications()
        .map { items -> items.count { !it.read } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun markRead(id: String) {
        viewModelScope.launch {
            repository.markRead(id)
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            repository.markAllRead()
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun acceptPairRequest(notification: NotificationItem) {
        viewModelScope.launch {
            val result = repository.handlePairRequest(notification, accept = true)
            if (result.isSuccess) {
                _actionMessage.value = "Accepted pairing with ${notification.fromName.ifBlank { "Guardian" }}"
            } else {
                _actionMessage.value = "Failed to accept pairing request"
            }
        }
    }

    fun rejectPairRequest(notification: NotificationItem) {
        viewModelScope.launch {
            val result = repository.handlePairRequest(notification, accept = false)
            if (result.isSuccess) {
                _actionMessage.value = "Declined pairing request"
            } else {
                _actionMessage.value = "Failed to decline request"
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
