plugins {
    id("com.android.application") version "9.3.0" apply false
    // No org.jetbrains.kotlin.android plugin — AGP 9.0+ has built-in Kotlin
    // support and requires that plugin NOT be applied (see
    // https://developer.android.com/build/releases/agp-9-0-0-release-notes#android-gradle-plugin-built-in-kotlin).
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
