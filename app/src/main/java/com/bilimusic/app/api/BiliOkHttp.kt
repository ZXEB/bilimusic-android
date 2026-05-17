package com.bilimusic.app.api

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object BiliOkHttp {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}
