package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ThemeMode
import com.example.data.model.Song
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.*
import com.example.ui.theme.GugakTheme
import kotlinx.coroutines.launch

sealed class Screen {
    object Splash : Screen()
    object Onboarding : Screen()
    object Home : Screen()
    object Songs : Screen()
    object OnlineMusic : Screen()
    object Albums : Screen()
    object Artists : Screen()
    object Folders : Screen()
    object Playlists : Screen()
    object Favorites : Screen()
    object Search : Screen()
    object Settings : Screen()
    data class CategoryDetail(
        val title: String,
        val subtitle: String,
        val albumArtUri: String? = null,
        val songs: List<Song>
    ) : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as GugakApplication
        val preferences = app.preferences
        val repository = app.repository
        val controller = app.playbackController
        val onlineMusicManager = app.onlineMusicManager

        setContent {
            val themeMode by preferences.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by preferences.dynamicColor.collectAsStateWithLifecycle()
            val hasSeenOnboarding by preferences.hasSeenOnboarding.collectAsStateWithLifecycle()

            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            GugakTheme(darkTheme = isDark, dynamicColor = dynamicColor) {
                val coroutineScope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }

                var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
                val screenBackStack = remember { mutableStateListOf<Screen>() }

                var isNowPlayingOpen by remember { mutableStateOf(false) }
                var isQueueOpen by remember { mutableStateOf(false) }
                var songForPlaylistDialog by remember { mutableStateOf<Song?>(null) }
                var showCreatePlaylistInDialog by remember { mutableStateOf(false) }

                val allSongs by repository.songs.collectAsStateWithLifecycle()
                val albums by repository.albums.collectAsStateWithLifecycle(emptyList())
                val artists by repository.artists.collectAsStateWithLifecycle(emptyList())
                val folders by repository.folders.collectAsStateWithLifecycle(emptyList())
                val playlists by repository.rawPlaylists.collectAsStateWithLifecycle()
                val favoriteIds by repository.favoriteSongIds.collectAsStateWithLifecycle()
                val recentSongs by repository.getRecentlyPlayedSongs().collectAsStateWithLifecycle(emptyList())
                val isScanning by repository.isScanning.collectAsStateWithLifecycle()
                val syncedTrackCount by repository.cloudTracksCount.collectAsStateWithLifecycle(0)

                val playbackState by controller.state.collectAsStateWithLifecycle()

                fun navigateTo(newScreen: Screen) {
                    screenBackStack.add(currentScreen)
                    currentScreen = newScreen
                }

                fun navigateBack() {
                    if (screenBackStack.isNotEmpty()) {
                        currentScreen = screenBackStack.removeLast()
                    } else if (currentScreen != Screen.Home) {
                        currentScreen = Screen.Home
                    }
                }

                BackHandler(enabled = screenBackStack.isNotEmpty() || currentScreen != Screen.Home) {
                    navigateBack()
                }

                val currentSongIsFavorite = playbackState.currentSong?.let { it.id in favoriteIds } ?: false

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Main Screen Flow
                        when (val screen = currentScreen) {
                            is Screen.Splash -> {
                                SplashScreen(
                                    onSplashFinished = {
                                        if (hasSeenOnboarding) {
                                            currentScreen = Screen.Home
                                        } else {
                                            currentScreen = Screen.Onboarding
                                        }
                                    }
                                )
                            }
                            is Screen.Onboarding -> {
                                OnboardingScreen(
                                    onComplete = {
                                        preferences.setHasSeenOnboarding(true)
                                        repository.rescanLibrary()
                                        currentScreen = Screen.Home
                                    }
                                )
                            }
                            is Screen.Home -> {
                                HomeScreen(
                                    allSongs = allSongs,
                                    recentlyPlayed = recentSongs,
                                    favoriteIds = favoriteIds,
                                    isScanning = isScanning,
                                    playbackState = playbackState,
                                    controller = controller,
                                    onNavigateToCategory = { route ->
                                        when (route) {
                                            "songs" -> navigateTo(Screen.Songs)
                                            "online_music" -> navigateTo(Screen.OnlineMusic)
                                            "albums" -> navigateTo(Screen.Albums)
                                            "artists" -> navigateTo(Screen.Artists)
                                            "playlists" -> navigateTo(Screen.Playlists)
                                            "favorites" -> navigateTo(Screen.Favorites)
                                            "folders" -> navigateTo(Screen.Folders)
                                        }
                                    },
                                    onNavigateToSearch = { navigateTo(Screen.Search) },
                                    onNavigateToSettings = { navigateTo(Screen.Settings) },
                                    onRescan = { repository.rescanLibrary() },
                                    onNavigateToOnlineMusic = { navigateTo(Screen.OnlineMusic) },
                                    onAddToPlaylist = { songForPlaylistDialog = it }
                                )
                            }
                            is Screen.Songs -> {
                                SongsScreen(
                                    songs = allSongs,
                                    favoriteIds = favoriteIds,
                                    playbackState = playbackState,
                                    controller = controller,
                                    onBack = { navigateBack() },
                                    onToggleFavorite = { id ->
                                        coroutineScope.launch { repository.toggleFavorite(id) }
                                    },
                                    onAddToPlaylist = { songForPlaylistDialog = it }
                                )
                            }
                            is Screen.Albums -> {
                                AlbumsScreen(
                                    albums = albums,
                                    onSelectAlbum = { album ->
                                        val albumSongs = allSongs.filter { it.albumId == album.id }
                                        navigateTo(
                                            Screen.CategoryDetail(
                                                title = album.title,
                                                subtitle = "${album.artist} • ${albumSongs.size} songs",
                                                albumArtUri = album.albumArtUri,
                                                songs = albumSongs
                                            )
                                        )
                                    },
                                    onBack = { navigateBack() }
                                )
                            }
                            is Screen.Artists -> {
                                ArtistsScreen(
                                    artists = artists,
                                    onSelectArtist = { artist ->
                                        val artistSongs = allSongs.filter { it.artist == artist.name }
                                        navigateTo(
                                            Screen.CategoryDetail(
                                                title = artist.name,
                                                subtitle = "${artistSongs.size} songs",
                                                songs = artistSongs
                                            )
                                        )
                                    },
                                    onBack = { navigateBack() }
                                )
                            }
                            is Screen.Folders -> {
                                FoldersScreen(
                                    folders = folders,
                                    onSelectFolder = { folder ->
                                        val folderSongs = allSongs.filter { it.folderPath == folder.path }
                                        navigateTo(
                                            Screen.CategoryDetail(
                                                title = folder.name,
                                                subtitle = "${folderSongs.size} tracks • ${folder.path}",
                                                songs = folderSongs
                                            )
                                        )
                                    },
                                    onBack = { navigateBack() }
                                )
                            }
                            is Screen.Playlists -> {
                                PlaylistsScreen(
                                    playlists = playlists,
                                    onCreatePlaylist = { name ->
                                        coroutineScope.launch { repository.createPlaylist(name) }
                                    },
                                    onRenamePlaylist = { id, name ->
                                        coroutineScope.launch { repository.renamePlaylist(id, name) }
                                    },
                                    onDeletePlaylist = { id ->
                                        coroutineScope.launch { repository.deletePlaylist(id) }
                                    },
                                    onSelectPlaylist = { pl ->
                                        coroutineScope.launch {
                                            repository.getSongsForPlaylist(pl.id).collect { plSongs ->
                                                navigateTo(
                                                    Screen.CategoryDetail(
                                                        title = pl.name,
                                                        subtitle = "Playlist • ${plSongs.size} tracks",
                                                        songs = plSongs
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    onBack = { navigateBack() }
                                )
                            }
                            is Screen.Favorites -> {
                                val favoriteSongs = allSongs.filter { it.id in favoriteIds }
                                SongsScreen(
                                    songs = favoriteSongs,
                                    favoriteIds = favoriteIds,
                                    playbackState = playbackState,
                                    controller = controller,
                                    onBack = { navigateBack() },
                                    onToggleFavorite = { id ->
                                        coroutineScope.launch { repository.toggleFavorite(id) }
                                    },
                                    onAddToPlaylist = { songForPlaylistDialog = it }
                                )
                            }
                            is Screen.Search -> {
                                SearchScreen(
                                    allSongs = allSongs,
                                    onlineManager = onlineMusicManager,
                                    favoriteIds = favoriteIds,
                                    playbackState = playbackState,
                                    controller = controller,
                                    onBack = { navigateBack() },
                                    onToggleFavorite = { id ->
                                        coroutineScope.launch { repository.toggleFavorite(id) }
                                    },
                                    onAddToPlaylist = { songForPlaylistDialog = it }
                                )
                            }
                            is Screen.Settings -> {
                                SettingsScreen(
                                    preferences = preferences,
                                    isScanning = isScanning,
                                    syncedTrackCount = syncedTrackCount,
                                    onRescanLibrary = { repository.rescanLibrary() },
                                    onClearOnlineCache = {
                                        coroutineScope.launch {
                                            onlineMusicManager.clearAllSyncedOnlineTracks()
                                            snackbarHostState.showSnackbar("Synced online cache cleared")
                                        }
                                    },
                                    onNavigateToOnlineHub = { navigateTo(Screen.OnlineMusic) },
                                    onBack = { navigateBack() }
                                )
                            }
                            is Screen.OnlineMusic -> {
                                OnlineMusicScreen(
                                    onlineManager = onlineMusicManager,
                                    playbackState = playbackState,
                                    allSongs = allSongs,
                                    controller = controller,
                                    onBack = { navigateBack() },
                                    onNavigateToUnifiedSearch = { navigateTo(Screen.Search) }
                                )
                            }
                            is Screen.CategoryDetail -> {
                                CategoryDetailScreen(
                                    title = screen.title,
                                    subtitle = screen.subtitle,
                                    albumArtUri = screen.albumArtUri,
                                    songs = screen.songs,
                                    favoriteIds = favoriteIds,
                                    playbackState = playbackState,
                                    controller = controller,
                                    onBack = { navigateBack() },
                                    onToggleFavorite = { id ->
                                        coroutineScope.launch { repository.toggleFavorite(id) }
                                    },
                                    onAddToPlaylist = { songForPlaylistDialog = it }
                                )
                            }
                        }

                        // Persistent Mini Player above screen bottom
                        if (currentScreen != Screen.Splash && currentScreen != Screen.Onboarding && !isNowPlayingOpen) {
                            MiniPlayer(
                                state = playbackState,
                                onExpand = { isNowPlayingOpen = true },
                                onTogglePlay = { controller.togglePlayPause() },
                                onNext = { controller.next() },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            )
                        }

                        // Full Screen Now Playing View
                        AnimatedVisibility(
                            visible = isNowPlayingOpen && playbackState.currentSong != null,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it }),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            NowPlayingScreen(
                                state = playbackState,
                                isFavorite = currentSongIsFavorite,
                                controller = controller,
                                onClose = { isNowPlayingOpen = false },
                                onOpenQueue = { isQueueOpen = true },
                                onToggleFavorite = {
                                    playbackState.currentSong?.id?.let { songId ->
                                        coroutineScope.launch { repository.toggleFavorite(songId) }
                                    }
                                },
                                onAddToPlaylist = {
                                    playbackState.currentSong?.let { current ->
                                        songForPlaylistDialog = current
                                    }
                                }
                            )
                        }

                        // Queue Sheet / Dialog
                        AnimatedVisibility(
                            visible = isQueueOpen,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it }),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            QueueScreen(
                                state = playbackState,
                                controller = controller,
                                onClose = { isQueueOpen = false }
                            )
                        }

                        // Playlist Dialogs
                        songForPlaylistDialog?.let { song ->
                            AddToPlaylistDialog(
                                playlists = playlists,
                                onDismiss = { songForPlaylistDialog = null },
                                onSelectPlaylist = { playlistId ->
                                    coroutineScope.launch {
                                        repository.addSongToPlaylist(playlistId, song.id)
                                        snackbarHostState.showSnackbar("Added '${song.title}' to playlist")
                                        songForPlaylistDialog = null
                                    }
                                },
                                onCreateNewPlaylist = {
                                    showCreatePlaylistInDialog = true
                                }
                            )
                        }

                        if (showCreatePlaylistInDialog) {
                            CreatePlaylistDialog(
                                onDismiss = { showCreatePlaylistInDialog = false },
                                onConfirm = { name ->
                                    coroutineScope.launch {
                                        val newId = repository.createPlaylist(name)
                                        songForPlaylistDialog?.let { song ->
                                            repository.addSongToPlaylist(newId, song.id)
                                            snackbarHostState.showSnackbar("Created '$name' and added '${song.title}'")
                                        }
                                        showCreatePlaylistInDialog = false
                                        songForPlaylistDialog = null
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            val app = application as? GugakApplication
            app?.playbackController?.stop()
        }
    }
}
