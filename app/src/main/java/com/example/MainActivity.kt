package com.example

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.model.FolderItem
import com.example.data.model.VideoItem
import com.example.service.PlaybackService
import com.example.ui.screens.FolderDetailScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.VideoPlayerTheme
import com.example.ui.viewmodel.VideoViewModel

enum class AppScreen {
    MAIN,
    FOLDER_DETAIL,
    PLAYER,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: VideoViewModel by viewModels()
    private var isInPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsState()
            var currentScreen by remember { mutableStateOf(AppScreen.MAIN) }
            val selectedFolder by viewModel.selectedFolder.collectAsState()

            VideoPlayerTheme(darkTheme = settings.isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isInPipMode) {
                        PlayerScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = AppScreen.MAIN },
                            onEnterPip = { enterPip() }
                        )
                    } else {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "ScreenTransition"
                        ) { screen ->
                            when (screen) {
                                AppScreen.MAIN -> {
                                    MainScreen(
                                        viewModel = viewModel,
                                        onOpenPlayer = { currentScreen = AppScreen.PLAYER },
                                        onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                                        onOpenFolder = { folder ->
                                            viewModel.selectFolder(folder)
                                            currentScreen = AppScreen.FOLDER_DETAIL
                                        }
                                    )
                                }

                                AppScreen.FOLDER_DETAIL -> {
                                    BackHandler {
                                        viewModel.selectFolder(null)
                                        currentScreen = AppScreen.MAIN
                                    }
                                    if (selectedFolder != null) {
                                        FolderDetailScreen(
                                            folder = selectedFolder!!,
                                            viewModel = viewModel,
                                            onBack = {
                                                viewModel.selectFolder(null)
                                                currentScreen = AppScreen.MAIN
                                            },
                                            onPlayVideo = { video, list ->
                                                viewModel.playVideo(video, list)
                                                currentScreen = AppScreen.PLAYER
                                            }
                                        )
                                    } else {
                                        LaunchedEffect(Unit) { currentScreen = AppScreen.MAIN }
                                    }
                                }

                                AppScreen.PLAYER -> {
                                    BackHandler {
                                        currentScreen = AppScreen.MAIN
                                    }
                                    PlayerScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = AppScreen.MAIN },
                                        onEnterPip = { enterPip() }
                                    )
                                }

                                AppScreen.SETTINGS -> {
                                    BackHandler {
                                        currentScreen = AppScreen.MAIN
                                    }
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = AppScreen.MAIN }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            if (uri != null) {
                val video = VideoItem(
                    id = System.currentTimeMillis(),
                    uri = uri,
                    path = uri.path ?: "",
                    title = uri.lastPathSegment ?: "Video Eksternal",
                    displayName = uri.lastPathSegment ?: "Video",
                    duration = 0L,
                    size = 0L,
                    dateModified = System.currentTimeMillis() / 1000,
                    bucketId = "external",
                    bucketDisplayName = "Video Luar",
                    folderPath = "external"
                )
                viewModel.playVideo(video)
            }
        }
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (viewModel.playerManager.isPlaying.value) {
            if (viewModel.settings.value.backgroundPlaybackEnabled) {
                PlaybackService.startService(this)
            }
        }
    }
}
