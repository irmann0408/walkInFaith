# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Bible Adventures: Walk in Faith** — a native Android/Jetpack Compose interactive Bible
storybook game for children ages 7+. Full product/engineering spec: [`bible prompt.txt`](bible%20prompt.txt).
Current status, milestone history, and architectural decisions log: [`docs/PROJECT_STATUS.md`](docs/PROJECT_STATUS.md)
— **read that file before starting new work**, and update it (Completed features / Known
issues / Next tasks / Architectural decisions log) after any meaningful change, per the
spec's AI development workflow (section 27).

## Commands

```bash
./gradlew build                    # full build (compile + unit tests + lint)
./gradlew test                     # JVM unit tests only
./gradlew testDebugUnitTest --tests "com.bibleadventures.game.puzzles.matching.MatchingGameTest"  # single test class
./gradlew testDebugUnitTest --tests "*.MatchingGameTest.specific test name"  # single test method
./gradlew connectedAndroidTest     # instrumented Compose UI tests — requires a running emulator/device
./gradlew installDebug             # build and install on a connected device/emulator
```

Requires JDK 17+ and Android SDK (platform 34, build-tools 34.0.0); point Gradle at the
SDK via `local.properties` (`sdk.dir=...`, gitignored). There is no linter/formatter
task beyond what `./gradlew build` runs.

## Architecture

Single-module (`app`) MVVM + Clean Architecture, no DI framework, no Room — see
`docs/PROJECT_STATUS.md`'s "Architectural decisions log" for the reasoning behind each
of these choices before changing them.

```
com.bibleadventures
├── ui/
│   ├── navigation/   Destination routes (sealed class) + single NavHost — source of truth for routing
│   ├── screens/      One package per screen (mainmenu, character, worldmap, noahsark/<scene>, comingsoon)
│   ├── components/   Reusable Compose components
│   └── theme/        Material 3 color scheme, typography (no dark theme variant — deliberate)
├── domain/
│   ├── model/        PlayerProfile, Chapter, CharacterCustomization, Badge, ScriptureCard, ...
│   └── repository/   Repository interfaces
├── data/
│   ├── local/        DataStore-backed local data sources
│   └── repository/   Repository implementations
├── game/
│   ├── stories/      ChapterCatalog, NoahsArkContent — static content only, no logic
│   ├── puzzles/      matching / dragsort / hiddenobject — pure, chapter-agnostic mini-game state+logic
│   └── rewards/      RewardCalculator, NoahsArkReward
├── progress/         ChapterUnlockRules (pure function) + ProgressionService
├── character/        Static picker content (CharacterOptionCatalog)
├── audio/            AudioController — NoOpAudioController for now, real playback is Milestone 7
├── AppContainer.kt   Manual DI: lazily-built repositories/services, one property per milestone
├── BibleAdventuresApplication.kt, MainActivity.kt
```

Key rules to preserve when extending this codebase:

- **Manual DI only.** `AppContainer` holds lazily-built dependencies; screens obtain
  ViewModels via `viewModel(factory = AppViewModelProvider.Factory)`. Do not introduce
  Hilt or another DI framework — the dependency graph is intentionally small.
- **Single source of truth for navigation.** All routes are `Destination` sealed-class
  entries in `ui/navigation/Destinations.kt`; the NavHost is the only place that wires
  routes to screens. Route on explicit enums (e.g. `MenuItemId`), never on a button's
  display-label string.
- **Puzzle packages are pure Kotlin, no Compose/Android dependency.** Each of
  `game/puzzles/{matching,dragsort,hiddenobject}` is a state holder + transition
  function, directly unit-testable, reused across chapters by supplying new content —
  not through a shared "engine" abstraction. Don't build a generic engine ahead of a
  second chapter actually needing one.
- **`game/stories/*Content.kt` files hold only static content** (dialogue, item lists,
  positions) — never gameplay logic. This keeps chapter content swappable/data-driven
  without touching the puzzle packages.
- **One save file.** Player state is a single `PlayerProfile` persisted as
  kotlinx.serialization JSON in one DataStore Preferences entry
  (`data/local/PlayerProfileLocalDataSource.kt`). Do not add Room or a second
  persistence mechanism for this scale of data. Corrupted/missing save data must fall
  back to `PlayerProfile.DEFAULT`, never crash.
- **Persisted keys are stable strings/enum names, never ordinals.** `ChapterId`
  serializes by name; `Badge`/`ScriptureCard` ids are plain strings resolved against a
  static catalog at display time. Renaming an enum constant already present in saved
  data silently loses that data — avoid it.
- **Per-scene navigation routes, one per Noah's-Ark-style chapter scene**, sharing a
  single graph-scoped ViewModel (resolved via
  `navController.getBackStackEntry(GRAPH_ROUTE)`), not one route with an internal scene
  index — this lets system Back pop one scene at a time for free. Follow this pattern
  for future chapters.
- **`onChapterFinished`-style completion callbacks must be idempotent** — re-entering a
  reward screen must never double-award stars/badges.
- **No business logic inside composables**; state flows ViewModel → StateFlow →
  Composable, unidirectionally.

## Product constraints (non-negotiable, from the spec)

- **No combat, loot boxes, gambling mechanics, in-app purchases, ads, external links to
  children, chat/messaging, or user-generated content.** Challenges are built from
  helping, puzzles, matching, memory, counting, finding objects — never fighting or
  racing a clock in a punishing way.
- **No personal data collection** (name, email, location, contacts, camera, mic) unless
  explicitly required later and justified.
- **Offline-first**: core gameplay must never depend on a network call, remote API, or
  account.
- **Accessibility**: large touch targets (≥48dp for tappable game objects), state is
  never conveyed by color alone (pair with icon + content description), meaningful
  content descriptions on interactive elements.
- **No failure states in mini-games.** Wrong answers get "try again," never a
  game-over/punishment state (see `matching` package: outcomes are always
  `CORRECT`/`TRY_AGAIN`).
- Bible verse text used in-app must be from a translation that is actually free to
  redistribute (e.g. World English Bible, public domain) — don't assume a translation's
  license; this was an explicit decision point in Milestone 4.
- New chapters follow the flow **Story → Exploration → Helping → Puzzle → Choice →
  Lesson → Reward**, matching the existing Noah's Ark scene structure.

## Working style expected in this repo

Per spec sections 1, 27, 28 (this project was bootstrapped from a "master prompt"
designed for AI-driven incremental development):

- Work one feature/milestone at a time; don't jump ahead to a later milestone's scope.
- Compile and run relevant tests after meaningful changes; keep the app in a buildable
  state at every step — never leave it deliberately broken.
- If something in the spec is ambiguous, make a sensible engineering call and record it
  in `docs/PROJECT_STATUS.md`'s "Architectural decisions log" rather than stalling.
- When a build or test fails, fix the root cause — don't disable the test or hide the
  error to get a green build.
- After finishing a unit of work, update `docs/PROJECT_STATUS.md` (completed features,
  known issues, next tasks) — this file is the actual project tracker, more current
  than the README.
