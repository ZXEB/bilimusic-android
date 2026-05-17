package com.bilimusic.app.player

data class Song(
    val bvid: String = "",
    val title: String = "",
    val author: String = "",
    val cover: String = "",
    val audioUrl: String = "",
    val duration: Long = 0L,
    val cid: Long = 0L,
    val partTitle: String = ""
) {
    val durationFormatted: String
        get() {
            val minutes = duration / 60
            val seconds = duration % 60
            return "%02d:%02d".format(minutes, seconds)
        }
}
