package com.example.player

data class EqualizerBand(
    val index: Short,
    val centerFreqHz: Int,
    val minLevelMilliBels: Short = -1500,
    val maxLevelMilliBels: Short = 1500,
    val currentLevelMilliBels: Short = 0
) {
    val formattedFreq: String
        get() = if (centerFreqHz >= 1000) {
            "${centerFreqHz / 1000} kHz"
        } else {
            "$centerFreqHz Hz"
        }
}
