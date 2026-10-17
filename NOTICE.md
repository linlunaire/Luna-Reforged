# Third-party and modification notices

Luna Reforged is an independent native Minecraft adaptation, not an official Auxilor release.

## Reforges

- Upstream: https://github.com/Auxilor/Reforges
- Inspected commit: `8ee14ce9c6d979d5fa5a7e71a6e2e4df964d2d4e`.
- Copyright remains with Auxilor / original Reforges contributors.
- Upstream `LICENSE.md` is retained as `LICENSE` (GPL version 3).
- The sixteen default reforge identities and gameplay values informed `ReforgeRules.kt`.
- Version 0.3.0 also adapts the sixteen gradient palettes, description layout, random/stone rules and the vanilla-ingredient stone recipes from the same GPL Reforges commit.
- Bukkit, eco, libreforge, GUI inventories, persistent-data containers and economy integrations have been replaced with native Minecraft code.
- New work: dual-version loader integration, data components, transactional menus, XP policies, voxel model geometry, screen geometry, translations, native recipes and verification tools.
- New modifications copyright (c) 2026 linlunaire; distributed under GPL-3.0-only for the reforge mod.

For behavioral comparison, libreforge 2026.39 (tag commit `957055ca5aa5c809d8af98fdbb6c3f58beb13f4d`) was inspected for trigger boundaries and attribute semantics. It is under its own Business Source License. Its source and binaries are not copied into this project's implementation or distributions; the native hooks, text parser and configuration schema are independently written here. Reference checkouts are excluded from source archives.

## Kotlin LunaCore dependency

- Upstream: https://github.com/linlunaire/Kotlin-LunaCore, version 0.4.1.
- License: LGPL-3.0-or-later. Complete sources, license texts and third-party notices are distributed by the LunaCore project.
- LunaCore supplies the shared Kotlin runtime as a separately installed mod for both supported Minecraft versions. This project does not bundle its source or binaries.

## Assets

All table model geometry and GUI geometry in this repository are newly authored. JSON item and block models reference vanilla Minecraft textures by identifier; those texture files are not included. No artwork from other reforging mods is copied.

The lunar GUI background is included at `assets/luna_reforged/textures/gui/lunar_backdrop.png`. Its layout and in-game dimensions are documented in `docs/visual-design.md`.

Apotheosis life-steal/status-effect descriptions and owo-lib presentation patterns were inspected as design references only; no code or assets from those projects are included. The sixteen new native affixes, bounded combat procs and shared stat descriptions are newly implemented here.
