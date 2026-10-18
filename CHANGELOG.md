# Changelog

All notable changes to MoreTNT+. The format follows Keep a Changelog; versions follow semantic versioning.

## [1.0.0] - 2026-10-18

### Added

- 40 TNT types across ten families, each with its own mechanic, radius, fuse and settings: structural and physics
  (7), terrain morphing (5), entity and status (4), timing and logic (4), visual and audio (4), utility and economy
  (4), military (2), and a ten-tier nuclear family.
- 18 crafting components, with recipes and textures. Nuclear and the strongest TNT need them.
- Shapeless and shaped recipes for every TNT and component, defined in `tnts.yml` and `items.yml`.
- Placed-TNT registry with persistence; per-block custom display; fuse links; directions; teleport targets.
- Fuse Linker tool for linking placed TNT.
- The `/mtnt` command: give, list, catalogue GUI, info, aim, target, unlink, count, reload.
- Staged block pipeline with WorldEdit or FastAsyncWorldEdit, streaming generators for large craters, and budgets
  in `config.yml`.
- PacketEvents-based client-only effects: per-player daytime, and a 5x5 array of client-side beacons for the nuclear beams.
- Optional WorldGuard (TNT flag) and GriefPrevention (claims) integration, with per-tick caching and an on/off switch per plugin.
- A resource pack generator that draws a texture per TNT and component.
- Modules: Api, Core, Spigot, ResourcePack. Maven profiles for Spigot-API 26.1, 26.2, 26.3 and 1.21.11.
- Documentation under `docs/`.

### Fixed before release

- Cancelled explosions no longer ignite placed TNT; chains respect other plugins' decisions.
- Placed TNT displays are centred on their block.
- A refused detonation returns its TNT item; the chain-depth limit is enforced.
- Placed TNT and fuse links are kept for worlds that load later, and are cleaned up on break and overwrite.
- `/mtnt target` takes coordinates; proximity arming persists across restarts.
- A failing block job is abandoned without stalling the queue.

### Verified

- Compiles against all four targets. Unit tests (16) pass.
- Enables on Paper 26.1.2, 26.2, 26.3 and 1.21.11 with WorldEdit 7.4.6 and PacketEvents 2.14.0.
- Gameplay has not been exercised in a client yet; see `docs/COMPATIBILITY.md`.
