# Configuration

MoreTNT+ has three configuration files, all in `plugins/MoreTNTPlus/`. They are written on first start from the
defaults inside the jar, and never overwritten afterwards.

| File | Holds |
|---|---|
| `config.yml` | Global settings, performance budgets, protection, display, persistence, messages |
| `tnts.yml` | Every TNT type: name, lore, radius, fuse, mechanic and its settings, recipe, texture |
| `items.yml` | Every crafting component: name, lore, material, recipe, texture |

Apply changes with **`/mtnt reload`**. It reloads all three files and re-registers the recipes. Placed TNT
and its links stay as they are. Moving the placed-TNT registry to a different file takes a restart.

Errors in these files are reported on the console and the affected entry is skipped; the others still load.

## config.yml

### `settings`

| Key | Default | Meaning |
|---|---|---|
| `debug` | `false` | Verbose console output. |
| `disabled-worlds` | `[]` | World names (case-insensitive) where custom TNT cannot be placed or lit. |
| `max-chain-depth` | `6` | Longest chain of detonations one ignition may start. Stops runaway chains. |
| `max-detonations-per-second` | `60` | Global cap, measured over a rolling 20-tick window. Detonations beyond it are dropped. |

### `performance`

| Key | Default | Meaning |
|---|---|---|
| `blocks-per-tick` | `25000` | Block writes applied per tick. Large blasts are spread over as many ticks as needed. Lower on weak hosts. |
| `instant-threshold` | `4000` | Change sets up to this size are applied in one tick, unstaged. |
| `chain-ignite-delay-ticks` | `6` | Delay before a vanilla explosion ignites a custom TNT it reached. |
| `particles-per-tick` | `1500` | Particle packets per tick across every effect. |
| `effect-view-distance` | `96` | Blocks within which players receive sounds and particles. |

### `integrations`

| Key | Default | Meaning |
|---|---|---|
| `worldguard` | `true` | Respect WorldGuard regions whose `tnt` flag denies TNT. Used only when WorldGuard is installed. |
| `griefprevention` | `true` | Keep blasts out of GriefPrevention claims. Used only when GriefPrevention is installed. |

Both checks are cached per tick and cost nothing when no protected area is nearby. Set a key to `false` to ignore
that plugin's claims, for example on a server where blasts are meant to reach claimed land.

### `protection`

| Key | Default | Meaning |
|---|---|---|
| `unbreakable-materials` | bedrock, barriers, command blocks, structure blocks, end portal frame and similar | Materials no blast may change. |
| `protect-from-pistons` | `true` | Cancel piston moves that would carry a placed TNT. |

### `display`

| Key | Default | Meaning |
|---|---|---|
| `custom-textures` | `true` | Show each placed TNT with its own model, through an item display over the TNT block. |
| `display-scale` | `1.002` | Display scale. Slightly above 1 hides the vanilla block texture beneath. |

### `placement`

| Key | Default | Meaning |
|---|---|---|
| `direction-from-player` | `true` | Store the placer's horizontal facing as the TNT's direction. Shaped charges and napalm use it. |

### `persistence`

| Key | Default | Meaning |
|---|---|---|
| `file` | `placed-tnt.yml` | Placed-TNT registry, inside the plugin's data folder. |
| `save-interval-seconds` | `300` | Automatic save interval. The registry is always saved on shutdown. |

### `messages`

Every player-facing line. Colour codes use `&`. Placeholders such as `{id}` or `{count}` are filled in by the
plugin; keep them. The keys are listed in the shipped `config.yml`.

## tnts.yml

One entry per TNT type, keyed by its id:

```yaml
tnts:
  chain_reaction_tnt:
    name: "&6Chain Reaction TNT"        # display name, colour codes allowed
    lore:                               # lines on the item
      - "&7Lights every TNT nearby, one step at a time."
    material: TNT                       # base item material
    fuse-ticks: 60                      # fuse length once lit (20 ticks = 1 second)
    radius: 6                           # blast radius; meaning depends on the mechanic
    mechanic: chain_reaction            # see docs/MECHANICS.md
    effects: []                         # shockwave_ring, debris, echo, silent
    settings:                           # mechanic-specific numbers
      delay-per-block: 3
    recipe:                             # see docs/CRAFTING.md
      type: shaped
      pattern: ["RRR", "RTR", "RRR"]
      ingredients: {R: "material:REDSTONE", T: "material:TNT"}
    texture:                            # read by the resource pack builder only
      base: "#B5362A"
      accent: "#FFB347"
      pattern: stripes
```

Keys:

| Key | Required | Meaning |
|---|---|---|
| `name` | no | Display name. Defaults to the id. |
| `lore` | no | Item lore lines. |
| `material` | no | Base material. Defaults to `TNT`. Use `TNT`, so a placed block behaves like TNT. |
| `fuse-ticks` | no | Fuse length. Defaults to 80. |
| `radius` | no | Blast radius. Defaults to 4. |
| `mechanic` | yes | Mechanic id. An unknown id skips the TNT with a warning. |
| `effects` | no | Modifiers: `shockwave_ring`, `debris`, `echo`, `silent`. Unknown names are ignored with a warning. |
| `settings` | no | Mechanic settings. Each mechanic's keys are in [MECHANICS.md](MECHANICS.md). |
| `recipe` | no | Crafting recipe. A TNT without one is obtainable only by `/mtnt give`. |
| `texture` | no | Used only when building the resource pack. |

The `settings` of several nuclear tiers are written once with a YAML anchor (`&nuke-common`) and reused with an
alias (`*nuke-common`). Any YAML feature Bukkit's loader supports is fine to use.

## items.yml

```yaml
components:
  enriched_uranium:
    name: "&aEnriched Uranium"
    lore:
      - "&7Weapons-grade stock. Dangerous to hold."
    material: NETHERITE_SCRAP
    recipe:
      type: shapeless
      ingredients: [material:NETHERITE_SCRAP, material:NETHERITE_SCRAP, ...]
    texture:
      base: "#4E5A3A"
      accent: "#A6FF3A"
      pattern: hazard
```

A component needs a `name`, a `material` and optionally `lore`, `recipe` and `texture`. The recipe grammar is the
same as for TNT: see [CRAFTING.md](CRAFTING.md).

Texture patterns, for both files: `stripes`, `dots`, `hazard`, `rings`, `checker`, `cross`, `nuclear`, `flame`,
`bio`, `circuit`, `glyph`. Colours are hex `#RRGGBB`.

## Where things live on disk

```
plugins/MoreTNTPlus/
├── config.yml
├── tnts.yml
├── items.yml
└── placed-tnt.yml        # placed TNT, links, directions, targets (created on first placement)
```

`placed-tnt.yml` is the only file the plugin rewrites on its own. Back it up with the world if you keep placed
TNT across restarts.
