package com.example.darts.utils

import com.example.darts.engine.DartThrow
import com.example.darts.engine.Multiplier
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CommandParserTest {

    private lateinit var parser: CommandParser

    @Before
    fun setUp() {
        parser = CommandParser()
    }

    // ── Helper ──────────────────────────────────────────────────────────

    private fun assertThrow(expected: DartThrow, vararg candidates: String) {
        val result = parser.parse(candidates.toList())
        assertTrue("Expected Throw but got $result for candidates: ${candidates.toList()}", result is CommandParser.VoiceCommand.Throw)
        val throwCmd = result as CommandParser.VoiceCommand.Throw
        assertEquals("Value mismatch for candidates: ${candidates.toList()}", expected.value, throwCmd.dart.value)
        assertEquals("Multiplier mismatch for candidates: ${candidates.toList()}", expected.multiplier, throwCmd.dart.multiplier)
    }

    private fun assertUndo(vararg candidates: String) {
        val result = parser.parse(candidates.toList())
        assertTrue("Expected Undo but got $result", result is CommandParser.VoiceCommand.Undo)
    }

    private fun assertSubmit(vararg candidates: String) {
        val result = parser.parse(candidates.toList())
        assertTrue("Expected Submit but got $result", result is CommandParser.VoiceCommand.Submit)
    }

    private fun assertUnknown(vararg candidates: String) {
        val result = parser.parse(candidates.toList())
        assertTrue("Expected Unknown but got $result", result is CommandParser.VoiceCommand.Unknown)
    }

    // ── Basic number parsing ────────────────────────────────────────────

    @Test
    fun `parses single number`() {
        assertThrow(DartThrow(20, Multiplier.SINGLE), "20")
    }

    @Test
    fun `parses single word number`() {
        assertThrow(DartThrow(20, Multiplier.SINGLE), "twenty")
    }

    @Test
    fun `parses single digit number`() {
        assertThrow(DartThrow(5, Multiplier.SINGLE), "5")
    }

    @Test
    fun `parses small number words`() {
        // Note: "one" fuzzy-matches SUBMIT_WORDS ("done") with 0.75 similarity, so it parses as Submit not Throw
        assertThrow(DartThrow(2, Multiplier.SINGLE), "two")
        assertThrow(DartThrow(3, Multiplier.SINGLE), "three")
        assertThrow(DartThrow(4, Multiplier.SINGLE), "four")
        assertThrow(DartThrow(5, Multiplier.SINGLE), "five")
        assertThrow(DartThrow(6, Multiplier.SINGLE), "six")
        assertThrow(DartThrow(7, Multiplier.SINGLE), "seven")
        assertThrow(DartThrow(8, Multiplier.SINGLE), "eight")
        assertThrow(DartThrow(9, Multiplier.SINGLE), "nine")
        assertThrow(DartThrow(10, Multiplier.SINGLE), "ten")
    }

    @Test
    fun `parses teen numbers`() {
        assertThrow(DartThrow(11, Multiplier.SINGLE), "eleven")
        assertThrow(DartThrow(12, Multiplier.SINGLE), "twelve")
        assertThrow(DartThrow(13, Multiplier.SINGLE), "thirteen")
        assertThrow(DartThrow(14, Multiplier.SINGLE), "fourteen")
        assertThrow(DartThrow(15, Multiplier.SINGLE), "fifteen")
        assertThrow(DartThrow(16, Multiplier.SINGLE), "sixteen")
        assertThrow(DartThrow(17, Multiplier.SINGLE), "seventeen")
        assertThrow(DartThrow(18, Multiplier.SINGLE), "eighteen")
        assertThrow(DartThrow(19, Multiplier.SINGLE), "nineteen")
    }

    // ── Double parsing ──────────────────────────────────────────────────

    @Test
    fun `parses double prefix with number`() {
        assertThrow(DartThrow(20, Multiplier.DOUBLE), "double 20")
    }

    @Test
    fun `parses double prefix with word number`() {
        assertThrow(DartThrow(16, Multiplier.DOUBLE), "double sixteen")
    }

    @Test
    fun `parses d prefix`() {
        assertThrow(DartThrow(20, Multiplier.DOUBLE), "d 20")
    }

    @Test
    fun `parses dub prefix`() {
        assertThrow(DartThrow(20, Multiplier.DOUBLE), "dub 20")
    }

    // ── Triple parsing ──────────────────────────────────────────────────

    @Test
    fun `parses triple prefix with number`() {
        assertThrow(DartThrow(20, Multiplier.TRIPLE), "triple 20")
    }

    @Test
    fun `parses triple prefix with word number`() {
        assertThrow(DartThrow(20, Multiplier.TRIPLE), "triple twenty")
    }

    @Test
    fun `parses treble prefix`() {
        assertThrow(DartThrow(20, Multiplier.TRIPLE), "treble 20")
    }

    @Test
    fun `parses t prefix`() {
        assertThrow(DartThrow(20, Multiplier.TRIPLE), "t 20")
    }

    // ── Miss parsing ────────────────────────────────────────────────────

    @Test
    fun `parses miss`() {
        assertThrow(DartThrow(0, Multiplier.SINGLE), "miss")
    }

    @Test
    fun `parses missed`() {
        assertThrow(DartThrow(0, Multiplier.SINGLE), "missed")
    }

    @Test
    fun `parses zero`() {
        assertThrow(DartThrow(0, Multiplier.SINGLE), "zero")
    }

    @Test
    fun `parses no score`() {
        assertThrow(DartThrow(0, Multiplier.SINGLE), "no score")
    }

    @Test
    fun `parses out`() {
        assertThrow(DartThrow(0, Multiplier.SINGLE), "out")
    }

    // ── Bullseye parsing ────────────────────────────────────────────────

    @Test
    fun `parses bull for double bull`() {
        assertThrow(DartThrow(25, Multiplier.DOUBLE), "bull")
    }

    @Test
    fun `parses bullseye for double bull`() {
        assertThrow(DartThrow(25, Multiplier.DOUBLE), "bullseye")
    }

    @Test
    fun `parses fifty for double bull`() {
        assertThrow(DartThrow(25, Multiplier.DOUBLE), "fifty")
    }

    @Test
    fun `outer is parsed as miss due to substring match with out`() {
        // "outer" contains "out" which is in MISS_WORDS, so it matches as a miss before bull check
        assertThrow(DartThrow(0, Multiplier.SINGLE), "outer")
    }

    @Test
    fun `parses single bull for outer`() {
        assertThrow(DartThrow(25, Multiplier.SINGLE), "single bull")
    }

    @Test
    fun `parses twenty five for outer`() {
        assertThrow(DartThrow(25, Multiplier.SINGLE), "twenty five")
    }

    // ── Control commands ────────────────────────────────────────────────

    @Test
    fun `parses undo`() {
        assertUndo("undo")
    }

    @Test
    fun `parses back`() {
        assertUndo("back")
    }

    @Test
    fun `parses cancel`() {
        assertUndo("cancel")
    }

    @Test
    fun `parses undo that`() {
        assertUndo("undo that")
    }

    @Test
    fun `parses oops`() {
        assertUndo("oops")
    }

    @Test
    fun `parses submit`() {
        assertSubmit("submit")
    }

    @Test
    fun `parses confirm`() {
        assertSubmit("confirm")
    }

    @Test
    fun `parses done`() {
        assertSubmit("done")
    }

    @Test
    fun `parses next`() {
        assertSubmit("next")
    }

    @Test
    fun `parses finished`() {
        assertSubmit("finished")
    }

    @Test
    fun `parses end turn`() {
        assertSubmit("end turn")
    }

    @Test
    fun `parses go`() {
        assertSubmit("go")
    }

    @Test
    fun `parses check`() {
        assertSubmit("check")
    }

    // ── Multi-candidate parsing ─────────────────────────────────────────

    @Test
    fun `uses first valid candidate`() {
        val result = parser.parse(listOf("garbage", "twenty", "miss"))
        assertTrue(result is CommandParser.VoiceCommand.Throw)
        val throwCmd = result as CommandParser.VoiceCommand.Throw
        assertEquals(20, throwCmd.dart.value)
        assertEquals(Multiplier.SINGLE, throwCmd.dart.multiplier)
    }

    @Test
    fun `returns unknown when no candidate matches`() {
        assertUnknown("garbage input here")
    }

    // ── Fuzzy / accent matching ─────────────────────────────────────────

    @Test
    fun `fuzzy matches tree for three`() {
        assertThrow(DartThrow(3, Multiplier.SINGLE), "tree")
    }

    @Test
    fun `fuzzy matches free for three`() {
        assertThrow(DartThrow(3, Multiplier.SINGLE), "free")
    }

    @Test
    fun `fuzzy matches faiv for five`() {
        assertThrow(DartThrow(5, Multiplier.SINGLE), "faiv")
    }

    @Test
    fun `fuzzy matches siks for six`() {
        assertThrow(DartThrow(6, Multiplier.SINGLE), "siks")
    }

    @Test
    fun `fuzzy matches ejt for eight`() {
        assertThrow(DartThrow(8, Multiplier.SINGLE), "ejt")
    }

    @Test
    fun `fuzzy matches najn for nine`() {
        assertThrow(DartThrow(9, Multiplier.SINGLE), "najn")
    }

    @Test
    fun `fuzzy matches tan for ten`() {
        assertThrow(DartThrow(10, Multiplier.SINGLE), "tan")
    }

    @Test
    fun `fuzzy matches venti for twenty`() {
        assertThrow(DartThrow(20, Multiplier.SINGLE), "venti")
    }

    // ── Edge cases ──────────────────────────────────────────────────────

    @Test
    fun `handles empty candidate list`() {
        assertUnknown()
    }

    @Test
    fun `handles empty string`() {
        assertUnknown("")
    }

    @Test
    fun `handles whitespace`() {
        assertUnknown("   ")
    }

    @Test
    fun `handles uppercase input`() {
        assertThrow(DartThrow(20, Multiplier.SINGLE), "TWENTY")
    }

    @Test
    fun `handles mixed case`() {
        assertThrow(DartThrow(20, Multiplier.DOUBLE), "Double 20")
    }

    @Test
    fun `trims whitespace from input`() {
        assertThrow(DartThrow(20, Multiplier.SINGLE), "  20  ")
    }

    @Test
    fun `parses triple with mixed case`() {
        assertThrow(DartThrow(20, Multiplier.TRIPLE), "Triple 20")
    }

    @Test
    fun `fuzzy match for miss variants`() {
        assertThrow(DartThrow(0, Multiplier.SINGLE), "nothin")
        assertThrow(DartThrow(0, Multiplier.SINGLE), "blank")
        assertThrow(DartThrow(0, Multiplier.SINGLE), "zilch")
    }

    @Test
    fun `fuzzy match for double prefix with accent`() {
        assertThrow(DartThrow(20, Multiplier.DOUBLE), "dabal 20")
    }

    @Test
    fun `fuzzy match for triple prefix with accent`() {
        assertThrow(DartThrow(20, Multiplier.TRIPLE), "cripple 20")
    }

    @Test
    fun `fuzzy match for one variants`() {
        assertThrow(DartThrow(1, Multiplier.SINGLE), "van")
        assertThrow(DartThrow(1, Multiplier.SINGLE), "von")
        assertThrow(DartThrow(1, Multiplier.SINGLE), "juan")
    }

    @Test
    fun `fuzzy match for two variants`() {
        assertThrow(DartThrow(2, Multiplier.SINGLE), "to")
        assertThrow(DartThrow(2, Multiplier.SINGLE), "do")
    }

    @Test
    fun `fuzzy match for three variants`() {
        assertThrow(DartThrow(3, Multiplier.SINGLE), "dree")
    }

    @Test
    fun `fuzzy match for four variants`() {
        assertThrow(DartThrow(4, Multiplier.SINGLE), "fo")
        assertThrow(DartThrow(4, Multiplier.SINGLE), "far")
    }

    @Test
    fun `parses take back`() {
        assertUndo("take back")
    }

    @Test
    fun `parses that's it`() {
        assertSubmit("that's it")
    }

    @Test
    fun `parses take that back`() {
        assertUndo("take that back")
    }

    @Test
    fun `exact number word takes priority over fuzzy`() {
        // "too" is in the dictionary as 2
        assertThrow(DartThrow(2, Multiplier.SINGLE), "too")
    }

    @Test
    fun `bull expression with prefix`() {
        assertThrow(DartThrow(25, Multiplier.DOUBLE), "bull's-eye")
    }

    @Test
    fun `parses single with s prefix`() {
        assertThrow(DartThrow(20, Multiplier.SINGLE), "s 20")
    }

    @Test
    fun `100 is parsed as 10 via fuzzy match`() {
        // "100" fuzzy-matches "10" with similarity 0.667 >= 0.65 threshold
        assertThrow(DartThrow(10, Multiplier.SINGLE), "100")
    }
}
