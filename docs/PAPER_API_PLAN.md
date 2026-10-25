# CoreFactory: Paper API migration plan

## Status

Each phase below is marked done, partly done or not done, with the reason. The phase bodies are the plan as it was written.

- Phase 0 (baseline): done. No code change; the later phases start from this baseline.
- Phase 1 (text helpers): done. Messages are Adventure components with an RGB palette (`Palette.java`), and MiniMessage tags work in config messages.
- Phase 2 (recipe restore): done. Recipes are re-registered on `ServerResourcesReloadedEvent` (`RecipeRestoreListener`), not by polling. Restore after `/reload` is unverified in game.
- Phase 3 (scheduler consolidation): done. One `TickScheduler` (`TickPlan` for the ordering) runs the repeating features. Core's transient per-event tasks (`Tasks`, `BlockJobs`, `MilitaryMechanics`) stay on the Bukkit scheduler, as planned.
- Phase 4 (achievement datapack JSON): done. The datapack is written with Gson from the Paper API classpath (`Json.java`). No visible change for players.
- Phase 5 (permissions and suggestions): done. Permissions are registered in code (`CorePermissions.java`) with their names and defaults unchanged. Tab completion comes from the command tree (Phase 6).
- Phase 6 (Brigadier command tree): done. `/cf` is one tree in `CfCommand.java`, registered through the lifecycle event, with typed arguments and permissions checked by the tree. The tree's behaviour in game is unverified.
- Phase 7 (GUI helper): not done. Optional, not started.
- Phase 8 (dialogs for the PIN keypad and confirmations): not done. The PIN keypad stays a chest menu (`LockKeypad`) until the owner decides, as open question 1 in section 5 asks.
- Phase 9 (data components for items): not done. Optional, not started. Items still use `ItemMeta`.
- Phase 10 (PacketEvents decision): done. PacketEvents is removed from the poms and the plugin. The clock, rain and block views use the Paper per-player API. Thunder is not shown per player, because the API has no per-player thunder.
- Phase 11 (config reading out of Core): done. Core reads plain maps, and the Paper module converts the `ConfigurationSection` to a map. Core still imports Bukkit types for blocks, items and locations; those are not configuration types.
- Folia: not done, out of scope. The plugin supports Paper and Purpur only.

Date: 2026-10-23.

## Basis and conventions

- Paper docs were read from the documentation site. Pages that were read, or only partly read, are marked in section 1. A page that could not be verified is marked as such.
- Short paths used below:
  - `P/` = `CoreFactory-Paper/src/main/java/io/corefactory/paper/`
  - `C/` = `CoreFactory-Core/src/main/java/io/corefactory/core/`
- Counts are literal matches from a code search over the module named. "Files" are distinct files.
- Build profiles: the root `pom.xml` default profile is `api-26.1` (paper-api `26.1.2.build.74-stable`). The profiles `api-26.2`, `api-26.3` and `api-1.21.11` also exist. The brief names 26.3, which is not the default. Every phase below should compile on all four profiles, because the plugin still declares `api-version: '1.21'` in `plugin.yml`.
- Two facts that change the plan:
  - `ChatColor` has zero matches in the whole project. Legacy text is written as `&` codes and parsed by `Text.parse`, not by `ChatColor`.
  - Several "Spigot-era" patterns are already the Paper-recommended ones, for example the custom InventoryHolder GUIs and display entities for holograms. Those are listed in section 4.

## 1. What Paper offers that matters here

One line each. "Read" means the page was fetched and its content checked against the claim.

- Command API, Brigadier trees, executors, arguments, `requires()`, suggestions: https://docs.papermc.io/paper/dev/command-api/basics/introduction/ (read; the introduction only links to sub-pages). Sub-pages read: executors, registration, requirements, argument-suggestions. Sub-pages not read: arguments-and-literals, command-tree, custom-arguments, minecraft arguments.
- Command registration through `LifecycleEvents.COMMANDS` in `onEnable` (main class) or in a `PluginBootstrap` (needs `paper-plugin.yml`): https://docs.papermc.io/paper/dev/command-api/basics/registration (read).
- Custom InventoryHolder for identifying plugin GUIs in events: https://docs.papermc.io/paper/dev/custom-inventory-holder (read).
- Menu Type API (`MenuType.GENERIC_9X6`, builder with `title(Component)`, `build(player)`, `open()`): https://docs.papermc.io/paper/dev/menu-type-api (read; the page does not cover click handling).
- Dialog API, added in 1.21.6, with notice, confirmation, multiAction, dialogList and serverLinks types, text, bool, singleOption and numberRange inputs, `Audience#showDialog`, `Audience#closeDialog`, `PlayerCustomClickEvent`: https://docs.papermc.io/paper/dev/dialogs (read; the page is written for 1.21.8). The page does not list a masked or password input type.
- Component API, with Paper accepting Adventure components natively: https://docs.papermc.io/paper/dev/component-api/introduction/ (read in part; the page names `Component`, MiniMessage and serializers, but not `Player.sendMessage` or `ItemName`).
- Adventure text and platforms: https://docs.papermc.io/adventure/text/ (read; no class names for hover or click events were given) and https://docs.papermc.io/adventure/ (read; index only).
- MiniMessage tags, including `hover:show_text`, `click:run_command`, `click:copy_to_clipboard`, gradients and `<shadow>`: https://docs.papermc.io/adventure/minimessage/format/ (read). The MiniMessage API page (deserialize, placeholders, resolvers) was not read.
- Adventure boss bars: `BossBar.bossBar(name, progress, color, overlay)`, `showBossBar` and `hideBossBar` on an audience: https://docs.papermc.io/adventure/bossbar (read).
- Display entities: `TextDisplay`, `BlockDisplay`, `ItemDisplay`, spawned with `world.spawn(location, Class, consumer)` and set with `text(Component)`: https://docs.papermc.io/paper/dev/display-entities (read; written for 1.20).
- Persistent data container: `PersistentDataContainer` on entities, chunks, worlds, tile states and items, `NamespacedKey`, `PersistentDataType`; `ItemStack#editPersistentDataContainer` (1.21.4+): https://docs.papermc.io/paper/dev/pdc (read).
- Data component API: `ItemStack#getData` and `setData` with `DataComponentTypes`, a prototype-and-patch model, version-specific: https://docs.papermc.io/paper/dev/data-component-api (read). The page does not document an `ItemName` class.
- Recipes: `Server#addRecipe(Recipe)` and `addRecipe(Recipe, boolean)`, which resends recipes to online players; `ShapedRecipe`, `ShapelessRecipe`; result names set with `DataComponentTypes.ITEM_NAME`: https://docs.papermc.io/paper/dev/recipes (read). The page does not mention a newer registry-based recipe API.
- Event listeners: `@EventHandler` with `priority`, `ignoreCancelled`, `Bukkit.getPluginManager().registerEvents`: https://docs.papermc.io/paper/dev/event-listeners/ (read).
- Scheduler: the Bukkit scheduler (`runTaskTimer`, `runTaskLater`, `runTask`), ticks, and a note that Folia users must use Folia's schedulers: https://docs.papermc.io/paper/dev/scheduler (read).
- Folia: `GlobalRegionScheduler`, `RegionScheduler`, `EntityScheduler`, `AsyncScheduler`, and the `folia-supported` flag: https://docs.papermc.io/paper/dev/folia-support (read).
- Plugin configuration: `getConfig()`, `saveResource`, `FileConfiguration`, `ConfigurationSerializable`: https://docs.papermc.io/paper/dev/plugin-configurations (read). The page describes the Bukkit YAML approach only. No typed config API was found.
- Developer index, which lists Registries, Lifecycle API, Plugin messaging, Particles, Entity API, Plugin configuration and others: https://docs.papermc.io/paper/dev/ (read; index only).

Not verified from the docs, so the plan does not depend on them:

- Scoreboards, attributes and player profiles have no page in the developer index or sidebar. The Bukkit APIs in the code (see section 2, row 16) stay as they are.
- The Paper items page does not exist. The data component page is the closest.
- The Adventure `ClickEvent` and `HoverEvent` Java class and method names are not given on the text page. Only the MiniMessage tags were checked.
- Not read: the MiniMessage API and dynamic replacement pages, the Paper plugins page (`paper-plugin.yml`), the datapacks lifecycle page, the registries page, the entity teleport page, the custom events page, and the arguments sub-pages.
- Paper's `ItemMeta` may have Component-based `displayName` and `lore` overloads. They are not on any page read here. Check the Javadoc at https://jd.papermc.io/paper before relying on them.
- Bukkit player-specific calls that might replace PacketEvents packets (time, weather, single-block change) are not on any page read here. Check the Javadoc before relying on them.

## 2. Where each feature applies

Columns: feature; where it applies (files and counts); what it replaces; benefit; risk; size (S, M or L).

| # | Paper feature | Where it applies | Replaces | Benefit | Risk | Size |
|---|---|---|---|---|---|---|
| 1 | Command API (Brigadier tree) | `P/command/CoreFactoryCommand.java`: `onCommand` switches on 20 subcommands (lines 112-132). `onTabComplete` has a second switch of 35 cases (from about line 481). Sub-handlers each parse `args[]` by hand: `P/friends/FriendsCommand.java` (8 `args[` reads), `P/turret/TurretCommand.java` (12), `P/research/ResearchCommand.java` (5), `P/energy/ChartCommand.java` (5), `P/geology/GeologyCommand.java` (3). `plugin.yml` declares one command, `cf`, with alias `corefactory`. | `onCommand` and `onTabComplete` switches, `args[]` index parsing, `toLowerCase` on the subcommand, the separate completion lists. | Typed arguments and one tree that covers execution, completion and syntax errors. Client-side syntax checks. | The `cf` tree is large. Tab completion and error text change for players. `plugin.yml` `commands:` entry must be reconciled with the new registration. Two registration paths are possible (main class or bootstrapper). | L |
| 2 | Suggestions (`suggests`, `SuggestionProvider`) | Same files as row 1. Completion lists in `CoreFactoryCommand.java` (`giveIds`, `recipeIds`, `aim`, `turret.options`, `research`, `grid`). | Hand-written completion switches. | Completion lives next to the argument it serves. | Async suggestion code must not call Paper API (the docs warn about this). | M |
| 3 | `requires()` for permissions | 32 `hasPermission(` calls in 11 files, the largest being `P/command/CoreFactoryCommand.java` (13), `P/energy/ChartCommand.java` (5), `P/lock/TntLocks.java` (6), `P/friends/FriendsCommand.java` (1), `P/turret/TurretCommand.java` (1). | Inline `hasPermission` checks at the top of each handler. | Commands a player cannot run are hidden from them. One place per permission. | Hiding a command is not the same as denying it. Checks in listeners (for example `TntLocks`) must stay. Client sync: the docs say `Player#updateCommands()` may be needed. | S-M |
| 4 | Command registration by lifecycle event | `P/CoreFactoryPlugin.java` (`onEnable`, command setup at lines 335-337 with `setExecutor` and `setTabCompleter`). | `getCommand("cf").setExecutor`, `setTabCompleter`. | Registration is the Paper path. Commands can be available to datapack functions when registered in a bootstrapper. | A bootstrapper needs `paper-plugin.yml`. The current build uses `plugin.yml` with `depend`, `loadbefore` and `softdepend`. Converting the file is a separate decision (section 5). | M |
| 5 | Custom InventoryHolder (already in use) | `P/gui/MenuHolder.java`, `P/gui/SlotMenu.java`, `P/gui/GuiListener.java` (cancels every click, lines 15-34). 12 `Bukkit.createInventory(` calls, one in each of 12 files: `P/lock/LockKeypad.java`, `P/water/WaterPumpMenu.java`, `P/machine/DrillResourcesMenu.java`, `P/gui/RecipeGui.java`, `P/logistics/SplitterMenu.java`, `P/gui/AchievementGui.java`, `P/gui/CatalogGui.java`, `P/machine/MachineMenu.java`, `P/scanner/DepositMap.java`, `P/vertical/VerticalMenu.java`, `P/turret/TurretMenu.java`, `P/energy/GeneratorMenu.java`. | Nothing that needs replacing. The holder pattern is the one the Paper page recommends. | Titles are already `Component` (`LockKeypad` passes `Text.parse(title)`). | Little benefit. Changing the GUI framework touches 12 screens and the click logic. | S (only if a shared helper is wanted) |
| 6 | Menu Type API | No target found. There is no crafting, anvil or merchant replica. All 12 menus are 9-row or 6-row chests. | Nothing. | Only useful for vanilla-shaped menus. | Behaviour of block-backed menus needs a real block location. | S (not recommended now) |
| 7 | Dialog API (1.21.6+) | `P/lock/LockKeypad.java` (27-slot PIN keypad with `SET_PIN` and `UNLOCK` purposes). Name entry in `P/friends/FriendsCommand.java` is a typed command argument (`/cf friends add <name>`). Confirmations for destructive actions, such as removing a PIN lock (`corefactory.lock.admin`), are in `P/lock/TntLocks.java`. | The keypad chest for PIN entry. Typed arguments where a form would do. Yes/no confirmations. | Keyboard entry and built-in confirmation. Less code than a chest keypad with 27 slots. | A PIN typed in a text input may be visible. The docs list no masked input type. Dialogs need 1.21.6 or newer, and `plugin.yml` says `api-version: '1.21'`. Clients may not close a dialog on their own. | M (keypad: L) |
| 8 | Component API and MiniMessage | `C/text/Text.java` already parses MiniMessage and `&` codes (`Text.parse`, `Text.escape`, `Text.toMiniMessage`). In `P/`: `Text.parse(` or `Text.escape(` appears 142 times. Heavy users include `P/energy/GeneratorText.java`, `P/turret/TurretMenu.java` and `P/energy/ChartService.java`. | String concatenation with escaped values. `&` codes written by hand in code. | Placeholders as resolvers rather than concatenation. Less escaping. | Server admins have `&` codes in `config.yml` and message files. `Text.parse` must keep accepting them. | M |
| 9 | Adventure click and hover events | Only `C/text/Text.java` (MiniMessage parsing). No Paper runtime class builds a click or hover event. The 25 `ClickEvent`-like matches in `P/` are `InventoryClickEvent`, not Adventure. | Nothing. | Chat actions such as copy-to-clipboard or run-command in the guide and friends list. Item hover in the catalogue. | `click:run_command` cannot run commands with signed arguments (the MiniMessage page warns about this). | S (optional) |
| 10 | Adventure BossBar | No use found in `P/` or `C/` (zero `BossBar` matches). | Nothing. | Only if a boss bar is wanted for a status such as a turret or a reactor countdown. | None for the existing code. | S (not planned) |
| 11 | Scoreboard API (not a Paper page) | `P/turret/TurretService.java` lines 689-697: `owner.getScoreboard()` and `Bukkit.getScoreboardManager().getMainScoreboard()`, used to read team membership for friendly fire. | Nothing. The Paper docs do not cover scoreboards. | None needed. | Team detection depends on the scoreboard being the one the player is on. Keep as is. | - |
| 12 | Attributes (not a Paper page) | `P/turret/TurretService.java` lines 939-951: `living.getAttribute(Attribute.ARMOR)`, `ARMOR_TOUGHNESS`, `MAX_HEALTH`. | Nothing. | None needed. | None. Keep. | - |
| 13 | Display entities (already in use) | `P/hologram/HologramService.java` (5), `P/effects/SpriteEffects.java` (7), `P/oil/OilEntities.java` (4), `P/water/WaterEntities.java` (5), `P/logistics/LogisticsEntities.java` (7), `P/vertical/VerticalEntities.java` (8), `P/turret/TurretRegistry.java` (3), `P/energy/DeviceRegistry.java` (3). 50 matches in 11 files. `ArmorStand` has zero matches. | Nothing. | Already modern. Only text values may still be strings, to check. | Display entities need a `remove()` on temporary displays (the page warns they never unload in spawn chunks). | S (check text values only) |
| 14 | Persistent data container (already in use) | 112 matches in 18 files: `P/geology/OreChunks.java` (11), `P/water/WaterEntities.java` (11), `P/logistics/LogisticsEntities.java` (11), `P/geology/DepositGenerator.java` (7), `P/achievement/AchievementDatapack.java` (6), `P/energy/ChartService.java` (5), `P/turret/TurretIds.java` (6), `P/energy/DeviceRegistry.java` (6). | Nothing. | Already modern. | Keys must be reused, not built each time (the docs say so). | S (tidy only) |
| 15 | Item building: `ItemMeta` or data components | `getItemMeta(`/`editMeta(` 52 matches in 27 files in `P/`. `ItemMeta` names and lore in `C/item/ItemFactory.java`, `C/item/PlaceableItems.java`, `C/recipe/RecipeRegistrar.java`, and 8 other Core files. | Plain `ItemMeta` calls. | Data components expose prototype values and complex parts (custom model data, lore builders). | Data components are version-specific. The docs say `ItemMeta` is more stable. The 1.21.11 profile may not have the same API. | M (optional) |
| 16 | Recipes: `addRecipe(Recipe, boolean)` | `C/recipe/RecipeRegistrar.java`: `Bukkit.addRecipe` (line 112), `Bukkit.removeRecipe` (line 48), `ShapedRecipe` (line 127), `ShapelessRecipe` (line 146), `restoreIfMissing()` (line 64). `P/CoreFactoryPlugin.java` line 343 polls `restoreIfMissing` every 40 ticks. | Polling every 40 ticks to restore missing recipes. | Fewer polls. Explicit resend for online players. | The poll may exist because recipes vanish on some events. The cause must be found first. | S-M |
| 17 | Event listeners (already in use) | `@EventHandler` 97 times in 27 files, for example `P/machine/MachineListener.java` (8), `P/geology/GeologyListener.java` (9), `P/listener/IgnitionListener.java` (6), `P/turret/TurretListener.java` (6). 27 `registerEvents` calls in `P/`. | Nothing. | Already the Paper pattern. | Priority and `ignoreCancelled` choices should be audited, not rewritten. | S (audit) |
| 18 | Scheduler: Bukkit scheduler (keep) | 19 scheduler call sites in 9 files in `P/`: `P/CoreFactoryPlugin.java` lines 340-352 (9 tasks: core tick, achievement save every 6000 ticks, recipe watch, Geiger display, resource refresh, energy, water, oil, machine), `P/achievement/AchievementTracker.java:217`, `P/effects/SpriteEffects.java:62,92`, `P/machine/BarrelConcealment.java:92,153`, `P/energy/ChartService.java:100`, `P/energy/EnergyEffects.java:141`, `P/scanner/DepositOutline.java:60`, `P/scanner/WaypointService.java:51`, `P/turret/TurretRuntime.java:94`. In `C/`: `C/engine/Tasks.java` (a `BukkitRunnable` helper, `runTaskLater` at line 43), `C/engine/BlockJobs.java:87`, `C/mechanics/MilitaryMechanics.java:134-154`. | Nothing on a non-Folia server. | One ticker with a fixed order would replace nine separate tasks. | Folia would need the region schedulers instead. | M (consolidation) / L (Folia) |
| 19 | PacketEvents (hard dependency) | Only `P/hook/PacketClientEffects.java`. It listens for `PacketType.Play.Server.TIME_UPDATE`, sends `WrapperPlayServerChangeGameState` (rain and thunder), `WrapperPlayServerBlockChange` and `WrapperPlayServerTimeUpdate`. The API interface is `CoreFactory-Api/src/main/java/io/corefactory/api/ClientEffects.java`. `plugin.yml` has `depend: [packetevents]`. The root `pom.xml` pins `packetevents.version` 2.14.0. | Four packet types, built by hand. | Removes a hard dependency and a packet layer that breaks when packet classes change. | Bukkit calls that might replace these packets are not verified (section 1). Per-player visual differences. Needs in-game checks. | M-L (decision) |
| 20 | Plugin configuration API (Bukkit YAML) | `ConfigurationSection`, `YamlConfiguration` or `FileConfiguration`: 129 matches in 16 files in `P/`, for example `P/logistics/LogisticsFile.java` (17), `P/energy/EnergyFile.java` (16), `P/CoreFactoryPlugin.java` (13), `P/config/DefaultsMerger.java` (12), `P/turret/TurretStore.java` (12), `P/vertical/VerticalStore.java` (12). In `C/`: 17 files import `org.bukkit.configuration` and 16 use `ConfigurationSection`, for example `C/config/CoreSettings.java` (12), `C/config/DefinitionParser.java` (14), `C/turret/TurretSettings.java` (8). | Direct reads of `ConfigurationSection` in Core. | Core can be tested without Bukkit classes. The pom already has SnakeYAML, so Core tests can parse YAML into maps. | Large rewrite of parsing code. Core tests (133 files) may already depend on Bukkit types. Not checked. | L |
| 21 | Achievement datapack JSON | `P/achievement/AchievementDatapack.java`: `mcmeta()` (lines 77-79) and the advancement JSON are built with string concatenation. | Hand-built JSON strings. | Less escaping risk. Advancement text can use Adventure's JSON serializer. | The datapacks lifecycle page was not read. Output must still match what the server expects. | S-M |

## 3. Phased migration order (lowest risk first)

Verification for every phase, without a live server:

- Compile on all four profiles: `api-26.1` (default), `api-26.2`, `api-26.3` and `api-1.21.11`. Use the `-P` profile switch of the root build.
- Run the Core tests (`CoreFactory-Core`, 133 test files) and the Paper tests (`CoreFactory-Paper`, 106 test files) after each phase.
- In-game checks need the user. They are listed per phase as "user check". No phase may be called done on compile alone.

### Phase 0: Baseline

- Files: none changed. Record the current test results and the current compile result for each profile.
- Verify: the compile and test results are written down before any change.
- User check: none.

### Phase 1: Text helpers (row 8)

- Files: `C/text/Text.java` (add tests for `parse`, `escape` and `toMiniMessage` covering `&` codes, `&&`, hex `&#`, and escaped values). Then the most-used call sites in `P/` (`P/energy/GeneratorText.java`, `P/turret/TurretText.java`, `P/energy/ChartText.java`, `P/logistics/LogisticsText.java`, `P/energy/EnergyText.java`).
- Change: replace concatenated text with MiniMessage text and resolvers. Keep `Text.parse` accepting `&` codes, so config files are unchanged.
- Verify: Core unit tests for the text helpers, and compile on all profiles.
- User check: colours and placeholders in chat and GUI titles on a real server.

### Phase 2: Recipe restore (row 16)

- Files: `C/recipe/RecipeRegistrar.java`, `P/CoreFactoryPlugin.java` (line 343).
- Change: find out why `restoreIfMissing` is needed (reload, client resend, or other plugins). Then decide between the `addRecipe(Recipe, boolean)` overload and the existing poll.
- Verify: Core tests for the recipe definitions (shape and ingredient mapping). Compile.
- User check: crafting recipes after a reload and after a restart. The poll may be the only thing that fixes some case, so this check matters.

### Phase 3: Scheduler consolidation (row 18, keep Bukkit)

- Files: `P/CoreFactoryPlugin.java` (lines 340-352), `P/achievement/AchievementTracker.java`, `P/effects/SpriteEffects.java`, `P/machine/BarrelConcealment.java`, `P/energy/ChartService.java`, `P/energy/EnergyEffects.java`, `P/scanner/DepositOutline.java`, `P/scanner/WaypointService.java`, `P/turret/TurretRuntime.java`, and in Core `C/engine/Tasks.java`, `C/engine/BlockJobs.java`, `C/mechanics/MilitaryMechanics.java`.
- Change: one ticker that runs the registered tick actions in a fixed order. Keep the existing periods (1, 6000, 40 ticks).
- Verify: Core tests for the ticker ordering (pure Java, no Bukkit). Compile.
- User check: TNT fuse timing, drills, water and oil pumps, turret firing, chart refresh.

### Phase 4: Achievement datapack JSON (row 21)

- Files: `P/achievement/AchievementDatapack.java`.
- Change: replace string concatenation with a small JSON writer or a JSON library that is already on the server classpath. Check the classpath first; the datapacks page was not read.
- Verify: Paper unit tests that parse the written `pack.mcmeta` and advancement files and compare them with expected values. Compile.
- User check: the advancements tab after a restart (the pack is read at startup).

### Phase 5: Command permissions and suggestions (rows 2 and 3)

- Files: `P/command/CoreFactoryCommand.java` (onTabComplete), `P/friends/FriendsCommand.java`, `P/turret/TurretCommand.java`, `P/research/ResearchCommand.java`, `P/energy/ChartCommand.java`, `P/geology/GeologyCommand.java`. These are changed while the existing executor stays in place. Suggestions are added as `suggests` callbacks behind the same completion lists.
- Verify: Core tests for the completion candidate lists (`Completion.Candidate`, permission strings). Compile.
- User check: tab completion output and what each permission group sees.

### Phase 6: Commands on the Brigadier tree (row 1, split by subcommand)

- Files: `P/CoreFactoryPlugin.java` (registration), `P/command/CoreFactoryCommand.java`, the five sub-handlers, `plugin.yml`.
- Change: register one `cf` literal tree through the lifecycle event. Move subcommands in this order, smallest first: `count`, `info`, `list`, `pack`, `book`, `gui`, then `geology`, `research`, `grid`, `pylons`, then `friends` and `turret`, then `give`, `recipe`, `aim`, `target`, `unlink`, `reload`. Keep the `plugin.yml` entry until the last subcommand moves, then decide (section 5).
- Verify: compile; Core tests for argument helpers (for example `C/drill/DrillSelection.java`, if it is used by a command). Paper tests for the command classes where they do not need a live server.
- User check: each subcommand, its error text, and tab completion, on a real server. This phase has the most user checks.

### Phase 7: GUI helper (row 5, optional)

- Files: the 12 `createInventory` call sites listed in row 5.
- Change: a shared helper that builds a holder and a `Component` title. Do not change the click logic.
- Verify: compile. Paper tests where the holder can be built without a server.
- User check: each menu opens and its clicks still work.

### Phase 8: Dialogs for the PIN keypad and confirmations (row 7, needs a decision)

- Files: `P/lock/LockKeypad.java`, `P/lock/TntLocks.java`, possibly `P/friends/FriendsCommand.java`.
- Change: only after the open questions in section 5 are answered. Gate dialog use on the server version, because dialogs need 1.21.6 or newer.
- Verify: compile on all profiles. Core tests for `C/lock/PinEntry.java` and `C/lock/PinAttempts.java` (already pure logic) must still pass.
- User check: the whole PIN flow on a real server, including wrong PINs, lockout, removal and the admin remove.

### Phase 9: Data components for items (row 15, optional)

- Files: `C/item/ItemFactory.java`, `C/item/PlaceableItems.java`, `C/recipe/RecipeRegistrar.java`.
- Change: only for item parts that `ItemMeta` cannot express. Keep `ItemMeta` for names and lore.
- Verify: compile on 26.x profiles. The 1.21.11 profile must still compile or be excluded for these files.
- User check: item names, lore and custom model data in the inventory and in the resource pack.

### Phase 10: PacketEvents decision (row 19, needs a decision)

- Files: `P/hook/PacketClientEffects.java`, `CoreFactory-Api/src/main/java/io/corefactory/api/ClientEffects.java`, `plugin.yml` (`depend`), root `pom.xml` (`packetevents.version`), `CoreFactory-Paper/pom.xml` (dependency).
- Change: only after Javadoc confirms Bukkit equivalents for time, rain and single-block changes. Otherwise keep PacketEvents.
- Verify: compile. Core tests are not affected.
- User check: day and night, rain and thunder, and block changes near a player, on a real server.

### Phase 11: Config reading out of Core (row 20, large)

- Files: `C/config/CoreSettings.java`, `C/config/DefinitionParser.java`, and the 14 other Core files that import `org.bukkit.configuration`.
- Change: Core reads a plain map. The Paper module converts a `ConfigurationSection` to that map.
- Verify: Core tests that parse the bundled YAML files (`CoreFactory-Paper/src/main/resources/*.yml`) with SnakeYAML into maps and compare them with the current parse results. This is the most important check in this phase.
- User check: a server start with the default config, and a start with a changed config.

Not in any phase: Folia. It is a separate project. Section 4 explains why.

## 4. Items that are not worth changing

- Event listeners (row 17). `@EventHandler` in 27 files is the Paper pattern. Only the priority and `ignoreCancelled` choices need an audit.
- Custom InventoryHolder GUIs (row 5). This is the pattern the Paper page recommends, and titles are already components. The only change worth making is a shared helper (Phase 7).
- Menu Type API (row 6). No menu in the codebase is shaped like a crafting table or an anvil. Chest menus do not need it.
- Holograms as display entities (row 13). Already `TextDisplay`, with no `ArmorStand` left. Only the text values may still be strings.
- Persistent data container (row 14). Already in use in 18 files, with the right API.
- Bukkit scheduler (row 18), on a non-Folia server. The Paper scheduler page says the Bukkit scheduler is for non-Folia servers and that Folia needs its own schedulers. Changing to region schedulers adds risk and gives no benefit unless Folia support is a goal.
- `ItemMeta` for item names and lore (row 15). It is stable across the four profiles. Data components are version-specific and the 1.21.11 profile may not have the same API.
- Scoreboards and attributes (rows 11 and 12). No Paper page covers them, and the code uses them only for team checks and armour and health values.
- Boss bars (row 10). No current use. Adding one is a feature decision, not a migration.
- Adventure click and hover events in chat (row 9). Optional UX work, not part of the migration. The MiniMessage `run_command` click cannot send signed arguments, so it is limited.
- Plugin configuration YAML (row 20) outside Core. The Paper page describes this API as the standard one, and no typed config API was found in the docs. Keep it in the Paper module.

## 5. Open questions for the user

1. Dialogs for the PIN keypad (row 7). Can a PIN be typed into a dialog text field, given that the docs list no masked input type? Is the keypad's chest UI worth keeping for that reason? Dialogs need 1.21.6 or newer, so the minimum version in `plugin.yml` (`api-version: '1.21'`) and the 1.21.x profiles would need to change, or dialogs would be gated.
2. Friends input (row 7). Keep `/cf friends add <name>` as a typed argument, or move it to a dialog with a text input? The typed argument already works and is simpler.
3. Brigadier for `/cf` (rows 1 to 3, Phase 6). Should the whole `cf` tree move to the Paper command API? It is the largest change in the plan (about 35 completion cases and 20 subcommands). Confirm that this is wanted now.
4. PacketEvents (row 19, Phase 10). Keep PacketEvents for beams, time and weather, or remove the hard dependency if Bukkit calls can do the same? The docs checked here do not settle it, so the Javadoc must be checked first.
5. Supported versions. Which profiles are release targets: `api-26.1` (the default), `api-26.2`, `api-26.3` or `api-1.21.11`? Dialogs, data components and the Menu Type API depend on this.
6. `plugin.yml` or `paper-plugin.yml`. The registration page says a bootstrapper needs `paper-plugin.yml`, and the main-class route works without it. Which should the plugin use? Moving to `paper-plugin.yml` also changes how `depend`, `loadbefore` and `softdepend` are written. The Paper plugins page was not read.
7. Folia. Is Folia support a goal? If not, the Bukkit scheduler stays (row 18).
8. Core and Bukkit. Is it a goal that Core tests run without Bukkit classes? If yes, Phase 11 is worth its size. If no, it can be dropped.
9. Recipe restore (Phase 2). Do you know which event makes recipes disappear? The answer decides whether the 40-tick poll can go.
10. Paper docs gaps. Should the arguments, datapacks, paper-plugins, registries and MiniMessage API pages be read before the phases that depend on them (Phases 4, 6 and 11)? They were not read for this plan.
