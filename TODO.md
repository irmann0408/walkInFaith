# TODO

Check this file first when picking this project back up. If anything
here suggests `docs/PROJECT_STATUS.md` needs updating, ask before editing
it — don't update it automatically.

## Publish v1.0 to Google Play Store

### Done
- [x] App icon (real art, adaptive + legacy mipmaps + Play Store 512×512) — `app-icon/`
- [x] Release signing keystore generated, `signingConfig` wired in `app/build.gradle.kts`
      (credentials in gitignored `keystore.properties`, keystore itself in gitignored
      `/keystore/` — **back these up outside this project folder**, losing them means
      never being able to publish an update to this app listing again)
- [x] Signed release AAB/APK built and verified working on-device —
      `app/build/outputs/bundle/release/app-release.aab`
- [x] Privacy Policy live at https://irmann0408.github.io/walkInFaith/privacy-policy.html
- [x] Store listing copy drafted — `docs/play-store-listing.md` (name, short/full
      description, category, content rating guidance, Data Safety guidance, contact email)
- [x] 6 store screenshots captured — `store-screenshots/`
- [x] Illustrated character style gated off for v1.0 (see "v2.0 Plan" below)
- [x] Play Console developer account registered — **waiting on Google's verification**

### Still open
- [ ] **Feature graphic (1024×500)** — not made yet, needs real graphic design (not
      something that can be generated the way the app icon was assembled). Could reuse
      the app icon's ark scene extended/cropped wider, or a simple banner in the same
      style.
- [ ] Once Play Console verification clears: create the app listing, upload the AAB,
      paste in the drafted copy from `docs/play-store-listing.md`, upload the icon
      (`app-icon/play-store-icon-512.png`) and screenshots (`store-screenshots/`)
- [ ] Fill out the content rating questionnaire and Data Safety form in Play Console
      (answers guidance already drafted in `docs/play-store-listing.md` — "no data
      collected" across the board, matches the app's actual empty-permissions manifest)
- [ ] Accept Google Play Families Policy declarations (this is a children's app)
- [ ] **Double-check `compileSdk`/`targetSdk` (currently 34) against whatever Play
      Console's current minimum target-API requirement actually is at submission
      time** — this shifts over time and wasn't re-verified against live policy
- [ ] Submit for review; respond to any Google review feedback

## v2.0 Plan

### Illustrated character style — re-enable
Fully built and working, currently gated off for v1.0. To bring back:
- [ ] Flip `CHARACTER_STYLE_ILLUSTRATED_ENABLED` to `true` in
      `app/src/main/java/com/bibleadventures/domain/model/CharacterCustomization.kt`
- [ ] Restore the removed "Character Style" `OptionPicker` row in
      `app/src/main/java/com/bibleadventures/ui/screens/character/CharacterScreen.kt`'s
      `CharacterContent` (the spot is marked with a comment pointing back at this)
- [ ] Re-test: `./gradlew build`, full instrumented suite, on-device spot check of
      style switching + all 5 clothing colors × 4 hairstyles × 2 appearances

### Known gaps in the Illustrated art, if addressing before/alongside re-enabling
- [ ] Skin Tone has no illustrated art variants — Classic-only currently. Either
      commission tone variants or decide Illustrated mode intentionally has one fixed
      skin tone per appearance.
- [ ] The girl Robe-family art (all 5 colors) shows the character holding a
      knife-shaped prop alongside a stick — currently being treated as "a stick" per
      an explicit call, but a redone, unambiguous asset would resolve this cleanly if
      it becomes available.
- [ ] Only 5 clothing colors × 2 appearances × 4 hairstyles exist for Illustrated mode
      (boy always tunic, girl always robe) — no additional outfits/colors/shapes
      planned unless specifically requested.

### Other open-ended future work (not scoped to v2.0 specifically)
- [ ] Replace the rest of the app's placeholder art — animals, supplies, badges,
      backgrounds are still simple flat vector shapes (Noah's Ark chapter and others).
      Same `*-art/` staging-folder workflow used for the character/icon art could be
      reused here.
- [ ] Make the Play Store feature graphic (see Publish section above)
- [ ] No other concrete backlog items are currently queued — next work beyond the
      above should come from a fresh round of playtesting or a new feature request.
