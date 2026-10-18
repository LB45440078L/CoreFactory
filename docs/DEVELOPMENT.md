# Development

## Requirements

- JDK 25 (`maven.compiler.release` is 25). The plugin's bytecode needs a Java 25 server.
- Maven 3.9 or newer.
- Network access to the Maven repositories declared in the parent POM: SpigotMC snapshots, EngineHub, and
  CodeMC. WorldGuard is compiled against its API (EngineHub); GriefPrevention is not a compile dependency, it is reached
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
| Plugin jar (shaded, with Api and Core) | `MoreTNTPlus-Spigot/target/MoreTNTPlus-Spigot-1.0.0.jar` |
| Resource pack zip | `MoreTNTPlus-ResourcePack/target/MoreTNTPlus-ResourcePack-1.0.0.zip` |

Builds of one module on its own need the others in the reactor, because they are not published yet:

```bash
mvn -pl MoreTNTPlus-Api,MoreTNTPlus-Core,MoreTNTPlus-Spigot -am package
```

The ResourcePack module runs its generator in the `package` phase, through `exec-maven-plugin`. Skip it with
`-pl` excluding that module when you only need the plugin.

## Module layout

```
MoreTNTPlus/
├── pom.xml                         parent: versions, profiles, dependency management
├── MoreTNTPlus-Api/                contracts (src/main/java/io/moretntplus/api)
├── MoreTNTPlus-Core/               implementation (src/main/java/io/moretntplus/core)
│   ├── config/                     CoreSettings, Messages, DefinitionParser
│   ├── engine/                     TntManager, Blast, BlockJobs, Effects, Explosions, Shapes, Tasks, ProtectionImpl
│   ├── item/                       ItemFactory, ComponentDefinition
│   ├── recipe/                     RecipeRegistrar, RecipeSpec
│   ├── placement/                  PlacedTntStore
│   ├── display/                    PlacedDisplays
│   └── mechanics/                  Mechanics catalogue and every built-in mechanic
├── MoreTNTPlus-Spigot/             plugin (src/main/java/io/moretntplus/spigot)
│   ├── hook/                       WorldEditBlockEditor, PacketClientEffects
│   ├── listener/                   placement, ignition, protection, interaction
│   ├── command/                    /mtnt
│   ├── gui/                        catalogue
│   └── src/main/resources/         plugin.yml, config.yml, tnts.yml, items.yml
├── MoreTNTPlus-ResourcePack/       texture generator (src/main/java/io/moretntplus/resourcepack)
└── docs/
```

## Tests

| Module | Tests | What they cover |
|---|---|---|
| Api | `MechanicSettingsTest`, `BlockChangeSetTest` | Typed settings with defaults, nested sections, vectors; the block-change buffer. |
| Core | `ShapesTest`, `TntManagerCodecTest` | Lattice counts for spheres, clamping, filtered scans; the direction codec. |
| ResourcePack | `TextureRendererTest` | Texture size, colour parsing and fallback, determinism. |

Tests that need a live server (block writes, explosions, placement) are not unit tests: they need Bukkit's registry,
so they are covered by the runtime checks in [COMPATIBILITY.md](COMPATIBILITY.md).

## Runtime testing on a server

The repository does not include a server. For a local check:

1. Download a Paper build for your target (`https://fill.papermc.io/v3/projects/paper/versions/<version>/builds/latest`).
2. Put `MoreTNTPlus-Spigot-1.0.0.jar`, `packetevents` and `WorldEdit` in its `plugins/` folder.
3. Start it with `java -jar paper.jar --nogui`, accept the EULA, and check the console for
   `MoreTNT+ enabled: 40 TNT types` and `Registered 58 crafting recipes.`
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
