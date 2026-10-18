# MoreTNT+

New TNT for Spigot: 40 TNT types with their own mechanics, 18 crafting components that make the strong ones
hard to build, a configurable recipe for every item, a generated resource pack with a unique texture for each one,
and optional respect for WorldGuard regions and GriefPrevention claims.

- **Targets:** Spigot-API 26.1 (default), 26.2, 26.3 and 1.21.11. One source tree, one Maven profile per target.
- **Runtime:** Java 25. Required: **PacketEvents**, and **WorldEdit or FastAsyncWorldEdit** (either one).
- **Optional:** WorldGuard, GriefPrevention. Used only when installed.
- **Build:** Maven 3.9+, JDK 25.

> The repository is named MoreTNTPlus; the plugin, its jar and its commands are `MoreTNTPlus` and `/mtnt`.

## What it adds

| Family | Types | What they do |
|---|---|---|
| Structural / physics | 7 | Chain reaction, shaped charge, gravity well, implosion, fuse-linked, delayed cascade, 4-dimensional |
| Terrain morphing | 5 | Vitrify, liquefy, terraform, randomize, sculpt |
| Entity / status | 4 | Concussion, launch, teleport, gravity flip |
| Timing / logic | 4 | Conditional, proximity fuse, pulse, mirror |
| Visual / audio | 4 | Shockwave ring, debris, echo, silent |
| Utility / economy | 4 | Harvest, sorting, marker, anchor |
| Military | 2 | Biological, napalm |
| Nuclear | 10 | Davy Crockett to Tsar Bomba: countdowns, fallout, a crater cut to bedrock, debris, and a beacon array in the sky |

Every type is a row in [`tnts.yml`](MoreTNTPlus-Spigot/src/main/resources/tnts.yml): its mechanic, radius, fuse,
settings, recipe and texture can be changed without touching code. The full list is in
[docs/MECHANICS.md](docs/MECHANICS.md).

### Client-side beacon beams

The nuclear tiers draw a 5 × 5 array of beacons, three blocks apart, on the surface around the blast. The beacons
are sent to each nearby player with PacketEvents and are never written to the world, so the server's terrain does
not change. When the player's view is restored, the real blocks are sent back. Each beacon draws its own beam up to
the sky, and the player's client clock is set to noon for the same period.

### Claims and integrations

- **WorldGuard:** a blast leaves alone any block inside a region whose `tnt` flag denies TNT.
- **GriefPrevention:** a blast leaves alone any block inside a claim.

Both checks are built for speed. WorldGuard is asked once per chunk per tick, and only where a region with TNT
rules touches the chunk, so most of the map costs one map lookup per block. GriefPrevention is asked once per
block column per tick, not once per block. With neither plugin installed, the checks are not run at all. Each can
be switched off under `integrations:` in `config.yml`.

## Quick start

```bash
# Build for the default target (Spigot-API 26.1). Produces the plugin jar and the resource pack zip.
mvn clean package

# Other targets
mvn clean package -Papi-26.2
mvn clean package -Papi-26.3
mvn clean package -Papi-1.21.11
```

Outputs:

- `MoreTNTPlus-Spigot/target/MoreTNTPlus-Spigot-1.0.0.jar`: the plugin. Put it in `plugins/`.
- `MoreTNTPlus-ResourcePack/target/MoreTNTPlus-ResourcePack-1.0.0.zip`: the textures. Host it and set
  `resource-pack` in `server.properties` (see [docs/RESOURCE_PACK.md](docs/RESOURCE_PACK.md)).

Install `PacketEvents` and `WorldEdit` (or `FastAsyncWorldEdit`) alongside it, start the server, and run
`/mtnt gui` to see the catalogue. Then `/mtnt give chain_reaction_tnt` to get one. WorldGuard and GriefPrevention
are optional; install them and the blasts will respect their protection automatically.

## Status

| Area | State |
|---|---|
| Compilation, all four targets | Passes |
| Unit tests (16) | Pass |
| Enable on Paper 26.1.2, 26.2, 26.3 and 1.21.11, with WorldEdit and PacketEvents | Passes; every server registers 58 recipes |
| GriefPrevention integration, enable on Paper 1.21.11 | Attaches; no warnings |
| WorldGuard integration | Compiles against the WorldGuard 7.0.20 API; not run on a server yet |
| Gameplay in a client (placing, lighting, blasts, beacon beams, resource pack) | Not yet exercised |

The full account, including known limitations, is in [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).

## Documentation

| Document | Covers |
|---|---|
| [docs/MECHANICS.md](docs/MECHANICS.md) | Every mechanic, what it does, its settings, its radius meaning, claims and integrations |
| [docs/CRAFTING.md](docs/CRAFTING.md) | The components, their recipes, the recipe grammar, how to change them |
| [docs/CONFIGURATION.md](docs/CONFIGURATION.md) | `config.yml`, `tnts.yml`, `items.yml`, every key |
| [docs/COMMANDS_AND_PERMISSIONS.md](docs/COMMANDS_AND_PERMISSIONS.md) | `/mtnt`, the Fuse Linker, permissions |
| [docs/RESOURCE_PACK.md](docs/RESOURCE_PACK.md) | Textures, hosting the pack, pack format, changing the look |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Modules, the Api/Core/Spigot split, ignition and detonation, the block pipeline |
| [docs/API.md](docs/API.md) | Writing your own mechanic or addon against the Api module |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | Building, profiles, tests, the module layout, contributing |
| [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) | Supported versions, what was verified, known limitations |

## Modules

| Module | Role |
|---|---|
| `MoreTNTPlus-Api` | Public contracts: `Mechanic`, `TntDefinition`, `MoreTntPlusApi`, the events. No implementation. |
| `MoreTNTPlus-Core` | Every mechanic, the item and recipe builders, the placed-TNT registry, the block pipeline, config parsing. Depends on Spigot-API only. |
| `MoreTNTPlus-Spigot` | The plugin jar. Wires the core to WorldEdit (block writes), PacketEvents (client effects), WorldGuard and GriefPrevention (claims), listeners, `/mtnt`, the catalogue GUI. Shades Api and Core. |
| `MoreTNTPlus-ResourcePack` | Build-time tool. Draws the textures and writes the resource pack zip from `tnts.yml` and `items.yml`. |

## Licence

No licence file is included yet. The repository is private; choose a licence before making it public.
