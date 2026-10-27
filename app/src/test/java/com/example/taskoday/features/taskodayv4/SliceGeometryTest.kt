package com.example.taskoday.features.taskodayv4

import org.junit.Assert.assertEquals
import org.junit.Test

class SliceGeometryTest {
    @Test
    fun cardOnlyExtendsMiddleColumnHorizontally() {
        val spans = calculateAxisSlices(
            cuts = listOf(0, 215, 1171, 1386),
            targetLength = 733,
            baseScale = 0.5f,
            stretchedBands = setOf(1),
        )
        assertEquals(3, spans.size)
        assertEquals(107.5f, spans[0].destinationLength.toFloat(), 1f)
        assertEquals(40f, spans[1].destinationLength - (956 * 0.5f), 1f)
        assertEquals(107.5f, spans[2].destinationLength.toFloat(), 1f)
        assertEquals(733, spans.sumOf { it.destinationLength })
    }

    @Test
    fun cardPreservesFixedRowsAndSharesExtensionAcrossAllowedRows() {
        val spans = calculateAxisSlices(
            cuts = listOf(0, 127, 183, 280, 335, 462),
            targetLength = 271,
            baseScale = 0.5f,
            stretchedBands = setOf(1, 3),
        )
        assertEquals(5, spans.size)
        assertEquals(63.5f, spans[0].destinationLength.toFloat(), 1f)
        assertEquals(48.5f, spans[2].destinationLength.toFloat(), 1f)
        assertEquals(63.5f, spans[4].destinationLength.toFloat(), 1f)
        assertEquals(40f, (spans[1].destinationLength + spans[3].destinationLength) - 55.5f, 1f)
        assertEquals(271, spans.sumOf { it.destinationLength })
    }

    @Test
    fun summarySpreadsHorizontalExtensionAcrossTwoBandsAndVerticalOnlyInMiddle() {
        val x = calculateAxisSlices(
            cuts = listOf(0, 376, 808, 1307, 1739, 2115),
            targetLength = 1098,
            baseScale = 0.5f,
            stretchedBands = setOf(1, 3),
        )
        assertEquals(5, x.size)
        assertEquals(20f, x[1].destinationLength - 216f, 1f)
        assertEquals(20f, x[3].destinationLength - 216f, 1f)
        assertEquals(188f, x[0].destinationLength.toFloat(), 1f)
        assertEquals(188f, x[4].destinationLength.toFloat(), 1f)

        val y = calculateAxisSlices(
            cuts = listOf(0, 170, 432, 630),
            targetLength = 335,
            baseScale = 0.5f,
            stretchedBands = setOf(1),
        )
        assertEquals(85f, y[0].destinationLength.toFloat(), 1f)
        assertEquals(99f, y[2].destinationLength.toFloat(), 1f)
        assertEquals(335, y.sumOf { it.destinationLength })
    }
}
