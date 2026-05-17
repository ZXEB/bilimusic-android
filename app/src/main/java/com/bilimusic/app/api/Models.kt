package com.bilimusic.app.api

data class User(
    val mid: Long = 0,
    val uname: String = "",
    val face: String = "",
    val level: Int = 0,
    val vipType: Int = 0
)

data class FavoriteFolder(
    val id: Long = 0,
    val title: String = "",
    val cover: String = "",
    val media_count: Int = 0
)

data class VideoInfo(
    val bvid: String = "",
    val aid: Long = 0,
    val title: String = "",
    val pic: String = "",
    val duration: Int = 0,
    val owner: OwnerInfo? = null,
    val stat: StatInfo? = null,
    val cid: Long = 0
)

data class OwnerInfo(
    val mid: Long = 0,
    val name: String = "",
    val face: String = ""
)

data class StatInfo(
    val view: Long = 0,
    val like: Long = 0
)

data class AudioTrack(
    val baseUrl: String = "",
    val id: Int = 0,
    val codec: String = "",
    val bandwidth: Int = 0,
    val mimeType: String = "",
    val qualityTag: String? = null,
    val candidateUrls: List<String> = emptyList()
)

enum class BiliQuality(val key: String, val qn: Int, val fnval: Int, val minBitrateKbps: Int) {
    DOLBY("dolby", 0, 128, 0),
    HIRES("hires", 0, 64, 1000),
    LOSSLESS("lossless", 30216, 16, 500),
    HIGH("high", 80, 16, 180),
    MEDIUM("medium", 64, 16, 120),
    LOW("low", 32, 16, 60);

    companion object {
        val degradationChain = listOf(DOLBY, HIRES, LOSSLESS, HIGH, MEDIUM, LOW)

        fun fromKey(key: String): BiliQuality =
            degradationChain.find { it.key == key } ?: HIGH

        fun degradeChain(from: BiliQuality): List<BiliQuality> {
            val startIdx = degradationChain.indexOf(from).coerceAtLeast(0)
            return degradationChain.drop(startIdx)
        }
    }
}

data class PlayUrlData(
    val dash: DashData? = null
)

data class DashData(
    val audio: List<AudioTrack> = emptyList()
)

data class BilibiliResponse<T>(
    val code: Int = -1,
    val message: String = "",
    val data: T? = null
)

data class NavData(
    val mid: Long = 0,
    val uname: String = "",
    val face: String = "",
    val level: Int = 0,
    val vipType: Int = 0
)

data class FavoriteListData(
    val list: List<FavoriteFolder> = emptyList()
)

data class FavoriteResourceData(
    val medias: List<FavoriteMedia> = emptyList(),
    val hasMore: Boolean = false
)

data class FavoriteMedia(
    val bvid: String = "",
    val title: String = "",
    val cover: String = "",
    val duration: Int = 0,
    val owner: OwnerInfo? = null
)

data class SearchData(
    val result: List<SearchVideo> = emptyList()
)

data class SearchVideo(
    val bvid: String = "",
    val title: String = "",
    val pic: String = "",
    val duration: Int = 0,
    val author: String = "",
    val authorFace: String = ""
)
