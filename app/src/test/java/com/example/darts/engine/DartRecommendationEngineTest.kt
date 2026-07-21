package com.example.darts.engine

import org.junit.Assert.*
import org.junit.Test

class DartRecommendationEngineTest {

    // ── Edge cases ──────────────────────────────────────────────────────

    @Test
    fun `no options for score 0`() {
        val options = DartRecommendationEngine.getCheckoutOptions(0)
        assertTrue(options.isEmpty())
    }

    @Test
    fun `no options for score 1`() {
        val options = DartRecommendationEngine.getCheckoutOptions(1)
        assertTrue(options.isEmpty())
    }

    @Test
    fun `returns single dart for score above 180`() {
        val options = DartRecommendationEngine.getCheckoutOptions(200)
        assertEquals(1, options.size)
        val option = options.first()
        assertFalse(option.finished)
        assertEquals("T20", option.darts.first().name)
    }

    // ── Simple checkouts ────────────────────────────────────────────────

    @Test
    fun `checkout for 40 recommends D20`() {
        val best = DartRecommendationEngine.getBestCheckout(40)
        assertNotNull(best)
        assertTrue(best!!.finished)
        assertTrue(best.darts.any { it.name == "D20" })
    }

    @Test
    fun `checkout for 50 recommends bull`() {
        val best = DartRecommendationEngine.getBestCheckout(50)
        assertNotNull(best)
        assertTrue(best!!.finished)
        assertTrue(best.darts.any { it.name == "BULL" })
    }

    @Test
    fun `checkout for 32 recommends D16`() {
        val best = DartRecommendationEngine.getBestCheckout(32)
        assertNotNull(best)
        assertTrue(best!!.finished)
        assertTrue(best.darts.any { it.name == "D16" })
    }

    @Test
    fun `checkout for 36 is possible`() {
        val options = DartRecommendationEngine.getCheckoutOptions(36)
        assertTrue(options.isNotEmpty())
        assertTrue(options.any { it.finished })
    }

    // ── Two-dart checkouts ──────────────────────────────────────────────

    @Test
    fun `checkout for 90 is possible with two darts`() {
        val best = DartRecommendationEngine.getBestCheckout(90)
        assertNotNull(best)
        assertTrue(best!!.finished)
        // 90 = T20 (60) + D15 (30)... but D15 doesn't exist
        // 90 = 50 + D20 (40): Bull + D20
        // Or T18 (54) + D18 (36)? D18 exists!
        // Or T20 (60) + something that leaves a double...
        // 90 = T20 + D15? No D15. 
        // 90 = 50 + 40 = Bull + D20 ✓
        assertTrue("Should have a valid checkout path for 90", best.darts.size in 1..2)
    }

    @Test
    fun `checkout for 100 is possible`() {
        val best = DartRecommendationEngine.getBestCheckout(100)
        assertNotNull(best)
        assertTrue(best!!.finished)
        // Popular route: T20 (60) + D20 (40) = 100
        assertTrue("Should finish on a double", best.darts.last().isDouble || best.darts.last().name == "BULL")
    }

    // ── Three-dart checkouts ────────────────────────────────────────────

    @Test
    fun `checkout for 170 is possible`() {
        val best = DartRecommendationEngine.getBestCheckout(170)
        assertNotNull(best)
        assertTrue(best!!.finished)
        // 170 = T20 + T20 + Bull (50) = 60+60+50
        assertEquals(3, best.darts.size)
    }

    @Test
    fun `checkout for 167 is possible`() {
        val best = DartRecommendationEngine.getBestCheckout(167)
        assertNotNull(best)
        assertTrue(best!!.finished)
        // 167 = T20 + T19 + Bull = 60+57+50
        assertEquals(3, best.darts.size)
    }

    // ── Ranking ─────────────────────────────────────────────────────────

    @Test
    fun `fewer darts is ranked higher`() {
        val options = DartRecommendationEngine.getCheckoutOptions(40)
        assertTrue(options.isNotEmpty())
        // Best should be 1-dart checkout (D20)
        val best = options.first()
        assertEquals(1, best.darts.size)
        assertEquals("D20", best.darts.first().name)
    }

    @Test
    fun `checkout options are sorted by score descending`() {
        val options = DartRecommendationEngine.getCheckoutOptions(40)
        assertTrue(options.isNotEmpty())
        for (i in 1 until options.size) {
            assertTrue("Options should be sorted by score descending: ${options[i-1].score} >= ${options[i].score}",
                options[i-1].score >= options[i].score)
        }
    }

    @Test
    fun `preferred doubles are ranked higher`() {
        val options = DartRecommendationEngine.getCheckoutOptions(40)
        val best = options.first()
        // D20 (preferred double) should be ranked higher
        assertEquals("D20", best.darts.first().name)
    }

    // ── Common checkout routes ──────────────────────────────────────────

    @Test
    fun `checkout for 41 is possible`() {
        val best = DartRecommendationEngine.getBestCheckout(41)
        assertNotNull(best)
        assertTrue(best!!.finished)
        // 41 = 9 + D16 = S9 + D16
        assertTrue("Last dart should be a double", best.darts.last().isDouble || best.darts.last().name == "BULL")
    }

    @Test
    fun `checkout for 25 is possible`() {
        val options = DartRecommendationEngine.getCheckoutOptions(25)
        assertTrue("Should have options for 25", options.isNotEmpty())
        val best = options.first()
        assertTrue("Best should be finished", best.finished)
    }

    // ── Multiple options ────────────────────────────────────────────────

    @Test
    fun `returns multiple options for common scores`() {
        val options = DartRecommendationEngine.getCheckoutOptions(40)
        assertTrue("Should have multiple checkout options for 40", options.size > 1)
    }

    @Test
    fun `getBestCheckout returns null for impossible scores`() {
        val best = DartRecommendationEngine.getBestCheckout(1)
        assertNull(best)
    }

    // ── Target details ──────────────────────────────────────────────────

    @Test
    fun `checkout targets have correct scores`() {
        val best = DartRecommendationEngine.getBestCheckout(60)
        assertNotNull(best)
        assertTrue(best!!.finished)
        val total = best.darts.sumOf { it.score }
        assertEquals(60, total)
    }

    @Test
    fun `checkout for 110 is possible`() {
        val options = DartRecommendationEngine.getCheckoutOptions(110)
        assertTrue("Should have options for 110", options.isNotEmpty())
        assertTrue("Should have a finished option", options.any { it.finished })
    }

    @Test
    fun `checkout for 120 is possible`() {
        val best = DartRecommendationEngine.getBestCheckout(120)
        assertNotNull(best)
        assertTrue(best!!.finished)
        // 120 = T20 (60) + T20 (60)? But that leaves 0, how to finish?
        // T20+T20 leaves 0, must finish on a double. 
        // Better: T20 + S20 + D20 = 60+20+40 = 120 ✓
        val total = best.darts.sumOf { it.score }
        assertEquals(120, total)
        assertTrue("Must finish on a double", best.darts.last().isDouble || best.darts.last().name == "BULL")
    }

    @Test
    fun `checkout for 164 is possible`() {
        val options = DartRecommendationEngine.getCheckoutOptions(164)
        assertTrue("164 should have checkout options", options.isNotEmpty())
        // 164 = T20 + T18 + Bull? 60+54+50=164 ✓
        val finished = options.any { it.finished }
        assertTrue("Should have a finished option for 164", finished)
    }

    @Test
    fun `checkout for 158 is possible`() {
        val options = DartRecommendationEngine.getCheckoutOptions(158)
        assertTrue("158 should have checkout options", options.isNotEmpty())
        val finished = options.any { it.finished }
        assertTrue("Should have a finished option for 158", finished)
    }
}
