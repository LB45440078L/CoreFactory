# Crafting

The cheap TNT types need only vanilla items. The strong ones need **components**: custom items you craft first.
The nuclear tiers need the most, and the Thermonuclear Core at the top of the chain is the hardest recipe on the server.

Both kinds of recipe are registered with the server when the plugin loads or `/mtnt reload` runs. Their
definitions live in [`items.yml`](../MoreTNTPlus-Spigot/src/main/resources/items.yml) (components) and
[`tnts.yml`](../MoreTNTPlus-Spigot/src/main/resources/tnts.yml) (TNT). Ingredients that are custom items match
exactly, by their identity, so a plain vanilla item of the same material never substitutes for a component.

## Components

| Component id | Name | Recipe (shaped rows, or shapeless list) |
|---|---|---|
| `fuse_linker` | Fuse Linker (tool) | shapeless: 2 Redstone, Iron Ingot |
| `fuse_wire` | Fuse Wire | shapeless: 3 Copper Ingot, Redstone |
| `shaped_charge_lens` | Shaped Charge Lens | `GIG` / `IDI` / `GIG`: G Glass, I Iron Ingot, D Diamond |
| `gravity_core` | Gravity Core | `OEO` / `ENE` / `OEO`: O Obsidian, E Ender Pearl, N Nether Star |
| `vitrifier_flux` | Vitrifier Flux | shapeless: 4 Sand, Glowstone Dust, Blaze Powder |
| `sorting_magnet` | Sorting Magnet | shapeless: 2 Iron Ingot, Redstone, Lodestone |
| `anchor_crystal` | Anchor Crystal | ` A ` / `AEA` / ` A `: A Amethyst Shard, E End Crystal |
| `pulse_capacitor` | Pulse Capacitor | `RRR` / `CCC` / `RRR`: R Redstone, C Copper Ingot |
| `quantum_detonator` | Quantum Detonator | `RDR` / `DGD` / `RDR`: R Redstone, D Diamond, G Gold Ingot |
| `biohazard_culture` | Biohazard Culture | shapeless: 2 Rotten Flesh, Spider Eye, Fermented Spider Eye, Slime Ball |
| `napalm_gel` | Napalm Gel | shapeless: Magma Cream, 2 Coal, Gunpowder, Slime Ball |
| `anchor_plate` | Anchor Plate | `III` / `IGI` / `III`: I Iron Ingot, G Gold Ingot. *Not used by a TNT recipe in the shipped config; for your own recipes.* |
| `enriched_uranium` | Enriched Uranium | shapeless: 4 Netherite Scrap, 4 Amethyst Shard, Emerald |
| `plutonium_pit` | Plutonium Pit | shapeless: 2 Enriched Uranium, Nether Star, Gold Block |
| `tritium_booster` | Tritium Booster | `GBG` / `BNB` / `GBG`: G Gold Ingot, B Blaze Powder, N Nether Star |
| `neutron_initiator` | Neutron Initiator | shapeless: Amethyst Block, 2 Gunpowder, Redstone Block |
| `nuclear_casing` | Nuclear Casing | `IGI` / `GDG` / `IGI`: I Iron Block, G Gold Block, D Diamond Block |
| `thermonuclear_core` | Thermonuclear Core | `PBP` / `BNB` / `PBP`: P Plutonium Pit, B Tritium Booster, N Neutron Initiator |

The component chain for the nuclear family is therefore: Netherite Scrap and Amethyst into **Enriched Uranium**;
Enriched Uranium with Nether Star and Gold Block into a **Plutonium Pit**; Gold, Blaze Powder and Nether Star into a
**Tritium Booster**; and Thermonuclear Core from pits, boosters and an initiator.

## TNT recipes

The shipped recipes place the TNT item (`T`) in the middle or on the outside of a 3×3 grid. Vanilla TNT is the base
of every type, so you still need gunpowder and sand to make the first one. Recipes with empty cells are shaped;
the nuclear recipes use only shaped grids too.

| TNT id | Recipe |
|---|---|
| `chain_reaction_tnt` | `RRR` / `RTR` / `RRR` — R Redstone, T TNT |
| `shaped_charge_tnt` | `LIL` / `ITI` / `LIL` — L Shaped Charge Lens, I Iron Ingot, T TNT |
| `gravity_well_tnt` | `OGO` / `GTG` / `OGO` — O Obsidian, G Gravity Core, T TNT |
| `implosion_tnt` | `DDD` / `DTD` / `DDD` — D Diamond Block, T TNT |
| `fuse_linked_tnt` | `WRW` / `RTR` / `WRW` — W Fuse Wire, R Redstone, T TNT |
| `delayed_cascade_tnt` | `SGS` / `GTG` / `SGS` — S Sand, G Gunpowder, T TNT |
| `four_dimensional_tnt` | `GNG` / `NTN` / `GNG` — G Amethyst Shard, N Neutron Initiator, T TNT |
| `vitrify_tnt` | `FSF` / `STS` / `FSF` — F Vitrifier Flux, S Glass, T TNT |
| `liquefy_tnt` | `WLW` / `LTL` / `WLW` — W Water Bucket, L Lava Bucket, T TNT |
| `terraform_tnt` | `DGD` / `GTG` / `DGD` — D Dirt, G Gravel, T TNT |
| `randomize_tnt` | `CRC` / `RTR` / `CRC` — C Cobblestone, R Redstone, T TNT |
| `sculpt_tnt` | `BIB` / `ITI` / `BIB` — B Iron Block, I Iron Ingot, T TNT |
| `concussion_tnt` | `GBG` / `BTB` / `GBG` — G Glowstone Dust, B Blaze Powder, T TNT |
| `launch_tnt` | `SPS` / `PTP` / `SPS` — S Slime Block, P Piston, T TNT |
| `teleport_tnt` | `EPE` / `PTP` / `EPE` — E Ender Pearl, P Chorus Fruit, T TNT |
| `gravity_flip_tnt` | `FGF` / `GTG` / `FGF` — F Feather, G Gravity Core, T TNT |
| `conditional_tnt` | `CRC` / `RTR` / `CRC` — C Comparator, R Redstone, T TNT |
| `proximity_tnt` | `QPQ` / `PTP` / `QPQ` — Q Quantum Detonator, P Stone Pressure Plate, T TNT |
| `pulse_tnt` | `CRC` / `CTC` / `CRC` — C Pulse Capacitor, R Redstone Block, T TNT |
| `mirror_tnt` | `GIG` / `ITI` / `GIG` — G Glass, I Iron Ingot, T TNT |
| `shockwave_tnt` | `FFF` / `FTF` / `FFF` — F Firework Rocket, T TNT |
| `debris_tnt` | `CCC` / `CTC` / `CCC` — C Cobblestone, T TNT |
| `echo_tnt` | `NNN` / `NTN` / `NNN` — N Note Block, T TNT |
| `silent_tnt` | `WWW` / `WTW` / `WWW` — W White Wool, T TNT |
| `harvest_tnt` | `HHH` / `HTH` / `HHH` — H Hopper, T TNT |
| `sorting_tnt` | `CMC` / `MTM` / `CMC` — C Chest, M Sorting Magnet, T TNT |
| `marker_tnt` | `PLP` / `LTL` / `PLP` — P Paper, L Lapis Lazuli, T TNT |
| `anchor_tnt` | `ACA` / `CTC` / `ACA` — A Anchor Crystal, C Crying Obsidian, T TNT |
| `biological_tnt` | `BCB` / `CTC` / `BCB` — B Biohazard Culture, C Composter, T TNT |
| `napalm_tnt` | `NGN` / `GTG` / `NGN` — N Napalm Gel, G Gunpowder, T TNT |
| `nuke_davy_crockett` | `ENE` / `NCN` / `ENE` — E Enriched Uranium, N Neutron Initiator, C Nuclear Casing |
| `nuke_little_boy` | `EEE` / `ECE` / `EEE` — E Enriched Uranium, C Nuclear Casing |
| `nuke_fat_man` | `PEP` / `ECE` / `PEP` — P Plutonium Pit, E Enriched Uranium, C Nuclear Casing |
| `nuke_joe_1` | `EPE` / `PCP` / `EPE` — E Enriched Uranium, P Plutonium Pit, C Nuclear Casing |
| `nuke_ivy_king` | `PBP` / `BCB` / `PBP` — P Plutonium Pit, B Tritium Booster, C Nuclear Casing |
| `nuke_b83` | `BPB` / `PCP` / `BPB` — B Tritium Booster, P Plutonium Pit, C Nuclear Casing |
| `nuke_ivy_mike` | `BBB` / `PCP` / `BBB` — B Tritium Booster, P Plutonium Pit, C Thermonuclear Core |
| `nuke_castle_bravo` | `NBN` / `PCP` / `NBN` — N Neutron Initiator, B Tritium Booster, P Plutonium Pit, C Thermonuclear Core |
| `nuke_mk17` | `PPP` / `BCB` / `PPP` — P Plutonium Pit, B Tritium Booster, C Thermonuclear Core |
| `nuke_tsar_bomba` | `MBM` / `BCB` / `MBM` — M Thermonuclear Core, B Tritium Booster, C Nuclear Casing |

## The recipe grammar

A recipe block in `tnts.yml` or `items.yml`:

```yaml
recipe:
  type: shaped              # or shapeless
  amount: 1                 # results per craft (optional, default 1)

  # shaped only: up to three rows of three characters; a space is an empty cell
  pattern: ["GIG", "IDI", "GIG"]
  ingredients:
    G: "material:GLASS"
    I: "material:IRON_INGOT"
    D: "material:DIAMOND"

  # shapeless only: up to nine ingredients, in any arrangement
  # ingredients: [material:REDSTONE, material:REDSTONE, material:IRON_INGOT]
```

Ingredient strings:

| Form | Meaning |
|---|---|
| `material:NAME` | A vanilla item. `NAME` is a Bukkit material, e.g. `material:IRON_INGOT`. |
| `NAME` (bare) | Also a vanilla material. |
| `item:<id>` | A component from `items.yml`, e.g. `item:enriched_uranium`. |
| `tnt:<id>` | A TNT type from `tnts.yml`, e.g. `tnt:chain_reaction_tnt`. |

A recipe whose ingredient is unknown, or whose pattern is malformed, is **skipped with a warning** in the console,
and the rest load normally. Check the log after editing.

## Changing the progression

- To make a TNT easier, drop a component from its recipe, or replace `item:` with a `material:` ingredient.
- To add a new component: add it under `components:` in `items.yml` with a `recipe:` (or none, if it should be
  obtained only from a loot table or a shop), then reference it with `item:<id>` anywhere.
- After any change, `/mtnt reload`. Recipes are removed and registered again; placed TNT keeps working.
  A player who already holds an item keeps it, and its item stays valid because identity is the stored id.
