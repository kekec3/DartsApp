package com.example.darts.viewModel

import com.example.darts.db.entities.Battle
import com.example.darts.db.entities.Player
import com.example.darts.db.repositories.BattleRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BattleViewModelTest {

    private val repository = mockk<BattleRepository>(relaxed = true)
    private lateinit var viewModel: BattleViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clearMocks(repository)
        every { repository.getAllPlayers() } returns MutableStateFlow(emptyList())
        every { repository.getAllBattles() } returns MutableStateFlow(emptyList())
        viewModel = BattleViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Name change ─────────────────────────────────────────────────────

    @Test
    fun `onNameChange updates battle name`() {
        viewModel.onNameChange("Friday Night Darts")
        assertEquals("Friday Night Darts", viewModel.battleName.value)
    }

    @Test
    fun `onNameChange with empty string`() {
        viewModel.onNameChange("")
        assertEquals("", viewModel.battleName.value)
    }

    // ── Player selection ────────────────────────────────────────────────

    @Test
    fun `togglePlayer adds player when not selected`() {
        viewModel.togglePlayer(1)
        assertTrue(viewModel.selectedPlayerIds.value.contains(1))
        assertEquals(1, viewModel.selectedPlayerIds.value.size)
    }

    @Test
    fun `togglePlayer removes player when already selected`() {
        viewModel.togglePlayer(1)
        viewModel.togglePlayer(1)
        assertFalse(viewModel.selectedPlayerIds.value.contains(1))
        assertTrue(viewModel.selectedPlayerIds.value.isEmpty())
    }

    @Test
    fun `togglePlayer handles multiple players`() {
        viewModel.togglePlayer(1)
        viewModel.togglePlayer(2)
        viewModel.togglePlayer(3)
        assertEquals(3, viewModel.selectedPlayerIds.value.size)
        assertTrue(viewModel.selectedPlayerIds.value.containsAll(setOf(1, 2, 3)))
    }

    @Test
    fun `togglePlayer can toggle individual player without affecting others`() {
        viewModel.togglePlayer(1)
        viewModel.togglePlayer(2)
        viewModel.togglePlayer(2) // remove player 2
        assertEquals(1, viewModel.selectedPlayerIds.value.size)
        assertTrue(viewModel.selectedPlayerIds.value.contains(1))
    }

    // ── Add new player ──────────────────────────────────────────────────

    @Test
    fun `addNewPlayer inserts player and toggles selection`() = runTest(testDispatcher) {
        coEvery { repository.insertPlayer(any()) } returns 5L

        viewModel.addNewPlayer("Charlie", "")

        coVerify { repository.insertPlayer(match { it.username == "Charlie" }) }
    }

    // ── SaveBattle duplicate detection ───────────────────────────────────

    @Test
    fun `saveBattle detects duplicate battle with same players`() = runTest(testDispatcher) {
        viewModel.onNameChange("Existing Battle")
        viewModel.togglePlayer(1)
        viewModel.togglePlayer(2)

        val existingBattle = Battle(1, "Existing Battle", "1234567890")
        every { repository.getAllBattles() } returns flowOf(listOf(existingBattle))
        coEvery { repository.getPlayersOfBattle(1) } returns listOf(
            Player(1, "Alice"),
            Player(2, "Bob"),
        )

        var duplicateFound: Battle? = null
        var successId: Int? = null

        viewModel.saveBattle(
            onSuccess = { successId = it },
            onDuplicateFound = { duplicateFound = it },
        )

        assertNotNull("Should find duplicate", duplicateFound)
        assertEquals(1, duplicateFound!!.idBattle)
        assertNull("Should not call onSuccess", successId)
    }

    @Test
    fun `saveBattle creates new battle when no duplicate`() = runTest(testDispatcher) {
        viewModel.onNameChange("New Battle")
        viewModel.togglePlayer(3)

        every { repository.getAllBattles() } returns flowOf(
            listOf(Battle(1, "Existing", "0"))
        )
        coEvery { repository.getPlayersOfBattle(1) } returns listOf(Player(1, "Alice"))
        coEvery { repository.createBattleWithPlayers(any(), any()) } returns 2

        var duplicateFound: Battle? = null
        var successId: Int? = null

        viewModel.saveBattle(
            onSuccess = { successId = it },
            onDuplicateFound = { duplicateFound = it },
        )

        assertNull("Should not find duplicate", duplicateFound)
        assertNotNull("Should succeed", successId)
        assertEquals(2, successId!!.toLong())
        coVerify { repository.createBattleWithPlayers("New Battle", listOf(3)) }
    }

    // ── availablePlayers ────────────────────────────────────────────────

    @Test
    fun `availablePlayers is initially empty`() {
        assertTrue(viewModel.availablePlayers.value.isEmpty())
    }
}
