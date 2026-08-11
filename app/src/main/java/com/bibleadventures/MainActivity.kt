package com.bibleadventures

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.bibleadventures.ui.LocalAudioController
import com.bibleadventures.ui.navigation.BibleAdventuresNavHost
import com.bibleadventures.ui.theme.BibleAdventuresTheme

class MainActivity : ComponentActivity() {
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
}
