package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.sp
import com.example.data.model.OnlineTrack
import com.example.data.model.Song
import com.example.data.online.OnlineMusicManager
import com.example.playback.PlaybackController
import com.example.playback.PlaybackState
import com.example.ui.components.AlbumArtThumbnail
import com.example.ui.components.EqualizerWaveAnimation
import kotlinx.coroutines.launch

enum class OnlineSourceTab {
    PIXABAY, FMA, AUDIUS, INTERNET_ARCHIVE, SYNCED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineMusicScreen(
    onlineManager: OnlineMusicManager,
    playbackState: PlaybackState,
    allSongs: List<Song>,
    controller: PlaybackController,
    onBack: () -> Unit,
    onNavigateToUnifiedSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(OnlineSourceTab.PIXABAY) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val pixabayTracks by onlineManager.pixabayTracks.collectAsState()
    val fmaTracks by onlineManager.fmaTracks.collectAsState()
    val audiusTracks by onlineManager.audiusTracks.collectAsState()
    val archiveTracks by onlineManager.archiveTracks.collectAsState()
    val searchResults by onlineManager.searchResults.collectAsState()
    val isLoading by onlineManager.isLoading.collectAsState()
    val syncingTrackId by onlineManager.syncingTrackId.collectAsState()
    val syncedTrackIds by onlineManager.syncedTrackIds.collectAsState()

    val syncedOnlineSongs = remember(allSongs) {
        allSongs.filter { it.isCloudSynced }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                onlineManager.searchOnline(it)
                            },
                            placeholder = { Text("Search online audio...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("online_search_field"),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        onlineManager.searchOnline("")
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    } else {
                        Column {
                            Text("Online Music Hub", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Pixabay, FMA, Audius & Archive.org",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                            onlineManager.searchOnline("")
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToUnifiedSearch,
                        modifier = Modifier.testTag("open_unified_search_from_online")
                    ) {
                        Icon(Icons.Default.TravelExplore, contentDescription = "Unified Search")
                    }
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.testTag("toggle_online_search_button")
                    ) {
                        Icon(
                            if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search Online"
                        )
                    }
                    IconButton(
                        onClick = { onlineManager.refreshOnlineMusic() },
                        modifier = Modifier.testTag("refresh_online_music_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Online Tracks")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Source Scrollable Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = selectedTab == OnlineSourceTab.PIXABAY,
                    onClick = { selectedTab = OnlineSourceTab.PIXABAY },
                    text = { Text("Pixabay (${pixabayTracks.size})") },
                    icon = { Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == OnlineSourceTab.FMA,
                    onClick = { selectedTab = OnlineSourceTab.FMA },
                    text = { Text("FMA (${fmaTracks.size})") },
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == OnlineSourceTab.AUDIUS,
                    onClick = { selectedTab = OnlineSourceTab.AUDIUS },
                    text = { Text("Audius (${audiusTracks.size})") },
                    icon = { Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == OnlineSourceTab.INTERNET_ARCHIVE,
                    onClick = { selectedTab = OnlineSourceTab.INTERNET_ARCHIVE },
                    text = { Text("Archive.org (${archiveTracks.size})") },
                    icon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == OnlineSourceTab.SYNCED,
                    onClick = { selectedTab = OnlineSourceTab.SYNCED },
                    text = { Text("Synced (${syncedOnlineSongs.size})") },
                    icon = { Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val displayTracks = when {
                isSearchActive && searchQuery.isNotBlank() -> searchResults
                selectedTab == OnlineSourceTab.PIXABAY -> pixabayTracks
                selectedTab == OnlineSourceTab.FMA -> fmaTracks
                selectedTab == OnlineSourceTab.AUDIUS -> audiusTracks
                selectedTab == OnlineSourceTab.INTERNET_ARCHIVE -> archiveTracks
                else -> emptyList()
            }

            if (selectedTab == OnlineSourceTab.SYNCED) {
                // Synced tracks list
                if (syncedOnlineSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No Online Songs Synced Yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Browse Pixabay, Free Music Archive, Audius or Archive.org and tap 'Sync' to download tracks for offline listening.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
                    ) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Offline Ready",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            "${syncedOnlineSongs.size} tracks saved in local storage",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            controller.playQueue(syncedOnlineSongs, 0)
                                        }
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Play All")
                                    }
                                }
                            }
                        }

                        items(syncedOnlineSongs, key = { it.id }) { song ->
                            val isCurrent = playbackState.currentSong?.id == song.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { controller.playSong(song) },
                                color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AlbumArtThumbnail(
                                        albumArtUri = song.albumArtUri,
                                        title = song.title,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${song.artist} • ${song.source}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            song.cloudFileId?.let { id ->
                                                coroutineScope.launch { onlineManager.removeSyncedTrack(id) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove synced track",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Online tracks list (Pixabay / FMA / Audius / Internet Archive)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
                ) {
                    // Header batch sync card
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val title = when (selectedTab) {
                                        OnlineSourceTab.PIXABAY -> "Pixabay Royalty-Free Music"
                                        OnlineSourceTab.FMA -> "Free Music Archive (FMA)"
                                        OnlineSourceTab.AUDIUS -> "Audius Music Stream"
                                        OnlineSourceTab.INTERNET_ARCHIVE -> "Internet Archive Audio"
                                        else -> "Online Tracks"
                                    }
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        "Stream online or sync to your offline music library",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (displayTracks.isNotEmpty()) {
                                    FilledTonalButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                onlineManager.syncAll(displayTracks.take(4))
                                            }
                                        },
                                        modifier = Modifier.testTag("sync_top_tracks_button")
                                    ) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sync Top 4")
                                    }
                                }
                            }
                        }
                    }

                    if (displayTracks.isEmpty() && !isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No tracks loaded. Tap Refresh to load online music.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(displayTracks, key = { it.id }) { track ->
                            val isSynced = track.id in syncedTrackIds
                            val isSyncing = syncingTrackId == track.id
                            val isPlayingThis = playbackState.currentSong?.cloudFileId == track.id

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isPlayingThis) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val queue = displayTracks.map { it.toSong() }
                                            val index = queue.indexOfFirst { it.cloudFileId == track.id }.coerceAtLeast(0)
                                            controller.playQueue(queue, index)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(52.dp)) {
                                        AlbumArtThumbnail(
                                            albumArtUri = track.artworkUrl,
                                            title = track.title,
                                            modifier = Modifier.fillMaxSize(),
                                            cornerRadius = 10.dp
                                        )
                                        if (isPlayingThis && playbackState.isPlaying) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color.Black.copy(alpha = 0.4f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                EqualizerWaveAnimation(
                                                    isPlaying = true,
                                                    color = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            track.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "${track.artist} • ${track.album}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            SourceBadge(source = track.source)
                                            Text(
                                                track.durationFormatted,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Sync Button
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (isSynced) {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    onlineManager.removeSyncedTrack(track.id)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.CloudDone,
                                                contentDescription = "Synced to offline. Tap to remove.",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        FilledTonalButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    onlineManager.syncTrackToLocal(track)
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CloudDownload,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sync", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
