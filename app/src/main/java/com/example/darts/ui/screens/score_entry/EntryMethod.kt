package com.example.darts.ui.screens.score_entry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

sealed class EntryMethod(
    val label: String,
    val icon: String
) {
    object BoardButtons : EntryMethod("Board", "grid_on")

    object ScoreInput : EntryMethod("Score", "keyboard")

    object Voice : EntryMethod("Voice", "mic")

    object Camera : EntryMethod("Camera", "videocam")

    object  Cricket : EntryMethod("Cricket", "sports_cricket")
}

/**
 * Compose icon for an entry method. Lives next to [EntryMethod] because it is a
 * property of the method, not of any one screen — it was previously duplicated
 * verbatim as a private function in both BoardButtonsScreen.kt and GameScreen.kt.
 */
internal fun methodIcon(method: EntryMethod): ImageVector = when (method) {
    is EntryMethod.BoardButtons -> Icons.Default.GridOn
    is EntryMethod.ScoreInput   -> Icons.Default.Keyboard
    is EntryMethod.Voice        -> Icons.Default.Mic
    is EntryMethod.Camera       -> Icons.Default.Videocam
    is EntryMethod.Cricket      -> Icons.Default.SportsCricket
}
