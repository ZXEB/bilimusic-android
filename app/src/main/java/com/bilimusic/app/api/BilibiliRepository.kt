package com.bilimusic.app.api

import com.bilimusic.app.player.LyricEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BilibiliRepository(
    private val api: BiliApiClient
) {
    suspend fun login(sessdata: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val jo = api.getUserInfo()
            if (api.parseCode(jo) == 0) {
                val data = api.parseData(jo) ?: return@withContext Result.failure(Exception("no data"))
                Result.success(User(
                    mid = data.optLong("mid", 0),
                    uname = data.optString("uname", ""),
                    face = data.optString("face", ""),
                    level = data.optInt("level", 0),
                    vipType = data.optJSONObject("vip")?.optInt("type", 0) ?: 0
                ))
            } else {
                Result.failure(Exception(api.parseMessage(jo)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFavoriteFolders(mid: Long): Result<List<FavoriteFolder>> = withContext(Dispatchers.IO) {
        try {
            val jo = api.getFavoriteFolders(mid)
            if (api.parseCode(jo) == 0) {
                val data = api.parseData(jo) ?: return@withContext Result.failure(Exception("no data"))
                val list = data.optJSONArray("list") ?: return@withContext Result.success(emptyList())
                val folders = (0 until list.length()).map { i ->
                    val item = list.getJSONObject(i)
                    FavoriteFolder(
                        id = item.optLong("id", 0),
                        title = item.optString("title", ""),
                        cover = item.optString("cover", ""),
                        media_count = item.optInt("media_count", 0)
                    )
                }
                Result.success(folders)
            } else {
                Result.failure(Exception(api.parseMessage(jo)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFavoriteDetail(folderId: Long): Result<List<VideoInfo>> = withContext(Dispatchers.IO) {
        try {
            val jo = api.getFavoriteDetail(folderId)
            if (api.parseCode(jo) == 0) {
                val data = api.parseData(jo) ?: return@withContext Result.failure(Exception("no data"))
                val medias = data.optJSONArray("medias") ?: return@withContext Result.success(emptyList())
                val videos = (0 until medias.length()).map { i ->
                    val m = medias.getJSONObject(i)
                    val owner = m.optJSONObject("owner")
                    VideoInfo(
                        bvid = m.optString("bvid", ""),
                        aid = m.optLong("aid", 0),
                        title = m.optString("title", ""),
                        pic = m.optString("cover", ""),
                        duration = m.optInt("duration", 0),
                        cid = m.optLong("cid", 0),
                        owner = owner?.let { OwnerInfo(
                            mid = it.optLong("mid", 0),
                            name = it.optString("name", ""),
                            face = it.optString("face", "")
                        )}
                    )
                }
                Result.success(videos)
            } else {
                Result.failure(Exception(api.parseMessage(jo)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVideoInfo(bvid: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        try {
            val jo = api.getVideoInfo(bvid)
            if (api.parseCode(jo) == 0) {
                val data = api.parseData(jo) ?: return@withContext Result.failure(Exception("no data"))
                val owner = data.optJSONObject("owner")
                val stat = data.optJSONObject("stat")
                Result.success(VideoInfo(
                    bvid = data.optString("bvid", ""),
                    aid = data.optLong("aid", 0),
                    title = data.optString("title", ""),
                    pic = data.optString("pic", ""),
                    duration = data.optInt("duration", 0),
                    cid = data.optLong("cid", 0),
                    owner = owner?.let { OwnerInfo(
                        mid = it.optLong("mid", 0),
                        name = it.optString("name", ""),
                        face = it.optString("face", "")
                    )},
                    stat = stat?.let { StatInfo(
                        view = it.optLong("view", 0),
                        like = it.optLong("like", 0)
                    )}
                ))
            } else {
                Result.failure(Exception(api.parseMessage(jo)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAudioUrl(bvid: String, preferredQuality: BiliQuality = BiliQuality.HIGH): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val info = getVideoInfo(bvid).getOrElse { return@withContext Result.failure(it) }
                val avid = info.aid
                val cid = info.cid
                if (cid == 0L) return@withContext Result.failure(Exception("no cid"))
                val url = fetchAudioUrlWithRetry(avid, cid, preferredQuality)
                if (url != null) Result.success(url)
                else Result.failure(Exception("No audio available"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun fetchAudioUrlWithRetry(avid: Long, cid: Long, preferredQuality: BiliQuality): String? {
        var lastError: Throwable? = null
        val maxRetries = 3

        for (attempt in 0 until maxRetries) {
            try {
                val playJo = api.getPlayUrl(avid, cid, preferredQuality)
                if (api.parseCode(playJo) != 0) {
                    val msg = api.parseMessage(playJo)
                    if (attempt < maxRetries - 1) {
                        kotlinx.coroutines.delay(500L * (attempt + 1))
                        continue
                    }
                    lastError = Exception(msg)
                    break
                }
                val data = api.parseData(playJo)
                if (data == null) {
                    if (attempt < maxRetries - 1) {
                        kotlinx.coroutines.delay(500L * (attempt + 1))
                        continue
                    }
                    lastError = Exception("no playurl data")
                    return null
                }

                val allTracks = parseAllAudioTracks(data)
                if (allTracks.isNotEmpty()) {
                    val selected = selectStreamByPreference(allTracks, preferredQuality.key)
                    if (selected != null) {
                        val bestUrl = prioritizeBiliStreamUrls(selected.baseUrl, selected.candidateUrls)
                            .firstOrNull() ?: selected.baseUrl
                        if (bestUrl.isNotEmpty()) return bestUrl
                    }
                }

                val durl = data.optJSONArray("durl")
                if (durl != null && durl.length() > 0) {
                    val first = durl.getJSONObject(0)
                    val url = first.optString("url", "")
                    if (url.isNotEmpty()) {
                        val backups = parseBackupUrls(first)
                        val best = prioritizeBiliStreamUrls(url, backups).firstOrNull() ?: url
                        return best
                    }
                }

                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay(500L * (attempt + 1))
                }
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay(500L * (attempt + 1))
                }
            }
        }

        return null
    }

    // === track parsing (from NeriPlayer's PlayInfo.toAudioStreamInfos) ===

    private fun parseAllAudioTracks(data: org.json.JSONObject): List<AudioTrack> {
        val tracks = mutableListOf<AudioTrack>()
        val dash = data.optJSONObject("dash") ?: return tracks

        val normalAudio = dash.optJSONArray("audio")
        if (normalAudio != null) {
            for (i in 0 until normalAudio.length()) {
                val a = normalAudio.getJSONObject(i)
                tracks += AudioTrack(
                    baseUrl = a.optString("baseUrl", ""),
                    id = a.optInt("id", 0),
                    codec = a.optString("codec", ""),
                    bandwidth = bitrateKbpsFromBandwidth(a.optLong("bandwidth", 0)),
                    mimeType = a.optString("mimeType", "audio/mp4"),
                    qualityTag = null,
                    candidateUrls = parseBackupUrls(a)
                )
            }
        }

        val dolby = dash.optJSONObject("dolby")
        val dolbyAudio = dolby?.optJSONArray("audio")
        if (dolbyAudio != null) {
            for (i in 0 until dolbyAudio.length()) {
                val a = dolbyAudio.getJSONObject(i)
                tracks += AudioTrack(
                    baseUrl = a.optString("baseUrl", ""),
                    id = a.optInt("id", 0),
                    codec = a.optString("codec", ""),
                    bandwidth = bitrateKbpsFromBandwidth(a.optLong("bandwidth", 0)),
                    mimeType = a.optString("mimeType", "audio/eac3"),
                    qualityTag = "dolby",
                    candidateUrls = parseBackupUrls(a)
                )
            }
        }

        val flac = dash.optJSONObject("flac")
        val flacAudio = flac?.optJSONObject("audio")
        if (flacAudio != null) {
            val a = flacAudio
            tracks += AudioTrack(
                baseUrl = a.optString("baseUrl", ""),
                id = a.optInt("id", 0),
                codec = a.optString("codec", ""),
                bandwidth = bitrateKbpsFromBandwidth(a.optLong("bandwidth", 0)),
                mimeType = a.optString("mimeType", "audio/flac"),
                qualityTag = "hires",
                candidateUrls = parseBackupUrls(a)
            )
        }

        return tracks
    }

    private fun parseBackupUrls(obj: org.json.JSONObject): List<String> {
        val arr = obj.optJSONArray("backupUrl") ?: obj.optJSONArray("backup_url") ?: return emptyList()
        return (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotEmpty() }
    }

    // === stream selection (from NeriPlayer's BiliAudioSelector) ===

    private fun selectStreamByPreference(
        available: List<AudioTrack>,
        preferredKey: String
    ): AudioTrack? {
        if (available.isEmpty()) return null
        val pref = BiliQuality.fromKey(preferredKey)

        val regularSorted = available
            .filter { it.qualityTag == null }
            .sortedByDescending { it.bandwidth }
        val taggedSorted = available
            .filter { it.qualityTag != null }
            .sortedByDescending { it.bandwidth }
        val sorted = (regularSorted + taggedSorted).distinctBy { it.baseUrl }

        when (pref) {
            BiliQuality.DOLBY ->
                sorted.firstOrNull { it.qualityTag == "dolby" }?.let { return it }
            BiliQuality.HIRES ->
                sorted.firstOrNull { it.qualityTag == "hires" }?.let { return it }
            BiliQuality.LOSSLESS ->
                sorted.firstOrNull(::isLosslessLikeStream)?.let { return it }
            else -> Unit
        }

        for (q in BiliQuality.degradeChain(pref)) {
            val hit = when (q) {
                BiliQuality.DOLBY -> sorted.firstOrNull { it.qualityTag == "dolby" }
                BiliQuality.HIRES -> sorted.firstOrNull { it.qualityTag == "hires" }
                BiliQuality.LOSSLESS ->
                    sorted.firstOrNull(::isLosslessLikeStream)
                        ?: regularSorted.firstOrNull { matchesRegularQuality(it, q) }
                else -> regularSorted.firstOrNull { matchesRegularQuality(it, q) }
            }
            if (hit != null) return hit
        }

        return sorted.firstOrNull()
    }

    private fun matchesRegularQuality(track: AudioTrack, quality: BiliQuality): Boolean {
        if (track.qualityTag != null) return false
        val upperBoundExclusive = regularQualityUpperBoundExclusive(quality)
        return track.bandwidth >= quality.minBitrateKbps &&
            track.bandwidth < upperBoundExclusive
    }

    private fun regularQualityUpperBoundExclusive(quality: BiliQuality): Int = when (quality) {
        BiliQuality.LOSSLESS -> BiliQuality.HIRES.minBitrateKbps
        BiliQuality.HIGH -> BiliQuality.LOSSLESS.minBitrateKbps
        BiliQuality.MEDIUM -> BiliQuality.HIGH.minBitrateKbps
        BiliQuality.LOW -> BiliQuality.MEDIUM.minBitrateKbps
        else -> Int.MAX_VALUE
    }

    private fun isLosslessLikeStream(track: AudioTrack): Boolean {
        if (track.qualityTag == "hires") return true
        val mimeType = track.mimeType.trim().lowercase()
        return mimeType == "audio/flac" || mimeType == "audio/x-flac"
    }

    // === URL prioritization (from NeriPlayer's BiliAudioSelector) ===

    private fun isBiliStreamHost(host: String): Boolean {
        val normalized = host.trim().lowercase()
        if (normalized.isBlank()) return false
        return normalized.contains("bilivideo.") || normalized.endsWith(".mountaintoys.cn")
    }

    private fun isBiliStreamUrl(url: String): Boolean =
        kotlin.runCatching { java.net.URI(url).host.orEmpty() }
            .getOrNull()
            ?.let(::isBiliStreamHost) == true

    private fun scoreBiliStreamUrl(url: String): Int {
        val host = kotlin.runCatching { java.net.URI(url).host.orEmpty().lowercase() }.getOrDefault("")
        return when {
            host.startsWith("upos-") && host.contains("bilivideo.") -> 3
            host.contains("bilivideo.") -> 2
            host.endsWith(".mountaintoys.cn") -> 1
            else -> 0
        }
    }

    private fun prioritizeBiliStreamUrls(primaryUrl: String, backupUrls: List<String>): List<String> {
        val deduped = buildList {
            add(primaryUrl)
            addAll(backupUrls)
        }.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        return deduped.withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<String>> { scoreBiliStreamUrl(it.value) }
                    .thenBy { it.index }
            )
            .map { it.value }
    }

    private fun bitrateKbpsFromBandwidth(bandwidth: Long): Int =
        maxOf(0, (bandwidth / 1000L).toInt())

    suspend fun addToFavFolder(folderId: Long, bvid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jo = api.addToFavFolder(folderId, bvid)
            if (api.parseCode(jo) == 0) Result.success(Unit)
            else Result.failure(Exception(api.parseMessage(jo)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromFavFolder(folderId: Long, bvid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jo = api.removeFromFavFolder(folderId, bvid)
            if (api.parseCode(jo) == 0) Result.success(Unit)
            else Result.failure(Exception(api.parseMessage(jo)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFavoriteDetailAll(folderId: Long): Result<List<VideoInfo>> = withContext(Dispatchers.IO) {
        try {
            val allVideos = mutableListOf<VideoInfo>()
            var page = 1
            var hasMore = true
            while (hasMore && page <= 100) {
                val jo = api.getFavoriteDetail(folderId, page)
                if (api.parseCode(jo) == 0) {
                    val data = api.parseData(jo) ?: break
                    val medias = data.optJSONArray("medias") ?: break
                    for (i in 0 until medias.length()) {
                        val m = medias.getJSONObject(i)
                        val owner = m.optJSONObject("upper")
                        allVideos.add(VideoInfo(
                            bvid = m.optString("bvid", ""),
                            aid = m.optLong("id", 0),
                            title = m.optString("title", ""),
                            pic = m.optString("cover", ""),
                            duration = m.optInt("duration", 0),
                            owner = owner?.let { OwnerInfo(
                                mid = it.optLong("mid", 0),
                                name = it.optString("name", ""),
                                face = it.optString("face", "")
                            )}
                        ))
                    }
                    hasMore = data.optBoolean("has_more", false)
                    page++
                } else break
            }
            Result.success(allVideos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun search(keyword: String): Result<List<VideoInfo>> = withContext(Dispatchers.IO) {
        try {
            val jo = api.search(keyword)
            if (api.parseCode(jo) == 0) {
                val data = api.parseData(jo) ?: return@withContext Result.failure(Exception("no data"))
                val result = data.optJSONArray("result") ?: return@withContext Result.success(emptyList())
                val videos = (0 until result.length())
                    .map { result.getJSONObject(it) }
                    .filter { it.optString("type", "") == "video" }
                    .map { r ->
                        val rawPic = r.optString("pic", "")
                        val pic = if (rawPic.startsWith("//")) "https:$rawPic" else rawPic
                        val rawTitle = r.optString("title", "")
                        val title = rawTitle.replace(Regex("<[^>]*>"), "")
                        val rawDuration = r.optString("duration", "0")
                        val duration = parseDurationToSeconds(rawDuration)
                        VideoInfo(
                            bvid = r.optString("bvid", ""),
                            aid = r.optLong("aid", 0),
                            title = title,
                            pic = pic,
                            duration = duration,
                            owner = OwnerInfo(
                                name = r.optString("author", ""),
                                face = r.optString("upic", "")
                            )
                        )
                    }
                Result.success(videos)
            } else {
                Result.failure(Exception(api.parseMessage(jo)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseDurationToSeconds(duration: String): Int {
        val parts = duration.split(":")
        return when (parts.size) {
            2 -> parts[0].toIntOrNull()?.let { it * 60 + (parts[1].toIntOrNull() ?: 0) } ?: 0
            3 -> parts[0].toIntOrNull()?.let { it * 3600 + (parts[1].toIntOrNull() ?: 0) * 60 + (parts[2].toIntOrNull() ?: 0) } ?: 0
            else -> duration.toIntOrNull() ?: 0
        }
    }

    suspend fun getSubtitles(bvid: String): Result<List<LyricEntry>> = withContext(Dispatchers.IO) {
        try {
            val info = getVideoInfo(bvid).getOrElse { return@withContext Result.failure(it) }
            val avid = info.aid
            val cid = info.cid
            if (cid == 0L) return@withContext Result.failure(Exception("no cid"))

            val jo = api.getPlayerSubtitleV2(avid, cid)
            if (api.parseCode(jo) != 0) return@withContext Result.failure(Exception(api.parseMessage(jo)))

            val data = api.parseData(jo) ?: return@withContext Result.failure(Exception("no data"))
            val subtitle = data.optJSONObject("subtitle") ?: return@withContext Result.success(emptyList())
            val subtitles = subtitle.optJSONArray("subtitles") ?: return@withContext Result.success(emptyList())

            val subtitleItems = (0 until subtitles.length()).map { subtitles.getJSONObject(it) }
            val selectedUrl = subtitleItems.firstOrNull(::isPreferredSubtitle)
                ?.optString("subtitle_url", "")
                ?.takeIf { it.isNotBlank() }
                ?: subtitleItems.firstOrNull()
                    ?.optString("subtitle_url", "")
                    ?.takeIf { it.isNotBlank() }
            if (selectedUrl.isNullOrEmpty()) return@withContext Result.failure(Exception("no subtitle url"))

            val subtitleContent = api.fetchText(normalizeSubtitleUrl(selectedUrl))
            val subtitleJson = org.json.JSONObject(subtitleContent)
            val body = subtitleJson.optJSONArray("body") ?: return@withContext Result.success(emptyList())

            val entries = (0 until body.length()).mapNotNull { i ->
                val b = body.getJSONObject(i)
                val text = b.optString("content", "").trim()
                val start = (b.optDouble("from", 0.0) * 1000).toLong().coerceAtLeast(0L)
                val end = (b.optDouble("to", 0.0) * 1000).toLong().coerceAtLeast(start + 1L)
                if (text.isBlank()) null else LyricEntry(text = text, startTimeMs = start, endTimeMs = end)
            }.sortedBy { it.startTimeMs }
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isPreferredSubtitle(subtitle: org.json.JSONObject): Boolean {
        val lan = subtitle.optString("lan", "").lowercase()
        val lanDoc = subtitle.optString("lan_doc", "").lowercase()
        return lan == "zh" ||
            lan.startsWith("zh-") ||
            lanDoc.contains("中文") ||
            lanDoc.contains("chinese") ||
            lanDoc.contains("简体") ||
            lanDoc.contains("繁体")
    }

    private fun normalizeSubtitleUrl(url: String): String {
        val clean = url.trim().replace("\\/", "/")
        return when {
            clean.startsWith("//") -> "https:$clean"
            clean.startsWith("http://") -> clean.replaceFirst("http://", "https://")
            else -> clean
        }
    }
}
