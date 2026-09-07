package com.aether.player.ui.nav

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aether.player.AppContainer
import com.aether.player.ui.components.AetherBackground
import com.aether.player.ui.components.MiniPlayerBar
import com.aether.player.ui.components.PermissionGate
import com.aether.player.ui.folders.FoldersScreen
import com.aether.player.ui.home.HomeScreen
import com.aether.player.ui.library.LibraryViewModel
import com.aether.player.ui.library.VideosScreen
import com.aether.player.ui.player.PlayerScreen
import com.aether.player.ui.playlists.PlaylistDetailScreen
import com.aether.player.ui.playlists.PlaylistsScreen
import com.aether.player.ui.search.SearchScreen
import com.aether.player.ui.settings.SettingsScreen
import com.aether.player.ui.url.OpenUrlScreen

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selected: ImageVector,
)

@Composable
fun AetherRoot(
    container: AppContainer,
    onEnterPip: () -> Unit,
    onImmersive: (Boolean) -> Unit,
    libraryViewModel: LibraryViewModel = viewModel(),
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val playerState by container.playerManager.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    var hasPermission by remember { mutableStateOf(PermissionGate.hasVideoPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        hasPermission = granted.values.any { it } || PermissionGate.hasVideoPermission(context)
        if (hasPermission) libraryViewModel.scan()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) libraryViewModel.scan()
    }

    val tabs = listOf(
        Tab("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
        Tab("videos", "Videos", Icons.Outlined.VideoLibrary, Icons.Filled.VideoLibrary),
        Tab("folders", "Folders", Icons.Outlined.Folder, Icons.Filled.Folder),
        Tab("playlists", "Playlists", Icons.Outlined.PlaylistPlay, Icons.Filled.PlaylistPlay),
        Tab("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
    )
    val inPlayer = route.startsWith("player")
    val hideNav = inPlayer || route.startsWith("search") || route.startsWith("url") || route.startsWith("playlist/")

    LaunchedEffect(inPlayer, playerState.locked) {
        onImmersive(inPlayer)
        val requested = container.preferences
        requested.settings.collect { settings ->
            activity?.requestedOrientation = when {
                !inPlayer -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                settings.orientation == "portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                settings.orientation == "landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                settings.orientation == "sensor" -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AetherBackground(Modifier.fillMaxSize())
        if (!hasPermission) {
            PermissionGate(
                onGrant = {
                    val perms = buildList {
                        if (Build.VERSION.SDK_INT >= 33) {
                            add(Manifest.permission.READ_MEDIA_VIDEO)
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }.toTypedArray()
                    permissionLauncher.launch(perms)
                },
            )
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    AnimatedVisibility(
                        visible = !hideNav,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                    ) {
                        NavigationBar(
                            containerColor = Color.Black.copy(alpha = 0.35f),
                            contentColor = Color.White,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                        ) {
                            val current = tabs.find { route.startsWith(it.route) }?.route ?: "home"
                            tabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = current == tab.route,
                                    onClick = {
                                        nav.navigate(tab.route) {
                                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        androidx.compose.material3.Icon(
                                            if (current == tab.route) tab.selected else tab.icon,
                                            contentDescription = tab.label,
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.White.copy(alpha = 0.12f),
                                    ),
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    NavHost(navController = nav, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                vm = libraryViewModel,
                                onOpenVideo = { nav.navigate("player") },
                                onOpenSearch = { nav.navigate("search") },
                                onOpenUrl = { nav.navigate("url") },
                                onOpenFolder = { nav.navigate("folders") },
                                onOpenPlaylists = { nav.navigate("playlists") },
                                onOpenSettings = { nav.navigate("settings") },
                            )
                        }
                        composable("videos") {
                            VideosScreen(
                                vm = libraryViewModel,
                                onOpenVideo = { nav.navigate("player") },
                                onOpenSearch = { nav.navigate("search") },
                            )
                        }
                        composable("folders") {
                            FoldersScreen(
                                vm = libraryViewModel,
                                onOpenVideo = { nav.navigate("player") },
                            )
                        }
                        composable("playlists") {
                            PlaylistsScreen(
                                vm = libraryViewModel,
                                onOpen = { id -> nav.navigate("playlist/$id") },
                            )
                        }
                        composable(
                            "playlist/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType }),
                        ) { entry ->
                            val id = entry.arguments?.getLong("id") ?: return@composable
                            PlaylistDetailScreen(
                                playlistId = id,
                                vm = libraryViewModel,
                                onBack = { nav.popBackStack() },
                                onOpenVideo = { nav.navigate("player") },
                            )
                        }
                        composable("settings") { SettingsScreen() }
                        composable("search") {
                            SearchScreen(
                                vm = libraryViewModel,
                                onBack = { nav.popBackStack() },
                                onOpenVideo = { nav.navigate("player") },
                            )
                        }
                        composable("url") {
                            OpenUrlScreen(
                                onBack = { nav.popBackStack() },
                                onPlay = { nav.navigate("player") },
                            )
                        }
                        composable("player") {
                            PlayerScreen(
                                onBack = {
                                    container.playerManager.setMiniPlayer(true)
                                    nav.popBackStack()
                                },
                                onPip = onEnterPip,
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = !inPlayer && playerState.current != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        MiniPlayerBar(
                            state = playerState,
                            onExpand = { nav.navigate("player") },
                            onPlayPause = { container.playerManager.playPause() },
                            onClose = { container.playerManager.stopAndClear() },
                            onNext = { container.playerManager.next() },
                        )
                    }
                }
            }
        }
    }

    BackHandler(enabled = inPlayer && playerState.locked) { /* swallow while locked */ }
}
