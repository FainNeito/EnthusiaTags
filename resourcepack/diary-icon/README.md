# Diary advancement icon

Source artwork: `journal_quill.aseprite`, exported losslessly at 16x16.

The advancement now uses Minecraft's 1.21.4+ `item_model` component directly:
`enthusia:journal_quill`.

For Nexo, place `Enthusia-Diary-Icon-Nexo-Assets.zip` in:
`plugins/Nexo/pack/external_packs/`

Then rebuild/reload Nexo's resource pack and have clients accept the updated pack.
The ZIP includes the required `assets/enthusia/items/journal_quill.json` item-model
declaration, baked model, and texture. The YAML file is optional and only defines
a hidden Nexo item pointing at the same item model.
