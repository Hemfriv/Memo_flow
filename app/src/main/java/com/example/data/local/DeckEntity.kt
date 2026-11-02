package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SQLite table 'decks' representing a collection of flashcards.
 */
@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Projection model returned by reactive Room queries to display deck statistics
 * on the main screen without N+1 queries.
 */
data class DeckWithCounts(
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "total_cards")
    val totalCards: Int,

    @ColumnInfo(name = "new_count")
    val newCount: Int,

    @ColumnInfo(name = "due_count")
    val dueCount: Int,

    @ColumnInfo(name = "learning_count")
    val learningCount: Int
)
