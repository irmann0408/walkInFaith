# Project Status

Last updated: 2026-08-10 (Milestone 4 addendum: content variety, decoys, context cards)

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

### Milestone 4 addendum — content variety, decoys, and story context cards
Follow-up pass on Noah's Ark: the "select/place everything provided" mini-games
(Find the Animals, Gather Supplies, Organize the Ark) had no real discrimination
required, which read as aimed at a younger audience than the 7+ target. Animal
Matching and Find Missing Items were left untouched (Matching already requires
finding a matching pair among many; Find Missing Items was excluded by the user's
explicit choice). Adding a player name to the Character screen was considered and
explicitly **not** done — spec section 16 (Child Safety) lists "Name" as personal
information the app must not request without justification, and none exists yet.
- **More real content**: `animals` grew from 6 to 8 (added camel, monkey); `supplies`
  grew from 4 to 6 (added honey, rope). Both flow automatically into Find the
  Animals, Animal Matching, and Gather Supplies, since those screens already iterate
  `NoahsArkContent` generically. `hiddenItems` stayed a 1:1 mirror of `supplies` (now
  6, with two new fractional positions). `sortableItems`' real-item count was
  deliberately left at 6 (unchanged) — a drag gesture is heavier than a tap, so only
  a decoy was added there rather than growing the real-item count too.
- **Decoy items** (`game/stories/NoahsArkContent.kt`'s new `DecoyItemDef`,
  `findAnimalsDecoys`/`gatherSuppliesDecoys`) — a rock, a toy ball, and (via
  `sortableItems`' new nullable `categoryKey`) a hammer for Organize the Ark. Never
  required for completion, never penalized, always stay interactive, and each shows
  its own scene-specific feedback text (not the shared "Try another one!") —
  `feedback_not_an_animal`/`feedback_not_a_supply`/`feedback_doesnt_belong`.
  `DragSortGameState`/`DragSortGame` gained `SortOutcome.NOT_SORTABLE` and a nullable
  `SortableItem.categoryKey` to represent "belongs in no bin," consistent with the
  puzzle packages' existing "never FAILED" design. Find the Animals/Gather Supplies
  have no dedicated engine, so their decoy state is two plain `DecoyTapOutcome`
  fields on `NoahsArkUiState` rather than a new pure-Kotlin engine package — a third
  micro-engine for "was a decoy tapped, yes/no" would go against this codebase's
  existing precedent of not building shared abstractions ahead of real need.
- **Story context cards**: one new reusable `ui/components/StoryBeatScreen.kt`
  (title + 1-2 narrative lines + Continue, modeled on `NoahsArkIntroScreen` but
  without the character render, to stay a quick narrator caption) — wired directly
  into `BibleAdventuresNavHost.kt`'s `noahsArkGraph()` three times as new
  `Destination.NoahsArk.{FindAnimalsContext,GatherSuppliesContext,OrganizeArkContext}`
  routes, immediately before their respective puzzle scenes, so the player has
  narrative grounding for what belongs before any decoy shows up.
- Tests: updated `NoahsArkViewModelTest`'s initial-state counts (16 matching items, 7
  sortable items, 6 hidden items) and added decoy-outcome tests; `DragSortGameTest`
  gained `NOT_SORTABLE`/decoy-completion/repeatable-retry tests; `NoahsArkFlowTest`
  gained one more Continue tap before each of the three puzzle scenes and now
  explicitly skips decoys in its "tap/drag everything" loops; new
  `NoahsArkDecoyInteractionTest` (instrumented) covers decoy feedback text,
  never-completes-early, and stays-interactive-after-repeated-taps for all three
  decoys, kept separate from `NoahsArkFlowTest`'s happy-path walk.
- Three item-tray layout bugs, found in three passes of on-device testing (same
  pattern as M4's original two on-device-only bugs): (1) `AnimalMatchingGameTest`
  still assumed Intro's Continue led straight to Find Animals, predating the new
  context card in between — fixed with an extra Continue tap. (2) `NoahsArkGatherSuppliesScreen.kt`'s
  supplies tray was a plain non-scrollable `Row`, already borderline-overflowing at 4
  items — an interim fix moved it (and Organize the Ark's pre-existing tray) to
  `LazyRow`. (3) That interim fix was itself wrong: `LazyRow` virtualizes off-screen
  children out of the semantics tree entirely, so once a tray held more items than
  fit on a phone's width (7, after the decoy), the extra items weren't just visually
  clipped — they didn't exist for either the accessibility tree or a sighted player
  to find, and there was no scroll affordance hinting they were there at all. A real
  user hit exactly this on Gather Supplies (found Honey/Rope unreachable, blocked
  from completing the scene). Final fix: both trays now wrap into a static multi-row
  grid (`tiles.chunked(4)` / `unplacedItems.chunked(4)`, no laziness, no scrolling) —
  every item is always visible and always in the tree, consistent with spec section
  13's "simple, discoverable navigation" and the same reasoning Find the Animals'
  grid already followed. All 11 instrumented tests pass on-device (Samsung Galaxy
  S25 Ultra); one flaky, pre-existing failure unrelated to this change
  (`CharacterNavigationTest`, on a screen untouched here) surfaced in one of three
  runs and did not reproduce on the others.

### Milestone 4 addendum 2 — Find the Missing Items: real hidden-object search
Find the Missing Items previously wasn't actually a hidden-object game — the spec's
"avoid frustrating pixel-hunting" caution was taken further than intended, so every
item sat fully visible on a plain two-tone background, making the scene a "tap
everything" checklist like Find the Animals/Gather Supplies rather than a search.
Per the user's explicit direction ("treat it like finding Waldo"), `bg_noahs_ark_missing_items.xml`
was replaced with a busy, abstract camouflage pattern — 8 blended, overlapping color
blobs (`android:fillAlpha`) plus ~30 scattered accent dots across a 300x300 viewport,
still simple placeholder vector shapes, no representational art needed. `HiddenItemTarget`'s
icon (`NoahsArkMissingItemsScreen.kt`) shrank from 40dp to 32dp and dropped to 0.85
alpha so items visually blend into the clutter instead of floating on top of it —
the 48dp tap target itself is unchanged, so the *search* got harder without anything
becoming harder to actually tap once spotted, preserving spec section 9's real intent.
Instructions text updated to set expectations ("hiding somewhere... look closely").
No logic/test changes: found/not-found state, positions, content descriptions, and
the no-penalty/always-recoverable design are all unchanged, so `NoahsArkFlowTest`
needed no updates — confirmed passing on-device after the change.

### Milestone 4 addendum 3 — visible name labels on every mini-game tile
Every tile across the five Noah's Ark mini-games (Find the Animals, Animal Matching,
Gather Supplies, Organize the Ark, Find the Missing Items) now shows the item's name
as a small caption under its icon, not just an accessibility content description —
so a child who doesn't yet recognize a species/object by its placeholder icon alone
can read what it is. The caption is purely visual: each tile's existing `contentDescription`
already covers screen readers, so the new `Text` is `Modifier.clearAndSetSemantics {}`
to avoid a duplicate/redundant TalkBack announcement, mirroring how each tile's
`Image` already uses `contentDescription = null` for the same reason.
- Find the Animals and Animal Matching switched their `LazyVerticalGrid`s to the same
  static wrapped-grid pattern (`items.chunked(n)`) already used by Gather Supplies and
  Organize the Ark, since taller (icon + label) tiles made virtualization overflow —
  and off-screen-but-uncomposed tiles were exactly the Gather Supplies bug from
  earlier in this milestone. Both now wrap in `Modifier.verticalScroll(...)` as a
  normal, discoverable vertical scroll if content ever exceeds the screen — unlike the
  earlier `LazyRow` bug, nothing here is hidden from the tree or unreachable.
- Organize the Ark's `DraggableSortItem` label sits in a `Column` below the
  draggable `Box` rather than inside it, so the label doesn't affect the drag/drop
  hit-testing math (which uses the draggable box's own center) — during a drag the
  icon lifts away while the label stays put in the tray slot.
- Find the Missing Items: the label only appears once an item is *found* (next to
  the checkmark) — showing it upfront would print the answer next to the hidden
  icon and undo the search difficulty added in the previous addendum.
- No changes to any puzzle engine, `NoahsArkContent`, or test files — content
  descriptions, positions, and win conditions are all unchanged, purely additive UI.
  All 11 instrumented tests still exercise every labeled tile via their existing
  `onNodeWithContentDescription` lookups; confirmed passing on-device (one flaky,
  pre-existing, unrelated failure on `WorldMapNavigationTest` — a screen untouched
  by any of this milestone's work — seen intermittently across four separate runs).

### Milestone 4 addendum 4 — Animal Matching becomes a real memory/concentration game
Per the user's direction, Animal Matching changed from "always-face-up tap two
matching icons" to a classic memory game: cards start face down, tapping flips one
up, a second tap compares it against the first. A match stays face up forever; a
mismatch stays face up too (so the player gets a real look, with "Try another one!"
showing) — the *next* tap anywhere flips the mismatched pair back down and starts a
fresh selection, at the player's own pace rather than a forced timer. The label
caption added in the previous addendum was removed from this scene specifically
(it would trivialize a memory game by printing the answer under a face-down card).
- `game/puzzles/matching/MatchingGameState.kt`: `selectedId: String?` became
  `selectedIds: List<String>` (0-2 unresolved face-up cards); added
  `isFaceUp(id)` (`matchedIds` or `selectedIds`).
- `game/puzzles/matching/MatchingGame.kt`: `onItemTapped` now clears a shown
  mismatch (`lastOutcome == TRY_AGAIN`) at the start of the *next* tap before
  processing that tap as a fresh first selection — no timer/coroutine needed, the
  engine stays fully synchronous and pure. Matched cards are still an immediate no-op.
- New placeholder drawable `ic_card_back.xml` — a simple six-petal rosette pattern
  (abstract, not representational), shown instead of the real icon while a card is
  face down.
- `NoahsArkMatchingScreen.kt`'s `MatchTile` renders `ic_card_back` or the real icon
  based on `MatchingGameState.isFaceUp`; the label `Column`/`Text` from the previous
  addendum was reverted back to a plain `Box`.
- **Judgment call:** each tile's `contentDescription` stays the item's name at all
  times (even face down), rather than something generic like "hidden card." A fully
  fair implementation would hide the identity from screen readers too until flipped,
  but that would require rewriting every content-description-based lookup in the
  existing instrumented test suite (`NoahsArkFlowTest`, `AnimalMatchingGameTest`) to
  a position/tag-based scheme instead. Kept as-is for now — flagged here rather than
  silently decided, easy to revisit.
- Card shuffle: already happened for free — `NoahsArkViewModel.createInitialState()`
  already calls `.shuffled()` once per fresh `NoahsArkViewModel` instance, and a new
  instance is created every time the chapter is (re)started, so "re-arrange the cards
  randomly every new game" needed no code change.
- Tests: rewrote `MatchingGameTest.kt` for the new face-down/face-up/selectedIds
  model (added a "cards start face down" case and a "next tap after a mismatch
  flips it back down" case). **No instrumented test changes were needed at all** —
  `AnimalMatchingGameTest`'s existing mismatch-then-correct-match sequence and
  `NoahsArkFlowTest`'s always-tap-the-same-animal-twice sequence both already match
  the new engine's tap semantics exactly; traced by hand and confirmed passing
  on-device.

### Milestone 4 addendum 5 — shuffle layout order for the remaining mini-games
Animal Matching already re-shuffled every game; the other four scenes (Find the
Animals, Gather Supplies, Organize the Ark, Find the Missing Items) always showed
their icons in the exact same order/position, which the user flagged directly.
Fixed by shuffling once per fresh game in `NoahsArkViewModel.createInitialState()`
(the same place Matching's shuffle already lived), not inside `NoahsArkContent`
itself — that `object`'s `val` lists are computed once per process lifetime, so
shuffling there would only randomize once per app install/launch, not per
playthrough (a new `NoahsArkViewModel` — and so a fresh shuffle — is created every
time the chapter is (re)started, same mechanism the Matching shuffle already relies on).
- Find the Animals / Gather Supplies: new `NoahsArkUiState.findAnimalsOrder` /
  `.gatherSuppliesOrder` (`List<String>`, real item ids + decoy id shuffled
  *together*, not each list shuffled separately then concatenated — otherwise the
  decoy would always land in the same last-in-grid slot every game). Each screen
  now builds its tile list by walking this order and looking up each id against
  `NoahsArkContent`, instead of iterating the static content lists directly.
- Organize the Ark: `NoahsArkContent.sortableItems.shuffled()` at construction time
  in `createInitialState()`, so the tray order (and the decoy hammer's position in
  it) varies — no screen changes needed, `NoahsArkOrganizeArkScreen.kt` already just
  renders whatever order `dragSortState.items` holds.
- Find the Missing Items: positions themselves stay fixed (they're hand-placed to
  fit the background and not overlap), but *which item lands on which position* is
  now shuffled — `NoahsArkContent.hiddenItems.map { it.position }.shuffled()`, then
  zipped back onto the id/icon list in original order. No screen changes needed here
  either, same reasoning as Organize the Ark.
- Tests: added two `NoahsArkViewModelTest` cases confirming the shuffles are true
  permutations (same id/position sets, just reordered) rather than losing or
  duplicating anything. No instrumented test changes needed — every existing lookup
  is by `contentDescription` (name), which is order-independent by construction;
  confirmed passing on-device.

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
