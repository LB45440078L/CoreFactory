# Compatibility

## Supported targets

| Target | Maven profile | Compile | Enable test on a Paper server |
|---|---|---|---|
| Spigot-API 26.1 | `api-26.1` (default) | Passed | Passed on Paper 26.1.2 |
| Spigot-API 26.2 | `api-26.2` | Passed | Passed on Paper 26.2 |
| Spigot-API 26.3 | `api-26.3` | Passed | Passed on Paper 26.3 |
| Spigot-API 1.21.11 | `api-1.21.11` | Passed | Passed on Paper 1.21.11 |

**Hard requirements, all targets:**

- **Java 25** on the server. The bytecode targets release 25, so a server on Java 21 will refuse the plugin. Each
  of the four targets runs on Java 25, which is what was tested.
- **PacketEvents 2.14.0 or newer** (plugin name `packetevents`). The plugin disables itself without it.
- **WorldEdit 7.4.x or FastAsyncWorldEdit** (either; you do not need both). The plugin disables itself without one.
  FastAsyncWorldEdit must be a build made for your exact server version: FAWE 2.8.1 on Hangar stops at Minecraft 1.20.2
  and fails on newer servers. The plugin checks the editor at startup and logs whether it works. If it does not, it logs
  a SEVERE message and switches to slower Bukkit block writes, so blasts still work but more slowly.

- **Storage drivers.** SQLite and PostgreSQL drivers are not in the jar. The chosen one is downloaded from Maven Central
  on first use, unless it is already on the classpath or in `plugins/CoreFactory/libraries/` with a matching `.sha1` file.
  A server with no outside access needs the jar and its `.sha1` copied in beforehand. See [CONFIGURATION.md](CONFIGURATION.md#database-drivers).
- **Java 25 build.** The source is compiled with `maven.compiler.release` 25. The shade plugin is pinned to 3.6.1, because
  3.6.0 cannot read Java 25 class files (major version 69) and fails to package the plugin.

The plugin uses the `api-version: '1.21'` marker, the lowest API level it relies on, so every server above it accepts
the plugin without the old-version warnings.

## What was verified, and how

- **Compilation** against each of the four Spigot-API versions, with WorldEdit 7.4.6 and PacketEvents 2.14.0, using
  Maven's profile for that target.
- **Unit tests.** The 1.0.0 run passed 16 tests across three modules. The tree now has many more test classes (see [DEVELOPMENT.md](DEVELOPMENT.md#tests)). The documentation pass that added the lab tools, drills, scanner and storage did not run them.
- **Runtime enable** on four Paper builds, 26.1.2, 26.2, 26.3 and 1.21.11, with WorldEdit 7.4.6 and PacketEvents
  2.14.0 installed. On each, in the 1.0.0 run: the plugin enabled, registered 58 crafting recipes (40 TNT and 18 components),
  loaded 40 TNT types and 18 components, and `/cf` ran its commands (`list`, `count`, `reload`, `help`, and the
  error messages for missing players and targets). Runs on 26.1.2 also covered disabling, which saved and shut down
  cleanly.
- The 26.2, 26.3 and 1.21.11 enable runs used the default-profile jar, built against Spigot-API 26.1; the per-target
  jars were compiled separately and are not the ones that ran.

**Not verified:**

- Since 1.0.0, none of these has been run on a server: the lab tool barrels and hopper rules, the drills (both kinds and
  all levels), the Ore Density Scanner, the solar panel, petroleum and the ore blocks, the progress holograms, the sprite
  effects, the research admin commands, the startup data merge, and the SQLite and PostgreSQL drivers with their download.
  The pure rules of several of them are covered by unit tests.
- The power network and the logistics devices (pylons, accumulators, generators, wires, belts, pickers, splitters, and
  `energy.yml` and `logistics.yml`) are covered by unit tests of their pure rules. No run on a server is recorded in this
  document.
- Gameplay, in a graphical client: none has connected to a test server. The catalogue GUI, the displays of placed TNT,
  the <img src="images/components/fuse_linker.png" width="24" height="24" align="absmiddle"> fuse linker, the napalm landing and the resource pack have not been seen on a real client. A headless client
  (mineflayer) has placed, lit and detonated TNT on Paper 26.1 and read back the client-side clock, rain and beacon
  blocks. Treat the rest as untested until you have played them.
- Live WorldEdit and PacketEvents calls were not exercised: only their enable and the plugin's startup path.
- The resource pack's pack format numbers: see [RESOURCE_PACK.md](RESOURCE_PACK.md#pack-format).
- WorldGuard runtime: the integration compiles against the WorldGuard 7.0.20 API, but no WorldGuard server jar could be
  downloaded for testing, so it has not been run on a server. GriefPrevention 16.18.4 was run: the guard attached and the
  plugin enabled with no warnings.
- Achievements, the guide book and the recipe view: on a headless client (Paper 26.1), ignition and detonation unlocked
  achievements with the chat line; a village nuke (eight villagers) and surviving a blast unlocked theirs; the recipe
  grid of <img src="images/tnt/chain_reaction_tnt.png" width="24" height="24" align="absmiddle"> Chain Reaction TNT matched its recipe; the achievements page showed its layout; and progress was reloaded
  from `achievements.yml` after a restart, with no repeat announcements. Not exercised: a real crafting-table craft,
  which goes through the same handler but was checked by reading the code only; and the chat and title on a graphical
  client.
- The placed TNT's look: the item display now draws the full-size cube, but no graphical client has looked at it, so
  the fix is verified from the model file rather than on screen.
- Beacon beams and the viewer's sky: on a headless client (mineflayer, Paper 26.1), the beacons, their iron platforms,
  the sky light blocks, the noon hold, the slide back to the real time, the rain removal and their restoration were
  all read back from the client. The beam condition (a pyramid, then a clear path to the sky) comes from the Minecraft
  code, decompiled from 26.3. No graphical client has confirmed that the beams draw; a previous version of this
  feature sent the beacons without the pyramid and was never seen to draw.
- Lighting and blasts were exercised with a headless player (mineflayer, which supports Minecraft 26.1) on a Paper 26.1
  server, and direct calls on a Spigot 26.3 server. Flint and steel, a fire charge and redstone were tested on 26.1; a
  real client on a Spigot 26.3 server has not been used. Several types were not tested through a player: napalm needs a
  thrown item, and fuse-linked, anchor and marker are persistent or inert by design.

## Changes for existing worlds

The power network and the logistics devices change how some builds from before them behave. The plugin converts most of
what was placed before, and what it cannot convert is listed here.

- **Units are real now.** The power network is in watts, watt-hours, volts and amperes. A pylon I carries 23 kW, a pylon II
  92 kW and a pylon III 368 kW. The stored energy in an old `energy.yml` is reset, as described below.
- **energy.yml is version 2.** A file with no `version`, or a version below 2, is in the old units. Its stored accumulator
  energy, the burning time of its generators and their produced totals are reset to zero, and the console says so in one
  line. Positions, levels, fuel counts, wires and pylons are kept. A file that is already version 2 loads as it is.
- **Grids and their ids.** Pylons that were wired to a generator form grids, and each grid gets a persistent id and a colour
  (see [MECHANICS.md](MECHANICS.md#grids-and-outlines)). A pylon that had no wire is a grid of its own. Pylons can now be
  wired to each other, so two grids that a player joins by a wire become one grid, and the surviving id is the smaller one.
- **Solar panels placed before this change still work.** Each is a daylight detector block, as before. Breaking one still
  gives back the solar panel item (`SolarPanelListener`), and a petroleum drill beside one still reads its daylight
  signal (`DrillTicker`).
- **A solar panel placed now is a generator.** It is placed on air and has no block of its own. It makes up to 320 W at noon
  and nothing at night. It does not power a drill by touching it: it feeds the grid through wires, and a drill can use the grid
  as an extra source.
- **Lab tools need power.** Each placed lab tool asks for `energy.consumers.lab-demand` (default 4000 W, which is 4 kW) from a
  pylon whose square covers it. A lab tool with no covering pylon stops. Set `energy.consumers.lab-demand` to `0` to get the
  old behaviour back.
- **Drills keep their own power.** Redstone, daylight detectors, charcoal and petroleum work as before. The grid is an
  extra source, used only when it is stronger than the drill's own power.
- **Drills dig deeper.** The depth of each level is now 48, 96, 144, 200 and 280 layers. The earlier docs gave 16, 24, 32,
  48 and 64. A drill placed before this change digs the deeper box at once, so a deposit or an ore that was out of range
  may now be in range.
- **processes.yml is replaced when its version is older.** A server's `processes.yml` with no version line, or an older one,
  is copied to `processes.yml.bak` and replaced by the shipped file. The data version is 5 now. Version 3 added the chemistry and
  ammunition machines, version 4 added the laser turret parts, and version 5 added the four turret upgrade kits (II to V) and their
  processes. Machines, processes and drills changed in the file are
  not kept; copy them back from the `.bak` copy by hand if you need them. See
  [CONFIGURATION.md](CONFIGURATION.md#processesyml-and-its-version).
- **Turrets have levels.** A turret saved in `turrets.yml` with no level loads at level 1, so it keeps the stats it had: a machine
  gun turret reaches 24 blocks, fires every 10 ticks, holds 128 rounds and asks for 24 kW, and a laser turret reaches 40 blocks,
  deals 12 damage, fires every 10 ticks and asks for 60 kW. Two things change for it. Every round now deals the level's damage
  (6 for a machine gun at level 1), where armour-piercing used to deal 7, incendiary 5 and depleted uranium 9; the round still sets
  its armour bypass, fire and spread. And the root keys `range`, `firing-interval-ticks`, `ammo-capacity` and `power-demand-w` of
  `turrets`, and `range`, `damage`, `firing-interval-ticks` and `power-demand-w` of `laser-turret`, are ignored: the `levels` blocks
  set those values now. A `config.yml` that still has them loads without an error. See
  [CONFIGURATION.md](CONFIGURATION.md#turrets).
- **Blasts destroy devices.** A blast now destroys lab tools, drills, pylons, accumulators, generators, belts, pickers and
  splitters within its radius, unless protection spares them. The machine gun turret and the laser turret are not destroyed by a blast yet. Before, lab tools and drills were left standing. Their items
  drop. See [MECHANICS.md](MECHANICS.md#placement-timing-and-chains).
- **Pickers and splitters are placed on a side face.** A new one goes on the side of a machine, not on its top or bottom. The
  input side of one placed earlier is saved in `logistics.yml`, and it is kept as it was saved.
- **Holograms change.** A lab tool's and a drill's hologram ends with a `Grid` line that gives the voltage, the demand and the
  share, such as `Grid 230.0 V  4.0 kW  100 %`. It is always shown, so the hologram keeps its lines.
- **Two files are new.** `energy.yml` and `logistics.yml` are written on the first save. With the default interval that is
  about 15 seconds after the server starts, so a server that never had a device gets both files too. Their keys are
  defaults in the code when `config.yml` lacks them, so an existing `config.yml` needs no change. See
  [CONFIGURATION.md](CONFIGURATION.md#energyyml-and-logisticsyml).
- **Charts are new.** `/cf grid create` makes a chart for a grid. Charts are saved in `energy.yml`, but their history is
  kept in memory, so each chart's graph starts empty after a restart.

- **Research is not re-gated on a server that already has `research.yml`.** The startup merge adds new entries but does not
  overwrite a node, so the new machines and items keep the old node unlocks there. Remove the node from `research.yml` to take
  the new unlocks, or grant them with `/cf research`. The laser turret, the optical lens and the laser emitter are in no node's
  `unlocks` on such a server, and an id that no node lists is free. So they can be crafted and placed there without research until
  a node lists them: add them to the `unlocks` of `materials_purification` and `tesseract_geometry` in `research.yml`, then restart.
- **Tall devices may overlap a block above them.** A generator or a level IV or V drill placed before this change stood one block
  high. If a block was placed above it, the upper half of the new footprint now overlaps that block. There is no migration. See
  [MECHANICS.md](MECHANICS.md#placing-and-breaking-the-energy-devices).
- **Charcoal from coal.** Ore drills now mine coal veins, which give charcoal. Coal blocks broken by hand give the same item.
- **Turret arc and tilt.** The arc defaults to `arc-degrees: 360` and the tilt limit to `pitch-limit-degrees: 90`, the whole
  sphere. A `config.yml` that already sets `arc-degrees: 180` keeps 180, and then a turret ignores every mob outside that
  sector. Change `arc-degrees` to `360` and `pitch-limit-degrees` to `90`, then restart. The scan rate is
  `turrets.search-interval-ticks`, which defaults to 20 (one second). A `config.yml` keeps the value it has, so a server that
  still reads 10 scans twice as often as the default and costs more server time.
- **Belts, pickers and splitters placed before this update have no owner.** A device placed before this update records no
  placer, so when it would put items into a container, it is refused inside every claim and every WorldGuard region. Its
  items stop at the belt's end. A picker placed before the update takes nothing from a container inside a claim or region. To let it deliver or take again, pick it up and place it again: a device placed after the update
  records its placer, and it delivers wherever that placer may use the container (see
  [PLAYER_GUIDE.md](PLAYER_GUIDE.md#belts-and-chests-the-claim-rule)).
- **Turret owners.** Only the owner, with `corefactory.turret`, can load, eject, upgrade, change targeting or break a turret.
  Anyone else can open its menu and look, but cannot touch it.

## Pending in-game checks

These behaviours are in the code and covered by the unit tests, but they have not been seen on a client yet. Each one is
listed here until it is checked on a server with a client.

| Check | What to look at | Where it is defined |
|---|---|---|
| Belt arrow direction | Fixed in code: the belt's front is the bottom edge of its top texture (the top face's v axis is flipped in `ResourcePackBuilder.addBelt`), and the arrows point toward that bottom edge and scroll that way (`BeltArt`). Check in game that the arrows match the way the items move on a straight belt. If they do not, the fix is in `BeltArt` or in the UV mapping in `ResourcePackBuilder`. The in-game check is pending. | `BeltArt`, `ResourcePackBuilder.addBelt` |
| Grid outlines | `/cf pylons show` draws each grid's outline in its colour, 0.3 blocks above the lowest pylon, and the points are drawn every 10 ticks. Check that the outline matches the squares of the pylons and that the colours differ between grids. | `EnergyService.outlinesNear`, `EnergyEffects.showOutlines` |
| Grid charts | A chart appears one block above the player's head, the right-click cycles satisfaction, production and storage, and the graph of 30 columns fits. Check that a chart with no grid shows `no grid`. | `ChartService`, `ChartText` |
| Level V drill depth | A level V drill from the surface reaches down 280 layers. Check that it reaches the bottom of the world when it stands at y 215 or lower. | `DrillTable`, `DrillBox` |
| Picker and splitter sides | A picker or a splitter placed on a north, south, east or west face takes from the machine it touches and gives away from it, and its arrows point outward. Check that it is refused on the top and the bottom. | `PlacementAim`, `SidePlacement` |
| Picker beside a drill | A picker placed on the side of a drill takes its products from the output slots. | `DrillPort` |
| Pylon-to-pylon wires | A wire between two pylons makes one grid, and the grid keeps the smaller id. The refusal text for a lab tool or an accumulator says that wires join a generator to a pylon, or two pylons. | `WireRules`, `GridRegistry`, `DeviceListener` |
| The Grid line width | The `Grid 230.0 V  4.0 kW  100 %` line fits in a hologram at the usual view distance. | `EnergyText.grid` |
| Sideways picker | A picker placed beside a straight belt takes items from it, at its speed, and the rest keep moving on the belt. A picker beside a corner takes nothing. | `PickSide`, `LogisticsNetwork` |
| Belt right corner | The right-hand corner belt renders with no black or inside-out faces. It is a mirrored texture (`corner_right`), not a mirrored display. Check the left corner still looks as it did. | `ResourcePackBuilder.addBelt`, `LogisticsEntities.beltTransform` |
| Tall devices | A wind, charcoal, petroleum, diesel or nuclear generator, and a level IV or V drill, stands two blocks tall. Check that the upper model lines up with the lower one, that the hitbox covers both blocks, that breaking either half breaks the whole device, and that a placement is refused when the block above is solid. | `Footprint`, `DevicePart`, `DeviceRegistry` |
| Drill animation | A running drill's head moves up and down into the ground, and a stopped drill stays still. | `DrillAnimation` |
| Sounds | The 40 sounds are quiet enough, and each generator kind, drill kind and process machine plays its own sound at its machine. | `MachineSounds`, `SoundKeys`, `sounds.json` |
| Wire rendering | A wire is a thin sag in its grid's colour, the pulses run from the generator end to the pylon end and speed up with the flow, and the wires near a player stay drawn within the particle cap. Check the look in game, and that the frame rate holds with many wires near one player. | `WirePath`, `WireRenderer`, `EnergyEffects` |
| Belt corner input | Fixed in code: the 90-degree corner arc on the top texture now starts at the input side and ends at the output side, which is the front (the bottom edge), so the arrows follow the arc from the input to the output (`BeltArt.cornerFrame`, `ResourcePackBuilder.addBelt`). A corner belt also turns so that its input side faces the side its items come from (`Orientation.cornerYaw`). The in-game check is pending: on all four sides and in both turns (left and right corners), check that a corner takes items from the side it faces, that its arrows run along the arc toward the front, and that the right corner's arrows match the left corner's. | `Orientation`, `LogisticsEntities`, `BeltArt`, `ResourcePackBuilder.addBelt` |
| Turret friends from claims | A WorldGuard member, or a GriefPrevention player trusted with Access or Manage at the turret's block, is never a target. Check it on a server with both plugins, with friendly fire on and off. | `TurretProtection`, `TurretService.relationOf` |
| Machine gun turret | It turns toward a mob and fires at the rate its power allows. Check the firing feel, the rotation at 6 degrees a tick, and that the barrel points at the target. The front of the model is assumed to be +Z at yaw 0. | `TurretService`, `TurretRegistry` |
| Turret hitbox | The interaction is 1 by 1.5 blocks over a level 1 or 2 turret, and 1 by 2.5 blocks over a tall one (levels 3 to 5), which covers both of its blocks. Check that a right-click opens the menu and a left-click breaks it, from either block of a tall turret. | `TurretListener`, `TurretRegistry` |
| Turret model | The placed machine gun turret shows its 3D model, not a missing-texture cube. Its base, pedestal, yoke, barrel, muzzle brake and ammunition box are in place, its amber front pulses every 3 ticks, and the turret turns toward its target. Each level has its own model: `machine_gun_turret_2` to `_5`, and the `_top` halves of levels 3 to 5. Check that the upper half lines up with the lower one. | `TurretArt`, `TurretLevelArt`, `MachineArt`, `TurretService.drawAim` |
| Turret sound | The machine gun's shot sounds like a bullet, a crack from `turret_fire` (a CC0 pistol recording), not a tone. The turret and laser sounds are quieter than before (the pack volume is about a quarter of its earlier value, and the server plays them at 0.35). Check at the usual distance that they are quiet but still heard, and that the reload sound plays on each load. | `TurretSounds`, `SoundKeys`, `SoundPack` |
| Laser beam look | A red dust beam runs from the barrel to where it stops, with a burst of END_ROD particles on a hit, and the model glows for 4 ticks. Check that it reads as a laser, and that many lasers firing at once do not flood the client. | `LaserEffects`, `BeamPlan`, `LaserEffects.PARTICLES_PER_TICK` |
| Laser light | While a laser fires, a white light block (level 12) stands in the air in front of the barrel for 4 ticks, on an empty block only. Check that it goes away on expiry, when its chunk unloads, and when the server stops. | `LaserLight` |
| Laser sounds | `laser_fire` and `laser_charge` are Kenney phaser sweeps, and they may not suit a laser. Check that they are not jarring, that they are quieter than before, and that `laser_charge` plays when a laser picks up a target. | `TurretSounds`, `SoundKeys` |
| Turret levels and kits | Right-click a turret you own while holding an upgrade kit: the kit is used up, and the turret moves up one level. A kit of the wrong level is refused, and so is a kit used on a level 5 turret. The messages read as they should, and a machine gun keeps its rounds with the belt at the new capacity. Check it for kits II to V, in creative and in survival. | `TurretListener.upgrade`, `TurretUpgrade`, `TurretService.upgrade` |
| Tall turret placement | A turret item carrying level 3 or higher stands two blocks high when placed. A placement is refused with its message when the block above the spot is solid. Check that the upper half lines up, and that breaking either block breaks the whole turret, which drops at its level. | `TurretListener`, `TurretRegistry` |
| Block above a tall turret | A block placed on the upper block of a tall turret is refused with "That block belongs to a turret." Check that a block placed on the lower block is refused the same way, and that a block placed beside it is left alone. | `TurretListener.onPlace`, `TurretRegistry.at` |
| Upgrade messages | The upgrade messages read "The turret is now level N", with ", two blocks tall." when the turret grows. A tall level refused for a block above reads "Nothing can grow above this turret". Check each text in game. | `TurretListener.upgrade` |
| Dual shots | A machine gun at levels 4 and 5 has two cannons, and both fire in the same tick. The second cannon prefers a mob within `turrets.dual-spacing-blocks` (6) of the first cannon's mob, and takes the nearest free mob in range when none is that close. Check that two tracers leave the two barrels, that one fire sound plays for the two shots, and that a level 4 or 5 turret with two mobs in range fires at both. A level 4 or 5 turret with one mob in range fires one cannon only. | `TargetAssignment.assign`, `TurretService.assignWorld` |
| Target coordination | With several turrets and several mobs in range, each turret's cannon takes a different mob, the nearest one that no other turret has claimed, so the turrets spread over the mobs. With more turrets than mobs, the extra turrets share them. A turret keeps its mob while the mob stays in range, and it moves to the next mob on the tick after its mob dies. Check it with three turrets and three mobs, with four turrets and two mobs, and with a turret whose mob is killed. | `TargetAssignment`, `TurretService.assignTargets`, `TurretService.assignWorld` |
| Red flash and immediate hits | A mob flashes red on each turret hit. Several turrets firing at one mob each land a hit, one after another, and no shot is dropped because of the one before it. A mob set alight by an incendiary round flashes red from the fire, not from a hit. Check that the mob flashes on each hit, that its health drops by every shot, and that armour still reduces a hit. | `TurretService.strike`, `TurretService.fireRound` |
| Team switch | The team switch (the menu's slot, or `/cf turret team`) changes how the owner's scoreboard team is treated: spared as friends, targeted like strangers with friendly fire on, or not counted. Check it with a real scoreboard team: a teammate is spared, targeted, or counted as a stranger in each mode. | `TargetRules.playerVerdict`, `TurretMenu`, `TurretCommand.team` |
| Friendly fire | With friendly fire on, a stranger is targeted; with it off, no player is targeted except a player whose stance is target. Friends and the owner are never targets. Check it both ways. | `TargetRules`, `TurretCommand.friendlyFire` |
| Turret commands | `/cf turret info`, `target`, `player`, `team` and `friendlyfire` work for the turret you look at (within 6 blocks) or the nearest one you own (within 8 blocks), refuse a turret you do not own, and tab completion offers the subcommands, the mob names and categories, the online players, and the on, off, target, spare, clear and team words. | `TurretCommand`, `CoreFactoryCommand` |
| Blue level 5 laser | A laser turret at level 5 (kit V) has a blue beam and a blue impact burst, and its model is blue. Levels 1 to 4 are red. Check the colour in game. | `LaserEffects`, `TurretLevelArt`, `TurretService.fireLaser` |
| Restart with turrets.yml | After a restart, turrets saved before the levels existed load at level 1, with their stats, and a turret saved at a higher level keeps that level. Check it on a server with an old `turrets.yml`. | `TurretStore` |
| Procedural sounds | The 30 procedural sounds that were not changed this round are unverified: the project owner has said many are poor. (`turret_rotate` is the 31st procedural sound, and it was rewritten.) Check each one by ear. | `make_sounds.py`, `sounds.json` |
| Loop joins | The four looped recordings (`diesel_generator`, `generator_hum`, `purifier` and `induction_hiss`) join at their crossfade without a click or a beat. Check each loop by ear. | `make_sounds.py`, `sources.json` |
| Research gating | The laser turret needs Tesseract Geometry, and its optical lens and laser emitter need Materials Purification. Check it on a server with a fresh `research.yml`: each one is refused until its node is finished. | `ResearchService.isUnlocked`, `research.yml` |
| Turret sphere | With the default arc and tilt, a machine gun or laser turret fires at a hostile mob on every side of it, and above or below it, within its range. A solid block on the line from the barrel stops the shot; grass, flowers and snow layers do not. Check each direction, a mob straight above, and a wall or a slab in the line. | `TargetRules`, `TurretService.clearShot`, `TurretAim.inReach` |
| Turret idle silence | With no target, a turret sweeps its arc without a sound. When a mob comes into range, the rotation sound plays while the turret turns to it, and the fire sound plays on each shot (two shots of one tick share one sound). Check that the sweep is silent, and that the sound starts with the target. | `TurretService`, `TurretSounds`, `TurretAim.playsRotationSound` |
| Turret owner-only controls | As the owner, load, eject, upgrade, targeting, and break all work. As anyone else, the menu opens for looking, but its buttons, a kit, a round and a left-click are refused with their messages. Check with and without `corefactory.turret`. | `TurretListener`, `TurretMenu.mayChange`, `TurretAccess` |
| Turret upgrades to level III and up | Kit III on a level 2 turret, with the block above free, makes it two blocks tall. With the block above solid, the kit stays in hand and the game says that nothing can grow above the turret. Kit IV on a level 3 turret and kit V on a level 4 turret both work, and the turret keeps its height. Check in survival and in creative. | `TurretListener.upgrade`, `TurretUpgrade.canUpgrade` |
| Tall turret and drill breaks | Breaking either block of a tall turret, or of a tall drill, breaks the whole device. A claim that refuses the upper block refuses the break as well, and refuses a tall drill's placement with "This spot is protected." Check each with a protection plugin. | `TurretListener.breakTurret`, `MachineInteractions.breakMachine`, `MachineListener.onPlace` |
| Chest deliveries | A belt's end, a picker and a splitter put items into a chest, a trapped chest, a barrel, a shulker box and a copper chest (each copper chest type). The container fills until full, then the belt waits. A picker takes from these on its input side, one item at a time. Check each container type, as a delivery and as a picker's input. | `LogisticsPorts`, `VanillaContainerPort` |
| Claim rule for containers | With GriefPrevention or WorldGuard: a belt placed by its owner delivers into the owner's claim or region. A stranger's belt is refused there, a belt placed before the update is refused inside any claim or region, and two belts of different owners feeding one chest are refused. Check each case, and check that a picker's take follows the same rule (see Picker claim rule). | `ContainerProtection`, `GriefPreventionGuard.permitsContainer`, `WorldGuardGuard.permitsContainer`, `LogisticsService.deliveryOwners` |
| Picker from a chest | A picker placed beside a chest, a trapped chest, a barrel, a shulker box or a copper chest on its input side is accepted. A splitter beside one is refused. The picker takes one item at a time from the first plain stack, skips a named or enchanted item, and takes from the whole of a double chest through either half. Check each container type, and a double chest. | `LogisticsPlacement.inputOf`, `VanillaContainerPort.peekOutput`, `VanillaContainerPort.takeOutput` |
| Picker claim rule | A stranger's picker beside a claimed chest takes nothing, and the chest keeps its items. The owner's picker takes from it, and so does a picker of a player trusted there with a container level (GriefPrevention Inventory, Container, Build, Edit or Manage), or a WorldGuard member or owner. A picker placed before the update takes nothing from a claimed chest. Check each case with GriefPrevention and with WorldGuard, and a double chest with one half claimed. | `ContainerProtection`, `ContainerAccess.permits`, `VanillaContainerPort.takeOutput` |
| Logistics after an error | When one world's logistics fails in a tick, the error is logged, the other worlds keep running that tick, and the failed world runs again on the next tick. Check the log for the message, and that the other worlds keep moving. | `LogisticsService.tick` |
| Kiln fuel and input paths | The fuel slot takes coal, vanilla charcoal and the charcoal component, by click, drag, shift-click, hopper and belt. A belt or picker feeds the firebox and the input slots, the crushed ore goes into the inputs, and the firebox lights while the kiln burns. | `ProcessEngine.FUEL`, `LabPort`, `MachineMenu.accepts` |
| Unload behaviour | When a chunk unloads, a kiln or lab menu open in it closes, and an edit not yet written to the machine is written first, so the machine keeps what was put in. Check that the items are there when the player comes back, and that nothing is duplicated. | `MachineStore.unloaded`, `MachineMenu.forgetBarrel`, `MachineMenu.closeViewers` |
| Chemistry and ammunition models | The induction furnace, powder mill, cartridge press, bullet swaging machine, ammunition assembly, metal lathe, component assembly and heavy weapons assembly look right, and their front faces animate. | `ProcessMachineArt` |
| New ore blocks | The saltpeter, sulfur, zinc and coal ore blocks look like their minerals on the ground. | `OreArt` |
| energy.yml reset | A real file from before version 2 loads with the one-line warning, and its accumulators start empty. | `EnergyFile.read`, `EnergyFile.resetLegacyUnits` |

## Known limitations

These are real, and are part of the design unless noted.

| Area | Limitation |
|---|---|
| Claims | WorldGuard `tnt` flags and GriefPrevention claims are respected by blasts (see [MECHANICS.md](MECHANICS.md#claims-and-integrations)). Other region systems are not. |
| Claims for containers | Belts, pickers and splitters respect the containers in WorldGuard regions and GriefPrevention claims, and only those two plugins. The rule is checked by reading the code only: GriefPrevention is read by reflection, and its claim lookup and trust levels are not checked against a live jar; WorldGuard has not run on a server. A read that fails refuses the container, so a device delivers less rather than into a claim it may not use. |
| Placed-TNT visuals | The placed block is a vanilla TNT block; its custom look is an item display over it. Without the display (`custom-textures: false`) it looks like vanilla TNT. |
| Pistons | Pistons cannot move a placed custom TNT (when `protect-from-pistons` is on). They can still move other blocks around it. |
| Gravity flip | An approximation: players levitate, mobs have gravity switched off. Real inverted gravity is not possible on the server side. |
| Teleport | Moves entities only. Blocks are never moved. |
| Pulse | The redstone shell is placed with physics on, one block at a time, capped at 4 000 blocks, so that neighbours are powered. Expect a short burst of updates. |
| Beams | The nuclear beams are client-side beacon blocks sent with PacketEvents. Each beacon stands on a client-side iron platform, because a client draws a beam only from a beacon with a pyramid. Each column also gets a light block at the top of the world, because a beam stops at the first solid block above it. Nothing is placed in the world. |
| Viewer's clock | The clock hold is applied to the server's periodic time packets for that player, so mods or other plugins that rewrite time packets can fight it. |
| Napalm | Only a **thrown** napalm (an item the player drops) bursts on landing. A placed napalm bursts in place. |
| Debris | Falling blocks land and can be collected by breaking them; they are not picked up as items. |
| Fire | Fire blocks follow the normal rules: they burn out, and they can spread where the game lets them. |
| Nuclear | TNT inside the radius is not ignited; a nuclear blast leaves it in place or, inside the crater, leaves it standing. |
| Nuclear crater | Streamed over several ticks. A sphere cut reads columns down to bedrock, and the largest tier (<img src="images/tnt/nuke_tsar_bomba.png" width="24" height="24" align="absmiddle"> Tsar Bomba) writes about two million blocks, estimated and not timed; on a large or unloaded area, the first pass loads chunks. On the test server the hole's centre column was cut within four seconds of the blast; the whole cut was not timed. Allow a few seconds on a strong host and longer on a weak one. |
| Chunks | Placed TNT and its display persist with their chunk. A display whose id is no longer in the registry is removed on the next enable. |
| GriefPrevention | If its claim method changes in a future release, the guard logs one warning and blasts skip the claim check until the next restart. |
| Recipes | Recipe ingredients match custom items by identity. A player who received a component before a config change keeps the old item only if its id still exists. |
| Textures | Every texture is painted by code, so the look is consistent but not hand-drawn. Changing it means changing the painter and rebuilding; see [TEXTURES.md](TEXTURES.md). |
| Concurrency | Everything runs on the main thread. There is no async block work. |
| Holograms | Progress holograms are packets sent to nearby players. Nothing is written to the world, and a hologram is not saved. |
| Barrel concealment | Block changes are sent to the players near each barrel, and each machine has an invisible interaction entity. Nothing is written to the world or saved. The views are sent again every 40 ticks. |
| Power and logistics | A lab tool and a belt, picker or splitter draw their power even when idle or empty. Generators give power only while their chunk is loaded. Logistics items are shown only up to `logistics.max-item-displays`; the rest are carried without a display. |
| Sprite effects | Sprite effects are item displays. At `effects.max-sprites` newer effects are dropped, with one warning. |
| Ore blocks | An ore re-textures every block of its host type in the world. See [RESOURCE_PACK.md](RESOURCE_PACK.md#limits). |
| Turrets and blasts | A blast does not destroy a machine gun turret or a laser turret. Keep turrets out of a blast's radius. |
| Turrets and hoppers | A hopper does not feed a turret. Belts and pickers do. |
| Turret coordination | The target coordination is checked by reading the code and by unit tests only. It has not run on a server with several turrets and several mobs. |
| Shots and the line of fire | A shot stops at the first living entity on its line, a friend or a spared mob included, as before. That entity takes no damage, and the shot does not reach the target behind it. |
| Turret targets | A claim protects every mob in it, whoever owns it. A tamed or named mob is not a target unless `turrets.target-tamed` or `turrets.target-named` is on. |
| Laser light | The laser's light is white, because vanilla light has no colour. The red comes from the glowing model and the beam. |
| Laser ammunition | The laser turret does not use the round table of the machine gun. Its damage is `laser-turret.levels.N.damage` for its level, and it holds no rounds, so no belt, picker or hopper feeds it. The laser has no belt at any level. |
| Round damage | A machine gun's damage per hit comes from its level table (`turrets.levels.N.damage`), not from the round it fires. Every round deals the same damage at a level; the round sets only its armour bypass, fire and spread. The `damage` key under `turrets.rounds` is read but never used. |
| Turret upper half | A tall turret's upper half is a second display over the block above. The energy and logistics systems key on the lower block, so a power or logistics port is found from the lower block only. |
| Procedural sounds | The 30 procedural sounds are not reviewed by ear. |
| Splitter beside a machine | A splitter placed on the side of a machine or a generator is accepted, but it never takes items, because a splitter takes only from the belt behind it. Pre-existing. |
| Sounds not yet played | `pylon_hum`, `accumulator_charge`, `belt_hum` and `picker_clunk` are in the pack but no code plays them yet. `generator_hum` and `drill_run` are in the pack too, but nothing plays them since each device kind got its own sound. |
| Belt flow direction (open) | A belt placed by a player flows opposite to the player's horizontal look: facing east places a belt that flows west (`Dir.fromPlacement`). The javadoc of `LogisticsPlacement` says the reverse, that a belt flows away from the player. The code and the player guide agree with each other, not with that javadoc. This is an open design question, not a bug report yet: the rule is unchanged, and it needs a decision on which way belts should flow. |
| Friends from GriefPrevention | A GriefPrevention player trusted with Access or Manage in the claim at a turret counts as a friend. The reflective names it uses (the `dataStore` field, `getClaimAt`, `getPermission`, and the `Access` and `Manage` levels) are not verified against a live GriefPrevention jar. If the lookup fails, the player counts as a friend, so the turret does not fire at them. |
| Tall devices | The two-block footprint does not move existing blocks. See the changes for existing worlds above. |
| Storage scope | SQLite keeps research in one server's folder. PostgreSQL shares research between servers. Achievements, lab tools and drills are saved per server, in files. |

## Changing the supported set

To add a version: add a profile in the parent POM with its `spigot.api.version`, build it, and run the enable test
on that server. To drop one, remove its profile. The source tree does not branch by version; if a future API breaks
a call, the compile step names the call.
