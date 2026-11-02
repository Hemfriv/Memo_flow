package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO providing reactive Flow queries and transactional batch operations.
 */
@Dao
interface FlashcardDao {

    /**
     * Observes all decks with computed counts for New cards and Due cards today.
     * Note: Due cards include cards in 'learning' or 'review' state where next_review_date <= :now,
     * while 'new_count' counts cards in 'new' state where next_review_date <= :now.
     */
    @Query(
        """
        SELECT 
            d.id AS id,
            d.name AS name,
            d.created_at AS created_at,
            COUNT(c.id) AS total_cards,
            SUM(CASE WHEN c.state = 'new' THEN 1 ELSE 0 END) AS new_count,
            SUM(CASE WHEN c.state != 'new' AND c.next_review_date <= :now THEN 1 ELSE 0 END) AS due_count,
            SUM(CASE WHEN c.state = 'learning' THEN 1 ELSE 0 END) AS learning_count
        FROM decks d
        LEFT JOIN cards c ON d.id = c.deck_id
        GROUP BY d.id
        ORDER BY d.created_at DESC
        """
    )
    fun observeDecksWithCounts(now: Long): Flow<List<DeckWithCounts>>

    /**
     * Observes all cards across all decks for reactive memory analytics and 7-day forecast charts.
     */
    @Query("SELECT * FROM cards ORDER BY next_review_date ASC")
    fun observeAllCards(): Flow<List<CardEntity>>

    /**
     * Fetches due cards for a specific deck (`next_review_date <= :now`) sorted by priority:
     * 1. 'learning' cards first (urgent short-interval steps)
     * 2. 'review' cards ordered by overdue timestamp
     * 3. 'new' cards ordered by creation / id
     */
    @Query(
        """
        SELECT * FROM cards
        WHERE deck_id = :deckId AND next_review_date <= :now
        ORDER BY 
            CASE state
                WHEN 'learning' THEN 0
                WHEN 'review' THEN 1
                ELSE 2
            END ASC,
            next_review_date ASC,
            id ASC
        """
    )
    suspend fun getDueCardsForDeck(deckId: Long, now: Long): List<CardEntity>

    /**
     * Fetches all due cards across all decks (`next_review_date <= :now`) sorted by priority.
     */
    @Query(
        """
        SELECT * FROM cards
        WHERE next_review_date <= :now
        ORDER BY 
            CASE state
                WHEN 'learning' THEN 0
                WHEN 'review' THEN 1
                ELSE 2
            END ASC,
            next_review_date ASC,
            id ASC
        """
    )
    suspend fun getAllDueCards(now: Long): List<CardEntity>

    /**
     * Counts total cards due for review right now (`next_review_date <= :now`).
     * Used by local notifications to only send push reminders when due cards exist.
     */
    @Query("SELECT COUNT(*) FROM cards WHERE next_review_date <= :now")
    suspend fun countDueCards(now: Long): Int

    /**
     * Observes all cards inside a specific deck for browsing/editing.
     */
    @Query("SELECT * FROM cards WHERE deck_id = :deckId ORDER BY next_review_date ASC, id DESC")
    fun observeCardsByDeck(deckId: Long): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity): Long

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteDeckById(deckId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    /**
     * Atomic batch insertion inside a single SQLite transaction for fast Anki TXT deck imports.
     */
    @Transaction
    suspend fun importDeckWithCardsTransaction(
        deckName: String,
        cards: List<Pair<String, String>>,
        now: Long = System.currentTimeMillis()
    ): Long {
        val deckId = insertDeck(DeckEntity(name = deckName, createdAt = now))
        val entities = cards.map { (front, back) ->
            CardEntity(
                deckId = deckId,
                front = front,
                back = back,
                repetitions = 0,
                interval = 0,
                easeFactor = 2.5,
                nextReviewDate = now,
                state = CardState.NEW
            )
        }
        insertCards(entities)
        return deckId
    }

    /**
     * Atomic batch insertion of cards into an existing deck inside a single SQLite transaction.
     */
    @Transaction
    suspend fun insertCardsBatchTransaction(
        deckId: Long,
        cards: List<Pair<String, String>>,
        now: Long = System.currentTimeMillis()
    ): Int {
        val entities = cards.map { (front, back) ->
            CardEntity(
                deckId = deckId,
                front = front,
                back = back,
                repetitions = 0,
                interval = 0,
                easeFactor = 2.5,
                nextReviewDate = now,
                state = CardState.NEW
            )
        }
        insertCards(entities)
        return entities.size
    }

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("SELECT COUNT(*) FROM decks")
    suspend fun getDeckCount(): Int
}
