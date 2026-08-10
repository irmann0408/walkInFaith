# Bible Adventures: Walk in Faith

An interactive Bible storybook / casual adventure game for children ages 7+, built as a
native Android app. The player travels through a warm, cartoon-style world inspired by
Bible stories, helping characters and solving gentle puzzles rather than fighting or
racing the clock.

Full product and engineering spec: [`bible prompt.txt`](bible%20prompt.txt).

## Status

Milestone 4 (Noah's Ark) is complete — the full chapter is playable end to end. See
[`docs/PROJECT_STATUS.md`](docs/PROJECT_STATUS.md) for the up-to-date milestone
tracker, known issues, and next tasks.

## Features (MVP scope)

- Main menu: Continue Adventure, Adventures, My Badges, Scripture Cards, Character,
  Settings, Parent Area.
- Character selection (boy/girl, 4 hairstyles, 4 skin tones, 4 clothing options),
  persisted on-device and reflected in a placeholder character preview.
- World Map: Home Village + all 6 chapter nodes, with real lock/unlock/completed
  state driven by progression. Only Noah's Ark is unlocked at the start.
- Chapter 1, "Noah's Ark," a complete playable adventure: story intro → find the
  animals → animal matching → gather supplies → organize the ark (drag-and-drop) →
  find missing items → lesson → scripture verse (World English Bible) → reward
  (stars, "Ark Builder" badge, scripture card).
- Offline-first: no accounts, no network calls, all progress stored on-device.
- No combat, no loot boxes, no in-app purchases, no ads, no data collection.

## Architecture

MVVM + Clean Architecture, native Android, single Gradle module (`app`) for the MVP.

```
com.bibleadventures
├── ui/
│   ├── navigation/   Destination routes + NavHost (single source of truth for routing)
│   ├── screens/      One package per screen (mainmenu, character, worldmap, noahsark/*, comingsoon, ...)
│   ├── components/   Reusable Compose components (buttons, character preview, locked-node overlay, scripture card)
│   └── theme/        Material 3 color scheme, typography, theme wrapper
├── domain/
│   ├── model/        PlayerProfile, Chapter, CharacterCustomization, Badge, ScriptureCard, ...
│   └── repository/   Repository interfaces (PlayerProfileRepository)
├── data/
│   ├── local/        DataStore-backed local data sources
│   └── repository/   Repository implementations
├── game/
│   ├── stories/      ChapterCatalog, NoahsArkContent (static content only)
│   ├── puzzles/      matching / dragsort / hiddenobject — pure, chapter-agnostic mini-game logic
│   └── rewards/      RewardCalculator, NoahsArkReward
├── progress/         ChapterUnlockRules (pure), ProgressionService
├── character/        Static picker content (CharacterOptionCatalog)
├── audio/            AudioController (silent no-op for now; real audio is Milestone 7)
├── AppContainer.kt, BibleAdventuresApplication.kt   Manual DI (no Hilt)
├── MainActivity.kt
```

A `settings/` package will be added once Milestone 6/7 need it, rather than stubbed
out ahead of time — see section 5/26 of the spec on avoiding overengineering.

Key principles: single source of truth for navigation and state, unidirectional data
flow (ViewModel → StateFlow → Composable), no business logic inside composables, no
network dependency for core gameplay. There is no DI framework — a small manual
`AppContainer` holds lazily-built repositories, and screens obtain ViewModels via
`viewModel(factory = AppViewModelProvider.Factory)`.

## Tech stack

- Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00), Material 3
- Navigation Compose 2.7.7
- AndroidX Lifecycle (ViewModel, StateFlow, `collectAsStateWithLifecycle`)
- AGP 8.5.2, Gradle 8.7, compileSdk/targetSdk 34, minSdk 24
- DataStore Preferences + kotlinx.serialization JSON for the single on-device save
  file (`PlayerProfile`) — not Room; see the architectural decisions log in
  `docs/PROJECT_STATUS.md` for why.

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
Feeding the 5,000, Daniel, Jesus Calms the Storm — currently `ChapterCatalog` entries
with no gameplay), more character customization, more mini-games, storybook mode,
scripture memory games, daily challenge, achievements, multiple save profiles. None of
these are implemented until the MVP (Chapters + progression + parent area) is stable.

## Asset requirements

The MVP runs on placeholder graphics (simple vector shapes, e.g.
`app/src/main/res/drawable/ic_launcher_foreground.xml`). Production art should be
dropped into an `assets/` structure of `characters/`, `animals/`, `environments/`,
`icons/`, `audio/`, `music/`, `ui/` without touching game logic — screens read
drawables/resources by name, not by hardcoded pixel data. Do not add downloaded
artwork without confirming its license permits redistribution in a commercial app.
