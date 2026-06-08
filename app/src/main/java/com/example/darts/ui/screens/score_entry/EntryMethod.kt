package com.example.darts.ui.screens.score_entry

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
