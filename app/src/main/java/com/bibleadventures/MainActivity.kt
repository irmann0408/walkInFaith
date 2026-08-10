package com.bibleadventures

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bibleadventures.ui.navigation.BibleAdventuresNavHost
import com.bibleadventures.ui.theme.BibleAdventuresTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BibleAdventuresTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BibleAdventuresNavHost()
                }
            }
        }
    }
}
