package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.audio.AlbumArtLoader
import com.example.data.local.EqualizerPresetEntity
import com.example.data.local.MusicDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.PlaylistWithTracks
import com.example.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val musicDao: MusicDao
) {
    val allTracks: Flow<List<TrackEntity>> = musicDao.observeAllTracks()
    val favoriteTracks: Flow<List<TrackEntity>> = musicDao.observeFavoriteTracks()
    val playlistsWithTracks: Flow<List<PlaylistWithTracks>> = musicDao.observePlaylistsWithTracks()
    val equalizerPresets: Flow<List<EqualizerPresetEntity>> = musicDao.observeEqualizerPresets()

    suspend fun ensureEqualizerPresetsSeeded() = withContext(Dispatchers.IO) {
        if (musicDao.getPresetCount() == 0) {
            val defaultPresets = listOf(
                EqualizerPresetEntity(
                    id = 1,
                    name = "Студийный Hi-Fi",
                    isCustom = false,
                    band60Hz = 2.5f,
                    band230Hz = 1.0f,
                    band910Hz = 0.0f,
                    band3600Hz = 1.8f,
                    band14000Hz = 3.0f,
                    bassBoostStrength = 350,
                    virtualizerStrength = 250,
                    loudnessGainDb = 1.5f
                ),
                EqualizerPresetEntity(
                    id = 2,
                    name = "Глубокий Бас",
                    isCustom = false,
                    band60Hz = 8.5f,
                    band230Hz = 5.5f,
                    band910Hz = -1.0f,
                    band3600Hz = 1.5f,
                    band14000Hz = 2.5f,
                    bassBoostStrength = 820,
                    virtualizerStrength = 300,
                    loudnessGainDb = 2.5f
                ),
                EqualizerPresetEntity(
                    id = 3,
                    name = "Вокал и Акустика",
                    isCustom = false,
                    band60Hz = -1.5f,
                    band230Hz = 1.5f,
                    band910Hz = 5.5f,
                    band3600Hz = 4.5f,
                    band14000Hz = 2.0f,
                    bassBoostStrength = 120,
                    virtualizerStrength = 400,
                    loudnessGainDb = 1.0f
                ),
                EqualizerPresetEntity(
                    id = 4,
                    name = "Электроника",
                    isCustom = false,
                    band60Hz = 6.5f,
                    band230Hz = 2.5f,
                    band910Hz = -2.0f,
                    band3600Hz = 3.8f,
                    band14000Hz = 6.0f,
                    bassBoostStrength = 680,
                    virtualizerStrength = 650,
                    loudnessGainDb = 2.0f
                ),
                EqualizerPresetEntity(
                    id = 5,
                    name = "Рок и Драйв",
                    isCustom = false,
                    band60Hz = 5.0f,
                    band230Hz = 3.0f,
                    band910Hz = -1.5f,
                    band3600Hz = 4.5f,
                    band14000Hz = 5.5f,
                    bassBoostStrength = 500,
                    virtualizerStrength = 350,
                    loudnessGainDb = 2.0f
                ),
                EqualizerPresetEntity(
                    id = 6,
                    name = "Ночной Лаунж",
                    isCustom = false,
                    band60Hz = 3.5f,
                    band230Hz = 2.0f,
                    band910Hz = -0.5f,
                    band3600Hz = -2.0f,
                    band14000Hz = -3.0f,
                    bassBoostStrength = 400,
                    virtualizerStrength = 750,
                    loudnessGainDb = 0.5f
                )
            )
            musicDao.insertEqualizerPresets(defaultPresets)
        }
    }

    /**
     * Scans local device storage via Android MediaStore for real audio files on the device.
     */
    suspend fun scanDeviceMusic(): Int = withContext(Dispatchers.IO) {
        val discoveredTracks = mutableListOf<TrackEntity>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 1000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Аудиозапись $mediaId"
                    val rawArtist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Неизвестный исполнитель"
                    val rawAlbum = cursor.getString(albumCol)?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Локальный альбом"
                    val albumId = cursor.getLong(albumIdCol)
                    val durationMs = cursor.getLong(durationCol).coerceAtLeast(1000L)
                    val sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaId
                    ).toString()

                    val paletteIndex = (rawTitle.hashCode() and 0x7FFFFFFF) % 8
                    val (dom, sec) = AlbumArtLoader.getFallbackColors(paletteIndex)

                    discoveredTracks.add(
                        TrackEntity(
                            title = rawTitle,
                            artist = rawArtist,
                            album = rawAlbum,
                            genre = "На устройстве",
                            durationMs = durationMs,
                            fileSizeBytes = sizeBytes,
                            contentUri = contentUri,
                            albumId = albumId,
                            coverPaletteIndex = paletteIndex,
                            dominantColorArgb = dom,
                            secondaryColorArgb = sec
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }

        if (discoveredTracks.isNotEmpty()) {
            musicDao.insertTracksIgnore(discoveredTracks)
        }
        discoveredTracks.size
    }

    suspend fun toggleFavorite(track: TrackEntity) {
        musicDao.setTrackFavorite(track.id, !track.isFavorite)
    }

    suspend fun deleteTrackFromLibrary(trackId: Long) {
        musicDao.deleteTrackById(trackId)
    }

    suspend fun incrementPlayCount(trackId: Long) {
        musicDao.incrementPlayCount(trackId)
    }

    suspend fun createPlaylist(name: String, description: String, paletteIndex: Int): Long {
        return musicDao.insertPlaylist(
            PlaylistEntity(
                name = name.trim(),
                description = description.trim().ifEmpty { "Пользовательский плейлист" },
                coverPaletteIndex = paletteIndex
            )
        )
    }

    suspend fun deletePlaylist(playlistId: Long) {
        musicDao.deleteAllCrossRefsForPlaylist(playlistId)
        musicDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        musicDao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId = playlistId, trackId = trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        musicDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun saveCustomEqualizerPreset(
        name: String,
        bandGains: List<Float>,
        bassBoost: Int,
        virtualizer: Int,
        loudnessDb: Float
    ): EqualizerPresetEntity {
        val preset = EqualizerPresetEntity(
            name = name.trim(),
            isCustom = true,
            band60Hz = bandGains.getOrElse(0) { 0f },
            band230Hz = bandGains.getOrElse(1) { 0f },
            band910Hz = bandGains.getOrElse(2) { 0f },
            band3600Hz = bandGains.getOrElse(3) { 0f },
            band14000Hz = bandGains.getOrElse(4) { 0f },
            bassBoostStrength = bassBoost,
            virtualizerStrength = virtualizer,
            loudnessGainDb = loudnessDb
        )
        val id = musicDao.insertEqualizerPreset(preset)
        return preset.copy(id = id)
    }

    suspend fun deleteCustomPreset(presetId: Long) {
        musicDao.deleteCustomPreset(presetId)
    }

    suspend fun importAudioUri(uri: Uri): TrackEntity? = withContext(Dispatchers.IO) {
        try {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            val existing = musicDao.getTrackByUri(uri.toString())
            if (existing != null) return@withContext existing

            var displayName = "Аудиозапись"
            var fileSize = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) {
                        displayName = cursor.getString(nameIndex)?.substringBeforeLast('.') ?: displayName
                    }
                    if (sizeIndex >= 0) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            var title = displayName
            var artist = "На устройстве"
            var album = "Локальная медиатека"
            var genre = "Аудиофайл"
            var durationMs = 180000L

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }?.let {
                    title = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }?.let {
                    artist = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }?.let {
                    album = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.takeIf { it.isNotBlank() }?.let {
                    genre = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                    if (it > 0) durationMs = it
                }
            } catch (_: Exception) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }

            val paletteIdx = (title.hashCode() and 0x7FFFFFFF) % 8
            val tempTrack = TrackEntity(
                title = title,
                artist = artist,
                album = album,
                genre = genre,
                durationMs = durationMs,
                bitrateKbps = 320,
                sampleRateHz = 44100,
                isFavorite = false,
                fileSizeBytes = fileSize,
                contentUri = uri.toString(),
                coverPaletteIndex = paletteIdx
            )

            val coverResult = AlbumArtLoader.loadCoverArt(context, tempTrack, targetSizePx = 256)
            val finalTrack = tempTrack.copy(
                dominantColorArgb = coverResult.dominantColorArgb,
                secondaryColorArgb = coverResult.secondaryColorArgb
            )

            val id = musicDao.insertTrack(finalTrack)
            finalTrack.copy(id = id)
        } catch (_: Exception) {
            null
        }
    }
}
