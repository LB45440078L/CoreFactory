# Development

## Requirements

- JDK 25 (`maven.compiler.release` is 25). The plugin's bytecode needs a Java 25 server.
- Maven 3.9 or newer.
- Network access to the Maven repositories declared in the parent POM: SpigotMC snapshots, EngineHub, and
  CodeMC, and Maven Central, which provides HikariCP. WorldGuard is compiled against its API (EngineHub); GriefPrevention is not a compile dependency, it is reached
  by reflection at runtime.

## Building

```bash
mvn clean package                     # default target: Spigot-API 26.1
mvn clean package -Papi-26.2          # Spigot-API 26.2
mvn clean package -Papi-26.3          # Spigot-API 26.3
mvn clean package -Papi-1.21.11       # Spigot-API 1.21.11
mvn test                              # unit tests only
```

Each profile changes one property, `spigot.api.version`. Everything else is shared, so one source tree builds
against all four. Build each profile before a release. `-Papi-x` is the only switch a target needs.

Build outputs:

| Artifact | Path |
|---|---|
| Plugin jar (shaded, with Api and Core) | `CoreFactory-Spigot/target/CoreFactory-Spigot-1.0.0.jar` |
| Resource pack zip | `CoreFactory-ResourcePack/target/CoreFactory-ResourcePack.zip` |

Builds of one module on its own need the others in the reactor, because they are not published yet:

```bash
mvn -pl CoreFactory-Api,CoreFactory-Core,CoreFactory-Spigot -am package
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
│   ├── drill/                      DrillTable, DrillRules, DrillCatalog, DrillStorage, DrillState, DrillBox, DrillKind, DrillPower, PowerSource, DrillStatus, DrillText, DrillSpec, DrillDefinition
│   ├── geology/                    GeologyService, VeinPlanner, OreDefinition, RetrofitQueue
│   ├── research/                   ResearchService, ResearchTree, ResearchStore, Progression
│   ├── hologram/                   HologramRules, HologramText
│   ├── survey/                     ChunkSurvey
│   ├── radiation/                  RadiationModel, Fallout, RadiationSuit, GeigerCounter
│   ├── energy/                     EnergyTable, Units, EnergyNetwork, EnergySnapshot, EnergyResult, GridGraph, GridRegistry,
│   │                               GridPalette, Grid, GridResult, OutlineTracer, ChartHistory, ChartMode, ChartRenderer,
│   │                               Pylon, PylonType, Accumulator, AccumulatorLevel, Generator, GeneratorKind, GeneratorFuel,
│   │                               Sky, OpenAir, WireRules, WireCheck, Consumer, Endpoint
│   ├── logistics/                  LogisticsTable, LogisticsNetwork, Topology, Lane, BeltUnit, BeltShape, Dir, Picker,
│   │                               Splitter, Port, Placement, PlacementReason, Endpoints, LogisticsResult, Item, LogisticsLevel
│   └── mechanics/                  Mechanics catalogue and every built-in mechanic
├── CoreFactory-Storage/            JDBC research store (src/main/java/io/corefactory/storage)
│   │                               Database (HikariCP pool), DriverLoader, JdbcResearchStore, SchemaMigrations, StorageSettings
├── CoreFactory-Spigot/             plugin (src/main/java/io/corefactory/spigot)
│   ├── hook/                       WorldEditBlockEditor, PacketClientEffects, WorldGuard and GriefPrevention guards
│   ├── listener/                   placement, ignition, protection, interaction
│   ├── machine/                    MachineStore, MachineTicker, DrillTicker, HopperListener, HopperRules, MachineMenu, MachineLayouts, Barrel, BarrelMigration, SolarPanelListener, BarrelConcealment, ConcealmentRules, InteractionRoute, MachineInteractions, LabPort, DrillPort
│   ├── hologram/                   HologramService, HologramPackets, Hologram, HologramJson
│   ├── energy/                     EnergyService, DeviceRegistry, EnergyDevice, GeneratorDevice, AccumulatorDevice, GeneratorMenu,
│   │                               GeneratorText, GeneratorStatus, GeneratorLook, GeneratorSites, WireSelection, EnergyEffects,
│   │                               EnergyText, EnergyGeometry, WorkPace, EnergyFile, EnergyStore, StoredEnergy, DeviceListener,
│   │                               EnergyPlacementListener, ChartService, ChartCommand, ChartRules, ChartText, ChartSamples, GridNames
│   ├── logistics/                  LogisticsService, LogisticsDevice, LogisticsEntities, LogisticsPlacement, LogisticsListener,
│   │                               SidePlacement, PlacementAim, Orientation, BeltPath, SplitterMenu, SplitterFilter, FuelPort,
│   │                               LogisticsPorts, SatisfactionMap, DisplayBudget, LogisticsText, LogisticsIds, LogisticsFile,
│   │                               LogisticsStore
│   ├── survey/                     ScannerListener, SurveyRenderer, SurveyMap, OreColours
│   ├── effects/                    SpriteEffects (the Visuals of the plugin)
│   ├── geology/                    GeologyWorker, GeologyListener, GeologyCommand, OreChunks
│   ├── research/                   ResearchRuntime, ResearchService adapter, ResearchCommand, ResearchConsoleUi
│   ├── config/                     DefaultsMerger (the startup data merge)
│   ├── pack/                       BundledPack, PackInstaller
│   ├── achievement/                achievement catalogue, science feats, progress store, tracker, datapack
│   ├── command/                    /cf
│   ├── guide/                      the tutorial book
│   ├── gui/                        catalogue, recipe view, achievements menu
│   └── src/main/resources/         plugin.yml, config.yml, tnts.yml, items.yml, materials.yml, processes.yml, ores.yml, research.yml
├── CoreFactory-ResourcePack/       texture generator (src/main/java/io/corefactory/resourcepack)
└── docs/
```

The energy and logistics tests are in `CoreFactory-Core/src/test/java/io/corefactory/core/energy` and
`.../core/logistics`, and in `CoreFactory-Spigot/src/test/java/io/corefactory/spigot/energy` and `.../spigot/logistics`.

## Tests

There are 158 test classes (the files named `*Test.java` in all modules). The ones that need a live server are not unit
tests: they need Bukkit's registry, so they are covered by the runtime checks in [COMPATIBILITY.md](COMPATIBILITY.md).

| Module | Test classes | What they cover |
|---|---|---|
| Api | `BlockChangeSetTest`, `MechanicSettingsTest`, `PackVersionTest` | The block-change buffer, typed settings with defaults, and the pack version format. |
| Core | `drill/`: `DrillBoxTest`, `DrillRulesTest`, `DrillStateTest`, `DrillStorageTest`, `DrillTableTest`; `engine/`: `ShapesTest`, `TntManagerCodecTest`, `VisualCatalogTest`; `geology/`: `GeologyRegistryTest`, `GeologyServiceTest`, `OreDefinitionTest`, `RetrofitQueueTest`, `VeinPlannerTest`; `hologram/`: `HologramRulesTest`, `HologramTextTest`; `mechanics/`: `MushroomHeightTest`, `NuclearCutTest`; `process/`: `ProcessCatalogTest`, `ProcessEngineTest`; `radiation/`: `FalloutZoneTest`, `RadiationModelTest`; `research/`: `ResearchServiceTest`, `ResearchTreeTest`; `survey/`: `ChunkSurveyTest` | Drill levels and rules, storage of drill items, the visual catalogue, the ore planner and registry, hologram text and rules, the mushroom height and the nuclear cut, process and recipe rules, radiation, research, and the chunk survey. |
| Core, energy and logistics | `energy/`: `ChartHistoryTest`, `ChartRendererTest`, `EnergyNetworkTest`, `EnergyTableTest`, `GeneratorFuelTest`, `GeneratorTest`, `GridGraphTest`, `GridPaletteTest`, `GridRegistryTest`, `OutlineTracerTest`, `PylonTest`, `SkyTest`, `UnitsTest`, `WireRulesTest`; `logistics/`: `BeltFlowTest`, `BeltTopologyTest`, `ConservationTest`, `DirTest`, `LogisticsTableTest`, `NetworkBuilderTest`, `PickerTest`, `PlacementTest`, `PowerTest`, `RestoreTest`, `SplitterTest` | The power network: grids and their ids and colours, the outlines, the chart history and rendering, the solver, pylons, wires, the sky, the units and generator fuel. The logistics layout, belts, pickers, splitters, placement, power and item conservation. |
| ResourcePack | `ArtTest`, `ResourcePackBuilderTest` | Texture size, uniqueness and determinism, every id has art, and a full pack round trip with the pack format. |
| Spigot | `ContentTest`, `ResourcePackKeysTest`, `AchievementDatapackTest`, `ScienceFeatsTest`, `CoreFactoryCommandTest`, `DefaultsMergerTest`, `CatalogModelTest`, `PageSplitterTest`, `ScienceGuideTest`, `HologramJsonTest`, `BarrelMigrationTest`, `DrillCatalogTest`, `DrillMenuTest`, `HopperRulesTest`, `LabEffectsTest`, `MachineLayoutsTest`, `MachineSlotsTest`, `PackInstallerTest`, `SurveyMapTest` | Content checks on the data files, the achievement datapack and feats, the command, the startup data merge, the catalogue, the book, the hologram JSON, the barrel migration, the lab tool and drill menus and rules, the survey map, and the pack installer. |
| Spigot, energy and logistics | `energy/`: `ChartRulesTest`, `ChartSamplesTest`, `ChartTextTest`, `EnergyFileGridTest`, `EnergyFileTest`, `EnergyGeometryTest`, `EnergySnapshotsTest`, `EnergyTextTest`, `GeneratorDeviceTest`, `GeneratorFileTest`, `GeneratorIdsTest`, `GeneratorLookTest`, `GeneratorSnapshotsTest`, `GeneratorStatusTest`, `GeneratorTextTest`, `GridNamesTest`, `PlacementRulesTest`, `WireSelectionTest`, `WorkPaceTest`; `logistics/`: `BeltPathTest`, `DisplayBudgetTest`, `FuelPortTest`, `LogisticsFileTest`, `LogisticsIdsTest`, `LogisticsTextTest`, `OrientationTest`, `PlacementAimTest`, `SatisfactionMapTest`, `SidePlacementTest`, `SplitterFilterTest` | The energy file, the grid names and charts, the generator rules, text, look and status, the wire tool, the placement rules and the work pace. The logistics file and ids, its text, the display budget, the fuel port, satisfaction, the splitter filter, the side placement and the placement aim. |
| Storage | `DatabaseTest`, `DriverLoaderTest`, `JdbcResearchStoreTest` | The pool and the schema, the driver loader's checks, and the research store. The tests run on SQLite, whose driver is in test scope. |

## Packaging

- The plugin jar is built by `CoreFactory-Spigot`, which shades `CoreFactory-Api`, `CoreFactory-Core`,
  `CoreFactory-Storage` and HikariCP. Spigot-API, WorldEdit, PacketEvents and WorldGuard are provided by the server and
  never shaded.
- HikariCP is relocated to `io.corefactory.libs.hikari`, because other plugins often bundle their own copy.
- The SQLite and PostgreSQL JDBC drivers are not shaded. The storage module downloads the one it needs at first use. See
  [CONFIGURATION.md](CONFIGURATION.md#database-drivers).
- The shade plugin is pinned to **3.6.1**. Version 3.6.0 cannot read Java 25 class files (major version 69) and fails to
  shade the plugin. Do not lower the pin, and raise it only after a build with the new version succeeds.
- The resource pack zip is copied into the jar under `resource-pack/`. On start the plugin copies it to `plugins/CoreFactory/pack/pack.zip`.

## Runtime testing on a server

The repository does not include a server. For a local check:

1. Download a Paper build for your target (`https://fill.papermc.io/v3/projects/paper/versions/<version>/builds/latest`).
2. Put `CoreFactory-Spigot-1.0.0.jar`, `packetevents` and `WorldEdit` in its `plugins/` folder.
3. Start it with `java -jar paper.jar --nogui`, accept the EULA, and check the console for
   `CoreFactory enabled: 42 TNT types` and `Registered N crafting recipes.`
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
