package com.bilimusic.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bilimusic_settings")

class PreferencesStorage(private val context: Context) {

    companion object {
        private val KEY_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_SESSDATA = stringPreferencesKey("sessdata")
        private val KEY_USER_MID = longPreferencesKey("user_mid")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_AVATAR = stringPreferencesKey("user_avatar")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_SEED_COLOR = stringPreferencesKey("seed_color")
        private val KEY_AUDIO_QUALITY = stringPreferencesKey("audio_quality")

        private val KEY_PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        private val KEY_PLAYBACK_PITCH = floatPreferencesKey("playback_pitch")
        private val KEY_PLAYBACK_LOUDNESS_GAIN_MB = intPreferencesKey("playback_loudness_gain_mb")
        private val KEY_PLAYBACK_EQUALIZER_ENABLED = booleanPreferencesKey("playback_equalizer_enabled")
        private val KEY_PLAYBACK_EQUALIZER_PRESET = stringPreferencesKey("playback_equalizer_preset")
        private val KEY_PLAYBACK_EQUALIZER_CUSTOM_BAND_LEVELS = stringPreferencesKey("playback_equalizer_custom_band_levels")
        private val KEY_SELECTED_FAV_FOLDER_IDS = stringPreferencesKey("selected_fav_folder_ids")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOGGED_IN] ?: false
    }

    val sessdata: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SESSDATA] ?: ""
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME] ?: ""
    }

    val userAvatar: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_AVATAR] ?: ""
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_THEME] ?: true
    }

    val seedColor: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SEED_COLOR] ?: "00A1D6"
    }

    val audioQuality: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUDIO_QUALITY] ?: "high"
    }

    val playbackSpeed: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_PLAYBACK_SPEED] ?: 1.0f
    }
    val playbackPitch: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_PLAYBACK_PITCH] ?: 1.0f
    }
    val playbackLoudnessGainMb: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_PLAYBACK_LOUDNESS_GAIN_MB] ?: 0
    }
    val playbackEqualizerEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PLAYBACK_EQUALIZER_ENABLED] ?: false
    }
    val playbackEqualizerPreset: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PLAYBACK_EQUALIZER_PRESET] ?: "flat"
    }
    val playbackEqualizerCustomBandLevels: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PLAYBACK_EQUALIZER_CUSTOM_BAND_LEVELS] ?: ""
    }

    val selectedFavFolderIds: Flow<List<Long>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SELECTED_FAV_FOLDER_IDS] ?: ""
        if (raw.isEmpty()) emptyList()
        else raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    suspend fun saveLoginInfo(sessdata: String, mid: Long, name: String, avatar: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = true
            prefs[KEY_SESSDATA] = sessdata
            prefs[KEY_USER_MID] = mid
            prefs[KEY_USER_NAME] = name
            prefs[KEY_USER_AVATAR] = avatar
        }
    }

    suspend fun clearLoginInfo() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DARK_THEME] = dark
        }
    }

    suspend fun setSeedColor(color: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SEED_COLOR] = color
        }
    }

    suspend fun setAudioQuality(quality: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUDIO_QUALITY] = quality
        }
    }

    suspend fun setPlaybackSpeed(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PLAYBACK_SPEED] = value
        }
    }

    suspend fun setPlaybackPitch(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PLAYBACK_PITCH] = value
        }
    }

    suspend fun setPlaybackLoudnessGainMb(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PLAYBACK_LOUDNESS_GAIN_MB] = value
        }
    }

    suspend fun setPlaybackEqualizerEnabled(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PLAYBACK_EQUALIZER_ENABLED] = value
        }
    }

    suspend fun setPlaybackEqualizerPreset(value: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PLAYBACK_EQUALIZER_PRESET] = value
        }
    }

    suspend fun setPlaybackEqualizerCustomBandLevels(value: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PLAYBACK_EQUALIZER_CUSTOM_BAND_LEVELS] = value
        }
    }

    suspend fun getAudioQuality(): String {
        return context.dataStore.data.first()[KEY_AUDIO_QUALITY] ?: "high"
    }

    suspend fun getSessdata(): String {
        return context.dataStore.data.first()[KEY_SESSDATA] ?: ""
    }

    suspend fun getLoggedIn(): Boolean {
        return context.dataStore.data.first()[KEY_LOGGED_IN] ?: false
    }

    suspend fun getUserName(): String {
        return context.dataStore.data.first()[KEY_USER_NAME] ?: ""
    }

    suspend fun getUserAvatar(): String {
        return context.dataStore.data.first()[KEY_USER_AVATAR] ?: ""
    }

    suspend fun getUserMid(): Long {
        return context.dataStore.data.first()[KEY_USER_MID] ?: 0L
    }

    suspend fun setSelectedFavFolderIds(ids: List<Long>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_FAV_FOLDER_IDS] = ids.joinToString(",")
        }
    }
}
