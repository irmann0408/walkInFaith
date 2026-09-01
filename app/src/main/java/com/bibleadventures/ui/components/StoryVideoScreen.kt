package com.bibleadventures.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.bibleadventures.R
import com.bibleadventures.domain.model.CharacterCustomization
import android.net.Uri

/**
 * A narrated video scene — the video+narration equivalent of [StoryBeatScreen],
 * used directly from the NavHost for Noah's Ark's restructured chapter (see
 * docs/PROJECT_STATUS.md). Two independent `ExoPlayer` instances, the same
 * technique already proven in the sibling `bibleStory` project this content
 * came from: the video plays once and holds its last frame (it does not
 * loop — that's what makes "the video finished" a real, single event a
 * [reflectionRes] speech bubble can key off) while a separate narration
 * track plays once over it, muting the video's own ambient audio for as
 * long as narration is playing and restoring it once narration ends.
 * Tapping the replay button restarts both the video and narration together
 * from the start.
 *
 * Unlike `bibleStory`'s own manual Previous/Next button row, this reuses the
 * app's existing [PuzzleTopBar] "Next Page" pattern (per the user's explicit
 * choice) so every screen in the app shares one navigation convention.
 *
 * [characterCustomization] and [reflectionRes] are both optional so a future
 * chapter can use this screen without the player's character appearing at
 * all — when both are supplied, [CharacterCallout] puts the player's own
 * character in the bottom-start corner for the whole scene, and once the
 * video finishes a full playthrough its speech bubble appears with a short
 * reflection line tying the scene to something it's learning.
 */
@OptIn(UnstableApi::class)
@Composable
fun StoryVideoScreen(
    videoRes: Int,
    narrationRes: Int,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    characterCustomization: CharacterCustomization? = null,
    reflectionRes: Int? = null,
) {
    val context = LocalContext.current
    val videoPlayer = remember { ExoPlayer.Builder(context).build() }
    val narrationPlayer = remember { ExoPlayer.Builder(context).build() }
    var videoEnded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        // Video is muted for as long as narration is playing, un-muted once it finishes.
        val narrationListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    videoPlayer.volume = 1f
                }
            }
        }
        narrationPlayer.addListener(narrationListener)

        // Drives the reflection speech bubble — only shown once the video
        // itself (not narration) has finished a full playthrough.
        val videoListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    videoEnded = true
                }
            }
        }
        videoPlayer.addListener(videoListener)

        onDispose {
            narrationPlayer.removeListener(narrationListener)
            videoPlayer.removeListener(videoListener)
            videoPlayer.release()
            narrationPlayer.release()
        }
    }

    LaunchedEffect(videoRes, narrationRes) {
        videoEnded = false
        videoPlayer.setMediaItem(rawResMediaItem(context.packageName, videoRes))
        videoPlayer.repeatMode = Player.REPEAT_MODE_OFF
        videoPlayer.prepare()
        videoPlayer.playWhenReady = true

        narrationPlayer.stop()
        narrationPlayer.setMediaItem(rawResMediaItem(context.packageName, narrationRes))
        narrationPlayer.repeatMode = Player.REPEAT_MODE_OFF
        narrationPlayer.prepare()
        narrationPlayer.playWhenReady = true

        // Video stays muted for as long as narration is (about to be) playing.
        videoPlayer.volume = 0f
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            PuzzleTopBar(
                showBackButton = false,
                onBackToMainMenu = {},
                showNextButton = true,
                onNext = onContinue,
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { videoContext ->
                    PlayerView(videoContext).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        player = videoPlayer
                    }
                },
            )

            if (characterCustomization != null) {
                CharacterCallout(
                    characterCustomization = characterCustomization,
                    message = if (reflectionRes != null && videoEnded) stringResource(reflectionRes) else null,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                )
            }

            IconButton(
                onClick = {
                    videoEnded = false
                    videoPlayer.seekTo(0)
                    videoPlayer.volume = 0f
                    videoPlayer.play()
                    narrationPlayer.seekTo(0)
                    narrationPlayer.play()
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Replay,
                    contentDescription = stringResource(R.string.action_replay_scene),
                )
            }
        }
    }
}

private fun rawResMediaItem(packageName: String, rawResId: Int): MediaItem =
    MediaItem.fromUri(Uri.parse("android.resource://$packageName/$rawResId"))
