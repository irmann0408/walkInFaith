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
import com.bibleadventures.audio.CharacterVoiceLine
import com.bibleadventures.domain.model.CharacterCustomization
import com.bibleadventures.ui.LocalAudioController
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
 *
 * [characterVoiceLine], when supplied (currently only Noah's Ark and David
 * & Goliath have real recordings), makes the character genuinely speak: its
 * own recorded line plays automatically once the narration track finishes,
 * or immediately if the player taps the character first — tapping pauses
 * narration and resumes it once the character's own line finishes, so the
 * two voices never overlap.
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
    characterVoiceLine: CharacterVoiceLine? = null,
) {
    val context = LocalContext.current
    val audioController = LocalAudioController.current
    val videoPlayer = remember { ExoPlayer.Builder(context).build() }
    val narrationPlayer = remember { ExoPlayer.Builder(context).build() }
    var videoEnded by remember { mutableStateOf(false) }
    // Guards against the character's own line playing twice for one scene —
    // once from the auto-trigger below and again if the (resumed) narration
    // later reaches its own natural end. Set the moment the line is
    // triggered by either path, whichever happens first.
    var characterLinePlayed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        // Video is muted for as long as narration is playing, un-muted once it finishes.
        // Once narration truly ends, the character's own recorded line (if
        // any) plays automatically — unless the player already triggered it
        // early by tapping the character (see CharacterCallout's onClick below).
        val narrationListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    videoPlayer.volume = 1f
                    if (characterVoiceLine != null && !characterLinePlayed) {
                        characterLinePlayed = true
                        audioController.playCharacterLine(characterVoiceLine)
                    }
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
            // Deliberately NOT audioController.stopSpeaking() here: Compose
            // Navigation keeps the outgoing and incoming screens composed
            // together during the transition, so this onDispose can — and,
            // confirmed on-device, reliably did — fire *after* the next
            // screen's own LaunchedEffect(Unit) had already started playing
            // its own intro line, killing it a couple of words in on every
            // single puzzle screen (every one of them is entered right after
            // a video). Stopping this screen's own audio belongs at the
            // moment *this* screen decides to leave (see the topBar's onNext
            // below), which runs strictly before the next screen exists.
        }
    }

    LaunchedEffect(videoRes, narrationRes) {
        videoEnded = false
        characterLinePlayed = false
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
                // Stops this screen's own narration/character-line audio
                // synchronously, strictly before navigating away — see the
                // onDispose comment above for why that ordering matters and
                // a same-instant stop in onDispose does not achieve it.
                onNext = {
                    audioController.stopSpeaking()
                    onContinue()
                },
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
                    // Reflection lines are full sentences, not short puzzle-feedback
                    // phrases — they routinely wrap to 2+ lines, so the default
                    // clearance (tuned for something like "Great job!") isn't
                    // enough to keep the bubble from growing down into the character.
                    bubbleAboveClearance = 76.dp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                    onClick = if (characterVoiceLine != null) {
                        {
                            val wasNarrating = narrationPlayer.isPlaying
                            if (wasNarrating) narrationPlayer.pause()
                            characterLinePlayed = true
                            audioController.playCharacterLine(characterVoiceLine) {
                                if (wasNarrating) narrationPlayer.play()
                            }
                        }
                    } else {
                        null
                    },
                )
            }

            IconButton(
                onClick = {
                    videoEnded = false
                    characterLinePlayed = false
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
