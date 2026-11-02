package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CardEntity
import com.example.data.local.DeckWithCounts
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FlashcardRepository
import com.example.domain.AnkiDelimiter
import com.example.domain.AnkiParseResult
import com.example.domain.AnkiTxtParser
import com.example.domain.ReviewRating
import com.example.domain.Sm2Scheduler
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Navigation screen state for the application.
 */
sealed interface AppScreen {
    data object DecksHome : AppScreen
    data class StudySession(val deckId: Long?, val deckTitle: String) : AppScreen
    data class DeckCardsBrowser(val deckId: Long, val deckTitle: String) : AppScreen
    data object Analytics : AppScreen
    data object ReminderSettings : AppScreen
}

/**
 * Statistics collected during an active SM-2 study session.
 */
data class SessionStats(
    val totalReviews: Int = 0,
    val againCount: Int = 0,
    val hardCount: Int = 0,
    val goodCount: Int = 0,
    val easyCount: Int = 0,
    val reQueuedCardsCount: Int = 0,
    val startedAtMillis: Long = System.currentTimeMillis()
)

/**
 * Active study session state including the forgotten cards re-queue.
 */
data class StudySessionUiState(
    val deckTitle: String = "",
    val queue: List<CardEntity> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val isSessionComplete: Boolean = false,
    val stats: SessionStats = SessionStats(),
    val cardTransitionKey: Int = 0
) {
    val currentCard: CardEntity?
        get() = queue.getOrNull(currentIndex)

    val remainingCount: Int
        get() = (queue.size - currentIndex).coerceAtLeast(0)

    val progressFraction: Float
        get() = if (queue.isEmpty()) 1f else (currentIndex.toFloat() / queue.size.toFloat()).coerceIn(0f, 1f)
}

/**
 * Main ViewModel managing decks, SM-2 study sessions, custom card interval shifting,
 * Anki TXT batch imports, memory analytics, and automatic daily reminders.
 */
class MemoFlowViewModel(
    private val repository: FlashcardRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val refreshTimestamp = MutableStateFlow(System.currentTimeMillis())

    @OptIn(ExperimentalCoroutinesApi::class)
    val decksState: StateFlow<List<DeckWithCounts>> = refreshTimestamp
        .flatMapLatest { now -> repository.observeDecksWithCounts(now) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val allCardsState: StateFlow<List<CardEntity>> = repository.observeAllCards()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val preferencesState: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.DecksHome)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _studyState = MutableStateFlow(StudySessionUiState())
    val studyState: StateFlow<StudySessionUiState> = _studyState.asStateFlow()

    private val selectedBrowserDeckId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val browserDeckCards: StateFlow<List<CardEntity>> = selectedBrowserDeckId
        .flatMapLatest { deckId ->
            if (deckId == null) flowOf(emptyList()) else repository.observeCardsByDeck(deckId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    fun dismissBanner() {
        _statusBannerMessage.value = null
    }

    fun showBanner(message: String) {
        _statusBannerMessage.value = message
    }

    fun navigateTo(screen: AppScreen) {
        if (screen is AppScreen.DeckCardsBrowser) {
            selectedBrowserDeckId.value = screen.deckId
        }
        if (screen is AppScreen.DecksHome || screen is AppScreen.Analytics) {
            refreshTimestamp.value = System.currentTimeMillis()
        }
        _currentScreen.value = screen
    }

    fun createDeck(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            repository.createDeck(cleanName)
            refreshTimestamp.value = System.currentTimeMillis()
            showBanner("Колода «$cleanName» создана")
        }
    }

    fun deleteDeck(deckId: Long, deckName: String) {
        viewModelScope.launch {
            repository.deleteDeck(deckId)
            refreshTimestamp.value = System.currentTimeMillis()
            showBanner("Колода «$deckName» удалена")
        }
    }

    fun addCardToDeck(deckId: Long, front: String, back: String, initialIntervalDays: Int = 0) {
        if (front.isBlank() || back.isBlank()) return
        viewModelScope.launch {
            repository.addCard(deckId, front, back, initialIntervalDays)
            refreshTimestamp.value = System.currentTimeMillis()
            val scheduleLabel = if (initialIntervalDays <= 0) {
                "на сегодня"
            } else {
                "через ${Sm2Scheduler.formatDaysRussian(initialIntervalDays)}"
            }
            showBanner("Карточка добавлена (повтор $scheduleLabel)")
        }
    }

    fun deleteCard(card: CardEntity) {
        viewModelScope.launch {
            repository.deleteCard(card)
            refreshTimestamp.value = System.currentTimeMillis()
        }
    }

    /**
     * Shifts the review date of a specific card in the browser by [days] days (e.g. 0 = Today, 1, 3, 7, 14, 30).
     */
    fun scheduleCardCustomDays(card: CardEntity, days: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = Sm2Scheduler.scheduleCustomDays(card, days, now)
            repository.updateCard(updated)
            refreshTimestamp.value = System.currentTimeMillis()
            val label = if (days <= 0) "на сегодня" else "на +${Sm2Scheduler.formatDaysRussian(days)}"
            showBanner("Повтор карточки перенесен $label")
        }
    }

    /**
     * Shifts the review date of the currently active card during a study session
     * by [days] days (e.g. +1 дн, +3 дн, +1 нед, +2 нед, +1 мес) and advances to the next card.
     */
    fun scheduleCurrentStudyCardCustomDays(days: Int) {
        val currentState = _studyState.value
        val card = currentState.currentCard ?: return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updatedCard = Sm2Scheduler.scheduleCustomDays(card, days, now)
            repository.updateCard(updatedCard)

            _studyState.update { state ->
                val nextIdx = state.currentIndex + 1
                val isFinished = nextIdx >= state.queue.size
                val updatedStats = state.stats.copy(
                    totalReviews = state.stats.totalReviews + 1,
                    goodCount = state.stats.goodCount + if (days in 1..6) 1 else 0,
                    easyCount = state.stats.easyCount + if (days >= 7) 1 else 0
                )
                state.copy(
                    currentIndex = nextIdx,
                    isFlipped = false,
                    isSessionComplete = isFinished,
                    stats = updatedStats,
                    cardTransitionKey = state.cardTransitionKey + 1
                )
            }
            refreshTimestamp.value = System.currentTimeMillis()
            showBanner("Карточка запланирована через ${Sm2Scheduler.formatDaysRussian(days)}")
        }
    }

    /**
     * Starts an SM-2 study session for a single deck (or all decks if [deckId] is null).
     * Cards with `next_review_date <= NOW()` are loaded in priority order (`learning` -> `review` -> `new`).
     */
    fun startStudySession(deckId: Long?, deckTitle: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dueCards = if (deckId != null) {
                repository.getDueCardsForDeck(deckId, now)
            } else {
                repository.getAllDueCards(now)
            }

            if (dueCards.isEmpty()) {
                showBanner("На сегодня все карточки в «$deckTitle» уже повторены!")
                return@launch
            }

            _studyState.value = StudySessionUiState(
                deckTitle = deckTitle,
                queue = dueCards,
                currentIndex = 0,
                isFlipped = false,
                isSessionComplete = false,
                stats = SessionStats(startedAtMillis = now),
                cardTransitionKey = 1
            )
            _currentScreen.value = AppScreen.StudySession(deckId, deckTitle)
        }
    }

    /**
     * Toggles the 3D flip state of the current flashcard.
     */
    fun flipCurrentCard() {
        _studyState.update { state ->
            if (state.isSessionComplete || state.currentCard == null) state
            else state.copy(isFlipped = !state.isFlipped)
        }
    }

    /**
     * Processes the user's SM-2 rating («Снова», «Трудно», «Хорошо», «Легко»).
     * If «Снова» (AGAIN) is selected, the card is updated in SQLite and appended
     * to the end of the active session queue so it is reviewed again before session completion.
     */
    fun submitCardRating(rating: ReviewRating) {
        val currentState = _studyState.value
        val card = currentState.currentCard ?: return

        viewModelScope.launch {
            val scheduleResult = Sm2Scheduler.schedule(card, rating)
            val updatedCard = scheduleResult.updatedCard
            repository.updateCard(updatedCard)

            _studyState.update { state ->
                val updatedQueue = if (scheduleResult.shouldRequeueInSession) {
                    state.queue + updatedCard
                } else {
                    state.queue
                }

                val nextIdx = state.currentIndex + 1
                val isFinished = nextIdx >= updatedQueue.size

                val updatedStats = state.stats.copy(
                    totalReviews = state.stats.totalReviews + 1,
                    againCount = state.stats.againCount + if (rating == ReviewRating.AGAIN) 1 else 0,
                    hardCount = state.stats.hardCount + if (rating == ReviewRating.HARD) 1 else 0,
                    goodCount = state.stats.goodCount + if (rating == ReviewRating.GOOD) 1 else 0,
                    easyCount = state.stats.easyCount + if (rating == ReviewRating.EASY) 1 else 0,
                    reQueuedCardsCount = state.stats.reQueuedCardsCount + if (scheduleResult.shouldRequeueInSession) 1 else 0
                )

                state.copy(
                    queue = updatedQueue,
                    currentIndex = nextIdx,
                    isFlipped = false,
                    isSessionComplete = isFinished,
                    stats = updatedStats,
                    cardTransitionKey = state.cardTransitionKey + 1
                )
            }
            refreshTimestamp.value = System.currentTimeMillis()
        }
    }

    /**
     * Reads a local text file selected via Android Document Picker and parses it with [AnkiTxtParser].
     */
    suspend fun readAndPreviewAnkiUri(
        context: Context,
        uri: Uri,
        delimiter: AnkiDelimiter
    ): Pair<String, AnkiParseResult> = withContext(Dispatchers.IO) {
        val rawText = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
            it.readText()
        } ?: ""
        rawText to AnkiTxtParser.parse(rawText, delimiter)
    }

    /**
     * Executes an atomic SQLite batch insert of parsed Anki cards inside a single transaction.
     */
    fun importParsedAnkiCards(
        targetDeckId: Long?,
        newDeckName: String,
        cards: List<Pair<String, String>>
    ) {
        if (cards.isEmpty()) {
            showBanner("Не найдено корректных карточек для импорта")
            return
        }
        viewModelScope.launch {
            val startMs = System.currentTimeMillis()
            val finalDeckName = newDeckName.trim().ifEmpty { "Импорт Anki" }
            repository.batchImportCards(
                targetDeckId = targetDeckId,
                newDeckName = finalDeckName,
                cards = cards
            )
            val elapsedMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
            refreshTimestamp.value = System.currentTimeMillis()
            showBanner("Пакетный импорт завершен: ${cards.size} карт. за $elapsedMs мс")
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHapticEnabled(enabled)
        }
    }

    fun updateReminderSchedule(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setReminderConfig(enabled, hour, minute)
            ReminderScheduler.scheduleDailyReminder(context, enabled, hour, minute)
            val timeFormatted = String.format("%02d:%02d", hour, minute)
            if (enabled) {
                showBanner("Автоматическое напоминание включено на $timeFormatted")
            } else {
                showBanner("Ежедневные напоминания отключены")
            }
        }
    }

    class Factory(
        private val repository: FlashcardRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MemoFlowViewModel(repository, preferencesRepository) as T
        }
    }
}
