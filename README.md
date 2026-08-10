# Bible Adventures: Walk in Faith

An interactive Bible storybook / casual adventure game for children ages 7+, built as a
native Android app. The player travels through a warm, cartoon-style world inspired by
Bible stories, helping characters and solving gentle puzzles rather than fighting or
racing the clock.

Full product and engineering spec: [`bible prompt.txt`](bible%20prompt.txt).

## Status

Milestone 1 (Foundation) is complete. See [`docs/PROJECT_STATUS.md`](docs/PROJECT_STATUS.md)
for the up-to-date milestone tracker, known issues, and next tasks.

## Features (MVP scope)

- Main menu: Continue Adventure, Adventures, My Badges, Scripture Cards, Character,
  Settings, Parent Area.
- Chapter 1, "Noah's Ark," as a complete playable adventure (story → exploration →
  matching → gathering → sorting → hidden object → lesson → reward). *Not yet built —
  scaffolded for Milestone 4.*
- Offline-first: no accounts, no network calls, all progress stored on-device.
- No combat, no loot boxes, no in-app purchases, no ads, no data collection.

## Architecture

MVVM + Clean Architecture, native Android, single Gradle module (`app`) for the MVP.

```
com.bibleadventures
├── ui/
│   ├── navigation/   Destination routes + NavHost (single source of truth for routing)
│   ├── screens/      One package per screen (mainmenu, comingsoon, ...)
│   ├── components/   Reusable Compose components (large-touch-target buttons, etc.)
│   └── theme/        Material 3 color scheme, typography, theme wrapper
├── MainActivity.kt
```

`data/`, `domain/`, `game/`, `audio/`, `character/`, `progress/`, and `settings/` packages
will be added incrementally as their owning milestones land (character persistence,
progression/reward logic, the Noah's Ark mini-games, etc.) rather than stubbed out ahead
of time — see section 5/26 of the spec on avoiding overengineering.

Key principles: single source of truth for navigation and state, unidirectional data
flow (ViewModel → StateFlow → Composable), no business logic inside composables, no
network dependency for core gameplay.

## Tech stack

- Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00), Material 3
- Navigation Compose 2.7.7
- AndroidX Lifecycle (ViewModel, StateFlow, `collectAsStateWithLifecycle`)
- AGP 8.5.2, Gradle 8.7, compileSdk/targetSdk 34, minSdk 24
- Room / DataStore, Kotlin serialization — planned for Milestone 2+ (character &
  progress persistence), not yet added since nothing persists data yet.

## Build instructions

Requires JDK 17+ and the Android SDK (platform 34, build-tools 34.0.0).

```bash
# Point Gradle at your SDK if ANDROID_HOME isn't already set:
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

./gradlew build
```

## Run instructions

Open the project in Android Studio (Koala+) and run the `app` configuration on an
emulator or device (API 24+), or from the command line:

```bash
./gradlew installDebug
```

## Testing instructions

```bash
./gradlew test              # unit tests (progress logic, ViewModels, etc.)
./gradlew connectedAndroidTest   # Compose UI tests — requires a running emulator/device
```

## Future roadmap

See section 24 of the spec: additional adventures (David & Goliath, Good Samaritan,
Feeding the 5,000, Daniel, Jesus Calms the Storm), more character customization, more
mini-games, storybook mode, scripture memory games, daily challenge, achievements,
multiple save profiles. None of these are implemented until the MVP (Chapters +
progression + parent area) is stable.

## Asset requirements

The MVP runs on placeholder graphics (simple vector shapes, e.g.
`app/src/main/res/drawable/ic_launcher_foreground.xml`). Production art should be
dropped into an `assets/` structure of `characters/`, `animals/`, `environments/`,
`icons/`, `audio/`, `music/`, `ui/` without touching game logic — screens read
drawables/resources by name, not by hardcoded pixel data. Do not add downloaded
artwork without confirming its license permits redistribution in a commercial app.
