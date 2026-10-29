# Commands and permissions

## `/cf`

Alias: `/corefactory`. Running it with no arguments prints the help. Tab completion covers the subcommands, the ids, the
directions, `update`, and the research subcommands. `/cf give` offers every TNT, component and ore id, and `/cf recipe`
offers the ids that have a crafting recipe or that a machine makes. An id that is not found lists up to five close ids, and says to press Tab after the command to see every id. A subcommand you lack the permission for is left out of the command tree: you cannot run it, and tab completion does not offer it. The help lists every subcommand, whatever your permissions. Words are case-sensitive, and a number that is not a number is refused. unverified in game: how tab completion looks to a player without a permission.

| Command | Permission | What it does |
|---|---|---|
| `/cf help` | none | Lists the subcommands. |
| `/cf give <id> [player] [amount]` | `corefactory.give` | Gives a TNT type, a component or an ore block. `id` may be an ore id, such as `uraninite_ore`, which gives its ore block item. `player` defaults to you. `amount` is 1 to 64, default 1. A larger number gives 64. Text that is not a number is refused. |
| `/cf list` | none | Prints every TNT id, every component id and every ore id. |
| `/cf gui` | `corefactory.guide` | Opens the catalogue: a paged browser of everything the plugin adds, in nineteen categories, each its own tab. Left-click an entry for one copy (needs `corefactory.give`, which operators have, and the research for the item). Without that permission a left click says only administrators can take items, and the entry's lore says so. Right-click an entry for its recipe. A click that does something plays a short sound, which only you hear. |
| `/cf recipe <id>` | `corefactory.recipes` | Shows the recipe of a TNT type or a component in a crafting grid. An item a machine makes has no crafting recipe, so its view shows the process and the machine that makes it. Click an ingredient that has a recipe of its own to open that recipe; BACK returns. |
| `/cf book [player]` | `corefactory.guide` | Gives the tutorial book: a short guide to crafting, placing, the <img src="images/components/fuse_linker.png" width="24" height="24" align="absmiddle"> fuse linker, components, nuclear weapons, achievements and commands, then the science chapters (ores, the uranium chain, breeding, hydrogen and particles, exotic matter, research, warheads and machines), with the recipes and costs read from the game's files. |
| `/cf achievements [player]` | `corefactory.achievements` | Opens your achievements, in pages of 45. Unlocked ones glow and show the date; locked ones show progress. With a player name it shows theirs, which needs `corefactory.give`. |
| `/cf info` | none | Inspects the placed TNT block you are looking at (within 6 blocks): mechanic, radius, fuse, direction, target, number of fuse links. |
| `/cf aim [north\|east\|south\|west\|up\|down]` | `corefactory.configure` | Sets the direction of the placed TNT you look at. Without an argument it uses your facing. Shaped charges use this direction; napalm uses it only when dropped. |
| `/cf target [x y z]` | `corefactory.configure` | Sets the teleport destination of the placed TNT you look at. Without coordinates it is the block you stand on; with `x y z` it is that block in your world. Give all three coordinates or none: one or two are refused. Used by <img src="images/tnt/teleport_tnt.png" width="24" height="24" align="absmiddle"> teleport TNT. |
| `/cf unlink` | `corefactory.configure` | Removes every fuse link of the placed TNT you look at. |
| `/cf holograms [on\|off]` | `corefactory.configure` | With no argument, says whether the holograms of devices are shown. `on` or `off` shows or hides them for everyone. Device holograms start hidden after each restart; a pylon's hologram is always shown. The setting is kept in memory only. |
| `/cf count` | none | Number of placed custom TNT blocks. |
| `/cf reload` | `corefactory.reload` | Re-reads `config.yml`, `tnts.yml`, `items.yml` and `materials.yml`, and re-registers recipes. Placed TNT stays as it is. `processes.yml`, `ores.yml`, `research.yml`, the `deposits` section and the drill section need a restart. See [CONFIGURATION.md](CONFIGURATION.md). |
| `/cf pack [update]` | `corefactory.reload` | Shows the bundled and installed resource pack versions and what the next start will do. `update` copies the bundled pack now, only if it is newer. See [RESOURCE_PACK.md](RESOURCE_PACK.md#versions-and-updates). |
| `/cf geology deposits [radius]` | `corefactory.geology` | Lists the deposits whose circle comes within `radius` blocks of you, in your world, nearest first, with no limit. Each line gives the ore, its direction and distance, its richness and what is left. `radius` is a whole number from 1 to 512, and the default is 64. Players only. |
| `/cf deposits` | `corefactory.geology` | Opens the deposit map: the nearest deposits of the scanner's filter, up to 54 in three pages of 18. A click on a deposit sets your waypoint. The filter and clear-waypoint buttons are in the menu. Players only. The permission is for operators by default; grant it to players to open the map for them. The scanner itself needs no permission. |
| `/cf research max [player]` | `corefactory.research.admin` | Finishes every research node for a player. Without a name it is you. The player may be offline if they have joined before. |
| `/cf research grant <node> [player]` | `corefactory.research.admin` | Finishes one node and every node it requires, for a player. The node id is checked; `/cf research nodes` lists them. |
| `/cf research nodes` | `corefactory.research.admin` | Lists the node ids in research order. |
| `/cf pylons show [seconds]` | `corefactory.pylons` | Draws the outline of each grid that has a pylon within 96 blocks of you (measured across, ignoring height), in the grid's colour, for `seconds`. The outline is the boundary of the grid's pylon squares. The default is 15 seconds, the value is clamped to 1 to 120, and text that is not a number is refused. Players only. See [MECHANICS.md](MECHANICS.md#grids-and-outlines). |
| `/cf grid create <name>` | `corefactory.grid` | Makes a chart of the grid you stand in, one block above your head. You must stand inside the square of a pylon of that grid. The name is 1 to 24 letters, digits or underscores. One chart per grid and five on the server by default (`charts.charts-per-grid`, `charts.charts-total`). Players only. See [MECHANICS.md](MECHANICS.md#grid-charts). |
| `/cf grid remove <name>` | `corefactory.grid` | Removes a chart and its hologram. |
| `/cf grid list` | `corefactory.pylons` | Lists the charts: name, grid, mode and how many players are within 48 blocks of each. |
| `/cf grid mode <name> <satisfaction\|production\|storage>` | `corefactory.grid` | Sets a chart's mode: satisfaction (0 to 100 %), production (kW, supply and load) or storage (% of the grid's capacity). |
| `/cf grid info` | `corefactory.pylons` | Shows the grid of the block you stand on: its id and colour, voltage, current, supply, capacity, satisfaction, and stored energy. Players only. |
| `/cf grid` | none | With no subcommand, prints the usage lines of `/cf grid`. |
| `/cf friends add <player>` | `corefactory.friends` | Adds a player to your friend list, kept in `friends.yml`. The player must have joined this server before. A friend may use the devices you placed, as you can: the turrets' menus, loading, ejecting, power, upgrades and breaking; the artillery pieces and command centers; the water pump kits; the vertical belts' direction; and the lab modules. Your turrets' targeting switches stay yours. A turret of yours never fires at a friend, unless you set that player to `target` with `/cf turret player`. You cannot add yourself. |
| `/cf friends remove <player>` | `corefactory.friends` | Removes a player from your friend list. The player may no longer use your devices. |
| `/cf friends list` | `corefactory.friends` | Lists your friends. With no argument, `/cf friends` does the same. |
| `/cf turret info` | `corefactory.turret` | Shows the level, range, damage per hit, cannons, fire interval and power of the turret you look at, within 6 blocks, or else of your nearest turret within 8 blocks. Also its friendly fire, team mode, mob settings and player stances. Owner only. Players only. |
| `/cf turret target <mob\|category> <on\|off>` | `corefactory.turret` | Sets one mob or one category for the turret: `on` fires at it, `off` spares it. A category is `monsters`, `phantoms`, `slimes` or `ghasts`; a mob is named as in the menu, such as `ZOMBIE`. The override beats the config, and a mob beats its category. Owner only. Players only. |
| `/cf turret player <name> <target\|spare\|clear>` | `corefactory.turret` | Sets a stance for one player: `target` makes the player a target even with friendly fire off, `spare` never targets the player, and `clear` puts the player back under the friend rules. The owner is never a target, so `target` and `spare` on yourself are refused. The player must have joined this server. Owner only. Players only. |
| `/cf turret team <spare\|target\|ignore>` | `corefactory.turret` | Sets how the owner's scoreboard team is treated: `spare` spares teammates as friends (the default), `target` treats them as strangers, targeted only while friendly fire is on, and `ignore` leaves the team out of the rules. The menu's team switch cycles the same three modes. Owner only. Players only. |
| `/cf turret friendlyfire <on\|off>` | `corefactory.turret` | Sets the friendly-fire switch of the turret you look at, within 6 blocks, or else of your nearest turret within 8 blocks. On: the turret fires at players who are not your friends. Off (the default): it fires at no player. Only the owner may change it. Players only. |

Research progress is the same whether a player finishes it at a Research Console or an admin grants it. The
console and the packs are described in [PLAYER_GUIDE.md](PLAYER_GUIDE.md#research).

## Colours

The help and the replies use one palette, in RGB, so a line reads the same wherever it appears. The palette is defined once, in `Palette.java`. Each colour is also a MiniMessage tag: `<accent>`, `<command>`, `<arg>`, `<value>`, `<muted>`, `<hint>`, `<good>`, `<bad>`, `<warn>` and `<research>`. The legacy `&` codes in config messages map onto the same palette.

| Role | Colour | Used for |
|---|---|---|
| Title | amber `#FFB02E`, bold | The heading of a help block |
| Command | light cyan `#7FD8FF` | A command or subcommand, as it is typed |
| Argument | soft yellow `#F7E26B` | A placeholder such as `<id>` or `[player]` |
| Description | cool gray `#A7B0BA` | What a command does |
| Hint | dim gray `#6B7480` | Separators, notes and other text that comes second |
| Success | green `#5BE37D` | A confirmation that something worked |
| Error | red `#FF5F6D` | A refusal, or a usage line for a command given wrongly |
| Warning | gold `#FFC857` | A caution that does not stop the command |
| ID | white `#FFFFFF` | An id the plugin defines, or one you typed |

Research text uses violet `#B48CFF`.

Some text reacts to the mouse. A help line is clickable, which types the command, and hovering over it shows what the command does. Friend names in `/cf friends list` are clickable and type the remove command. Hovering over a research requirement shows the node's description, and hovering over an achievement title shows its description.

Tab-completion suggestions are plain text, because the server does not colour them.

## The Fuse Linker

The <img src="images/components/fuse_linker.png" width="24" height="24" align="absmiddle"> `fuse_linker` component is a tool (craft it, or `/cf give fuse_linker`). Hold it and:

| Action | Result |
|---|---|
| Right-click a placed custom TNT | That TNT is selected. You are told to click a second one. |
| Right-click a second placed custom TNT | The two are linked both ways. A red fuse line is drawn between them. |
| Right-click the same TNT twice | "A TNT cannot be linked to itself." |
| Sneak-right-click a placed custom TNT | All of its links are removed. |

Links matter for <img src="images/tnt/fuse_linked_tnt.png" width="24" height="24" align="absmiddle"> `fuse_linked_tnt` (which relays along them) and for any mechanic that reads `links`. Any placed TNT
can be linked; only fuse-linked TNT does something with the links.

## Right-click actions

The water pump, the vertical belt and the lab machines take their fuel, upgrades and direction changes by right-click, not by
a command. The table gives each action and who may do it. None of them has a permission node (see [Permissions](#permissions)).

| Item | Action | Who may do it |
|---|---|---|
| Water pump | Right-click it with a pump kit (`water_pump_upgrade_2` to `water_pump_upgrade_5`) to move it up one level. | The owner of the pump, or a friend of the owner. |
| Water pump | Right-click it with coal or charcoal (level I) to add fuel. A level II pump takes no items: it draws diesel from the oil network. | Anyone. |
| Water pump | Right-click it with anything else to see its status lines in chat. | Anyone. |
| Vertical belt | Right-click it with an empty hand, not sneaking, to open its menu: an UP and a DOWN button, which set the direction of the whole tower. | Anyone may open the menu. Only the owner of the belt, or a friend of the owner, may change its direction. A belt with no stored owner may be changed by anyone. |
| Lab machine | Right-click a lab machine (not a drill) with `machine_upgrade_2` or `machine_upgrade_3` in the main hand, not sneaking, to move it up one level. A kit takes a machine only from the level before it. | The player who placed the machine, or a friend of that player. A machine saved before owners were kept: anyone. |

## Permissions

| Node | Default | Covers |
|---|---|---|
| `corefactory.*` | op | Every node below. |
| `corefactory.give` | op | `/cf give`, taking items from the catalogue, and another player's achievements (`/cf achievements <player>`). Only holders of this node can take items from the catalogue; everyone else can browse it and open recipes. |
| `corefactory.configure` | op | `/cf aim`, `/cf target`, `/cf unlink` and `/cf holograms`. |
| `corefactory.reload` | op | `/cf reload` and `/cf pack`. |
| `corefactory.guide` | everyone | `/cf gui` (the catalogue) and `/cf book`. |
| `corefactory.recipes` | everyone | `/cf recipe`, and right-clicking a catalogue entry. |
| `corefactory.achievements` | everyone | `/cf achievements` for yourself. |
| `corefactory.geology` | op | `/cf geology deposits` and `/cf deposits`. |
| `corefactory.research.admin` | op | `/cf research max`, `grant` and `nodes`. |
| `corefactory.pylons` | everyone | `/cf pylons show`, `/cf grid list` and `/cf grid info`. |
| `corefactory.grid` | op | `/cf grid create`, `/cf grid remove`, `/cf grid mode`, and right-clicking a chart to cycle its mode. |
| `corefactory.lock` | everyone | Setting, changing and removing the PIN lock on your own nuclear TNT. See [MECHANICS.md](MECHANICS.md#pin-lock). |
| `corefactory.lock.admin` | op | Removing any nuclear TNT's lock without its PIN, for recovery. Each removal is logged to the console. |
| `corefactory.friends` | everyone | `/cf friends add`, `remove` and `list`: your own friend list. Only you manage it; a friend cannot add or remove anyone for you. |
| `corefactory.turret` | everyone | `/cf turret info`, `target`, `player`, `team` and `friendlyfire`, and changing the targeting, team and friendly fire of the turrets you placed, in their menus. Loading, ejecting, the rail gun's power, upgrading and breaking a turret also need it, for the owner and for a friend of the owner. The permission is needed to change a turret; the owner check is separate: a turret can be changed only by its owner or a friend of its owner, whatever the permission. `info` and the targeting switches are the owner's alone. Anyone else may open a turret's menu and look at it, but cannot touch it. |

The `corefactory.*` wildcard holds `give`, `configure`, `reload`, `guide`, `achievements`, `recipes`, `geology`,
`research.admin`, `pylons`, `grid`, `lock`, `lock.admin`, `friends` and `turret`. `corefactory.guide` is also what opens the catalogue, not only the book.

Placing, lighting and detonating custom TNT needs no permission. To restrict who can place it, use your world
protection plugin, or add `disabled-worlds` in `config.yml`. Placing a lab tool or a drill needs no permission either;
the research gate is the limit on what a player may place. The same holds for pylons, accumulators, generators, turrets and the
belts, pickers and splitters: no permission node, and any research gate on the item applies. Wires need no permission
either. Charts are the exception: making, removing and changing one needs `corefactory.grid`.

Placing water parts (pipes, splitters and pumps) and vertical belts needs no permission node either; the research gate is the
limit on them. The right-click actions above need no node: the owner and friend checks in that section are the only limit on them.

Placing oil tubes and their splitters needs no permission node either. The research gate (Materials Purification) is the only limit on them.
