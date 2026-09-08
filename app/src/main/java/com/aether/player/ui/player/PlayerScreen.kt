package com.aether.player.ui.player

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.mediarouter.app.MediaRouteButton
import com.aether.player.AetherApp
import com.aether.player.data.prefs.AspectMode
import com.aether.player.data.prefs.TimelineStyle
import com.aether.player.domain.GestureMath
import com.aether.player.domain.RepeatMode
import com.aether.player.domain.TimeFormat
import com.aether.player.domain.WatchProgressLogic
import com.aether.player.playback.CaptureResult
import com.aether.player.playback.PlayerManager
import com.aether.player.ui.components.GlassButton
import com.aether.player.ui.components.GlassIconButton
import com.aether.player.ui.theme.LocalAnimScale
import com.aether.player.ui.theme.LocalGlass
import com.aether.player.ui.theme.animDur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SPEEDS = listOf(1f, 1.25f, 1.5f, 1.75f, 2f, 0.5f, 0.75f)

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onPip: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as AetherApp
    val pm = app.container.playerManager
    val prefs = app.container.preferences
    val state by pm.state.collectAsState()
    val overlay by pm.overlay.collectAsState()
    val settings by app.container.preferences.settings.collectAsState(
        initial = com.aether.player.data.prefs.AetherSettings(),
    )
    val haptics = LocalHapticFeedback.current
    val audio = remember { context.getSystemService(AudioManager::class.java) }
    val activity = context as? Activity
    val animScale = LocalAnimScale.current
    val glass = LocalGlass.current
    var controls by remember { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }
    var audioSheet by remember { mutableStateOf(false) }
    var textSheet by remember { mutableStateOf(false) }
    var infoSheet by remember { mutableStateOf(false) }
    var chaptersSheet by remember { mutableStateOf(false) }
    var bookmarksSheet by remember { mutableStateOf(false) }
    var aiSheet by remember { mutableStateOf(false) }
    var speedSheet by remember { mutableStateOf(false) }
    var sleepSheet by remember { mutableStateOf(false) }
    var eqSheet by remember { mutableStateOf(false) }
    var queueSheet by remember { mutableStateOf(false) }
    var playlistSheet by remember { mutableStateOf(false) }
    var statsOverlay by remember { mutableStateOf(false) }
    var resumeAsk by remember { mutableStateOf(false) }
    var showHints by remember { mutableStateOf(false) }
    var boostOn by remember { mutableStateOf(false) }
    var favOn by remember(state.current?.id) { mutableStateOf(state.current?.isFavorite == true) }
    var brightness by remember { mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f) }
    var volumeAcc by remember { mutableFloatStateOf(0f) }
    var playerHost by remember { mutableStateOf<PlayerView?>(null) }
    val boostActive = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pm.loadExternalSubtitle(uri)
    }

    LaunchedEffect(Unit) {
        val seen = withContext(Dispatchers.IO) { prefs.getString("hints_seen") }
        showHints = seen != "1"
    }
    LaunchedEffect(state.current?.id) {
        val v = state.current ?: return@LaunchedEffect
        resumeAsk = settings.resumePlayback &&
            WatchProgressLogic.shouldOfferResume(v.lastPositionMs, v.durationMs, settings.completionThreshold) &&
            state.positionMs < 2_000L
        pm.setAspect(settings.defaultAspect)
        if (settings.keepScreenAwake) activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            pm.persistProgress(increment = true)
        }
    }
    LaunchedEffect(controls, state.playing, state.locked) {
        if (controls && state.playing && !state.locked) {
            delay(settings.controlsTimeoutMs.toLong().coerceAtLeast(1200L))
            controls = false
        }
    }

    fun toggleRotate() {
        activity?.let {
            val next = if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            it.requestedOrientation = next
            val id = state.current?.id
            if (id != null) {
                scope.launch {
                    prefs.putString(
                        "ori_$id",
                        if (next == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) "sensor_landscape" else "sensor_portrait",
                    )
                }
            }
        }
    }

    fun cycleSpeed() {
        val next = SPEEDS.firstOrNull { it > state.speed + 0.01f } ?: SPEEDS.first()
        pm.setSpeed(next)
    }

    fun cycleAspect() {
        val modes = AspectMode.entries
        val next = modes[(modes.indexOf(state.aspect) + 1) % modes.size]
        pm.setAspect(next)
        pm.flash(next.name.lowercase().replace('_', ' '))
    }

    fun cycleRepeat() {
        val next = when (state.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.OFF
        }
        pm.setRepeat(next)
        pm.flash("Repeat: ${next.name.lowercase()}")
    }

    fun cycleAb() {
        val loop = state.abLoop
        if (loop == null) pm.setAbPoint(true)
        else if (loop.second == Long.MAX_VALUE) pm.setAbPoint(false)
        else {
            pm.clearAbLoop()
            pm.flash("A-B cleared")
        }
    }

    fun capture() {
        scope.launch {
            when (val result = pm.captureFrame(playerHost)) {
                is CaptureResult.Saved -> pm.flash("Frame saved to Pictures/Aether")
                is CaptureResult.Unavailable -> pm.flash(result.reason)
            }
        }
    }

    fun shareCurrent() {
        val video = state.current ?: return
        runCatching {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = video.mimeType ?: "video/*"
                putExtra(Intent.EXTRA_STREAM, Uri.parse(video.uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(share, "Share video"))
        }
    }

    fun toggleFav() {
        val video = state.current ?: return
        favOn = !favOn
        scope.launch { app.container.library.toggleFavorite(video.id) }
        pm.flash(if (favOn) "Added to favorites" else "Removed from favorites")
    }

    fun toggleCc() {
        val tracks = state.textTracks
        if (tracks.none { it.selected }) {
            val first = tracks.firstOrNull()
            if (first != null) {
                pm.selectTextTrack(first)
                pm.flash("Subtitles on")
            } else {
                pm.flash("No subtitle tracks")
            }
        } else {
            pm.selectTextTrack(null)
            pm.flash("Subtitles off")
        }
    }

    fun toggleBoost() {
        val next = !pm.isVolumeBoosted()
        pm.setVolumeBoost(next)
        boostOn = next
    }

    BackHandler {
        if (menuOpen) {
            menuOpen = false
        } else if (statsOverlay) {
            statsOverlay = false
        } else if (state.locked) {
            pm.setLocked(false)
        } else {
            pm.setMiniPlayer(true)
            onBack()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(settings, state.locked) {
                if (state.locked) {
                    detectTapGestures(onDoubleTap = { pm.setLocked(false) })
                    return@pointerInput
                }
                detectTapGestures(
                    onTap = { controls = !controls },
                    onDoubleTap = { offset ->
                        if (!settings.gestureDoubleTap) return@detectTapGestures
                        val amount = GestureMath.doubleTapSeekMs(settings.doubleTapIntervalSec)
                        if (GestureMath.isLeftSide(offset.x, size.width.toFloat())) {
                            pm.seekBy(-amount)
                            pm.flash("−${settings.doubleTapIntervalSec}s")
                        } else {
                            pm.seekBy(amount)
                            pm.flash("+${settings.doubleTapIntervalSec}s")
                        }
                        if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onLongPress = {
                        if (settings.gestureLongPressSpeed) {
                            boostActive.value = true
                            pm.beginSpeedBoost()
                        }
                    },
                    onPress = {
                        try {
                            awaitRelease()
                        } finally {
                            if (boostActive.value) {
                                boostActive.value = false
                                pm.endSpeedBoost()
                            }
                        }
                    },
                )
            }
            .pointerInput(settings, state.locked) {
                if (state.locked) return@pointerInput
                var totalX = 0f
                var totalY = 0f
                var minimized = false
                detectDragGestures(
                    onDragStart = {
                        totalX = 0f
                        totalY = 0f
                        minimized = false
                        volumeAcc = 0f
                    },
                    onDragEnd = {
                        if (!minimized && settings.gestureSwipeDownMini &&
                            totalY > 260f && totalY > 2f * kotlin.math.abs(totalX)
                        ) {
                            minimized = true
                            pm.setMiniPlayer(true)
                            onBack()
                        } else if (totalY < -180f && -totalY > 2f * kotlin.math.abs(totalX)) {
                            controls = true
                        }
                    },
                    onDragCancel = { },
                    onDrag = { change, drag ->
                        change.consume()
                        totalX += drag.x
                        totalY += drag.y
                        if (!minimized && settings.gestureSwipeDownMini &&
                            totalY > 320f && kotlin.math.abs(totalX) < 140f
                        ) {
                            minimized = true
                            pm.setMiniPlayer(true)
                            onBack()
                            return@detectDragGestures
                        }
                        val x = change.position.x
                        val absX = kotlin.math.abs(drag.x)
                        val absY = kotlin.math.abs(drag.y)
                        when {
                            absX > absY && settings.gestureSeek -> {
                                val delta = GestureMath.seekDeltaMs(drag.x, size.width.toFloat(), state.durationMs, settings.seekSensitivity)
                                pm.seekBy(delta)
                            }
                            absY >= absX && GestureMath.isLeftSide(x, size.width.toFloat()) && settings.gestureBrightness -> {
                                val d = GestureMath.brightnessDelta(drag.y, size.height.toFloat(), settings.brightnessSensitivity)
                                brightness = GestureMath.clampUnit(brightness + d)
                                activity?.let { act ->
                                    val lp = act.window.attributes
                                    lp.screenBrightness = brightness
                                    act.window.attributes = lp
                                }
                                pm.flash("Brightness ${(brightness * 100).toInt()}%", brightness)
                            }
                            absY >= absX && !GestureMath.isLeftSide(x, size.width.toFloat()) && settings.gestureVolume -> {
                                val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                volumeAcc += GestureMath.volumeDeltaF(drag.y, size.height.toFloat(), max, settings.volumeSensitivity)
                                val steps = volumeAcc.toInt()
                                if (steps != 0) {
                                    volumeAcc -= steps.toFloat()
                                    val next = (audio.getStreamVolume(AudioManager.STREAM_MUSIC) + steps).coerceIn(0, max)
                                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                                }
                                val cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val pct = if (max == 0) 0 else cur * 100 / max
                                pm.flash("Volume $pct%", pct / 100f)
                            }
                        }
                    },
                )
            }
            .pointerInput(settings.gesturePinch, state.zoom) {
                if (!settings.gesturePinch) return@pointerInput
                detectTransformGestures { _, _, zoom, _ ->
                    pm.setZoom(GestureMath.zoomFactor(state.zoom, zoom))
                }
            },
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = state.zoom
                    scaleY = state.zoom
                },
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = pm.player
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    subtitleView?.setStyle(
                        CaptionStyleCompat(
                            android.graphics.Color.WHITE,
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                            android.graphics.Color.BLACK,
                            null,
                        ),
                    )
                    playerHost = this
                }
            },
            update = { view ->
                view.player = pm.player
                view.resizeMode = when (state.aspect) {
                    AspectMode.FIT, AspectMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectMode.FILL, AspectMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectMode.RATIO_16_9, AspectMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
                view.subtitleView?.setFractionalTextSize(0.04f * settings.subtitleSize)
                view.subtitleView?.translationY = -settings.subtitlePosition * 180f
            },
        )

        overlay?.let { hint ->
            Box(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(hint.label, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    hint.progress?.let {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = it,
                            modifier = Modifier.width(140.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )
                    }
                }
            }
        }

        if (state.buffering) {
            androidx.compose.material3.CircularProgressIndicator(
                Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        state.error?.let { err ->
            Column(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC12080C))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(err.title, color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(err.detail, color = Color(0xFFFFC9D1), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Code ${err.code}",
                    color = Color(0xFFFFC9D1).copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton("Retry", filled = true, onClick = { pm.retry() })
                    GlassButton("Copy") {
                        val cm = context.getSystemService(ClipboardManager::class.java)
                        cm.setPrimaryClip(
                            ClipData.newPlainText(
                                "Aether error",
                                "${err.title}: ${err.detail} (code ${err.code})",
                            ),
                        )
                        pm.flash("Error copied")
                    }
                    state.current?.let { video ->
                        GlassButton("Open with") {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(video.uri), video.mimeType ?: "video/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching { context.startActivity(Intent.createChooser(intent, "Open with")) }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = controls && !state.locked,
            enter = fadeIn(tween(animDur(220, animScale))),
            exit = fadeOut(tween(animDur(180, animScale))),
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))),
                )
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)))),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIconButton(Icons.Outlined.ArrowBack, "Back", onClick = onBack)
                    Spacer(Modifier.width(10.dp))
                    GlassIconButton(Icons.Outlined.PlaylistPlay, "Chapters") { chaptersSheet = true }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        state.current?.title ?: "Aether",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                Column(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GlassIconButton(Icons.Outlined.Fullscreen, "Rotate") { toggleRotate() }
                    GlassIconButton(
                        if (state.muted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                        "Mute",
                    ) { pm.toggleMute() }
                    GlassIconButton(Icons.Outlined.PhotoCamera, "Capture frame") { capture() }
                    val castButton = remember {
                        runCatching { MediaRouteButton(context) }.getOrNull()
                    }
                    if (castButton != null) {
                        AndroidView(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(glass.surfaceStrong),
                            factory = { castButton },
                        )
                    }
                }

                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlassIconButton(Icons.Outlined.Audiotrack, "Audio") { audioSheet = true }
                        GlassIconButton(Icons.Outlined.ClosedCaption, "Subtitles") { textSheet = true }
                        GlassIconButton(Icons.Outlined.List, "Chapters") { chaptersSheet = true }
                        GlassIconButton(Icons.Outlined.Bookmark, "Bookmarks") { bookmarksSheet = true }
                        GlassIconButton(Icons.Outlined.Speed, "Speed (${state.speed}x)") { cycleSpeed() }
                        GlassIconButton(Icons.Outlined.Apps, "Menu") { menuOpen = true }
                    }
                    Spacer(Modifier.height(10.dp))
                    val duration = state.durationMs.coerceAtLeast(0L)
                    val pos = state.positionMs.coerceAtLeast(0L)
                    val trackScaleY = when (settings.timelineStyle) {
                        TimelineStyle.SLIM -> 0.6f
                        TimelineStyle.BOLD -> 1f
                        TimelineStyle.CHAPTER -> 1.15f
                    }
                    Box(Modifier.fillMaxWidth()) {
                        Slider(
                            value = if (duration <= 0L) 0f else pos / duration.toFloat(),
                            onValueChange = { frac -> pm.seekTo((frac * duration).toLong()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { scaleY = trackScaleY },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                            ),
                        )
                        if (settings.timelineStyle == TimelineStyle.CHAPTER) {
                            val fractions = remember(state.queue, duration) { pm.chapterFractions() }
                            if (fractions.isNotEmpty()) {
                                val density = LocalDensity.current
                                Canvas(Modifier.matchParentSize()) {
                                    val inset = with(density) { 12.dp.toPx() }
                                    val y = size.height / 2f
                                    fractions.forEach { frac ->
                                        val x = inset + frac * (size.width - inset * 2)
                                        drawLine(
                                            Color.White.copy(alpha = 0.6f),
                                            androidx.compose.ui.geometry.Offset(x, y - 9f),
                                            androidx.compose.ui.geometry.Offset(x, y + 9f),
                                            strokeWidth = 3f,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { scope.launch { prefs.update { it.copy(showRemaining = !it.showRemaining) } } },
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${state.speed}x · ${TimeFormat.formatMs(pos)}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            if (settings.showRemaining) TimeFormat.formatRemaining(pos, duration) else TimeFormat.formatMs(duration),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val pill = RoundedCornerShape(30.dp)
                        Row(
                            modifier = Modifier
                                .clip(pill)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), pill)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GlassIconButton(
                                if (state.locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                "Lock",
                            ) {
                                pm.setLocked(true)
                                controls = false
                            }
                            GlassIconButton(Icons.Filled.SkipPrevious, "Previous") { pm.previous() }
                            Box(
                                Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                androidx.compose.material3.IconButton(
                                    onClick = { pm.playPause() },
                                    modifier = Modifier.size(60.dp),
                                ) {
                                    androidx.compose.material3.Icon(
                                        if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                            GlassIconButton(Icons.Filled.SkipNext, "Next") { pm.next() }
                            GlassIconButton(Icons.Outlined.Fullscreen, "Rotate") { toggleRotate() }
                        }
                    }
                }
            }
        }

        if (state.locked) {
            Text(
                "Double-tap to unlock",
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp),
            )
        }

        state.sleepRemainingMs?.let { left ->
            if (left > 0) {
                Text(
                    "Sleep ${TimeFormat.formatMs(left)}",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp),
                )
            }
        }

        state.abLoop?.let { (a, b) ->
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 68.dp),
            ) {
                GlassButton(
                    if (b == Long.MAX_VALUE) "A ${TimeFormat.formatMs(a)}… (tap ✕ to clear)"
                    else "A-B ${TimeFormat.formatMs(a)}–${TimeFormat.formatMs(b)} ✕",
                    filled = true,
                ) {
                    pm.clearAbLoop()
                    pm.flash("A-B cleared")
                }
            }
        }

        if (menuOpen) {
            val cells = listOf(
                Triple(Icons.Outlined.SmartToy, "AI") { aiSheet = true; menuOpen = false },
                Triple(Icons.Outlined.PhotoCamera, "Capture") { capture(); menuOpen = false },
                Triple(Icons.Outlined.Timer, "Sleep") { sleepSheet = true; menuOpen = false },
                Triple(Icons.Outlined.GraphicEq, "EQ") { eqSheet = true; menuOpen = false },
                Triple(Icons.Outlined.Loop, "A-B") { cycleAb(); menuOpen = false },
                Triple(Icons.Outlined.AspectRatio, "Aspect") { cycleAspect(); menuOpen = false },
                Triple(
                    Icons.Outlined.Shuffle,
                    if (state.shuffle) "Shuffle on" else "Shuffle",
                ) { pm.toggleShuffle(); menuOpen = false },
                Triple(Icons.Outlined.Repeat, "Repeat ${state.repeatMode.name.lowercase()}") { cycleRepeat(); menuOpen = false },
                Triple(Icons.Filled.PictureInPictureAlt, "PiP") { onPip(); menuOpen = false },
                Triple(Icons.Outlined.FolderOpen, "Sub file") {
                    subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "*/*"))
                    menuOpen = false
                },
                Triple(Icons.Outlined.Info, "Info") { infoSheet = true; menuOpen = false },
                Triple(Icons.Outlined.PlaylistPlay, "Up next") { queueSheet = true; menuOpen = false },
                Triple(
                    if (favOn) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                    if (favOn) "Favorited" else "Favorite",
                ) { toggleFav(); menuOpen = false },
                Triple(Icons.Outlined.Share, "Share") { shareCurrent(); menuOpen = false },
                Triple(Icons.Outlined.Audiotrack, "Stats") { statsOverlay = true; menuOpen = false },
                Triple(Icons.Outlined.VolumeUp, if (boostOn) "Boost on" else "Boost") { toggleBoost(); menuOpen = false },
                Triple(Icons.Outlined.Speed, "Speed+") { speedSheet = true; menuOpen = false },
                Triple(Icons.Outlined.SkipPrevious, "Frame −") { pm.frameStep(false); menuOpen = false },
                Triple(Icons.Outlined.SkipNext, "Frame +") { pm.frameStep(true); menuOpen = false },
                Triple(
                    Icons.Outlined.ClosedCaption,
                    if (state.textTracks.any { it.selected }) "CC on" else "CC off",
                ) { toggleCc(); menuOpen = false },
                Triple(Icons.Outlined.List, "Playlist") { playlistSheet = true; menuOpen = false },
            )
            MenuGrid(cells = cells, onDismiss = { menuOpen = false })
        }

        if (statsOverlay && state.current != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { statsOverlay = false },
                contentAlignment = Alignment.Center,
            ) {
                StatsCard(state = state, onClose = { statsOverlay = false })
            }
        }

        if (showHints) {
            HintsCard(
                onGotIt = {
                    scope.launch {
                        withContext(Dispatchers.IO) { prefs.putString("hints_seen", "1") }
                        showHints = false
                    }
                },
            )
        }
    }

    if (resumeAsk && state.current != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { resumeAsk = false },
            title = { Text("Continue watching") },
            text = { Text("Continue from ${TimeFormat.formatMs(state.current!!.lastPositionMs)}?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    pm.seekTo(state.current!!.lastPositionMs)
                    resumeAsk = false
                }) { Text("Continue") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    pm.seekTo(0)
                    resumeAsk = false
                }) { Text("Start over") }
            },
        )
    }

    if (audioSheet) {
        TrackSheet(
            title = "Audio tracks",
            tracks = state.audioTracks,
            onPick = { pm.selectAudioTrack(it); audioSheet = false },
            onOff = { audioSheet = false },
            onDismiss = { audioSheet = false },
        )
    }
    if (textSheet) {
        TrackSheet(
            title = "Subtitles",
            tracks = state.textTracks,
            onPick = { pm.selectTextTrack(it); textSheet = false },
            onOff = { pm.selectTextTrack(null); textSheet = false },
            onDismiss = { textSheet = false },
            extra = {
                GlassButton("Load subtitle file") {
                    subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "*/*"))
                    textSheet = false
                }
            },
        )
    }
    if (chaptersSheet) {
        ChaptersSheet(
            chapters = pm.currentChapters(),
            currentIndex = pm.player.currentMediaItemIndex,
            onSeek = { pm.seekToChapter(it); chaptersSheet = false },
            onDismiss = { chaptersSheet = false },
        )
    }
    if (bookmarksSheet && state.current != null) {
        BookmarksSheet(
            videoId = state.current!!.id,
            onSeek = { pm.seekTo(it); bookmarksSheet = false },
            onDismiss = { bookmarksSheet = false },
        )
    }
    if (aiSheet) {
        AiSheet(pm = pm, onDismiss = { aiSheet = false })
    }
    if (infoSheet && state.current != null) {
        VideoInfoSheet(video = state.current!!, state = state, onDismiss = { infoSheet = false })
    }
    if (speedSheet) {
        SpeedSheet(pm = pm, onDismiss = { speedSheet = false })
    }
    if (sleepSheet) {
        SleepSheet(pm = pm, onDismiss = { sleepSheet = false })
    }
    if (eqSheet) {
        EqSheet(pm = pm, onDismiss = { eqSheet = false })
    }
    if (queueSheet) {
        QueueSheet(pm = pm, onDismiss = { queueSheet = false })
    }
    if (playlistSheet && state.current != null) {
        PlaylistSheet(videoId = state.current!!.id, onDismiss = { playlistSheet = false })
    }
}

@Composable
private fun MenuGrid(
    cells: List<Triple<ImageVector, String, () -> Unit>>,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        val card = RoundedCornerShape(28.dp)
        Column(
            Modifier
                .padding(horizontal = 40.dp, vertical = 48.dp)
                .clip(card)
                .background(Color(0xFF1C1C22))
                .border(1.dp, Color.White.copy(alpha = 0.12f), card)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            cells.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { (icon, label, action) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = action)
                                .padding(vertical = 4.dp),
                        ) {
                            Icon(
                                icon,
                                contentDescription = label,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                label,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun HintsCard(onGotIt: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        val card = RoundedCornerShape(28.dp)
        Column(
            Modifier
                .padding(horizontal = 40.dp)
                .clip(card)
                .background(Color(0xFF1C1C22))
                .border(1.dp, Color.White.copy(alpha = 0.12f), card)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Control with swipes", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text("◀ Left edge up/down: brightness", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(6.dp))
            Text("Right edge up/down: volume ▶", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(6.dp))
            Text("◀ ▶ Sideways: seek", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(6.dp))
            Text("Double-tap sides: skip ±10s", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(6.dp))
            Text("Pinch: zoom", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            GlassButton("Got it", filled = true, modifier = Modifier.fillMaxWidth(), onClick = onGotIt)
        }
    }
}
