package com.example

import com.example.data.local.CardState
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun cardStateConverter_roundTripsDatabaseValues() {
        assertEquals(CardState.NEW, CardState.fromDbValue("new"))
        assertEquals(CardState.LEARNING, CardState.fromDbValue("learning"))
        assertEquals(CardState.REVIEW, CardState.fromDbValue("review"))
    }
}
