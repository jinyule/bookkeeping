package com.github.bookkeeping.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattingTest {
    @Test
    fun parseAmountCentsKeepsCentPrecision() {
        assertEquals(1029L, parseAmountCents("10.29"))
        assertEquals(1L, parseAmountCents("0.01"))
        assertEquals(100L, parseAmountCents("1."))
    }

    @Test
    fun parseAmountCentsRejectsBlankInput() {
        assertNull(parseAmountCents(""))
    }
}
