# Resource pack

Every TNT and component has its own 32 × 32 texture. The textures are painted by Java code (see
[TEXTURES.md](TEXTURES.md)), and the ResourcePack module packs them into a zip that the server hosts and players load.

## Pack format

The plugin supports Paper 26.3 only. The pack format is the number that the server's version expects, and a client that
sees a pack with another number does not apply it. Every custom item then falls back to its vanilla look.

The number is set by the Maven profile `api-26.3`, which is the default, through the `pack.format` property of the root POM:

| Profile | Server | `pack_format` | Where the number comes from |
|---|---|---|---|
| `api-26.3` (default) | Paper 26.3 | **97** | The server jar's `version.json`: `pack_version.resource_major` is 97. |

The other `api-*` profiles still build, but they are not supported, and no pack format is listed for them.

To check the number for a server, open its server jar (or the `META-INF/versions/*.jar` inside it) and read
`version.json`: the `resource_major` value under `pack_version` is the pack format.

## What is in the pack

| File | Role |
|---|---|
| `pack.mcmeta` | Pack format and description. |
| `pack.png` | Pack icon. |
| `corefactory-pack.properties` | Version, build and pack format of the bundled pack. The plugin reads it to decide whether an installed pack is outdated. |
| `assets/corefactory/items/<kind>/<id>.json` | Item definition: points the item at its model. The kinds are `tnt`, `component`, `machine`, `ore` and `particle`. |
| `assets/corefactory/models/item/<kind>/<id>.json` | Item model: a cube for TNT, a flat sprite for a component or an effect, or a 3D model for a machine. |
| `assets/corefactory/models/block/ore/<id>.json` | Block model of an ore: a full cube with the ore texture. |
| `assets/minecraft/blockstates/<host>.json` | Blockstate override of the vanilla host block of an ore. See the limits below. |
| `assets/corefactory/textures/block/tnt/<id>.png` | The 32 × 32 TNT texture. |
| `assets/corefactory/textures/block/machine/<id>/<face>.png` | The 32 × 32 texture of each of the six faces of a machine or drill. |
| `assets/corefactory/textures/block/ore/<id>.png` | The 16 × 16 ore block texture. |
| `assets/corefactory/textures/item/component/<id>.png` | The 32 × 32 component sprite. Lab tools and drills also have one, for the inventory. An animated sprite is a strip of square frames, with a `.mcmeta` beside it. |
| `assets/corefactory/textures/item/particle/<name>.png` | The 32 × 32 effect sprite. |
| `assets/corefactory/textures/gui/advancements/background.png` | The background of the achievements tab. |

Each turret has a 3D model for each of its five levels, and the tall levels 3 to 5 have an upper model with `_top` added, such as
`machine/machine_gun_turret_3_top`. The names are in `ModelKeys`, and they must match the pack. The four upgrade kits are flat sprites,
`textures/item/component/turret_upgrade_2.png` to `turret_upgrade_5.png`. See [TEXTURES.md](TEXTURES.md#turret-models).

Each of the seven artillery pieces is two models, a body and a barrel: `machine/artillery_1_body` and `machine/artillery_1_barrel`, up to
`machine/artillery_7_body` and `machine/artillery_7_barrel`. The body is the carriage, the turret ring and the base, and the plugin turns it
to the aim. The barrel is the barrel and its breech, and the plugin tilts it to the elevation about its trunnion
(`ArtilleryArt.trunnionBlocks`). Each level is as tall as it was, up to 3.75 blocks. Every model keeps its elements between -16 and 32 units,
so it is written from y -16 and the plugin lifts its display one block (`ArtilleryArt.displayLift`). Level VII also has a top model,
`machine/artillery_7_top`, which turns with the body and does not tilt. Its barrel and its top are written in the upper frame, so the plugin
places both three blocks above the body (`ArtilleryArt.displayOffset`). The command center is `machine/command_center`, a table with a screen
the plugin fills with a hologram. The shells and the propellant charges are flat sprites, `component/<id>`, and so are the inventory sprites of
the pieces, `component/artillery_1` to `component/artillery_7`. See [TEXTURES.md](TEXTURES.md#artillery-models).

The water system has a model for every orientation, so the game draws each one with no display rotation. The ids are those of
`WaterModels.all()` in CoreFactory-Core, such as `machine/water_pipe_bend_south_west`, `machine/water_splitter_t_north_up` and
`machine/water_pump_3_east`. A pump of level IV or V is two blocks tall, so each of its fronts has an upper model with `_top` added,
such as `machine/water_pump_4_north_top`. The inventory sprites are unchanged: `water_pump`, `water_pipe`, `water_splitter_t` and
`water_splitter_x`, and the upgrade kits.

The plugin sets the item model on each of its items: `corefactory:tnt/<id>`, `corefactory:component/<id>` or, for an ore
block item, `corefactory:ore/<id>`. The definitions above resolve those names.

A placed TNT block is still a vanilla TNT block, which the client draws with the vanilla texture. To show the custom
look on the ground, the plugin puts an **item display** over each placed block and scales it slightly above 1, so it
covers the vanilla texture. It is turned off with `display.custom-textures: false`.

## Sounds

The pack has 41 sounds under `assets/corefactory/sounds/`, and `sounds.json` names each one as an event
`corefactory:<key>`. There is one sound for each generator kind, each drill kind and each process machine, plus the turret,
laser, press, pylon and pump sounds. Nine of the 41 are recordings from CC0 sources (OpenGameArt and Kenney), and the other 32 are
procedural. The pump sound is `water_pump`: a two-second procedural loop (a turbine rumble, a motor hum and a soft vane swish), in
`art/sounds/water_pump.ogg`. It is registered in `SoundPack`, and a running pump plays it every two seconds. `tools/sounds/make_sounds.py` builds the OGG files in `art/sounds`: it downloads the recordings listed in
`tools/sounds/sources.json` (checking each file's SHA-256), edits them with ffmpeg, synthesises the procedural ones, and encodes
OGG Vorbis. `SoundPack` copies them into the pack. The table of sources, licences and edits is in
[`art/sounds/SOURCES.md`](../CoreFactory-ResourcePack/art/sounds/SOURCES.md), and the credits are in [CREDITS.md](CREDITS.md). Four keys are packed but not played yet, and two older ones are no longer played; the
list is in [MECHANICS.md](MECHANICS.md#sounds). The script needs an ffmpeg binary at build time (`pip install imageio-ffmpeg` provides
one); the plugin does not need it at run time. The sounds are kept quiet: see [MECHANICS.md](MECHANICS.md#sounds) for which ones
play, and which are not wired to the game yet.

## Texture folders and the atlas

The client packs block textures into a block atlas and item textures into an item atlas. A texture is found through the
atlas whose source folder holds it, so the folder of a PNG decides where a model may use it.

| Folder in the pack | Atlas | Used by |
|---|---|---|
| `textures/block/` | Block atlas | TNT block faces, machine and drill faces, ore blocks. |
| `textures/item/` | Item atlas | Component sprites, effect sprites. |
| `textures/gui/` | Copied as it is | GUI textures, such as the achievements tab background. |

Rules for pack authors:

- A texture that a block model or a 3D item model uses goes under `textures/block/`. Its model names it as
  `corefactory:block/<path>`, with the `textures/` prefix and the `.png` extension removed. The face
  `textures/block/machine/crusher/north.png` is `corefactory:block/machine/crusher/north`.
- A flat sprite goes under `textures/item/`, and its model names it as `corefactory:item/<path>`. Components and effect
  sprites are flat sprites.
- Machine faces are block textures, so they live under `textures/block/machine/<id>/`. They were moved there so that
  the block atlas holds them. Keep them there when you add a machine.
- An animated texture, such as a belt top or an animated machine face, is a vertical strip of square frames stacked top to
  bottom: a 16 × 128 file is eight 16-pixel frames. The builder checks the height and writes a `.mcmeta` beside the PNG with
  the frame time from the descriptor. See [TEXTURES.md](TEXTURES.md#animated-faces).

Every texture a descriptor names must exist, or the build stops with an error. A texture in the wrong folder may not reach
the atlas that its model uses, and the client then shows its missing-texture pattern. This was checked from the file
layout and the builder's code, not on a graphical client.

## Building the pack

`mvn package` builds the zip. It uses the pack format of the active profile:

```bash
mvn clean package                  # api-26.3 (default): pack_format 97
```

The zip is written to `CoreFactory-ResourcePack/target/CoreFactory-ResourcePack.zip`, and the plugin build bundles it into the plugin jar (see below). The build reads the
generated art in `CoreFactory-ResourcePack/art/`. To regenerate that art after changing a painter, see
[TEXTURES.md](TEXTURES.md#2-tools). The art output is generated and committed, so it must be regenerated with `ArtGenerator` after any art change, and the pack is built from it.

The builder can also be run on its own, from the module's `target/classes`:

```
io.corefactory.resourcepack.ResourcePackBuilder <art-dir> <output.zip> <pack-format> <description> <version> <build>
```

## Bundled in the plugin, and RPMS

The plugin jar already contains the pack. `mvn package` builds the ResourcePack module first (Maven orders the modules
from the Paper module's dependency on it), then copies `CoreFactory-ResourcePack.zip` into the jar under
`resource-pack/`.

When the server loads the plugin, before any plugin is enabled, CoreFactory:

1. creates `plugins/CoreFactory/pack/`;
2. copies the bundled pack there as `pack.zip`, but only if it is newer than the one already there (see [Versions and updates](#versions-and-updates)).

The plugin declares `loadbefore: [ResourcePackManager]`, so it is enabled before RPMS as well. RPMS
(ResourcePackManager) reads that exact file: its `compatible_plugins/more_tnt_plus.yml` has
`localPath: CoreFactory/pack/pack.zip`, and RPMS merges the pack into its own output.

To check it on a server: after startup, `plugins/CoreFactory/pack/pack.zip` exists, and RPMS's
`plugins/ResourcePackManager/output/ResourcePackManager_RSP.zip` contains `assets/corefactory/`.

If you also copied the pack into RPMS's `mixer` folder by hand, remove that copy. RPMS would merge the same pack twice,
once from the mixer and once from the compatibility entry.

## Versions and updates

Server owners often replace the plugin jar without deleting the `plugins/CoreFactory` folder. So the plugin decides
for itself whether the pack in `pack/pack.zip` is outdated, and it replaces that file only when the bundled pack is
newer.

Each pack carries its version inside the zip, in `corefactory-pack.properties`:

```properties
version=1.0.0
build=20261019130456
pack-format=97
```

- `version` is the project version, compared number by number: `1.10.0` is newer than `1.9.0`.
- `build` is the build time (`yyyyMMddHHmmss`, from the build machine's clock). Rebuilding without bumping the version
  still gives a newer pack, because the build time is larger. It is compared only when the versions are equal.

On every start, before RPMS, the plugin compares the bundled pack with the installed one:

| Installed pack | Result |
|---|---|
| none | Copied in. |
| older (lower version, or the same version with an older build) | Replaced, and the log says from and to which version. |
| the same version and build | Kept. The log says it is up to date. |
| newer (for example, a newer jar was installed earlier and then the older one was copied in) | Kept. The plugin never downgrades a pack. |
| unreadable, or from before versioning | Replaced. |

To see the state at any time, run `/cf pack` in game or on the console. It shows the bundled and installed versions
and what would happen on the next start. `/cf pack update` applies the same rule immediately; if RPMS has the pack
open, close it and restart the server instead.

## Hosting the pack

Upload the zip to a URL the players can reach, then set it in `server.properties`:

```properties
resource-pack=https://example.com/CoreFactory-ResourcePack-1.0.0.zip
resource-pack-sha1=<sha1 of the zip>
require-resource-pack=false
```

Compute the hash with `certutil -hashfile <zip> SHA1` on Windows or `sha1sum <zip>` elsewhere.

## Changing the pack format

Build the pack for the server's own version. A pack for a different number is not applied, and the textures look
missing. After changing `pack_format`, rebuild the zip, re-host it, update `resource-pack-sha1`, and have players
reconnect to load it.

## Limits

- Only the item and the placed-TNT display use the custom look. The vanilla TNT block that remains beneath, and the
  block-breaking particles, use vanilla TNT.
- An ore block re-textures its host block for every block of that vanilla type in the world. Every glazed terracotta
  block of an ore's colour takes the ore's texture, including any that players placed as decoration. The vanilla
  glazed terracotta item is not changed.
- A player without the resource pack sees the base material (`TNT`) and the item's name and lore, but not the model.
