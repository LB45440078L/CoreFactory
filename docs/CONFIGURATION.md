# Configuration

CoreFactory reads its settings from files in `plugins/CoreFactory/`. The shipped files are copied from the jar on first
start. After that the plugin does not overwrite a file, with one exception: `processes.yml` is replaced when the jar ships a
newer version of it. The plugin also adds new entries to the data files when the jar is updated (see
[Startup data merge](#startup-data-merge) and [processes.yml and its version](#processesyml-and-its-version)).

| File | Holds |
|---|---|
| `config.yml` | Global settings, performance budgets, effects, holograms, the power network and logistics, the turrets, the rail guns, the artillery and the Command Center, storage, deposits, drill rates, protection, display, messages |
| `tnts.yml` | Every TNT type: name, lore, radius, fuse, mechanic and its settings, recipe |
| `items.yml` | Crafting components: name, lore, material, recipe |
| `materials.yml` | Science materials, research packs, lab tools, the scanner, the solar panel and the drills, as components |
| `processes.yml` | Lab tool kinds (`machines:`), their processes (`processes:`), and the drill kinds and levels (`drills:`) |
| `ores.yml` | Special ores: the vanilla host block of each ore, the drop of a placed ore block, and the height range |
| `research.yml` | The research line: nodes, their costs in research packs, and the machines, components and TNT each one unlocks |

Apply changes with **`/cf reload`**. It re-reads `config.yml`, `tnts.yml`, `items.yml` and `materials.yml`, and it
re-registers the recipes. Placed TNT and its links stay as they are.

Some changes need a restart, not a reload:

- `processes.yml`, `ores.yml` and `research.yml` are read once, at start.
- The `drills:` section of `processes.yml` is read once, at start.
- `effects.*`, `holograms.*`, `concealment.*`, `energy.*`, `charts.*`, `logistics.*`, `machine-upgrades.*`, `vertical-belt.*`, `water.*`, `oil.*`, `deposits.*`, `drill-rates`, `storage.*`, `railgun.*`, `artillery.*` and `command-center.*` are read once, at start.
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

Holograms show a device's state above it: lab tools, drills, generators, pylons, accumulators, turrets, pumps and logistics
devices. Each grid chart is a hologram too. Each hologram is one text display entity, drawn by the game to the players in range.
The plugin sends no packets for holograms, and nothing is written to the world.

- A lab tool's hologram has three lines: the process, a bar of 20 segments, and the percentage with the seconds left (or `idle`).
- A drill's hologram has five lines: its level and kind, its power, its rate (or its status), its deposit, and its storage and fuel.
- Both also end with a `Grid` line for the power network: the grid's voltage, the power the device asks for and the share it
  gets, such as `Grid 230.0 V  4.0 kW  100 %`. It is always shown. See [MECHANICS.md](MECHANICS.md#looks-menus-and-the-grid-line).

The text is drawn at half size, above the device's top, and it grows upward from its bottom line. The background is faint green while
a device works and faint red while it is stuck (no power, no fuel, no room, no target), and there is none otherwise. Pylons,
accumulators, belts and grid charts have no background. A lab tool's hologram is removed when the tool goes idle, and a drill's when
it has no power or no room for its output. Both are removed when their chunk unloads.

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | `false` shows no holograms. Read at start. |
| `view-distance` | `32` | Blocks from a player within which that player sees a hologram. Values above 64 act as 64. Read at start. |

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

### `machine-upgrades`

The work pace of the lab machine levels, set by the upgrade modules `machine_upgrade_2` and `machine_upgrade_3`.

| Key | Default | Meaning |
|---|---|---|
| `speed` | `[1.0, 1.5, 2.0]` | Work pace factor of lab machine levels I, II and III. A level II machine runs its process 1.5 times as fast at full power, and level III twice as fast; the power it draws is unchanged. Each factor must be finite, above zero, and never lower for a higher level. A list that is not valid reverts to the defaults, with one warning. Read at start. |

### `vertical-belt`

The vertical transport belts, which carry items up or down a tower of blocks.

| Key | Default | Meaning |
|---|---|---|
| `speed` | `[1.875, 3.75, 7.5]` | Carry speed of vertical belt levels I, II and III, in blocks per second. Each speed must be finite and above zero. Read at start. |
| `capacity` | `[4, 8, 12]` | Items one block of tower holds at levels I, II and III. Each is a whole number of at least 1. Read at start. |
| `max-item-displays` | `600` | Most item displays on towers at once. Past the cap items still move, but are not shown, and one warning is logged. Read at start. |

A list that is not valid for its key reverts to the defaults, with one warning. The regular belts' speeds are set in the
code, so changing `speed` here does not change them.

### `water`

The water network: pumps, pipes and splitters. A pump stands on air or water and feeds the pipes joined to it. Its source is the
water its network reaches, not the water under it. The rules are in [ARCHITECTURE.md](ARCHITECTURE.md#water-network).

| Key | Default | Meaning |
|---|---|---|
| `source-min-blocks` | `100` | Water blocks a pump's source needs. The count is the water a pump's network reaches: a 6-connected flood fill of water from each block of the network (a pipe, a splitter or the pump) that stands in water or touches it. The pump uses the largest count, capped at this value plus one. A pump is not refused at placement for its source; a pump whose count is below this value stops. Read at start. |
| `source-check-ticks` | `200` | Ticks between checks of a pump's source. `200` is ten seconds. Read at start. |
| `solve-ticks` | `5` | Ticks between solves of the water network. Lower follows changes sooner and costs more. Read at start. |
| `pump-flow` | `[2, 4, 8, 12, 16]` | Units of water per second for pump levels I to V. Five values, each finite and 0 or more. A list that is not five valid values reverts to the defaults, without a warning. Read at start. |
| `pump-power-w` | `[0, 0, 60000, 120000, 240000]` | Power in watts (W) drawn by pump levels I to V. Levels I and II burn fuel and draw none: level I burns coal or charcoal, and level II burns diesel from the oil network. Five values, each 0 or more. A list that is not five valid values reverts to the defaults, without a warning. Read at start. |
| `unit-per-process-run` | `1` | Units of water one run of a process takes from its network, in place of the water bucket. The units are spread over the length of the run. At least 1. Read at start. |

### `oil`

The oil network: oil tubes carry petroleum, from petroleum drills, and diesel, from the crude oil refinery, as litres of fluid
to the generators, ore drills and level II pumps that burn them. A network never holds both fluids. The rules are in
[MECHANICS.md](MECHANICS.md#oil-and-fluids). Read at start.

| Key | Default | Meaning |
|---|---|---|
| `tube-capacity-litres` | `100` | Litres of fluid one oil tube holds. Finite and above 0. A value that is not valid reverts to the default. |
| `pipe-flow-litres-per-second` | `4` | Litres per second each oil network moves from its sources to its sinks at most. The flow is counted per server tick (0.05 s). A fraction a tick does not move is carried to the next tick. Finite and 0 or more. A value that is not valid reverts to the default. |

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

**Existing servers keep their saved values.** The defaults of `arc-degrees` and `pitch-limit-degrees` are `360` and `90`, the whole sphere. A `config.yml` that already sets `arc-degrees: 180` keeps it, and a turret then ignores every mob outside that sector. Change `arc-degrees` to `360` and `pitch-limit-degrees` to `90`, then restart. See [CHANGELOG.md](../CHANGELOG.md).

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
| `power-demand-w` | `24000`, `33000`, `42000`, `51000`, `60000` | Power the level asks for, in watts, while a cannon holds a target. Otherwise it asks for 1 W of standby. |

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
| `power-demand-w` | `60000`, `75000`, `90000`, `105000`, `120000` | Power the level asks for, in watts, while a cannon holds a target. Otherwise it asks for 1 W of standby. |

A laser holds no rounds, so an `ammo-capacity` key under its levels is ignored. Levels 3 to 5 are two blocks high, as for the machine gun. At level 5 the beam and its impact are blue.

The root keys `range`, `damage`, `firing-interval-ticks` and `power-demand-w` of this section are no longer read: the `levels` blocks set them. An old file that still has them loads without an error.

### `railgun`

The rail guns. They are electric, like the laser turret: they hold no rounds and fire one heavy shot at the mobs a machine gun turret fires at, with the same targeting and arc as the `turrets` section. The shot is a helix that flies to its target, and the rail gun is built for armoured mobs and bosses. Read at startup, so a change needs a restart.

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Turn off to stop every rail gun: none searches, aims or fires. |
| `hit-distance-blocks` | `0.5` | The distance from the frontier to the target at which the shot arrives and deals its damage, in blocks. |
| `armour-bypass` | `0.75` | The share of the target's armour the shot ignores, from 0 to 1. `1` ignores all of it. |
| `helix-speed-level-1` | `2` | The speed of the flame frontier at level I, in blocks per second. |
| `helix-speed-level-3` | `9` | The speed of the flame frontier at level III, in blocks per second. Level II is halfway between the two. |
| `spiral-delay-seconds` | `0.5` | Seconds after the frontier leaves the barrel before the blue spiral begins. |
| `strand-radius-blocks` | `0.3` | The radius of the red strands around the path, in blocks. |
| `strands` | `4` | The number of red strands, spaced evenly around the path, from 1 to 8. |
| `strand-twist-turns-per-block` | `0.5` | The turns the red strands make around the path for each block it travels. |
| `spiral-radius-blocks` | `0.75` | The radius of the blue spiral, in blocks. It is larger than the strands' radius. |
| `spiral-turns-per-block` | `1` | The turns the blue spiral makes around the path for each block it travels. |
| `sample-spacing-blocks` | `0.25` | The distance along the path between two drawn points of a strand or the spiral, in blocks, from 0.05 to 2. A path too long for the budget is drawn with the spacing doubled until it fits. |
| `spin-turns-per-second` | `1` | The turns the helix turns about the path each second, toward the target, from 0 to 10. |
| `particle-budget` | `96` | The most points one shot draws in one tick, its frontier, strands and spiral together, from 0 to 256. |
| `max-particles-per-tick` | `192` | The most points all the rail guns' shots in flight draw in one tick together, from 0 to 2000. The shots share it equally. |
| `max-light` | `15` | The light level of a rail gun when it is ready to fire, from 0 to 15. It rises from 0 over the charge. |

**`levels`.** The rail gun has three levels, 1 to 3, and a `levels` block for each. The keys are `damage`, `power-demand-w`, `range`, `charge-seconds` and `reload-seconds`. The defaults are:

| Key | Default at levels 1, 2, 3 | Meaning |
|---|---|---|
| `damage` | `400`, `1200`, `3500` | Damage of one shot to the mob it hits, in HP (2 HP is one heart), before the armour the shot does not ignore. |
| `power-demand-w` | `2000000`, `4000000`, `8000000` | Power the level draws, in watts, while it charges and while it fires. It draws none while it reloads. |
| `range` | `60`, `80`, `110` | Blocks from the barrel within which the rail gun sees a target, and how far the shot travels. |
| `charge-seconds` | `6`, `5`, `4` | Seconds from a target acquired to a shot ready. |
| `reload-seconds` | `10`, `9`, `8` | Seconds from a shot to the next charge starting. |

The time between two shots of a level is its charge plus its reload. A rail gun has no level 4: a `levels` block for 4 is not read. Level 3 is two blocks high, as the upgrade rule sets for every turret. A rail gun keeps the turrets' shared rotation speed, and it has no ammunition, so no `ammo-capacity` key applies.

### `artillery`

The artillery pieces: their levels, charges, shells, ballistics, magazines, safety margins and the cloud of a phosphorus shell. The rules are in [MECHANICS.md](MECHANICS.md#artillery), and the player's view is in [the player guide](PLAYER_GUIDE.md#artillery). Read at startup, so a change needs a restart. The yields of the nuclear shells are not keys: the Little Boy and the Davy Crockett take their yields from `nuke_little_boy` and `nuke_davy_crockett` in `tnts.yml`, which is read at the same time.

A missing key, or a value that is not finite or is out of range, gives its default. A list of levels with the wrong number of values gives the defaults of that list.

**Levels.** Each list has seven values, for levels I to VII. The range, reload and scatter of the other levels are worked out from the two end values, by the formulas in [MECHANICS.md](MECHANICS.md#artillery).

| Key | Default | Meaning |
|---|---|---|
| `levels.height-blocks` | `[1.5, 1.5, 2.5, 2.5, 2.5, 3.0, 3.75]` | The model height of levels I to VII, in blocks. |
| `levels.power-w` | `[100000, 150000, 230000, 350000, 540000, 830000, 1270000]` | The power of levels I to VII at full power, in W. |
| `levels.range-level-1-blocks` | `1500` | The range of a level I piece with a tier I charge, in blocks. |
| `levels.range-level-7-blocks` | `9000` | The range of a level VII piece with a tier I charge, in blocks. |
| `levels.reload-level-1-ticks` | `400` | The reload of a level I piece with a tier I charge, in ticks. `400` is 20 seconds. |
| `levels.reload-level-7-ticks` | `1200` | The reload of a level VII piece with a tier I charge, in ticks. `1200` is 60 seconds. |
| `levels.scatter-level-1-blocks` | `60` | The standard deviation of the landing points of a level I piece, in blocks at 1000 blocks of range. |
| `levels.scatter-level-7-blocks` | `8` | The same, for a level VII piece. |

**Ballistics.** `ballistics.drag-per-second` is `0.002`: the linear air drag, the share of the speed a shell loses each second.

**Magazine.** `magazine-capacity` is `16`: the most shells, and the most charges, a piece's magazine takes. A load adds one shell and one charge, so a full magazine holds 16 of each. Each shot uses one shell and one charge. A piece's magazine is loaded and unloaded in its dialog at the Command Center; see [the player guide](PLAYER_GUIDE.md#loading-and-unloading). Lowering the value below what a magazine holds keeps what it holds: the magazine takes no more until it is below the new capacity.

**Charges.** Each tier has three multipliers: `range`, `reload` (a higher value reloads more slowly) and `scatter` (a higher value spreads the shells more). The defaults:

| Key | Range | Reload | Scatter |
|---|---|---|---|
| `charges.tier-1` | `1.0` | `1.0` | `1.0` |
| `charges.tier-2` | `1.25` | `1.25` | `1.2` |
| `charges.tier-3` | `1.6` | `1.5` | `1.5` |

**Shells.** The yields are in tons of TNT, and the burst heights in blocks above the ground. A shell that bursts at one height has the same minimum and maximum.

| Key | Default | Meaning |
|---|---|---|
| `shells.tnt-block-tons` | `1.0` | The tons of TNT in one block of TNT, for the high explosive. |
| `shells.explosive.yield-tons` | `1.0` | The yield of the explosive shell. |
| `shells.high-explosive.tnt-blocks` | `10` | The TNT blocks of filler. The yield is the blocks times `tnt-block-tons`. |
| `shells.white-phosphorus.burst-min-blocks` | `25` | The lowest burst height. |
| `shells.white-phosphorus.burst-max-blocks` | `35` | The highest burst height. |
| `shells.white-phosphorus.cloud-radius-blocks` | `8` | The radius of the cloud on the ground, in blocks. |
| `shells.white-phosphorus.damage-per-tick` | `0.05` | The damage a creature takes each tick at full strength, in HP. |
| `shells.white-phosphorus.duration-seconds` | `60` | How long the cloud lasts, in seconds. |
| `shells.white-phosphorus.half-life-seconds` | `20` | The time the damage takes to halve, in seconds. |
| `shells.white-phosphorus.light-particles` | `600` | The light particles one burst spawns. A visual only. |
| `shells.napalm.length-blocks` | `30` | The length of the burn along the shot, in blocks. |
| `shells.napalm.width-blocks` | `12` | The width of the burn across the shot, in blocks. |
| `shells.cluster.yield-tons` | `3` | The tons of TNT in one submunition. |
| `shells.cluster.burst-min-blocks` | `30` | The lowest burst height. |
| `shells.cluster.burst-max-blocks` | `50` | The highest burst height. |
| `shells.cluster.submunitions` | `40` | The submunitions one shell releases. |
| `shells.cluster.spread-radius-blocks` | `30` | The radius of the disc the submunitions land in, in blocks. |
| `shells.cluster.min-spacing-blocks` | `4` | The least distance between two submunitions, in blocks. |
| `shells.enriched-uranium.burst-min-blocks` | `8` | The lowest burst height. The yield is not a key: it is `nuke_little_boy` in `tnts.yml`. |
| `shells.enriched-uranium.burst-max-blocks` | `8` | The highest burst height. |
| `shells.hydrogen-thermonuclear.yield-tons` | `1000000` | The yield, in tons. |
| `shells.hydrogen-thermonuclear.burst-min-blocks` | `20` | The lowest burst height. |
| `shells.hydrogen-thermonuclear.burst-max-blocks` | `30` | The highest burst height. |
| `shells.cluster-enriched-uranium.bomb-share-of-davy-crockett` | `0.75` | The share of the Davy Crockett's yield in one bomb. From above 0 to 1. The Davy Crockett is `nuke_davy_crockett` in `tnts.yml`. |
| `shells.cluster-enriched-uranium.burst-min-blocks` | `8` | The lowest burst height of a bomb. |
| `shells.cluster-enriched-uranium.burst-max-blocks` | `8` | The highest burst height of a bomb. |
| `shells.cluster-enriched-uranium.bombs` | `5` | The bombs one shell releases. |
| `shells.cluster-enriched-uranium.spread-radius-blocks` | `50` | The radius of the disc the bombs land in, in blocks. |
| `shells.cluster-enriched-uranium.min-spacing-blocks` | `40` | The least distance between two bombs, in blocks. |
| `shells.cluster-enriched-uranium.min-elevation-degrees` | `25` | The least elevation of the shot, so the bombs fall from above. |
| `shells.condensed-antimatter.yield-tons` | `500000000` | The yield, in tons: half a gigaton. |
| `shells.condensed-antimatter.burst-min-blocks` | `50` | The lowest burst height. |
| `shells.condensed-antimatter.burst-max-blocks` | `50` | The highest burst height. |

**Safety.** The margins of the firing rules, in blocks, degrees and hours. See [Safety rules in the player guide](PLAYER_GUIDE.md#safety-rules).

| Key | Default | Meaning |
|---|---|---|
| `safety.minimum-distance-conventional-blocks` | `50` | The least distance from a piece to its target for a conventional shell. |
| `safety.minimum-distance-nuclear-blocks` | `200` | The same, for a nuclear shell. |
| `safety.minimum-distance-antimatter-blocks` | `2000` | The same, for the antimatter shell. |
| `safety.blast-margin-blocks` | `20` | The gap kept between a blast's reach and the piece or the player who fires. |
| `safety.scatter-sigmas` | `3` | How many standard deviations of scatter the blast checks allow for. |
| `safety.blast-radius-constant` | `1.15` | The blast radius per cube root of the yield in tons: radius = constant x cube root of tons. |
| `safety.blast-radius-floor-blocks` | `15` | The least blast radius, in blocks. |
| `safety.fallout-multiple` | `3` | The fallout zone of a nuclear shell as a multiple of its reach. No player may be inside it. |
| `safety.min-elevation-degrees` | `5` | The lowest elevation of any shot. |
| `safety.max-elevation-degrees` | `85` | The highest elevation of any shot. |
| `safety.antimatter-cooldown-hours` | `12` | The hours a player waits between two antimatter shots. |

**Presentation.** The sounds, particles and motion of a shot. The sounds themselves are vanilla ones, named in the code; these keys set how loud, how often and how much. A shot's flight is not changed by them.

| Key | Default | Meaning |
|---|---|---|
| `presentation.launch-volume` | `4.0` | The volume of the launch report. A volume of 4 is heard about 64 blocks away. |
| `presentation.launch-pitch` | `0.5` | The pitch of the launch report, from 0.5 to 2.0. Low sounds heavy. |
| `presentation.recoil-blocks` | `0.5` | How far the barrel kicks back at a shot, in blocks. It returns to rest in about half a second. |
| `presentation.whistle-volume` | `0.5` | The volume of the whistle of a shell in the air. A volume of 0.5 is heard about 8 blocks away. |
| `presentation.whistle-interval-ticks` | `40` | The ticks between two whistles of one shell. `40` is two seconds. |
| `presentation.trail-puffs-per-tick` | `3` | The smoke puffs a shell leaves each tick, at most. |
| `presentation.impact-volume` | `3.0` | The volume of the thud of a shell that lands without a blast, such as a napalm shell. |
| `presentation.debris-particles` | `60` | The debris particles where a blast lands on the ground. A blast in the air throws none. |
| `presentation.hazard-volume` | `0.6` | The volume of the hiss of a phosphorus cloud and the crackle of a napalm fire. |
| `presentation.hazard-interval-ticks` | `80` | The ticks between two hisses or crackles of one cloud or fire. `80` is four seconds. |
| `presentation.reload-volume` | `0.7` | The volume of the sound when a piece is ready to fire again. |

### `command-center`

The Artillery Command Center: the Set Target mode, the map and the screen. Read at startup, so a change needs a restart. The rules are in [MECHANICS.md](MECHANICS.md#artillery), and the player's view is in [the player guide](PLAYER_GUIDE.md#the-command-center).

| Key | Default | Meaning |
|---|---|---|
| `listen-timeout-seconds` | `20` | The seconds the Set Target mode stays open with no click, after which it ends and stores nothing. |
| `map-radius-blocks` | `64` | The blocks from the centre of the map to its edge. At zoom 1 the map shows twice this across, one block to a pixel. |
| `zoom-levels` | `[1, 2, 4]` | The zoom levels of the map, in blocks per pixel, from the closest to the widest. The list must be strictly increasing and above 0. Otherwise the default is used. |
| `screen-refresh-ticks` | `10` | The ticks between two refreshes of the screen. `10` is half a second. |
| `map-wall-size` | `5` | The side of the wall of item frames that shows the map, in frames: `3` or `5`. The wall stands above the Command Center, in front of its screen, one frame to a block, and shows the map while the Set Target mode is open. Any other value gives `5`. |
| `terrain-reads-in-flight` | `8` | The most chunks the Set Target map reads from the server at once, without generating them. |
| `terrain-generations-in-flight` | `4` | The most unexplored chunks the Set Target map generates at once. Generation is slow, so this is kept low. |
| `terrain-cache-chunks` | `4096` | The chunks whose terrain the map keeps in memory, the least recently used dropped first. About 256 bytes each. |

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
| `scanner-radius` | `96` | Radius, in blocks, that a scan of the Ore Density Scanner searches around the player. A deposit counts when its circle reaches within it. From 1 to 512. |
| `scan-cooldown-ticks` | `60` | Ticks between two scans of one scanner, so a held scanner cannot scan every tick. 20 ticks is one second. From 0 to 1200. |
| `outline-seconds` | `20` | Seconds that a scan's outline of the nearest deposits stays drawn around the player. From 1 to 120. |

The last three keys are the scanner's, and they are read on their own: when they break a rule, one warning is logged and their defaults are used, while the planning keys keep their values. The shipped `config.yml` has a `deposits:` section with these defaults, so editing it changes the deposits. If the values break a rule, the console logs one warning, and the defaults are used for the whole section, not only for the key that is wrong. A `config.yml` with no `deposits:` section takes the defaults in the table.

### drill-rates

One rate for each drill level from 1 to 5. A drill's output is its level's rate times its power efficiency (see [MECHANICS.md](MECHANICS.md#drills)). Read at startup, so a change needs a restart.

| Key | Default | Meaning |
|---|---|---|
| `drill-rates` | `[1.0, 1.5, 2.5, 4.0, 6.0]` | Five numbers for levels 1 to 5, level 1 first, at full power. For an ore drill, items per second. For a petroleum drill, litres of petroleum per second pumped into the oil network. The defaults are 60, 90, 150, 240 and 360 per minute. |

- **The rules.** The list has exactly five numbers. Each is finite and at least 0, and a higher level may not yield less than the level below it.
- **A bad list.** A list of the wrong length, or a value that breaks a rule, logs one warning, and the defaults above are used for the whole list.

### `chunks`

`keep-loaded` (default `true`) keeps loaded the chunks that hold working devices: drills, lab machines, generators, pumps, the refinery,
turrets, pylons, belts and oil and water pipes. Without it, a chunk unloads when no player is near, and its devices stop. `max-forced`
(default `256`) caps how many chunks the devices keep loaded with tickets; a value of `0` keeps none. Both are read at startup. The
tickets are not saved, so they are added again when the server starts.

The plugin holds all its chunk tickets in one place. The artillery holds the chunks of its shots and blasts there too, so a chunk that
both a device and a shot need stays loaded until both have let go. `max-forced` counts only the device chunks.

The artillery pieces and Command Centers are not device chunks: they hold their own chunks under an owner of their own, outside the
cap and the order of `max-forced`. While a Command Center has a target, the chunk of the aim and the eight chunks around it are held
too, until the target is cleared, replaced or the center is removed. `keep-loaded` turns these holds off as well.

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
(`solar_panel`), and the drills (`petroleum_drill_1` to `petroleum_drill_5`, `ore_drill_1` to `ore_drill_5`). Petroleum and
diesel are not items: they are fluids in the oil network, so no entry here names them.

A lab tool or a drill is a component whose `material` is the block it stands on. `BARREL` is the host of the lab tools and of every drill. The solar panel's `material` is `DAYLIGHT_DETECTOR`. The Ore Density Scanner (`ore_density_scanner`) is a held item that finds deposits. Its scanner keys are in the `deposits` section.

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

The keys `veins-per-chunk` and `vein-size` are no longer read, so they do nothing. Deposits are not placed from this file: the world lays them, with the settings under [deposits](#deposits) (see [MECHANICS.md](MECHANICS.md#deposits)). A crude petroleum deposit (`crude_petroleum_deposit`) is one of the entries. It has no `drop`: a petroleum drill pumps its
petroleum into the oil network as litres of fluid, and no item is given.

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

- `pylons`: each pylon's id, world, block, level (1 to 5) and grid id (`grid`).
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

## water.yml and vertical.yml

These two files hold the water pumps, pipes and splitters, and the vertical belts with the items on their towers. As with
the files above, the plugin writes them and they are not meant to be edited by hand.

`water.yml` has a `version` (1) and numbered `devices` entries. Each entry has its `type` (the component id: `water_pump`,
`water_pipe`, `water_splitter_t` or `water_splitter_x`), its `id`, `owner`, `world`, the block (`x`, `y`, `z`), and `face`
(lowercase, for the splitters and the pumps). A pump also has its `level`, its fuel (`fuel`, the items it holds) and
`burn-ticks` (the ticks left on the item that burns). Pump sources are not saved: they are measured again. An entry with a
splitter that has no face is skipped, and a pump with no face gets north. A splitter's face may be up or down: a vertical splitter
stands on the top or the bottom of a block.

`vertical.yml` has a `version` (1), `belts` and `items`. Each belt has its `id`, its `owner` (if one is set), `world`, the
block, its `level` and its `direction`, which is `UP` or `DOWN`. Each item on a tower has its `world`, its position (`x`,
`y`, `z`), and its `kind` and `id`.

Both files are written to a `.tmp` file first, which then replaces the real file. `water.yml` is saved when a part is placed,
removed, upgraded or fuelled, on shutdown, and every 300 ticks. `vertical.yml` is saved at once when a belt is added, removed
or changes direction, and with each logistics save (every `logistics.save-interval-ticks`, and on shutdown), which is when
the items on the towers are written. An item found at load on a tower that covers its height goes back on that tower;
otherwise it is dropped at its block.

## oil.yml

`oil.yml` holds the oil tubes: each one's type, block, owner and, when it holds any, its fluid (`petroleum` or `diesel`) and
its litres. The file has a `version` (currently 2) and the tubes as numbered `devices` entries. A version 1 file reads as empty
tubes. An entry that cannot be read is skipped with a warning.

When the file loads, a network that holds both fluids is settled before the first tick: the fluid more of its tubes hold is kept,
the other is emptied, and a warning names the network. On a tie petroleum is kept. The networks are not saved: they are rebuilt
from the tubes on each start.

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
# data-version: 10
```

The shipped version is 10. The changes of each version are listed in [CHANGELOG.md](../CHANGELOG.md).

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
├── materials.yml              science materials, lab tools, drills, scanner, solar panel
├── processes.yml              lab tool kinds, processes, drill kinds and levels
├── ores.yml                   special ores
├── research.yml               the research line
├── *.yml.bak                  copies made before the startup data merge or the processes.yml version check rewrote a file
├── placed-tnt.yml             placed TNT, links, targets, directions
├── machines.yml              lab tools and drills: where they stand, their progress and process
├── energy.yml                pylons, accumulators, generators and wires (see above)
├── logistics.yml             belts, pickers, splitters and the items on belts (see above)
├── water.yml                 water pumps, pipes and splitters, with their fuel and levels (see above)
├── oil.yml                   oil tubes, with the fluid in each (see above)
├── vertical.yml              vertical belts and the items on their towers (see above)
├── turrets.yml                placed turrets, their levels, rounds, facing and targeting
├── friends.yml                each owner's friend list, for the turrets
├── achievements.yml           each player's achievement progress
├── research.db                research progress and deposits (when storage.type is sqlite)
├── libraries/                 JDBC drivers downloaded for storage, with their .sha1 files
└── pack/pack.zip              the resource pack copied from the jar
```

The items inside a lab tool or a drill are kept in the barrel under it. `machines.yml` keeps each machine's position, type,
progress and process. Back up the world together with `machines.yml` and `placed-tnt.yml`, because the items are in the
world's blocks. Back up `energy.yml`, `logistics.yml`, `water.yml`, `oil.yml`, `vertical.yml` and `turrets.yml` as well: the fuel in a generator
or a pump, the fluid in the oil tubes, and the items on a belt or on a vertical tower, are kept in those files, not in the world.
