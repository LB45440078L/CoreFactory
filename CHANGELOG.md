# Changelog

All notable changes to MoreTNT+. The format follows Keep a Changelog; versions follow semantic versioning.

## [Unreleased]

### Added

- `docs/TEXTURES.md`: the specification for every TNT and component texture, the tools, and the process to create,
  check and ship one.
- `ClientEffects.setClientWeather`: rain and thunder shown to one player.

### Changed

- Nuclear craters are now shaped like the real thing. Tiers 1 to 5 (surface bursts) cut a shallow saucer with a
  compacted floor and fused glass at the centre. Tiers 6 to 10 (thermonuclear) cut a deep hemisphere with a sinking rim.
  Both clear everything above the ground out to `flatten-radius`. The old `crater-fraction` setting is replaced by
  `shape`, `crater-radius`, `crater-depth`, `sink-depth` and `flatten-radius`.
- Nuclear radii are several times larger (Davy Crockett 6 to 14 blocks, Tsar Bomba 150 to 220). Only the nuclear family
  changed; other TNT types keep their radii.

### Fixed

- Beacon beams for nuclear blasts were not drawn. A client draws a beam only from a beacon with a pyramid, and the
  array had none. Each beacon now sits on a client-side 3 × 3 iron platform. The beam scan also stopped at the beacon
  itself, so each column gets a client-side light block at the top of the world, and the beam runs to the sky. The
  platform hangs one block above the ground, because the blast's own ground changes would overwrite it.
- The client clock did not hold at noon: the server re-sent the real time every second and undid a plain packet. The
  outgoing time packets are now rewritten for that player while the hold lasts, and the clock slides back to the real
  time over 20 seconds.
- The rain now stops for players within 3 times the radius during the blast, and comes back afterwards.

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
- Flint and steel (and fire charges) on a placed custom TNT now light it through MoreTNT+. Before, vanilla lit the block
  and its plain explosion replaced the custom mechanic, which is why most types appeared to do nothing.
- Custom TNT that vanilla primes anyway (arrows, fire, lava) runs its own mechanic instead of a vanilla blast.
- Vitrify no longer throws when it plays its particle effect (FLASH needs a colour).
- `debug: true` logs ignitions, explosions, detonations and blast sizes.

### Verified

- Compiles against all four targets. Unit tests (16) pass.
- Enables on Paper 26.1.2, 26.2, 26.3 and 1.21.11 with WorldEdit 7.4.6 and PacketEvents 2.14.0.
- Gameplay has not been exercised in a client yet; see `docs/COMPATIBILITY.md`.
