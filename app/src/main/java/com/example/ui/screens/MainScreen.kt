package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.FolderItem
import com.example.data.model.VideoItem
import com.example.data.model.VideoSortOrder
import com.example.ui.components.FolderCard
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.VideoItemRow
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentViolet
import com.example.ui.viewmodel.VideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: VideoViewModel,
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFolder: (FolderItem) -> Unit
) {
    val context = LocalContext.current
    val videos by viewModel.videos.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val favoriteFolders by viewModel.favoriteFolders.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val playerManager = viewModel.playerManager
    val currentPlayingVideo by playerManager.currentVideo.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPos by playerManager.currentPosition.collectAsState()
    val duration by playerManager.duration.collectAsState()
    val isBackgroundMode by playerManager.isBackgroundMode.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Runtime Permission for Reading Video Files
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
        if (isGranted) {
            viewModel.refreshVideos()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasStoragePermission) {
            permissionLauncher.launch(permission)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchExpanded) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Cari judul video...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.setSearchQuery("")
                                    isSearchExpanded = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Tutup Cari")
                                }
                            }
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(listOf(AccentViolet, AccentCyan))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleFilled,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Video Player",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Semua Format • Pemutar Bebas Gangguan",
                                    fontSize = 10.sp,
                                    color = AccentCyan
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (!isSearchExpanded) {
                        IconButton(onClick = { isSearchExpanded = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Cari Video")
                        }
                    }

                    // Quick Dark Mode Toggle
                    IconButton(onClick = { viewModel.toggleDarkMode(!settings.isDarkMode) }) {
                        Icon(
                            imageVector = if (settings.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Ganti Mode Gelap/Terang",
                            tint = if (settings.isDarkMode) AccentAmber else AccentViolet
                        )
                    }

                    // Refresh
                    IconButton(onClick = { viewModel.refreshVideos() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Segarkan")
                    }

                    // Settings
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (currentPlayingVideo != null) {
                MiniPlayerBar(
                    video = currentPlayingVideo!!,
                    isPlaying = isPlaying,
                    currentPosition = currentPos,
                    duration = duration,
                    isBackgroundMode = isBackgroundMode,
                    onExpand = onOpenPlayer,
                    onPlayPause = { playerManager.togglePlayPause() },
                    onNext = { playerManager.playNext() },
                    onClose = { playerManager.stopAndClear() }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission Banner (if not granted)
            if (!hasStoragePermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentViolet.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Izin Penyimpanan Diperlukan",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Izinkan akses untuk memutar semua video dari perangkat Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { permissionLauncher.launch(permission) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentViolet)
                        ) {
                            Text("Izinkan", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Tabs: 1. Folder Favorit, 2. Semua Folder, 3. Semua Video
            val tabTitles = listOf("Folder Favorit", "Semua Folder", "Semua Video")
            val tabIcons = listOf(Icons.Default.Star, Icons.Default.Folder, Icons.Default.VideoLibrary)

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = AccentViolet,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AccentViolet
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = tabIcons[index],
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == index) {
                                        if (index == 0) AccentAmber else AccentViolet
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                }
            }

            // Automatic Sorting Bar (Date or Size)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )

                FilterChip(
                    selected = sortOrder == VideoSortOrder.DATE_DESC,
                    onClick = { viewModel.setSortOrder(VideoSortOrder.DATE_DESC) },
                    label = { Text("Tanggal (Terbaru)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentViolet.copy(alpha = 0.2f),
                        selectedLabelColor = AccentViolet
                    )
                )

                FilterChip(
                    selected = sortOrder == VideoSortOrder.SIZE_DESC,
                    onClick = { viewModel.setSortOrder(VideoSortOrder.SIZE_DESC) },
                    label = { Text("Ukuran (Terbesar)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentViolet.copy(alpha = 0.2f),
                        selectedLabelColor = AccentViolet
                    )
                )

                FilterChip(
                    selected = sortOrder == VideoSortOrder.NAME_ASC,
                    onClick = { viewModel.setSortOrder(VideoSortOrder.NAME_ASC) },
                    label = { Text("Nama (A-Z)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentViolet.copy(alpha = 0.2f),
                        selectedLabelColor = AccentViolet
                    )
                )
            }

            // Tab Content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentViolet)
                }
            } else {
                when (selectedTab) {
                    0 -> FavoriteFoldersTab(
                        favorites = favoriteFolders,
                        onFolderClick = { folder ->
                            viewModel.selectFolder(folder)
                            onOpenFolder(folder)
                        },
                        onToggleFavorite = { folder -> viewModel.toggleFavoriteFolder(folder) },
                        onExcludeFolder = { folder -> viewModel.excludeFolder(folder) },
                        onPlayAll = { folder ->
                            viewModel.playAllInFolder(folder)
                            onOpenPlayer()
                        }
                    )
                    1 -> AllFoldersTab(
                        folders = folders,
                        onFolderClick = { folder ->
                            viewModel.selectFolder(folder)
                            onOpenFolder(folder)
                        },
                        onToggleFavorite = { folder -> viewModel.toggleFavoriteFolder(folder) },
                        onExcludeFolder = { folder -> viewModel.excludeFolder(folder) },
                        onPlayAll = { folder ->
                            viewModel.playAllInFolder(folder)
                            onOpenPlayer()
                        }
                    )
                    2 -> AllVideosTab(
                        videos = videos,
                        onPlayVideo = { video ->
                            viewModel.playVideo(video, videos)
                            onOpenPlayer()
                        },
                        onPlayBackground = { video ->
                            viewModel.toggleBackgroundPlayback(true)
                            viewModel.playVideo(video, videos)
                        },
                        onRemoveFromList = { video -> viewModel.hideVideoFromList(video) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteFoldersTab(
    favorites: List<FolderItem>,
    onFolderClick: (FolderItem) -> Unit,
    onToggleFavorite: (FolderItem) -> Unit,
    onExcludeFolder: (FolderItem) -> Unit,
    onPlayAll: (FolderItem) -> Unit
) {
    if (favorites.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AccentAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Belum Ada Folder Favorit",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ketuk ikon bintang pada folder mana pun di tab 'Semua Folder' untuk menambahkannya ke daftar favorit agar mudah dan cepat diakses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(favorites, key = { it.path }) { folder ->
                FolderCard(
                    folder = folder,
                    onClick = { onFolderClick(folder) },
                    onToggleFavorite = { onToggleFavorite(folder) },
                    onExcludeFolder = { onExcludeFolder(folder) },
                    onPlayAll = { onPlayAll(folder) }
                )
            }
        }
    }
}

@Composable
private fun AllFoldersTab(
    folders: List<FolderItem>,
    onFolderClick: (FolderItem) -> Unit,
    onToggleFavorite: (FolderItem) -> Unit,
    onExcludeFolder: (FolderItem) -> Unit,
    onPlayAll: (FolderItem) -> Unit
) {
    if (folders.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tidak ada folder video ditemukan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(folders, key = { it.path }) { folder ->
                FolderCard(
                    folder = folder,
                    onClick = { onFolderClick(folder) },
                    onToggleFavorite = { onToggleFavorite(folder) },
                    onExcludeFolder = { onExcludeFolder(folder) },
                    onPlayAll = { onPlayAll(folder) }
                )
            }
        }
    }
}

@Composable
private fun AllVideosTab(
    videos: List<VideoItem>,
    onPlayVideo: (VideoItem) -> Unit,
    onPlayBackground: (VideoItem) -> Unit,
    onRemoveFromList: (VideoItem) -> Unit
) {
    if (videos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tidak ada video yang ditemukan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(videos, key = { it.id }) { video ->
                VideoItemRow(
                    video = video,
                    onClick = { onPlayVideo(video) },
                    onPlayBackground = { onPlayBackground(video) },
                    onRemoveFromList = { onRemoveFromList(video) }
                )
            }
        }
    }
}
