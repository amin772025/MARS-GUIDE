package com.marsguide.vzd.ui.components

import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.marsguide.vzd.R
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.util.SoundManager
import kotlinx.coroutines.delay

/**
 * Full-Screen Cinematic Boot Sequence featuring NASA Mars reconnaissance footage
 * rendered on a TextureView-backed PlayerView so Compose HUD overlays composite cleanly.
 */
@Composable
fun BootSequenceOverlay(
    onComplete: () -> Unit,
    onSkipForever: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var step by remember { mutableIntStateOf(0) }
    var isVideoFinished by remember { mutableStateOf(false) }
    var isFirstFrameRendered by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .build()
            .apply {
                val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.lunar_first_step}")
                setMediaItem(MediaItem.fromUri(videoUri))
                volume = 1.0f
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        isFirstFrameRendered = true
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            isVideoFinished = true
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        android.util.Log.e("BootSequence", "Video playback error: ${error.message}")
                        isVideoFinished = true
                    }
                })
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (!isVideoFinished) exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try { exoPlayer.stop() } catch (_: Exception) {}
            try { exoPlayer.release() } catch (_: Exception) {}
        }
    }

    val steps = listOf(
        R.string.boot_initializing,
        R.string.boot_comm_bus,
        R.string.boot_science_db,
        R.string.boot_media_link,
        R.string.boot_carto_engine
    )

    LaunchedEffect(Unit) {
        // Silence background SFX/ambience while intro video audio plays
        SoundManager.setGlobalMute(true)
        delay(600)
        for (i in 1..steps.size) {
            step = i
            delay(850)
        }
        // Safety fallback: ensure intro never hangs indefinitely even if codec stalls
        delay(4500)
        if (!isVideoFinished) {
            isVideoFinished = true
        }
    }

    LaunchedEffect(isVideoFinished) {
        if (isVideoFinished) {
            SoundManager.setGlobalMute(false)
            SoundManager.playMissionConfirm()
            SoundManager.playArmstrongQuote()
            delay(650)
            onComplete()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("boot_sequence_overlay")
    ) {
        // 1. FULL-SCREEN VIDEO VIEWPORT (TextureView PlayerView for glitch-free Compose compositing)
        AndroidView(
            factory = { ctx ->
                val view = LayoutInflater.from(ctx).inflate(R.layout.view_boot_player, null, false) as PlayerView
                view.player = exoPlayer
                view
            },
            update = { playerView ->
                if (playerView.player !== exoPlayer) {
                    playerView.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isFirstFrameRendered) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(color = MarsOrangeBright, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    Text(
                        text = "ESTABLISHING ORBITAL FEED...",
                        color = ScienceTeal,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // 2. TACTICAL HUD OVERLAY
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.12f),
                            Color.Black.copy(alpha = 0.72f)
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            // SKIP CONTROLS (TOP RIGHT)
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        try { exoPlayer.stop() } catch (_: Exception) {}
                        SoundManager.setGlobalMute(false)
                        SoundManager.playClick()
                        onSkipForever()
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("skip_forever_button"),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, ScienceTeal.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.65f),
                        contentColor = ScienceTeal
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "DON'T SHOW",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = {
                        try { exoPlayer.stop() } catch (_: Exception) {}
                        SoundManager.setGlobalMute(false)
                        SoundManager.playClick()
                        onComplete()
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("skip_boot_button"),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MarsOrangeBright.copy(alpha = 0.8f)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black.copy(alpha = 0.75f),
                        contentColor = MarsOrangeBright
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "SKIP INTRO",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // BOOT LOG (BOTTOM LEFT)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .widthIn(max = 340.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MarsOrangeBright,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "VZD // MARS MISSION TACTICAL HUD",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScienceTeal,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                steps.forEachIndexed { index, stringRes ->
                    val isVisible = step > index
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn() + expandVertically()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[ OK ]",
                                color = ScienceGreen,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.width(56.dp)
                            )
                            Text(
                                text = stringResource(stringRes).uppercase(),
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
