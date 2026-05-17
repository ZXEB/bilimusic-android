package com.bilimusic.app.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BiliApiClient(private val sessdata: String = "") {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val cookieHeader: String
        get() = if (sessdata.isNotBlank()) "SESSDATA=$sessdata" else ""

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun getText(url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Referer", "https://www.bilibili.com")
            .apply { if (cookieHeader.isNotBlank()) header("Cookie", cookieHeader) }
            .build()
        return client.newCall(req).execute().use { it.body?.string() ?: "{}" }
    }

    private fun getJson(url: String): JSONObject = JSONObject(getText(url))

    fun getUserInfo(): JSONObject = getJson("https://api.bilibili.com/x/web-interface/nav")

    suspend fun getFavoriteFolders(upMid: Long): JSONObject {
        val url = WbiSigner.signUrl(
            "https://api.bilibili.com/x/v3/fav/folder/created/list",
            mapOf("up_mid" to upMid.toString(), "pn" to "1", "ps" to "100"),
            cookieHeader
        )
        return JSONObject(getText(url.toString()))
    }

    suspend fun getFavoriteDetail(folderId: Long, page: Int = 1): JSONObject {
        val url = WbiSigner.signUrl(
            "https://api.bilibili.com/x/v3/fav/resource/list",
            mapOf("media_id" to folderId.toString(), "pn" to page.toString(), "ps" to "30", "type" to "2"),
            cookieHeader
        )
        return JSONObject(getText(url.toString()))
    }

    suspend fun getVideoInfo(bvid: String): JSONObject {
        val url = WbiSigner.signUrl(
            "https://api.bilibili.com/x/web-interface/wbi/view",
            mapOf("bvid" to bvid),
            cookieHeader
        )
        return JSONObject(getText(url.toString()))
    }

    suspend fun getPlayUrl(avid: Long, cid: Long, quality: BiliQuality = BiliQuality.HIGH): JSONObject {
        val url = WbiSigner.signUrl(
            "https://api.bilibili.com/x/player/wbi/playurl",
            mapOf(
                "avid" to avid.toString(),
                "cid" to cid.toString(),
                "qn" to quality.qn.toString(),
                "fnval" to "192",
                "fnver" to "0",
                "fourk" to "1",
                "platform" to "web"
            ),
            cookieHeader
        )
        return JSONObject(getText(url.toString()))
    }

    suspend fun search(keyword: String): JSONObject {
        val url = WbiSigner.signUrl(
            "https://api.bilibili.com/x/web-interface/wbi/search/type",
            mapOf("search_type" to "video", "keyword" to keyword, "page" to "1"),
            cookieHeader
        )
        return JSONObject(getText(url.toString()))
    }

    suspend fun addToFavFolder(folderId: Long, bvid: String): JSONObject {
        val json = JSONObject().apply {
            put("rid", bvid)
            put("type", 2)
            put("add_media_ids", folderId.toString())
        }
        val url = WbiSigner.signUrl(
            "https://api.bilibili.com/x/v3/fav/resource/deal",
            mapOf(),
            cookieHeader
        ).toString().substringBefore("?")
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Referer", "https://www.bilibili.com")
            .header("Content-Type", "application/json; charset=utf-8")
            .apply {
                if (cookieHeader.isNotBlank()) header("Cookie", cookieHeader)
                val body = json.toString().toByteArray()
                post(okhttp3.RequestBody.create(JSON, body))
            }
            .build()
        return client.newCall(req).execute().use { JSONObject(it.body?.string() ?: "{}") }
    }

    suspend fun removeFromFavFolder(folderId: Long, bvid: String): JSONObject {
        val json = JSONObject().apply {
            put("rid", bvid)
            put("type", 2)
            put("del_media_ids", folderId.toString())
        }
        val url = "https://api.bilibili.com/x/v3/fav/resource/deal"
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Referer", "https://www.bilibili.com")
            .header("Content-Type", "application/json; charset=utf-8")
            .apply {
                if (cookieHeader.isNotBlank()) header("Cookie", cookieHeader)
                val body = json.toString().toByteArray()
                post(okhttp3.RequestBody.create(JSON, body))
            }
            .build()
        return client.newCall(req).execute().use { JSONObject(it.body?.string() ?: "{}") }
    }

    suspend fun getPlayerSubtitleV2(avid: Long, cid: Long): JSONObject {
        val url = WbiSigner.signUrl(
            "https://api.bilibili.com/x/player/wbi/v2",
            mapOf("aid" to avid.toString(), "cid" to cid.toString()),
            cookieHeader
        )
        return JSONObject(getText(url.toString()))
    }

    fun fetchText(url: String): String = getText(url)

    fun parseCode(jo: JSONObject): Int = jo.optInt("code", -1)
    fun parseMessage(jo: JSONObject): String = jo.optString("message", "")
    fun parseData(jo: JSONObject): JSONObject? = jo.optJSONObject("data")
}
