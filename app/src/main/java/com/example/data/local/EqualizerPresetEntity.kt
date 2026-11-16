package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equalizer_presets")
data class EqualizerPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCustom: Boolean = false,
    // Band gains in decibels (-15.0f to +15.0f)
    val band60Hz: Float = 0f,
    val band230Hz: Float = 0f,
    val band910Hz: Float = 0f,
    val band3600Hz: Float = 0f,
    val band14000Hz: Float = 0f,
    // AudioFX levels (0 to 1000)
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainDb: Float = 0f
) {
    fun bandGainsList(): List<Float> = listOf(band60Hz, band230Hz, band910Hz, band3600Hz, band14000Hz)
}
