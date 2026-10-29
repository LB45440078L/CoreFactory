# Architecture

## Platform

The plugin targets the Paper API only. The default Maven profile, `api-26.3`, compiles against Paper API 26.3
(`paper.api.version` in the root pom), and the plugin runs on Paper and Purpur. Folia is not supported. The other
`api-*` profiles in the root pom still build the same sources, but they are not supported.

The descriptor is `paper-plugin.yml` with `api-version: '26.3'`. Its `dependencies: server` section lists the soft
dependencies WorldEdit or FastAsyncWorldEdit (one of them is required, and the plugin disables itself without one),
WorldGuard, GriefPrevention and ResourcePackManager. It names no PacketEvents. Commands are registered in code through
Paper's Brigadier command API, and permissions when the plugin enables (`CorePermissions`). The Paper module's packages
are still named `io.corefactory.paper`.

All text is Adventure components. The colours are RGB values from `Palette` (in Core's `text` package): `accent`,
`command`, `arg`, `value`, `muted`, `hint`, `good`, `bad`, `warn` and `research`. `Text.parse` reads both the legacy
ampersand codes, which map onto the palette, and MiniMessage tags, so a config message can use either.

## Modules and their dependencies

```
CoreFactory (parent pom; default profile api-26.3, Paper API 26.3)
├── CoreFactory-Api          Paper API only
│     public contracts: Mechanic, TntDefinition, MechanicSettings, DetonationContext,
│     PlacedTnt, BlockChangeSet, BlockGenerator, BlockEditor, ClientEffects,
│     ProtectionService, CoreFactoryApi, CoreFactoryProvider, TntIgniteEvent, TntDetonateEvent,
│     PackVersion
├── CoreFactory-Storage      HikariCP and the JDBC drivers. JDBC research store, JDBC deposit store, schema migrations, runtime driver loading.
│     No Bukkit. The deposit store implements Core's `DepositStore`, so Storage depends on Core's deposit types.
├── CoreFactory-Core         Api + Paper API
│     CoreFactoryCore (the implementation of CoreFactoryApi), TntManager, Blast, BlockJobs,
│     Effects, Visuals, VisualCatalog, Explosions, Shapes, Tasks, ProtectionImpl, PlacedTntStore,
│     PlacedDisplays, ItemFactory, RecipeRegistrar, DefinitionParser, CoreSettings, Messages,
│     mechanics/*  (every built-in mechanic)
│     process/*    (lab tool processes: ProcessCatalog, ProcessEngine, Machine)
│     drill/*      (drill levels, efficiency and status: DrillTable, DrillRules, DrillKind, DrillStatus, DrillText, DrillCatalog, DrillState, DrillDeposits)
│     deposit/*   (the deposit model, pure: Deposit, DepositPlanner, DepositSettings, DepositStore, MemoryDepositStore, DrillAccumulator, DrillYield, TakeBatch, OreBlocks)
│     geology/*   (the placed ore blocks that a player puts down: GeologyService, GeologyRegistry, OreDefinition, DepositVolumes, Placement)
│     research/*   (the research tree and service: ResearchTree, ResearchService, ResearchStore)
│     hologram/*   (progress text, rules and layout: HologramText, HologramRules, HologramLayout, Tone)
│     oil/*        (the oil network, pure: Fluid, FluidRules, FluidTank, OilNetwork, OilPart, OilSettings, OilModels, Refinery, FluidPhysics)
│     text/*       (the colours and the text helpers: Palette, Text)
│     radiation/*  (fallout, the suit, the Geiger counter)
│     energy/*     (the power network, pure: EnergyTable, Units, EnergyNetwork, GridGraph, GridRegistry, GridPalette, OutlineTracer, ChartHistory, ChartRenderer, Generator, Sky)
│     logistics/*  (belts, pickers and splitters, pure: LogisticsTable, LogisticsNetwork, Topology, Lane, Port)
│     water/*     (the water network, pure: WaterFlow, WaterGrid, WaterNode, WaterOrientation, WaterFrame, WaterRules, PumpFuel, PumpLevels, WaterSource, WaterText, WaterSettings)
│     vertical/*  (vertical belt towers, pure: VerticalTowers, VerticalCarry, VerticalTower, VerticalLevels)
├── CoreFactory-ResourcePack Build-time only. snakeyaml. Draws textures; writes the pack zip, which the Paper build copies into the jar.
└── CoreFactory-Paper        Api + Core + Storage + ResourcePack, Paper API, WorldEdit and WorldGuard (provided by the server)
      the plugin jar, CoreFactory-Paper-1.0.0.jar. Shades Api, Core, Storage and HikariCP. Holds paper-plugin.yml, config.yml and the six
      data files. WorldEditBlockEditor, BukkitClientEffects, listeners, /cf (CfCommand), the catalogue GUI, lab tool
      and drill machines, barrel concealment, holograms, device screens, the energy, logistics, water and oil devices, sprite
      effects, the startup data merge. geology/*: DepositGenerator, DepositChunkListener, GeologyCommand, and the
      placed-ore-block classes GeologyListener, GeologyWorker, OreChunks. ledger/*: DepositLedger (the deposit store). scanner/*: the Ore Density Scanner, its map, outlines and waypoints (DepositScanner, DepositMap, DepositOutline, WaypointService). oil/*: OilService and the tube listeners. hook/*: the Paper implementations of the API's hooks. screen/*: the device screens (DeviceScreen, ScreenGeometry, ScreenText). scheduler: TickScheduler and TickPlan, the one tick scheduler.
```

The rule that keeps this clean: **Api has no implementation, Core has no WorldEdit, and only Paper touches that library.**
Core reaches the block pipeline through `BlockEditor` and the client through `ClientEffects`, both defined in Api and
implemented in Paper (`WorldEditBlockEditor` and `BukkitClientEffects`). That is why a future port to another editor needs
one new class, not a change to any mechanic. Storage knows nothing of Bukkit. It depends on Core only for the deposit types (`Deposit` and `DepositStore`) that
`JdbcDepositStore` implements; the Paper module opens it and hands its stores to the research service and the drills.

## Runtime composition

`CoreFactoryPlugin.onLoad` installs the bundled resource pack, and writes the achievements datapack's pack format
before the worlds load.

`CoreFactoryPlugin.onEnable`, in order:

1. Checks that one of `WorldEdit` / `FastAsyncWorldEdit` is loaded; disables itself otherwise.
2. Saves `config.yml` and writes any missing data file from the jar. `DefaultsMerger.merge` first checks `processes.yml`
   against `DataVersion.CURRENT` (12, in `io.corefactory.paper.config`): a file with no version line or an older one is copied to `processes.yml.bak` and
   replaced by the shipped file, and a newer one is left with a warning. It then adds the missing entries of the other
   data files (see [CONFIGURATION.md](CONFIGURATION.md#startup-data-merge) and
   [processes.yml and its version](CONFIGURATION.md#processesyml-and-its-version)). `DataVersion.CURRENT` is raised whenever
   the machines, processes or drills change.
3. Parses `research.yml` (`ResearchTree`), `processes.yml` (`ProcessCatalog`) and `ores.yml` (`OreDefinition`). These are
   read once, at start.
4. Creates `WorldEditBlockEditor` (block batches), `CoreFactoryCore(plugin, editor, BukkitClientEffects)`, and
   `SpriteEffects`, which the core takes as its `Visuals`.
5. Loads `config.yml`, `tnts.yml`, `items.yml` and `materials.yml`, loads the placed-TNT registry, registers the
   integrations, and publishes the core through `CoreFactoryProvider`.
6. Opens research storage (`ResearchRuntime.open`) and sets the research gate. Players who are online load their research.
   The deposit store is opened with it (`DepositLedger.open`): in the research database when that is open, or in memory
   otherwise.
7. Registers the listeners: placement, ignition, protection, achievements, interaction, the GUI, and geology. The geology
   listeners are `DepositChunkListener`, which lays the deposits of a chunk on its first load, and `GeologyListener`, which
   handles the placed ore blocks.
8. Creates the hologram service, the drill catalog, the machine store (`machines.yml`), the lab tool and drill
   listeners, the hopper listener, the solar panel listener, and the interaction and concealment pieces
   (`MachineInteractions`, `BarrelConcealment`). It also creates the energy service (`energy.yml`), the water service
   (`water.yml`), the oil service (`oil.yml`), the vertical belt service (`vertical.yml`) and the logistics service
   (`logistics.yml`), and registers their listeners.
9. Registers `/cf` and the recipe restore listener, and starts the tick scheduler. The features that repeat on it, and
   the ones that start their own actions (the sprite effects, the charts and the barrel concealment),
   are listed under [Tick scheduler](#tick-scheduler).

`onDisable` cancels the tick scheduler, shuts the turrets, oil, water, logistics, charts and energy services down (each
saves its file and releases its entities), stops the sprite effects and the barrel concealment, shuts the holograms down,
saves the machines, saves achievements and placed TNT, and closes research storage after its pending writes.

**Configuration.** Core reads its settings as plain data. The Paper module reads `config.yml` with the Bukkit
configuration API and hands Core a map (`configData()`), so Core has no Bukkit configuration types. Core still uses
Bukkit value types, such as `Location`, `Material` and `ItemStack`.

**Recipes.** `RecipeRestoreListener` registers the plugin's recipes again when Paper fires `ServerResourcesReloadedEvent`.
Nothing polls for them. A `/reload` drops the recipes and the event restores them. Unverified in game: the recipe restore
after `/reload`.

## Tick scheduler

`TickScheduler` (Paper module) is one Bukkit repeating task that runs once per server tick. It drives `TickPlan`, which
has no Bukkit types and is tested without a server. Each action has a period in ticks and first runs after one period.
The actions due on one tick run in the order they were added, and an action that throws is reported, so the actions
after it still run on that tick. The order is the order of the calls in `CoreFactoryPlugin.onEnable`, which gives:

1. `turrets`, every tick, and `railgun-shots`, every tick (`TurretRuntime.register`, before the core is registered).
2. `artillery`, every tick, `artillery-shots`, every tick, `artillery-hazards`, every 10 ticks (`ArtilleryHazards.STEP_TICKS`), and `command-center`, every tick (`ArtilleryRuntime.register`, also before the core).
3. `waypoints`, every 5 ticks (`WaypointService`, when `DepositScanner` is built, also before the core).
4. `core`, every tick.
5. `achievement-save`, every 6000 ticks.
6. `geiger`, every 10 ticks.
7. `resources`, every 40 ticks.
8. `oil`, every tick.
9. `energy`, every tick: the energy network, then the logistics network.
10. `water`, every tick.
11. `machines`, every tick (`MachineTicker`).
12. `chunks`, every 100 ticks, first run on the next tick (`ChunkKeeper.reconcile`).
13. `sprites`, every tick (`SpriteEffects`, registered by `sprites.start()`).
14. `charts`, every `charts.refresh-ticks` (default 10) (`ChartService`, registered by `charts.start()`).
15. `concealment`, every 40 ticks, first run after one tick (`BarrelConcealment`, registered by `concealment.start()`).

Two more actions are added on demand and removed when they end. `artillery-targeting` runs every tick while a player has a
listening session open on a command center (`ListeningMode`). `DepositOutline` and `EnergyEffects` each add one outline
action that runs every 10 ticks until its seconds are up or the player leaves.

Core keeps its transient per-event tasks on the Bukkit scheduler and does not use `TickScheduler`: `Tasks`, `BlockJobs`
and `MilitaryMechanics` start their own tasks with `Bukkit.getScheduler()`.

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
explosions reach it through their events, and it removes each lab tool, drill, energy device, logistics device, water part, oil tube and vertical belt whose block
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

A lab tool, a petroleum drill and an ore drill each stand on a barrel. The host block's inventory holds what the machine owns: a lab tool's inputs, fuel and outputs, and an ore drill's nine output slots. A drill has no input or fuel slot, and a petroleum drill has no output slots either, since it pumps into the oil network. `machines.yml` keeps only each machine's position, type, progress and process. An ore drill from an earlier build stood on a dropper; `BarrelMigration` and `MachineStore.settle` move it to a barrel once its chunk loads.

- `MachineMenu` is a view of the barrel. It copies the barrel into its slots, skips any slot the player has changed, and
  writes the player's changes back. The engine, the hoppers and the menu all read and write the same barrel.
- `MachineTicker` advances each machine in a loaded chunk once per tick. A machine whose host block has gone is removed,
  and its items drop where the block was. Machines in unloaded chunks do not run.
- `HopperListener` handles `InventoryMoveItemEvent`. A move into a lab tool puts one item into the input or fuel slot that
  takes it. A move out takes one item from the first output slot. Any other move that touches a lab tool is cancelled.
  The decisions are in `HopperRules`, which is pure.
- `DrillTicker` searches the deposits every 20 ticks through `DepositStore`, and at once when its deposit runs out. The
  search finds the deposits whose circle touches the drill's square, and the drill works on one of them at a time (the
  one it has, or else the mineable one with the largest remaining fraction, so every deposit in the square is mined in turn). Its items are made at the drill's rate and kept in a take
  batch (`DrillMining`, which also holds the search state). The batch is taken from the deposit in lots of ten, or when
  the oldest item has waited a second, and the taken items go into the output slots. `DrillRates` reads `drill-rates`
  from `config.yml`. Every drill is powered by the energy network alone: `DrillRules.efficiency` clamps the satisfaction its
  grid gives it to 0 to 1, and that is the drill's efficiency (see [Energy and logistics](#energy-and-logistics)). The
  cycle advances by the efficiency each tick. A petroleum drill pumps `rate x efficiency` litres a tick into the oil network
  through `OilService`; an ore drill's batch of items grows at `rate x efficiency`, so the rate scales with the grid as it does
  for a lab tool. No drill draws from the oil network or burns charcoal. `DrillBox` is the region a drill works in, and
  `DrillTable` gives each level's area, depth and cycle.
- `DrillRules.status` returns the status in this order: no power (the grid gives nothing), output full, no deposit in range, then running. A
  drill whose deposits in range are all empty reports `EXHAUSTED` ("Deposit exhausted"), and the drill stays there until
  it is moved. `DrillStatus.text` gives the words. `DrillText` gives the rate, the power and the lines of the info item.
- `SolarPanelListener` handles the solar panel, which is a daylight detector. Breaking any daylight detector gives back the solar panel item.
- `BarrelMigration` moves the stacks of an older `machines.yml` into empty barrel slots once the chunk loads. A stack that has no empty slot stays in the file and is logged, so nothing is overwritten.

## Energy and logistics

Power and items each have a pure model in CoreFactory-Core, and devices that read the world in CoreFactory-Paper. Both
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
  or takes from, and `PortItems` maps an item on a belt to the item reference a process slot holds. `CrafterGrid` is a
  crafter's nine-slot grid as a port, with all of its slot rules.

**Paper module.**

- `io.corefactory.paper.energy`: `EnergyService` runs the network each tick, keeps each grid's chart history and draws
  the outlines. `DeviceRegistry` owns the devices and their entities, and `GeneratorDevice`, `AccumulatorDevice` and
  `EnergyDevice` hold one device's state. `GeneratorMenu` is the generator's menu, `WireSelection` the two-click wire tool,
  and `EnergyEffects` the particles and sounds. `EnergyGeometry` turns an outline into points. `EnergyText` and
  `GeneratorText` write the hologram lines, and `GridNames` names the colours. `EnergyFile` and `EnergyStore` read and write
  `energy.yml` (version 2, with the grid ids and the charts). `ChartService` makes the charts' holograms and their
  interaction entities, refreshes them, and cycles a mode on a right-click. `ChartCommand` runs `/cf grid`, `ChartRules`
  holds the name rule, the limits and the mode cycle, `ChartText` lays out a chart's lines, and `ChartSamples` turns the
  grid's reading into a history sample.
- `io.corefactory.paper.logistics`: `LogisticsService` owns the devices and one network per world. `LogisticsPlacement`
  places them and refuses a placement with the message of the first failing check. `SidePlacement` decides a picker's or a
  splitter's placement against a side face, and whether a left-click breaks a device. `PlacementAim` gives a device's spot
  and direction from the face aimed at. `LogisticsListener` routes the clicks. `SplitterMenu` and `SplitterFilter` set a
  splitter's lists. `FuelPort` connects a network to a generator's fuel. The ports of a lab tool and a drill are in
  `io.corefactory.paper.machine` (see below). `LogisticsFile` and `LogisticsStore` read and write `logistics.yml`.

**Ports.** A port is what a belt, picker or splitter gives to or takes from. `LogisticsPorts.at` finds the port of a block,
and there are these kinds:

- `LabPort` (`paper.machine`): a lab tool's barrel. It accepts an item exactly when a hopper would, into an input slot, and
  it gives items from its output slots, first slot first. Fuel is never taken through it.
- `DrillPort` (`paper.machine`): a drill's output slots, first slot first. It accepts nothing: `accepts` is false and
  `accept` is refused, because a drill has no input slots. The port keeps no items of its own, so the
  barrel is the only record.
- `FuelPort` (`paper.logistics`): a generator's fuel. It accepts one fuel item at a time while the generator keeps fewer
  than `GeneratorFuel.CAP` (64), and it has no output.
- `VanillaContainerPort` (`paper.machine`): a vanilla storage block, a chest, a trapped chest, a barrel, a shulker box or a copper
  chest, as a sink and as a source. As a sink, a belt's end, a picker's output or a splitter's output puts an item in exactly when
  the container has room, verified by `HopperTransfer.placeOne`, and the claim check asks for the owner of the device that puts.
  As a source, a picker's input, `peekOutput` shows the first plain stack and `takeOutput` removes exactly that one item, verified
  by `HopperTransfer.takeOne`; the claim check asks for the owner of the picker that takes. A double chest is one inventory, and its
  claim check asks both halves.
- `CrafterPort` (`paper.machine`): a crafter's nine grid slots, as a picker's source and as a sink for a belt's end or a
  picker's output. The slot rules are in Core's `CrafterGrid`: a picker takes from the first enabled slot that holds a plain
  item, in slot order, and an item goes into the first enabled slot that holds it with room, else the first enabled empty
  slot. A slot the crafter disables is neither offered nor filled, and its items stay. The claim check is the chest's. The
  port holds no product: a crafter crafts only on a redstone pulse, and vanilla then ejects the product into the block it faces.
- `TurretPort` (`paper.turret`): a machine gun turret's magazine, which a belt or a picker fills one round at a time. It gives nothing.

**Tick order.** The energy action runs `energy.tick()`, then `logistics.tick()`, on every server tick (see
[Tick scheduler](#tick-scheduler)). The machine ticker is registered after it, so lab tools and drills read this tick's
satisfaction.

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

`ChartService` registers its refresh with the tick scheduler: every `charts.refresh-ticks` (10), each chart with a player within 48 blocks of its top line
is redrawn. A chart's interaction entity is made when its chunk loads, and it cycles the mode on a
right-click.

**Satisfaction.** The solver gives each consumer a satisfaction from 0 to 1: the part of its demand it got.
`EnergyService.satisfaction(id)` returns it, and 0 for a consumer the solver did not see. A lab tool advances through
`WorkPace`: at satisfaction `s` it runs `s` of its process ticks on average. A drill reads the same value through
`DrillRules.efficiency`. Logistics nodes read it through `SatisfactionMap`, and run at satisfaction times their normal
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

## Water network

The water system is two packages. `io.corefactory.core.water` holds the rules, with no Bukkit types: `WaterFlow` shares a
network's water among its consumers by max-min fair water-filling and works out each pump's output; `WaterGrid` and
`WaterNode` are the network graph and the joins; `WaterOrientation` gives the pipe model (straight or a bend, from its joins) and
the splitter faces; `WaterFrame` maps a model's axes to world faces, which turns a bend, a T or an X into any plane, vertical
included; `WaterRules` decides the joins and the nuclear generator rule; `PumpLevels` and `PumpFuel` hold the pump levels (the
tall levels 4 and 5 fill two blocks, `PumpLevels.height`), kits and fuel; `WaterSource` counts the water a network reaches, by a
flood fill from each block that stands in or touches water, capped at the minimum plus one; `WaterText` makes the status lines.

`io.corefactory.paper.water` runs them. `WaterService` owns the placed parts, solves every `solve-ticks` (5), checks each pump's
source every `source-check-ticks` (200), burns fuel or draws power, and implements `WaterFeed`. The machines and the nuclear
generators call `WaterFeed` only: `canSupply` (a lab process that needs a water bucket skips the bucket when it returns true),
`waterLine` (the `Water: x of y /s` line) and `nuclearMayRun`. The pumps at levels III to V are consumers of the energy network. A level II pump burns diesel that it draws from the
oil network (`PumpFuel.FLUID`, one litre a minute); level I burns coal or charcoal.
`WaterListener` takes the clicks, `WaterPlacement` makes the placement rules and refusals, `WaterEntities` draws the parts and
their pipe models (a tall pump has a second display on the block above, and one interaction two blocks high), and `WaterStore` reads and writes `water.yml`. The part-to-model mapping is in `WaterEntities`; the placed
state is in `WaterDevice`. See [MECHANICS.md](MECHANICS.md#water) for the rules.

## Oil network

Petroleum and diesel are fluids, not items: each is a volume in litres inside an oil network. The rules are in
`io.corefactory.core.oil`, with no Bukkit types: `Fluid` (the two fluids, with their energy per litre), `FluidTank`
(one fluid kind up to a capacity), `OilNetwork` (the tanks of one network, which supply and draw in proportion to free
space and content), `FluidRules` (a network holds one fluid kind), `Refinery` (3 L of petroleum give 2 L of diesel over
600 ticks at full speed), `FluidPhysics` (how fast a generator burns a litre) and `OilSettings` (`oil.tube-capacity-litres`
and `oil.pipe-flow-litres-per-second`). `io.corefactory.paper.oil` runs them. `OilService` owns the tubes (`oil_pipe`,
`oil_splitter_t`, `oil_splitter_x`) and their networks, rebuilds the networks from the loaded blocks each tick, and
applies the pipe flow once per 0.05 s tick. `OilPlacement` and `OilListener` place the tubes and refuse a join that
would mix the fluids, and `MixedNetworks` settles a saved network that holds two fluids at start-up (the minority fluid
is emptied and a warning is logged; on a tie, petroleum is kept). `OilStore` saves the tubes with their fluid in
`oil.yml` (version 2). The joins are decided by `OilJoinPlan` in Core, from the tubes and the devices beside them. A
merging device (a petroleum drill, which gives; the generators and the level II pump, which take) joins every
network its footprint touches into one. The refinery is separate: it is not a node of the grid, so each network beside it
stays its own, and it takes petroleum from each intake and gives diesel into each output (a network that holds diesel, or
is empty with a diesel consumer on it). A network whose fluids disagree moves nothing. Unverified in game: the network's
flow and the refinery's output faces.

## Vertical belts

The vertical belts are in `io.corefactory.core.vertical` (`VerticalTowers` finds a tower's shape and sets the direction of the
whole tower, the cascade; `VerticalCarry` keeps the items and their heights; `VerticalTower` and `VerticalLevels` hold the shape and
the per-level speed and capacity) and in `io.corefactory.paper.vertical`. `VerticalService` owns the towers, moves their items once
per tick, gives the item at the output end to the block beyond it, and shows the items as displays. `VerticalListener` places the
belts and opens the menu (`VerticalMenu`); `VerticalStore` reads and writes `vertical.yml`. `LogisticsService` gives a tower's input
port to a belt or picker whose output cell is a tower belt, and the logistics tick runs `VerticalService.tick`. See
[MECHANICS.md](MECHANICS.md#vertical-transport-belts).

## Lab machine levels

`io.corefactory.core.upgrade.LabLevels` holds the five levels, the work pace from `machine-upgrades.speed`, the kit detection
(`machine_upgrade_2` and `machine_upgrade_3` only) and the model ids. `MachineListener.applyKit` applies a module by right-click and
refuses a wrong level. `MachineStore` saves each machine's level in `machines.yml` and loads a missing one as level I, and
`MachineTicker` uses the pace and the title level. The level models are generated by
`io.corefactory.resourcepack.art.LevelArt`. See [MECHANICS.md](MECHANICS.md#lab-machine-levels).

## Barrel concealment

`BarrelConcealment` hides each machine's barrel from the players near it. The server keeps the barrel. Every 40 ticks (`PASS_TICKS`) it decides each machine's view with `ConcealmentRules.decide`, which is pure: it returns the players to conceal, to refresh and to restore. The changes go out through `ClientEffects`, one block-change set per player. `showBlocks` sends air at the host block, and `restoreBlocks` sends the real block back.

- A machine is concealed only when it is eligible: concealment is on, the machine is settled and its chunk is loaded, it has a host, and its host block is still that material.
- A player is in range when they stand in the machine's world and within `concealment.range` of the centre of the host block.
- Each machine gets one invisible `Interaction` entity over its host block, made by `attach` when it is missing. Its id is stored on the machine, and the entity is marked with a persistent key, so an entity left over from an earlier run is removed when its chunk loads (`sweep`).
- `MachineInteractions` routes the clicks on that entity with `InteractionRoute`, which is pure. A main-hand right-click opens the menu through `MachineListener.openMenu`. A main-hand left-click fires a `BlockBreakEvent` for the host block, so the break handler and any protection plugin see it, and the host becomes air when no one cancels it. Off-hand clicks do nothing.
- `MachineStore.onRemoved` tells `BarrelConcealment`, which shows the real barrel again and removes the entity. A chunk unload forgets the views without packets, since the client drops the chunk with them.

## Turrets and two-block devices

The machine gun turret is a package of its own in the Paper module, `turret`. It is an entity device like the energy devices:
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
`DevicePart` in the Paper module maps a clicked block back to the anchor. Every lookup from a block to a device, the break, the
placement and the blast's reach, goes through `DevicePart`, so the rule is in one place.

## Holograms

`HologramService` (Paper module) gives each hologram one `TextDisplay`, spawned as a non-persistent entity. The plugin
sends no packets and checks no players: the game draws the display to each player within its view range.

- Placement. `HologramService.spotAbove(block, height)` is the centre of the device's top, `height + HologramLayout.GAP`
  (0.3 blocks) above the block. The hologram is anchored by its bottom line and grows upward, so a device of any height
  does not reach into its text. The text is drawn at half size (`HologramLayout.SCALE`, 0.5), so each line is
  `HologramLayout.LINE_SPACING` (0.125 blocks) below the one above.
- `HologramService.create(Location, List<String>)` returns an `Optional<Hologram>` with one line per text, top first. It is empty when holograms are off, the point has no world, or the list is empty.
- `Hologram.setLines(List<String>)` changes the text. The line count is fixed at creation: a longer list is cut to it, and a shorter one is padded with empty lines. The display is written only when the text differs.
- `Hologram.setTone(Tone)` sets the background: `WORKING` a faint green, `STUCK` a faint red, `NEUTRAL` none
  (`HologramService.colourOf`). Generators, lab tools, drills, turrets and pumps set a tone; pylons, accumulators, belts
  and charts do not. Unverified in game: the tone colours.
- `HologramService.remove(Hologram)` removes the display and drops the hologram. A display is not saved with its chunk,
  so `onChunkLoad` spawns it again when its chunk loads.
- Visibility. `HologramLayout.viewRange` turns `holograms.view-distance` into the display's view range: the distance over
  64 blocks, from 0.05 to 1.0.
- `MachineTicker` shows a lab tool's lines (`HologramText.forMachine`) while `HologramRules.shouldShow` says so. `DrillTicker` shows a drill's lines (`HologramText.drillLines`) while the drill has power and room. Both add the network line at the bottom (`EnergyText.withGrid`). Otherwise `MachineStore.releaseHologram` removes it.
- `HologramText` is pure formatting. `progressBar(fraction, width)` draws the bar, and `percent` truncates the percentage, so a pass that has not finished never reads 100%.

## Device screens

A generator, a drill or a lab machine (including the refinery) has a screen on its front face, in `io.corefactory.paper.screen`.
The screen is a `BlockDisplay` of `BLACK_CONCRETE`, 0.6 blocks wide, 0.75 tall and 0.04 thick, with a `TextDisplay` of four
lines in front of it. It is a display entity, not a texture. `ScreenGeometry` places the panel: its centre lies 0.54 blocks
from the centre of the anchor block, on the horizontal face toward the player who placed the device. The refinery has no
outlet face. `ScreenText` tints the first line by tone
(green while working, red while stuck, muted otherwise). `DeviceScreen` makes and removes both entities, and
`DeviceRegistry` and `MachineStore` keep them. Unverified in game: the screen's geometry and the text orientation and fit.

## Effects and Visuals

The core asks for a named effect with `Effects.visual(name, at, scale)`, which calls `Visuals.play`. `Visuals` is a
functional interface in `io.corefactory.core.engine`. Until the platform supplies one it is `Visuals.NONE`, which shows
nothing. The Paper plugin passes its `SpriteEffects` with `CoreFactoryCore.setVisuals`.

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

### Client effects

`ClientEffects` (Api) changes what one player sees without touching the world. `BukkitClientEffects` (Paper module)
implements it with the Paper per-player API: `setClientTime` and `resetClientTime` hold and release the player's clock
(`setPlayerTime`, `resetPlayerTime`), `setClientWeather` shows rain (`setPlayerWeather`), and `showBlocks` and
`restoreBlocks` send block changes to one player (`sendBlockChange`). The API has no per-player thunder, so the
thundering flag is not shown. The nuclear mechanic uses them for the beacon beams (block views, sent to the viewer only),
the rain and the clock hold. `BarrelConcealment` uses the block views to hide each machine's barrel. Unverified in game:
the clock hold survives the server's own time updates.

### Sounds

`MachineSounds` (Paper module, package `audio`) plays the pack's sounds at a block, with a throttle per device: each device takes its
turn on a cycle of its block coordinates, so no state is kept. The sound events come from `sounds.json`, which the resource-pack
builder writes from the OGG files in `art/sounds` (`SoundPack`). The sounds are built by `tools/sounds/make_sounds.py`, which downloads and edits the
recordings listed in `tools/sounds/sources.json` (with ffmpeg) and synthesises the rest. It needs an ffmpeg at build time only.

## Storage

The `CoreFactory-Storage` module holds the research store and the deposit store, and nothing else.

- `StorageSettings` is the record of the `storage:` section. Any `storage.type` other than `postgresql` means SQLite.
- `Database.open` chooses the driver. `DriverLoader.ensure` downloads it when it is not on the classpath, checks it
  against its SHA-1, and loads it in its own class loader from `libraries/`. See [CONFIGURATION.md](CONFIGURATION.md#database-drivers).
- The HikariCP pool has one connection for SQLite, with a busy timeout of 5 seconds. PostgreSQL uses `pool-size`.
- `SchemaMigrations.apply` runs at open. It keeps a version in the `corefactory_schema` table, and each migration runs in
  its own transaction. The tables are:
  - `corefactory_research_unlocks` and `corefactory_research_progress` (migration 1), the research store.
  - `corefactory_deposits` (migration 3): `id` (primary key), `world`, `centre_x`, `centre_z`, `radius`, `ore`,
    `initial_amount`, and `remaining` (which must not be negative), with the index `corefactory_deposits_centre` on
    (`world`, `centre_x`, `centre_z`). A take lowers `remaining` in one conditional update.
  - `corefactory_mining_ledger` and `corefactory_chunk_budget` (created by migration 2) are dropped by migration 4, which runs
    `DROP TABLE IF EXISTS` for both when the database opens. No code reads or writes them: `corefactory_deposits` replaced the
    chunk budgets.
- `JdbcResearchStore` implements `unlocked`, `unlock`, `progress` and `setProgress`. It reads and writes through
  `Database.query` and `Database.execute`. `JdbcDepositStore` does the same for the deposits.

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

## Geology and deposits

- `DepositPlanner` (Core, pure) plans the deposits from the world seed, the world name and the cell. The surface is cut
  into square cells of `deposits.cell-size` blocks. A cell holds at most one deposit, with the chance `density`, and a
  deposit's circle lies inside its cell, so deposits never overlap. Every choice is a hash of the seed, the world name
  and the cell, so the planner keeps no state and the same world always has the same deposits.
- `DepositGenerator` runs from `DepositChunkListener`, on `ChunkLoadEvent`. It plans the deposits that touch the chunk,
  inserts each one once (`insertIfAbsent`), and places the host blocks: in each deposit's footprint, every replaceable
  block in the ore's height band becomes the ore's host block, with the chance `ore-block-density`. `OreBlocks` hashes
  the deposit and the position, so the choice never changes. A chunk writes at most 4,096 blocks. The chunk marker
  `corefactory:deposits` is set last, so a chunk is laid once, and a restart adds no ore blocks. A chunk generated before
  the rework has no marker, so it is laid the first time it loads after the upgrade.
- `DepositStore` keeps the deposits. `JdbcDepositStore` writes them to the research database (`corefactory_deposits`).
  `MemoryDepositStore` is used when that database is not open, and then the deposits are lost when the server stops (a
  warning is logged). A take is one conditional update, so `remaining` never goes below zero. Drills read and take
  through the store (see [Lab tools, drills and hoppers](#lab-tools-drills-and-hoppers)).
- The placed-ore-block path still exists for the ore block items a player places: `GeologyListener`, `GeologyWorker`,
  `OreChunks` and `GeologyRegistry`. A placed ore item is registered, and breaking a registered block gives the raw
  mineral (or the ore block item with silk touch). Patch blocks laid by `DepositGenerator` are not registered.
- Hand-mining a patch block. `PatchBlocks` (Core, pure) says whether a block is a patch site of a deposit: the deposit covers
  its column, the ore and host match, the height is in band, and `OreBlocks.shows` holds. `GeologyListener.planPatchBreak`
  cancels the vanilla drop of such a block at the break, and `givePatchOre` (monitor priority, so a later cancel by another
  plugin takes nothing) makes one component item, takes one unit from the deposit and gives the item. An empty deposit gives
  nothing. `PatchExclusions` keeps, per chunk, the entries of host blocks that are never patch blocks: a placed host block, a
  piston or dispenser move, or a host block already in the stone when the chunk was laid. Those are recorded from the
  `BlockPlaceEvent`, `BlockPistonExtendEvent`, `BlockPistonRetractEvent` and `BlockDispenseEvent` handlers.
- The Ore Density Scanner (`scanner/*`) is a listener, `DepositScanner`, that scans on right-click, cycles the filter on
  sneak and right-click, and surveys on sneak and left-click. Its rules are pure Core code in `deposit/*`: `DepositScan`
  (which deposits count, their order and reach), `DepositFilter`, `DepositText` (the action bar and chat lines), `RichnessTier`,
  `Compass` and `ScannerSettings` (the `scanner-radius`, `scan-cooldown-ticks` and `outline-seconds` keys of `deposits`). `DepositMap` is the `/cf deposits` menu, `WaypointService`
  ticks each player's waypoint, and `DepositOutline` draws the rings. Filters, cooldowns and waypoints are kept in memory.

## Why it looks like this

- **One pipeline for every block write.** Staging, protection and the placed-TNT bookkeeping live in one place, so
  mechanics stay short and cannot forget any of them.
- **Mechanics are stateless.** All numbers arrive in `DetonationContext.settings()`, so one class serves every tier
  of the nuclear family, and a server admin can add a TNT type by writing YAML.
- **Effects are modifiers, not subclasses.** Shockwave, debris, echo and silent are flags on a standard blast.
- **Client-only effects stay client-only.** The clock hold, the rain and the beams are per-player calls on the Paper API
  (see [Client effects](#client-effects)). They never touch the world, so restoring a player's view is just re-sending
  the real blocks and weather.
