# Configuration

CoreFactory reads its settings from files in `plugins/CoreFactory/`. The shipped files are copied from the jar on first
start. After that the plugin does not overwrite a file, with one exception: `processes.yml` is replaced when the jar ships a
newer version of it. The plugin also adds new entries to the data files when the jar is updated (see
[Startup data merge](#startup-data-merge) and [processes.yml and its version](#processesyml-and-its-version)).

| File | Holds |
|---|---|
| `config.yml` | Global settings, performance budgets, effects, holograms, the power network and logistics, storage, deposits, drill rates, protection, display, messages |
| `tnts.yml` | Every TNT type: name, lore, radius, fuse, mechanic and its settings, recipe |
| `items.yml` | Crafting components: name, lore, material, recipe |
| `materials.yml` | Science materials, research packs, lab tools, the scanner, the solar panel, petroleum and the drills, as components |
| `processes.yml` | Lab tool kinds (`machines:`), their processes (`processes:`), and the drill kinds and levels (`drills:`) |
| `ores.yml` | Special ores: the vanilla host block of each ore, the drop of a placed ore block, and the height range |
| `research.yml` | The research line: nodes, their costs in research packs, and the machines, components and TNT each one unlocks |

Apply changes with **`/cf reload`**. It re-reads `config.yml`, `tnts.yml`, `items.yml` and `materials.yml`, and it
re-registers the recipes. Placed TNT and its links stay as they are.

Some changes need a restart, not a reload:

- `processes.yml`, `ores.yml` and `research.yml` are read once, at start.
- The `drills:` section of `processes.yml` is read once, at start.
- `effects.*`, `holograms.*`, `concealment.*`, `energy.*`, `charts.*`, `logistics.*`, `deposits.*`, `drill-rates` and `storage.*` are read once, at start.
- Moving the placed-TNT registry to a different file takes a restart.

Errors in these files are reported on the console and the affected entry is skipped; the others still load.

## config.yml

A key that is missing from `config.yml` takes the default shown here, which is the value the code uses when the key is
absent.

### `settings`

| Key | Default | Meaning |
|---|---|---|
| `debug` | `false` | Verbose console output: each ignition, explosion, detonation and blast (origin, mechanic, number of blocks changed). Turn it on when a TNT does not behave. |
| `disabled-worlds` | `[]` | World names (case-insensitive) where custom TNT cannot be placed or lit. |
| `max-chain-depth` | `6` | Longest chain of detonations one ignition may start. Stops runaway chains. |
| `max-detonations-per-second` | `60` | Global cap, measured over a rolling 20-tick window. Detonations beyond it are dropped. |

### `performance`

| Key | Default | Meaning |
|---|---|---|
| `blocks-per-tick` | `25000` | Block writes applied per tick. Large blasts are spread over as many ticks as needed. Lower on weak hosts. |
| `instant-threshold` | `4000` | Change sets up to this size are applied in one tick, unstaged. |
| `chain-ignite-delay-ticks` | `6` | Delay before a vanilla explosion ignites a custom TNT it reached. |
| `particles-per-tick` | `1500` | Particle packets per tick across every effect. Keeps shockwaves and beams from flooding clients. |
| `effect-view-distance` | `96` | Blocks within which a player receives an effect's sounds and particles. |

### `effects`

Sprite effects: fireballs, shock rings, mushroom clouds, the ground surge, fallout, machine steam, lab operations,
research bursts and ore breaks. They are drawn with item displays. The effect names and sprites are in
[TEXTURES.md](TEXTURES.md#6-effect-sprites).

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | `false` shows no sprite effects. Read at start. |
| `max-sprites` | `600` | Most sprites alive at once. At the cap, newer effects are dropped and one warning is logged. Read at start. |

### `holograms`

Progress holograms over running lab tools and drills. The text is sent as packets, so nothing is written to the world.

- A lab tool's hologram has three lines: the process, a bar of 20 segments, and the percentage with the seconds left (or `idle`).
- A drill's hologram has five lines: its level and kind, its power, its rate (or its status), its deposit, and its storage and fuel.
- Both also end with a `Grid` line for the power network: the grid's voltage, the power the device asks for and the share it
  gets, such as `Grid 230.0 V  4.0 kW  100 %`. It is always shown. See [MECHANICS.md](MECHANICS.md#looks-menus-and-the-grid-line).

Each line is its own invisible armour stand. A hologram is removed when its lab tool goes idle, when its drill has no power or its outputs are full, and when its chunk unloads.

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | `false` shows no holograms. Read at start. |
| `view-distance` | `32` | Blocks from a player within which that player sees a hologram. Read at start. |

### `concealment`

Hides the barrel under each lab tool and drill from nearby players, who see the custom model instead. The server keeps the barrel, and players farther away still see it. Each machine also gets an invisible interaction hitbox over its host block: a right-click opens the machine, and a left-click breaks it. The rules are in [MECHANICS.md](MECHANICS.md#barrel-concealment).

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | `false` shows every barrel and makes no hitboxes. Leftover hitboxes are still removed. Read at start. |
| `range` | `48` | Blocks from a machine's barrel, to the centre of the block, within which a player sees the barrel hidden. A negative value hides nothing. Read at start. |

Trade-offs to know about:

- A player can briefly see a ghost block when placing a block against a machine.
- A server that already has a `config.yml` does not get these keys written into it. The defaults in the table apply until the keys are added. The shipped `config.yml` lists them.

### `energy`

The power network: pylons, accumulators and generators, and the power that placed lab tools and drills ask for. The
rules, the units and the grids are in [MECHANICS.md](MECHANICS.md#electricity-and-logistics). Generator output, burn times
and fuel are fixed in the code, so they have no keys.

| Key | Default | Meaning |
|---|---|---|
| `consumers.lab-demand` | `4000` | Power, in watts (W), that each placed lab tool asks for, whether or not it runs. A machine whose `processes.yml` entry has a `power` value asks for that instead (the chemistry and ammunition machines do). A lab tool runs at the share its network supplies, so one with no pylon in range stops. `0` means lab tools need no power, which was the behaviour before pylons. Read at start. |
| `consumers.drill-demand` | `15000` | Power, in watts (W), that a placed drill asks for when its level is not known. A drill normally asks for the demand of its level: 15 kW, 25 kW, 40 kW, 60 kW or 90 kW for levels I to V. A drill runs on the stronger of its own power and its network. Read at start. |
| `save-interval-ticks` | `300` | Ticks between saves of `energy.yml`. The file is also saved on shutdown. Read at start. |
| `wire-view-blocks` | `24` | Blocks from a player within which a wire is drawn to them. A wire counts as near by its middle. Read at start. |
| `wire-particle-cap` | `320` | The most particles a player is sent for wires in one redraw, which is every 5 ticks. The nearest wires come first, and each is sent whole or not at all. `0` turns the wires off for every player. Read at start. |

### `charts`

Grid charts (`/cf grid create`): a hologram of a grid's satisfaction, production or storage. The charts and their grids are
saved in `energy.yml`. The history each chart draws from is kept in memory and starts empty after a restart. See
[MECHANICS.md](MECHANICS.md#grid-charts).

| Key | Default | Meaning |
|---|---|---|
| `charts-per-grid` | `1` | The most charts one grid may have. Read at start. |
| `charts-total` | `5` | The most charts the server may have, in all. Read at start. |
| `refresh-ticks` | `10` | Ticks between the refreshes of a chart's text. A chart is refreshed only while a player is within 48 blocks of it. Read at start. |

### `logistics`

The belts, pickers and splitters. Their rules are in [MECHANICS.md](MECHANICS.md#electricity-and-logistics).

| Key | Default | Meaning |
|---|---|---|
| `max-item-displays` | `2000` | Most item displays shown on belts at once. Past the cap an item is still carried, but not shown, and one warning is logged. Read at start. |
| `save-interval-ticks` | `300` | Ticks between saves of `logistics.yml`. The file is also saved on shutdown. Read at start. |

As with `concealment`, a `config.yml` without these keys takes the defaults in the tables above.

### `locks`

The PIN lock on nuclear TNT. The rules are in [MECHANICS.md](MECHANICS.md#pin-lock).

| Key | Default | Meaning |
|---|---|---|
| `max-attempts` | `3` | Wrong PINs in a row before the keypad locks. At least 1. |
| `lockout-seconds` | `30` | How long the keypad stays locked after the last allowed wrong PIN. |

### `turrets`

The machine gun turrets. The rules are in [MECHANICS.md](MECHANICS.md#machine-gun-turret). Read at startup, so a change needs a
restart. Setting `enabled` to `false` stops every turret: none searches, aims or fires, and none asks the network for power.

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Turn the turrets on or off. |
| `search-interval-ticks` | `20` | The scan rate: ticks between two passes that give the cannons of each world's turrets their targets. `20` is one second. A pass also runs on the tick after a target dies. A lower value scans more often, so a turret reacts sooner and the turrets spread over the mobs sooner, but it costs more server time; a higher one scans less often. Allowed: 1 to 1200. |
| `rotation-speed-degrees` | `6` | The most a turret turns in one tick, in degrees. |
| `arc-degrees` | `360` | The total sweep, centred on the way the turret faced when placed. `360` turns all round, so every hostile mob in range is in reach. A smaller value restricts the turret to a sector. |
| `pitch-limit-degrees` | `90` | The most a turret tilts up or down. `90` reaches straight up and down; a smaller value refuses steeper targets. |
| `idle-sweep` | `true` | Sweep the arc slowly while there is no target. The sweep is silent: the rotation sound plays only while a turret turns onto a target it has detected. |
| `accuracy-spread` | `3` | The spread of a regular round, as a half angle in degrees. Each round scales it by its `spread-factor`. |
| `ammo-per-shot` | `1` | Rounds one shot uses. |
| `targets` | `[monsters]` | The kinds of mob a turret fires at: `monsters` (the default), `phantoms`, `slimes` and `ghasts`. Phantoms, slimes and ghasts are spared until they are listed. An empty list fires at none. |
| `target-named` | `false` | Fire at a mob with a custom name. |
| `target-tamed` | `false` | Fire at a tamed mob. |
| `dual-spacing-blocks` | `6` | The spacing the second cannon of a machine gun (levels 4 and 5) prefers: an unclaimed mob in range within this many blocks of the first cannon's mob. When no such mob exists, the second cannon takes the nearest unclaimed mob in range instead, so the spacing is a preference, not a limit. Kept from 0 to 64. |
| `rounds.regular.armour-bypass` | `0` | The share of the target's armour the round ignores, from 0 to 1. |
| `rounds.regular.fire-ticks` | `0` | Ticks the target is set alight. |
| `rounds.regular.spread-factor` | `1` | Scales `accuracy-spread`: lower is tighter. |
| `rounds.armor-piercing` | bypass `0.5`, fire `0`, spread `1` | The armour-piercing round. |
| `rounds.incendiary` | bypass `0`, fire `80`, spread `1` | The incendiary round. |
| `rounds.depleted-uranium` | bypass `0.35`, fire `0`, spread `0.5` | The depleted-uranium round. |

**Existing servers keep their saved values.** The defaults of `arc-degrees` and `pitch-limit-degrees` are `360` and `90`, the whole sphere. A `config.yml` that already sets `arc-degrees: 180` keeps it, and a turret then ignores every mob outside that sector. Change `arc-degrees` to `360` and `pitch-limit-degrees` to `90`, then restart. See [COMPATIBILITY.md](COMPATIBILITY.md#changes-for-existing-worlds).

A round's damage is not a key. Every round deals the damage of its turret's level. A `damage` key under `rounds` is read by the code but never used, so it has no effect.

**`levels`.** The stats of each level are under `levels`, with one block for each of the levels `1` to `5`. A level's block sets these keys, and a key it leaves out keeps its default:

| Key | Default at levels 1, 2, 3, 4, 5 | Meaning |
|---|---|---|
| `damage` | `6` at every level | Damage per hit before the boost. |
| `damage-boost-percent` | `0`, `10`, `20`, `35`, `50` | The boost on the damage, in per cent. |
| `fire-interval-ticks` | `10`, `8`, `7`, `5`, `4` | Ticks between shots at full power. Less power fires more slowly. |
| `range` | `24`, `26`, `28`, `30`, `32` | Blocks from the barrel within which the turret sees a target. |
| `ammo-capacity` | `128`, `256`, `384`, `512`, `768` | Rounds the belt holds, of one kind at a time. |
| `cannons` | `1`, `1`, `1`, `2`, `2` | Cannons that fire, one target each: 1 or 2. |
| `power-demand-w` | `24000`, `33000`, `42000`, `51000`, `60000` | Power the level asks for, in watts. |

Levels 3 to 5 are two blocks high. That is set by the upgrade rule, so it is not a key. The defaults are the same as the table in [MECHANICS.md](MECHANICS.md#upgrading-a-turret).

The root keys `range`, `firing-interval-ticks`, `ammo-capacity` and `power-demand-w` of this section are no longer read: the `levels` blocks set them. An old file that still has them loads without an error.

A turret's own targets, and its friendly-fire switch, are not config keys. Each turret sets them in its menu, or with `/cf turret friendlyfire`, and they are saved in `turrets.yml`. The friends of each player are saved in `friends.yml`. A turret's choices are on top of `targets`: see [MECHANICS.md](MECHANICS.md#machine-gun-turret).

### `laser-turret`

The laser turrets. They are electric: they hold no rounds and fire an instant beam at the mobs a machine gun turret fires at. Their
targeting, rotation, arc, tilt, search and menu come from the `turrets` section above, and they stop with the turrets when
`turrets.enabled` is `false`. Read at startup, so a change needs a restart. The rules are in [MECHANICS.md](MECHANICS.md#laser-turret).

| Key | Default | Meaning |
|---|---|---|
| `armour-bypass` | `1.0` | The share of the target's armour the beam ignores, from 0 to 1. `1` ignores all of it, so the armour takes none of the damage. |
| `beam-particles` | `24` | Dust particles one beam draws at most, from 0 to 32. A hit adds six more. |

**`levels`.** As for the machine gun, the laser's stats are under `levels`, with a block for each level from `1` to `5`. The keys are `damage`, `damage-boost-percent`, `fire-interval-ticks`, `range`, `cannons` and `power-demand-w`. The defaults are:

| Key | Default at levels 1, 2, 3, 4, 5 | Meaning |
|---|---|---|
| `damage` | `12` at every level | Damage of one beam before the boost. |
| `damage-boost-percent` | `0`, `10`, `20`, `35`, `50` | The boost on the beam's damage, in per cent. |
| `fire-interval-ticks` | `10`, `8`, `6`, `5`, `4` | Ticks between shots at full power. |
| `range` | `40`, `44`, `48`, `54`, `60` | Blocks from the barrel within which the laser sees a target and its beam reaches. |
| `cannons` | `1` at every level | One beam at every level. |
| `power-demand-w` | `60000`, `75000`, `90000`, `105000`, `120000` | Power the level asks for, in watts. It asks for this whenever it is loaded. |

A laser holds no rounds, so an `ammo-capacity` key under its levels is ignored. Levels 3 to 5 are two blocks high, as for the machine gun. At level 5 the beam and its impact are blue.

The root keys `range`, `damage`, `firing-interval-ticks` and `power-demand-w` of this section are no longer read: the `levels` blocks set them. An old file that still has them loads without an error.

### `integrations`

| Key | Default | Meaning |
|---|---|---|
| `worldguard` | `true` | Respect WorldGuard regions. Blasts leave alone a region whose `tnt` flag denies TNT. A belt, picker or splitter puts items into a container in a region only where its placer is a member or an owner of every region there. Used only when WorldGuard is installed. |
| `griefprevention` | `true` | Respect GriefPrevention claims. Blasts leave alone any block in a claim. A belt, picker or splitter puts items into a container in a claim only where its placer owns the claim or is trusted there with Inventory, Container, Build, Edit or Manage. Used only when GriefPrevention is installed. |

The blast checks are cached per tick and cost nothing when no protected area is nearby. Set a key to `false` to ignore
that plugin's claims, for blasts and for containers, for example on a server where blasts are meant to reach claimed land.

**Claim plugins and containers.** With no claim plugin installed, every container is open to a device. A claim plugin that is
installed but whose hook cannot attach closes every container to devices, so a failed hook never opens a claim. A device with no
stored owner, such as a belt placed before this update, is refused in any claim or region. See
[MECHANICS.md](MECHANICS.md#claims-and-integrations) and [COMPATIBILITY.md](COMPATIBILITY.md#known-limitations).

### `protection`

| Key | Default | Meaning |
|---|---|---|
| `unbreakable-materials` | bedrock, barriers, command blocks, structure blocks, jigsaw, end portal frame and similar | Materials no blast may change. |
| `protect-from-pistons` | `true` | Cancel piston moves that would carry a placed TNT. |

### `display`

| Key | Default | Meaning |
|---|---|---|
| `custom-textures` | `true` | Show each placed TNT with its own model, through an item display over the TNT block. |
| `display-scale` | `1.002` | Display scale. Slightly above 1 hides the vanilla block texture beneath. |

### `placement`

| Key | Default | Meaning |
|---|---|---|
| `direction-from-player` | `true` | Store the placer's horizontal facing as the TNT's direction. Shaped charges and napalm use it. |

### deposits

The patches of ore that the world lays, and how much each holds. The rules are in [MECHANICS.md](MECHANICS.md#deposits). Read at startup, so a change needs a restart.

| Key | Default | Meaning |
|---|---|---|
| `cell-size` | `160` | Side of a planning cell, in blocks. It must be from twice `max-radius` to 4096. Each cell holds at most one deposit. |
| `density` | `0.6` | Chance that a cell holds a deposit. From 0 to 1. |
| `oil-share` | `0.25` | Chance that a deposit is crude petroleum rather than a solid ore. From 0 to 1. |
| `min-radius` | `24` | Smallest radius of a deposit, in blocks. From 1 to `max-radius`. |
| `max-radius` | `48` | Largest radius, in blocks. From `min-radius` to 256. |
| `min-amount` | `500000` | Smallest amount a deposit starts with. At least 1. |
| `max-amount` | `25000000` | Largest amount a deposit starts with. At least `min-amount`. |
| `ore-block-density` | `0.3` | Share of the stone blocks in a deposit's patch that become its host block when the chunk is laid. From 0 to 1. |

The shipped `config.yml` has a `deposits:` section with these defaults, so editing it changes the deposits. If the values break a rule, the console logs one warning, and the defaults are used for the whole section, not only for the key that is wrong. A `config.yml` with no `deposits:` section takes the defaults in the table.

### drill-rates

The items a drill yields per second at full power, one rate for each level from 1 to 5. A drill yields its level's rate times its power efficiency (see [MECHANICS.md](MECHANICS.md#drills)). The same rates apply to ore drills and petroleum drills. Read at startup, so a change needs a restart.

| Key | Default | Meaning |
|---|---|---|
| `drill-rates` | `[1.0, 1.5, 2.5, 4.0, 6.0]` | Five numbers, items per second at full power for levels 1 to 5, level 1 first. The defaults are 60, 90, 150, 240 and 360 items per minute. |

- **The rules.** The list has exactly five numbers. Each is finite and at least 0, and a higher level may not yield less than the level below it.
- **A bad list.** A list of the wrong length, or a value that breaks a rule, logs one warning, and the defaults above are used for the whole list.

### `persistence`

| Key | Default | Meaning |
|---|---|---|
| `file` | `placed-tnt.yml` | Placed-TNT registry, inside the plugin's data folder. |
| `save-interval-seconds` | `300` | Automatic save interval. The registry is always saved on shutdown. |

### `storage`

Where research progress and the remaining amounts of deposits are kept, permanently. Achievements, lab tools and drills are not in this database: their
files are listed under [Where things live on disk](#where-things-live-on-disk).

| Key | Default | Meaning |
|---|---|---|
| `type` | `sqlite` | `postgresql` (any capitalisation) keeps research in a shared PostgreSQL database. Any other value keeps it in SQLite. |
| `sqlite-file` | `research.db` | The SQLite file, inside the plugin folder. Its folders are created when missing. |
| `postgresql.host` | `localhost` | PostgreSQL host name. |
| `postgresql.port` | `5432` | PostgreSQL port. |
| `postgresql.database` | `corefactory` | PostgreSQL database name. |
| `postgresql.user` | `corefactory` | PostgreSQL user. |
| `postgresql.password` | empty | PostgreSQL password. |
| `postgresql.pool-size` | `6` | Most connections the pool keeps. |

SQLite uses one connection, with a busy timeout of 5 seconds. Several servers that share research need `postgresql`.

If the database cannot open, research is kept in memory until the server stops, the log says so with a SEVERE message,
and gated items stay locked until research is finished again.

#### Database drivers

The SQLite and PostgreSQL drivers are not in the plugin jar. The plugin needs the one that matches `storage.type`:

- If the driver class is already on the server's classpath, nothing is downloaded.
- Otherwise the driver is downloaded from Maven Central the first time it is needed, into `plugins/CoreFactory/libraries/`.
  The versions are sqlite-jdbc 3.46.1.3 and PostgreSQL 42.7.4.
- The published SHA-1 of the jar is checked. A jar that does not match is deleted and downloaded again.
- The download needs a connection to `repo1.maven.org`. Each request times out after 20 seconds. On a server with no
  outside access, copy the jar into `libraries/` together with a file `<jar name>.sha1` that holds its SHA-1. Without that
  file the jar is downloaded again.

The connection pool itself, HikariCP, is part of the plugin jar.

### `messages`

Every player-facing line. Colour codes use `&`. Placeholders such as `{id}` or `{count}` are filled in by the
plugin; keep them. The keys are listed in the shipped `config.yml`.

## tnts.yml

One entry per TNT type, keyed by its id. This is the entry for `chain_reaction_tnt`, as shipped:

```yaml
tnts:
  chain_reaction_tnt:
    name: "&6Chain Reaction TNT"
    lore:
      - "&7Lights every TNT nearby, one step at a time."
      - "&8Builds a rolling wave, not a single frame."
    material: TNT
    fuse-ticks: 60
    radius: 6
    mechanic: chain_reaction
    effects: []
    settings:
      delay-per-block: 3
      chain-radius: 10
      damage: 10
    recipe:
      type: shaped
      pattern: ["RRR", "RTR", "RRR"]
      ingredients: {R: "material:REDSTONE", T: "material:TNT"}
```

| Key | Default | Meaning |
|---|---|---|
| `name` | the id | Display name. Colour codes allowed. |
| `lore` | none | Item lore lines. |
| `material` | `TNT` | Base material. An unknown name falls back to `TNT`. |
| `fuse-ticks` | `80` | Fuse length once lit. 20 ticks is 1 second. |
| `radius` | `4.0` | Blast radius. What it means depends on the mechanic. |
| `mechanic` | `standard` | Mechanic id. An id the plugin does not know skips the TNT, with a warning. The mechanics are in [MECHANICS.md](MECHANICS.md). |
| `effects` | none | Modifiers: `shockwave_ring`, `debris`, `echo`, `silent`. Any other name is ignored, with a warning. |
| `settings` | none | Mechanic settings. The keys for each mechanic are in [MECHANICS.md](MECHANICS.md). |
| `recipe` | none | Crafting recipe. A TNT without one is obtainable only by `/cf give`. The grammar is in [CRAFTING.md](CRAFTING.md). |

Nuclear tiers keep their settings in the same file, as ordinary keys (`yield-megatons`, `shape`, `crater-radius`,
`flatten-radius` and so on). Each tier sets its own numbers.

## items.yml

A crafting component, keyed by its id. This is `fuse_linker`, as shipped:

```yaml
components:
  fuse_linker:
    name: "&bFuse Linker"
    lore:
      - "&7Right-click a placed TNT, then another,"
      - "&7to link them with a visible fuse."
      - "&8Sneak-right-click a TNT to clear its links."
    material: REDSTONE
    recipe:
      type: shapeless
      ingredients: [material:REDSTONE, material:REDSTONE, material:IRON_INGOT]
```

| Key | Default | Meaning |
|---|---|---|
| `name` | the id | Display name. |
| `lore` | none | Item lore lines. |
| `material` | `PAPER` | The vanilla item it is made from. An unknown name skips the component, with a warning. |
| `recipe` | none | Crafting recipe. `type` is `shaped` (the default) or `shapeless`. |

A component's look is generated, not configured: see [TEXTURES.md](TEXTURES.md). A component id that appears in both
`items.yml` and `materials.yml` keeps its first definition and logs a warning.

## materials.yml

The same shape as `items.yml`, for the science chain. It holds the raw and crushed ores (the drops of special ores),
the products of roasting, separation and conversion, the research packs, the lab tools (`crusher`, `roaster`,
`separator`, `purifier`, `converter`, `enrichment_cascade`, `transmuter`, `spectrometer`, `research_console`,
`particle_accelerator`, `antimatter_condenser`), the Ore Density Scanner (`ore_density_scanner`), the solar panel
(`solar_panel`), petroleum (`petroleum`), and the drills (`petroleum_drill_1` to `petroleum_drill_5`, `ore_drill_1` to
`ore_drill_5`).

A lab tool or a drill is a component whose `material` is the block it stands on. `BARREL` is the host of the lab tools and of every drill. The solar panel's `material` is `DAYLIGHT_DETECTOR`. The Ore Density Scanner (`ore_density_scanner`) is still an item, and it can still be crafted, but it does nothing: no code reacts to its use.

## processes.yml

Three sections.

- `machines:` gives each lab tool its host, its slot counts and whether it burns fuel (`fuel: 0` or `fuel: 1`).
- `processes:` gives each process its machine, its ticks, its inputs (`item:<id>` or `material:<NAME>`, with an amount)
  and its outputs (with an optional `chance`). A process runs only when every input is present in full and the outputs fit.
- `drills:` names each drill, its kind (`petroleum` or `ore`) and its level from 1 to 5. The area, depth and cycle of each
  level are fixed in the code, not in the file. They are described in [MECHANICS.md](MECHANICS.md).

The full recipes for the lab tools are in [CRAFTING.md](CRAFTING.md). Editing a process changes its numbers only; the
slot layout of a lab tool is in the code.

## ores.yml

One entry per special ore, keyed by its id:

| Key | Meaning |
|---|---|
| `name` | Display name. |
| `host` | The vanilla block the ore is placed as: the block of its deposit's patch, and the block of a placed ore block. Each ore needs its own host. |
| `drop` | The component given when a placed ore block is mined without silk touch. |
| `drop-amount` | `[min, max]` items per placed ore block. |
| `replace` | Blocks a patch block may replace. |
| `min-y`, `max-y` | Vertical range in which a deposit's patch blocks become host blocks. |

The keys `veins-per-chunk` and `vein-size` are no longer read, so they do nothing. Deposits are not placed from this file: the world lays them, with the settings under [deposits](#deposits) (see [MECHANICS.md](MECHANICS.md#deposits)). A crude petroleum deposit (`crude_petroleum_deposit`) is one of the entries. A placed one drops `petroleum` when mined, and a petroleum drill takes petroleum from the deposits of that ore.

## research.yml

One node per entry, in a single line. Each node requires the node before it.

| Key | Meaning |
|---|---|
| `name`, `description` | Shown in the console and the research screen. |
| `requires` | The node before it. |
| `cost` | Research packs, by id and count, fed into a Research Console to finish the node. |
| `unlocks` | The machines, components and TNT types the node gates. A gated id cannot be crafted, placed or ignited until the node is finished. An id that no node lists is free. |

Restarting the server is needed for a change to `research.yml` to take effect. The research tree is in
[ARCHITECTURE.md](ARCHITECTURE.md#research).

## energy.yml and logistics.yml

These two files hold the state of the power network and of the logistics devices. The plugin writes them; they are not
meant to be edited by hand.

`energy.yml` has a `version` (2 now), the `next-grid-id` (the id the next new grid gets), and five lists:

- `pylons`: each pylon's id, world, block, level (1 to 3) and grid id (`grid`).
- `accumulators`: the same, with the energy held in Wh (`stored-wh`) and whether it was discharging.
- `generators`: each generator's id, world, block and kind, its fuel count (`fuel`), the seconds left on the item that
  burns (`remaining-seconds`), and the energy it has made since it was placed in Wh (`total-wh`).
- `wires`: each wire's id and the ids of its two ends (`from` and `to`).
- `charts`: each chart's name, world, block, grid id and mode (`satisfaction`, `production` or `storage`).

Energy is in watt-hours and time in seconds. A file with no `version`, or a version below 2, is in the old units, which
cannot be converted. Its stored accumulator energy, the burning time of its generators and their produced totals are reset
to zero, and the console says so in one line. Positions, levels, fuel counts, wires and pylons are kept.

`logistics.yml` has four lists:

- `belts`: each belt's world, block, flow and level.
- `pickers`: each picker's id, world, block, input side and level.
- `splitters`: the same, with the two filter lists (`left` and `right`) and the item the splitter holds (`held`), if any.
- `items`: each item on a belt, with the belt's block and its position along the belt, from 0 at the entry to 1 at the exit.

Both files are written to a `.tmp` file first, which then replaces the real file, so a crash during a write leaves the last
good file. They are saved every `save-interval-ticks`, after each change to a device or a wire, and on shutdown. The first
save comes after one interval, so with the default of 300 ticks both files exist about 15 seconds after the server starts,
even on a server with no devices.

An entry with a missing or invalid field is skipped, and the console names the file and the kind of entry. A device in a
world that is not loaded at start is kept in the file until that world loads. If a file cannot be read at start, its
devices start empty and the console says so.

Items on belts keep their place across a restart. If an item cannot be put back when the file is read (its belt is gone,
or its place overlaps another item), it is not kept, and the console logs how many were lost.

## Startup data merge

When the server starts, the plugin adds to the data files the entries that the jar ships and the file does not have.
This means a server that was set up with an older version gets the new TNT types, machines, drills, ores and research
nodes without losing its own changes.

The merge covers these sections:

| File | Sections |
|---|---|
| `tnts.yml` | `tnts` |
| `items.yml` | `components` |
| `materials.yml` | `components` |
| `ores.yml` | `ores` |
| `research.yml` | `nodes` |

`processes.yml` is not in this list. Its machines, processes and drills are tied to the code, so it has its own rule (see below).

The rules:

- A missing entry is added from the shipped default. The log lists the ids it added.
- Before the file is rewritten, the old file is copied to the same name with `.bak` added, for example `tnts.yml.bak`.
  The copy is replaced each time the merge rewrites that file.
- An entry that is already in the file is never changed.
- An entry that differs from the shipped default is named in a warning on the log. It is kept as it is.
- To take the new default for an entry, delete the entry from the file and restart. The default comes back on that start.

The rewritten file is written by the configuration writer, so its layout and comments are not kept. The `.bak` copy has
the earlier text.

`config.yml` is not part of the merge. A key that is missing from `config.yml` takes the default in the code (see the
tables above).

### processes.yml and its version

`processes.yml` holds the lab tool kinds, their processes and the drill kinds and levels. Their slot counts, layouts and
process ids are tied to the code, so the plugin does not merge its entries. Its first line names the version it was
written for:

```
# data-version: 5
```

Version 5 added the four turret upgrade kits (`turret_upgrade_2` to `turret_upgrade_5`), with their processes: kits II and III in the component assembly, and kits IV and V in the heavy weapons assembly. Version 4 added the laser turret parts: the optical lens, the laser emitter and the laser turret, with their processes. Version 3
added the eight chemistry and ammunition machines, their recipes, and the saltpeter, sulfur and zinc processes. A server that still
has an older file gets the new one on the next start, and the old one is kept as `processes.yml.bak`. The version does not touch
`research.yml`: a server that already has that file keeps its node unlocks, so the laser parts and the upgrade kits are not gated there until the file
is updated (see [COMPATIBILITY.md](COMPATIBILITY.md)).

On start the plugin compares that line with the version the jar ships. The file is handled as follows:

| The server's `processes.yml` | What happens |
|---|---|
| No version line, or an older version | The file is copied to `processes.yml.bak`, and the shipped file replaces it. The log warns that the file was replaced. |
| The same version | Nothing changes. |
| A newer version | The file is left as it is, and the log warns that it is newer than the plugin. |

Edits you made to the old file are in `processes.yml.bak`, not in the new one, so copy them back by hand. The `.bak` copy
is replaced the next time the file is replaced. The change takes effect on the next start.

## Where things live on disk

```
plugins/CoreFactory/
├── config.yml                 settings (this page)
├── tnts.yml                   TNT types
├── items.yml                  crafting components
├── materials.yml              science materials, lab tools, drills, scanner, solar panel, petroleum
├── processes.yml              lab tool kinds, processes, drill kinds and levels
├── ores.yml                   special ores
├── research.yml               the research line
├── *.yml.bak                  copies made before the startup data merge or the processes.yml version check rewrote a file
├── placed-tnt.yml             placed TNT, links, targets, directions
├── machines.yml              lab tools and drills: where they stand, their progress and process
├── energy.yml                pylons, accumulators, generators and wires (see above)
├── logistics.yml             belts, pickers, splitters and the items on belts (see above)
├── turrets.yml                placed turrets, their levels, rounds, facing and targeting
├── friends.yml                each owner's friend list, for the turrets
├── achievements.yml           each player's achievement progress
├── research.db                research progress and deposits (when storage.type is sqlite)
├── libraries/                 JDBC drivers downloaded for storage, with their .sha1 files
└── pack/pack.zip              the resource pack copied from the jar
```

The items inside a lab tool or a drill are kept in the barrel under it. `machines.yml` keeps each machine's position, type,
progress and process. Back up the world together with `machines.yml` and `placed-tnt.yml`, because the items are in the
world's blocks. Back up `energy.yml`, `logistics.yml` and `turrets.yml` as well: the fuel in a generator and the items on a belt are
kept in those files, not in the world.
