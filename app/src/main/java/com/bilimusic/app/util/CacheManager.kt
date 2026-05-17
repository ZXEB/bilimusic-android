package com.bilimusic.app.util

import android.content.Context
import coil.Coil
import coil.ImageLoader
import com.bilimusic.app.player.PlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CacheManager {

    data class CacheStats(
        val mediaCache: Long = 0,
        val imageCache: Long = 0,
        val logCache: Long = 0,
        val totalCache: Long = 0
    )

    suspend fun getCacheStats(context: Context): CacheStats = withContext(Dispatchers.IO) {
        val mediaCache = calculateDirSize(getMediaCacheDir(context))
        val imageCache = calculateDirSize(getImageCacheDir(context))
        val logCache = calculateDirSize(getLogDir(context))
        val total = mediaCache + imageCache + logCache
        CacheStats(mediaCache, imageCache, logCache, total)
    }

    suspend fun clearMediaCache(context: Context) {
        withContext(Dispatchers.IO) {
            PlayerManager.clearCache(context)
        }
    }

    suspend fun clearImageCache(context: Context) {
        withContext(Dispatchers.IO) {
            val dir = getImageCacheDir(context)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
            try {
                val loader = Coil.imageLoader(context)
                loader.diskCache?.clear()
                loader.memoryCache?.clear()
            } catch (_: Exception) { }
        }
    }

    suspend fun clearLogCache(context: Context) {
        withContext(Dispatchers.IO) {
            val dir = getLogDir(context)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
    }

    suspend fun clearAllCache(context: Context) {
        withContext(Dispatchers.IO) {
            clearMediaCache(context)
            clearImageCache(context)
            clearLogCache(context)
        }
    }

    private fun getMediaCacheDir(context: Context): File {
        return File(context.cacheDir, "bilimusic_cache")
    }

    private fun getImageCacheDir(context: Context): File {
        return File(context.cacheDir, "image_manager_disk_cache")
    }

    private fun getLogDir(context: Context): File {
        return File(context.cacheDir, "logs")
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0
        var size = 0L
        val files = dir.listFiles() ?: return 0
        for (file in files) {
            size += if (file.isDirectory) {
                calculateDirSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
