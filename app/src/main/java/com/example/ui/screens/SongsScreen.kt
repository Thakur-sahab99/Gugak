package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Song
import com.example.data.model.SortOption
import com.example.playback.PlaybackController
import com.example.playback.PlaybackState
import com.example.ui.components.SongListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    songs: List<Song>,
    favoriteIds: Set<Long>,
    playbackState: PlaybackState,
    controller: PlaybackController,
    onBack: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortOption by remember { mutableStateOf(SortOption.TITLE) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedSongs = remember(songs, sortOption) {
        when (sortOption) {
            SortOption.TITLE -> songs.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> songs.sortedBy { it.artist.lowercase() }
            SortOption.ALBUM -> songs.sortedBy { it.album.lowercase() }
            SortOption.DURATION -> songs.sortedByDescending { it.durationMs }
            SortOption.DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("All Songs", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${songs.size} tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_songs_button")
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort Songs")
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (option) {
                                                SortOption.TITLE -> "Title"
                                                SortOption.ARTIST -> "Artist"
                                                SortOption.ALBUM -> "Album"
                                                SortOption.DURATION -> "Duration"
                                                SortOption.DATE_ADDED -> "Recently Added"
                                            }
                                        )
                                    },
                                    trailingIcon = {
                                        if (sortOption == option) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        sortOption = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
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
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (sortedSongs.isNotEmpty()) {
                                controller.playQueue(sortedSongs, 0)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All")
                    }

                    FilledTonalButton(
                        onClick = {
                            if (sortedSongs.isNotEmpty()) {
                                controller.playQueue(sortedSongs.shuffled(), 0)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                }
            }

            items(sortedSongs, key = { it.id }) { song ->
                val isCurrent = playbackState.currentSong?.id == song.id
                val isFav = song.id in favoriteIds
                SongListItem(
                    song = song,
                    isPlaying = playbackState.isPlaying,
                    isCurrent = isCurrent,
                    isFavorite = isFav,
                    onClick = {
                        val index = sortedSongs.indexOfFirst { it.id == song.id }
                        controller.playQueue(sortedSongs, index)
                    },
                    onToggleFavorite = { onToggleFavorite(song.id) },
                    onPlayNext = { controller.playNext(song) },
                    onAddToQueue = { controller.addToQueue(song) },
                    onAddToPlaylist = { onAddToPlaylist(song) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        }
    }
}
