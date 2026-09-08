package com.aether.player.ui.nav

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.aether.player.ui.diagnostics.DiagnosticsScreen
import com.aether.player.ui.downloads.DownloadsScreen
import com.aether.player.ui.folders.FoldersScreen
import com.aether.player.ui.history.HistoryScreen
import com.aether.player.ui.home.HomeScreen
import com.aether.player.ui.library.LibraryViewModel
import com.aether.player.ui.library.VideosScreen
import com.aether.player.ui.player.PlayerScreen
import com.aether.player.ui.playlists.PlaylistDetailScreen
import com.aether.player.ui.playlists.PlaylistsScreen
import com.aether.player.ui.search.SearchScreen
import com.aether.player.ui.settings.SettingsDetailScreen
import com.aether.player.ui.settings.SettingsScreen
import com.aether.player.ui.theme.LocalAnimScale
import com.aether.player.ui.theme.animDur
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
        Tab("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
    )
    val animScale = LocalAnimScale.current
    val inPlayer = route.startsWith("player")
    val hideNav = inPlayer || route.startsWith("search") || route.startsWith("url") ||
        route.startsWith("playlist/") || route.startsWith("history") ||
        route.startsWith("downloads") || route.startsWith("diagnostics") ||
        route.startsWith("settings/")

    LaunchedEffect(inPlayer, playerState.locked) {
        onImmersive(inPlayer)
        container.preferences.settings.collect { settings ->
            val videoId = container.playerManager.state.value.current?.id
            val saved = if (inPlayer && settings.rememberOrientation && videoId != null) {
                container.preferences.getString("ori_$videoId")
            } else {
                null
            }
            val mode = saved ?: settings.orientation
            activity?.requestedOrientation = when {
                !inPlayer -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                mode == "portrait" || mode == "sensor_portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                mode == "landscape" || mode == "sensor_landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                mode == "sensor" -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
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
                        enter = fadeIn(tween(animDur(220, animScale))) +
                            slideInVertically(tween(animDur(300, animScale))) { it },
                        exit = fadeOut(tween(animDur(180, animScale))) +
                            slideOutVertically(tween(animDur(240, animScale))) { it },
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            val pill = RoundedCornerShape(30.dp)
                            Row(
                                modifier = Modifier
                                    .clip(pill)
                                    .background(Color(0xFF232329))
                                    .border(1.dp, Color.White.copy(alpha = 0.10f), pill)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val current = tabs.find { route.startsWith(it.route) }?.route ?: "home"
                                tabs.forEach { tab ->
                                    TabPill(
                                        tab = tab,
                                        selected = current == tab.route,
                                        onClick = {
                                            nav.navigate(tab.route) {
                                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    NavHost(
                        navController = nav,
                        startDestination = "home",
                        enterTransition = {
                            fadeIn(tween(animDur(220, animScale))) +
                                scaleIn(
                                    initialScale = 0.98f,
                                    animationSpec = tween(animDur(220, animScale)),
                                )
                        },
                        exitTransition = {
                            fadeOut(tween(animDur(170, animScale))) +
                                scaleOut(
                                    targetScale = 0.98f,
                                    animationSpec = tween(animDur(170, animScale)),
                                )
                        },
                        popEnterTransition = { fadeIn(tween(animDur(220, animScale))) },
                        popExitTransition = {
                            fadeOut(tween(animDur(170, animScale))) +
                                scaleOut(
                                    targetScale = 0.98f,
                                    animationSpec = tween(animDur(170, animScale)),
                                )
                        },
                    ) {
                        composable("home") {
                            HomeScreen(
                                vm = libraryViewModel,
                                onOpenVideo = { nav.navigate("player") },
                                onOpenSearch = { nav.navigate("search") },
                                onOpenUrl = { nav.navigate("url") },
                                onOpenFolder = { nav.navigate("folders") },
                                onOpenPlaylists = { nav.navigate("playlists") },
                                onOpenSettings = { nav.navigate("settings") },
                                onOpenHistory = { nav.navigate("history") },
                                onOpenDownloads = { nav.navigate("downloads") },
                            )
                        }
                        composable("videos") {
                            VideosScreen(
                                vm = libraryViewModel,
                                onOpenVideo = { nav.navigate("player") },
                                onOpenSearch = { nav.navigate("search") },
                                onShowInFolder = { folderId -> nav.navigate("folders?focus=$folderId") },
                            )
                        }
                        composable(
                            "folders?focus={focus}",
                            arguments = listOf(
                                navArgument("focus") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                            ),
                        ) { entry ->
                            FoldersScreen(
                                vm = libraryViewModel,
                                onOpenVideo = { nav.navigate("player") },
                                focusId = entry.arguments?.getString("focus"),
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
                        composable("settings") {
                            SettingsScreen(onOpenCategory = { nav.navigate("settings/$it") })
                        }
                        composable(
                            "settings/{category}",
                            arguments = listOf(navArgument("category") { type = NavType.StringType }),
                        ) { entry ->
                            val category = entry.arguments?.getString("category") ?: return@composable
                            SettingsDetailScreen(
                                category = category,
                                onBack = { nav.popBackStack() },
                                onOpenDiagnostics = { nav.navigate("diagnostics") },
                            )
                        }
                        composable("history") {
                            HistoryScreen(
                                vm = libraryViewModel,
                                onBack = { nav.popBackStack() },
                                onOpenVideo = { nav.navigate("player") },
                            )
                        }
                        composable("downloads") {
                            DownloadsScreen(
                                vm = libraryViewModel,
                                onBack = { nav.popBackStack() },
                            )
                        }
                        composable("diagnostics") {
                            DiagnosticsScreen(
                                vm = libraryViewModel,
                                onBack = { nav.popBackStack() },
                            )
                        }
                        composable("search") {
                            SearchScreen(
                                vm = libraryViewModel,
                                onBack = { nav.popBackStack() },
                                onOpenVideo = { nav.navigate("player") },
                                onOpenFolder = { folderId -> nav.navigate("folders?focus=$folderId") },
                                onOpenPlaylist = { id -> nav.navigate("playlist/$id") },
                            )
                        }
                        composable("url") {
                            OpenUrlScreen(
                                vm = libraryViewModel,
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
                        enter = slideInVertically(tween(animDur(300, animScale))) { it } +
                            fadeIn(tween(animDur(220, animScale))),
                        exit = slideOutVertically(tween(animDur(240, animScale))) { it } +
                            fadeOut(tween(animDur(180, animScale))),
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

@Composable
private fun TabPill(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val animScale = LocalAnimScale.current
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(animDur(250, animScale)),
        label = "tabBg",
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
        animationSpec = tween(animDur(250, animScale)),
        label = "tabFg",
    )
    val iconScale by animateFloatAsState(
        if (selected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "tabIcon",
    )
    val itemShape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .clip(itemShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (selected) tab.selected else tab.icon,
            contentDescription = tab.label,
            tint = fg,
            modifier = Modifier.size(22.dp).scale(iconScale),
        )
        Spacer(Modifier.width(8.dp))
        Text(tab.label, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

