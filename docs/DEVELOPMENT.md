# Development

## Requirements

- JDK 25 (`maven.compiler.release` is 25). The plugin's bytecode needs a Java 25 server.
- Maven 3.9 or newer.
- Network access to the Maven repositories declared in the parent POM: PaperMC (the Paper API), EngineHub (WorldEdit and
  WorldGuard, compiled against their APIs), and Maven Central, which provides HikariCP and the other libraries. The POM
  also declares the CodeMC repository, which no dependency uses now. GriefPrevention is not a
  compile dependency; it is reached by reflection at runtime.

## Building

```bash
mvn clean package                     # default profile api-26.3: Paper API 26.3
mvn test                              # unit tests only
```

Do not run `mvn clean` while a server runs this build. The server holds files under `CoreFactory-Paper/target`, and a clean removes them.

The default profile is `api-26.3`, which is active by default in the root POM and sets `paper.api.version` (26.3) and
`pack.format` (97). The other profiles in the root POM, `api-26.1`, `api-26.2` and `api-1.21.11`, still exist and build
the same sources, but they are not supported. Build the default profile before a release.

Build outputs:

| Artifact | Path |
|---|---|
| Plugin jar (shaded, with Api and Core) | `CoreFactory-Paper/target/CoreFactory-Paper-1.0.0.jar` |
| Resource pack zip | `CoreFactory-ResourcePack/target/CoreFactory-ResourcePack.zip` |

Builds of one module on its own need the others in the reactor, because they are not published yet:

```bash
mvn -pl CoreFactory-Api,CoreFactory-Core,CoreFactory-Paper -am package
```

The ResourcePack module runs its generator in the `package` phase, through `exec-maven-plugin`. Skip it with
`-pl` excluding that module when you only need the plugin.

## Module layout

```
CoreFactory/
├── pom.xml                         parent: versions, profiles, dependency management
├── CoreFactory-Api/                contracts (src/main/java/io/corefactory/api), PackVersion
├── CoreFactory-Core/               implementation (src/main/java/io/corefactory/core)
│   ├── config/                     CoreSettings, Messages, DefinitionParser
│   ├── engine/                     TntManager, Blast, BlockJobs, Effects, Visuals, VisualCatalog, Explosions, Shapes, Tasks, ProtectionImpl
│   ├── item/                       ItemFactory, ComponentDefinition
│   ├── recipe/                     RecipeRegistrar, RecipeSpec
│   ├── placement/                  PlacedTntStore
│   ├── display/                    PlacedDisplays
│   ├── process/                    ProcessCatalog, ProcessEngine, Machine, MachineState, ItemRef, Stack
│   ├── drill/                      DrillTable, DrillRules, DrillCatalog, DrillStorage, DrillState, DrillBox, DrillKind, DrillStatus, DrillText, DrillSpec, DrillDefinition, DrillDeposits, DrillSelection
│   ├── deposit/                    Deposit, DepositPlanner, DepositSettings, DepositStore, MemoryDepositStore, DrillAccumulator, DrillYield, TakeBatch, OreBlocks
│   ├── geology/                    GeologyService, GeologyRegistry, OreDefinition, DepositVolumes, Placement
│   ├── research/                   ResearchService, ResearchTree, ResearchStore, Progression
│   ├── hologram/                   HologramRules, HologramText, HologramLayout, Tone
│   ├── oil/                        Fluid, FluidRules, FluidTank, FluidPhysics, OilNetwork, OilJoinPlan, OilPart, OilSettings, OilModels, Refinery
│   ├── text/                       Palette (the colours), Text (the Adventure and MiniMessage helpers)
│   ├── radiation/                  RadiationModel, Fallout, RadiationSuit, GeigerCounter
│   ├── energy/                     EnergyTable, Units, EnergyNetwork, EnergySnapshot, EnergyResult, GridGraph, GridRegistry,
│   │                               GridPalette, Grid, GridResult, OutlineTracer, ChartHistory, ChartMode, ChartRenderer,
│   │                               Pylon, PylonType, Accumulator, AccumulatorLevel, Generator, GeneratorKind, GeneratorFuel,
│   │                               Sky, OpenAir, WireRules, WireCheck, Consumer, Endpoint
│   ├── logistics/                  LogisticsTable, LogisticsNetwork, Topology, Lane, BeltUnit, BeltShape, Dir, Picker,
│   │                               Splitter, Port, Placement, PlacementReason, Endpoints, LogisticsResult, Item, LogisticsLevel
│   └── mechanics/                  Mechanics catalogue and every built-in mechanic
├── CoreFactory-Storage/            JDBC research store (src/main/java/io/corefactory/storage)
│   │                               Database (HikariCP pool), DriverLoader, JdbcResearchStore, JdbcDepositStore, SchemaMigrations, StorageSettings
├── CoreFactory-Paper/              plugin (src/main/java/io/corefactory/paper)
│   ├── hook/                       WorldEditBlockEditor, BukkitClientEffects, ContainerProtection, WorldGuardGuard, GriefPreventionGuard
│   ├── listener/                   placement, ignition, protection, interaction
│   ├── machine/                    MachineStore, MachineTicker, DrillTicker, DrillMining, DrillRates, HopperListener, HopperRules, MachineMenu, MachineLayouts, Barrel, BarrelMigration, SolarPanelListener, BarrelConcealment, ConcealmentRules, InteractionRoute, MachineInteractions, LabPort, DrillPort
│   ├── hologram/                   HologramService, Hologram (one TextDisplay each)
│   ├── oil/                        OilService, OilDevice, OilEntities, OilListener, OilPlacement, OilStore, MixedNetworks
│   ├── screen/                     DeviceScreen, ScreenGeometry, ScreenText
│   ├── TickScheduler.java, TickPlan.java, CorePermissions.java   the tick scheduler and the permission nodes (top level of the package)
│   ├── energy/                     EnergyService, DeviceRegistry, EnergyDevice, GeneratorDevice, AccumulatorDevice, GeneratorMenu,
│   │                               GeneratorText, GeneratorStatus, GeneratorLook, GeneratorSites, WireSelection, EnergyEffects,
│   │                               EnergyText, EnergyGeometry, WorkPace, EnergyFile, EnergyStore, StoredEnergy, DeviceListener,
│   │                               EnergyPlacementListener, ChartService, ChartCommand, ChartRules, ChartText, ChartSamples, GridNames
│   ├── logistics/                  LogisticsService, LogisticsDevice, LogisticsEntities, LogisticsPlacement, LogisticsListener,
│   │                               SidePlacement, PlacementAim, Orientation, BeltPath, SplitterMenu, SplitterFilter, FuelPort,
│   │                               LogisticsPorts, SatisfactionMap, DisplayBudget, LogisticsText, LogisticsIds, LogisticsFile,
│   │                               LogisticsStore
│   ├── effects/                    SpriteEffects (the Visuals of the plugin)
│   ├── geology/                    DepositGenerator, DepositChunkListener, GeologyCommand, GeologyWorker, GeologyListener, OreChunks
│   ├── ledger/                     DepositLedger (the deposit store: JDBC when the research database is open, else memory)
│   ├── research/                   ResearchRuntime, ResearchService adapter, ResearchCommand, ResearchConsoleUi
│   ├── config/                     DefaultsMerger (the startup data merge)
│   ├── pack/                       BundledPack, PackInstaller
│   ├── achievement/                achievement catalogue, science feats, progress store, tracker, datapack
│   ├── command/                    /cf: CfCommand (the Brigadier tree), CoreFactoryCommand (the handlers), Style, Completion
│   ├── guide/                      the tutorial book
│   ├── gui/                        catalogue, recipe view, achievements menu
│   └── src/main/resources/         paper-plugin.yml, config.yml, tnts.yml, items.yml, materials.yml, processes.yml, ores.yml, research.yml
├── CoreFactory-ResourcePack/       texture generator (src/main/java/io/corefactory/resourcepack)
└── docs/
```

The energy and logistics tests are in `CoreFactory-Core/src/test/java/io/corefactory/core/energy` and
`.../core/logistics`, and in `CoreFactory-Paper/src/test/java/io/corefactory/paper/energy` and `.../paper/logistics`.

## Tests

The test classes are the files named `*Test.java` in each module's `src/test/java`. The ones that need a live server are not
unit tests: they need Bukkit's registry, so they are covered by the runtime checks in [COMPATIBILITY.md](COMPATIBILITY.md).

| Module | Test classes | What they cover |
|---|---|---|
| Api | `BlockChangeSetTest`, `MechanicSettingsTest`, `PackVersionTest` | The block-change buffer, typed settings with defaults, and the pack version format. |
| Core | `deposit/`: `DepositPlannerTest`, `DepositSettingsTest`, `DepositSettingsFromConfigTest`, `DepositTest`, `DrillAccumulatorTest`, `DrillYieldTest`, `OreBlocksTest`, `TakeBatchTest`; `drill/`: `DrillBoxTest`, `DrillDepositsTest`, `DrillRulesTest`, `DrillStateTest`, `DrillStorageTest`, `DrillTableTest`; `engine/`: `ShapesTest`, `TntManagerCodecTest`, `VisualCatalogTest`; `geology/`: `GeologyRegistryTest`, `GeologyServiceTest`, `OreDefinitionTest`; `hologram/`: `HologramRulesTest`, `HologramTextTest`; `mechanics/`: `MushroomHeightTest`, `NuclearCutTest`; `process/`: `ProcessCatalogTest`, `ProcessEngineTest`; `radiation/`: `FalloutZoneTest`, `RadiationModelTest`; `research/`: `ResearchServiceTest`, `ResearchTreeTest` | The deposit planner, the deposit settings and their config reading, a deposit, the drill's accumulator, yield and take batch, the ore block choice, drill levels and rules, the drill's deposit search, storage of drill items, the visual catalogue, the geology registry and ore definitions, hologram text and rules, the mushroom height and the nuclear cut, process and recipe rules, radiation, and research. |
| Core, energy and logistics | `energy/`: `ChartHistoryTest`, `ChartRendererTest`, `EnergyNetworkTest`, `EnergyTableTest`, `GeneratorFuelTest`, `GeneratorTest`, `GridGraphTest`, `GridPaletteTest`, `GridRegistryTest`, `OutlineTracerTest`, `PylonTest`, `SkyTest`, `UnitsTest`, `WireRulesTest`; `logistics/`: `BeltFlowTest`, `BeltTopologyTest`, `ConservationTest`, `DirTest`, `LogisticsTableTest`, `NetworkBuilderTest`, `PickerTest`, `PlacementTest`, `PowerTest`, `RestoreTest`, `SplitterTest` | The power network: grids and their ids and colours, the outlines, the chart history and rendering, the solver, pylons, wires, the sky, the units and generator fuel. The logistics layout, belts, pickers, splitters, placement, power and item conservation. |
| ResourcePack | `ArtTest`, `ResourcePackBuilderTest` | Texture size, uniqueness and determinism, every id has art, and a full pack round trip with the pack format. |
| Paper | `ContentTest`, `ResourcePackKeysTest`, `AchievementDatapackTest`, `ScienceFeatsTest`, `CoreFactoryCommandTest`, `DefaultsMergerTest`, `CatalogModelTest`, `PageSplitterTest`, `ScienceGuideTest`, `BarrelMigrationTest`, `DrillCatalogTest`, `DrillMenuTest`, `DrillRatesTest`, `HopperRulesTest`, `LabEffectsTest`, `MachineLayoutsTest`, `MachineSlotsTest`, `PackInstallerTest`, `DepositConfigTest` | Content checks on the data files, the achievement datapack and feats, the command, the startup data merge, the catalogue, the book, the barrel migration, the lab tool and drill menus and rules, the drill rates, the deposit config, and the pack installer. |
| Paper, energy and logistics | `energy/`: `ChartRulesTest`, `ChartSamplesTest`, `ChartTextTest`, `EnergyFileGridTest`, `EnergyFileTest`, `EnergyGeometryTest`, `EnergySnapshotsTest`, `EnergyTextTest`, `GeneratorDeviceTest`, `GeneratorFileTest`, `GeneratorIdsTest`, `GeneratorLookTest`, `GeneratorSnapshotsTest`, `GeneratorStatusTest`, `GeneratorTextTest`, `GridNamesTest`, `PlacementRulesTest`, `WireSelectionTest`, `WorkPaceTest`; `logistics/`: `BeltPathTest`, `DisplayBudgetTest`, `FuelPortTest`, `LogisticsFileTest`, `LogisticsIdsTest`, `LogisticsTextTest`, `OrientationTest`, `PlacementAimTest`, `SatisfactionMapTest`, `SidePlacementTest`, `SplitterFilterTest` | The energy file, the grid names and charts, the generator rules, text, look and status, the wire tool, the placement rules and the work pace. The logistics file and ids, its text, the display budget, the fuel port, satisfaction, the splitter filter, the side placement and the placement aim. |
| Storage | `DatabaseTest`, `DriverLoaderTest`, `JdbcResearchStoreTest`, `JdbcDepositStoreTest`, `MemoryDepositStoreTest` | The pool and the schema, the driver loader's checks, the research store, and the deposit store, JDBC and in memory. Both deposit store tests run the same checks from `DepositStoreContract`, which is a shared base and not a test class itself. The tests run on SQLite, whose driver is in test scope. |

## Packaging

- The plugin jar is built by `CoreFactory-Paper`, which shades `CoreFactory-Api`, `CoreFactory-Core`,
  `CoreFactory-Storage` and HikariCP. The Paper API, WorldEdit and WorldGuard are provided by the server and
  never shaded.
- HikariCP is relocated to `io.corefactory.libs.hikari`, because other plugins often bundle their own copy.
- The SQLite and PostgreSQL JDBC drivers are not shaded. The storage module downloads the one it needs at first use. See
  [CONFIGURATION.md](CONFIGURATION.md#database-drivers).
- The shade plugin is pinned to **3.6.1**. Version 3.6.0 cannot read Java 25 class files (major version 69) and fails to
  shade the plugin. Do not lower the pin, and raise it only after a build with the new version succeeds.
- The resource pack zip is copied into the jar under `resource-pack/`. On start the plugin copies it to `plugins/CoreFactory/pack/pack.zip`.

## Runtime testing on a server

The repository does not include a server. For a local check:

1. Download a Paper or Purpur 26.3 build (for Paper: `https://fill.papermc.io/v3/projects/paper/versions/<version>/builds/latest`).
2. Put `CoreFactory-Paper-1.0.0.jar` and `WorldEdit` (or `FastAsyncWorldEdit`) in its `plugins/` folder.
3. Start it with `java -jar paper.jar --nogui`, accept the EULA, and check the console for the line
   `CoreFactory enabled: <n> TNT types, <m> placed.`
4. Put test servers in an ignored folder (`server/`, `run/` and `test-server/` are in `.gitignore`).

Keep the server's process ID and stop only that process. Do not kill Java processes by name.

## Conventions

- Java: 4-space indent, 120-column lines, Javadoc on every public type and every Api method. The `.editorconfig`
  enforces the basics.
- Configuration keys: lower-kebab-case. Mechanic ids: lower snake case. TNT ids end with `_tnt` for the shipped
  types; nuclear tiers start with `nuke_`.
- Every new mechanic: add its doc entry in `docs/MECHANICS.md`, its settings in the shipped `tnts.yml`, and at
  least one unit test where the logic is pure.
- Keep `config.yml` comments in sync with the code; they are the server admin's only documentation at the console.

## Commits and releases

- `main` holds releases. Work on a branch and merge with a pull request.
- Commit messages: one imperative subject line, a short body when the reason is not obvious.
- The version is in the parent POM (`1.0.0`) and in `CHANGELOG.md`. Bump both together.
