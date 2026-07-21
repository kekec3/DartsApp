package com.example.darts.engine

import org.junit.Assert.*
import org.junit.Test

class DartThrowTest {

    // ── score() ─────────────────────────────────────────────────────────

    @Test
    fun `score returns value times multiplier for single`() {
        val dart = DartThrow(20, Multiplier.SINGLE)
        assertEquals(20, dart.score())
    }

    @Test
    fun `score returns value times multiplier for double`() {
        val dart = DartThrow(20, Multiplier.DOUBLE)
        assertEquals(40, dart.score())
    }

    @Test
    fun `score returns value times multiplier for triple`() {
        val dart = DartThrow(20, Multiplier.TRIPLE)
        assertEquals(60, dart.score())
    }

    @Test
    fun `score returns zero for a miss`() {
        val dart = DartThrow(0, Multiplier.SINGLE)
        assertEquals(0, dart.score())
    }

    @Test
    fun `score returns 50 for bullseye`() {
        val dart = DartThrow(25, Multiplier.DOUBLE)
        assertEquals(50, dart.score())
    }

    @Test
    fun `score returns 25 for outer bull`() {
        val dart = DartThrow(25, Multiplier.SINGLE)
        assertEquals(25, dart.score())
    }

    // ── isDouble() ──────────────────────────────────────────────────────

    @Test
    fun `isDouble returns true for double multiplier`() {
        val dart = DartThrow(20, Multiplier.DOUBLE)
        assertTrue(dart.isDouble())
    }

    @Test
    fun `isDouble returns true for bullseye`() {
        val dart = DartThrow(25, Multiplier.DOUBLE)
        assertTrue(dart.isDouble())
    }

    @Test
    fun `isDouble returns false for single`() {
        val dart = DartThrow(20, Multiplier.SINGLE)
        assertFalse(dart.isDouble())
    }

    @Test
    fun `isDouble returns false for triple`() {
        val dart = DartThrow(20, Multiplier.TRIPLE)
        assertFalse(dart.isDouble())
    }

    @Test
    fun `isDouble returns false for miss`() {
        val dart = DartThrow(0, Multiplier.SINGLE)
        assertFalse(dart.isDouble())
    }

    @Test
    fun `isDouble returns true for outer bull with double`() {
        val dart = DartThrow(25, Multiplier.DOUBLE)
        assertTrue(dart.isDouble())
    }

    // ── isTriple() ──────────────────────────────────────────────────────

    @Test
    fun `isTriple returns true for triple multiplier`() {
        val dart = DartThrow(20, Multiplier.TRIPLE)
        assertTrue(dart.isTriple())
    }

    @Test
    fun `isTriple returns true for bullseye`() {
        val dart = DartThrow(25, Multiplier.DOUBLE)
        assertTrue(dart.isTriple())
    }

    @Test
    fun `isTriple returns false for single`() {
        val dart = DartThrow(20, Multiplier.SINGLE)
        assertFalse(dart.isTriple())
    }

    @Test
    fun `isTriple returns false for double`() {
        val dart = DartThrow(20, Multiplier.DOUBLE)
        assertFalse(dart.isTriple())
    }

    @Test
    fun `isTriple returns false for miss`() {
        val dart = DartThrow(0, Multiplier.SINGLE)
        assertFalse(dart.isTriple())
    }

    // ── isMiss() ────────────────────────────────────────────────────────

    @Test
    fun `isMiss returns true when value is zero`() {
        val dart = DartThrow(0, Multiplier.SINGLE)
        assertTrue(dart.isMiss())
    }

    @Test
    fun `isMiss returns false when value is non-zero`() {
        val dart = DartThrow(1, Multiplier.SINGLE)
        assertFalse(dart.isMiss())
    }

    // ── displayString() ─────────────────────────────────────────────────

    @Test
    fun `displayString shows Miss when missing`() {
        val dart = DartThrow(0, Multiplier.SINGLE)
        assertEquals("Miss", dart.displayString())
    }

    @Test
    fun `displayString shows Bull for bullseye`() {
        val dart = DartThrow(25, Multiplier.DOUBLE)
        assertEquals("Bull", dart.displayString())
    }

    @Test
    fun `displayString shows Outer for outer bull`() {
        val dart = DartThrow(25, Multiplier.SINGLE)
        assertEquals("Outer", dart.displayString())
    }

    @Test
    fun `displayString shows D prefix for doubles`() {
        val dart = DartThrow(16, Multiplier.DOUBLE)
        assertEquals("D16", dart.displayString())
    }

    @Test
    fun `displayString shows T prefix for triples`() {
        val dart = DartThrow(20, Multiplier.TRIPLE)
        assertEquals("T20", dart.displayString())
    }

    @Test
    fun `displayString shows plain number for singles`() {
        val dart = DartThrow(17, Multiplier.SINGLE)
        assertEquals("17", dart.displayString())
    }

    // ── shortDisplay() ──────────────────────────────────────────────────

    @Test
    fun `shortDisplay shows 0 for miss`() {
        val dart = DartThrow(0, Multiplier.SINGLE)
        assertEquals("0", dart.shortDisplay())
    }

    @Test
    fun `shortDisplay shows D prefix for double`() {
        val dart = DartThrow(20, Multiplier.DOUBLE)
        assertEquals("D20", dart.shortDisplay())
    }

    @Test
    fun `shortDisplay shows T prefix for triple`() {
        val dart = DartThrow(20, Multiplier.TRIPLE)
        assertEquals("T20", dart.shortDisplay())
    }

    @Test
    fun `shortDisplay shows plain number for single`() {
        val dart = DartThrow(5, Multiplier.SINGLE)
        assertEquals("5", dart.shortDisplay())
    }

    @Test
    fun `shortDisplay shows 25 for outer bull`() {
        val dart = DartThrow(25, Multiplier.SINGLE)
        assertEquals("25", dart.shortDisplay())
    }
}
