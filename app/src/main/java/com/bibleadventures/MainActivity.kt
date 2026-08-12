package com.bibleadventures

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.bibleadventures.ui.LocalAudioController
import com.bibleadventures.ui.navigation.BibleAdventuresNavHost
import com.bibleadventures.ui.theme.BibleAdventuresTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /**
     * Tracks foreground time for the Parent Area's "time played" stat. Since
     * this app is single-`Activity`, onStart/onStop already mirrors
     * process-level foreground tracking with no extra dependency. A hard
     * process kill mid-session loses that session's unflushed time — a
     * documented, accepted limitation (see docs/PROJECT_STATUS.md).
     */
    private var sessionStartElapsedRealtime: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val audioController = (application as BibleAdventuresApplication).container.audioController
        setContent {
            CompositionLocalProvider(LocalAudioController provides audioController) {
                BibleAdventuresTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        BibleAdventuresNavHost()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        sessionStartElapsedRealtime = SystemClock.elapsedRealtime()
    }

    override fun onStop() {
        super.onStop()
        val startedAt = sessionStartElapsedRealtime ?: return
        sessionStartElapsedRealtime = null
        val sessionMillis = SystemClock.elapsedRealtime() - startedAt
        val repository = (application as BibleAdventuresApplication).container.playerProfileRepository
        lifecycleScope.launch { repository.addPlayTime(sessionMillis) }
    }
}
