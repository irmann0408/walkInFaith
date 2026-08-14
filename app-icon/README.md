# App Icon

Drop the source app icon image here. This folder is just a staging area
— files here aren't compiled into the app on their own. Once ready, it
gets wired into two places:

1. **In-app adaptive icon** — replaces the placeholder shapes in
   `app/src/main/res/drawable/ic_launcher_foreground.xml` /
   `ic_launcher_background.xml` (referenced by
   `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`).
2. **Play Store listing icon** — a separate 512×512 PNG uploaded directly
   in Play Console (not part of the app itself).

## What's needed

- Square image, ideally **512×512px or larger**, PNG.
- For the adaptive icon specifically, Android crops/masks the edges
  differently per device — keep the important part of the design inside
  the center ~66% ("safe zone") so it doesn't get clipped.
- Solid or transparent background both work; if there's important detail
  right at the edges on a transparent version, a solid-background variant
  is safer for the adaptive icon's background layer.

Exact filename doesn't matter — just drop it in and let me know.
