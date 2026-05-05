package com.example.darts.viewModel

import com.example.darts.engine.DartThrow
import com.example.darts.ui.screens.score_entry.EntryMethod
import com.example.darts.viewModel.states.GameDisplayState
import kotlinx.coroutines.flow.StateFlow

interface BaseGameViewModel {
    val displayState: StateFlow<GameDisplayState>
    val activeEntryMethod: StateFlow<EntryMethod>

    fun addDart(dart: DartThrow)
    fun undoLastDart()
    fun commitTurn()
    fun setEntryMethod(method: EntryMethod)

    val supportedEntryMethods: List<EntryMethod>
        get() = listOf(EntryMethod.BoardButtons, EntryMethod.ScoreInput,
            EntryMethod.Voice, EntryMethod.Camera)
}