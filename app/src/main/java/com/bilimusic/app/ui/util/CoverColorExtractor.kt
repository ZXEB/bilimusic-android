package com.bilimusic.app.ui.util

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val dominantColorCache = LruCache<String, Color>(80)

@Composable
fun rememberDominantColor(
    imageUrl: String,
    defaultColor: Color = Color(0xFF00A1D6)
): State<Color> {
    val context = LocalContext.current.applicationContext
    return produceState(initialValue = defaultColor, key1 = imageUrl) {
        if (imageUrl.isEmpty()) return@produceState
        dominantColorCache.get(imageUrl)?.let {
            value = it
            return@produceState
        }
        val color = withContext(Dispatchers.IO) {
            extractDominantColor(context, imageUrl)
        }
        val resolved = color ?: defaultColor
        dominantColorCache.put(imageUrl, resolved)
        value = resolved
    }
}

private suspend fun extractDominantColor(context: android.content.Context, imageUrl: String): Color? {
    val loader = context.imageLoader
    val request = ImageRequest.Builder(context)
        .data(imageUrl)
        .size(Size(72, 72))
        .allowHardware(false)
        .build()

    val result = runCatching { loader.execute(request) }.getOrNull() ?: return null
    val drawable = result.drawable ?: return null

    val bitmap = drawable.let { d ->
        if (d is android.graphics.drawable.BitmapDrawable) d.bitmap.copy(Bitmap.Config.ARGB_8888, true)
        else {
            val bmp = Bitmap.createBitmap(d.intrinsicWidth.coerceAtLeast(1), d.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            d.setBounds(0, 0, canvas.width, canvas.height)
            d.draw(canvas)
            bmp
        }
    } ?: return null

    val palette = Palette.Builder(bitmap).maximumColorCount(16).generate()
    val swatch = palette.darkVibrantSwatch
        ?: palette.vibrantSwatch
        ?: palette.dominantSwatch
        ?: palette.darkMutedSwatch
        ?: palette.mutedSwatch
    return swatch?.rgb?.let { Color(it) }
}
