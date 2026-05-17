package com.bilimusic.app.player

class PlaylistManager {
    val queue = mutableListOf<Song>()
    var currentIndex: Int = -1

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        queue.clear()
        queue.addAll(songs)
        currentIndex = startIndex.coerceIn(0, queue.size - 1)
    }

    fun nextIndex(): Int {
        if (queue.isEmpty()) return -1
        val next = currentIndex + 1
        return if (next < queue.size) next else -1
    }

    fun previousIndex(): Int {
        if (queue.isEmpty()) return -1
        val prev = currentIndex - 1
        return if (prev >= 0) prev else -1
    }

    fun add(song: Song) {
        queue.add(song)
    }

    fun remove(index: Int) {
        if (index in queue.indices) {
            queue.removeAt(index)
            if (index <= currentIndex) currentIndex--
        }
    }

    fun clear() {
        queue.clear()
        currentIndex = -1
    }
}
