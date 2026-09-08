package com.womensafety.sos.ui.screens.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womensafety.sos.data.entity.TrustedContact
import com.womensafety.sos.di.ServiceLocator
import com.womensafety.sos.domain.repository.SafetyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrustedContactsViewModel(
    private val repository: SafetyRepository = ServiceLocator.repository
) : ViewModel() {

    val contacts: StateFlow<List<TrustedContact>> = repository.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addContact(name: String, phoneNumber: String, relationship: String) {
        viewModelScope.launch {
            val currentList = repository.getContactsList()
            val newContact = TrustedContact(
                name = name,
                phoneNumber = phoneNumber,
                relationship = relationship,
                priorityRank = currentList.size + 1
            )
            repository.addContact(newContact)
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            repository.deleteContact(id)
            reorderPriorityRanks()
        }
    }

    fun movePriorityUp(contact: TrustedContact) {
        viewModelScope.launch {
            val list = repository.getContactsList().toMutableList()
            val index = list.indexOfFirst { it.id == contact.id }
            if (index > 0) {
                val temp = list[index]
                list[index] = list[index - 1]
                list[index - 1] = temp
                repository.reorderContacts(list)
            }
        }
    }

    fun movePriorityDown(contact: TrustedContact) {
        viewModelScope.launch {
            val list = repository.getContactsList().toMutableList()
            val index = list.indexOfFirst { it.id == contact.id }
            if (index != -1 && index < list.size - 1) {
                val temp = list[index]
                list[index] = list[index + 1]
                list[index + 1] = temp
                repository.reorderContacts(list)
            }
        }
    }

    private suspend fun reorderPriorityRanks() {
        val list = repository.getContactsList()
        repository.reorderContacts(list)
    }
}
