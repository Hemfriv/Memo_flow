package com.example

import com.example.data.local.CardEntity
import com.example.data.local.CardState
import com.example.domain.AnkiDelimiter
import com.example.domain.AnkiTxtParser
import com.example.domain.ReviewRating
import com.example.domain.Sm2Scheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying the SuperMemo SM-2 algorithm, custom day/week interval shifting,
 * and the Anki TXT parser.
 */
class ExampleUnitTest {

    @Test
    fun sm2Scheduler_againResetsIntervalAndRequeuesInSession() {
        val card = CardEntity(
            id = 1L,
            deckId = 1L,
            front = "Вопрос",
            back = "Ответ",
            repetitions = 3,
            interval = 12,
            easeFactor = 2.5,
            state = CardState.REVIEW
        )

        val result = Sm2Scheduler.schedule(card, ReviewRating.AGAIN, nowMillis = 1_000_000L)

        assertTrue(result.shouldRequeueInSession)
        assertEquals(0, result.updatedCard.repetitions)
        assertEquals(0, result.updatedCard.interval)
        assertEquals(CardState.LEARNING, result.updatedCard.state)
        assertEquals("<10 мин", result.formattedInterval)
    }

    @Test
    fun sm2Scheduler_goodFollowsStandardSm2Intervals() {
        var card = CardEntity(
            id = 2L,
            deckId = 1L,
            front = "Q",
            back = "A",
            repetitions = 0,
            interval = 0,
            easeFactor = 2.5,
            state = CardState.NEW
        )

        // 1st Good -> 1 day
        val step1 = Sm2Scheduler.schedule(card, ReviewRating.GOOD)
        assertFalse(step1.shouldRequeueInSession)
        assertEquals(1, step1.updatedCard.interval)
        assertEquals(1, step1.updatedCard.repetitions)

        // 2nd Good -> 6 days
        card = step1.updatedCard
        val step2 = Sm2Scheduler.schedule(card, ReviewRating.GOOD)
        assertEquals(6, step2.updatedCard.interval)
        assertEquals(2, step2.updatedCard.repetitions)

        // 3rd Good -> round(6 * 2.5) = 15 days
        card = step2.updatedCard
        val step3 = Sm2Scheduler.schedule(card, ReviewRating.GOOD)
        assertEquals(15, step3.updatedCard.interval)
        assertEquals(3, step3.updatedCard.repetitions)
    }

    @Test
    fun sm2Scheduler_scheduleCustomDaysShiftsWeeksAndMonths() {
        val now = 10_000_000L
        val card = CardEntity(
            id = 3L,
            deckId = 1L,
            front = "Слово",
            back = "Перевод",
            repetitions = 0,
            interval = 0,
            easeFactor = 2.5,
            state = CardState.NEW
        )

        val shiftedWeek = Sm2Scheduler.scheduleCustomDays(card, days = 7, nowMillis = now)
        assertEquals(7, shiftedWeek.interval)
        assertEquals(CardState.REVIEW, shiftedWeek.state)
        assertEquals(now + 7 * Sm2Scheduler.MILLIS_PER_DAY, shiftedWeek.nextReviewDate)
        assertEquals("1 нед", Sm2Scheduler.formatDaysRussian(7))
    }

    @Test
    fun ankiTxtParser_handlesCommentsMultilineQuotesAndDelimiters() {
        val raw = """
            #separator:semicolon
            #deck:Тестовая колода
            Простой вопрос;Простой ответ
            "Многострочный
            вопрос с ; внутри";"Многострочный
            ответ с ""кавычками""!"
        """.trimIndent()

        val result = AnkiTxtParser.parse(raw, AnkiDelimiter.AUTO)

        assertEquals(';', result.detectedDelimiter)
        assertEquals(2, result.skippedCommentLines)
        assertEquals(2, result.cards.size)
        assertEquals("Простой вопрос", result.cards[0].first)
        assertEquals("Простой ответ", result.cards[0].second)
        assertTrue(result.cards[1].first.contains("Многострочный\nвопрос с ; внутри"))
        assertTrue(result.cards[1].second.contains("ответ с \"кавычками\"!"))
    }
}
