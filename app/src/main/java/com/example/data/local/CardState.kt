package com.example.data.local

import androidx.room.TypeConverter

/**
 * Represents the learning lifecycle state of a flashcard in the SM-2 algorithm.
 * Stored in SQLite as lowercase strings ('new', 'learning', 'review').
 */
enum class CardState(val dbValue: String, val russianLabel: String) {
    NEW("new", "Новая"),
    LEARNING("learning", "Изучается"),
    REVIEW("review", "Повторение");

    companion object {
        fun fromDbValue(value: String): CardState {
            return entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: NEW
        }
    }
}

class CardStateConverter {
    @TypeConverter
    fun fromCardState(state: CardState): String = state.dbValue

    @TypeConverter
    fun toCardState(value: String): CardState = CardState.fromDbValue(value)
}
