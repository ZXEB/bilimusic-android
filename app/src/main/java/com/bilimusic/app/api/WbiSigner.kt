package com.bilimusic.app.api

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object WbiSigner {

    private const val NAV_URL = "https://api.bilibili.com/x/web-interface/nav"
    private const val WEB_TICKET_URL = "https://api.bilibili.com/bapis/bilibili.api.ticket.v1.Ticket/GenWebTicket"
    private const val REFERER = "https://www.bilibili.com"
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private const val WBI_CACHE_MS = 10 * 60 * 1000L
    private const val WEB_TICKET_KEY = "XgwSnGZ1p"

    private val MIXIN_INDEX = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
        37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
        22, 25, 54, 21, 56, 62, 6, 63, 57, 20, 34, 52, 59, 11, 36, 44
    )

    private val keyMutex = Mutex()
    @Volatile private var cachedMixinKey: String? = null
    @Volatile private var cachedAt: Long = 0L

    private val client = OkHttpClient()

    suspend fun signUrl(baseUrl: String, params: Map<String, String>, cookieHeader: String = ""): HttpUrl {
        val mixinKey = getOrRefreshMixinKey(cookieHeader)
        val filtered = params.mapValues { (_, v) -> filterValue(v) }.toMutableMap()
        val wts = (System.currentTimeMillis() / 1000L).toString()
        filtered["wts"] = wts

        val sorted = filtered.toSortedMap()
        val query = sorted.entries.joinToString("&") { (k, v) ->
            "${urlEncode(k)}=${urlEncode(v)}"
        }
        val wRid = md5(query + mixinKey)

        val builder = baseUrl.toHttpUrl().newBuilder()
        sorted.forEach { (k, v) -> builder.addQueryParameter(k, v) }
        builder.addQueryParameter("w_rid", wRid)
        return builder.build()
    }

    private fun filterValue(v: String): String {
        return v.replace(Regex("""[!'()*]"""), "")
    }

    private suspend fun getOrRefreshMixinKey(cookieHeader: String): String {
        val now = System.currentTimeMillis()
        cachedMixinKey?.let { if (now - cachedAt < WBI_CACHE_MS) return it }
        return keyMutex.withLock {
            val again = System.currentTimeMillis()
            if (cachedMixinKey != null && again - cachedAt < WBI_CACHE_MS) return@withLock cachedMixinKey!!
            val mk = fetchMixinKey(cookieHeader)
            cachedMixinKey = mk
            cachedAt = again
            mk
        }
    }

    private suspend fun fetchMixinKey(cookieHeader: String): String = withContext(Dispatchers.IO) {
        try {
            fetchFromNav(cookieHeader)
        } catch (e: Exception) {
            fetchFromTicket(cookieHeader)
        }
    }

    private fun fetchFromNav(cookieHeader: String): String {
        val req = Request.Builder()
            .url(NAV_URL)
            .header("User-Agent", UA)
            .header("Referer", REFERER)
            .apply { if (cookieHeader.isNotBlank()) header("Cookie", cookieHeader) }
            .build()
        val text = client.newCall(req).execute().use { it.body?.string() ?: "" }
        val jo = JSONObject(text)
        val data = jo.optJSONObject("data") ?: JSONObject()
        val wbiImg = data.optJSONObject("wbi_img") ?: JSONObject()
        return ensureValidMixin(
            wbiImg.optString("img_url", ""),
            wbiImg.optString("sub_url", "")
        )
    }

    private fun fetchFromTicket(cookieHeader: String): String {
        val ts = System.currentTimeMillis() / 1000L
        val hexSign = hmacSha256Hex("ts$ts", WEB_TICKET_KEY)
        val urlBuilder = WEB_TICKET_URL.toHttpUrl().newBuilder()
            .addQueryParameter("key_id", "ec02")
            .addQueryParameter("hexsign", hexSign)
            .addQueryParameter("context[ts]", ts.toString())

        val req = Request.Builder()
            .url(urlBuilder.build())
            .header("User-Agent", UA)
            .post(ByteArray(0).toRequestBody(null))
            .build()

        val text = client.newCall(req).execute().use { it.body?.string() ?: "" }
        val jo = JSONObject(text)
        val data = jo.optJSONObject("data") ?: JSONObject()
        val nav = data.optJSONObject("nav") ?: JSONObject()
        return ensureValidMixin(
            nav.optString("img", ""),
            nav.optString("sub", "")
        )
    }

    private fun ensureValidMixin(imgUrl: String, subUrl: String): String {
        if (imgUrl.isBlank() || subUrl.isBlank()) {
            throw java.io.IOException("Invalid Wbi mixin url: img=$imgUrl sub=$subUrl")
        }
        val imgKey = imgUrl.substringAfterLast('/').substringBefore('.')
        val subKey = subUrl.substringAfterLast('/').substringBefore('.')
        val raw = imgKey + subKey
        val mixed = StringBuilder()
        for (idx in MIXIN_INDEX) {
            if (idx < raw.length) mixed.append(raw[idx])
        }
        return if (mixed.length >= 32) mixed.substring(0, 32) else mixed.toString()
    }

    private fun md5(s: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(s.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun urlEncode(v: String): String {
        return URLEncoder.encode(v, Charsets.UTF_8.name()).replace("+", "%20")
    }

    private fun hmacSha256Hex(message: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        val bytes = mac.doFinal(message.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
