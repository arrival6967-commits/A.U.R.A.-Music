package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks")
    suspend fun getAllTracksSnapshot(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE contentUri = :uri LIMIT 1")
    suspend fun getTrackByUri(uri: String): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracksIgnore(tracks: List<TrackEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun setTrackFavorite(trackId: Long, isFavorite: Boolean)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrackById(trackId: Long)

    @Query("UPDATE tracks SET playCount = playCount + 1 WHERE id = :trackId")
    suspend fun incrementPlayCount(trackId: Long)

    // Playlists
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylistsWithTracks(): Flow<List<PlaylistWithTracks>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
    suspend fun deleteAllCrossRefsForPlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    // Equalizer Presets
    @Query("SELECT * FROM equalizer_presets ORDER BY isCustom ASC, id ASC")
    fun observeEqualizerPresets(): Flow<List<EqualizerPresetEntity>>

    @Query("SELECT COUNT(*) FROM equalizer_presets")
    suspend fun getPresetCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEqualizerPresets(presets: List<EqualizerPresetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEqualizerPreset(preset: EqualizerPresetEntity): Long

    @Query("DELETE FROM equalizer_presets WHERE id = :presetId AND isCustom = 1")
    suspend fun deleteCustomPreset(presetId: Long)
}
