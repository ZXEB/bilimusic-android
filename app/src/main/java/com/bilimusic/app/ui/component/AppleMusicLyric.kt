package com.bilimusic.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bilimusic.app.player.LyricEntry

@Composable
fun AppleMusicLyric(
    lyrics: List<LyricEntry>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    textColor: Color = if (isSystemInDarkTheme()) Color.White else Color.Black,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    onLyricClick: ((LyricEntry) -> Unit)? = null,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex, lyrics.size) {
        if (currentIndex in lyrics.indices && !listState.isScrollInProgress) {
            listState.animateScrollToItem(currentIndex, scrollOffset = -180)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val verticalPadding = maxHeight * 0.42f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.18f to Color.Black,
                            0.76f to Color.Black,
                            1f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = verticalPadding)
            ) {
                itemsIndexed(lyrics, key = { _, line -> "${line.startTimeMs}:${line.endTimeMs}:${line.text}" }) { index, line ->
                    LyricLine(
                        text = line.text,
                        active = index == currentIndex,
                        distance = lyricDistance(index, currentIndex),
                        textColor = textColor,
                        fontSize = fontSize,
                        onClick = onLyricClick?.let { { it(line) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricLine(
    text: String,
    active: Boolean,
    distance: Int,
    textColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onClick: (() -> Unit)?
) {
    val scale = when {
        active -> 1f
        distance <= 1 -> 0.94f
        else -> 0.88f
    }
    val alpha = when {
        active -> 1f
        distance <= 1 -> 0.6f
        distance <= 3 -> 0.34f
        else -> 0.18f
    }

    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = if (active) 12.dp else 9.dp, horizontal = 24.dp),
        style = TextStyle(
            color = textColor.copy(alpha = alpha),
            fontSize = if (active) fontSize else 17.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            lineHeight = if (active) 30.sp else 24.sp
        ),
        maxLines = Int.MAX_VALUE,
        softWrap = true
    )
}

private fun lyricDistance(index: Int, currentIndex: Int): Int =
    if (currentIndex < 0) Int.MAX_VALUE else kotlin.math.abs(index - currentIndex)
