package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderItem
import com.example.data.model.VideoItem
import com.example.data.model.VideoSortOrder
import com.example.ui.components.VideoItemRow
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentViolet
import com.example.ui.viewmodel.VideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folder: FolderItem,
    viewModel: VideoViewModel,
    onBack: () -> Unit,
    onPlayVideo: (VideoItem, List<VideoItem>) -> Unit
) {
    val videos by viewModel.videos.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    var showExcludeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "${videos.size} video • ${folder.formattedTotalSize}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    // Favorite Toggle
                    IconButton(onClick = { viewModel.toggleFavoriteFolder(folder) }) {
                        Icon(
                            imageVector = if (folder.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorit",
                            tint = if (folder.isFavorite) AccentAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Privacy: Exclude folder from scan
                    IconButton(onClick = { showExcludeDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Jangan Scan Folder Ini (Privasi)",
                            tint = AccentRose
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Play All Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Koleksi Folder",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentCyan
                        )
                        Text(
                            text = "${videos.size} Video Tersedia",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = AccentViolet,
                        modifier = Modifier.clip(RoundedCornerShape(24.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .background(Brush.horizontalGradient(listOf(AccentViolet, AccentCyan)))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.playAllInFolder(folder) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Putar Semua",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Automatic Sorting Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
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
                    selected = sortOrder == VideoSortOrder.DATE_ASC,
                    onClick = { viewModel.setSortOrder(VideoSortOrder.DATE_ASC) },
                    label = { Text("Tanggal (Terlama)") },
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
                    selected = sortOrder == VideoSortOrder.SIZE_ASC,
                    onClick = { viewModel.setSortOrder(VideoSortOrder.SIZE_ASC) },
                    label = { Text("Ukuran (Terkecil)") },
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

            // Video List
            if (videos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak ada video di folder ini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                            onClick = { onPlayVideo(video, videos) },
                            onPlayBackground = {
                                viewModel.toggleBackgroundPlayback(true)
                                onPlayVideo(video, videos)
                            },
                            onRemoveFromList = { viewModel.hideVideoFromList(video) }
                        )
                    }
                }
            }
        }
    }

    if (showExcludeDialog) {
        AlertDialog(
            onDismissRequest = { showExcludeDialog = false },
            title = { Text("Jangan Scan Folder Ini?") },
            text = {
                Text("Folder '${folder.name}' tidak akan dipindai lagi dalam aplikasi demi menjaga privasi konten Anda. Video di dalamnya akan disembunyikan dari aplikasi.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExcludeDialog = false
                        viewModel.excludeFolder(folder)
                        onBack()
                    }
                ) {
                    Text("Jangan Scan", color = AccentRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExcludeDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
