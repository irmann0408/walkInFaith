# Project Status

Last updated: 2026-08-10

## Current milestone

**Milestone 4 — Noah's Ark: COMPLETE**

## Completed features

### Milestone 1 — Foundation
- Android project scaffolded: Gradle Kotlin DSL, AGP 8.5.2, Kotlin 1.9.24, Gradle 8.7
  wrapper checked in.
- Jetpack Compose + Material 3 configured (`compose = true`, Compose BOM 2024.06.00).
- App-wide theme (`ui/theme`): warm/bright color scheme, large-type typography scale.
- Navigation Compose wired up via a single `BibleAdventuresNavHost` and a sealed
  `Destination` class (no route strings scattered through the UI).
- Main Menu screen with all seven MVP buttons, backed by a `MainMenuViewModel`.
- Reusable `AdventureMenuButton` component (64dp min height for large touch targets).
- Placeholder `ComingSoonScreen` for menu items not yet built.

### Milestone 2 — Character
- Domain models (`domain/model`) for the full spec section 18 schema: `ChapterId`,
  `ChapterStatus`, `Chapter`, `AdventureProgress`, `Badge`, `ScriptureCard`,
  `CharacterCustomization` (+ `Appearance`/`Hairstyle`/`SkinTone`/`Clothing` enums),
  `PlayerProfile` — defined once now to avoid schema churn in M3/M4.
- Persistence: a single DataStore Preferences entry holding one kotlinx.serialization
  JSON-encoded `PlayerProfile` (`data/local/PlayerProfileLocalDataSource.kt`), behind
  `domain/repository/PlayerProfileRepository` / `data/repository/PlayerProfileRepositoryImpl`.
  Corrupted or missing saved data falls back to `PlayerProfile.DEFAULT` instead of
  crashing (spec section 20) — covered by an instrumented test that writes garbage
  JSON directly and asserts the fallback.
- Manual DI: `BibleAdventuresApplication` + `AppContainer` (lazily-built repositories)
  + `ui/AppViewModelProvider` (`viewModelFactory { initializer { } }` DSL) — no Hilt.
- `MainMenuScreen`/`BibleAdventuresNavHost` refactored to route on an explicit
  `MenuItemId` enum instead of the button's display-label string.
- `character/CharacterOptionCatalog.kt` — static picker content (2 appearances, 4
  hairstyles, 4 skin tones, 4 clothing options), placeholder swatch colors.
- `ui/components/CharacterPreview.kt` — Canvas-drawn placeholder character render
  (no per-combination art needed); reusable in later milestones.
- `ui/screens/character/{CharacterScreen,CharacterViewModel}` + `OptionPicker`
  component — selections save immediately through the repository (single source of
  truth, no local duplicate state); selection state is never color-only (check icon +
  content description also mark the selected option, per spec section 13).
- "Character" menu item now routes to the real screen instead of `ComingSoonScreen`.
- Tests: `PlayerProfileRepositoryImplTest` (character update, scene/chapter completion
  merging), `CharacterViewModelTest`, `PlayerProfileLocalDataSourceInstrumentedTest`
  (real DataStore round-trip + corrupted-data fallback), `CharacterNavigationTest`
  (select a hairstyle, leave, come back, selection persisted).
- `./gradlew build` passes; unit tests run and pass.

### Milestone 3 — World Map
- `game/stories/ChapterCatalog.kt` — all 6 chapters in a linear `requiredChapter`
  chain, matching spec section 7's example order. Only Noah's Ark has real gameplay;
  the other 5 exist as content (title/description/lesson/scripture reference) so the
  map, unlock rules, and progression logic have something real to work with ahead of
  their own milestones.
- `progress/ChapterUnlockRules.kt` — pure function computing each chapter's
  `LOCKED`/`UNLOCKED`/`COMPLETED` status from the catalog + a set of completed chapter
  ids. `progress/ProgressionService.kt` wraps this with `PlayerProfileRepository` and
  exposes `Flow<Map<ChapterId, ChapterStatus>>` — this *is* the "progression
  service/repository" spec section 11 asks for; no screen computes unlock logic
  itself. Added to `AppContainer`.
- `ui/screens/worldmap/{WorldMapScreen,WorldMapViewModel}` — Home Village header
  (non-interactive) followed by all 6 chapter nodes; locked nodes are disabled
  (`clickable(enabled = false)`) with a lock icon (`ui/components/LockedNodeOverlay`)
  *and* a content description naming the prerequisite chapter — lock state is never
  conveyed by color alone (spec section 13). Completed nodes show stars earned.
- "Adventures" menu item now routes to the real World Map instead of `ComingSoonScreen`.
  Tapping Noah's Ark (the only unlocked chapter) still routes to `ComingSoonScreen`
  for now, since its real gameplay doesn't exist until Milestone 4; every other node
  is locked and non-interactive. The existing `MainMenuNavigationTest`'s "tap a menu
  item → ComingSoon" case was repointed from "Adventures" to "My Badges" since
  Adventures no longer goes there.
- Tests: `ChapterUnlockRulesTest` (initial-unlock state, chain progression, a
  synthetic out-of-order-completion case), `WorldMapViewModelTest` (status + star
  mapping, before/after completing a chapter), `WorldMapNavigationTest` (all 6 nodes
  render with correct enabled/disabled state; tapping Noah's Ark reaches the
  placeholder).
- `./gradlew build` passes; unit tests run and pass.

### Milestone 4 — Noah's Ark
The full playable adventure: story intro, exploration, animal matching, food gathering,
ark organization, hidden-object search, lesson, and reward — all 9 items from spec
section 8's Noah's Ark checklist.
- `audio/AudioController.kt` — silent hook points only (`NoOpAudioController`), called
  at match-success/reward-celebration moments; real playback + Settings toggles are
  Milestone 7 scope, per the user's decision for this batch of work.
- `game/rewards/{RewardCalculator,NoahsArkReward}` — full completion always awards 3
  stars (no partial-credit penalty, consistent with "no punishment for mistakes");
  `NoahsArkReward` holds the "Ark Builder" badge and the Genesis 6:22 scripture card,
  using the literal **World English Bible (WEB)** text (public domain), per the user's
  decision.
- Three reusable, chapter-agnostic puzzle packages under `game/puzzles/` — each a pure,
  directly-unit-testable state holder + transition function, no Compose/Android
  dependency: `matching` (tap-to-match pairs, outcome always `CORRECT`/`TRY_AGAIN`,
  never a failure state), `dragsort` (drop-target logic; the actual drag gesture
  handling lives in the screen via `pointerInput`/`detectDragGestures`), `hiddenobject`
  (found-item tracking; the screen renders ≥48dp tap targets regardless of icon size to
  avoid pixel-hunting, per spec section 9). No `game/engine/` abstraction was factored
  out — reuse comes from configuring each package with new content, not a shared engine.
- `game/stories/NoahsArkContent.kt` — static content only (6 animals, 4 supplies, sort
  categories/items, hidden-item positions, intro dialogue); this is the only file
  that's Noah's-Ark-specific, keeping the puzzle packages reusable for future chapters.
- `ui/screens/noahsark/` — one screen per scene (`intro`, `findanimals`, `matching`,
  `gathersupplies`, `organizeark`, `missingitems`, `lesson`, `reward`), each its own
  `Destination` route inside a nested `navigation()` graph, sharing one graph-scoped
  `NoahsArkViewModel` (resolved via `navController.getBackStackEntry(GRAPH_ROUTE)` +
  `viewModel(viewModelStoreOwner = ...)`). Per-scene routes (not one route + an
  internal scene index) so system Back naturally pops one scene at a time.
  `NoahsArkViewModel.onChapterFinished()` is idempotent — re-entering the Reward screen
  never double-awards stars (spec section 20).
- "Return to Map" from the Reward screen pops the entire Noah's Ark back stack
  (`popUpTo(WorldMap.route)`), so Back afterward can't re-enter a finished run.
- World Map's Noah's Ark node now routes directly into the real adventure instead of
  `ComingSoonScreen`.
- Tests: `RewardCalculatorTest`, `MatchingGameTest`, `DragSortGameTest`,
  `HiddenObjectGameTest` (pure puzzle logic — spec section 19), `NoahsArkViewModelTest`
  (initial content counts, scene callbacks, `onChapterFinished` awards stars exactly
  once even when called twice), extended `PlayerProfileRepositoryImplTest` coverage
  from M2 already exercises the merge logic `completeChapter` relies on.
  `NoahsArkFlowTest` (instrumented, full walk World Map → all 8 scenes → Reward →
  Return to Map → World Map shows Noah's Ark completed and David & Goliath unlocked,
  including driving the drag-and-drop scene via `performTouchInput { swipe(...) }`),
  `AnimalMatchingGameTest` (instrumented, confirms "Great job!"/"Try another one!" text
  per spec section 9 and that a mismatch never blocks a correct match afterward).
- `./gradlew build` passes; all 34 unit tests pass. All 10 instrumented tests pass on
  a real device (`./gradlew connectedDebugAndroidTest`), including `NoahsArkFlowTest`'s
  drag-and-drop step (`performTouchInput { swipe(...) }` onto the correct category)
  and `AnimalMatchingGameTest`'s "Great job!"/"Try another one!" feedback check.
- Two bugs only surfaced once the suite actually ran on-device (first real run since
  M2) and were fixed: `WorldMapNavigationTest` had a stale assertion expecting Noah's
  Ark to still route to `ComingSoonScreen`, predating M4's real destination; and
  `PlayerProfileLocalDataSourceInstrumentedTest.tearDown()` was an expression-bodied
  function whose inferred return type was `Preferences` instead of `Unit`, which
  JUnit4 rejects for `@After` methods — fixed with an explicit block body.

## Environment notes

- This machine had no Android SDK installed. Set up locally at `C:\Android\Sdk`
  (cmdline-tools, platform-tools, `platforms;android-34`, `build-tools;34.0.0`) and a
  standalone Gradle 8.7 distribution at `C:\Android\gradle-dist` was used once to
  generate the wrapper. Neither path is committed; `local.properties` (gitignored)
  points at the SDK.
- A physical device (Samsung Galaxy S25 Ultra, `SM-S938B`) is used for
  `connectedAndroidTest` instead of an emulator/AVD — connect via USB with debugging
  enabled.

## Known issues / follow-ups

- Launcher icon is a placeholder vector shape, not final art.
- minSdk 24 devices fall back to a non-adaptive icon; no legacy PNG mipmap was
  generated (only the `mipmap-anydpi-v26` adaptive icon exists). Cosmetic only.
- No `settings/` package yet — deferred until Milestone 6/7 need it.
- `MainMenuViewModel.hasAdventureInProgress` is still hardcoded `false` — wiring it to
  the real `PlayerProfileRepository` is cheap now that the repository exists, but is
  being left for Milestone 5 (Progression) as originally planned, to keep "Continue
  Adventure" behavior consistent with the rest of the progression system landing in
  one milestone rather than piecemeal.
- David & Goliath through Jesus Calms the Storm still exist only as `ChapterCatalog`
  entries with no gameplay — expected per spec section 7, each lands in its own future
  milestone.
- All Noah's Ark art (animals, supplies, badge, backgrounds) is simple placeholder
  vector shapes, not final art (spec section 25) — code reads them by drawable
  resource id, so swapping in real art later doesn't touch game logic.

## Next tasks (Milestone 5 — Progression)

- Wire `MainMenuViewModel.hasAdventureInProgress` to real save data (per-chapter
  `completedActivities` already tracks enough to resume mid-adventure).
- Badges gallery and Scripture Cards gallery screens (data already collected in
  `PlayerProfile.badges`/`scriptureCards`; only the "My Badges"/"Scripture Cards" menu
  items still route to `ComingSoonScreen`).
- Formalize save/load edge cases beyond the corrupted-JSON case already covered (e.g.
  app killed mid-`completeChapter`).

## Architectural decisions log

- **Single Gradle module for the MVP.** A multi-module split (`core`, `feature-*`) is
  not warranted yet at this scope; revisit if build times or team size grow.
- **No dark theme variant.** The target audience and warm/storybook visual language
  make a single bright palette the right default; can revisit as an accessibility
  option later.
- **Kotlin 1.9.24 / Compose Compiler via `composeOptions`** rather than Kotlin 2.0's
  `org.jetbrains.kotlin.plugin.compose` plugin, to keep the dependency graph on a
  well-established, widely-documented combination for the initial scaffold.
- **DataStore Preferences + kotlinx.serialization JSON, not Room, for the save file.**
  The whole save is one small `PlayerProfile` blob (character choice, ≤6 chapters of
  progress, a handful of badge/scripture-card ids) — no relational queries are ever
  needed, so Room's entities/DAOs/migrations would be pure overhead at this scale.
- **Manual DI (`AppContainer` + `viewModelFactory`), not Hilt.** The dependency graph
  is small; a DI framework would be an unnecessary dependency at this scope (spec
  section 5). `AppContainer` grows one property per milestone, never speculatively.
- **Domain models' persisted collection keys are stable strings/enum names, never
  ordinals.** `ChapterId` serializes by name; `Badge`/`ScriptureCard` ids are plain
  `String`s resolved against a static catalog at display time. This matters for
  forward-compatible saves — renaming an enum constant used in already-saved data
  would silently lose that data.
- **`CharacterCustomization` omits the spec's `accessories` field for now.** YAGNI —
  add it once there's an actual accessories feature to back it.
- **An unlocked chapter with no built content still routes to `ComingSoonScreen`**
  rather than being artificially kept `LOCKED`. `ChapterStatus` reflects real
  progression state (so future unlock-rule changes don't need screen changes); which
  chapters have a real destination is a separate, small allowlist inside the NavHost.
- **Puzzle packages (`game/puzzles/*`) are pure Kotlin, no Compose/Android dependency.**
  Directly unit-testable without instrumentation, and reusable by future chapters by
  supplying new content — no shared "engine" abstraction was built ahead of a second
  chapter actually needing one.
- **Tap-to-match (not drag-and-drop) for Animal Matching.** Spec section 9 allows
  either for that scene; picking tap-to-match means the drag-gesture code only has to
  be built once, for Organize the Ark, where the spec is explicit about drag-and-drop.
- **Per-scene navigation routes sharing one graph-scoped ViewModel**, not a single
  route with an internal scene-index. System Back pops one scene at a time without a
  hand-rolled `BackHandler`, and resuming mid-adventure later (Milestone 5) is just
  "navigate straight to route X" — no new mechanism needed.
- **Genesis 6:22 uses the World English Bible (WEB) text**, a modern public-domain
  translation — the user's explicit decision, since not every Bible translation is
  freely licensed for redistribution (spec section 9's caution).
