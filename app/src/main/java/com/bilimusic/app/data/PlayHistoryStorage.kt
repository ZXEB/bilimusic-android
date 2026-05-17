package com.bilimusic.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bilimusic.app.player.PlayHistoryItem
import com.bilimusic.app.player.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "play_history")

class PlayHistoryStorage(private val context: Context) {

    companion object {
        private val KEY_HISTORY = stringPreferencesKey("history")
        private const val MAX_ITEMS = 200
    }

    val historyFlow = context.historyDataStore.data.map { prefs ->
        val json = prefs[KEY_HISTORY] ?: "[]"
        parseHistory(json)
    }

    suspend fun saveHistory(items: List<PlayHistoryItem>) {
        context.historyDataStore.edit { prefs ->
            prefs[KEY_HISTORY] = toJson(items.take(MAX_ITEMS))
        }
    }

    suspend fun loadHistory(): List<PlayHistoryItem> {
        val json = context.historyDataStore.data.first()[KEY_HISTORY] ?: "[]"
        return parseHistory(json)
    }

    private fun toJson(items: List<PlayHistoryItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("bvid", item.song.bvid)
                put("title", item.song.title)
                put("author", item.song.author)
                put("cover", item.song.cover)
                put("audioUrl", item.song.audioUrl)
                put("duration", item.song.duration)
                put("timestamp", item.timestamp)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseHistory(json: String): List<PlayHistoryItem> {
        val arr = JSONArray(json)
        val list = mutableListOf<PlayHistoryItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val song = Song(
                bvid = obj.optString("bvid", ""),
                title = obj.optString("title", ""),
                author = obj.optString("author", ""),
                cover = obj.optString("cover", ""),
                audioUrl = obj.optString("audioUrl", ""),
                duration = obj.optLong("duration", 0L)
            )
            val timestamp = obj.optLong("timestamp", 0L)
            list.add(PlayHistoryItem(song = song, timestamp = timestamp))
        }
        return list
    }
}
