package com.example.darts.db.repositories

import com.example.darts.db.dao.StatDao
import com.example.darts.db.entities.PlayerCareerStats
import com.example.darts.db.entities.PlayerLegStats
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StatRepositoryTest {

    private val statDao = mockk<StatDao>(relaxed = true)
    private lateinit var repository: StatRepository

    @Before
    fun setUp() {
        clearMocks(statDao)
        repository = StatRepository(statDao)
    }

    // ── saveLegStats ────────────────────────────────────────────────────

    @Test
    fun `saveLegStats passes rows to dao and returns them`() = runTest {
        val rows = listOf(
            PlayerLegStats(gameId = 1, playerId = 1, legNumber = 1, won = true,
                dartsThrown = 9, totalScored = 501, average = 167.0f,
                checkoutAttempts = 1, checkoutsHit = 1, highestCheckout = 501,
                scores180 = 1, scores140Plus = 0, scores100Plus = 0),
            PlayerLegStats(gameId = 1, playerId = 2, legNumber = 1, won = false,
                dartsThrown = 9, totalScored = 400, average = 133.33f,
                checkoutAttempts = 0, checkoutsHit = 0, highestCheckout = 0,
                scores180 = 0, scores140Plus = 0, scores100Plus = 1),
        )

        val result = repository.saveLegStats(rows)

        coVerify { statDao.insertLegStats(rows) }
        assertEquals(rows, result)
    }

    // ── getLegStats ─────────────────────────────────────────────────────

    @Test
    fun `getLegStats delegates to dao`() = runTest {
        coEvery { statDao.getLegStats(1, 1) } returns emptyList()

        val result = repository.getLegStats(1, 1)

        coVerify { statDao.getLegStats(1, 1) }
        assertTrue(result.isEmpty())
    }

    // ── getMatchStats ───────────────────────────────────────────────────

    @Test
    fun `getMatchStats delegates to dao`() = runTest {
        coEvery { statDao.getAllLegStatsForGame(1) } returns emptyList()

        val result = repository.getMatchStats(1)

        coVerify { statDao.getAllLegStatsForGame(1) }
        assertTrue(result.isEmpty())
    }

    // ── observeLegStatsForPlayer ────────────────────────────────────────

    @Test
    fun `observeLegStatsForPlayer delegates to dao`() {
        every { statDao.observeLegStatsForPlayer(1) } returns flowOf(emptyList())

        val flow = repository.observeLegStatsForPlayer(1)

        verify { statDao.observeLegStatsForPlayer(1) }
        assertNotNull(flow)
    }

    // ── updateCareerStats ───────────────────────────────────────────────

    @Test
    fun `updateCareerStats accumulates leg stats for match winner`() = runTest {
        val captured = mutableListOf<PlayerCareerStats>()
        // Use coAnswers because insertOrReplaceCareerStats is suspend
        coEvery { statDao.insertOrReplaceCareerStats(any()) } coAnswers {
            captured.add(firstArg())
            Unit
        }

        val slot = slot<(PlayerCareerStats) -> PlayerCareerStats>()
        coEvery { statDao.upsertCareerStats(eq(1), capture(slot)) } coAnswers {
            // Apply the captured lambda to get the updated stats
            val existing = PlayerCareerStats(playerId = 1)
            val updated = slot.captured(existing)
            statDao.insertOrReplaceCareerStats(updated)
        }

        val legStat = PlayerLegStats(
            gameId = 1, playerId = 1, legNumber = 1, won = true,
            dartsThrown = 9, totalScored = 501, average = 167.0f,
            checkoutAttempts = 1, checkoutsHit = 1, highestCheckout = 100,
            scores180 = 0, scores140Plus = 0, scores100Plus = 0,
        )

        repository.updateCareerStats(listOf(legStat), isMatchEnd = true, matchWinnerId = 1)

        coVerify { statDao.upsertCareerStats(eq(1), any()) }

        assertEquals(1, captured.size)
        val updated = captured[0]
        assertEquals(1, updated.matchesPlayed)
        assertEquals(1, updated.matchesWon)
        assertEquals(1, updated.legsPlayed)
        assertEquals(1, updated.legsWon)
        assertEquals(9, updated.totalDartsThrown)
        assertEquals(501, updated.totalScored)
        assertEquals(1, updated.checkoutsAttempted)
        assertEquals(1, updated.checkoutsHit)
        assertEquals(100, updated.highestCheckout)
    }

    @Test
    fun `updateCareerStats does not count match win for non-winner`() = runTest {
        val captured = mutableListOf<PlayerCareerStats>()
        coEvery { statDao.insertOrReplaceCareerStats(any()) } coAnswers {
            captured.add(firstArg())
            Unit
        }

        val slot = slot<(PlayerCareerStats) -> PlayerCareerStats>()
        coEvery { statDao.upsertCareerStats(eq(2), capture(slot)) } coAnswers {
            val existing = PlayerCareerStats(playerId = 2)
            val updated = slot.captured(existing)
            statDao.insertOrReplaceCareerStats(updated)
        }

        val legStat = PlayerLegStats(
            gameId = 1, playerId = 2, legNumber = 1, won = false,
            dartsThrown = 12, totalScored = 300, average = 75.0f,
            checkoutAttempts = 0, checkoutsHit = 0, highestCheckout = 0,
            scores180 = 0, scores140Plus = 0, scores100Plus = 0,
        )

        repository.updateCareerStats(listOf(legStat), isMatchEnd = true, matchWinnerId = 1)

        assertEquals(1, captured.size)
        val updated = captured[0]
        assertEquals(1, updated.matchesPlayed)
        assertEquals(0, updated.matchesWon)
        assertEquals(1, updated.legsPlayed)
        assertEquals(0, updated.legsWon)
    }

    @Test
    fun `updateCareerStats tracks highest checkout`() = runTest {
        val captured = mutableListOf<PlayerCareerStats>()
        coEvery { statDao.insertOrReplaceCareerStats(any()) } coAnswers {
            captured.add(firstArg())
            Unit
        }

        val slot = slot<(PlayerCareerStats) -> PlayerCareerStats>()
        coEvery { statDao.upsertCareerStats(eq(1), capture(slot)) } coAnswers {
            val existing = PlayerCareerStats(playerId = 1, highestCheckout = 50)
            val updated = slot.captured(existing)
            statDao.insertOrReplaceCareerStats(updated)
        }

        val legStat = PlayerLegStats(
            gameId = 1, playerId = 1, legNumber = 1, won = true,
            dartsThrown = 3, totalScored = 120, average = 120.0f,
            checkoutAttempts = 1, checkoutsHit = 1, highestCheckout = 120,
            scores180 = 0, scores140Plus = 0, scores100Plus = 1,
        )

        repository.updateCareerStats(listOf(legStat), isMatchEnd = false)

        assertEquals(1, captured.size)
        val updated = captured[0]
        assertEquals(120, updated.highestCheckout)
        assertEquals(1, updated.scores100Plus)
        assertEquals(0, updated.matchesPlayed)
    }

    @Test
    fun `updateCareerStats accumulates across multiple players`() = runTest {
        val captured = mutableListOf<PlayerCareerStats>()
        coEvery { statDao.insertOrReplaceCareerStats(any()) } coAnswers {
            captured.add(firstArg())
            Unit
        }

        val slot1 = slot<(PlayerCareerStats) -> PlayerCareerStats>()
        val slot2 = slot<(PlayerCareerStats) -> PlayerCareerStats>()
        coEvery { statDao.upsertCareerStats(eq(1), capture(slot1)) } coAnswers {
            val existing = PlayerCareerStats(playerId = 1)
            statDao.insertOrReplaceCareerStats(slot1.captured(existing))
        }
        coEvery { statDao.upsertCareerStats(eq(2), capture(slot2)) } coAnswers {
            val existing = PlayerCareerStats(playerId = 2)
            statDao.insertOrReplaceCareerStats(slot2.captured(existing))
        }

        val legStats = listOf(
            PlayerLegStats(gameId = 1, playerId = 1, legNumber = 1, won = true,
                dartsThrown = 9, totalScored = 501, average = 167.0f,
                checkoutAttempts = 1, checkoutsHit = 1, highestCheckout = 100,
                scores180 = 1, scores140Plus = 0, scores100Plus = 0),
            PlayerLegStats(gameId = 1, playerId = 2, legNumber = 1, won = false,
                dartsThrown = 12, totalScored = 300, average = 75.0f,
                checkoutAttempts = 0, checkoutsHit = 0, highestCheckout = 0,
                scores180 = 0, scores140Plus = 0, scores100Plus = 0),
        )

        repository.updateCareerStats(legStats, isMatchEnd = true, matchWinnerId = 1)

        assertEquals(2, captured.size)

        val p1 = captured.find { it.playerId == 1 }
        val p2 = captured.find { it.playerId == 2 }

        assertNotNull(p1)
        assertNotNull(p2)

        assertEquals(1, p1!!.matchesPlayed)
        assertEquals(1, p1.matchesWon)
        assertEquals(1, p1.scores180)

        assertEquals(1, p2!!.matchesPlayed)
        assertEquals(0, p2.matchesWon)
    }

    // ── observeCareerStats ──────────────────────────────────────────────

    @Test
    fun `observeCareerStats delegates to dao`() {
        every { statDao.observeCareerStats(1) } returns flowOf(null)

        val flow = repository.observeCareerStats(1)

        verify { statDao.observeCareerStats(1) }
        assertNotNull(flow)
    }
}
