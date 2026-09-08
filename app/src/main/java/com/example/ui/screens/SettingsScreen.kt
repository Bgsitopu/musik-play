package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EqualizerBottomSheet
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentViolet
import com.example.ui.viewmodel.VideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VideoViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val excludedFolders by viewModel.excludedFolders.collectAsState()
    val allowedFolders by viewModel.allowedFolders.collectAsState()
    val hiddenCount by viewModel.hiddenCount.collectAsState()
    val folders by viewModel.folders.collectAsState()

    val playerManager = viewModel.playerManager
    val eqEnabled by playerManager.equalizerEnabled.collectAsState()
    val bassStrength by playerManager.bassStrength.collectAsState()
    val trebleStrength by playerManager.trebleStrength.collectAsState()
    val eqBands by playerManager.equalizerBands.collectAsState()
    val currentPreset by playerManager.currentPreset.collectAsState()

    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showAddExcludeDialog by remember { mutableStateOf(false) }
    var showAllowedFoldersDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan & Privasi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Tampilan & Kenyamanan (Dark Mode)
            SettingsSectionHeader(title = "Tampilan & Kenyamanan Mata")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                SettingsSwitchRow(
                    icon = Icons.Default.DarkMode,
                    iconTint = AccentViolet,
                    title = "Mode Gelap Elegan",
                    subtitle = "Warna hitam OLED yang nyaman di mata untuk menonton di malam hari",
                    checked = settings.isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pemutaran & Audio
            SettingsSectionHeader(title = "Pemutaran & Audio Bebas Hambatan")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    SettingsSwitchRow(
                        icon = Icons.Default.MusicNote,
                        iconTint = AccentCyan,
                        title = "Putar Bersamaan Aplikasi Lain",
                        subtitle = "Video terus berjalan lancar tanpa terhenti saat ada video atau audio dari aplikasi lain yang aktif",
                        checked = settings.ignoreAudioFocus,
                        onCheckedChange = { viewModel.toggleConcurrentPlayback(it) }
                    )

                    SettingsDivider()

                    SettingsSwitchRow(
                        icon = Icons.Default.Headphones,
                        iconTint = AccentViolet,
                        title = "Putar di Latar Belakang (Musik)",
                        subtitle = "Tetap memutar audio video saat aplikasi diminimalkan atau layar mati dengan notifikasi kontrol",
                        checked = settings.backgroundPlaybackEnabled,
                        onCheckedChange = { viewModel.toggleBackgroundPlayback(it) }
                    )

                    SettingsDivider()

                    SettingsActionRow(
                        icon = Icons.Default.GraphicEq,
                        iconTint = AccentRose,
                        title = "Equalizer, Bass & Treble",
                        subtitle = "Atur frekuensi suara, bass menggelegar, dan treble jernih",
                        onClick = { showEqualizerSheet = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Privasi & Scan Folder
            SettingsSectionHeader(title = "Privasi Folder & Kontrol Scan")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    // Excluded folders list / add
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentRose.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = AccentRose,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Folder yang Dikecualikan (Jangan Scan)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${excludedFolders.size} folder dikecualikan dari pemindaian privasi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { showAddExcludeDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Folder Dikecualikan",
                                tint = AccentRose
                            )
                        }
                    }

                    if (excludedFolders.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            excludedFolders.forEach { excluded ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = excluded.folderName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = excluded.folderPath,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeExcludedFolder(excluded.folderPath) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hapus Pengecualian",
                                            tint = AccentRose,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    SettingsDivider()

                    // Whitelist Mode: Select which folders are allowed to be shown
                    SettingsSwitchRow(
                        icon = Icons.Default.FilterList,
                        iconTint = AccentEmerald,
                        title = "Batasi Tampilan Folder (Whitelist)",
                        subtitle = if (settings.whitelistModeEnabled)
                            "Hanya menampilkan folder yang Anda izinkan secara khusus"
                        else
                            "Menampilkan semua folder (kecuali yang dikecualikan)",
                        checked = settings.whitelistModeEnabled,
                        onCheckedChange = { viewModel.toggleWhitelistMode(it) }
                    )

                    if (settings.whitelistModeEnabled) {
                        SettingsActionRow(
                            icon = Icons.Default.Security,
                            iconTint = AccentEmerald,
                            title = "Pilih Folder yang Boleh Ditampilkan",
                            subtitle = "${allowedFolders.size} folder diizinkan untuk tampil",
                            onClick = { showAllowedFoldersDialog = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Manajemen Daftar Video
            SettingsSectionHeader(title = "Daftar Video & Pemulihan")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                SettingsActionRow(
                    icon = Icons.Default.Restore,
                    iconTint = AccentCyan,
                    title = "Pulihkan Video yang Dihapus dari Daftar",
                    subtitle = if (hiddenCount > 0)
                        "$hiddenCount video telah disembunyikan dari daftar (file asli tetap aman)"
                    else
                        "Tidak ada video yang disembunyikan dari daftar",
                    onClick = {
                        if (hiddenCount > 0) {
                            showRestoreDialog = true
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // Dialog: Restore all hidden videos
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Pulihkan Semua Video?") },
            text = {
                Text("Semua video yang sebelumnya dihapus dari daftar akan ditampilkan kembali di aplikasi. File fisik pada perangkat tidak pernah terpengaruh.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        viewModel.restoreAllHiddenVideos()
                    }
                ) {
                    Text("Pulihkan", color = AccentCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog: Add excluded folder manually
    if (showAddExcludeDialog) {
        var newFolderName by remember { mutableStateOf("") }
        var newFolderPath by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddExcludeDialog = false },
            title = { Text("Kecualikan Folder dari Scan") },
            text = {
                Column {
                    Text("Pilih atau ketik nama folder yang tidak ingin dipindai oleh aplikasi:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Nama Folder") },
                        placeholder = { Text("misal: Rahasia / Pribadi") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFolderPath,
                        onValueChange = { newFolderPath = it },
                        label = { Text("Jalur Folder (Opsional)") },
                        placeholder = { Text("misal: /storage/emulated/0/...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            val path = if (newFolderPath.isNotBlank()) newFolderPath else newFolderName
                            viewModel.excludeFolder(
                                com.example.data.model.FolderItem(
                                    bucketId = newFolderName,
                                    name = newFolderName,
                                    path = path,
                                    videoCount = 0,
                                    totalSize = 0L,
                                    latestDate = 0L,
                                    firstVideoUri = null
                                )
                            )
                            showAddExcludeDialog = false
                        }
                    }
                ) {
                    Text("Kecualikan", color = AccentRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExcludeDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog: Whitelist folder selection
    if (showAllowedFoldersDialog) {
        val selectedPaths = remember {
            mutableStateListOf<String>().apply {
                addAll(allowedFolders.map { it.folderPath })
            }
        }

        AlertDialog(
            onDismissRequest = { showAllowedFoldersDialog = false },
            title = { Text("Pilih Folder yang Boleh Tampil") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(folders, key = { it.path }) { f ->
                        val isChecked = selectedPaths.contains(f.path)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedPaths.remove(f.path) else selectedPaths.add(f.path)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedPaths.add(f.path) else selectedPaths.remove(f.path)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = AccentEmerald)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(f.name, style = MaterialTheme.typography.bodyMedium)
                                Text("${f.videoCount} video", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pairs = folders.filter { selectedPaths.contains(it.path) }.map { Pair(it.path, it.name) }
                        viewModel.saveAllowedFolders(pairs)
                        showAllowedFoldersDialog = false
                    }
                ) {
                    Text("Simpan", color = AccentEmerald, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAllowedFoldersDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Equalizer Sheet
    if (showEqualizerSheet) {
        EqualizerBottomSheet(
            enabled = eqEnabled,
            onToggleEnabled = { viewModel.toggleEqualizer(it) },
            bassStrength = bassStrength,
            onBassChange = { viewModel.setBassStrength(it) },
            trebleStrength = trebleStrength,
            onTrebleChange = { viewModel.setTrebleStrength(it) },
            bands = eqBands,
            onBandChange = { band, level -> viewModel.setBandLevel(band, level) },
            currentPreset = currentPreset,
            onPresetSelect = { viewModel.applyEqPreset(it) },
            onDismiss = { showEqualizerSheet = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = AccentViolet,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iconTint
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    )
}
