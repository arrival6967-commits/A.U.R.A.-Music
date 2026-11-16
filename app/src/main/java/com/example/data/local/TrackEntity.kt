package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    indices = [Index(value = ["contentUri"], unique = true)]
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationMs: Long,
    val bitrateKbps: Int = 320,
    val sampleRateHz: Int = 44100,
    val isFavorite: Boolean = false,
    val fileSizeBytes: Long = 0L,
    val contentUri: String,
    val albumId: Long? = null,
    val coverPaletteIndex: Int = 0,
    val dominantColorArgb: Int? = null,
    val secondaryColorArgb: Int? = null,
    val playCount: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
