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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Speed
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.aether.player.domain.TimeFormat
import com.aether.player.domain.WatchProgressLogic
import com.aether.player.playback.CaptureResult
import com.aether.player.ui.components.GlassButton
import com.aether.player.ui.components.GlassIconButton
import com.aether.player.ui.theme.LocalAnimScale
import com.aether.player.ui.theme.LocalGlass
import com.aether.player.ui.theme.animDur
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onPip: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as AetherApp
    val pm = app.container.playerManager
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
    var more by remember { mutableStateOf(false) }
    var audioSheet by remember { mutableStateOf(false) }
    var textSheet by remember { mutableStateOf(false) }
    var infoSheet by remember { mutableStateOf(false) }
    var chaptersSheet by remember { mutableStateOf(false) }
    var bookmarksSheet by remember { mutableStateOf(false) }
    var aiSheet by remember { mutableStateOf(false) }
    var resumeAsk by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f) }
    val boostActive = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pm.loadExternalSubtitle(uri)
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

    BackHandler {
        if (state.locked) {
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
                                val d = GestureMath.volumeDelta(drag.y, size.height.toFloat(), max, settings.volumeSensitivity)
                                val next = (audio.getStreamVolume(AudioManager.STREAM_MUSIC) + d).coerceIn(0, max)
                                audio.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                                val pct = if (max == 0) 0 else next * 100 / max
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
                        .height(220.dp)
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
                    Text(
                        state.current?.title ?: "Aether",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    AndroidView(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(glass.surfaceStrong),
                        factory = { ctx ->
                            MediaRouteButton(ctx)
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(Icons.Outlined.Audiotrack, "Audio") { audioSheet = true }
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(Icons.Outlined.ClosedCaption, "Subtitles") { textSheet = true }
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(Icons.Filled.PictureInPictureAlt, "Picture in picture", onClick = onPip)
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(Icons.Outlined.MoreVert, "More") { more = true }
                }

                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIconButton(Icons.Filled.SkipPrevious, "Previous") { pm.previous() }
                    GlassIconButton(Icons.Filled.Replay10, "Rewind") { pm.skip(false) }
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(0.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.IconButton(onClick = { pm.playPause() }, modifier = Modifier.size(72.dp)) {
                            androidx.compose.material3.Icon(
                                if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                    GlassIconButton(Icons.Filled.Forward10, "Forward") { pm.skip(true) }
                    GlassIconButton(Icons.Filled.SkipNext, "Next") { pm.next() }
                }

                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(TimeFormat.formatMs(pos), color = Color.White, style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (settings.showRemaining) TimeFormat.formatRemaining(pos, duration) else TimeFormat.formatMs(duration),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        GlassIconButton(Icons.Outlined.Speed, "Speed") { more = true }
                        GlassIconButton(if (state.locked) Icons.Filled.Lock else Icons.Filled.LockOpen, "Lock") {
                            pm.setLocked(true)
                            controls = false
                        }
                        Text(
                            "${state.speed}x",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
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

    if (more) {
        PlayerMoreSheet(
            state = state,
            eqInfo = pm.equalizerInfo(),
            onDismiss = { more = false },
            onSpeed = { pm.setSpeed(it); more = false },
            onAspect = { pm.setAspect(it) },
            onSleep = { pm.startSleepTimer(it); more = false },
            onLock = { pm.setLocked(true); more = false },
            onInfo = { infoSheet = true; more = false },
            onSubtitleFile = {
                subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "*/*"))
                more = false
            },
            onAbA = { pm.setAbPoint(true) },
            onAbB = { pm.setAbPoint(false) },
            onAbClear = { pm.clearAbLoop() },
            onFrame = { pm.frameStep(true) },
            onRepeat = { pm.setRepeat(it) },
            onShuffle = { pm.toggleShuffle() },
            onBookmark = {
                val cur = state.current ?: return@PlayerMoreSheet
                scope.launch {
                    app.container.library.addBookmark(
                        cur.id,
                        state.positionMs,
                        "Bookmark ${TimeFormat.formatMs(state.positionMs)}",
                    )
                    pm.flash("Bookmark saved")
                    more = false
                }
            },
            onRotate = {
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
                            app.container.preferences.putString(
                                "ori_$id",
                                if (next == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) "sensor_landscape" else "sensor_portrait",
                            )
                        }
                    }
                }
            },
            onChapters = { chaptersSheet = true; more = false },
            onBookmarks = { bookmarksSheet = true; more = false },
            onAi = { aiSheet = true; more = false },
            onCapture = {
                more = false
                scope.launch {
                    when (val result = pm.captureFrame()) {
                        is CaptureResult.Saved -> pm.flash("Frame saved")
                        is CaptureResult.Unavailable -> pm.flash(result.reason)
                    }
                }
            },
            onEqPreset = { pm.applyEqualizerPreset(it) },
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
}
