package com.example.data.repository

import com.example.data.local.CardEntity
import com.example.data.local.CardState
import com.example.data.local.DeckEntity
import com.example.data.local.DeckWithCounts
import com.example.data.local.FlashcardDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository abstracting Room database access and ensuring heavy batch operations
 * execute on [Dispatchers.IO].
 */
class FlashcardRepository(private val dao: FlashcardDao) {

    fun observeDecksWithCounts(now: Long = System.currentTimeMillis()): Flow<List<DeckWithCounts>> {
        return dao.observeDecksWithCounts(now)
    }

    fun observeAllCards(): Flow<List<CardEntity>> {
        return dao.observeAllCards()
    }

    fun observeCardsByDeck(deckId: Long): Flow<List<CardEntity>> {
        return dao.observeCardsByDeck(deckId)
    }

    suspend fun getDueCardsForDeck(deckId: Long, now: Long = System.currentTimeMillis()): List<CardEntity> {
        return withContext(Dispatchers.IO) {
            dao.getDueCardsForDeck(deckId, now)
        }
    }

    suspend fun getAllDueCards(now: Long = System.currentTimeMillis()): List<CardEntity> {
        return withContext(Dispatchers.IO) {
            dao.getAllDueCards(now)
        }
    }

    suspend fun countDueCards(now: Long = System.currentTimeMillis()): Int {
        return withContext(Dispatchers.IO) {
            dao.countDueCards(now)
        }
    }

    suspend fun createDeck(name: String): Long {
        return withContext(Dispatchers.IO) {
            dao.insertDeck(DeckEntity(name = name.trim()))
        }
    }

    suspend fun deleteDeck(deckId: Long) {
        withContext(Dispatchers.IO) {
            dao.deleteDeckById(deckId)
        }
    }

    suspend fun addCard(deckId: Long, front: String, back: String, initialIntervalDays: Int = 0): Long {
        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L
            val state = if (initialIntervalDays <= 0) CardState.NEW else CardState.REVIEW
            dao.insertCard(
                CardEntity(
                    deckId = deckId,
                    front = front.trim(),
                    back = back.trim(),
                    repetitions = if (initialIntervalDays > 0) 1 else 0,
                    interval = initialIntervalDays.coerceAtLeast(0),
                    easeFactor = 2.5,
                    nextReviewDate = now + initialIntervalDays.coerceAtLeast(0) * dayMs,
                    state = state
                )
            )
        }
    }

    suspend fun updateCard(card: CardEntity) {
        withContext(Dispatchers.IO) {
            dao.updateCard(card)
        }
    }

    suspend fun deleteCard(card: CardEntity) {
        withContext(Dispatchers.IO) {
            dao.deleteCard(card)
        }
    }

    /**
     * Performs a high-speed batch insert inside a single SQLite transaction.
     * Can either create a new deck or append to an existing [targetDeckId].
     */
    suspend fun batchImportCards(
        targetDeckId: Long?,
        newDeckName: String,
        cards: List<Pair<String, String>>
    ): Long {
        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (targetDeckId != null && targetDeckId > 0L) {
                dao.insertCardsBatchTransaction(targetDeckId, cards, now)
                targetDeckId
            } else {
                dao.importDeckWithCardsTransaction(newDeckName, cards, now)
            }
        }
    }
}
