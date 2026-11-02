package com.example.domain

import com.example.data.local.CardEntity
import com.example.data.local.CardState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The 4 user response ratings in the SuperMemo SM-2 algorithm.
 * Mapped to SM-2 quality grades (q):
 * - AGAIN (q = 1): Forgotten card, re-queued in the current session.
 * - HARD  (q = 3): Recalled with significant difficulty.
 * - GOOD  (q = 4): Recalled after a hesitation (standard SM-2 target).
 * - EASY  (q = 5): Perfect, effortless recall.
 */
enum class ReviewRating(val quality: Int, val russianLabel: String) {
    AGAIN(1, "Снова"),
    HARD(3, "Трудно"),
    GOOD(4, "Хорошо"),
    EASY(5, "Легко")
}

/**
 * Preset custom shift options allowing users to manually schedule a specific card
 * across days, weeks, or a month alongside SM-2.
 */
enum class CustomIntervalPreset(val days: Int, val chipLabel: String, val descriptionRu: String) {
    TODAY(0, "Сегодня", "Повторить сегодня"),
    ONE_DAY(1, "+1 дн", "Через 1 день (завтра)"),
    THREE_DAYS(3, "+3 дн", "Через 3 дня"),
    ONE_WEEK(7, "+1 нед", "Через 1 неделю (7 дн.)"),
    TWO_WEEKS(14, "+2 нед", "Через 2 недели (14 дн.)"),
    ONE_MONTH(30, "+1 мес", "Через 1 месяц (30 дн.)")
}

/**
 * Result of applying an SM-2 review rating to a card.
 */
data class Sm2ScheduleResult(
    val updatedCard: CardEntity,
    val shouldRequeueInSession: Boolean,
    val formattedInterval: String
)

/**
 * SuperMemo SM-2 Spaced Repetition Scheduler.
 *
 * Implements the classic SM-2 ease factor update equation:
 *   EF' = max(1.3, EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
 * and calculates the next repetition count, interval (in days), and next review timestamp.
 */
object Sm2Scheduler {

    private const val MIN_EASE_FACTOR = 1.3
    const val MILLIS_PER_DAY = 86_400_000L
    private const val TEN_MINUTES_MS = 600_000L

    /**
     * Computes the updated SM-2 parameters for [card] given the user's [rating].
     */
    fun schedule(
        card: CardEntity,
        rating: ReviewRating,
        nowMillis: Long = System.currentTimeMillis()
    ): Sm2ScheduleResult {
        val q = rating.quality
        val currentEf = card.easeFactor
        val currentReps = card.repetitions
        val currentInterval = card.interval

        // Classic SM-2 Ease Factor delta formula:
        // delta = 0.1 - (5 - q) * (0.08 + (5 - q) * (0.02))
        val efDelta = 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)
        val nextEaseFactor = max(MIN_EASE_FACTOR, currentEf + efDelta)

        return when (rating) {
            ReviewRating.AGAIN -> {
                // Forgotten card: reset repetitions and interval, place in LEARNING state,
                // and mark for re-queueing at the end of the active study session.
                val updated = card.copy(
                    repetitions = 0,
                    interval = 0,
                    easeFactor = max(MIN_EASE_FACTOR, currentEf - 0.20),
                    nextReviewDate = nowMillis + TEN_MINUTES_MS,
                    state = CardState.LEARNING
                )
                Sm2ScheduleResult(
                    updatedCard = updated,
                    shouldRequeueInSession = true,
                    formattedInterval = "<10 мин"
                )
            }

            ReviewRating.HARD -> {
                val nextInterval = when {
                    currentReps == 0 -> 1
                    currentReps == 1 -> max(2, (currentInterval * 1.2).roundToInt())
                    else -> max(currentInterval + 1, (currentInterval * 1.2).roundToInt())
                }
                val nextReps = max(1, currentReps)
                val updated = card.copy(
                    repetitions = nextReps,
                    interval = nextInterval,
                    easeFactor = nextEaseFactor,
                    nextReviewDate = nowMillis + nextInterval * MILLIS_PER_DAY,
                    state = if (currentReps == 0) CardState.LEARNING else CardState.REVIEW
                )
                Sm2ScheduleResult(
                    updatedCard = updated,
                    shouldRequeueInSession = false,
                    formattedInterval = formatDaysRussian(nextInterval)
                )
            }

            ReviewRating.GOOD -> {
                val nextInterval = when (currentReps) {
                    0 -> 1
                    1 -> 6
                    else -> max(currentInterval + 1, (currentInterval * currentEf).roundToInt())
                }
                val nextReps = currentReps + 1
                val updated = card.copy(
                    repetitions = nextReps,
                    interval = nextInterval,
                    easeFactor = nextEaseFactor,
                    nextReviewDate = nowMillis + nextInterval * MILLIS_PER_DAY,
                    state = CardState.REVIEW
                )
                Sm2ScheduleResult(
                    updatedCard = updated,
                    shouldRequeueInSession = false,
                    formattedInterval = formatDaysRussian(nextInterval)
                )
            }

            ReviewRating.EASY -> {
                val easyBonus = 1.3
                val nextInterval = when (currentReps) {
                    0 -> 4
                    1 -> 8
                    else -> max(
                        currentInterval + 2,
                        (currentInterval * nextEaseFactor * easyBonus).roundToInt()
                    )
                }
                val nextReps = currentReps + 1
                val updated = card.copy(
                    repetitions = nextReps,
                    interval = nextInterval,
                    easeFactor = nextEaseFactor,
                    nextReviewDate = nowMillis + nextInterval * MILLIS_PER_DAY,
                    state = CardState.REVIEW
                )
                Sm2ScheduleResult(
                    updatedCard = updated,
                    shouldRequeueInSession = false,
                    formattedInterval = formatDaysRussian(nextInterval)
                )
            }
        }
    }

    /**
     * Manually schedules a specific card to be reviewed after [days] days (e.g. +1d, +3d, +7d, +14d, +30d).
     * Updates its SM-2 interval and nextReviewDate so automatic notifications and daily queues
     * pick it up on the target day.
     */
    fun scheduleCustomDays(
        card: CardEntity,
        days: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): CardEntity {
        val clampedDays = days.coerceAtLeast(0)
        return if (clampedDays == 0) {
            card.copy(
                interval = 0,
                nextReviewDate = nowMillis,
                state = if (card.repetitions == 0) CardState.NEW else CardState.LEARNING
            )
        } else {
            card.copy(
                repetitions = max(1, card.repetitions + 1),
                interval = clampedDays,
                nextReviewDate = nowMillis + clampedDays * MILLIS_PER_DAY,
                state = if (clampedDays < 3) CardState.LEARNING else CardState.REVIEW
            )
        }
    }

    /**
     * Generates a map of preview interval labels for all 4 rating buttons
     * to display below each button once a flashcard is flipped.
     */
    fun previewNextIntervals(card: CardEntity): Map<ReviewRating, String> {
        return ReviewRating.entries.associateWith { rating ->
            schedule(card, rating).formattedInterval
        }
    }

    /**
     * Formats an interval in days into a compact Russian string (e.g., "1 дн", "6 дн", "1 нед", "1 мес").
     */
    fun formatDaysRussian(days: Int): String {
        return when {
            days <= 0 -> "<10 мин"
            days == 7 -> "1 нед"
            days == 14 -> "2 нед"
            days < 30 -> "$days дн"
            days < 365 -> {
                val months = days / 30.0
                if (months < 1.5) "1 мес" else "${months.roundToInt()} мес"
            }
            else -> {
                val years = (days / 36.5).roundToInt() / 10.0
                "$years г"
            }
        }
    }

    /**
     * Formats a card's [nextReviewDate] into a human-readable Russian schedule label.
     */
    fun formatNextReviewDateRussian(
        nextReviewDate: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val diffMs = nextReviewDate - nowMillis
        if (diffMs <= 0L) {
            return "Готова к повтору сегодня"
        }
        val diffDays = ceil(diffMs.toDouble() / MILLIS_PER_DAY.toDouble()).toInt()
        val dateFormatter = SimpleDateFormat("d MMM", Locale.forLanguageTag("ru"))
        val dateStr = dateFormatter.format(Date(nextReviewDate))
        return when (diffDays) {
            1 -> "Завтра ($dateStr)"
            in 2..6 -> "Через $diffDays дн. ($dateStr)"
            7 -> "Через 1 нед. ($dateStr)"
            14 -> "Через 2 нед. ($dateStr)"
            else -> "Через $diffDays дн. ($dateStr)"
        }
    }
}
