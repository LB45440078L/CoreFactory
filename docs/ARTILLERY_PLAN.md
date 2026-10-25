# Artillery: plan and status

The artillery weapon is built in five phases. All five are built, as of 2026-10-24. The decisions taken are listed at the end. What is not verified in game is listed under the status table.

## Status

Status as of 2026-10-24.

| Phase | What | Status |
| --- | --- | --- |
| A | Pure logic in Core: levels, charges, shells, ballistics, dispersion, effects geometry, safety rules, config reader, tests | Built, 2026-10-24 |
| B | Items, recipes, research, models and animated textures, the artillery machine in the world | Built, 2026-10-24 |
| C | Firing at runtime: the shell's flight, impact, effects, cooldown, the power draw | Built, 2026-10-24 |
| D | Command Center: the map, the screen, the Dialogs, raytraced target setting | Built, 2026-10-24 |
| E | Magazines: each piece holds shells and charges, loaded and unloaded in its dialog, one of each used per shot | Built, 2026-10-24 |

Not verified in game:

- The map's look: its colours, its centre mark and its strike area.
- Whether every nearby player sees the map wall while a session is open, and whether a player who walks up mid-session gets the map.
- Whether the screen's text fits its frame at every text size.
- The look of the Command Center's and the pieces' dialogs.
- The Load and Unload buttons on a real server: that a load takes one shell and one charge from the inventory, and that a broken piece gives its magazine back.

## Phase A: what exists (CoreFactory-Core/src/main/java/io/corefactory/core/artillery)

- `ArtilleryLevel`: levels I to VII, with `number()` and `ofNumber()`.
- `ChargeTier`: tier I, tier II, tier III. Tier III is the charge the heaviest shells need.
- `ArtilleryShell`: nine shells, each with its charge tier, minimum level, detonation rule, effect, and whether it is nuclear.
- `ArtilleryTable`: range, reload and scatter formulas, evaluated over the settings.
- `ArtilleryBallistics`: a quasi-parabolic flight integrated per tick with linear drag, a speed solver by bisection, and the landing point.
- `ArtilleryDispersion`: the landing point with a Gaussian scatter, seeded for tests.
- `ArtilleryEffects`: cluster submunition points, the napalm ellipse, the phosphorus cloud points, and an explosion outline.
- `ArtilleryFiringRules`: the safety checks, each returning a named verdict.
- `ArtillerySettings`: the `artillery:` section of `config.yml`, read through `ConfigValues`.
- `ArtilleryMagazine`: the shells and charges a piece holds, with load, unload and fire, each refused with its verdict.

### Level values (tier I charge)

| Level | Height (blocks) | Power | Range (blocks) | Reload (ticks) | Scatter sigma at 1000 blocks |
| --- | --- | --- | --- | --- | --- |
| I | 1.5 | 100 kW | 1500 | 400 | 60.0 |
| II | 1.5 | 150 kW | 2022 | 533 | 42.9 |
| III | 2.5 | 230 kW | 2726 | 667 | 30.7 |
| IV | 2.5 | 350 kW | 3674 | 800 | 21.9 |
| V | 2.5 | 540 kW | 4953 | 933 | 15.7 |
| VI | 3.0 | 830 kW | 6677 | 1067 | 11.2 |
| VII | 3.75 | 1270 kW | 9000 | 1200 | 8.0 |

The formulas: range = 1500 x 6^((n-1)/6); reload = 400 + 800 x (n-1)/6 ticks; scatter = 60 x (8/60)^((n-1)/6) x range/1000.

### Charge tier multipliers (range / reload / scatter)

- Tier I: 1.0 / 1.0 / 1.0
- Tier II: 1.25 / 1.25 / 1.2
- Tier III: 1.6 / 1.5 / 1.5

### Shells

| Shell | Charge / min level | Detonation | Yield | Reach (blocks) | Min distance (blocks) | Nuclear |
| --- | --- | --- | --- | --- | --- | --- |
| Explosive | I / I | contact | 1 t | 15 | 50 | no |
| High explosive | I / I | contact | 10 t | 15 | 50 | no |
| White phosphorus | I / I | air burst, 25 to 35 | smoke cloud | 8 | 50 | no |
| Napalm | I / I | contact | fire, no terrain damage | 15 | 50 | no |
| Cluster | II / II | air burst, 30 to 50 | 40 submunitions x 3 t | 45 | 50 | no |
| Enriched uranium | II / VI | air burst, 8 | 15 000 t | 28.4 | 200 | yes |
| Hydrogen thermonuclear | III / VI | air burst, 20 to 30 | 1 000 000 t | 115 | 200 | yes |
| Cluster enriched uranium | III / VII | air burst, 8 | 5 x 15 t, spread 50, spacing 40, elevation at least 25 | 65 | 200 | yes |
| Condensed antimatter | III / VII | air burst, 50 | 500 000 000 t | 913 | 2000 | yes, 12 h cooldown |

The blast radius is max(15, 1.15 x cube root of the tons). That gives about 28 blocks for the 15 kt yield, matching `tnts.yml`.

### Safety margins

- Blast margin: 20 blocks beyond the blast radius, from the impact point to the artillery and to the firing player.
- Scatter allowance: 3 sigma, added to the blast and nuclear checks.
- Nuclear player exclusion: 3 times the reach, plus the scatter.
- Elevation: 5 to 85 degrees. Cluster enriched uranium needs 25 at least.

### White phosphorus cloud balance

- Radius: 8 blocks. A player who walks out within a few seconds is clear of it.
- Damage: 0.05 HP per tick at full strength, which is 1 HP per second, one heart every 2 seconds. That is a little above Poison I.
- Half-life 20 s, duration 60 s.
- A player who stands 10 s in a fresh cloud takes about 8.5 HP. Standing the full 60 s takes about 25 HP, which is lethal. For a cloud that does not kill, halve the damage per tick.
- 600 light particles per burst.

## Known gaps in Phase A

- The nuclear yields are copied into `artillery.shells` from `tnts.yml`, because Core has no constant for them. Phase B should read them from `tnts.yml` once, so the number lives in one place.
- The TNT block yield (1 t) is a new value in `artillery.shells`.
- The antimatter reach formula gives 913 blocks, against the game's real blast of about 260 blocks. The safety rule is stricter than the blast: the fallout rule keeps every player about 2810 blocks from the target (3 times 913 plus 3 sigma, and the sigma is 24 blocks at 2000 blocks).
- Physics constants are in code: 9.8 m/s^2, the 0.05 s tick, the RK4 method and the power tolerance.

## Phase B: items, models and the machine

- Items and recipes in `materials.yml` and `items.yml`: the Tier I and Tier II explosive charges and the Tier III charge, and each shell. Recipe quantities are much larger than the turret ammunition, as asked. Enriched uranium, the hydrogen shell, the cluster enriched uranium and the antimatter shell need much harder recipes, and antimatter takes days to craft.
- Research nodes in `research.yml`: one node each for the artillery, each charge tier and each shell family.
- The artillery machine at seven levels. The 3D models at 1.5, 2.5, 3.0 and 3.75 blocks tall are procedural, as the other machines are. The animated textures need an animation pipeline in CoreFactory-ResourcePack: one texture strip per animation, with an `.mcmeta`, per model.
- Power: each level draws its power from the grid, and nothing else. The machine has no fuel slot.
- The shells and charges are items. A piece holds them in its magazine (see Phase E), not in slots of its own.

## Phase C: firing at runtime

- A firing request first checks the piece's magazine (Phase E). Then the shot is checked by `ArtilleryFiringRules`, and refused with its verdict, so no unsafe shot is fired.
- The flight is the `ArtilleryBallistics` trajectory, shown as an arc of particles or display entities.
- On landing the scatter applies, then the shell's effect: blast (through the existing explosion mechanics), smoke cloud, fire, submunitions, or a nuclear blast.
- The cluster submunitions and the napalm burst use `ArtilleryEffects`.
- The cooldown for the antimatter shell is kept per player, with a timestamp, and saved.
- The artillery draws its power while it reloads and fires. A machine without full power does not fire.
- What the player sees and hears, by stage. The flight takes time: at 45 degrees a level I shell is in the air about 17.6 seconds over 1500 blocks, and a level VII shell about 43.5 seconds over 9000 blocks, with an apex of about 2300 blocks.
  - Launch: a low report at the barrel, a muzzle flash and a puff of smoke at its tip, and the barrel kicks back about half a block and eases to rest over half a second.
  - Flight: the shell's own item model, moved along its arc each tick, with a smoke trail and a faint whistle near it. The arc ends at the landing point after the scatter, not at the aim, so the shell comes down where its blast goes off. The display is made only in loaded chunks and only while the shell is in the air; it is removed on landing, when its chunk or world unloads, and on shutdown, and it is never saved.
  - Landing: a blast on the ground throws debris and dust, and its explosion is the core's own sound. A shell that lands without a blast (napalm) gives a thud. A phosphorus cloud hisses when it forms and then every few seconds; a napalm fire crackles the same way while it burns.
  - Reload: a mechanical click when the piece is ready to fire again.
  - The volumes, the recoil and the intervals are in `artillery.presentation` in config.yml.

## Phase D: Command Center

- Items: the Command Center Unit, and the 5x5 map. The map is a real, live rendering of the world, centred on the player.
- The Command Center's screen shows a black hologram of the strike data: the impact coordinates, the chosen shells, the connected artillery, the watts required and satisfied, and the safety check result.
- Clicking the screen opens the main Dialog: View Artillery Machines, Set Target, Perform Safety Check.
- View Artillery Machines lists the machines in the grid, two pages, and each machine has its own Dialog: name, on/off, Load, Unload, its magazine, and info (wattage, shells, magazine, target).
- Set Target: the player's view is raytraced onto the map every tick while in listening mode. The map shows zoom controls, cardinal arrows, a coordinates Dialog, and a green tick to confirm. The red centre marks the strike point. After confirmation the controls disappear, and the strike area is drawn for the shell, including every submunition for a cluster.
- The listening mode has a time limit and an exit, so it cannot get stuck.

## Phase E: magazines

- Each piece holds a magazine: the shells and the charges loaded into it, saved with the piece in `artillery.yml` (`magazine-shells` and `magazine-charges`, store version 3). The capacity is `artillery.magazine-capacity`, 16 by default.
- A magazine holds one shell kind and one charge tier, the shell and tier the Command Center is set to when the load is made. A load of another kind is refused while the magazine holds anything.
- Load: from the piece's dialog, one shell and one charge of the chosen kind come out of the inventory of the player who clicks. It is refused when the magazine is full, or when a shell or a charge is missing from the inventory. Right-clicking the piece with the item is not a way to load it.
- Unload: the magazine goes back to the inventory of the player who clicks, and anything that does not fit is dropped. Breaking a piece gives its magazine back as well.
- Fire: a shot uses one shell and one charge. A piece whose magazine lacks either is refused, and the launch lists it. The Command Center's safety check does not look at the magazine.

## Decisions taken

7. Ammunition: taken. The ammunition is a magazine on each piece, loaded from the piece's dialog. Each shot uses one shell and one charge of the tier the piece holds. The loading is not done by right-click, so a piece is loaded at its Command Center.
1. Animated textures: taken. Each animated piece model and the Command Center use one texture strip per animation, with an `.mcmeta`. The frame counts are in `docs/TEXTURES.md`.
2. The live map: the Set Target mode draws one map and shows it on a wall of `map-wall-size` item frames, 5 by 5 by default, while the session is open. The wall comes down when the session ends, and the target stays on the center. The view is 128 by 128 pixels, at zoom 1, 2 or 4 blocks a pixel, scaled up to the wall.
3. The Set Target timeout: taken. The mode ends after 20 seconds with no click (`command-center.listen-timeout-seconds`), and stores nothing. Each click starts the 20 seconds again.
4. The antimatter reach: taken. The reach stays at the formula's 913 blocks, and the in-game blast is not used for the check. The fallout rule keeps every player more than 3 times the reach, plus 3 sigma of scatter, from the aim. At a range of 2000 blocks that is about 2810 blocks. The piece must also stand at least 2000 blocks from the target.
5. Nuclear yields: taken. The yields are read from `tnts.yml` (`nuke_little_boy` and `nuke_davy_crockett`) at startup. The config holds only the bomb share of the Davy Crockett, 0.75.
6. Elevation: taken. A Command Center keeps its stored elevation, 45 degrees by default. No control in this build changes it.
