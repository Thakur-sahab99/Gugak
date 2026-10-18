package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Song
import com.example.playback.PlaybackController
import com.example.playback.PlaybackState
import com.example.ui.components.AlbumArtThumbnail
import com.example.ui.components.SongListItem
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    allSongs: List<Song>,
    recentlyPlayed: List<Song>,
    favoriteIds: Set<Long>,
    isScanning: Boolean,
    playbackState: PlaybackState,
    controller: PlaybackController,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRescan: () -> Unit,
    onNavigateToOnlineMusic: () -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..22 -> "Good evening"
            else -> "Night owl vibes"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Gugak",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.testTag("home_search_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search library")
                    }
                    IconButton(
                        onClick = onNavigateToOnlineMusic,
                        modifier = Modifier.testTag("home_online_music_button")
                    ) {
                        Icon(Icons.Default.Public, contentDescription = "Online Music Hub")
                    }
                    IconButton(
                        onClick = onRescan,
                        modifier = Modifier.testTag("home_rescan_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan audio files")
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            if (isScanning) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Hero Pixel Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${allSongs.size} tracks in library • Offline & Online Music",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    if (allSongs.isNotEmpty()) {
                                        controller.playQueue(allSongs.shuffled(), 0)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shuffle All")
                            }

                            FilledTonalButton(
                                onClick = {
                                    if (allSongs.isNotEmpty()) {
                                        controller.playQueue(allSongs, 0)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play All")
                            }
                        }
                    }
                }
            }

            // Quick Category Navigation Chips
            item {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf(
                        Triple("Songs", Icons.Default.MusicNote, "songs"),
                        Triple("Online Music", Icons.Default.Public, "online_music"),
                        Triple("Albums", Icons.Default.Album, "albums"),
                        Triple("Artists", Icons.Default.Person, "artists"),
                        Triple("Playlists", Icons.Default.QueueMusic, "playlists"),
                        Triple("Favorites", Icons.Default.Favorite, "favorites"),
                        Triple("Folders", Icons.Default.Folder, "folders")
                    )
                    items(categories) { (label, icon, route) ->
                        FilterChip(
                            selected = false,
                            onClick = { onNavigateToCategory(route) },
                            label = { Text(label) },
                            leadingIcon = {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }

            // Recently Played Section
            if (recentlyPlayed.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Recently Played",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentlyPlayed.take(8)) { song ->
                            Column(
                                modifier = Modifier
                                    .width(128.dp)
                                    .clickable { controller.playSong(song) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(128.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                ) {
                                    AlbumArtThumbnail(
                                        albumArtUri = song.albumArtUri,
                                        title = song.title,
                                        modifier = Modifier.fillMaxSize(),
                                        cornerRadius = 16.dp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Recently Added Songs Section
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Added & Synced",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = { onNavigateToCategory("songs") }) {
                        Text("See All (${allSongs.size})")
                    }
                }
            }

            if (allSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Audiotrack,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "No music found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tap 'Sync Cloud Audio' above or refresh device storage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(allSongs.take(8), key = { it.id }) { song ->
                    val isCurrent = playbackState.currentSong?.id == song.id
                    val isFav = song.id in favoriteIds
                    SongListItem(
                        song = song,
                        isPlaying = playbackState.isPlaying,
                        isCurrent = isCurrent,
                        isFavorite = isFav,
                        onClick = { controller.playSong(song) },
                        onToggleFavorite = {
                            // Handled at root
                        },
                        onPlayNext = { controller.playNext(song) },
                        onAddToQueue = { controller.addToQueue(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
