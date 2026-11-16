package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PlaylistWithTracks
import com.example.data.local.TrackEntity
import com.example.ui.components.TrackCoverArtwork
import com.example.ui.components.formatDurationMs

@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistWithTracks>,
    allTracks: List<TrackEntity>,
    selectedPlaylistId: Long?,
    onSelectPlaylist: (Long?) -> Unit,
    onCreatePlaylist: (String, String, Int) -> Unit,
    onDeletePlaylist: (Long, String) -> Unit,
    onPlayTrackInPlaylist: (TrackEntity, List<TrackEntity>) -> Unit,
    onAddTrackToPlaylist: (Long, TrackEntity, String) -> Unit,
    onRemoveTrackFromPlaylist: (Long, Long) -> Unit,
    onBackToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val selectedPlaylist = playlists.find { it.playlist.playlistId == selectedPlaylistId }

    if (selectedPlaylist != null) {
        PlaylistDetailContent(
            playlistWithTracks = selectedPlaylist,
            allTracks = allTracks,
            onBack = { onSelectPlaylist(null) },
            onPlayTrack = { track -> onPlayTrackInPlaylist(track, selectedPlaylist.tracks) },
            onAddTrack = { track ->
                onAddTrackToPlaylist(
                    selectedPlaylist.playlist.playlistId,
                    track,
                    selectedPlaylist.playlist.name
                )
            },
            onRemoveTrack = { trackId ->
                onRemoveTrackFromPlaylist(selectedPlaylist.playlist.playlistId, trackId)
            },
            onDeletePlaylist = {
                onDeletePlaylist(selectedPlaylist.playlist.playlistId, selectedPlaylist.playlist.name)
            },
            modifier = modifier
        )
    } else {
        BackHandler(onBack = onBackToLibrary)

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("playlists_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ваши Плейлисты",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Пользовательские подборки из треков устройства",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    androidx.compose.material3.FilledIconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("create_playlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Создать плейлист",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (playlists.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Плейлистов пока нет",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Нажмите «+», чтобы собрать собственную подборку музыки с вашего устройства.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(playlists, key = { it.playlist.playlistId }) { item ->
                    val totalDuration = item.tracks.sumOf { it.durationMs }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPlaylist(item.playlist.playlistId) }
                            .testTag("playlist_card_${item.playlist.playlistId}"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackCoverArtwork(
                                track = item.tracks.firstOrNull(),
                                paletteIndex = item.playlist.coverPaletteIndex,
                                size = 64.dp,
                                cornerRadius = 16.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.playlist.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.playlist.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${item.tracks.size} треков • ${formatDurationMs(totalDuration)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (item.tracks.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        onPlayTrackInPlaylist(item.tracks.first(), item.tracks)
                                    },
                                    modifier = Modifier.testTag("play_playlist_${item.playlist.playlistId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Воспроизвести плейлист",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc, palette ->
                onCreatePlaylist(name, desc, palette)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun PlaylistDetailContent(
    playlistWithTracks: PlaylistWithTracks,
    allTracks: List<TrackEntity>,
    onBack: () -> Unit,
    onPlayTrack: (TrackEntity) -> Unit,
    onAddTrack: (TrackEntity) -> Unit,
    onRemoveTrack: (Long) -> Unit,
    onDeletePlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showAddTracksModal by remember { mutableStateOf(false) }
    val playlist = playlistWithTracks.playlist
    val tracks = playlistWithTracks.tracks

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("playlist_detail_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("playlist_detail_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад к плейлистам")
                }

                Text(
                    text = "Плейлист",
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(
                    onClick = onDeletePlaylist,
                    modifier = Modifier.testTag("delete_playlist_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Удалить плейлист",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TrackCoverArtwork(
                            track = tracks.firstOrNull(),
                            paletteIndex = playlist.coverPaletteIndex,
                            size = 80.dp,
                            cornerRadius = 18.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = playlist.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Треков: ${tracks.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Clean, symmetrical, aligned action buttons without "Скачать офлайн"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (tracks.isNotEmpty()) {
                                    onPlayTrack(tracks.first())
                                }
                            },
                            enabled = tracks.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("playlist_play_all_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Слушать")
                        }

                        FilledTonalButton(
                            onClick = { showAddTracksModal = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("playlist_add_tracks_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Добавить треки")
                        }
                    }
                }
            }
        }

        items(tracks, key = { it.id }) { track ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayTrack(track) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackCoverArtwork(
                        track = track,
                        size = 48.dp,
                        cornerRadius = 12.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${track.artist} • ${formatDurationMs(track.durationMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { onRemoveTrack(track.id) },
                        modifier = Modifier.testTag("remove_from_playlist_${track.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveCircleOutline,
                            contentDescription = "Убрать из плейлиста",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddTracksModal) {
        val existingIds = tracks.map { it.id }.toSet()
        val availableToAdd = allTracks.filter { it.id !in existingIds }

        AlertDialog(
            onDismissRequest = { showAddTracksModal = false },
            title = { Text("Добавить треки в «${playlist.name}»") },
            text = {
                if (availableToAdd.isEmpty()) {
                    Text(
                        if (allTracks.isEmpty()) {
                            "Сначала добавьте музыку с устройства во вкладке «Медиатека»."
                        } else {
                            "Все треки из медиатеки уже добавлены в этот плейлист."
                        }
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableToAdd.forEach { candidate ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddTrack(candidate)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TrackCoverArtwork(
                                        track = candidate,
                                        size = 36.dp,
                                        cornerRadius = 8.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(candidate.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            candidate.artist,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Default.Add, contentDescription = "Добавить")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddTracksModal = false }) {
                    Text("Готово")
                }
            }
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val paletteIndex by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый плейлист") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название плейлиста") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_playlist_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (необязательно)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_playlist_desc_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, description, paletteIndex)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_playlist_button")
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
