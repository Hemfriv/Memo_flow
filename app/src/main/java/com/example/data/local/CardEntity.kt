package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * SQLite table 'cards' storing flashcards and their SuperMemo SM-2 scheduling parameters.
 */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deck_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deck_id"]),
        Index(value = ["next_review_date"]),
        Index(value = ["deck_id", "next_review_date"])
    ]
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "deck_id")
    val deckId: Long,

    @ColumnInfo(name = "front")
    val front: String,

    @ColumnInfo(name = "back")
    val back: String,

    @ColumnInfo(name = "repetitions")
    val repetitions: Int = 0,

    @ColumnInfo(name = "interval")
    val interval: Int = 0,

    @ColumnInfo(name = "ease_factor")
    val easeFactor: Double = 2.5,

    @ColumnInfo(name = "next_review_date")
    val nextReviewDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "state")
    val state: CardState = CardState.NEW
)
