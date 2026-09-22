package com.example.taskoday.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgeTest {
    @Test
    fun `age is derived from birth date at display time`() {
        assertEquals(35, ageFromBirthDate("1990-09-22", LocalDate.of(2026, 9, 21)))
        assertEquals(36, ageFromBirthDate("1990-09-21", LocalDate.of(2026, 9, 21)))
        assertNull(ageFromBirthDate("2999-01-01", LocalDate.of(2026, 9, 21)))
    }
}
