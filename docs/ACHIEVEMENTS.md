# Achievements

CoreFactory has its own achievements, separate from the vanilla advancements. There are 364 of them with the shipped data.
The count is built from the loaded TNT types, components, research nodes and machines, so a new type gets its achievements on
the next `/cf reload`, and the total changes with the data files.

Open them with `/cf achievements`. The menu shows 45 entries a page, which is nine pages with the shipped data. Unlocked entries glow and show the date they were
earned; locked entries show progress. Each unlock is announced by the server, to everyone, through the Advancements screen (see below).

## What counts

Progress comes from things a player does themselves. Items given by `/cf give`, the catalogue, or a console command
do not count, so the achievements measure play.

| Event | Credited to |
|---|---|
| Crafting a TNT or component from its recipe, at a crafting table | The player who took the result. |
| Using a component as an ingredient in a recipe | The player who crafted it. |
| Lighting a placed TNT with flint and steel or a fire charge | The player who lit it. |
| Setting off a TNT, including a chain it starts | The player who started the chain. |
| A fuse-linked pair | The player who linked it, when the link is made. |
| Dropping a TNT item | The player who dropped it. |
| Mining an ore block | The player who mined it. |
| Taking an item out of a machine's output slots | The player who took it. |
| Placing a machine | The player who placed it. |
| Finishing a research node at a console | The player who fed the last packs. |
| Redstone power, or a blast from another plugin | Nobody. |

## The sets

### Per TNT type (126 achievements: 3 for each of the 42 types)

Each TNT type has three achievements, named from its display name.

| Kind | Title | Needs |
|---|---|---|
| Forging | `Forged: <name>` | Craft one from its recipe. |
| Lighting | `Lit: <name>` | Light a placed one. |
| Detonating | `Detonated: <name>` | Set one off. |

The nuclear tiers count as `Detonated: ...` in the Nuclear section, and they feed the Atomic Age mastery.

### Per component (182 achievements: 2 for each of the 91 components)

Every component counts, including the lab tools, the drills, the Ore Density Scanner, the solar panel, and petroleum.

| Kind | Title | Needs |
|---|---|---|
| Forging | `Forged: <name>` | Craft one. |
| Using | `Used: <name>` | Use it as an ingredient in a recipe. |

### Counters and feats (19 achievements)

| Achievement | Section | Needs |
|---|---|---|
| First Steps | Getting started | Craft any TNT or component. |
| Busy Hands | Forging | Craft 25. |
| Factory Floor | Forging | Craft 100. |
| First Spark | Getting started | Light any TNT. |
| Pyromaniac | Lighting | Light 50. |
| Boom | Getting started | Set off a TNT. |
| Ten Blasts | Detonations | Set off 10. |
| Demolition | Detonations | Set off 100. |
| Armageddon | Detonations | Set off 1000. |
| Wired Up | Using components | Link two placed TNT. |
| Electrician | Using components | Link 50. |
| Throw It | Using components | Drop a TNT item. |
| Napalm Pitcher | Using components | Throw a <img src="images/tnt/napalm_tnt.png" width="24" height="24" align="absmiddle"> napalm TNT, so it bursts on landing. |
| Domino | Chaos | Start a chain of three detonations. |
| Cascade | Chaos | Start a chain of eight. |
| Mushroom Cloud | Nuclear | Set off a nuclear weapon. |
| Nuclear Habit | Nuclear | Set off ten. |
| Village Nuker | Chaos | A nuclear blast that kills at least three villagers near its centre. |
| Still Standing | Nuclear | Stand within 1.5 times a nuclear blast's radius when it goes off, and be alive ten seconds later. |

Village Nuker is checked ten seconds after the blast: the villagers found within the radius when the blast started are
counted, and at least three must be dead by then.

### Science (30 achievements)

The thirty are: Prospector, Crushing Work, First Research Pack, the seven products below, the fifteen research nodes, First Machine, Full Laboratory (one of each of the 11 machine kinds), and the three First Blast feats.

| Achievement | Needs |
|---|---|
| Prospector | Mine any ore block. |
| Crushing Work | Take crushed ore out of a <img src="images/components/crusher.png" width="24" height="24" align="absmiddle"> crusher. |
| First Research Pack | Take a research pack out of a <img src="images/components/spectrometer.png" width="24" height="24" align="absmiddle"> spectrometer. |
| Made: <img src="images/components/yellowcake.png" width="24" height="24" align="absmiddle"> Yellowcake (U3O8), <img src="images/components/uf6_cylinder.png" width="24" height="24" align="absmiddle"> UF6 Cylinder, <img src="images/components/enriched_uranium.png" width="24" height="24" align="absmiddle"> Enriched Uranium, <img src="images/components/u233_oxide.png" width="24" height="24" align="absmiddle"> Uranium-233 Oxide, <img src="images/components/plutonium_oxide.png" width="24" height="24" align="absmiddle"> Plutonium Oxide, <img src="images/components/deuterium_cell.png" width="24" height="24" align="absmiddle"> Deuterium Cell, <img src="images/components/antimatter_condensate.png" width="24" height="24" align="absmiddle"> Antimatter Condensate | Take one out of a machine. One feat each. |
| Researched: `<node>` | Finish each research node. One feat per node, fifteen in all. |
| First Machine | Place any machine. |
| Full Laboratory | Place one machine of every kind: the 11 kinds of lab tool. |
| First Blast: <img src="images/tnt/exotic_neutron_bomb.png" width="24" height="24" align="absmiddle"> Exotic Neutron Bomb, <img src="images/tnt/exotic_antimatter_bomb.png" width="24" height="24" align="absmiddle"> Antimatter Bomb, <img src="images/tnt/four_dimensional_tnt.png" width="24" height="24" align="absmiddle"> 4-Dimensional Bomb | Set off one of each. These also count toward the matching `Detonated:` achievement. |

### Mastery (7 achievements)

Each one unlocks when every achievement in its group is unlocked.

| Achievement | Group |
|---|---|
| Full Arsenal | Every TNT type forged. |
| Full Armoury | Every component forged. |
| Every Spark | Every TNT type lit. |
| Annihilator | Every TNT type detonated. |
| Atomic Age | Every nuclear tier detonated. |
| Engineer | Every component used in a recipe. |
| Completionist | Every other achievement. |

## In the Advancements screen

The achievements are also written as a vanilla datapack, so they appear in the Advancements button (the book on the
inventory screen, or the advancements key) under a **CoreFactory** tab, with the TNT and component models as their icons.
Each category is a branch of the tab, and each achievement is a node under its category.

- **Unlocks are announced by the server**, as for any advancement: the toast for the player, and the chat message
  "<player> has made the advancement [...]" for everyone. Vanilla's `announceAdvancements` gamerule controls the chat
  message; if an operator turns it off, nothing is announced in chat.
- **Progress is shared with the vanilla advancement**, so a counter such as "Armageddon" fills up in the tab as the
  player goes. Counters are split into at most 20 steps.
- **Mastery groups** complete when their last member does, as a vanilla advancement.

The datapack is `world/datapacks/CoreFactory-achievements`. It is rewritten on every start, so do not edit it. The
server reads datapacks at startup. If the tab does not appear after the first start with this version, run
`datapack enable "file/CoreFactory-achievements"` in the console, or restart once. The pack declares the data pack
format range of the running server, read from its built-in data packs. If the pack was disabled with
`/datapack disable`, it stays disabled until it is enabled again.

Achievements unlocked before this version still show in `/cf achievements`. They are not re-announced, and their
vanilla progress starts from the update.

## Storage

Progress is kept in `plugins/CoreFactory/achievements.yml`. It holds, for each player, the count toward each
achievement and the time each one was unlocked. It is saved every five minutes, when a player leaves, and when the
server stops.

To reset one player, remove their entry from the file while the server is stopped. To reset everyone, delete the file.

## Limits

- A craft is counted once per crafted result, so a shift-click that makes several items counts as one craft.
- A fuse link counts when the link is made, and the link can be cleared again: a link that is later removed still
  counts.
- Village Nuker and Still Standing are judged by the state of the world ten seconds after the blast. A villager who dies
  later, or a player who logs out before the check, is not credited.
- Blasts started by redstone or by another plugin are not credited to anyone.
