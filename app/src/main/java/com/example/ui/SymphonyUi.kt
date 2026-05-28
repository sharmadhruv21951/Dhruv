package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.Playlist
import com.example.data.Song
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SymphonyAppScreen(viewModel: MusicViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    
    val context = LocalContext.current
    var isMaximizedPlayerVisible by remember { mutableStateOf(false) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    val sharedSong by viewModel.sharedSong.collectAsState()

    // Back button handling if player is maximized
    if (isMaximizedPlayerVisible) {
        BackHandler {
            isMaximizedPlayerVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        // Main Screen Scaffold holding Active Tab
        Scaffold(
            bottomBar = {
                Column {
                    // Mini Player Floating bar (Only displays if a song is loaded)
                    if (currentSong != null) {
                        MiniPlayerBar(
                            song = currentSong!!,
                            isPlaying = isPlaying,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onProgressBarClick = { isMaximizedPlayerVisible = true },
                            onNext = { viewModel.playNext() }
                        )
                    }

                    // Bottom Navigation Bar
                    SymphonyBottomNavigation(
                        currentTab = currentTab,
                        onTabSelected = { viewModel.selectTab(it) }
                    )
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Keep screens alive or transition smoothly
                Crossfade(targetState = currentTab, label = "tabScreenTransition") { tab ->
                    when (tab) {
                        0 -> HomeScreen(
                            viewModel = viewModel,
                            onPlaySong = { song, queue ->
                                viewModel.setAndPlaySong(song, queue)
                            },
                            onSettingsClick = { isSettingsVisible = true }
                        )
                        1 -> SearchScreen(viewModel = viewModel, onPlaySong = { song, queue ->
                            viewModel.setAndPlaySong(song, queue)
                        })
                        2 -> LibraryScreen(viewModel = viewModel, onPlaySong = { song, queue ->
                            viewModel.setAndPlaySong(song, queue)
                        })
                        3 -> SocialSyncScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Settings Dialog Overlay
        if (isSettingsVisible) {
            SymphonySettingsDialog(
                viewModel = viewModel,
                onDismiss = { isSettingsVisible = false }
            )
        }

        // Maximized Full-Screen Player Overlay
        AnimatedVisibility(
            visible = isMaximizedPlayerVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(stiffness = Spring.StiffnessLow)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = Spring.StiffnessLow))
        ) {
            if (currentSong != null) {
                MaximizedPlayerScreen(
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    viewModel = viewModel,
                    onCollapse = { isMaximizedPlayerVisible = false }
                )
            }
        }

        // Lyric Sharing Custom Card overlay pop up
        if (sharedSong != null) {
            LyricShareDialog(
                song = sharedSong!!,
                onDismiss = { viewModel.showShareCard(null) }
            )
        }
    }
}

// ==========================================
// TABS & NAVIGATIONS
// ==========================================

@Composable
fun SymphonyBottomNavigation(
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = BlackBg,
        tonalElevation = 8.dp,
        modifier = Modifier
            .height(56.dp)
            .testTag("bottom_nav_bar")
    ) {
        NavigationBarItem(
            selected = currentTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home", modifier = Modifier.size(20.dp)) },
            label = { Text("Home", fontSize = 9.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SpotifyGreen,
                selectedTextColor = SpotifyGreen,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = currentTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(if (currentTab == 1) Icons.Filled.Search else Icons.Outlined.Search, contentDescription = "Search", modifier = Modifier.size(20.dp)) },
            label = { Text("Search", fontSize = 9.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SpotifyGreen,
                selectedTextColor = SpotifyGreen,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = currentTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(if (currentTab == 2) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic, contentDescription = "Library", modifier = Modifier.size(20.dp)) },
            label = { Text("Library", fontSize = 9.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SpotifyGreen,
                selectedTextColor = SpotifyGreen,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = currentTab == 3,
            onClick = { onTabSelected(3) },
            icon = { Icon(if (currentTab == 3) Icons.Filled.Group else Icons.Outlined.Group, contentDescription = "Social", modifier = Modifier.size(20.dp)) },
            label = { Text("Premium Session", fontSize = 9.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SpotifyGreen,
                selectedTextColor = SpotifyGreen,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted,
                indicatorColor = Color.Transparent
            )
        )
    }
}

// ==========================================
// HOME SCREEN
// ==========================================

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onPlaySong: (Song, List<Song>) -> Unit,
    onSettingsClick: () -> Unit
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val recommendedSongs by viewModel.recommendedSongs.collectAsState()
    val isRecommendationLoading by viewModel.isRecommendationLoading.collectAsState()
    val recommendationMood by viewModel.recommendationMood.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Determine greeting word based on system time hour
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour in 0..11 -> "Good morning"
        hour in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // High Density StatusBar & Header
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Profile dot indicator "JD"
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF282828))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JD",
                            color = TextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = greeting,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        letterSpacing = (-0.5).sp
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = TextWhite,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                Toast.makeText(context, "Symphony notifications: No new alerts", Toast.LENGTH_SHORT).show()
                            }
                    )
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "Recent History",
                        tint = TextWhite,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                Toast.makeText(context, "Recent History: Loaded successfully", Toast.LENGTH_SHORT).show()
                            }
                    )
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextWhite,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                onSettingsClick()
                            }
                    )
                }
            }
        }

        // Filters pills row
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SpotifyGreen)
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Music",
                        color = DeepBlack,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF282828))
                        .clickable {
                            Toast.makeText(context, "Podcasts coming preview soon on Symphony!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Podcasts",
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Quick Category List grid
        item {
            Text(
                text = "Recently Played",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            // Show first 4 songs in a beautiful 2x2 custom Grid
            val recentList = allSongs.take(4)
            if (recentList.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in recentList.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RecentGridTile(
                                song = recentList[i],
                                onClick = { onPlaySong(recentList[i], allSongs) },
                                modifier = Modifier.weight(1f)
                            )
                            if (i + 1 < recentList.size) {
                                RecentGridTile(
                                    song = recentList[i + 1],
                                    onClick = { onPlaySong(recentList[i + 1], allSongs) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                Text("Prepopulating catalog, enjoy Symphony shortly...", color = TextMuted)
            }
        }

        // Gemini smart Recommendations system trigger Dashboard
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SpotifyGreen)
                            )
                            Text(
                                text = "Symphony Gemini AI Recommendations",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpotifyGreen
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Powered",
                            tint = SpotifyLightGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "Real-time playlists generated based on your preference atmosphere. Choose what you need:",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    // Mood selector pill buttons
                    val moods = listOf("Synth Wave", "Lo-Fi Beats", "Bollywood", "Electro Pop", "Relaxing Studio")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(moods) { mood ->
                            val isSelected = recommendationMood == mood
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) SpotifyGreen else Color(0xFF282828))
                                    .clickable { viewModel.triggerRecommendations(mood) }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = mood,
                                    color = if (isSelected) DeepBlack else TextWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Status of generated recommendation
                    if (isRecommendationLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = SpotifyGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini is curating recommendation...", color = TextMuted, fontSize = 11.sp)
                        }
                    } else {
                        // Horizontal cards recommendation view
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recommendedSongs) { song ->
                                Column(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .clickable { onPlaySong(song, recommendedSongs) },
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box {
                                        ArtImage(
                                            imageUrl = song.imageUrl,
                                            title = song.title,
                                            modifier = Modifier
                                                .size(110.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                        // Floating play circle badge
                                        Box(
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(DeepBlack.copy(alpha = 0.75f))
                                                .align(Alignment.BottomEnd)
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = SpotifyGreen,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    }
                                    Text(
                                        text = song.title,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Friends Activity (Listening with Friends) - New high-density layout widget from the theme
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x26282828)), // bg-[#282828]/24 (dark charcoal transparent mix)
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.selectTab(3) // Direct smooth redirection to sync session
                        Toast.makeText(context, "Redirecting to Group Session Sync space!", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Pulse dot indicator
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .graphicsLayer(alpha = alpha)
                                    .clip(CircleShape)
                                    .background(SpotifyGreen)
                            )
                            
                            Text(
                                text = "Listening with Friends",
                                color = TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = "JOIN SESSION",
                            color = SpotifyGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stacked avatars overlapping row: simple custom styled bullets
                        Box(
                            modifier = Modifier.width(52.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(x = 24.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE65C00))
                                    .border(1.dp, Color(0xFF121212), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .offset(x = 12.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8A2387))
                                    .border(1.dp, Color(0xFF121212), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(SpotifyGreen)
                                    .border(1.dp, Color(0xFF121212), CircleShape)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = "Rahul & 2 others are listening to Stay",
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Standard Music Tracks lists
        item {
            Text(
                text = "Discover All Songs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        items(allSongs) { song ->
            SongRowItem(
                song = song,
                onPlay = { onPlaySong(song, allSongs) },
                onFavoriteToggle = { viewModel.toggleFavorite(song) },
                onAddPlaylistClick = {
                    Toast.makeText(context, "Symphony: Added to Liked Songs and Daily Mixes!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun RecentGridTile(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF282828)),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            ArtImage(
                imageUrl = song.imageUrl,
                title = song.title,
                modifier = Modifier
                    .size(48.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f).padding(end = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = TextMuted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==========================================
// SEARCH SCREEN
// ==========================================

@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    onPlaySong: (Song, List<Song>) -> Unit
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Search Symphony",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Large rounded search input
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Song labels, creators, moods...", color = TextMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextWhite, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextWhite, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GraySurface,
                unfocusedContainerColor = GraySurface,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                cursorColor = SpotifyGreen,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 44.dp)
                .testTag("search_input_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (searchQuery.isNotEmpty()) {
            // Display results match list
            Text(
                text = "Results (${searchResults.size})",
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(searchResults) { song ->
                    SongRowItem(
                        song = song,
                        onPlay = { onPlaySong(song, searchResults) },
                        onFavoriteToggle = { viewModel.toggleFavorite(song) }
                    )
                }
            }
        } else {
            // Category grids representing Spotify-like quick explore tiles
            Text(
                text = "Explore Popular Genres",
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            val categories = listOf(
                Pair("Bollywood Blast", Color(0xFFC33764)),
                Pair("Lofi Chill Lounge", Color(0xFF1D976C)),
                Pair("Retro Synthwave", Color(0xFF6F38C5)),
                Pair("Acoustic Indie Beats", Color(0xFF2C5364)),
                Pair("Electronic Ibiza Dance", Color(0xFF8A2387)),
                Pair("High Definition Hip Hop", Color(0xFFE65C00))
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categories) { category ->
                    Box(
                        modifier = Modifier
                            .height(80.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(category.second, category.second.copy(alpha = 0.5f))
                                )
                            )
                            .clickable {
                                // Extract simple query tag
                                val q = category.first
                                    .replace("Blast", "")
                                    .replace("Chill", "")
                                    .replace("Retro", "")
                                    .replace("Beats", "")
                                    .trim()
                                viewModel.triggerRecommendations(q)
                                viewModel.selectTab(0) // Go to home to see matched items
                                Toast
                                    .makeText(
                                        context,
                                        "Triggered recommendations for: $q",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = category.first,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// LIBRARY SCREEN
// ==========================================

@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onPlaySong: (Song, List<Song>) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val isOfflineModeActive by viewModel.isOfflineModeActive.collectAsState()

    val activePlaylistId by viewModel.activePlaylistId.collectAsState()
    val activePlaylistWithSongs by viewModel.activePlaylistWithSongs.collectAsState()

    var isCreatePlaylistDialogVisible by remember { mutableStateOf(false) }
    var isAddCustomSongDialogVisible by remember { mutableStateOf(false) }
    var isAddSongToPlaylistDialogVisible by remember { mutableStateOf<Song?>(null) }

    val context = LocalContext.current

    // If viewing a playlist specific details
    if (activePlaylistId != null) {
        val playlist = activePlaylistWithSongs?.playlist
        val songsOfPlaylist = activePlaylistWithSongs?.songs ?: emptyList()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            // Header Row of Playlist
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.selectPlaylist(null) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                }
                
                IconButton(
                    onClick = {
                        viewModel.deletePlaylist(activePlaylistId!!)
                        Toast.makeText(context, "Playlist Deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = Color.Red.copy(alpha = 0.8f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = playlist?.name ?: "Symphony Playlist",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = SpotifyGreen
            )
            Text(
                text = playlist?.description ?: "No description provided",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Play whole Playlist button
            if (songsOfPlaylist.isNotEmpty()) {
                Button(
                    onClick = { onPlaySong(songsOfPlaylist[0], songsOfPlaylist) },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play All", tint = DeepBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play Live Mix (${songsOfPlaylist.size} Songs)", color = DeepBlack, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = "Empty", tint = TextMuted, modifier = Modifier.size(48.dp))
                        Text("No songs inside. Search and add songs to custom list!", color = TextMuted)
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(songsOfPlaylist) { song ->
                    SongRowItem(
                        song = song,
                        onPlay = { onPlaySong(song, songsOfPlaylist) },
                        onFavoriteToggle = { viewModel.toggleFavorite(song) },
                        trailingContent = {
                            IconButton(
                                onClick = { viewModel.removeSongFromPlaylist(activePlaylistId!!, song.id) }
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = Color.Red)
                            }
                        }
                    )
                }
            }
        }
        return
    }

    // Standard Library Screen with tabs/toggle list
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Playlist Action controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Collection",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Add Song manually button
                    IconButton(onClick = { isAddCustomSongDialogVisible = true }) {
                        Icon(Icons.Default.LibraryAdd, contentDescription = "Add custom URL", tint = SpotifyGreen)
                    }
                    // Add Playlist button
                    IconButton(onClick = { isCreatePlaylistDialogVisible = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Playlist", tint = TextWhite)
                    }
                }
            }
        }

        // Offline mode filtration toggle button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(GraySurface)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (isOfflineModeActive) Icons.Default.CloudOff else Icons.Default.CloudQueue,
                        contentDescription = "Offline Mode",
                        tint = if (isOfflineModeActive) SpotifyGreen else TextWhite
                    )
                    Column {
                        Text("Simulate Offline Mode Only", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Show only files downloaded for offline", color = TextMuted, fontSize = 11.sp)
                    }
                }
                Switch(
                    checked = isOfflineModeActive,
                    onCheckedChange = { viewModel.toggleOfflineMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DeepBlack,
                        checkedTrackColor = SpotifyGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = LightGraySurface
                    )
                )
            }
        }

        if (isOfflineModeActive) {
            // Displays downloaded files list
            item {
                Text(
                    text = "Offline Available Tracks (${downloadedSongs.size})",
                    color = SpotifyGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            if (downloadedSongs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No downloaded songs. Tap raw down arrow icon on song overlays to save offline!", color = TextMuted, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(downloadedSongs) { song ->
                    SongRowItem(
                        song = song,
                        onPlay = { onPlaySong(song, downloadedSongs) },
                        onFavoriteToggle = { viewModel.toggleFavorite(song) }
                    )
                }
            }
        } else {
            // Displays playlists list
            item {
                Text(
                    text = "Symphony Playlists (${playlists.size})",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (playlists.isEmpty()) {
                item {
                    Text("No playlists created. Make one by tapping plus icon on top right!", color = TextMuted, fontSize = 13.sp)
                }
            }

            items(playlists) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectPlaylist(playlist.id) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SpotifyGreen.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FeaturedPlayList, contentDescription = "Playlist", tint = SpotifyGreen)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(playlist.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(playlist.description, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Enter", tint = TextMuted)
                }
            }

            // Liked Songs Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Favorite, contentDescription = "Likes", tint = SpotifyGreen)
                    Text(
                        text = "Liked Songs (${favoriteSongs.size})",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            if (favoriteSongs.isEmpty()) {
                item {
                    Text("No liked songs yet. Heart some tunes!", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                items(favoriteSongs) { song ->
                    SongRowItem(
                        song = song,
                        onPlay = { onPlaySong(song, favoriteSongs) },
                        onFavoriteToggle = { viewModel.toggleFavorite(song) },
                        trailingContent = {
                            IconButton(onClick = { isAddSongToPlaylistDialogVisible = song }) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to Playlist", tint = TextWhite)
                            }
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Dialog Create Playlist
    if (isCreatePlaylistDialogVisible) {
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { isCreatePlaylistDialogVisible = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GraySurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Create New Symphony Playlist", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("E.g. Study Mix, Old Beats") },
                        colors = TextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                    )

                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Optional description") },
                        colors = TextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { isCreatePlaylistDialogVisible = false }) {
                            Text("Cancel", color = Color.Red)
                        }
                        Button(
                            onClick = {
                                if (name.isNotEmpty()) {
                                    viewModel.createPlaylist(name, description)
                                    isCreatePlaylistDialogVisible = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                        ) {
                            Text("Create", color = DeepBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog Add Custom Song
    if (isAddCustomSongDialogVisible) {
        var title by remember { mutableStateOf("") }
        var artist by remember { mutableStateOf("") }
        var genre by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { isAddCustomSongDialogVisible = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GraySurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Import Custom Track", color = SpotifyGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Song Title") },
                        colors = TextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                    )
                    TextField(
                        value = artist,
                        onValueChange = { artist = it },
                        placeholder = { Text("Artist / Composer") },
                        colors = TextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                    )
                    TextField(
                        value = genre,
                        onValueChange = { genre = it },
                        placeholder = { Text("Genre (Pop, Lofi)") },
                        colors = TextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                    )
                    TextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = { Text("Direct MP3 Url (Optional)") },
                        colors = TextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isAddCustomSongDialogVisible = false }) {
                            Text("Cancel", color = Color.Red)
                        }
                        Button(
                            onClick = {
                                if (title.isNotEmpty() && artist.isNotEmpty()) {
                                    viewModel.addCustomSong(title, artist, genre, url)
                                    isAddCustomSongDialogVisible = false
                                    Toast.makeText(context, "Successfully Imported Custom Song!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                        ) {
                            Text("Import", color = DeepBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog to select playlist to save a song to
    if (isAddSongToPlaylistDialogVisible != null) {
        val targetSong = isAddSongToPlaylistDialogVisible!!
        Dialog(onDismissRequest = { isAddSongToPlaylistDialogVisible = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GraySurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add song to playlist", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("'${targetSong.title}' will be copied to selected playlist mix:", color = TextMuted, fontSize = 12.sp)

                    Divider(color = LightGraySurface)

                    if (playlists.isEmpty()) {
                        Text("No playlists found. Create one first!", color = TextMuted)
                    }

                    playlists.forEach { playlist ->
                        Text(
                            text = playlist.name,
                            color = TextWhite,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addSongToPlaylist(playlist.id, targetSong.id)
                                    isAddSongToPlaylistDialogVisible = null
                                    Toast.makeText(context, "Copied to ${playlist.name}!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(onClick = { isAddSongToPlaylistDialogVisible = null }, modifier = Modifier.align(Alignment.End)) {
                        Text("Dismiss", color = SpotifyGreen)
                    }
                }
            }
        }
    }
}

// ==========================================
// SOCIAL SYNC SCREEN
// ==========================================

@Composable
fun SocialSyncScreen(viewModel: MusicViewModel) {
    val groupSessionCode by viewModel.groupSessionCode.collectAsState()
    val friendSessions by viewModel.friendSessions.collectAsState()
    val floatingEmojis by viewModel.floatingEmojis.collectAsState()

    var hostCodeToJoin by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Group, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(32.dp))
                Text(
                    text = "Symphony Group Sync",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
            Text(
                text = "Real-time synchronized audio listening panel. Host code sessions to stream direct music synchronously with friends, or tap on standard running sessions to align progress!",
                color = TextMuted,
                fontSize = 13.sp
            )

            if (groupSessionCode == null) {
                // Host / Join controllers layout
                Card(
                    colors = CardDefaults.cardColors(containerColor = GraySurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Start a Sync Session", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        
                        Button(
                            onClick = { viewModel.hostGroupSession() },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Host Live Sync Space", color = DeepBlack, fontWeight = FontWeight.Bold)
                        }

                        Divider(color = LightGraySurface, modifier = Modifier.padding(vertical = 4.dp))

                        Text("Join Friend's Live Session", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        TextField(
                            value = hostCodeToJoin,
                            onValueChange = { hostCodeToJoin = it },
                            placeholder = { Text("E.g. S-7422") },
                            colors = TextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (hostCodeToJoin.isNotEmpty()) {
                                    viewModel.joinGroupSession(hostCodeToJoin)
                                    Toast.makeText(context, "Connected to ${hostCodeToJoin.uppercase()}!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LightGraySurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Join Space", color = TextWhite)
                        }
                    }
                }
            } else {
                // Host State Screen
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkGreenGlow.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, SpotifyGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Connected Sync Session Active", color = SpotifyGreen, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { viewModel.leaveGroupSession() }) {
                                Text("Disconnect", color = Color.Red)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = groupSessionCode!!,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite,
                                letterSpacing = 2.sp
                            )
                        }

                        Text(
                            text = "Share this code with team. Real-time synchronised audio playback buffers when tracks are changed by any user in session.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Emoji feedback pad
                        Text("React on friend's ears:", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val reactions = listOf("🔥", "❤️", "🎉", "⚡", "🎸", "🌟")
                            reactions.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(LightGraySurface)
                                        .clickable { viewModel.dispatchGroupEmoji(emoji) }
                                ) {
                                    Text(emoji, fontSize = 20.sp, modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        }
                    }
                }
            }

            // Friend Active Sessions feed
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SpotifyGreen))
                Text("Friends Playing Right Now", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(friendSessions) { session ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GraySurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Friend Avatar circle
                            AsyncImage(
                                model = session.avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(session.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(12.dp))
                                    Text(
                                        "${session.currentSongTitle} - ${session.currentSongArtist}",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                // Mini synced progress indicator
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { session.elapsedMs.toFloat() / session.maxDurationMs },
                                    color = SpotifyGreen,
                                    trackColor = LightGraySurface,
                                    modifier = Modifier.fillMaxWidth().height(2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Action button to Sync Ear
                            Button(
                                onClick = {
                                    viewModel.syncPlaybackWithFriend(session)
                                    Toast.makeText(context, "Synced playback with ${session.name}!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkGreenGlow),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = SpotifyLightGreen, modifier = Modifier.size(12.dp))
                                    Text("Sync", fontSize = 11.sp, color = SpotifyLightGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating upward emojis animation
        floatingEmojis.forEach { emoji ->
            FloatingEmojiAnimation(emoji = emoji)
        }
    }
}

@Composable
fun FloatingEmojiAnimation(emoji: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "emojiFloat")
    val translationY by infiniteTransition.animateFloat(
        initialValue = 1000f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "emojiY"
    )
    val floatX = remember { (100..600).random().toFloat() }

    Box(
        modifier = Modifier
            .offset { IntOffset(floatX.roundToInt(), translationY.roundToInt()) }
    ) {
        Text(emoji, fontSize = 38.sp)
    }
}

// ==========================================
// DETAILED UTILITY PARTS & GENERAL ROWS
// ==========================================

@Composable
fun SongRowItem(
    song: Song,
    onPlay: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddPlaylistClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtImage(
            imageUrl = song.imageUrl,
            title = song.title,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (song.isDownloaded) {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = "Downloaded for offline playing",
                        tint = SpotifyGreen,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = "${song.artist} • ${song.genre}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Heart favorite icon toggle button
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like Song",
                    tint = if (song.isFavorite) SpotifyGreen else TextWhite
                )
            }

            if (trailingContent != null) {
                trailingContent()
            } else if (onAddPlaylistClick != null) {
                IconButton(onClick = onAddPlaylistClick) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "Add Playlist", tint = TextWhite)
                }
            }
        }
    }
}

@Composable
fun ArtImage(
    imageUrl: String,
    title: String,
    modifier: Modifier = Modifier
) {
    // Check if network URL is valid, if empty or error fallback beautifully
    if (imageUrl.isNotEmpty() && !imageUrl.contains("local")) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            error = null // let local gradient draw handle error automatically
        )
    } else {
        // Aesthetic Neon Spot Gradient fallback representing standard song art!
        Box(
            modifier = modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(SpotifyGreen, DarkGreenGlow)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.take(1).uppercase(),
                color = TextWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
        }
    }
}

// ==========================================
// MINI PLAYER FLOATING COMPONENT
// ==========================================

@Composable
fun MiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onProgressBarClick: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF5A3B2B)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onProgressBarClick() }
            .testTag("mini_player_bar")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArtImage(
                    imageUrl = song.imageUrl,
                    title = song.title,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                
                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = song.title,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        color = TextWhite.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "PlayPause",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Thin bottom 2px progress bar (2/3 full) from high density layout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.66f)
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }
        }
    }
}

// ==========================================
// MAXIMIZED FULL SCREEN PLAYER SCREEN
// ==========================================

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MaximizedPlayerScreen(
    song: Song,
    isPlaying: Boolean,
    viewModel: MusicViewModel,
    onCollapse: () -> Unit
) {
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val downloadingProgress by viewModel.downloadingSongsProgress.collectAsState()

    val currentDownloadProgress = downloadingProgress[song.id]
    val context = LocalContext.current

    // Rotation animation for spinning Vinyl artwork
    val infiniteTransition = rememberInfiniteTransition(label = "vinylRotate")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 12000 else 10000000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angleRotation"
    )

    // Lyrics tracker scroll simulator
    val lyricList = remember(song.lyrics) {
        if (song.lyrics.isEmpty()) {
            listOf("Symphony HD Audio Flowing...", "[Instrumental Vibes]", "Sing your heart out in real-time!")
        } else {
            song.lyrics.split("|")
        }
    }
    // Simple lyric line selection based on playback percentage
    val activeLyricIndex = remember(currentPosition, duration) {
        if (duration <= 0) 0
        else {
            val progressPercent = currentPosition.toFloat() / duration
            val finalIdx = (progressPercent * lyricList.size).toInt()
            finalIdx.coerceIn(0, lyricList.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkGreenGlow, DeepBlack)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("maximized_player_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Collapse Player", tint = TextWhite, modifier = Modifier.size(32.dp))
                }
                
                Text(
                    text = "PLAYING FROM SYMPHONY",
                    color = TextWhite.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                IconButton(
                    onClick = { viewModel.showShareCard(song) }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = SpotifyGreen)
                }
            }

            // High Definition spinning Spotify Artwork layer
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                // Background dark disk vinyl design circle
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .rotate(angle)
                        .border(3.dp, GraySurface, CircleShape)
                ) {
                    // Concentric circle vinyl groves
                    Box(modifier = Modifier.size(200.dp).align(Alignment.Center).border(1.dp, Color.DarkGray, CircleShape))
                    Box(modifier = Modifier.size(160.dp).align(Alignment.Center).border(1.dp, Color.DarkGray, CircleShape))
                }

                // Foreground square artwork inside
                ArtImage(
                    imageUrl = song.imageUrl,
                    title = song.title,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .border(2.dp, SpotifyGreen, CircleShape)
                )

                // Small Center Vinyl spindle pin circle
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(DeepBlack)
                        .border(2.dp, TextWhite, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text Info labels
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            color = TextMuted,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Heart Like Icon
                    IconButton(onClick = { viewModel.toggleFavorite(song) }) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (song.isFavorite) SpotifyGreen else TextWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Download to Offline Sync cloud trigger
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        if (currentDownloadProgress != null) {
                            CircularProgressIndicator(
                                progress = { currentDownloadProgress },
                                color = SpotifyGreen,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            IconButton(onClick = { viewModel.triggerSongDownload(song) }) {
                                Icon(
                                    imageVector = if (song.isDownloaded) Icons.Default.CloudDone else Icons.Default.Download,
                                    contentDescription = "Offline Download",
                                    tint = if (song.isDownloaded) SpotifyGreen else TextWhite,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Live progress slider seeker
            Column(modifier = Modifier.fillMaxWidth()) {
                val progressFloat = if (duration > 0) currentPosition.toFloat() / duration else 0f
                Slider(
                    value = progressFloat,
                    onValueChange = { newValue ->
                        val targetMs = (newValue * duration).toLong()
                        viewModel.seekTo(targetMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = TextWhite,
                        activeTrackColor = SpotifyGreen,
                        inactiveTrackColor = LightGraySurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatMs(currentPosition), color = TextMuted, fontSize = 11.sp)
                    Text(formatMs(duration.ifZero(song.durationMs)), color = TextMuted, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Player control panel triggers (Shuffle, Prev, PlayCircle, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { Toast.makeText(context, "Symphony Shuffle toggled!", Toast.LENGTH_SHORT).show() }) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = SpotifyGreen, modifier = Modifier.size(22.dp))
                }

                IconButton(onClick = { viewModel.playPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = TextWhite, modifier = Modifier.size(36.dp))
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(TextWhite)
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = DeepBlack, modifier = Modifier.size(30.dp))
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "PlayPause",
                            tint = DeepBlack,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                IconButton(onClick = { viewModel.playNext() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TextWhite, modifier = Modifier.size(36.dp))
                }

                IconButton(onClick = { Toast.makeText(context, "Loop selection active!", Toast.LENGTH_SHORT).show() }) {
                    Icon(Icons.Default.Loop, contentDescription = "Repeat", tint = TextMuted, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrolling Lyrics simulation subpanel
            Card(
                colors = CardDefaults.cardColors(containerColor = GraySurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Lyrics Real-time Sync", fontWeight = FontWeight.Bold, color = SpotifyGreen, fontSize = 12.sp)
                        Icon(Icons.Default.Waves, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(14.dp))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    lyricList.forEachIndexed { index, line ->
                        val isActiveLine = index == activeLyricIndex
                        Text(
                            text = line,
                            color = if (isActiveLine) SpotifyLightGreen else TextMuted.copy(alpha = 0.5f),
                            fontWeight = if (isActiveLine) FontWeight.ExtraBold else FontWeight.Bold,
                            fontSize = if (isActiveLine) 16.sp else 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = if (isActiveLine) 1.05f else 1f
                                    scaleY = if (isActiveLine) 1.05f else 1f
                                }
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ==========================================
// SHARE LYRICS DIALOG MOCK ACTION
// ==========================================

@Composable
fun LyricShareDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GraySurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Symphony Share Card",
                    fontWeight = FontWeight.Bold,
                    color = SpotifyGreen,
                    fontSize = 16.sp
                )

                // High-End Graphic design for lyric sharing card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SpotifyGreen, DarkGreenGlow, DeepBlack)
                            )
                        )
                        .border(1.dp, TextWhite.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Icon(Icons.Default.FormatQuote, contentDescription = null, tint = TextWhite, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Welcome to the Night Owl session...\nFloating in the cosmic black 🌌",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                style = androidx.compose.ui.text.TextStyle(lineHeight = 20.sp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(DeepBlack),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(song.title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(song.artist, color = TextMuted, fontSize = 9.sp)
                            }
                        }
                    }
                }

                Text(
                    text = "This lyric quote card looks amazing on stories or direct links!",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Listening to ${song.title} on Symphony")
                                putExtra(Intent.EXTRA_TEXT, "Listen to '${song.title}' by ${song.artist} synchronously with me on Symphony!\nSpace Code: SYNC-S7")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Track Quote"))
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Share Lyric Direct", color = DeepBlack, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Lyric Card copied to Clipboard!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LightGraySurface),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy Story Card", color = TextWhite)
                    }
                }
            }
        }
    }
}

// ==========================================
// UTIL MATHEMATIC FORMATTERS
// ==========================================

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val sec = (ms / 1000) % 60
    val min = (ms / 1000) / 60
    return String.format("%d:%02d", min, sec)
}

// Fallback logic
private fun Long.ifZero(fallback: Long): Long {
    return if (this == 0L) fallback else this
}

@Composable
fun SymphonySettingsDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val volumeBoost by viewModel.volumeBoost.collectAsState()
    
    var backupText by remember { mutableStateOf("") }
    var backupMessage by remember { mutableStateOf("") }
    var importText by remember { mutableStateOf("") }
    var importStatus by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1C1A)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("symphony_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Symphony Settings",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.15f))
                
                // Section 1: In-App Sound Control & Booster
                Text(
                    text = "🔊 Sound Booster (Amplifier)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Boost the audio loudness up to 300% (beyond mobile limits). Use carefully on headphones.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                // Boost Levels chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "100%" to 1.0f,
                        "150%" to 1.5f,
                        "200%" to 2.0f,
                        "300%" to 3.0f
                    ).forEach { (label, value) ->
                        val isSelected = volumeBoost == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFFE35D5D) else Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    viewModel.setVolumeBoost(value)
                                    Toast.makeText(context, "Volume Booster: $label", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = 0.15f))
                
                // Section 2: Playlist Backup & Restore
                Text(
                    text = "💾 Playlist Backup & Restore",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Save your playlists to a secure text chunk or restore playlists from a previously generated copy.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Backup controls
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val backup = viewModel.exportPlaylistsBackup()
                            backupText = backup
                            try {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Symphony Backup", backup)
                                clipboard.setPrimaryClip(clip)
                                backupMessage = "✅ Copied backup chunk to clipboard!"
                            } catch (e: Exception) {
                                backupMessage = "Generated backup, please copy from text area below."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC8D2F)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate & Copy Playlist Backup", fontSize = 11.sp, color = Color.White)
                }
                
                if (backupMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = backupMessage,
                        color = Color(0xFF9CCC65),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (backupText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = backupText,
                        onValueChange = {},
                        readOnly = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White.copy(alpha = 0.7f),
                            unfocusedTextColor = Color.White.copy(alpha = 0.7f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 100.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Restore controls
                Text(
                    text = "Paste Backup Chunk to Restore:",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    placeholder = { Text("Paste JSON string here...", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f)) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (importText.isBlank()) {
                                importStatus = "❌ Please paste some text first!"
                            } else {
                                val success = viewModel.importPlaylistsBackup(importText)
                                if (success) {
                                    importText = ""
                                    importStatus = "🎉 Restore success! Playlists imported."
                                    Toast.makeText(context, "Playlists restored successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    importStatus = "❌ Invalid backup. Incorrect format."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Verify & Restore Playlists", fontSize = 11.sp, color = Color.White)
                }
                
                if (importStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = importStatus,
                        color = if (importStatus.startsWith("🎉")) Color(0xFF9CCC65) else Color(0xFFEF5350),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
