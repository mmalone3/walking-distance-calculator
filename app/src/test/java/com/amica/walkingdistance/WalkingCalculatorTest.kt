package com.amica.walkingdistance

import org.junit.Assert.assertEquals
import org.junit.Test

class WalkingCalculatorTest {
    @Test
    fun convertsFortyMinutesAtDefaultSpeed() {
        val walk = Walk(40.0 * 60, 2.4)
        assertEquals(1.6, walk.miles, 0.000001)
        assertEquals(2.5749504, walk.kilometers, 0.000001)
        assertEquals("25:00 /mi", formatPace(walk.speed))
    }

    @Test
    fun formatsLongAndFractionalDurationsAndPaceRollover() {
        assertEquals("2h 14m 00s", formatTime(8040.0))
        assertEquals("0h 01m 00s", formatTime(59.6))
        assertEquals("20:00 /mi", formatPace(3.001))
        assertEquals("— /mi", formatPace(0.0))
    }

    @Test
    fun sumsSegmentsAtTheirOwnSpeeds() {
        val walks = listOf(Walk(1800.0, 2.4), Walk(3600.0, 3.0))
        assertEquals(4.2, walks.sumOf { it.miles }, 0.000001)
        assertEquals("1h 30m 00s", formatTime(walks.sumOf { it.seconds }))
    }
}
