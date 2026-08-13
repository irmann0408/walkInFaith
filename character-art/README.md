# Character Art

Drop character design source images here (PNG, transparent background).
This folder is just a staging area — files here aren't compiled into the
app on their own. Once art is ready to use, it gets copied into
`app/src/main/res/drawable/` under a matching resource name and wired
into `app/src/main/java/com/bibleadventures/ui/components/CharacterPreview.kt`.

## Current design: boy = tunic, girl = robe

Illustrated mode keeps it simple — the boy always wears a tunic and the
girl always wears a robe, in whichever of the app's 5 clothing colors is
picked (Blue, Green, Red, Yellow, Purple). Full art coverage already
exists for both (see `app/src/main/res/drawable/`):

- `character_clothing_tunic_boy_blue.png`, `_green.png`, `_red.png`, `_yellow.png`, `_purple.png`
- `character_clothing_robe_girl_blue.png`, `_green.png`, `_red.png`, `_yellow.png`, `_purple.png`

## Naming

One full-body character render per file, named:

```
character_clothing_<shape>_<appearance>_<color>.png
```

- `<shape>`: `tunic` (boy) or `robe` (girl)
- `<appearance>`: `boy` or `girl`
- `<color>`: `blue`, `green`, `red`, `yellow`, or `purple`

Exact filenames don't have to match perfectly — just say which is which
and it'll get wired in correctly either way.
