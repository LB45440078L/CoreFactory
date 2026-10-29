# Compatibility

## Supported targets

| Target | Maven profile | Status |
|---|---|---|
| Paper 26.3 | `api-26.3` (default) | Supported. Built against Paper API 26.3. |
| Purpur 26.3 | `api-26.3` | Supported. Unverified in game for this build. |
| Folia | none | Not supported. Out of scope. |
| Paper 26.1, 26.2 and 1.21.11 | `api-26.1`, `api-26.2`, `api-1.21.11` | Not supported. The profiles still exist, but the sources are not tested on them. |

**Requirements:**

- **Java 25** on the server. The bytecode targets release 25, so a server on an older Java refuses the plugin.
- **Paper API 26.3.** `paper-plugin.yml` declares `api-version: '26.3'`.
- **WorldEdit 7.4.x or FastAsyncWorldEdit.** Either one; you do not need both. The plugin disables itself without one. A
  FastAsyncWorldEdit build must be made for your exact server version. The plugin checks the editor at startup and logs
  whether it works. If it does not, it logs a SEVERE message and switches to slower Bukkit block writes, so blasts still
  work, but more slowly.
- **Optional:** WorldGuard and GriefPrevention, for the claim checks (see [Known limitations](#known-limitations)).
  PacketEvents is not needed.
- **Storage drivers.** SQLite and PostgreSQL drivers are not in the jar. The chosen one is downloaded from Maven Central
  on first use, unless it is already on the classpath or in `plugins/CoreFactory/libraries/` with a matching `.sha1` file.
  A server with no outside access needs the jar and its `.sha1` copied in beforehand. See [CONFIGURATION.md](CONFIGURATION.md#database-drivers).

## Verification status

Nothing below has been run in game on the current build. The pure rules of most features are covered by unit tests (see
[DEVELOPMENT.md](DEVELOPMENT.md#tests)), and the Paper integration is checked by reading the code. The checks to make on a
server with a client are listed under [Pending in-game checks](#pending-in-game-checks). The main ones are:

- The enable of the plugin on Paper 26.3 and on Purpur 26.3.
- The `/cf` command tree: its typed arguments, the subcommands a sender without a permission does not see, and tab
  completion (unverified in game: the Brigadier tree's behaviour).
- Recipe restore after `/reload` (unverified in game).
- The fluid network: its flow, the refinery's output faces, the refusals that keep petroleum and diesel apart, and the check of a
  saved network that holds two fluids at start-up (the oil load check, unverified in game).
- Device screens: their geometry, the text orientation and the fit (unverified in game).
- Holograms: their placement and the background tone colours (unverified in game).
- Client effects: the clock hold, the rain and the beacon beams (block views). Whether the clock hold survives the
  server's own time updates is unverified in game. Thunder is not shown per player.

## Saved data and versions

Changes from earlier releases are listed in [CHANGELOG.md](../CHANGELOG.md). The rules for the data files of an older build are:

- **processes.yml.** Its data version is 12 in this build. A file with no version line, or an older one, is copied to `processes.yml.bak`
  and replaced by the shipped file. Machines, processes and drills changed in the file are not kept; copy them back from
  the `.bak` copy by hand if you need them. See [CONFIGURATION.md](CONFIGURATION.md#processesyml-and-its-version).
- **research.yml.** The startup merge adds only the node ids that are missing and keeps an existing node's unlock list. On a
  server that already has `research.yml`, a new item may not be gated by research until its node is updated: remove the node
  from `research.yml`, or grant it with `/cf research grant <node>`.
- **energy.yml.** Version 2 is in watts. A file with no version, or a version below 2, is in the old units: its stored
  accumulator energy, generator burn time and produced totals are reset to zero, and the console says so in one line.
  Positions, levels, fuel counts, wires and pylons are kept.
- **oil.yml.** Version 2 keeps each tube's fluid and litres. A version 1 file loads its tubes empty. A saved network that
  holds two fluids is settled at start-up (see [ARCHITECTURE.md](ARCHITECTURE.md#oil-network)).
- **Petroleum and diesel items.** They are no longer items, and no code converts an item that an older world holds. Unverified
  in game: what a petroleum or diesel item in a chest or a machine slot does.
- **Lab machines and turrets.** A lab machine saved with no level loads at level I. A turret saved with no level loads at
  level 1.
- **water.yml and vertical.yml** are written on first save. Nothing is migrated from an older server.
- **Ore and deposits.** Ore is laid as deposits. A chunk generated before that has no deposit marker, so it is laid once, on
  its first load after the upgrade. The ore blocks that the old code placed stay where they are.
- **Schema.** Migration 4 drops the old chunk-budget and mining-ledger tables. No code reads them any more.

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
| Sounds | The 41 sounds are quiet enough, and each generator kind, drill kind and process machine plays its own sound at its machine. | `MachineSounds`, `SoundKeys`, `sounds.json` |
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
| Turret owner and friend controls | As the owner, or a friend of the owner, load, eject, power, upgrade and break all work, and the targeting switches work for the owner only. As anyone else, the menu opens for looking, but its buttons, a kit, a round and a left-click are refused with their messages. A friend's targeting button is refused. Check with and without `corefactory.turret`, and as a friend after `/cf friends add`. | `TurretListener`, `TurretMenu.mayUse`, `TurretMenu.mayChange`, `TurretAccess`, `FriendAccess` |
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
| Deposit patches | In a new world, deposits show as patches of ore blocks over stone, inside each ore's height band, about 30 percent of the stone in a patch. Check the look, and that the patches are where the deposit list says they are. | `DepositGenerator`, `OreBlocks`, `DepositPlanner` |
| Deposit list | `/cf geology deposits` lists the deposits near you, with the ore, what is left, the centre and the radius, and says "None. Try a larger radius." when there are none. Check the radius limits (1 to 512) and the usage line. | `GeologyCommand` |
| Chunk laid once | A chunk is populated once: after a restart, its ore blocks are the same and no new ore blocks appear. | `DepositGenerator.populate` |
| Drill power | Every drill, of either kind, runs on the energy network alone. Place a drill inside a pylon's square with a generator wired to it: the drill runs at full rate. Remove the grid, or place the drill outside every pylon's square: it shows "No power" and does not mine or pump. Redstone and a daylight detector beside a drill do not run it. | `DrillRules.efficiency`, `EnergyService`, `DrillTicker` |
| Drill yield | A drill over a patch yields about its rate: a level 1 drill at full power makes about 1.0 item per second, and the info item shows the rate. | `DrillTicker`, `DrillMining`, `DrillRates` |
| No deposit in range | A drill with no deposit in its square says "No deposit in range". | `DrillStatus`, `DrillRules.status` |
| Exhausted deposit | An exhausted deposit reports "Deposit exhausted", and the drill moves on to another deposit in range when one exists. | `DrillMining`, `DrillRules.status` |
| Deposits survive a restart | The remaining amounts survive a restart. This holds with the JDBC store only. | `JdbcDepositStore`, `DepositLedger` |
| Resources menu | The resources menu pages the deposits, six per page, with the arrows and the back button. | `DrillResourcesMenu`, `DrillResourceRows` |
| No database, restart | Without the database, a drill finds no deposit in a chunk laid before a restart, because the deposits were kept in memory only. | `DepositLedger`, `MemoryDepositStore` |
| Hand-mined deposit block | Dig a patch block of a deposit by hand. You get one raw item of its ore (coal gives charcoal; a crude petroleum block gives no item, because its volume is pumped as fluid), the deposit goes down by one, and `/cf geology deposits` shows the new amount. A block with an empty deposit gives nothing and still breaks. | `GeologyListener.planPatchBreak`, `GeologyListener.givePatchOre`, `PatchBlocks` |
| Silk touch on a patch block | Silk touch on a patch block gives the same one raw item as without it, and no ore block item. | `GeologyListener.givePatchOre` |
| Placed host block on a mined-out site | Mine a patch block until its deposit is empty. Place the same host block on that spot, then break it: it gives no ore. | `PatchExclusions`, `GeologyListener.onHostPlace` |
| Claim you cannot break | In a claim or region that refuses the break, a patch block gives nothing, and it stays in place. | `GeologyListener.givePatchOre` |
| Piston and dispenser | A piston that pushes a host block onto a patch site, and a dispenser that puts one there, leave a block that gives no ore when broken. | `GeologyListener.onPistonExtend`, `GeologyListener.onDispense` |
| Explosions | A blast that destroys a patch block gives no ore, and the deposit keeps its amount. | `GeologyListener.onBlockExplode`, `GeologyListener.onEntityExplode` |
| Scanner | Right-click scans the nearest deposits within 96 blocks: the action bar names the nearest, the chat lists up to five, and a scan inside 60 ticks says "Scanner recharging". Sneak and right-click cycles the filter. Sneak and left-click surveys up to ten deposits with their centre and radius, and breaks no block. The nearest eight are outlined for 20 seconds. `/cf deposits` opens the map (it needs `corefactory.geology`, which is for operators by default), and a click sets a waypoint: a gold beam, its direction on the action bar, and an arrival message inside the deposit, ending after two minutes. | `DepositScanner`, `DepositMap`, `DepositOutline`, `WaypointService`, `DepositScan` |
| Picker and splitter at partial power | At about half power a picker and a splitter move items at about half their rate, and no item is lost to tick rounding. Count the items over a minute. | `LogisticsNetwork.stepPicker`, `LogisticsNetwork.stepSplitter` |
| Laser beside machine guns | With a machine gun and a laser in one world and more mobs in range than turrets, the two kinds fire at different mobs. Put a mob that a machine gun holds in a laser's range, with no other free mob there: the laser takes it. With another free mob in range, the laser takes that one instead. | `TargetAssignment.assign`, `TurretService.claimsOutside` |
| Pump on land with a source tube | Place a pump on dry land, with a pipe standing in a lake of at least 100 connected water blocks and joined to the pump. Check that the pump is placed with no refusal, that its hologram reads "Source low" and then "Water source: infinite" within 10 seconds, and that it runs once a machine draws water. Break the pipe and check that the hologram reads "Source low" within 10 seconds. Check a pump standing in a small pool too: its count should read low. | `WaterService.networkSource`, `WaterSource.sourceAround`, `WaterText.source` |
| Pump touching water | A pump on land with water on one of its own faces, and no pipe, takes that water as its source. Check that the hologram reads "Water source: infinite" when the lake is large enough. | `WaterService.networkSource`, `WaterSource.sourceAround` |
| Pipe bent up and down | Join a pipe to a side face and to the top, and another to a side face and to the bottom. Check that each is drawn as a bend, that its ends meet the two faces square, and that the curve bulges toward the block's centre. Check also that a pipe joined on the top and the bottom is straight up and down. The curvature is checked by render only (see Known limitations). | `WaterOrientation.pipe`, `WaterFrame.bend`, `WaterArt` |
| Vertical T and X | Click the top and the bottom of a block with a T, then with an X. Check that the input points into the block that was clicked, that the T's outputs run to the left and right of the way the player looks, and that the X's arms run up and down and across. Check that a horizontal T or X still faces the player. Check that a vertical splitter is refused on a solid block. | `WaterPlacement.orientation`, `WaterFrame.tee`, `WaterFrame.cross` |
| Tall pumps, levels IV and V | Place a level IV pump with a free block above it, then with a block above it (refused, with "A level 4 pump stands two blocks tall, and the block above this spot is in the way."). Upgrade a level III pump to IV with a block above (refused, the kit kept). Repeat for level V. Check that the upper model is drawn on the block above, that the hologram sits above that block, and that a right-click on the upper block works as a click on the pump. | `PumpLevels.height`, `WaterService.spaceAbove`, `WaterEntities.spawn`, `WaterListener.upgrade` |
| Pump sound | A running pump plays the pump sound every 2 seconds. Check that it is heard at the usual distance, that a stopped pump is silent, and that each level sounds a little higher than the one below (level I is the lowest). Check that the sound is not too loud beside a pump. | `MachineSounds.pumpRun`, `SoundPack`, `art/sounds/water_pump.ogg` |
| Catalogue Water tab | Open `/cf gui`. Check that the tab row holds 19 tabs, that Water sits between Splitters and Ammunition, and that it holds the pump, the kits II to V, the pipes and the water splitters T and X. Check that the Belts tab holds the vertical belts I to III, and that the Lab machines tab holds Upgrade Modules II and III. | `CatalogModel` |
| Pump kits | Right-click a pump you own with a kit, in order (II, then III, and so on). Check the owner and friend rule, the refusal messages for a wrong level or a pump you do not own, and that a kit is used up only when the pump moves up one level. Check it in survival and in creative. | `WaterListener`, `PumpLevels`, `WaterService` |
| Pump fuel | At level I, coal or charcoal burns, one item for 60 seconds, and the fuel slot holds at most 16 items. At level II, diesel is drawn from the oil network at one litre a minute. Check that the pump stops when the network has no diesel. | `PumpFuel`, `WaterService` |
| Pump hologram lines | Check the text of each hologram line of a pump at levels I and II, and the change when its fuel runs out. | `WaterText` |
| Pipe orientations | A pipe draws straight, bent, vertical, as a T and as a cross, by its connections. Check each shape in game. A T or a cross clicked on the top or the bottom face stands vertically (see Vertical T and X above), and is not refused. | `WaterOrientation`, `WaterPart`, `WaterPlacement` |
| Water bucket substitute | A lab process that needs a water bucket runs from a connected pipe and takes one unit per run. While it waits, its hologram shows "Water: x of y /s". Check it with a lab process and a pipe. | `WaterFeed`, `WaterText` |
| Nuclear generator with a pipe | A nuclear generator with a pipe attached on any face runs while water flows through the pipe network, outside any water. Without a pipe it runs only in water. Check both cases. | `WaterRules.nuclearMayRun`, `EnergyService` |
| Vertical belt input from a side | A regular belt or a picker placed beside any belt of a tower feeds that tower from the side. Check it at a low and a high belt of the tower. | `VerticalService`, `VerticalRules` |
| Picker into a tower | A picker beside a tower feeds it, and the items travel up or down the tower at the belt's speed. Check it with a level I, II and III tower. | `VerticalService`, `TowerNeighbours` |
| Vertical belt output | At the output end, a tower feeds the block beyond it. Check each case: a belt, a machine and a chest. | `VerticalService`, `TowerNeighbours` |
| Cascade menu on a tall tower | Setting one belt's direction sets the whole tower above and below it. Check it on a tower of 24 belts. | `VerticalMenu`, `VerticalService` |
| Restart with water.yml and vertical.yml | After a restart, the water parts and the vertical belts, with their items, are back where they were. Check that both files are written and read back. | `WaterStore`, `VerticalStore` |
| Lab level models and holograms | A lab machine at level II shows gold trim, and at level III cyan trim. Its hologram title shows the level. Check each level. | `LevelArt`, `MachineStore.upgrade`, `LabLevels` |
| Research gating on an existing server | On a server whose `research.yml` was made before this version, check whether the pump, its kits, the pipes, the splitters, the vertical belts and the lab modules are refused before their node is finished. The documented rule is in Changes for existing worlds. | `ResearchGateListener`, `research.yml` |
| Brigadier command tree | `/cf` is one tree with typed arguments. Check that a subcommand a sender may not use is absent from their help and tab completion, that literal words are case-sensitive, that a non-number is refused, and that `/cf target` needs all three coordinates or none. Check the help lines: a click types the command, and the hover shows what it does. | `CfCommand`, `CoreFactoryCommand.help`, `Style.usage` |
| Permissions in code | The nodes of `CorePermissions` appear with their defaults, and `corefactory.*` holds them. | `CorePermissions` |
| Recipe restore | After `/reload`, the plugin's recipes are back in the crafting menus and in the recipe view. | `RecipeRestoreListener` |
| Fluid network flow | A petroleum drill fills a tube line, and a generator at its end draws from it at the pipe flow (`oil.pipe-flow-litres-per-second`, default 4 L/s). Each tube holds up to `oil.tube-capacity-litres` (100 L). Check the litres on the info item and the hologram. | `OilService`, `OilSettings` |
| Refinery outputs | A refinery takes petroleum from each tube network on its faces that holds petroleum, and pushes diesel into the first tube network on any other face that holds diesel or has a diesel generator on it. At full speed it draws 0.1 L/s of petroleum and makes 2 L of diesel from each 3 L. Check that a diesel tube on the east or south face gets diesel, that an empty tube beside the refinery takes a petroleum drill later, and that "No petroleum" and "no output tube takes diesel" show when they should. | `OilJoinPlan`, `OilService`, `Refinery`, `RefineryTicker` |
| Mixing refusals | A tube that would join a petroleum network to a diesel network is refused with "Petroleum and diesel cannot share a pipe.". A drill, generator or pump that would mix them is refused with "This would mix petroleum and diesel in one oil network.". | `OilPlacement`, `MachineListener`, `EnergyPlacementListener`, `WaterPlacement` |
| Oil load check | Put a petroleum and a diesel tube in one saved network (by editing `oil.yml` while the server is stopped), start the server, and check that the minority fluid is emptied, that one warning is logged for the network, and that on a tie petroleum is kept. | `OilService.settleMixed`, `MixedNetworks` |
| Device screens | A generator, a drill or a lab machine, and the refinery, shows a black panel with four lines on its front face. Check the panel's size and position, that the text faces the player who placed the device, that the first line is green, red or grey by tone, and the refinery's faces. | `DeviceScreen`, `ScreenGeometry`, `ScreenText` |
| Hologram tones | A working device has a faint green background, a stuck device a faint red one, and an idle one none. Check that the colours read well with the text. | `HologramService.colourOf` |
| Clock hold and thunder | A nuclear blast holds the player's clock at noon, then releases it, and shows rain. Thunder is not shown. Check that the clock hold survives the server's own time updates. | `BukkitClientEffects`, `NuclearMechanic` |

## Known limitations

These are real, and are part of the design unless noted.

| Area | Limitation |
|---|---|
| Claims | WorldGuard `tnt` flags and GriefPrevention claims are respected by blasts (see [MECHANICS.md](MECHANICS.md#claims-and-integrations)). Other region systems are not. |
| Thunder | The Paper API has no per-player thunder, so a nuclear blast shows rain and the clock hold only. |
| Claims for containers | Belts, pickers and splitters respect the containers in WorldGuard regions and GriefPrevention claims, and only those two plugins. The rule is checked by reading the code only: GriefPrevention is read by reflection, and its claim lookup and trust levels are not checked against a live jar; WorldGuard has not run on a server. A read that fails refuses the container, so a device delivers less rather than into a claim it may not use. |
| Placed-TNT visuals | The placed block is a vanilla TNT block; its custom look is an item display over it. Without the display (`custom-textures: false`) it looks like vanilla TNT. |
| Pistons | Pistons cannot move a placed custom TNT (when `protect-from-pistons` is on). They can still move other blocks around it. |
| Gravity flip | An approximation: players levitate, mobs have gravity switched off. Real inverted gravity is not possible on the server side. |
| Teleport | Moves entities only. Blocks are never moved. |
| Pulse | The redstone shell is placed with physics on, one block at a time, capped at 4 000 blocks, so that neighbours are powered. Expect a short burst of updates. |
| Fluid networks | A network holds one fluid kind, so a join that would mix petroleum and diesel is refused. A saved network that holds both is settled at start-up, and its minority fluid is emptied. |
| Beams | The nuclear beams are block views sent to the viewer only. Each beacon stands on a client-side iron platform, because a client draws a beam only from a beacon with a pyramid. Each column also gets a light block at the top of the world, because a beam stops at the first solid block above it. Nothing is placed in the world. |
| Viewer's clock | The clock hold is a per-player time (`setPlayerTime`). Unverified in game: that it survives the server's own time updates. |
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
| Holograms | Each hologram is a `TextDisplay` that the game draws to nearby players. Nothing is written to the world, and a hologram is not saved; its display is spawned again when its chunk loads. |
| Barrel concealment | Block changes are sent to the players near each barrel, and each machine has an invisible interaction entity. Nothing is written to the world or saved. The views are sent again every 40 ticks. |
| Power and logistics | A lab tool and a belt, picker or splitter draw their power even when idle or empty. Generators give power only while their chunk is loaded. Logistics items are shown only up to `logistics.max-item-displays`; the rest are carried without a display. |
| Sprite effects | Sprite effects are item displays. At `effects.max-sprites` newer effects are dropped, with one warning. |
| Ore blocks | An ore re-textures every block of its host type in the world. A deposit's ore block is one of its ore's host blocks (glazed terracotta for most ores, vanilla coal ore for coal), so this applies to those blocks wherever they are. See [RESOURCE_PACK.md](RESOURCE_PACK.md#limits). |
| Turrets and blasts | A blast does not destroy a machine gun turret or a laser turret. Keep turrets out of a blast's radius. |
| Turrets and hoppers | A hopper does not feed a turret. Belts and pickers do. |
| Turret coordination | The target coordination is checked by reading the code and by unit tests only. It has not run on a server with several turrets and several mobs. |
| Shots and the line of fire | A shot stops at the first living entity on its line, a friend or a spared mob included, as before. That entity takes no damage, and the shot does not reach the target behind it. |
| Turret targets | A claim protects every mob in it, whoever owns it. A tamed or named mob is not a target unless `turrets.target-tamed` or `turrets.target-named` is on. |
| Laser light | The laser's light is white, because vanilla light has no colour. The red comes from the glowing model and the beam. |
| Laser ammunition | The laser turret does not use the round table of the machine gun. Its damage is `laser-turret.levels.N.damage` for its level, and it holds no rounds, so no belt, picker or hopper feeds it. The laser has no belt at any level. |
| Round damage | A machine gun's damage per hit comes from its level table (`turrets.levels.N.damage`), not from the round it fires. Every round deals the same damage at a level; the round sets only its armour bypass, fire and spread. The `damage` key under `turrets.rounds` is read but never used. |
| Turret upper half | A tall turret's upper half is a second display over the block above. The energy and logistics systems key on the lower block, so a power or logistics port is found from the lower block only. |
| Procedural sounds | The 31 procedural sounds that were not rewritten this round, the pump sound included, are not reviewed by ear. |
| Splitter beside a machine | A splitter placed on the side of a machine or a generator is accepted, but it never takes items, because a splitter takes only from the belt behind it. Pre-existing. |
| Sounds not yet played | `pylon_hum`, `accumulator_charge`, `belt_hum` and `picker_clunk` are in the pack but no code plays them yet. `generator_hum` and `drill_run` are in the pack too, but nothing plays them since each device kind got its own sound. |
| Belt flow direction (open) | A belt placed by a player flows opposite to the player's horizontal look: facing east places a belt that flows west (`Dir.fromPlacement`). The javadoc of `LogisticsPlacement` says the reverse, that a belt flows away from the player. The code and the player guide agree with each other, not with that javadoc. This is an open design question, not a bug report yet: the rule is unchanged, and it needs a decision on which way belts should flow. |
| Friends from GriefPrevention | A GriefPrevention player trusted with Access or Manage in the claim at a turret counts as a friend. The reflective names it uses (the `dataStore` field, `getClaimAt`, `getPermission`, and the `Access` and `Manage` levels) are not verified against a live GriefPrevention jar. If the lookup fails, the player counts as a friend, so the turret does not fire at them. |
| Tall devices | The two-block footprint does not move existing blocks. A block already above a device overlaps its upper half. |
| Storage scope | SQLite keeps research in one server's folder. PostgreSQL shares research between servers. Achievements, lab tools and drills are saved per server, in files. |
| Admin-given ore blocks | An ore block from `/cf give` or the catalogue is a placed ore block: it yields ore when mined, without drawing on any deposit. |
| Patch exclusions and events | The placed-host exclusion is enforced for player, piston and dispenser placements only. WorldEdit, or a command that places blocks without events, can make a host block on a patch count as a patch block. |
| Turret damage count | The damage a turret reports as dealt depends on the server's damage handling. A cancelled damage event, or a resistance, can leave a mob standing; the turret then counts only the health the mob lost. |
| Logistics on a refused belt | Logistics items on a belt or a splitter that its network refuses are counted as lost, not dropped. A warning is logged with the count. |
| Vertical belt DOWN model | The orientation of the DOWN model is checked by reading the code only, not in game. |
| Refused kit click | A refused kit click does not open the machine menu. This is by design. |
| Water bends and vertical splitters | The curvature of a pipe bend and the model of a vertical splitter are checked by render only, not in game. The pending checks list what to look for in game. |
| Blasts and vertical belts | A blast destroys a vertical belt within its radius, unless protection spares it, as it does the other logistics devices. Checked by reading the code only. | `DeviceBlast` |
| Picker on a chest side | A picker placed on the side of a chest may not take from it: under investigation; if you see this, note the picker's hologram status, the chest type and the side. |
| Vertical belt direction | The direction check is per clicked belt: the owner and a friend of the owner may change it, and a belt with no stored owner can be changed by anyone. Placing and breaking vertical belts is not owner-only. | `VerticalRules.decide`, `FriendAccess` |
| Water pump and pipe owner | Fuel and breaking a water pump or pipe are not owner-only; only the kits are, and they are for the owner and a friend of the owner. |
| Lab module owner | A lab machine records the player who placed it, and only that player or a friend of that player may fit a module to it. A machine saved before owners were kept can be changed by anyone. | `MachineListener`, `FriendAccess` |

## Changing the supported set

The supported target is the default profile, `api-26.3`, which sets `paper.api.version` in the root POM. To move to another
Paper API version, change that property and the default profile, build, and run the checks in
[Pending in-game checks](#pending-in-game-checks) on that version. The source tree does not branch by version; if a future
API breaks a call, the compile step names the call. The other `api-*` profiles stay unsupported unless this section changes.
