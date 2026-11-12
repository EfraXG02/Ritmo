package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayer
import com.example.ui.components.QueueBottomSheet
import com.example.ui.components.SleepTimerDialog
import com.example.ui.screens.ArtistRadioScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceLight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.NavTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MusicViewModel = viewModel()) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val isFullPlayerVisible by viewModel.isFullPlayerVisible.collectAsStateWithLifecycle()
    val showSleepTimerDialog by viewModel.showSleepTimerDialog.collectAsStateWithLifecycle()
    val showQueueSheet by viewModel.showQueueSheet.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val featuredArtists = viewModel.featuredArtists
    val artistHistory by viewModel.artistShuffleHistory.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearchingOnline by viewModel.isSearchingOnline.collectAsStateWithLifecycle()
    val isGeneratingShuffle by viewModel.isGeneratingShuffle.collectAsStateWithLifecycle()

    // Handle Toast notifications
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DarkBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Mini player bar when a track is active
                    AnimatedVisibility(
                        visible = playerState.currentTrack != null && !isFullPlayerVisible,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        MiniPlayer(
                            playerState = playerState,
                            onExpand = { viewModel.setFullPlayerVisible(true) },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.skipNext() }
                        )
                    }

                    // Navigation Bar
                    NavigationBar(
                        containerColor = DarkSurface,
                        contentColor = TextPrimary,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        // Home
                        NavigationBarItem(
                            selected = currentTab == NavTab.HOME,
                            onClick = { viewModel.selectTab(NavTab.HOME) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == NavTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Início"
                                )
                            },
                            label = { Text("Músicas", fontSize = 11.sp, fontWeight = if (currentTab == NavTab.HOME) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = TextMuted,
                                selectedTextColor = NeonCyan,
                                unselectedTextColor = TextMuted,
                                indicatorColor = NeonViolet
                            ),
                            modifier = Modifier.testTag("nav_tab_home")
                        )

                        // Modo Cantor (Artist Shuffle Hub)
                        NavigationBarItem(
                            selected = currentTab == NavTab.ARTIST_SHUFFLE,
                            onClick = { viewModel.selectTab(NavTab.ARTIST_SHUFFLE) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == NavTab.ARTIST_SHUFFLE) Icons.Filled.Shuffle else Icons.Outlined.Shuffle,
                                    contentDescription = "Por Cantor"
                                )
                            },
                            label = { Text("Por Cantor", fontSize = 11.sp, fontWeight = if (currentTab == NavTab.ARTIST_SHUFFLE) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = TextMuted,
                                selectedTextColor = NeonCyan,
                                unselectedTextColor = TextMuted,
                                indicatorColor = NeonViolet
                            ),
                            modifier = Modifier.testTag("nav_tab_artist_shuffle")
                        )

                        // Library
                        NavigationBarItem(
                            selected = currentTab == NavTab.LIBRARY,
                            onClick = { viewModel.selectTab(NavTab.LIBRARY) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == NavTab.LIBRARY) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                                    contentDescription = "Biblioteca"
                                )
                            },
                            label = { Text("Biblioteca", fontSize = 11.sp, fontWeight = if (currentTab == NavTab.LIBRARY) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = TextMuted,
                                selectedTextColor = NeonCyan,
                                unselectedTextColor = TextMuted,
                                indicatorColor = NeonViolet
                            ),
                            modifier = Modifier.testTag("nav_tab_library")
                        )

                        // Search
                        NavigationBarItem(
                            selected = currentTab == NavTab.SEARCH,
                            onClick = { viewModel.selectTab(NavTab.SEARCH) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == NavTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                                    contentDescription = "Buscar"
                                )
                            },
                            label = { Text("Buscar", fontSize = 11.sp, fontWeight = if (currentTab == NavTab.SEARCH) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = TextMuted,
                                selectedTextColor = NeonCyan,
                                unselectedTextColor = TextMuted,
                                indicatorColor = NeonViolet
                            ),
                            modifier = Modifier.testTag("nav_tab_search")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    NavTab.HOME -> {
                        HomeScreen(
                            playerState = playerState,
                            tracks = allTracks,
                            favoriteTracks = favoriteTracks,
                            onTrackSelect = { track, queue ->
                                viewModel.playTrack(track, queue)
                            },
                            onFavoriteToggle = { track ->
                                viewModel.toggleFavorite(track)
                            }
                        )
                    }

                    NavTab.ARTIST_SHUFFLE -> {
                        ArtistRadioScreen(
                            playerState = playerState,
                            featuredArtists = featuredArtists,
                            isGeneratingShuffle = isGeneratingShuffle,
                            onStartArtistShuffle = { name, vibe ->
                                viewModel.startArtistShuffle(name, vibe)
                            }
                        )
                    }

                    NavTab.LIBRARY -> {
                        LibraryScreen(
                            playerState = playerState,
                            allTracks = allTracks,
                            favoriteTracks = favoriteTracks,
                            onTrackSelect = { track, queue ->
                                viewModel.playTrack(track, queue)
                            },
                            onFavoriteToggle = { track ->
                                viewModel.toggleFavorite(track)
                            },
                            onStartArtistShuffle = { name, vibe ->
                                viewModel.startArtistShuffle(name, vibe)
                            },
                            onScanLocalMedia = {
                                viewModel.scanLocalMedia()
                            }
                        )
                    }

                    NavTab.SEARCH -> {
                        SearchScreen(
                            playerState = playerState,
                            searchQuery = searchQuery,
                            searchResults = searchResults,
                            isSearchingOnline = isSearchingOnline,
                            onQueryChange = { q -> viewModel.onSearchQueryChanged(q) },
                            onTrackSelect = { track -> viewModel.playTrack(track, searchResults) },
                            onFavoriteToggle = { track -> viewModel.toggleFavorite(track) },
                            onStartArtistShuffle = { name, vibe -> viewModel.startArtistShuffle(name, vibe) }
                        )
                    }
                }
            }
        }

        // Expanded Full Player Modal
        AnimatedVisibility(
            visible = isFullPlayerVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            FullPlayerSheet(
                playerState = playerState,
                onDismiss = { viewModel.setFullPlayerVisible(false) },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.skipNext() },
                onPrevious = { viewModel.skipPrevious() },
                onSeek = { ms -> viewModel.seekTo(ms) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onSpeedChange = { spd -> viewModel.setSpeed(spd) },
                onOpenSleepTimer = { viewModel.setShowSleepTimerDialog(true) },
                onOpenQueue = { viewModel.setShowQueueSheet(true) },
                onFavoriteToggle = { track -> viewModel.toggleFavorite(track) },
                onQuickArtistShuffle = { singer -> viewModel.startArtistShuffle(singer) }
            )
        }

        // Sleep Timer Dialog
        if (showSleepTimerDialog) {
            SleepTimerDialog(
                currentMinutesLeft = playerState.sleepTimerMinutesLeft,
                onSelectMinutes = { mins -> viewModel.setSleepTimer(mins) },
                onDismiss = { viewModel.setShowSleepTimerDialog(false) }
            )
        }

        // Queue Bottom Sheet
        if (showQueueSheet) {
            QueueBottomSheet(
                playerState = playerState,
                onTrackSelect = { track -> viewModel.playTrack(track) },
                onFavoriteToggle = { track -> viewModel.toggleFavorite(track) },
                onDismiss = { viewModel.setShowQueueSheet(false) }
            )
        }
    }
}
