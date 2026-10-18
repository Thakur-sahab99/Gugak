package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.ui.components.SongListItem
import kotlinx.coroutines.launch

enum class SearchFilter {
    ALL, OFFLINE, AUDIUS, ARCHIVE, PIXABAY, FMA, PODCAST, SPOTIFY
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    allSongs: List<Song>,
    onlineManager: OnlineMusicManager,
    favoriteIds: Set<Long>,
    playbackState: PlaybackState,
    controller: PlaybackController,
    onBack: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(SearchFilter.ALL) }

    val onlineResults by onlineManager.searchResults.collectAsState()
    val isOnlineLoading by onlineManager.isLoading.collectAsState()
    val syncingTrackId by onlineManager.syncingTrackId.collectAsState()
    val syncedTrackIds by onlineManager.syncedTrackIds.collectAsState()

    // Trigger online search whenever query changes
    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            onlineManager.searchOnline(query)
        }
    }

    val filteredOfflineSongs = remember(query, allSongs) {
        if (query.isBlank()) {
            emptyList()
        } else {
            val q = query.trim().lowercase()
            allSongs.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q) ||
                it.source.lowercase().contains(q)
            }
        }
    }

    val filteredOnlineTracks = remember(onlineResults, selectedFilter) {
        when (selectedFilter) {
            SearchFilter.ALL -> onlineResults
            SearchFilter.OFFLINE -> emptyList()
            SearchFilter.AUDIUS -> onlineResults.filter { it.source == "Audius" }
            SearchFilter.ARCHIVE -> onlineResults.filter { it.source == "Internet Archive" }
            SearchFilter.PIXABAY -> onlineResults.filter { it.source == "Pixabay Music" }
            SearchFilter.FMA -> onlineResults.filter { it.source == "Free Music Archive" }
            SearchFilter.PODCAST -> onlineResults.filter { it.source == "Podcast Index" }
            SearchFilter.SPOTIFY -> onlineResults.filter { it.source == "Spotify" }
        }
    }

    val showOffline = selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.OFFLINE

    val quickSearches = remember {
        listOf("Lofi", "Beethoven", "Cinematic", "Chill Ambient", "Electronic", "Piano", "Acoustic", "Jazz")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search offline & online music...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = {
                                    query = ""
                                    onlineManager.searchOnline("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .testTag("search_text_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Source Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == SearchFilter.ALL,
                        onClick = { selectedFilter = SearchFilter.ALL },
                        label = { Text("All Sources") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == SearchFilter.OFFLINE,
                        onClick = { selectedFilter = SearchFilter.OFFLINE },
                        label = { Text("Offline Library (${if (query.isBlank()) allSongs.size else filteredOfflineSongs.size})") },
                        leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == SearchFilter.PIXABAY,
                        onClick = { selectedFilter = SearchFilter.PIXABAY },
                        label = { Text("Pixabay") },
                        leadingIcon = { Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == SearchFilter.FMA,
                        onClick = { selectedFilter = SearchFilter.FMA },
                        label = { Text("Free Music Archive") },
                        leadingIcon = { Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == SearchFilter.AUDIUS,
                        onClick = { selectedFilter = SearchFilter.AUDIUS },
                        label = { Text("Audius") },
                        leadingIcon = { Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == SearchFilter.ARCHIVE,
                        onClick = { selectedFilter = SearchFilter.ARCHIVE },
                        label = { Text("Internet Archive") },
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == SearchFilter.PODCAST,
                        onClick = { selectedFilter = SearchFilter.PODCAST },
                        label = { Text("Podcasts") },
                        leadingIcon = { Icon(Icons.Default.Podcasts, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == SearchFilter.SPOTIFY,
                        onClick = { selectedFilter = SearchFilter.SPOTIFY },
                        label = { Text("Spotify") },
                        leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            if (isOnlineLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (query.isBlank()) {
                // Empty search landing with quick search recommendations
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 2.dp
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.TravelExplore,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Unified Music Search",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            "Search your device, Pixabay, Free Music Archive, Audius & Archive.org in one place",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "Popular Searches",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickSearches.forEach { item ->
                                SuggestionChip(
                                    onClick = { query = item },
                                    label = { Text(item) },
                                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            "Supported Music Sources",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                        )
                    }

                    val sourcesInfo = listOf(
                        Triple("Offline Library", "${allSongs.size} tracks stored locally on device", Icons.Default.Storage),
                        Triple("Pixabay Music", "Royalty-free lofi, cinematic, acoustic & beats", Icons.Default.Audiotrack),
                        Triple("Free Music Archive", "High-quality legal CC & public domain indie music", Icons.Default.LibraryMusic),
                        Triple("Audius Network", "Trending indie electronic, hip-hop & alternative tracks", Icons.Default.Headphones),
                        Triple("Internet Archive", "Live concerts, classical recordings & audiobooks", Icons.Default.Public),
                        Triple("Podcast Index", "Live podcasts across tech, music, comedy & news", Icons.Default.Podcasts),
                        Triple("Spotify", "Stream preview hits, global top charts & discover songs", Icons.Default.MusicNote)
                    )

                    items(sourcesInfo) { (name, desc, icon) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                // Search Results View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
                ) {
                    // Offline Results Section
                    if (showOffline && filteredOfflineSongs.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Offline Library (${filteredOfflineSongs.size})",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                TextButton(onClick = {
                                    controller.playQueue(filteredOfflineSongs, 0)
                                }) {
                                    Text("Play All")
                                }
                            }
                        }

                        items(filteredOfflineSongs, key = { "offline_${it.id}" }) { song ->
                            val isPlayingThis = playbackState.currentSong?.id == song.id
                            SongListItem(
                                song = song,
                                isPlaying = isPlayingThis && playbackState.isPlaying,
                                isFavorite = song.id in favoriteIds,
                                onClick = {
                                    val index = filteredOfflineSongs.indexOf(song)
                                    controller.playQueue(filteredOfflineSongs, index.coerceAtLeast(0))
                                },
                                onToggleFavorite = { onToggleFavorite(song.id) },
                                onAddToPlaylist = { onAddToPlaylist(song) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Online Results Section
                    if (selectedFilter != SearchFilter.OFFLINE) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Cloud,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Online Music (${filteredOnlineTracks.size})",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                if (filteredOnlineTracks.isNotEmpty()) {
                                    TextButton(onClick = {
                                        val songs = filteredOnlineTracks.map { it.toSong() }
                                        controller.playQueue(songs, 0)
                                    }) {
                                        Text("Play Online")
                                    }
                                }
                            }
                        }

                        if (filteredOnlineTracks.isEmpty() && !isOnlineLoading) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No online tracks found for '$query'. Try another search term.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredOnlineTracks, key = { "online_${it.id}" }) { track ->
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
                                                val trackSong = track.toSong()
                                                val queue = filteredOnlineTracks.map { it.toSong() }
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
                                                track.artist,
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

                    if (showOffline && filteredOfflineSongs.isEmpty() && (selectedFilter == SearchFilter.OFFLINE || filteredOnlineTracks.isEmpty())) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "No results for '$query'",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "Try adjusting filters or checking the spelling",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourceBadge(source: String) {
    val (badgeBg, badgeFg) = when (source) {
        "Pixabay Music" -> Color(0xFF00A86B).copy(alpha = 0.18f) to Color(0xFF008F5B)
        "Free Music Archive" -> Color(0xFF009688).copy(alpha = 0.18f) to Color(0xFF00796B)
        "Audius" -> Color(0xFF8A2BE2).copy(alpha = 0.18f) to Color(0xFF7B1FA2)
        "Internet Archive" -> Color(0xFFFF9800).copy(alpha = 0.18f) to Color(0xFFE65100)
        "Podcast Index" -> Color(0xFFE91E63).copy(alpha = 0.18f) to Color(0xFFC2185B)
        "Spotify" -> Color(0xFF1DB954).copy(alpha = 0.18f) to Color(0xFF1ED760)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeBg
    ) {
        Text(
            text = source,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = badgeFg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
