package com.example.darts.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("darts_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GAME_TYPE = "pref_game_type"
        private const val KEY_STARTING_SCORE = "pref_starting_score"
        private const val KEY_LEGS = "pref_legs"
        private const val KEY_DOUBLE_OUT = "pref_double_out"
        private const val KEY_STARTING_PLAYER_DEFAULT = "pref_starting_player_default"
        private const val KEY_SOUND_EFFECTS = "pref_sound_effects"
        private const val KEY_SHOW_SUGGESTIONS = "pref_show_suggestions"
        private const val KEY_TRACK_LOCATION = "pref_track_location"
    }

    fun getGameType(): String = prefs.getString(KEY_GAME_TYPE, "x01") ?: "x01"
    fun setGameType(type: String) = prefs.edit { putString(KEY_GAME_TYPE, type) }

    fun getStartingScore(): String = prefs.getString(KEY_STARTING_SCORE, "501") ?: "501"
    fun setStartingScore(score: String) = prefs.edit { putString(KEY_STARTING_SCORE, score)}

    fun getLegs(): Int = prefs.getInt(KEY_LEGS, 3)
    fun setLegs(legs: Int) = prefs.edit { putInt(KEY_LEGS, legs) }

    fun isDoubleOut(): Boolean = prefs.getBoolean(KEY_DOUBLE_OUT, false)
    fun setDoubleOut(doubleOut: Boolean) = prefs.edit { putBoolean(KEY_DOUBLE_OUT, doubleOut) }

    fun getStartingPlayerDefault(): String = prefs.getString(KEY_STARTING_PLAYER_DEFAULT, "Random") ?: "Random"
    fun setStartingPlayerDefault(value: String) = prefs.edit {
        putString(
            KEY_STARTING_PLAYER_DEFAULT,
            value
        )
    }

    fun isSoundEffects(): Boolean = prefs.getBoolean(KEY_SOUND_EFFECTS, true)
    fun setSoundEffects(enabled: Boolean) = prefs.edit { putBoolean(KEY_SOUND_EFFECTS, enabled) }

    fun isShowSuggestions(): Boolean = prefs.getBoolean(KEY_SHOW_SUGGESTIONS, true)
    fun setShowSuggestions(enabled: Boolean) = prefs.edit {
        putBoolean(
            KEY_SHOW_SUGGESTIONS,
            enabled
        )
    }

    fun isTrackLocation(): Boolean = prefs.getBoolean(KEY_TRACK_LOCATION, false)
    fun setTrackLocation(enabled: Boolean) = prefs.edit { putBoolean(KEY_TRACK_LOCATION, enabled) }
}