package com.example.darts.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    fun playScore(score: Int) {
        val resName = "an_$score"
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
        if (resId != 0) playSound(resId)
        // If resId == 0 the file doesn't exist, just silently skip
    }

    fun playGameShot() {
        val resId = context.resources.getIdentifier("gsm", "raw", context.packageName)
        if (resId != 0) playSound(resId)
    }

    fun playGameOn() {
        val resId = context.resources.getIdentifier("game_on", "raw", context.packageName)
        Log.d("SoundManager", "playGameOn - resId: $resId")
        if (resId != 0) playSound(resId)
        else Log.e("SoundManager", "gameon file not found!")
    }

    private fun playSound(resId: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, resId)?.apply {
            setOnCompletionListener { release() }
            start()
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}