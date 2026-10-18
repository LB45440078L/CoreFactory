# Commands and permissions

## `/mtnt`

Alias: `/moretnt`. Running it with no arguments prints the help.

| Command | Permission | What it does |
|---|---|---|
| `/mtnt help` | none | Lists the subcommands. |
| `/mtnt give <id> [player] [amount]` | `moretntplus.give` | Gives a TNT type or a component. `player` defaults to you; `amount` is 1 to 64, default 1. |
| `/mtnt list` | none | Prints every TNT and component id. |
| `/mtnt gui` | `moretntplus.give` | Opens the catalogue: every TNT and component. Clicking an entry gives a copy. |
| `/mtnt info` | none | Inspects the placed TNT block you are looking at (within 6 blocks): mechanic, radius, fuse, direction, target, number of fuse links. |
| `/mtnt aim [north\|east\|south\|west\|up\|down]` | `moretntplus.configure` | Sets the direction of the placed TNT you look at. Without an argument it uses your facing. Shaped charges use this direction; napalm uses it only when dropped. |
| `/mtnt target [x y z]` | `moretntplus.configure` | Sets the teleport destination of the placed TNT you look at. Without coordinates it is the block you stand on; with `x y z` it is that block in your world. Used by teleport TNT. |
| `/mtnt unlink` | `moretntplus.configure` | Removes every fuse link of the placed TNT you look at. |
| `/mtnt count` | none | Number of placed custom TNT blocks. |
| `/mtnt reload` | `moretntplus.reload` | Reloads `config.yml`, `tnts.yml` and `items.yml`, and re-registers recipes. Placed TNT stays as it is. |

Tab completion covers subcommands, ids and directions.

## The Fuse Linker

The `fuse_linker` component is a tool (craft it, or `/mtnt give fuse_linker`). Hold it and:

| Action | Result |
|---|---|
| Right-click a placed custom TNT | That TNT is selected. You are told to click a second one. |
| Right-click a second placed custom TNT | The two are linked both ways. A red fuse line is drawn between them. |
| Right-click the same TNT twice | "A TNT cannot be linked to itself." |
| Sneak-right-click a placed custom TNT | All of its links are removed. |

Links matter for `fuse_linked_tnt` (which relays along them) and for any mechanic that reads `links`. Any placed TNT
can be linked; only fuse-linked TNT does something with the links.

## Permissions

| Node | Default | Covers |
|---|---|---|
| `moretntplus.*` | op | Everything below. |
| `moretntplus.give` | op | `/mtnt give`, `/mtnt gui`, and taking items from the catalogue. |
| `moretntplus.configure` | op | `/mtnt aim`, `/mtnt target`, `/mtnt unlink`. |
| `moretntplus.reload` | op | `/mtnt reload`. |

Placing, lighting and detonating custom TNT needs no permission. To restrict who can place it, use your world
protection plugin, or add `disabled-worlds` in `config.yml`.
