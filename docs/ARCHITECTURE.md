# Architecture

## Modules and their dependencies

```
CoreFactory (parent pom, profiles per Spigot-API version)
├── CoreFactory-Api          Spigot-API only
│     public contracts: Mechanic, TntDefinition, MechanicSettings, DetonationContext,
│     PlacedTnt, BlockChangeSet, BlockGenerator, BlockEditor, ClientEffects,
│     ProtectionService, CoreFactoryApi, CoreFactoryProvider, TntIgniteEvent, TntDetonateEvent,
│     PackVersion
├── CoreFactory-Core         Api + Spigot-API
│     CoreFactoryCore (the implementation of CoreFactoryApi), TntManager, Blast, BlockJobs,
│     Effects, Visuals, VisualCatalog, Explosions, Shapes, Tasks, ProtectionImpl, PlacedTntStore,
│     PlacedDisplays, ItemFactory, RecipeRegistrar, DefinitionParser, CoreSettings, Messages,
│     mechanics/*  (every built-in mechanic)
│     process/*    (lab tool processes: ProcessCatalog, ProcessEngine, Machine)
│     drill/*      (drill levels, power and status: DrillTable, DrillRules, DrillPower, PowerSource, DrillStatus, DrillText, DrillCatalog, DrillState)
│     geology/*    (ore veins: GeologyService, VeinPlanner, OreDefinition, RetrofitQueue)
│     research/*   (the research tree and service: ResearchTree, ResearchService, ResearchStore)
│     hologram/*   (progress text and rules: HologramText, HologramRules)
│     survey/*     (the chunk survey: ChunkSurvey)
│     radiation/*  (fallout, the suit, the Geiger counter)
│     energy/*     (the power network, pure: EnergyTable, Units, EnergyNetwork, GridGraph, GridRegistry, GridPalette, OutlineTracer, ChartHistory, ChartRenderer, Generator, Sky)
│     logistics/*  (belts, pickers and splitters, pure: LogisticsTable, LogisticsNetwork, Topology, Lane, Port)
├── CoreFactory-Storage      HikariCP only. JDBC research store, schema migrations, runtime driver loading.
│     No Bukkit and no Core types.
├── CoreFactory-Spigot       Api + Core + Storage + Spigot-API + WorldEdit, PacketEvents (provided)
│     the plugin jar. Shades Api, Core, Storage and HikariCP. Holds plugin.yml, config.yml and the six
│     data files. WorldEditBlockEditor, PacketClientEffects, listeners, /cf, the catalogue GUI, lab tool
│     and drill machines, barrel concealment, holograms, the energy and logistics devices, the chunk scanner, sprite
     effects, the startup data merge.
└── CoreFactory-ResourcePack Build-time only. snakeyaml. Draws textures; writes the pack zip.
```

The rule that keeps this clean: **Api has no implementation, Core has no WorldEdit or PacketEvents, and only Spigot
touches those two libraries.** Core reaches the block pipeline through `BlockEditor` and the client through
`ClientEffects`, both defined in Api and implemented in Spigot. That is why a future port to another editor needs
one new class, not a change to any mechanic. Storage knows nothing of Bukkit or Core: the Spigot module opens it and
hands its store to the research service.

## Runtime composition

`CoreFactoryPlugin.onLoad` installs the bundled resource pack, and writes the achievements datapack's pack format
before the worlds load.

`CoreFactoryPlugin.onEnable`, in order:

1. Checks that `packetevents` and one of `WorldEdit` / `FastAsyncWorldEdit` are loaded; disables itself otherwise.
2. Saves `config.yml` and writes any missing data file from the jar. `DefaultsMerger.merge` first checks `processes.yml`
   against `DataVersion.CURRENT` (4): a file with no version line or an older one is copied to `processes.yml.bak` and
   replaced by the shipped file, and a newer one is left with a warning. It then adds the missing entries of the other
   data files (see [CONFIGURATION.md](CONFIGURATION.md#startup-data-merge) and
   [processes.yml and its version](CONFIGURATION.md#processesyml-and-its-version)). `DataVersion.CURRENT` is raised whenever
   the machines, processes or drills change.
3. Parses `research.yml` (`ResearchTree`), `processes.yml` (`ProcessCatalog`) and `ores.yml` (`OreDefinition`). These are
   read once, at start.
4. Creates `WorldEditBlockEditor` (block batches), `CoreFactoryCore(plugin, editor, PacketClientEffects)`, and
   `SpriteEffects`, which the core takes as its `Visuals`.
5. Loads `config.yml`, `tnts.yml`, `items.yml` and `materials.yml`, loads the placed-TNT registry, registers the
   integrations, and publishes the core through `CoreFactoryProvider`.
6. Opens research storage (`ResearchRuntime.open`) and sets the research gate. Players who are online load their research.
7. Registers the listeners: placement, ignition, protection, achievements, interaction, the GUI, geology, and the scanner.
8. Creates the hologram service, the drill catalog, the machine store (`machines.yml`), the lab tool and drill
   listeners, the hopper listener, the solar panel listener, and the interaction and concealment pieces
   (`MachineInteractions`, `BarrelConcealment`). It also creates the energy service (`energy.yml`) and the logistics
   service (`logistics.yml`), and registers their listeners.
9. Registers `/cf` and starts the repeating tasks: the core tick, the energy and logistics tick, the machine ticker,
   the geology drain, the recipe watch, the Geiger display, and the achievement save. It then starts the sprite effects
   and the holograms.

`onDisable` cancels the tasks, shuts the logistics and the energy services down (each saves its file and releases its
entities), stops the sprite effects, shuts the holograms down, saves the machines, drains the ore
queue, saves achievements and placed TNT, and closes research storage after its pending writes.

## Ignition and detonation

```
flint / fire / lightning / redstone / explosion
        │
        ▼
IgnitionListener  ──►  CoreFactoryCore.ignite(PlacedTnt, source)
                                  │
                       TntManager.ignite
                       ├─ fire TntIgniteEvent (cancellable)
                       ├─ mechanic.onIgnite(...)           (warnings, e.g. nuclear countdown)
                       └─ by IgnitionMode:
                            PRIMED_ENTITY  remove the block, spawn a tagged TNTPrimed
                            DELAYED        remove the block, schedule detonate after the fuse
                            PERSISTENT     detonate now (cooldown prevents relay loops)
                            INERT          nothing

TNTPrimed explodes ──► IgnitionListener.onEntityExplode ──► TntManager.onEntityExplode
                              ├─ our tag present: cancel vanilla explosion, remove entity,
                              │   detonate(definition, origin, source, direction, placed)
                              └─ otherwise: pull placed TNT out of the block list, ignite it later

TntManager.detonate:
   depth limit, rate limit, anchor-field check, fire TntDetonateEvent (cancellable),
   then mechanic.detonate(DetonationContext)
```

Lighting that does not come from a primed TNT goes through `IgnitionListener` as well: `BlockIgniteEvent` (fire, lava,
lightning, fire spread and the other vanilla causes), a flaming arrow or fire charge hitting a placed TNT
(`ProjectileHitEvent`), a flint and steel or fire charge used on it (`PlayerInteractEvent`), and redstone power reaching it
(`BlockPhysicsEvent`). The `BlockIgniteEvent`, interaction and physics handlers use `ignoreCancelled = true`, so an ignition
another plugin cancelled stays cancelled. `IgnitionRules.decide` then gives the verdict: a block that is not a placed custom
TNT is left to vanilla, a block that is already lit or that detonated on the same tick ignites only once, and every vanilla
cause is handled.

Every custom detonation also reaches `DeviceBlast` through `DetonationObserver`, with the TNT's radius. Vanilla
explosions reach it through their events, and it removes each lab tool, drill, energy device and logistics device whose block
centre lies within the radius (3D distance) and that no protection spares. Its vanilla radii are in the code: 3 for a creeper
(6 when powered), 6 for an end crystal, 1 for a fireball or wither skull, 4 for any other entity, and 5 for a block explosion.

A **primed TNT** is a real `TNTPrimed` entity, so the fuse, sound and smoke are vanilla. Its identity is stored in
its persistent data (`corefactory:primed_tnt_id`, and the direction if any). The `PlacedTnt` snapshot it came from
travels in a map keyed by the entity's id, so a teleport target or a fuse link is still known at detonation.

## The block pipeline

Mechanics never write blocks directly. They build a `BlockChangeSet` (primitive arrays of coordinates and
`BlockData`) or a `BlockGenerator` (a lazy source of such sets), and hand it to `submitBlockChanges` or
`submitBlockGenerator`.

```
mechanic ──► BlockChangeSet / BlockGenerator
                     │
              BlockJobs.submit
              ├─ size ≤ instant-threshold: write now
              └─ else: a job, advanced by up to blocks-per-tick writes each tick
                     │
              before each write: placed TNT at that position is unregistered (OverwriteHook)
                     │
              BlockEditor.apply(world, set, from, to)   ── WorldEditBlockEditor: one edit session per slice,
                                                          fast mode, closed to flush
```

A nuclear crater is a generator: it produces each column on demand, so a crater hundreds of blocks across never exists as
one object in memory.

## Placed TNT

`PlacedTntStore` maps each block position (world UUID and integer coordinates) to a `PlacedTnt`: type id,
direction, target, fuse links, owner, display id, and the proximity arming state. It is saved to
`placed-tnt.yml` periodically and on shutdown, and loaded on enable. <img src="images/tnt/anchor_tnt.png" width="24" height="24" align="absmiddle"> Anchor TNT also registers its field in
`ProtectionImpl`.

Two checks keep the registry honest. Every 100 ticks, entries whose block is no longer TNT are dropped (for
example after an unmanaged removal). And the block pipeline unregisters any placed TNT it overwrites.

## Per-tick work

`CoreFactoryCore.tick()` runs once per server tick. It resets the particle budget, runs each placed mechanic's
`onPlacedTick` at its interval (every 5 ticks it checks which are due), drops stale registry entries every 100
ticks, and saves on schedule. `BlockJobs` runs its own task only while there is work to do.

## Lab tools, drills and hoppers

A lab tool, a petroleum drill and an ore drill each stand on a barrel. The host block's inventory holds everything the machine owns: inputs, fuel and outputs (for a drill, its fuel slot and nine outputs). `machines.yml` keeps only each machine's position, type, progress and process. An ore drill from an earlier build stood on a dropper; `BarrelMigration` and `MachineStore.settle` move it to a barrel once its chunk loads.

- `MachineMenu` is a view of the barrel. It copies the barrel into its slots, skips any slot the player has changed, and
  writes the player's changes back. The engine, the hoppers and the menu all read and write the same barrel.
- `MachineTicker` advances each machine in a loaded chunk once per tick. A machine whose host block has gone is removed,
  and its items drop where the block was. Machines in unloaded chunks do not run.
- `HopperListener` handles `InventoryMoveItemEvent`. A move into a lab tool puts one item into the input or fuel slot that
  takes it. A move out takes one item from the first output slot. Any other move that touches a lab tool is cancelled.
  The decisions are in `HopperRules`, which is pure.
- `DrillTicker` advances each drill's `DrillState` by the efficiency of its power. The grid is one more source there: `DrillRules.withGrid` takes the network's satisfaction when it is stronger than the local power (see [Energy and logistics](#energy-and-logistics)). `DrillRules.petroleumPower` takes the best of three sources: the redstone power the drill's block receives, each daylight detector touching it (`signalEfficiency`, power over `DrillRules.FULL_SIGNAL`, which is 15), and charcoal in the fuel slot (`CHARCOAL_EFFICIENCY`, 0.35). Ties go to solar, then redstone, then charcoal. `DrillRules.orePower` gives an ore drill full power while it holds petroleum. Each full cycle pumps one petroleum from the nearest deposit in the box, which is kept, or mines the nearest registered ore block (`DrillRules.nearest`). `DrillBox` is the region a drill works in, and `DrillTable` gives each level's area, depth and cycle.
- `DrillRules.status` returns the status in this order: no power, output full, no target in the box, then running. `DrillStatus.text` gives the words. `DrillText` gives the rate, the power and the lines of the info item.
- `SolarPanelListener` handles the solar panel, which is a daylight detector. Breaking any daylight detector gives back the solar panel item.
- `BarrelMigration` moves the stacks of an older `machines.yml` into empty barrel slots once the chunk loads. A stack that has no empty slot stays in the file and is logged, so nothing is overwritten.

## Energy and logistics

Power and items each have a pure model in CoreFactory-Core, and devices that read the world in CoreFactory-Spigot. Both
models are plain data and functions, so their rules are tested without a server. The numbers are in one table per side.
The player-facing rules are in [MECHANICS.md](MECHANICS.md#electricity-and-logistics).

**Core (pure).**

- `io.corefactory.core.energy`: `EnergyTable` holds every number in watts, watt-hours and amperes: the pylon ranges and
  currents, the accumulator levels, the generators' output and fuel, and the demands. `Units` converts and formats them
  (W, kW, MW, Wh, kWh, V, A) and gives the grid's voltage, `V = 230 - (P / 230) * R`. `EnergyNetwork.solve` runs one tick on
  an `EnergySnapshot` and returns what each grid, consumer and accumulator did. `GridGraph` finds the grids: the connected
  groups of pylons and wired generators, from the wires, through `WireRules`, which says which wires may exist. `GridRegistry`
  keeps the persistent grid ids (each pylon's id, and `nextFreshId`), `GridPalette` gives each id its colour (16 colours,
  by id modulo 16), and `Grid` is one grid's record. `OutlineTracer` traces the boundary of a union of pylon squares
  (`square`, `coverOf`, `trace`). `Sky` gives the sun and the wind, `GeneratorFuel` the fuel each kind takes, and `Generator`
  is one generator's tick. `Pylon` and `PylonType` give the range squares and the capacities, `AccumulatorLevel` the
  accumulator sizes, and `ChartHistory`, `ChartMode` and `ChartRenderer` hold and draw a grid's chart samples.
- `io.corefactory.core.logistics`: `LogisticsTable` holds every number. `LogisticsNetwork` ticks the belts, pickers and
  splitters, downstream nodes first. `Topology` checks a layout (no merges, no head-on feeds, a picker or splitter fed
  from its input side only) and gives each belt its shape. `Lane` holds the items on one belt unit. `Dir`, `BeltShape`,
  `Picker` and `Splitter` describe the parts. `Port` is the interface for anything a belt, picker or splitter gives to
  or takes from, and `PortItems` maps an item on a belt to the item reference a process slot holds.

**Spigot (Bukkit).**

- `io.corefactory.spigot.energy`: `EnergyService` runs the network each tick, keeps each grid's chart history and draws
  the outlines. `DeviceRegistry` owns the devices and their entities, and `GeneratorDevice`, `AccumulatorDevice` and
  `EnergyDevice` hold one device's state. `GeneratorMenu` is the generator's menu, `WireSelection` the two-click wire tool,
  and `EnergyEffects` the particles and sounds. `EnergyGeometry` turns an outline into points. `EnergyText` and
  `GeneratorText` write the hologram lines, and `GridNames` names the colours. `EnergyFile` and `EnergyStore` read and write
  `energy.yml` (version 2, with the grid ids and the charts). `ChartService` makes the charts' holograms and their
  interaction entities, refreshes them, and cycles a mode on a right-click. `ChartCommand` runs `/cf grid`, `ChartRules`
  holds the name rule, the limits and the mode cycle, `ChartText` lays out a chart's lines, and `ChartSamples` turns the
  grid's reading into a history sample.
- `io.corefactory.spigot.logistics`: `LogisticsService` owns the devices and one network per world. `LogisticsPlacement`
  places them and refuses a placement with the message of the first failing check. `SidePlacement` decides a picker's or a
  splitter's placement against a side face, and whether a left-click breaks a device. `PlacementAim` gives a device's spot
  and direction from the face aimed at. `LogisticsListener` routes the clicks. `SplitterMenu` and `SplitterFilter` set a
  splitter's lists. `FuelPort` connects a network to a generator's fuel. The ports of a lab tool and a drill are in
  `io.corefactory.spigot.machine` (see below). `LogisticsFile` and `LogisticsStore` read and write `logistics.yml`.

**Ports.** A port is what a belt, picker or splitter gives to or takes from. `LogisticsPorts.at` finds the port of a block,
and there are these kinds:

- `LabPort` (`spigot.machine`): a lab tool's barrel. It accepts an item exactly when a hopper would, into an input slot, and
  it gives items from its output slots, first slot first. Fuel is never taken through it.
- `DrillPort` (`spigot.machine`): a drill's output slots, first slot first. It accepts nothing: `accepts` is false and
  `accept` is refused, because a drill's fuel comes from hoppers and by hand. The port keeps no items of its own, so the
  barrel is the only record.
- `FuelPort` (`spigot.logistics`): a generator's fuel. It accepts one fuel item at a time while the generator keeps fewer
  than `GeneratorFuel.CAP` (64), and it has no output.
- `VanillaContainerPort` (`spigot.machine`): a vanilla storage block, a chest, a trapped chest, a barrel, a shulker box or a copper
  chest, as a sink and as a source. As a sink, a belt's end, a picker's output or a splitter's output puts an item in exactly when
  the container has room, verified by `HopperTransfer.placeOne`, and the claim check asks for the owner of the device that puts.
  As a source, a picker's input, `peekOutput` shows the first plain stack and `takeOutput` removes exactly that one item, verified
  by `HopperTransfer.takeOne`; the claim check asks for the owner of the picker that takes. A double chest is one inventory, and its
  claim check asks both halves.
- `TurretPort` (`spigot.turret`): a machine gun turret's magazine, which a belt or a picker fills one round at a time. It gives nothing.

**Tick order.** One repeating task runs every server tick: `energy.tick()`, then `logistics.tick()`. The machine ticker
is registered after it, so lab tools and drills read this tick's satisfaction.

`EnergyService.tick`, in order:

1. Every 20 ticks: pending devices are added and missing entities are made.
2. Each loaded generator ticks once. A generator in an unloaded chunk gives nothing.
3. The solver runs once per world. Pylons and accumulators take part whether or not their chunk is loaded.
4. Each accumulator's hologram takes its new state.
5. Every 20 ticks (one second): each grid's chart history takes a sample, and the histories of grids that are gone are dropped.
6. Every 10 ticks: generator holograms, open generator menus and generator particles are updated, and the wires are drawn.
7. Wire starts older than 60 seconds expire.
8. Every `energy.save-interval-ticks`, `energy.yml` is saved.

`LogisticsService.tick`: every 20 ticks, missing entities are made. Each network of a loaded world runs once, with the
satisfaction its devices get from the energy service. Every 10 ticks, the picker and splitter holograms update. Every
`logistics.save-interval-ticks`, `logistics.yml` is saved.

`ChartService` runs on its own task: every `charts.refresh-ticks` (10), each chart with a player within 48 blocks of its top line
is redrawn. A chart's interaction entity is made when its chunk loads, and it cycles the mode on a
right-click.

**Satisfaction.** The solver gives each consumer a satisfaction from 0 to 1: the part of its demand it got.
`EnergyService.satisfaction(id)` returns it, and 0 for a consumer the solver did not see. A lab tool advances through
`WorkPace`: at satisfaction `s` it runs `s` of its process ticks on average. A drill reads the same value through
`DrillRules.withGrid`. Logistics nodes read it through `SatisfactionMap`, and run at satisfaction times their normal
speed: a node at 0 does nothing, and a node in an unloaded chunk reads 0.

**Grids, outlines and charts.** A grid is not stored. Each tick `GridGraph.components` finds the connected groups of pylons
and generators, `GridRegistry.assign` gives each group the id of the old grid whose pylons it still holds (the smallest such id when grids
join, or a fresh id when it holds none), and `EnergyNetwork.solve` builds the `Grid` records with their colour. The ids are saved in
`energy.yml`, with `next-grid-id`, so a grid keeps its id and colour across a restart. `EnergyService.outlinesNear` builds the
outlines for `/cf pylons show`: the grid's pylons, `OutlineTracer.coverOf` for their squares, `trace` for the loops, and
`EnergyGeometry.loops` for the points at 0.3 blocks above the lowest pylon. `EnergyEffects.showOutlines` draws them every 10
ticks for the seconds asked. A chart reads its grid's sample on each refresh: `ChartSamples.sample` builds it from the grid's
result and its accumulators' storage, `ChartText.lines` lays out the text, and `ChartRenderer` draws the bars. The history is a
`ChartHistory` ring of 48 samples per grid in memory, so it is not saved and starts empty after a restart.

**Devices.** An energy or logistics device is placed on air, so it has no vanilla block, only its entry in the saved
file. Its look is an item display, its clicks go to an invisible interaction entity, and a hologram shows its state:
pylons, accumulators, generators, pickers and splitters have one. Belts have none. The display and the interaction entity
carry a PDC marker with the device's id. On chunk load, `sweep` removes marked entities that no device owns. On chunk
unload, the device forgets its entity handles and keeps its data. The entities are made again within 20 ticks after the
chunk loads.

**Wires.** A wire is saved data, drawn every 10 ticks. It joins two pylons, or a generator to a pylon. Two generators are
never wired together, and a consumer or an accumulator never takes a wire. A pylon that is wired to another pylon is in the
same grid as it.

## Barrel concealment

`BarrelConcealment` hides each machine's barrel from the players near it. The server keeps the barrel. Every 40 ticks (`PASS_TICKS`) it decides each machine's view with `ConcealmentRules.decide`, which is pure: it returns the players to conceal, to refresh and to restore. The changes go out through `ClientEffects`, one block-change set per player. `showBlocks` sends air at the host block, and `restoreBlocks` sends the real block back.

- A machine is concealed only when it is eligible: concealment is on, the machine is settled and its chunk is loaded, it has a host, and its host block is still that material.
- A player is in range when they stand in the machine's world and within `concealment.range` of the centre of the host block.
- Each machine gets one invisible `Interaction` entity over its host block, made by `attach` when it is missing. Its id is stored on the machine, and the entity is marked with a persistent key, so an entity left over from an earlier run is removed when its chunk loads (`sweep`).
- `MachineInteractions` routes the clicks on that entity with `InteractionRoute`, which is pure. A main-hand right-click opens the menu through `MachineListener.openMenu`. A main-hand left-click fires a `BlockBreakEvent` for the host block, so the break handler and any protection plugin see it, and the host becomes air when no one cancels it. Off-hand clicks do nothing.
- `MachineStore.onRemoved` tells `BarrelConcealment`, which shows the real barrel again and removes the entity. A chunk unload forgets the views without packets, since the client drops the chunk with them.

## Turrets and two-block devices

The machine gun turret is a package of its own in the Spigot module, `turret`. It is an entity device like the energy devices:
`TurretRegistry` holds the placed turrets and their entities, `TurretService` runs the one scheduled tick that targets, aims,
fires and reloads every turret, and `TurretStore` saves them in `turrets.yml`. The pure rules are in Core, package `turret`:
`TargetRules` (the ordered verdict on a mob), `TurretAim` (the yaw and pitch maths), `Magazine` (one round kind at a time),
`ArmourBypass`, `FireRate` (the interval from the power share) and `TurretSettings` (the `turrets` section). The levels are `TurretLevels` and `TurretLevel` (the stats of levels 1 to 5, read from the `levels` blocks), `TurretUpgrade` (the rule of a kit), `TargetAssignment` (the coordinated choice of each cannon's target across the turrets of a world, in three passes: sticky claims, spread and share), `DualTargeting` (the cannon count and the dual spacing) and `ModelKeys` (the names of the models of each level). The turret reaches
the logistics network through `TurretPort`, which `LogisticsPorts` returns for a block that holds a turret.

The laser turret is the second kind of turret, `TurretKind.LASER`, in the same package. It holds no rounds and fires a beam each
shot, so its fire path is `TurretService.fireLaser`. Its stats are `LaserSettings` (the `laser-turret` section), and its rules are
`LaserRules` and `BeamPlan` in Core. `LaserEffects` draws its particles under a per-tick budget, and `LaserLight` places and removes
the light block and the glow of a shot.

A two-block device is one anchor and one more block above it. `Footprint` in Core (`TALL_IDS`) says which ids are tall, and
`DevicePart` in Spigot maps a clicked block back to the anchor. Every lookup from a block to a device, the break, the
placement and the blast's reach, goes through `DevicePart`, so the rule is in one place.

## Holograms

`HologramService` shows a hologram over each running lab tool and drill. A hologram is a stack of invisible marker armour stands, one per line, that exist only as PacketEvents packets. The top line stands 1.6 blocks above the host block, and each line below it is `Hologram.LINE_SPACING` (0.25) blocks lower.

- `HologramService.create(Location, List<String>)` returns an `Optional<Hologram>` with one line per text, top first. It is empty when holograms are off, the point has no world, or the list is empty.
- `Hologram.setLines(List<String>)` changes the text. A longer list is cut to the line count fixed at creation, and a shorter one is padded with empty lines. The viewers see the change at the next text update.
- `HologramService.remove(Hologram)` hides every line from the viewers and drops the hologram.
- A player sees a hologram when they are in its world and within `holograms.view-distance` blocks. The viewers are checked every 20 ticks, and the text is sent every 10 ticks.
- `MachineTicker` shows a lab tool's three lines (`HologramText.forMachine`) while `HologramRules.shouldShow` says so. `DrillTicker` shows a drill's four lines (`HologramText.drillLines`) while the drill has power and room. Both add the network line at the bottom (`EnergyText.withGrid`). Otherwise `MachineStore.releaseHologram` removes it.
- `HologramText` is pure formatting. `progressBar` draws the bar of 20 segments, and `percent` truncates the percentage, so a pass that has not finished never reads 100%.
- A packet that fails turns holograms off for the rest of the run and logs the failure once.

## Effects and Visuals

The core asks for a named effect with `Effects.visual(name, at, scale)`, which calls `Visuals.play`. `Visuals` is a
functional interface in `io.corefactory.core.engine`. Until the platform supplies one it is `Visuals.NONE`, which shows
nothing. The Spigot plugin passes its `SpriteEffects` with `CoreFactoryCore.setVisuals`.

`VisualCatalog` lists the 22 named effects. Each has a shape, the sprite ids it draws from, a body count and a lifetime.
The shapes are `SPHERE_BURST`, `RING`, `COLUMN`, `DRIFT`, `FLASH`, `SPIRAL`, `PUFF`, `BURST` and `BEAM`. The geometry is
pure, and `SpriteEffects` turns it into item displays, one per sprite, and stops at `effects.max-sprites`.

The effects are `nuclear_fireball`, `nuclear_shockwave`, `nuclear_surge`, `nuclear_mushroom`, `nuclear_fallout`,
`neutron_burst`, `antimatter_flash`, `tesseract_fold`, `machine_work`, `research_complete`, `ore_break`,
`geiger_crackle`, and the ten lab effects `lab_crusher`, `lab_roaster`, `lab_separator`, `lab_purifier`,
`lab_converter`, `lab_enrichment`, `lab_transmuter`, `lab_spectrometer`, `lab_accelerator` and `lab_condenser`.

The sprites are the 15 ids in `VisualCatalog.SPRITES`. Their art is in the resource pack, see [TEXTURES.md](TEXTURES.md#6-effect-sprites).

A mushroom cloud is a column. Its height is `10 + 12 * log2(1 + yield)` blocks, with the yield clamped to 0.5 to 50
megatons. Its body is 40 sprites at a height of 26 blocks or less, and up to three times that for taller clouds.

`MachineTicker` plays `machine_work` while a machine runs, at most every 40 ticks, and plays the machine's own lab effect
when a pass completes. The research console has its own screen and no lab effect.

### Sounds

`MachineSounds` (Spigot, package `audio`) plays the pack's sounds at a block, with a throttle per device: each device takes its
turn on a cycle of its block coordinates, so no state is kept. The sound events come from `sounds.json`, which the resource-pack
builder writes from the OGG files in `art/sounds` (`SoundPack`). The sounds are built by `tools/sounds/make_sounds.py`, which downloads and edits the
recordings listed in `tools/sounds/sources.json` (with ffmpeg) and synthesises the rest. It needs an ffmpeg at build time only.

## Storage

The `CoreFactory-Storage` module holds the research store and nothing else.

- `StorageSettings` is the record of the `storage:` section. Any `storage.type` other than `postgresql` means SQLite.
- `Database.open` chooses the driver. `DriverLoader.ensure` downloads it when it is not on the classpath, checks it
  against its SHA-1, and loads it in its own class loader from `libraries/`. See [CONFIGURATION.md](CONFIGURATION.md#database-drivers).
- The HikariCP pool has one connection for SQLite, with a busy timeout of 5 seconds. PostgreSQL uses `pool-size`.
- `SchemaMigrations.apply` runs at open. It keeps a version in the `corefactory_schema` table, and each migration runs in
  its own transaction. The research tables are `corefactory_research_unlocks` and `corefactory_research_progress`.
- `JdbcResearchStore` implements `unlocked`, `unlock`, `progress` and `setProgress`. It reads and writes through
  `Database.query` and `Database.execute`.

`ResearchRuntime` opens the store from `config.yml`. When the database cannot open, it keeps research in memory for that
start and logs a SEVERE message. The research writes run on a single background thread, which `close` waits for.

Achievements, lab tools and drills, and placed TNT are not in the database. They are in `achievements.yml`,
`machines.yml` and `placed-tnt.yml`.

## Research

`research.yml` becomes a `ResearchTree` when the server starts. Each node names the node before it, the research packs it
costs, and what it unlocks. `ResearchService` grants nodes and answers whether a node is finished. `ResearchStore` keeps
the grants, and `ResearchGateListener` cancels any craft, placement or ignition of an id that a node gates and the
player has not finished. A Research Console (`ResearchConsoleUi`) takes the packs in its six input slots. `ResearchCommand`
is the admin command, and a finished node is an achievement (see [ACHIEVEMENTS.md](ACHIEVEMENTS.md)).

## Geology and the survey

- `GeologyService` plans the veins of a chunk from the world seed and the chunk coordinates. `VeinPlanner` is the random
  walk, and it takes blocks only from an ore's `replace` set. The same seed and chunk always give the same veins.
- A chunk's ore marker and its registry of placed ore blocks are kept in the chunk's persistent data. `GeologyWorker`
  queues new chunks as they generate, and `/cf geology retrofit` queues old ones. It lays `geology.chunks-per-tick`
  chunks per tick.
- The Ore Density Scanner (`ScannerListener`) plans the chunk with hosts included. `ChunkSurvey` reduces the planned
  blocks to one dominant ore and a count per 16 by 16 column. `SurveyRenderer` paints the 128 by 128 map, with each column
  as an 8 by 8 cell. The colour is the ore's colour from `OreColours`, darker for fewer blocks, and full at 64 blocks.
  The "still in the ground" count comes from the registry, not from the plan.

## Why it looks like this

- **One pipeline for every block write.** Staging, protection and the placed-TNT bookkeeping live in one place, so
  mechanics stay short and cannot forget any of them.
- **Mechanics are stateless.** All numbers arrive in `DetonationContext.settings()`, so one class serves every tier
  of the nuclear family, and a server admin can add a TNT type by writing YAML.
- **Effects are modifiers, not subclasses.** Shockwave, debris, echo and silent are flags on a standard blast.
- **Client-only effects stay client-only.** Daytime, weather and beams are PacketEvents packets. They never touch the
  world, so restoring a player's view is just re-sending the real blocks and weather. The clock hold is the one effect
  that also rewrites the server's own time packets on their way to that player, because the server would otherwise
  undo it once a second.
