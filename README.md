# CoreFactory

**Research. Refine. Rule.**

A Paper plugin that adds TNT, power grids, oil, water, logistics, artillery and turrets to Minecraft. Research unlocks machines, automation builds production lines, and the strongest explosives sit at the top of a long tech tree.

[![License: PolyForm Strict 1.0.0](https://img.shields.io/badge/license-PolyForm%20Strict%201.0.0-red)](LICENSE)
[![Paper 26.3](https://img.shields.io/badge/Paper-26.3-blue)](https://papermc.io)
[![Java 25](https://img.shields.io/badge/Java-25-orange)](https://adoptium.net)
[![Maven](https://img.shields.io/badge/build-Maven%203.9%2B-c71a36)](pom.xml)

## Features

- **42 TNT types**, each with its own mechanic, from the standard charge to the nuclear warheads.
- **Over 200 items**, each with a recipe, a research gate and a generated texture.
- **Power and fluids**: pylons, generators, accumulators, oil tubes, water pipes and a refinery.
- **Automation**: belts, pickers, splitters, vertical belts and crafters.
- **Artillery and turrets**: seven artillery levels, a Command Center, and machine gun, laser and railgun turrets.
- **Optional integration**: WorldGuard regions and GriefPrevention claims are respected when installed.

## Requirements

| | |
| --- | --- |
| Server | Paper or Purpur 26.3. Folia is not supported. |
| Java | 25 |
| Required plugin | WorldEdit or FastAsyncWorldEdit |
| Optional plugins | WorldGuard, GriefPrevention, ResourcePackManager |

## Install

1. Build the plugin (see below), or download a release.
2. Copy `CoreFactory-Paper-<version>.jar` into your server's `plugins` folder.
3. Start the server. The resource pack is built and installed automatically.
4. Run `/cf gui` in game to browse the catalogue.

## Build

```bash
mvn -B -q clean package
```

The plugin jar and the resource pack are written to `CoreFactory-Paper/target/`. Run the tests with `mvn -B -q test`.

## Documentation

| Topic | Where |
| --- | --- |
| Player guide | [docs/PLAYER_GUIDE.md](docs/PLAYER_GUIDE.md) |
| Mechanics | [docs/MECHANICS.md](docs/MECHANICS.md) |
| Configuration | [docs/CONFIGURATION.md](docs/CONFIGURATION.md) |
| Commands and permissions | [docs/COMMANDS_AND_PERMISSIONS.md](docs/COMMANDS_AND_PERMISSIONS.md) |
| Crafting | [docs/CRAFTING.md](docs/CRAFTING.md) |
| Architecture | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| Changelog | [CHANGELOG.md](CHANGELOG.md) |

## Contributing

Contributions are by invitation. Open an issue first, and read [CONTRIBUTING.md](CONTRIBUTING.md).

## Licence

Licensed under the [PolyForm Strict License 1.0.0](LICENSE). This is a source-available licence, not an open-source one. See [LICENSING.md](LICENSING.md) for what you may and may not do.
