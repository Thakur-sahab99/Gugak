package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.RepeatMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("gugak_preferences", Context.MODE_PRIVATE)

    private fun loadThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    private fun loadRepeatMode(): RepeatMode {
        val name = prefs.getString(KEY_REPEAT_MODE, RepeatMode.OFF.name)
        return try {
            RepeatMode.valueOf(name ?: RepeatMode.OFF.name)
        } catch (e: Exception) {
            RepeatMode.OFF
        }
    }

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _hasSeenOnboarding = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING, false))
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding.asStateFlow()

    private val _shuffle = MutableStateFlow(prefs.getBoolean(KEY_SHUFFLE, false))
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(loadRepeatMode())
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _dynamicColor.value = enabled
    }

    fun setHasSeenOnboarding(seen: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING, seen).apply()
        _hasSeenOnboarding.value = seen
    }

    fun setShuffle(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHUFFLE, enabled).apply()
        _shuffle.value = enabled
    }

    fun setRepeatMode(mode: RepeatMode) {
        prefs.edit().putString(KEY_REPEAT_MODE, mode.name).apply()
        _repeatMode.value = mode
    }

    fun saveLastPlayback(songId: Long, positionMs: Long) {
        prefs.edit()
            .putLong(KEY_LAST_SONG_ID, songId)
            .putLong(KEY_LAST_POSITION, positionMs)
            .apply()
    }

    fun getLastSongId(): Long = prefs.getLong(KEY_LAST_SONG_ID, -1L)
    fun getLastPositionMs(): Long = prefs.getLong(KEY_LAST_POSITION, 0L)

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_ONBOARDING = "has_seen_onboarding"
        private const val KEY_SHUFFLE = "shuffle_mode"
        private const val KEY_REPEAT_MODE = "repeat_mode"
        private const val KEY_LAST_SONG_ID = "last_song_id"
        private const val KEY_LAST_POSITION = "last_position"
    }
}
