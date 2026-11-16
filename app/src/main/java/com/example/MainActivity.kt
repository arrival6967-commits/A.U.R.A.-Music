package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NowPlayingFullScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val musicViewModel: MusicViewModel = viewModel()
            MyApplicationTheme {
                AuraMusicApp(viewModel = musicViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraMusicApp(
    viewModel: MusicViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsStateWithLifecycle()
    val isNowPlayingOpen by viewModel.isNowPlayingSheetOpen.collectAsStateWithLifecycle()

    val openAudioDocumentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.importLocalAudioUris(uris)
            }
        }
    )

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { results ->
            val audioGranted = results[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
            if (audioGranted) {
                viewModel.scanDeviceMusicLibrary()
            }
        }
    )

    val requestScanPermissions = {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_AUDIO)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        try {
            permissionsLauncher.launch(perms.toTypedArray())
        } catch (_: Exception) {
            viewModel.scanDeviceMusicLibrary()
        }
    }

    LaunchedEffect(Unit) {
        requestScanPermissions()
    }

    LaunchedEffect(uiState.filter.statusBannerMessage) {
        if (uiState.filter.statusBannerMessage != null) {
            delay(3200L)
            viewModel.clearBannerMessage()
        }
    }

    val navItems = listOf(
        Triple(AppDestination.LIBRARY, stringResource(R.string.nav_library), Icons.Default.LibraryMusic),
        Triple(AppDestination.PLAYLISTS, stringResource(R.string.nav_playlists), Icons.AutoMirrored.Filled.QueueMusic),
        Triple(AppDestination.EQUALIZER, stringResource(R.string.nav_equalizer), Icons.Default.Equalizer)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 640.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Локальная медиатека • EQ: ${equalizerState.activePresetName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (equalizerState.isEnabled) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable { viewModel.navigateTo(AppDestination.EQUALIZER) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("top_bar_eq_badge")
                        ) {
                            Text(
                                text = if (equalizerState.isEnabled) "DSP ON" else "DSP OFF",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (equalizerState.isEnabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MiniPlayerBar(
                            playbackState = playbackState,
                            equalizerState = equalizerState,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onSkipNext = viewModel::skipNext,
                            onExpand = { viewModel.setNowPlayingSheetOpen(true) }
                        )

                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            navItems.forEach { (dest, label, icon) ->
                                val tag = when (dest) {
                                    AppDestination.LIBRARY -> "nav_tab_library"
                                    AppDestination.PLAYLISTS -> "nav_tab_playlists"
                                    AppDestination.EQUALIZER -> "nav_tab_equalizer"
                                }
                                NavigationBarItem(
                                    selected = currentDestination == dest,
                                    onClick = { viewModel.navigateTo(dest) },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag(tag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { (dest, label, icon) ->
                            val tag = when (dest) {
                                AppDestination.LIBRARY -> "nav_tab_library"
                                AppDestination.PLAYLISTS -> "nav_tab_playlists"
                                AppDestination.EQUALIZER -> "nav_tab_equalizer"
                            }
                            NavigationRailItem(
                                selected = currentDestination == dest,
                                onClick = { viewModel.navigateTo(dest) },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentDestination) {
                        AppDestination.LIBRARY -> {
                            LibraryScreen(
                                uiState = uiState,
                                playbackState = playbackState,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onSelectGenre = viewModel::selectGenre,
                                onToggleFavoritesOnly = viewModel::toggleFavoritesFilter,
                                onPlayTrack = { track -> viewModel.playTrack(track) },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                                onImportAudioClick = {
                                    openAudioDocumentsLauncher.launch(arrayOf("audio/*"))
                                },
                                onScanDeviceClick = {
                                    viewModel.scanDeviceMusicLibrary()
                                    requestScanPermissions()
                                }
                            )
                        }

                        AppDestination.PLAYLISTS -> {
                            PlaylistsScreen(
                                playlists = uiState.playlists,
                                allTracks = uiState.allTracks,
                                selectedPlaylistId = selectedPlaylistId,
                                onSelectPlaylist = viewModel::selectPlaylist,
                                onCreatePlaylist = viewModel::createPlaylist,
                                onDeletePlaylist = viewModel::deletePlaylist,
                                onPlayTrackInPlaylist = { track, queue ->
                                    viewModel.playTrack(track, queue)
                                },
                                onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                                onRemoveTrackFromPlaylist = viewModel::removeTrackFromPlaylist,
                                onBackToLibrary = { viewModel.navigateTo(AppDestination.LIBRARY) }
                            )
                        }

                        AppDestination.EQUALIZER -> {
                            EqualizerScreen(
                                equalizerState = equalizerState,
                                presets = uiState.equalizerPresets,
                                onToggleEnabled = viewModel::setEqualizerEnabled,
                                onBandChange = viewModel::updateEqualizerBand,
                                onBassBoostChange = viewModel::setBassBoostStrength,
                                onVirtualizerChange = viewModel::setVirtualizerStrength,
                                onLoudnessChange = viewModel::setLoudnessGainDb,
                                onApplyPreset = viewModel::applyEqualizerPreset,
                                onResetFlat = viewModel::resetEqualizerFlat,
                                onSaveCustomPreset = viewModel::saveCustomEqualizerPreset,
                                onDeleteCustomPreset = viewModel::deleteCustomPreset,
                                onBackToLibrary = { viewModel.navigateTo(AppDestination.LIBRARY) }
                            )
                        }
                    }

                    if (isExpandedScreen) {
                        MiniPlayerBar(
                            playbackState = playbackState,
                            equalizerState = equalizerState,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onSkipNext = viewModel::skipNext,
                            onExpand = { viewModel.setNowPlayingSheetOpen(true) },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }

                    // Status toast banner
                    androidx.compose.animation.AnimatedVisibility(
                        visible = uiState.filter.statusBannerMessage != null,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            tonalElevation = 6.dp,
                            modifier = Modifier.testTag("status_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = uiState.filter.statusBannerMessage.orEmpty(),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        // Full-Screen Now Playing Sheet
        AnimatedVisibility(
            visible = isNowPlayingOpen && playbackState.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            NowPlayingFullScreen(
                playbackState = playbackState,
                equalizerState = equalizerState,
                onClose = { viewModel.setNowPlayingSheetOpen(false) },
                onTogglePlayPause = viewModel::togglePlayPause,
                onSkipNext = viewModel::skipNext,
                onSkipPrevious = viewModel::skipPrevious,
                onSeekTo = viewModel::seekTo,
                onToggleShuffle = viewModel::toggleShuffle,
                onCycleRepeat = viewModel::cycleRepeatMode,
                onToggleFavorite = viewModel::toggleFavorite,
                onSetSpeed = viewModel::setPlaybackSpeed,
                onSetSleepTimer = viewModel::setSleepTimerMinutes,
                onOpenEqualizer = {
                    viewModel.setNowPlayingSheetOpen(false)
                    viewModel.navigateTo(AppDestination.EQUALIZER)
                }
            )
        }
    }
}
