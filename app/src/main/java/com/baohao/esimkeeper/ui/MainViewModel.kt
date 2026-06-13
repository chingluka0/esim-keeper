package com.baohao.esimkeeper.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baohao.esimkeeper.data.AppDatabase
import com.baohao.esimkeeper.data.CountryOption
import com.baohao.esimkeeper.data.ESimCard
import com.baohao.esimkeeper.data.ESimRepository
import com.baohao.esimkeeper.data.SettingsRepository
import com.baohao.esimkeeper.data.SortOrder
import com.baohao.esimkeeper.domain.ExpiryCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class CardEditorInput(
    val name: String,
    val phoneNumber: String,
    val country: CountryOption,
    val balanceText: String,
    val startDate: LocalDate,
    val cycleDays: Int?,
    val expiryDate: LocalDate,
    val reminderDaysBefore: Int?,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val repository = ESimRepository(AppDatabase.get(application).eSimCardDao())

    private val _sortOrder = MutableStateFlow(SortOrder.EXPIRY_ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    init {
        viewModelScope.launch {
            settingsRepository.sortOrder.collect { _sortOrder.value = it }
        }
    }

    val cards: StateFlow<List<ESimCard>> = combine(repository.cards, _sortOrder) { cards, order ->
        when (order) {
            SortOrder.EXPIRY_ASC -> cards.sortedBy { it.expiryDate }
            SortOrder.CREATED_DESC -> cards.sortedByDescending { it.createdAt }
            SortOrder.NAME_ASC -> cards.sortedBy { it.name.lowercase() }
            SortOrder.BALANCE_DESC -> cards.sortedByDescending { parseBalance(it.balanceText) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    var searchQuery by mutableStateOf("")
        private set

    var editorTarget by mutableStateOf<ESimCard?>(null)
        private set

    var isAdding by mutableStateOf(false)
        private set

    var isDarkMode by mutableStateOf(false)
        private set

    val isEditorOpen: Boolean
        get() = isAdding || editorTarget != null

    init {
        viewModelScope.launch {
            settingsRepository.isDarkMode.collect { isDarkMode = it }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            settingsRepository.setDarkMode(!isDarkMode)
        }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch {
            settingsRepository.setSortOrder(order)
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun openAdd() {
        editorTarget = null
        isAdding = true
    }

    fun openEdit(card: ESimCard) {
        editorTarget = card
        isAdding = false
    }

    fun closeEditor() {
        editorTarget = null
        isAdding = false
    }

    fun saveCard(input: CardEditorInput) {
        val existing = editorTarget
        val now = Instant.now()
        val displayName = input.name.trim().ifBlank { "${input.country.countryName} eSIM" }
        val card = ESimCard(
            id = existing?.id ?: 0,
            name = displayName,
            phoneNumber = input.phoneNumber.trim(),
            countryName = input.country.countryName,
            countryCode = input.country.countryCode,
            flagEmoji = input.country.flagEmoji,
            balanceText = input.balanceText.trim().ifBlank { "未填写" },
            startDate = input.startDate,
            cycleDays = input.cycleDays,
            expiryDate = input.expiryDate,
            reminderDaysBefore = input.reminderDaysBefore,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )

        viewModelScope.launch {
            repository.save(card)
            closeEditor()
        }
    }

    fun deleteCard(card: ESimCard) {
        viewModelScope.launch {
            repository.delete(card)
        }
    }

    fun renewCard(card: ESimCard, today: LocalDate = LocalDate.now()) {
        val cycleDays = card.cycleDays ?: return
        val (newStart, newExpiry) = ExpiryCalculator.renewFrom(today, cycleDays)
        viewModelScope.launch {
            repository.save(
                card.copy(
                    startDate = newStart,
                    expiryDate = newExpiry,
                    updatedAt = Instant.now(),
                ),
            )
        }
    }

    fun saveCardDirect(card: ESimCard) {
        viewModelScope.launch {
            repository.save(card)
        }
    }

    companion object {
        private fun parseBalance(text: String): Double {
            val cleaned = text.replace(Regex("[^0-9.]"), "")
            return cleaned.toDoubleOrNull() ?: 0.0
        }
    }
}
