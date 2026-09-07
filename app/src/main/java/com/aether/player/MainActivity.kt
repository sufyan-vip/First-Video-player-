package com.aether.player

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aether.player.ui.nav.AetherRoot
import com.aether.player.ui.theme.AetherTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var autoPipEnabled = true
    private var backgroundPlayback = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val container = (application as AetherApp).container
        activityScope.launch {
            container.preferences.settings.collect { settings ->
                autoPipEnabled = settings.autoPip
                backgroundPlayback = settings.backgroundPlayback
            }
        }
        handleIncoming(intent?.data)
        setContent {
            val settings by container.preferences.settings.collectAsState(
                initial = com.aether.player.data.prefs.AetherSettings(),
            )
            AetherTheme(settings) {
                AetherRoot(
                    container = container,
                    onEnterPip = { enterPip() },
                    onImmersive = { immersive -> setImmersiveMode(immersive) },
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent.data)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!autoPipEnabled) return
        val pm = (application as AetherApp).container.playerManager
        if (pm.state.value.playing && !pm.state.value.miniPlayer) {
            enterPip()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!backgroundPlayback && !isInPictureInPictureMode) {
            val pm = (application as AetherApp).container.playerManager
            if (pm.state.value.playing) pm.player.pause()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        val pm = (application as AetherApp).container.playerManager
        pm.setMiniPlayer(isInPictureInPictureMode)
    }

    private fun handleIncoming(uri: Uri?) {
        if (uri == null) return
        val container = (application as AetherApp).container
        val title = uri.lastPathSegment ?: "Opened media"
        activityScope.launch {
            val video = container.library.upsertNetworkVideo(uri.toString(), title)
            container.playerManager.playSingle(video)
        }
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT < 26) return
        val state = (application as AetherApp).container.playerManager.state.value
        if (state.current == null) return
        val w = state.videoWidth.takeIf { it > 0 } ?: 16
        val h = state.videoHeight.takeIf { it > 0 } ?: 9
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(w, h).let { r ->
                if (r.toFloat() < 0.42f || r.toFloat() > 2.39f) Rational(16, 9) else r
            })
            .build()
        runCatching { enterPictureInPictureMode(params) }
    }

    private fun setImmersiveMode(immersive: Boolean) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (immersive) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
