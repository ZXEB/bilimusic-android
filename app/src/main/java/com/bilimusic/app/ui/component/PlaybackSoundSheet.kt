package com.bilimusic.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bilimusic.app.player.model.MAX_PLAYBACK_LOUDNESS_GAIN_MB
import com.bilimusic.app.player.model.MAX_PLAYBACK_PITCH
import com.bilimusic.app.player.model.MAX_PLAYBACK_SPEED
import com.bilimusic.app.player.model.MIN_PLAYBACK_LOUDNESS_GAIN_MB
import com.bilimusic.app.player.model.MIN_PLAYBACK_PITCH
import com.bilimusic.app.player.model.MIN_PLAYBACK_SPEED
import com.bilimusic.app.player.model.PlaybackEqualizerPresetId
import com.bilimusic.app.player.model.PlaybackEqualizerPresets
import com.bilimusic.app.player.model.PlaybackSoundState
import com.bilimusic.app.player.model.formatEqualizerFrequencyLabel
import com.bilimusic.app.player.model.formatPlaybackGainLabel
import com.bilimusic.app.player.model.normalizePlaybackLoudnessGainMb
import com.bilimusic.app.player.model.normalizePlaybackPitch
import com.bilimusic.app.player.model.normalizePlaybackSpeed
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val SPEED_QUICK_PRESETS = listOf(0.1f, 0.5f, 0.75f, 0.85f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
private val PITCH_QUICK_PRESETS = listOf(0.5f, 0.75f, 0.85f, 1.0f, 1.25f, 1.5f)
private val LOUDNESS_QUICK_PRESETS = listOf(0, 300, 600, 900, 1_200, 1_500)
private const val SPEED_SLIDER_STEPS = 77
private const val PITCH_SLIDER_STEPS = 34
private const val LOUDNESS_SLIDER_STEP_MB = 50
private const val EQUALIZER_SLIDER_STEP_MB = 50

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaybackSoundSheet(
    state: PlaybackSoundState,
    onSpeedChange: (Float, Boolean) -> Unit,
    onPitchChange: (Float, Boolean) -> Unit,
    onLoudnessGainChange: (Int, Boolean) -> Unit,
    onEqualizerEnabledChange: (Boolean) -> Unit,
    onPresetSelected: (String) -> Unit,
    onBandLevelChange: (Int, Int, Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    val presets = buildList {
        if (state.presetId == PlaybackEqualizerPresetId.CUSTOM) {
            add(PlaybackChipData(PlaybackEqualizerPresetId.CUSTOM, "Custom"))
        }
        addAll(PlaybackEqualizerPresets.map { PlaybackChipData(it.id, it.label) })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "音效与倍速",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "调整播放速度、音调、均衡器等音效设置",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PlaybackControlCard(
            title = "播放速度",
            valueLabel = formatMultiplier(state.speed),
            quickPresets = SPEED_QUICK_PRESETS,
            currentValue = state.speed,
            range = MIN_PLAYBACK_SPEED..MAX_PLAYBACK_SPEED,
            steps = SPEED_SLIDER_STEPS,
            normalize = ::normalizePlaybackSpeed,
            onValueChange = onSpeedChange
        )

        PlaybackControlCard(
            title = "音调",
            valueLabel = formatMultiplier(state.pitch),
            quickPresets = PITCH_QUICK_PRESETS,
            currentValue = state.pitch,
            range = MIN_PLAYBACK_PITCH..MAX_PLAYBACK_PITCH,
            steps = PITCH_SLIDER_STEPS,
            normalize = ::normalizePlaybackPitch,
            onValueChange = onPitchChange
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "响度增强",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = if (state.audioSessionId == null) {
                                "等待音频会话..."
                            } else if (!state.loudnessEnhancerAvailable) {
                                "设备不支持响度增强"
                            } else {
                                "增强音频的整体响度"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatPlaybackGainLabel(state.loudnessGainMb),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace)
                    )
                }
                var loudnessSliderValue by remember(state.loudnessGainMb) {
                    mutableIntStateOf(state.loudnessGainMb)
                }
                Slider(
                    value = loudnessSliderValue.toFloat(),
                    onValueChange = { raw ->
                        val normalized = ((raw / LOUDNESS_SLIDER_STEP_MB).roundToInt() * LOUDNESS_SLIDER_STEP_MB)
                            .coerceIn(
                                minimumValue = MIN_PLAYBACK_LOUDNESS_GAIN_MB,
                                maximumValue = MAX_PLAYBACK_LOUDNESS_GAIN_MB
                            )
                        loudnessSliderValue = normalizePlaybackLoudnessGainMb(normalized)
                        onLoudnessGainChange(loudnessSliderValue, false)
                    },
                    onValueChangeFinished = {
                        onLoudnessGainChange(loudnessSliderValue, true)
                    },
                    valueRange = MIN_PLAYBACK_LOUDNESS_GAIN_MB.toFloat()..MAX_PLAYBACK_LOUDNESS_GAIN_MB.toFloat(),
                    steps = buildDiscreteSliderSteps(
                        range = MIN_PLAYBACK_LOUDNESS_GAIN_MB..MAX_PLAYBACK_LOUDNESS_GAIN_MB,
                        stepSize = LOUDNESS_SLIDER_STEP_MB
                    )
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LOUDNESS_QUICK_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = loudnessSliderValue == preset,
                            onClick = {
                                loudnessSliderValue = preset
                                onLoudnessGainChange(loudnessSliderValue, true)
                            },
                            label = { Text(formatPlaybackGainLabel(preset)) }
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "均衡器",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "均衡器预设",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.equalizerEnabled,
                        onCheckedChange = onEqualizerEnabledChange
                    )
                }

                val infoText = when {
                    state.audioSessionId == null ->
                        "等待音频会话..."
                    state.equalizerEnabled && !state.equalizerAvailable ->
                        "设备不支持均衡器"
                    else ->
                        "选择预设或手动调节各频段增益"
                }
                Text(
                    text = infoText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = state.presetId == preset.id,
                            onClick = { onPresetSelected(preset.id) },
                            label = { Text(preset.label) }
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = "手动调节各频段",
                    style = MaterialTheme.typography.titleSmall
                )

                state.bands.forEach { band ->
                    var bandSliderValue by remember(band.index, band.levelMb) {
                        mutableIntStateOf(band.levelMb)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatEqualizerFrequencyLabel(band.centerFreqHz),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = formatBandLevelDb(band.levelMb),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                        Slider(
                            value = bandSliderValue.toFloat(),
                            onValueChange = { raw ->
                                val normalized = ((raw / EQUALIZER_SLIDER_STEP_MB).roundToInt() * EQUALIZER_SLIDER_STEP_MB)
                                    .coerceIn(
                                        minimumValue = state.bandLevelRangeMb.first,
                                        maximumValue = state.bandLevelRangeMb.last
                                    )
                                bandSliderValue = normalized
                                onBandLevelChange(band.index, bandSliderValue, false)
                            },
                            onValueChangeFinished = {
                                onBandLevelChange(band.index, bandSliderValue, true)
                            },
                            valueRange = state.bandLevelRangeMb.first.toFloat()..state.bandLevelRangeMb.last.toFloat(),
                            steps = buildDiscreteSliderSteps(
                                range = state.bandLevelRangeMb,
                                stepSize = EQUALIZER_SLIDER_STEP_MB
                            )
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("恢复默认")
        }

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("完成")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaybackControlCard(
    title: String,
    valueLabel: String,
    quickPresets: List<Float>,
    currentValue: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    normalize: (Float) -> Float,
    onValueChange: (Float, Boolean) -> Unit
) {
    var sliderValue by remember(currentValue) {
        mutableFloatStateOf(currentValue)
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace)
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = normalize(it)
                    onValueChange(sliderValue, false)
                },
                onValueChangeFinished = {
                    onValueChange(sliderValue, true)
                },
                valueRange = range,
                steps = steps
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPresets.forEach { preset ->
                    val normalizedPreset = normalize(preset)
                    FilterChip(
                        selected = abs(sliderValue - normalizedPreset) < 0.001f,
                        onClick = {
                            sliderValue = normalizedPreset
                            onValueChange(sliderValue, true)
                        },
                        label = { Text(formatMultiplier(preset)) }
                    )
                }
            }
        }
    }
}

private fun buildDiscreteSliderSteps(range: IntRange, stepSize: Int): Int {
    val rawSteps = ((range.last - range.first) / stepSize) - 1
    return rawSteps.coerceAtLeast(0)
}

private fun formatMultiplier(value: Float): String {
    return String.format(Locale.US, "%.2fx", value)
}

private fun formatBandLevelDb(levelMb: Int): String {
    return String.format(Locale.US, "%+.1f dB", levelMb / 100f)
}

private data class PlaybackChipData(
    val id: String,
    val label: String
)
