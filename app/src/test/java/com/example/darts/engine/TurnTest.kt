package com.example.darts.engine

import org.junit.Assert.*
import org.junit.Test

class TurnTest {

    @Test
    fun `totalScore returns zero for empty turn`() {
        val turn = Turn()
        assertEquals(0, turn.totalScore())
    }

    @Test
    fun `totalScore sums single dart values`() {
        val turn = Turn(listOf(
            DartThrow(20, Multiplier.SINGLE),
            DartThrow(20, Multiplier.SINGLE),
            DartThrow(20, Multiplier.SINGLE),
        ))
        assertEquals(60, turn.totalScore())
    }

    @Test
    fun `totalScore handles mixed multipliers`() {
        val turn = Turn(listOf(
            DartThrow(20, Multiplier.TRIPLE), // 60
            DartThrow(20, Multiplier.DOUBLE), // 40
            DartThrow(20, Multiplier.SINGLE), // 20
        ))
        assertEquals(120, turn.totalScore())
    }

    @Test
    fun `totalScore handles bullseyes`() {
        val turn = Turn(listOf(
            DartThrow(25, Multiplier.DOUBLE), // 50
            DartThrow(25, Multiplier.SINGLE), // 25
        ))
        assertEquals(75, turn.totalScore())
    }

    @Test
    fun `totalScore handles misses`() {
        val turn = Turn(listOf(
            DartThrow(0, Multiplier.SINGLE),  // 0
            DartThrow(20, Multiplier.SINGLE), // 20
            DartThrow(0, Multiplier.SINGLE),  // 0
        ))
        assertEquals(20, turn.totalScore())
    }

    @Test
    fun `totalScore with less than 3 darts`() {
        val turn = Turn(listOf(
            DartThrow(20, Multiplier.TRIPLE),
        ))
        assertEquals(60, turn.totalScore())
    }

    @Test
    fun `totalScore with single dart`() {
        val turn = Turn(listOf(
            DartThrow(25, Multiplier.DOUBLE),
        ))
        assertEquals(50, turn.totalScore())
    }
}
