package com.example.darts.utils

import androidx.compose.foundation.shape.GenericShape

val DartShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height

    moveTo(0f, 0f)                          // Top-left wing tip
    lineTo(w * 0.15f, h * 0.22f)            // Top fin inner angle
    lineTo(w * 0.82f, h * 0.20f)            // Top body edge tapering forward
    lineTo(w, h * 0.50f)                    // Razor tip point (right center)
    lineTo(w * 0.82f, h * 0.80f)            // Bottom body edge
    lineTo(w * 0.15f, h * 0.78f)            // Bottom fin inner angle
    lineTo(0f, h)                           // Bottom-left wing tip
    lineTo(w * 0.10f, h * 0.50f)            // Central tail notch
    close()
}