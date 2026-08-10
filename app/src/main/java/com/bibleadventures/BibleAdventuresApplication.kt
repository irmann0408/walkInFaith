package com.bibleadventures

import android.app.Application

class BibleAdventuresApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
