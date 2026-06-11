package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.dto.ChildStatsCountDto
import com.example.taskoday.data.remote.dto.ChildStatsDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileStatsMappingTest {
    @Test
    fun `nested backend stats map to profile dashboard values`() {
        val mapped =
            ChildStatsDto(
                totalXp = 5,
                missions = ChildStatsCountDto(completed = 1, total = 2),
                quests = ChildStatsCountDto(completed = 2, total = 3),
            ).toDomain()

        assertEquals(5, mapped.totalXp)
        assertEquals(1, mapped.missionsCompleted)
        assertEquals(2, mapped.missionsTotal)
        assertEquals(2, mapped.questsCompleted)
        assertEquals(3, mapped.questsTotal)
    }

    @Test
    fun `missing backend stats keep safe zero fallback`() {
        val mapped = ChildStatsDto().toDomain()

        assertEquals(0, mapped.totalXp)
        assertEquals(0, mapped.missionsTotal)
        assertEquals(0, mapped.questsTotal)
    }
}
