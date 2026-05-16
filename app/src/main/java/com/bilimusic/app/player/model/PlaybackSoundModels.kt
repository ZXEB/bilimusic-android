package com.bilimusic.app.player.model

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

const val DEFAULT_PLAYBACK_SPEED = 1.0f
const val DEFAULT_PLAYBACK_PITCH = 1.0f
const val DEFAULT_PLAYBACK_LOUDNESS_GAIN_MB = 0
const val MIN_PLAYBACK_SPEED = 0.1f
const val MAX_PLAYBACK_SPEED = 4.0f
const val MIN_PLAYBACK_PITCH = 0.25f
const val MAX_PLAYBACK_PITCH = 2.0f
const val MIN_PLAYBACK_LOUDNESS_GAIN_MB = 0
const val MAX_PLAYBACK_LOUDNESS_GAIN_MB = 1_500
val DEFAULT_EQUALIZER_BAND_LEVEL_RANGE_MB = -1500..1500

object PlaybackEqualizerPresetId {
    const val CUSTOM = "custom"
    const val FLAT = "flat"
}

private val PRESET_ANCHOR_FREQUENCIES_HZ = listOf(60, 230, 910, 3600, 14_000)
private val DEFAULT_EQUALIZER_CENTER_FREQUENCIES_HZ = PRESET_ANCHOR_FREQUENCIES_HZ

data class PlaybackSoundConfig(
    val speed: Float = DEFAULT_PLAYBACK_SPEED,
    val pitch: Float = DEFAULT_PLAYBACK_PITCH,
    val loudnessGainMb: Int = DEFAULT_PLAYBACK_LOUDNESS_GAIN_MB,
    val equalizerEnabled: Boolean = false,
    val presetId: String = PlaybackEqualizerPresetId.FLAT,
    val customBandLevelsMb: List<Int> = emptyList()
)

data class PlaybackEqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val levelMb: Int
) {
    val levelDb: Float
        get() = levelMb / 100f
}

data class PlaybackSoundState(
    val speed: Float = DEFAULT_PLAYBACK_SPEED,
    val pitch: Float = DEFAULT_PLAYBACK_PITCH,
    val loudnessGainMb: Int = DEFAULT_PLAYBACK_LOUDNESS_GAIN_MB,
    val equalizerEnabled: Boolean = false,
    val presetId: String = PlaybackEqualizerPresetId.FLAT,
    val bands: List<PlaybackEqualizerBand> = defaultPlaybackEqualizerBands(),
    val bandLevelRangeMb: IntRange = DEFAULT_EQUALIZER_BAND_LEVEL_RANGE_MB,
    val audioSessionId: Int? = null,
    val equalizerAvailable: Boolean = false,
    val loudnessEnhancerAvailable: Boolean = false
)

data class PlaybackEqualizerPreset(
    val id: String,
    val label: String,
    private val anchorLevelsDb: List<Float>
) {
    fun resolveBandLevelsMb(
        bandCentersHz: List<Int>,
        bandLevelRangeMb: IntRange
    ): List<Int> {
        if (bandCentersHz.isEmpty()) return emptyList()
        return bandCentersHz.map { centerFreqHz ->
            val interpolatedDb = interpolatePresetLevelDb(centerFreqHz)
            (interpolatedDb * 100f).roundToInt().coerceIn(
                minimumValue = bandLevelRangeMb.first,
                maximumValue = bandLevelRangeMb.last
            )
        }
    }

    private fun interpolatePresetLevelDb(centerFreqHz: Int): Float {
        if (anchorLevelsDb.size != PRESET_ANCHOR_FREQUENCIES_HZ.size) return 0f
        if (centerFreqHz <= PRESET_ANCHOR_FREQUENCIES_HZ.first()) {
            return anchorLevelsDb.first()
        }
        if (centerFreqHz >= PRESET_ANCHOR_FREQUENCIES_HZ.last()) {
            return anchorLevelsDb.last()
        }
        val target = centerFreqHz.toFloat()
        for (index in 0 until PRESET_ANCHOR_FREQUENCIES_HZ.lastIndex) {
            val leftFreq = PRESET_ANCHOR_FREQUENCIES_HZ[index].toFloat()
            val rightFreq = PRESET_ANCHOR_FREQUENCIES_HZ[index + 1].toFloat()
            if (target in leftFreq..rightFreq) {
                val leftDb = anchorLevelsDb[index]
                val rightDb = anchorLevelsDb[index + 1]
                val progress = ((ln(target) - ln(leftFreq)) / (ln(rightFreq) - ln(leftFreq)))
                    .coerceIn(0f, 1f)
                return leftDb + (rightDb - leftDb) * progress
            }
        }
        return 0f
    }
}

val PlaybackEqualizerPresets = listOf(
    PlaybackEqualizerPreset(PlaybackEqualizerPresetId.FLAT, "Flat", listOf(0f, 0f, 0f, 0f, 0f)),
    PlaybackEqualizerPreset("acoustic", "Acoustic", listOf(3f, 2f, 1f, 2f, 3f)),
    PlaybackEqualizerPreset("bass_boost", "Bass Booster", listOf(7f, 5f, 2f, -1f, -3f)),
    PlaybackEqualizerPreset("bass_reducer", "Bass Reducer", listOf(-7f, -5f, -2f, 0f, 1f)),
    PlaybackEqualizerPreset("classical", "Classical", listOf(4f, 2f, -1f, 3f, 5f)),
    PlaybackEqualizerPreset("club", "Club", listOf(5f, 3f, 0f, 3f, 4f)),
    PlaybackEqualizerPreset("dance", "Dance", listOf(6f, 4f, 1f, 4f, 5f)),
    PlaybackEqualizerPreset("deep", "Deep", listOf(8f, 5f, 1f, 0f, 2f)),
    PlaybackEqualizerPreset("electronic", "Electronic", listOf(5f, 2f, -1f, 4f, 6f)),
    PlaybackEqualizerPreset("folk", "Folk", listOf(1f, 3f, 0f, 4f, 5f)),
    PlaybackEqualizerPreset("hip_hop", "Hip Hop", listOf(7f, 4f, 1f, 2f, 4f)),
    PlaybackEqualizerPreset("jazz", "Jazz", listOf(3f, 2f, 1f, 3f, 4f)),
    PlaybackEqualizerPreset("latin", "Latin", listOf(4f, 3f, 1f, 4f, 5f)),
    PlaybackEqualizerPreset("lounge", "Lounge", listOf(2f, 1f, 1f, 3f, 4f)),
    PlaybackEqualizerPreset("piano", "Piano", listOf(2f, 1f, 0f, 3f, 4f)),
    PlaybackEqualizerPreset("pop", "Pop", listOf(-1f, 3f, 5f, 3f, 0f)),
    PlaybackEqualizerPreset("rnb", "R&B", listOf(4f, 3f, 2f, 4f, 3f)),
    PlaybackEqualizerPreset("rock", "Rock", listOf(6f, 3f, -1f, 3f, 6f)),
    PlaybackEqualizerPreset("small_speakers", "Small Speakers", listOf(4f, 1f, -2f, 4f, 7f)),
    PlaybackEqualizerPreset("spoken_word", "Spoken Word", listOf(-4f, -2f, 4f, 5f, 2f)),
    PlaybackEqualizerPreset("treble_boost", "Treble Booster", listOf(-3f, -1f, 2f, 5f, 8f)),
    PlaybackEqualizerPreset("treble_reducer", "Treble Reducer", listOf(1f, 0f, -2f, -5f, -8f)),
    PlaybackEqualizerPreset("vocal_boost", "Vocal Booster", listOf(-2f, 1f, 5f, 5f, 1f))
)

fun defaultPlaybackEqualizerBands(): List<PlaybackEqualizerBand> {
    return DEFAULT_EQUALIZER_CENTER_FREQUENCIES_HZ.mapIndexed { index, centerFreqHz ->
        PlaybackEqualizerBand(
            index = index,
            centerFreqHz = centerFreqHz,
            levelMb = 0
        )
    }
}

fun normalizePlaybackSpeed(value: Float): Float {
    return ((value * 20f).roundToInt() / 20f)
        .coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED)
}

fun normalizePlaybackPitch(value: Float): Float {
    return ((value * 20f).roundToInt() / 20f)
        .coerceIn(MIN_PLAYBACK_PITCH, MAX_PLAYBACK_PITCH)
}

fun pitchToSemitoneOffset(pitch: Float): Float {
    val normalizedPitch = normalizePlaybackPitch(pitch).toDouble()
    return ((ln(normalizedPitch) / ln(2.0)) * 12.0).toFloat()
}

fun semitoneOffsetToPitch(semitoneOffset: Float): Float {
    val pitch = 2.0.pow(semitoneOffset / 12.0).toFloat()
    return normalizePlaybackPitch(pitch)
}

fun normalizePlaybackLoudnessGainMb(value: Int): Int {
    return value.coerceIn(
        minimumValue = MIN_PLAYBACK_LOUDNESS_GAIN_MB,
        maximumValue = MAX_PLAYBACK_LOUDNESS_GAIN_MB
    )
}

fun findPlaybackEqualizerPreset(id: String): PlaybackEqualizerPreset? {
    return PlaybackEqualizerPresets.firstOrNull { it.id == id }
}

fun resolvePlaybackEqualizerBandLevelsMb(
    presetId: String,
    customBandLevelsMb: List<Int>,
    bandCentersHz: List<Int>,
    bandLevelRangeMb: IntRange
): List<Int> {
    if (bandCentersHz.isEmpty()) return emptyList()
    if (presetId == PlaybackEqualizerPresetId.CUSTOM) {
        return bandCentersHz.indices.map { index ->
            customBandLevelsMb.getOrNull(index)?.coerceIn(
                minimumValue = bandLevelRangeMb.first,
                maximumValue = bandLevelRangeMb.last
            ) ?: 0
        }
    }
    val preset = findPlaybackEqualizerPreset(presetId)
        ?: findPlaybackEqualizerPreset(PlaybackEqualizerPresetId.FLAT)
        ?: return List(bandCentersHz.size) { 0 }
    return preset.resolveBandLevelsMb(bandCentersHz, bandLevelRangeMb)
}

fun formatEqualizerFrequencyLabel(centerFreqHz: Int): String {
    if (centerFreqHz < 1000) return "${centerFreqHz}Hz"
    val khz = centerFreqHz / 1000f
    val rounded = (khz * 10).roundToInt() / 10f
    return if (rounded % 1f == 0f) {
        "${rounded.toInt()}kHz"
    } else {
        "${rounded}kHz"
    }
}

fun encodePlaybackEqualizerBandLevels(levelsMb: List<Int>): String? {
    if (levelsMb.isEmpty()) return null
    return levelsMb.joinToString(separator = ",")
}

fun decodePlaybackEqualizerBandLevels(raw: String?): List<Int> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split(',')
        .mapNotNull { it.trim().toIntOrNull() }
}

fun formatPlaybackRatioLabel(value: Float): String {
    return "${(value * 100).roundToInt() / 100f}x"
}

fun formatPlaybackGainLabel(levelMb: Int): String {
    val db = normalizePlaybackLoudnessGainMb(levelMb) / 100f
    return "+${(db * 10).roundToInt() / 10f} dB"
}
