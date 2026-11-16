package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.PlaybackState
import com.example.data.local.PlaylistWithTracks
import com.example.data.local.TrackEntity
import com.example.ui.components.TrackCoverArtwork
import com.example.ui.components.formatDurationMs
import com.example.ui.viewmodel.MusicUiState

@Composable
fun LibraryScreen(
    uiState: MusicUiState,
    playbackState: PlaybackState,
    onSearchQueryChange: (String) -> Unit,
    onSelectGenre: (String) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onPlayTrack: (TrackEntity) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onAddTrackToPlaylist: (Long, TrackEntity, String) -> Unit,
    onImportAudioClick: () -> Unit,
    onScanDeviceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var trackForPlaylistDialog by remember { mutableStateOf<TrackEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Local Device Music Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LibraryMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "МУЗЫКА НА УСТРОЙСТВЕ",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (uiState.allTracks.isEmpty()) {
                                "Добавьте аудиофайлы из памяти устройства или запустите сканирование"
                            } else {
                                "Треков в медиатеке устройства: ${uiState.allTracks.size}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = onScanDeviceClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("scan_device_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Сканировать", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            FilledTonalButton(
                                onClick = onImportAudioClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("import_audio_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Добавить файлы", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        if (uiState.allTracks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val list = uiState.filteredTracks.ifEmpty { uiState.allTracks }
                                    onPlayTrack(list.first())
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("play_all_library_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Слушать")
                            }
                        }
                    }
                }
            }
        }

        // Search input
        item {
            OutlinedTextField(
                value = uiState.filter.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input"),
                placeholder = { Text("Поиск треков на устройстве…") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                },
                trailingIcon = {
                    if (uiState.filter.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить поиск")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Filter Chips: Favorites + Genres (when tracks exist)
        if (uiState.allTracks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.filter.onlyFavorites,
                        onClick = onToggleFavoritesOnly,
                        label = { Text("Избранное") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.filter.onlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("filter_favorites_chip")
                    )

                    if (uiState.availableGenres.size > 1) {
                        uiState.availableGenres.forEach { genre ->
                            FilterChip(
                                selected = uiState.filter.selectedGenre == genre,
                                onClick = { onSelectGenre(genre) },
                                label = { Text(genre) },
                                modifier = Modifier.testTag("genre_chip_$genre")
                            )
                        }
                    }
                }
            }
        }

        if (uiState.filteredTracks.isEmpty()) {
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
                            imageVector = Icons.Default.AudioFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (uiState.allTracks.isEmpty()) {
                                "В медиатеке пока нет треков"
                            } else {
                                "По вашему запросу ничего не найдено"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.allTracks.isEmpty()) {
                                "Нажмите «Сканировать» для поиска музыки в памяти телефона или «Добавить файлы» для выбора аудиофайлов."
                            } else {
                                "Попробуйте изменить поисковый запрос или сбросить фильтр."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(uiState.filteredTracks, key = { it.id }) { track ->
                val isCurrentTrack = playbackState.currentTrack?.id == track.id
                TrackListItemCard(
                    track = track,
                    isCurrentTrack = isCurrentTrack,
                    onClick = { onPlayTrack(track) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onAddToPlaylistClick = { trackForPlaylistDialog = track }
                )
            }
        }
    }

    trackForPlaylistDialog?.let { selectedTrack ->
        AddToPlaylistDialog(
            track = selectedTrack,
            playlists = uiState.playlists,
            onDismiss = { trackForPlaylistDialog = null },
            onSelectPlaylist = { playlist ->
                onAddTrackToPlaylist(playlist.playlist.playlistId, selectedTrack, playlist.playlist.name)
                trackForPlaylistDialog = null
            }
        )
    }
}

@Composable
fun TrackListItemCard(
    track: TrackEntity,
    isCurrentTrack: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isCurrentTrack) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("track_card_${track.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackCoverArtwork(
                track = track,
                size = 54.dp,
                cornerRadius = 14.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrentTrack) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Играет сейчас",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isCurrentTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatDurationMs(track.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (track.genre.isNotBlank()) {
                        Text(
                            text = track.genre,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("track_favorite_toggle_${track.id}")
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Избранное",
                    tint = if (track.isFavorite) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            IconButton(
                onClick = onAddToPlaylistClick,
                modifier = Modifier.testTag("track_add_playlist_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Добавить в плейлист",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddToPlaylistDialog(
    track: TrackEntity,
    playlists: List<PlaylistWithTracks>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (PlaylistWithTracks) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить в плейлист") },
        text = {
            if (playlists.isEmpty()) {
                Text(
                    text = "Сначала создайте плейлист во вкладке «Плейлисты».",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Выберите плейлист для трека «${track.title}»:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    playlists.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPlaylist(item) }
                                .testTag("dialog_playlist_option_${item.playlist.playlistId}"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TrackCoverArtwork(
                                    track = item.tracks.firstOrNull(),
                                    paletteIndex = item.playlist.coverPaletteIndex,
                                    size = 38.dp,
                                    cornerRadius = 10.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.playlist.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${item.tracks.size} треков",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}
