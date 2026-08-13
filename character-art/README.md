# Character Art

Drop character design source images here (PNG, transparent background).
This folder is just a staging area — files here aren't compiled into the
app on their own. Once art is ready to use, it gets copied into
`app/src/main/res/drawable/` under a matching resource name and wired
into `app/src/main/java/com/bibleadventures/ui/components/CharacterPreview.kt`.

## Current design: boy = tunic, girl = robe, full hairstyle set

Illustrated mode keeps the outfit simple — the boy always wears a tunic
and the girl always wears a robe, in whichever of the app's 5 clothing
colors is picked (Blue, Green, Red, Yellow, Purple). Full art now exists
for **every hairstyle too** (Short, Curly, Braided, Ponytail) — 5 colors ×
4 hairstyles × 2 appearances = 40 images, all present in
`app/src/main/res/drawable/`.

## Naming

One full-body character render per file, named:

```
character_clothing_<shape>_<appearance>_<color>_<hairstyle>.png
```

- `<shape>`: `tunic` (boy) or `robe` (girl)
- `<appearance>`: `boy` or `girl`
- `<color>`: `blue`, `green`, `red`, `yellow`, or `purple`
- `<hairstyle>`: `short`, `curly`, `pigtail` (maps to the app's "Braided"),
  or `ponytail`

Exact filenames don't have to match perfectly — just say which is which
and it'll get wired in correctly either way.

**Watch for transparency**: a couple of the first hairstyle-variant
exports had an opaque black background instead of a transparent one,
which showed up as a black box behind the character on-device. If new
art gets added later, worth a quick check that the background is
genuinely transparent before wiring it in.
