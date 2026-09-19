# Diary advancement icon

Source artwork: `journal_quill.aseprite` supplied by the user and exported at its native 16x16 resolution.

Install with Nexo:
1. Copy `enthusia_diary_advancement_icon.yml` into the server's Nexo item configuration.
2. Zip the *contents* of `external_pack` so that `assets/` is at the ZIP root, then place that ZIP in `plugins/Nexo/pack/external_packs/`.
3. Regenerate/reload Nexo's resource pack using the server's normal workflow and have clients accept the updated pack.
4. Keep `advancements.diary-icon-custom-model-data: 815002` in EnthusiaTags.

The icon uses WRITABLE_BOOK custom model data 815002. Without the resource pack, Minecraft falls back to the vanilla writable-book icon.
