package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.Song
import com.example.playback.PlaybackController
import com.example.playback.PlaybackState
import com.example.ui.components.AlbumArtThumbnail
import com.example.ui.components.SongListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    title: String,
    subtitle: String,
    albumArtUri: String? = null,
    songs: List<Song>,
    favoriteIds: Set<Long>,
    playbackState: PlaybackState,
    controller: PlaybackController,
    onBack: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            if (albumArtUri != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        AlbumArtThumbnail(
                            albumArtUri = albumArtUri,
                            title = title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            cornerRadius = 20.dp
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (songs.isNotEmpty()) {
                                controller.playQueue(songs, 0)
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
                            if (songs.isNotEmpty()) {
                                controller.playQueue(songs.shuffled(), 0)
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

            items(songs, key = { it.id }) { song ->
                val isCurrent = playbackState.currentSong?.id == song.id
                val isFav = song.id in favoriteIds
                SongListItem(
                    song = song,
                    isPlaying = playbackState.isPlaying,
                    isCurrent = isCurrent,
                    isFavorite = isFav,
                    onClick = {
                        val index = songs.indexOfFirst { it.id == song.id }
                        controller.playQueue(songs, index)
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
