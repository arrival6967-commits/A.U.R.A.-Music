package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.EqualizerState
import com.example.audio.PlaybackState
import com.example.audio.RepeatMode
import com.example.data.local.TrackEntity

@Composable
fun MiniPlayerBar(
    playbackState: PlaybackState,
    equalizerState: EqualizerState,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    val progress = if (playbackState.durationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val baseSurface = MaterialTheme.colorScheme.surfaceContainerHigh
    val targetDominant = playbackState.coverDominantColorArgb?.let { Color(it) }
        ?: MaterialTheme.colorScheme.primary
    val tintedContainer by animateColorAsState(
        targetValue = lerp(baseSurface, targetDominant, 0.22f),
        animationSpec = tween(500),
        label = "mini_player_tint"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onExpand)
            .testTag("mini_player_bar"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = tintedContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackCoverArtwork(
                    track = track,
                    preloadedBitmap = playbackState.currentCoverBitmap,
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
                        text = "${track.artist} • EQ: ${equalizerState.activePresetName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                SpectrumAnalyzerView(
                    bars = playbackState.spectrumBars.take(6),
                    isPlaying = playbackState.isPlaying,
                    accentColor = targetDominant,
                    modifier = Modifier
                        .width(28.dp)
                        .height(22.dp)
                        .padding(end = 4.dp)
                )

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Пауза" else "Воспроизвести",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Следующий трек"
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = targetDominant,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NowPlayingFullScreen(
    playbackState: PlaybackState,
    equalizerState: EqualizerState,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onOpenEqualizer: () -> Unit
) {
    BackHandler(onBack = onClose)

    val track = playbackState.currentTrack ?: return
    val fallbackPalette = getPaletteColors(track.coverPaletteIndex)

    val rawDominant = playbackState.coverDominantColorArgb?.let { Color(it) }
        ?: track.dominantColorArgb?.let { Color(it) }
        ?: fallbackPalette.first()
    val rawSecondary = playbackState.coverSecondaryColorArgb?.let { Color(it) }
        ?: track.secondaryColorArgb?.let { Color(it) }
        ?: fallbackPalette.getOrElse(1) { MaterialTheme.colorScheme.secondary }

    val animatedDominant by animateColorAsState(
        targetValue = rawDominant,
        animationSpec = tween(durationMillis = 600),
        label = "cover_dominant_color"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = rawSecondary,
        animationSpec = tween(durationMillis = 600),
        label = "cover_secondary_color"
    )

    val baseBackground = MaterialTheme.colorScheme.background
    var showSpeedOptions by remember { mutableStateOf(false) }
    var showSleepOptions by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("now_playing_screen"),
        color = baseBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            lerp(baseBackground, animatedDominant, 0.55f),
                            lerp(baseBackground, animatedSecondary, 0.28f),
                            baseBackground
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_now_playing_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Свернуть плеер",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "СЕЙЧАС ИГРАЕТ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        )
                        Text(
                            text = track.album,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier.testTag("now_playing_open_eq_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "Открыть эквалайзер",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Static Rounded-Square Album Cover Artwork (No vinyl record)
                TrackCoverArtwork(
                    track = track,
                    preloadedBitmap = playbackState.currentCoverBitmap,
                    size = 284.dp,
                    cornerRadius = 28.dp,
                    elevation = 20.dp,
                    modifier = Modifier.testTag("now_playing_cover_art")
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Live 18-band Spectrum Visualizer tinted by the cover's colors
                SpectrumAnalyzerView(
                    bars = playbackState.spectrumBars,
                    isPlaying = playbackState.isPlaying,
                    accentColor = animatedDominant,
                    secondaryColor = animatedSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Track title, artist & favorite button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onToggleFavorite(track) },
                        modifier = Modifier.testTag("now_playing_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Избранное",
                            tint = if (track.isFavorite) animatedDominant else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Seekbar + Timecodes
                val duration = playbackState.durationMs.coerceAtLeast(1000L)
                val currentPos = playbackState.currentPositionMs.coerceIn(0L, duration)
                Slider(
                    value = currentPos.toFloat(),
                    onValueChange = { onSeekTo(it.toLong()) },
                    valueRange = 0f..duration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = animatedDominant,
                        activeTrackColor = animatedDominant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("now_playing_seek_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDurationMs(currentPos),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDurationMs(duration),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Transport Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier.testTag("now_playing_shuffle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Перемешать",
                            tint = if (playbackState.isShuffleEnabled) {
                                animatedDominant
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    FilledIconButton(
                        onClick = onSkipPrevious,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("now_playing_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Предыдущий трек"
                        )
                    }

                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = animatedDominant
                        ),
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .testTag("now_playing_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Пауза" else "Играть",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = onSkipNext,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("now_playing_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Следующий трек"
                        )
                    }

                    IconButton(
                        onClick = onCycleRepeat,
                        modifier = Modifier.testTag("now_playing_repeat_button")
                    ) {
                        val icon = if (playbackState.repeatMode == RepeatMode.ONE) {
                            Icons.Default.RepeatOne
                        } else {
                            Icons.Default.Repeat
                        }
                        val tint = if (playbackState.repeatMode != RepeatMode.OFF) {
                            animatedDominant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Icon(imageVector = icon, contentDescription = "Режим повтора", tint = tint)
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Utility Chips: Speed, Sleep Timer, Equalizer Preset
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    AssistChip(
                        onClick = { showSpeedOptions = !showSpeedOptions },
                        label = { Text("Скорость ${playbackState.playbackSpeed}x") },
                        leadingIcon = {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("speed_chip")
                    )

                    val sleepLabel = playbackState.sleepTimerRemainingSeconds?.let {
                        "Сон: ${it / 60}:${String.format("%02d", it % 60)}"
                    } ?: "Таймер сна"

                    AssistChip(
                        onClick = { showSleepOptions = !showSleepOptions },
                        label = { Text(sleepLabel) },
                        leadingIcon = {
                            Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("sleep_timer_chip")
                    )

                    AssistChip(
                        onClick = onOpenEqualizer,
                        label = { Text("EQ: ${equalizerState.activePresetName}") },
                        leadingIcon = {
                            Icon(Icons.Default.Equalizer, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }

                if (showSpeedOptions) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { spd ->
                            FilterChip(
                                selected = playbackState.playbackSpeed == spd,
                                onClick = { onSetSpeed(spd) },
                                label = { Text("${spd}x") }
                            )
                        }
                    }
                }

                if (showSleepOptions) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        listOf(null to "Выкл", 5 to "5 мин", 15 to "15 мин", 30 to "30 мин").forEach { (mins, title) ->
                            FilterChip(
                                selected = (mins == null && playbackState.sleepTimerRemainingSeconds == null),
                                onClick = {
                                    onSetSleepTimer(mins)
                                    showSleepOptions = false
                                },
                                label = { Text(title) }
                            )
                        }
                    }
                }
            }
        }
    }
}
