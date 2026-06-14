package com.baohao.esimkeeper.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baohao.esimkeeper.R
import com.baohao.esimkeeper.data.AppDatabase
import com.baohao.esimkeeper.data.CardSortOrder
import com.baohao.esimkeeper.data.CardSorter
import com.baohao.esimkeeper.data.CountryOption
import com.baohao.esimkeeper.data.ESimCard
import com.baohao.esimkeeper.data.ESimRepository
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
    val countryName: String,
    val balanceText: String,
    val startDate: LocalDate,
    val cycleDays: Int?,
    val expiryDate: LocalDate,
    val reminderDaysBefore: Int?,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("esim_hub_preferences", Context.MODE_PRIVATE)
    private val repository = ESimRepository(AppDatabase.get(application).eSimCardDao())
    private val _sortOrder = MutableStateFlow(
        CardSortOrder.fromPreferenceValue(preferences.getString(KEY_SORT_ORDER, null)),
    )

    val sortOrder: StateFlow<CardSortOrder> = _sortOrder

    val cards: StateFlow<List<ESimCard>> = combine(repository.cards, _sortOrder) { cards, sortOrder ->
        CardSorter.sort(cards, sortOrder)
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

    var isDarkMode by mutableStateOf(preferences.getBoolean(KEY_DARK_MODE, false))
        private set

    val isEditorOpen: Boolean
        get() = isAdding || editorTarget != null

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
        preferences.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply()
    }

    fun setSortOrder(order: CardSortOrder) {
        _sortOrder.value = order
        preferences.edit().putString(KEY_SORT_ORDER, order.preferenceValue).apply()
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
        val displayName = input.name.trim().ifBlank {
            getApplication<Application>().getString(R.string.default_esim_name, input.countryName)
        }
        val card = ESimCard(
            id = existing?.id ?: 0,
            name = displayName,
            phoneNumber = input.phoneNumber.trim(),
            countryName = input.countryName,
            countryCode = input.country.countryCode,
            flagEmoji = input.country.flagEmoji,
            balanceText = input.balanceText.trim().ifBlank {
                getApplication<Application>().getString(R.string.value_not_set)
            },
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

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_SORT_ORDER = "sort_order"
    }
}
