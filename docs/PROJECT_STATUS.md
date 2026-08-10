# Project Status

Last updated: 2026-08-10

## Current milestone

**Milestone 1 — Foundation: COMPLETE**

## Completed features

- Android project scaffolded: Gradle Kotlin DSL, AGP 8.5.2, Kotlin 1.9.24, Gradle 8.7
  wrapper checked in.
- Jetpack Compose + Material 3 configured (`compose = true`, Compose BOM 2024.06.00).
- App-wide theme (`ui/theme`): warm/bright color scheme, large-type typography scale.
- Navigation Compose wired up via a single `BibleAdventuresNavHost` and a sealed
  `Destination` class (no route strings scattered through the UI).
- Main Menu screen with all seven MVP buttons (Continue Adventure, Adventures, My
  Badges, Scripture Cards, Character, Settings, Parent Area), backed by a
  `MainMenuViewModel` exposing `StateFlow<MainMenuUiState>`.
  - "Continue Adventure" renders disabled since no save data exists yet.
- Reusable `AdventureMenuButton` component (64dp min height for large touch targets).
- Placeholder `ComingSoonScreen` — every menu item not yet built (all of them, for now)
  routes here, so navigation + back-stack behavior is exercised end-to-end even though
  Character/Map/Badges/etc. don't exist yet.
- Unit test for `MainMenuViewModel` initial state; Compose UI test covering app launch
  and forward/back navigation between Main Menu and the placeholder screen.
- `./gradlew build` passes (assemble debug + release, lint, unit tests, androidTest
  compilation).

## Environment notes

- This machine had no Android SDK installed. Set up locally at `C:\Android\Sdk`
  (cmdline-tools, platform-tools, `platforms;android-34`, `build-tools;34.0.0`) and a
  standalone Gradle 8.7 distribution at `C:\Android\gradle-dist` was used once to
  generate the wrapper. Neither path is committed; `local.properties` (gitignored)
  points at the SDK.
- No emulator/AVD has been created yet, so `connectedAndroidTest` has not been run —
  only compiled. Unit tests did run and pass.

## Known issues / follow-ups

- Launcher icon is a placeholder vector shape, not final art.
- minSdk 24 devices fall back to a non-adaptive icon; no legacy PNG mipmap was
  generated (only the `mipmap-anydpi-v26` adaptive icon exists). Cosmetic only —
  does not block the build.
- No `data/`, `domain/`, `game/`, `audio/`, `character/`, `progress/`, or `settings/`
  packages yet — intentionally deferred until the milestone that needs them, per the
  spec's guidance against overengineering.

## Next tasks (Milestone 2 — Character)

- Character selection screen (boy/girl, hairstyle, skin tone, clothing).
- `CharacterCustomization` domain model.
- Persistence (DataStore) for the chosen character.
- Character preview composable.
- Wire "Character" menu item to the new screen instead of `ComingSoonScreen`.

## Architectural decisions log

- **Single Gradle module for the MVP.** A multi-module split (`core`, `feature-*`) is
  not warranted yet at this scope; revisit if build times or team size grow.
- **No dark theme variant.** The target audience and warm/storybook visual language
  make a single bright palette the right default; can revisit as an accessibility
  option later.
- **Kotlin 1.9.24 / Compose Compiler via `composeOptions`** rather than Kotlin 2.0's
  `org.jetbrains.kotlin.plugin.compose` plugin, to keep the dependency graph on a
  well-established, widely-documented combination for the initial scaffold.
