# CoreFactory

**Research. Refine. Rule.**

New TNT for Spigot: 42 TNT types with their own mechanics, 24 crafting components that make the strong ones
hard to build, a configurable recipe for every item, a generated resource pack with a unique texture for each one,
and optional respect for WorldGuard regions and GriefPrevention claims.

- **Targets:** Spigot-API 26.1 (default), 26.2, 26.3 and 1.21.11. One source tree, one Maven profile per target.
- **Runtime:** Java 25. Required: **PacketEvents**, and **WorldEdit or FastAsyncWorldEdit** (either one).
- **Optional:** WorldGuard, GriefPrevention. Used only when installed.
- **Build:** Maven 3.9+, JDK 25.

> The repository is named CoreFactory; the plugin, its jar and its commands are `CoreFactory` and `/cf`.

**Players:** start with the [player guide](docs/PLAYER_GUIDE.md). **Administrators:** see [docs/CONFIGURATION.md](docs/CONFIGURATION.md)
and [docs/COMMANDS_AND_PERMISSIONS.md](docs/COMMANDS_AND_PERMISSIONS.md).

## Feature list

- **Science chain:** seven ores, and five ores that the roasting kiln roasts (uraninite, pitchblende, galena, cinnabar and, for
  zinc, sphalerite). The chain separates, converts, enriches and transmutes them to U-233 and plutonium, in twenty machines. Each lab tool keeps its items in a barrel that hoppers can feed and empty.
- **Progress holograms:** a three-line hologram over each running lab tool (process, bar, time left), and a five-line one over each running drill (name, power, rate or status, the deposit it works on, storage and fuel). Set by `holograms` in `config.yml`.
- **Ore Density Scanner:** a held item that crafts after Laboratory Basics. Right-click it to find the nearest deposits and their richness, sneak and right-click to filter by ore, sneak and left-click to survey them, and `/cf deposits` for a map with waypoints.
- **Drills:** petroleum drills (five levels) pump petroleum from crude deposits, on the strongest of a solar panel, redstone or charcoal.
  Ore drills (five levels) mine ore deposits, and run on petroleum. Every machine and drill keeps its items in a barrel, which is
  hidden from nearby players (`concealment` in `config.yml`).
- **Solar panel:** a generator placed on air. Panels placed before the electrical system are daylight detectors that still power a petroleum drill next to them.
- **Electricity and belts:** pylons carry power to the machines in their range, wires join pylons and generators into grids, grids have
  outlines and charts, and accumulators store power. Belts, pickers and splitters move items between machines. See the [player guide](docs/PLAYER_GUIDE.md#electricity).
- **Protection and claims:** belts, pickers and splitters respect the chests in WorldGuard regions and GriefPrevention claims, so a device delivers, and a picker takes, only where its placer may use the container. Only an owner can change a turret. See [Claims and integrations](docs/MECHANICS.md#claims-and-integrations).
- **Ore blocks:** each special ore is a placeable block, and its deposits are patches of one ore in the stone, with a remaining amount, dug by hand for one raw item each, and drilled by ore and petroleum drills. Silk touch keeps a placed ore block item when it is mined.
- **Catalogue:** `/cf gui` browses everything in seventeen categories, each its own tab. Right-click an entry for its recipe; in the recipe view,
  click an ingredient that has a recipe of its own to open it.
- **Research:** a line of fifteen nodes, finished at a Research Console. Admins can finish it with `/cf research`.
- **Particle effects:** sprite effects for blasts, machines, research and ore breaks (`effects` in `config.yml`).
- **Storage:** research in SQLite for one server, or in PostgreSQL shared by several. The JDBC driver is downloaded on first use.

## What it adds

| Family | Types | What they do |
|---|---|---|
| Structural / physics | 7 | <img src="docs/images/tnt/chain_reaction_tnt.png" width="24" height="24" align="absmiddle"> Chain reaction, <img src="docs/images/tnt/shaped_charge_tnt.png" width="24" height="24" align="absmiddle"> shaped charge, <img src="docs/images/tnt/gravity_well_tnt.png" width="24" height="24" align="absmiddle"> gravity well, <img src="docs/images/tnt/implosion_tnt.png" width="24" height="24" align="absmiddle"> implosion, <img src="docs/images/tnt/fuse_linked_tnt.png" width="24" height="24" align="absmiddle"> fuse-linked, <img src="docs/images/tnt/delayed_cascade_tnt.png" width="24" height="24" align="absmiddle"> delayed cascade, <img src="docs/images/tnt/four_dimensional_tnt.png" width="24" height="24" align="absmiddle"> 4-dimensional |
| Terrain morphing | 5 | <img src="docs/images/tnt/vitrify_tnt.png" width="24" height="24" align="absmiddle"> Vitrify, <img src="docs/images/tnt/liquefy_tnt.png" width="24" height="24" align="absmiddle"> liquefy, <img src="docs/images/tnt/terraform_tnt.png" width="24" height="24" align="absmiddle"> terraform, <img src="docs/images/tnt/randomize_tnt.png" width="24" height="24" align="absmiddle"> randomize, <img src="docs/images/tnt/sculpt_tnt.png" width="24" height="24" align="absmiddle"> sculpt |
| Entity / status | 4 | <img src="docs/images/tnt/concussion_tnt.png" width="24" height="24" align="absmiddle"> Concussion, <img src="docs/images/tnt/launch_tnt.png" width="24" height="24" align="absmiddle"> launch, <img src="docs/images/tnt/teleport_tnt.png" width="24" height="24" align="absmiddle"> teleport, <img src="docs/images/tnt/gravity_flip_tnt.png" width="24" height="24" align="absmiddle"> gravity flip |
| Timing / logic | 4 | <img src="docs/images/tnt/conditional_tnt.png" width="24" height="24" align="absmiddle"> Conditional, <img src="docs/images/tnt/proximity_tnt.png" width="24" height="24" align="absmiddle"> proximity fuse, <img src="docs/images/tnt/pulse_tnt.png" width="24" height="24" align="absmiddle"> pulse, <img src="docs/images/tnt/mirror_tnt.png" width="24" height="24" align="absmiddle"> mirror |
| Visual / audio | 4 | <img src="docs/images/tnt/shockwave_tnt.png" width="24" height="24" align="absmiddle"> Shockwave ring, <img src="docs/images/tnt/debris_tnt.png" width="24" height="24" align="absmiddle"> debris, <img src="docs/images/tnt/echo_tnt.png" width="24" height="24" align="absmiddle"> echo, <img src="docs/images/tnt/silent_tnt.png" width="24" height="24" align="absmiddle"> silent |
| Utility / economy | 4 | <img src="docs/images/tnt/harvest_tnt.png" width="24" height="24" align="absmiddle"> Harvest, <img src="docs/images/tnt/sorting_tnt.png" width="24" height="24" align="absmiddle"> sorting, <img src="docs/images/tnt/marker_tnt.png" width="24" height="24" align="absmiddle"> marker, <img src="docs/images/tnt/anchor_tnt.png" width="24" height="24" align="absmiddle"> anchor |
| Military | 2 | <img src="docs/images/tnt/biological_tnt.png" width="24" height="24" align="absmiddle"> Biological, <img src="docs/images/tnt/napalm_tnt.png" width="24" height="24" align="absmiddle"> napalm |
| Exotic matter series | 2 | <img src="docs/images/tnt/exotic_neutron_bomb.png" width="24" height="24" align="absmiddle"> Exotic neutron bomb, a 5 kt blast whose radiation reaches far beyond it; <img src="docs/images/tnt/exotic_antimatter_bomb.png" width="24" height="24" align="absmiddle"> antimatter bomb, 180 megatons |
| Nuclear | 10 | <img src="docs/images/tnt/nuke_davy_crockett.png" width="24" height="24" align="absmiddle"> Davy Crockett to <img src="docs/images/tnt/nuke_tsar_bomba.png" width="24" height="24" align="absmiddle"> Tsar Bomba: countdowns, a saucer crater from surface bursts or a deep hemisphere from thermonuclear ones, flattened ground, debris, and a beacon array in the sky |

Every type is a row in [`tnts.yml`](CoreFactory-Spigot/src/main/resources/tnts.yml): its mechanic, radius, fuse,
settings and recipe can be changed without touching code. The full list is in
[docs/MECHANICS.md](docs/MECHANICS.md). Textures are painted by code, see [docs/TEXTURES.md](docs/TEXTURES.md).

### Achievements, a guide and recipes

364 achievements, counted from the shipped data and built from the TNT types and components: forge, light and detonate every TNT, forge and use every component, counters, chain and nuclear feats (including nuking a village), and mastery groups. Progress is kept per player, and the menu is `/cf achievements`.

`/cf book` gives a tutorial book. `/cf recipe <id>` shows any recipe in a crafting grid, and right-clicking the catalogue does the same. See [docs/ACHIEVEMENTS.md](docs/ACHIEVEMENTS.md).

### Client-side beacon beams

The nuclear tiers draw a 5 × 5 array of beacons, three blocks apart, around the blast. Each beacon sits on a small iron
platform, because a client draws a beam only from a beacon with a pyramid. The beacons, platforms and light blocks are
sent to each player within 3 × the radius with PacketEvents and are never written to the world, so the server's
terrain does not change. Each beacon draws its own beam up to the sky. Those players also see the rain stop, and their
clock hold at noon for 5 seconds and slide back to the real time over 20 more. When their view is restored, the real
blocks, weather and clock are sent back. See [docs/MECHANICS.md](docs/MECHANICS.md#nuclear) for the details.

### Claims and integrations

- **WorldGuard:** a blast leaves alone any block inside a region whose `tnt` flag denies TNT.
- **GriefPrevention:** a blast leaves alone any block inside a claim.
- **Containers:** a belt, picker or splitter puts items into a chest in a region or claim only where its placer may use it, and a picker takes items out of a chest on its input side only where its placer may use it. A device with no owner is refused there.

Both blast checks are built for speed. WorldGuard is asked once per chunk per tick, and only where a region with TNT
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

- `CoreFactory-Spigot/target/CoreFactory-Spigot-1.0.0.jar`: the plugin, with the resource pack inside. Put it in `plugins/`.
  On start it copies the pack to `plugins/CoreFactory/pack/pack.zip`, where ResourcePackManager (RPMS) reads it.
- `CoreFactory-ResourcePack/target/CoreFactory-ResourcePack.zip`: the textures, also inside the plugin jar. Host it and set
  `resource-pack` in `server.properties` (see [docs/RESOURCE_PACK.md](docs/RESOURCE_PACK.md)).

Install `PacketEvents` and `WorldEdit` (or `FastAsyncWorldEdit`) alongside it, start the server, and run
`/cf gui` to see the catalogue. Then `/cf give chain_reaction_tnt` to get one. WorldGuard and GriefPrevention
are optional; install them and the blasts will respect their protection automatically.

## Status

| Area | State |
|---|---|
| Compilation, all four targets | Passes |
| Unit tests | 16 passed in the 1.0.0 run. The tree has 195 test classes now; they have not been run since the labs, drills and storage were added |
| Enable on Paper 26.1.2, 26.2, 26.3 and 1.21.11, with WorldEdit and PacketEvents | Passed in the 1.0.0 run; every server registered 58 recipes |
| GriefPrevention integration, enable on Paper 1.21.11 | Attaches; no warnings |
| WorldGuard integration | Compiles against the WorldGuard 7.0.20 API; not run on a server yet |
| Gameplay in a client (placing, lighting, blasts, beacon beams, resource pack) | A headless client (mineflayer) on Paper 26.1 has placed, lit and detonated TNT, and read back the nuclear clock, rain and beacon blocks. No graphical client has been used yet. |

The full account, including known limitations, is in [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).

## Documentation

| Document | Covers |
|---|---|
| [docs/MECHANICS.md](docs/MECHANICS.md) | Every mechanic, what it does, its settings, its radius meaning, claims and integrations |
| [docs/CRAFTING.md](docs/CRAFTING.md) | The components, their recipes, the recipe grammar, how to change them |
| [docs/PLAYER_GUIDE.md](docs/PLAYER_GUIDE.md) | The player guide: ores, the lab tools, drills, research, crafting, bombs and the catalogue |
| [docs/SCIENCE.md](docs/SCIENCE.md) | The science chain, from ore to particle physics |
| [docs/CONFIGURATION.md](docs/CONFIGURATION.md) | For administrators: `config.yml`, `tnts.yml`, `items.yml` and the data files, every key, the startup data merge |
| [docs/COMMANDS_AND_PERMISSIONS.md](docs/COMMANDS_AND_PERMISSIONS.md) | `/cf`, the <img src="docs/images/components/fuse_linker.png" width="24" height="24" align="absmiddle"> Fuse Linker, permissions |
| [docs/ACHIEVEMENTS.md](docs/ACHIEVEMENTS.md) | Every achievement, what counts toward it, and where progress is stored |
| [docs/RESOURCE_PACK.md](docs/RESOURCE_PACK.md) | Textures, hosting the pack, pack format, changing the look |
| [docs/TEXTURES.md](docs/TEXTURES.md) | Texture specification, tools and processes for every TNT and component, and a table of all of them |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Modules, the Api/Core/Spigot split, ignition and detonation, the block pipeline |
| [docs/API.md](docs/API.md) | Writing your own mechanic or addon against the Api module |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | Building, profiles, tests, the module layout, contributing |
| [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) | Supported versions, what was verified, known limitations |
| [docs/CREDITS.md](docs/CREDITS.md) | The authors of the recorded sounds in the resource pack, all CC0 |

## Modules

| Module | Role |
|---|---|
| `CoreFactory-Api` | Public contracts: `Mechanic`, `TntDefinition`, `CoreFactoryApi`, the events. No implementation. |
| `CoreFactory-Core` | Every mechanic, the item and recipe builders, the placed-TNT registry, the block pipeline, config parsing. Depends on Spigot-API only. |
| `CoreFactory-Storage` | Research storage: a HikariCP pool, schema migrations, SQLite or PostgreSQL. Downloads the JDBC driver it needs. No Bukkit types. |
| `CoreFactory-Spigot` | The plugin jar. Wires the core to WorldEdit (block writes), PacketEvents (client effects), WorldGuard and GriefPrevention (claims), listeners, `/cf`, the catalogue GUI. Shades Api and Core. |
| `CoreFactory-ResourcePack` | Build-time tool. Draws the textures and writes the resource pack zip from `tnts.yml` and `items.yml`. |

## Credits

Nine of the resource pack's sounds are recordings from CC0 sources on OpenGameArt and from Kenney. Their authors are named in
[docs/CREDITS.md](docs/CREDITS.md).

## Licence

No licence file is included yet. The repository is private; choose a licence before making it public.
