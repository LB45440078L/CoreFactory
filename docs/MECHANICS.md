# Mechanics

Every TNT type names one **mechanic** (its behaviour) and carries that mechanic's **settings** in `tnts.yml`.
Mechanics are shared: several TNT types can use the same one with different numbers. The `radius` of a TNT
means whatever its mechanic needs; the table of each family says what.

The values in brackets are the ones in the shipped `tnts.yml`. Remove a key to use the built-in fallback; where the fallback differs from the shipped value it is noted in the setting's description.

Contents: [How ignition works](#how-ignition-works) · [Standard blast and effects](#standard-blast-and-effect-modifiers) ·
[Structural](#structural--physics) · [Terrain](#terrain-morphing) · [Entity](#entity--status) ·
[Logic](#timing--logic) · [Visual / audio](#visual--audio) · [Utility](#utility--economy) ·
[Military](#military) · [Nuclear](#nuclear) · [Placement, timing and chains](#placement-timing-and-chains)

## How ignition works

| Way it is lit | What happens |
|---|---|
| Flint and steel, fire, lava, lightning, explosions reaching it | The placed block is ignited by the core (its vanilla ignition is cancelled). |
| Redstone power | Same as above, when a neighbouring source powers the block. |
| A chain from another blast | The block is ignited after a delay that grows with distance. |

Ignition depends on the mechanic's **ignition mode**:

| Mode | Used by | Behaviour |
|---|---|---|
| `PRIMED_ENTITY` | most types | The block becomes a tagged, vanilla-looking primed TNT with the TNT's fuse. When it explodes, the mechanic runs instead of the vanilla blast. |
| `DELAYED` | (silent TNT) | The block is removed; the mechanic runs after the fuse with no entity, no hiss and no boom. |
| `PERSISTENT` | fuse-linked, marker | The block stays; the mechanic runs at once. |
| `INERT` | anchor | Lighting it does nothing. |

Any TNT with the `silent` effect uses the delayed path.

## Standard blast and effect modifiers

The **standard** mechanic is a sphere of radius `radius`. Blocks inside are destroyed (except unbreakable
materials, anchor fields and containers in some mechanics). Entities inside take damage that falls off to zero at
the edge. TNT caught in the blast is **ignited, not destroyed**, so chains look right. Vanilla TNT is primed.

| Setting | Default | Meaning |
|---|---|---|
| `damage` | 12 | Maximum entity damage at the centre |
| `chain-delay-per-block` | 2 | Extra ignition delay, in ticks per block, for TNT caught in the blast |

Effect modifiers are listed under `effects:` in `tnts.yml`. Any standard-blast TNT can use them:

| Effect | Settings | What it does |
|---|---|---|
| `shockwave_ring` | `ring-ticks` (12) | A ring of flame expands for `ring-ticks` before anything breaks. Only players near the ring see it. |
| `debris` | | Up to 80 destroyed blocks fly out as falling blocks, land, and can be collected. |
| `echo` | `echo-count` (3), `echo-interval` (8), `echo-jitter` (4) | Extra booms follow at the same place, with sound and smoke only and no damage. |
| `silent` | | No boom and no fuse sound; the blast comes from a silent, delayed ignition. |

Effects are capped per tick (`performance.particles-per-tick`) and reach only players within
`performance.effect-view-distance`.

## Structural / physics

| TNT id | Mechanic | Radius means | Settings (default) | Behaviour |
|---|---|---|---|---|
| `chain_reaction_tnt` | `chain_reaction` | blast radius | `delay-per-block` (3), `chain-radius` (1.6 × radius), `damage` (10) | After the blast, TNT out to `chain-radius` is ignited with a delay of `delay-per-block` ticks per block away. A rolling wave. |
| `shaped_charge_tnt` | `shaped_charge` | cone length | `length` (radius), `cone-degrees` (25), `damage` (14), `default-direction` ([1,0,0]) | Blasts only inside a cone along the TNT's direction: the facing it was placed with, or set with `/mtnt aim`. |
| `gravity_well_tnt` | `gravity_well` | pull radius | `pull-ticks` (40), `pull-strength` (0.45), `max-debris` (300), `crater-fraction` (0.5), `damage` (6) | Blocks become floating debris and entities are pulled in for `pull-ticks`; then the debris vanishes into a smaller crater. |
| `implosion_tnt` | `implosion` | collapse radius | `core-radius` (0.25 × radius), `core-material` (BLACK_CONCRETE), `pull-strength` (1.2), `damage` (4) | Everything in the radius is removed and pulled inward, leaving a dense core of `core-material`. |
| `fuse_linked_tnt` | `fuse_linked` | not used | `link-delay` (4) | Does not explode. Lighting it relays the signal along its fuse links, one link after another, `link-delay` ticks apart. Links are drawn as red fuse lines. Set links with the Fuse Linker. |
| `delayed_cascade_tnt` | `delayed_cascade` | spread | `waves` (3), `wave-interval` (6), `submunitions` (6), `spread` (radius), `submunition-radius` (0.35 × radius), `damage` (6) | A cluster munition: several waves, each a set of small blasts scattered around the target. |
| `four_dimensional_tnt` | `four_dimensional` | hypersphere radius | `steps` (14), `step-ticks` (2), `thickness` (1.4), `damage` (10) | Carves a different 3D slice of a four-dimensional sphere on each step, with a random starting phase. The pattern is hard to predict and damages entities as it sweeps. |

## Terrain morphing

| TNT id | Mechanic | Radius means | Settings (default) | Behaviour |
|---|---|---|---|---|
| `vitrify_tnt` | `vitrify` | sphere | `outer-material` (GLASS), `inner-material` (OBSIDIAN), `inner-fraction` (0.45), `damage` (3) | Solid blocks become glass, with an obsidian core. Nothing is destroyed or dropped. |
| `liquefy_tnt` | `liquefy` | sphere | `fluid` (LAVA), `damage` (4) | Solid blocks become a fluid source: LAVA, WATER or SLIME_BLOCK. |
| `terraform_tnt` | `terraform` | sphere | `fill-material` (DIRT), `damage` (6) | Blocks are removed, and the crater is refilled with `fill-material` up to the old ground level. |
| `randomize_tnt` | `randomize` | sphere | `pool` (list of `{material, weight}`), `damage` (5) | Each block is replaced by a random pick from the weighted pool. Defaults to a stone-like mix. |
| `sculpt_tnt` | `sculpt` | outer size | `shape` (sphere, cylinder or torus), `thickness` (1.5), `height` (radius), `tube-radius` (0.3 × radius), `damage` (4) | Removes only a thin shell along the chosen shape. Precise demolition. |

## Entity / status

| TNT id | Mechanic | Radius means | Settings (default) | Behaviour |
|---|---|---|---|---|
| `concussion_tnt` | `concussion` | effect radius | `slowness-seconds` (8), `nausea-seconds` (12), `blindness-seconds` (5) | No block damage, no deaths. Slowness, nausea and blindness for everything in range. |
| `launch_tnt` | `launch` | knockback radius | `up` (1.4), `radial` (1.6), `damage` (0) | Knocks everything in range up and away. Fall damage is cancelled for the launched. |
| `teleport_tnt` | `teleport` | pick-up radius | `random-offset` (12) | Moves living entities in range, keeping their formation. With a target (`/mtnt target`) they go there; otherwise to a random spot within `random-offset`. Blocks never move. |
| `gravity_flip_tnt` | `gravity_flip` | lift radius | `duration-ticks` (100), `lift` (0.9) | Lifts everything in range. Players get levitation; other mobs have gravity off, restored afterwards. This is an approximation of inverted gravity. |

## Timing / logic

| TNT id | Mechanic | Radius means | Settings (default) | Behaviour |
|---|---|---|---|---|
| `conditional_tnt` | `conditional` | block search radius | `require-block` (none), `block-radius` (3), `require-player-within` (0 = off), `require-powered` (false), `time-min` / `time-max` (-1 = off; world time, wraps past midnight), `mode` (all or any), `damage` (14) | A logic gate. Detonates only when its conditions hold; otherwise it fizzles and drops back as an item. |
| `proximity_tnt` | `proximity_tnt` | blast radius | `arm-ticks` (60), `trigger-radius` (3), `damage` (14) | Arms `arm-ticks` after placement. The first player to come within `trigger-radius` starts its fuse. |
| `pulse_tnt` | `pulse_tnt` | blast radius | `explode` (true), `pulse-radius` (1.5 × radius), `pulse-ticks` (40), `pulse-material` (REDSTONE_BLOCK), `damage` (6) | Places a shell of redstone blocks around it for `pulse-ticks`, powering neighbours, then removes them. Optionally also explodes. |
| `mirror_tnt` | `mirror_tnt` | blast radius | `axis` (x or z), `plane-offset` (24), `damage` (10) | Carves its blast, then carves the same blocks again, reflected across a plane `plane-offset` blocks away. |

## Visual / audio

| TNT id | Mechanic | Effects | Behaviour |
|---|---|---|---|
| `shockwave_tnt` | `standard` | `shockwave_ring` | Standard blast after a flame ring. |
| `debris_tnt` | `standard` | `debris` | Standard blast; destroyed blocks fly out as collectable falling blocks. |
| `echo_tnt` | `standard` | `echo` | Standard blast followed by visual and audible echoes with no damage. |
| `silent_tnt` | `standard` | `silent` | Standard blast with no sound until it goes off. |

## Utility / economy

| TNT id | Mechanic | Radius means | Settings (default) | Behaviour |
|---|---|---|---|---|
| `harvest_tnt` | `harvest` | blast radius | `max-items` (2000), `damage` (0) | Blocks break, and their drops go to whoever lit it (the ground if nobody did). Containers survive. |
| `sorting_tnt` | `sorting` | blast radius | `sort-materials` (list of block names), `container-radius` (2 × radius), `damage` (6) | Blocks of the listed types are gathered into the nearest chest, barrel or shulker in range. Overflow drops on the ground. |
| `marker_tnt` | `marker` | outline radius | | Does not explode. While placed it outlines its region for nearby players. Lighting it reports the materials in the region to the player who lit it. |
| `anchor_tnt` | `anchor` | field radius | | Creates an anti-TNT field. While it stands, no explosion can change blocks inside its radius, and TNT inside it does not go off. Its boundary is outlined. |

## Military

| TNT id | Mechanic | Radius means | Settings (default) | Behaviour |
|---|---|---|---|---|
| `biological_tnt` | `biological` | cloud radius | `burst-radius` (0.35 × radius), `duration-seconds` (30), `tick-interval` (20), `samples` (180), `player-damage` (1.0), `mob-damage` (2.0) | A small burst, then a cloud. Creatures inside are poisoned or withered and take damage. Grass turns to dirt path; leaves, flowers and saplings die. |
| `napalm_tnt` | `napalm` | burst radius | `stretch` (1.6), `damage` (4) | **Throw it** (drop it): it bursts when it lands, spreading fire in an ellipse stretched along the way it fell; standing still it is a circle. Some burning ground becomes magma or lava. Placed and lit, it bursts in a circle. |

## Nuclear

One mechanic, `nuclear`, covers all ten tiers. Each tier only changes its radius, fuse and crafting recipe.

Effects, measured from the blast centre as a fraction of the tier's radius R:

| Distance | Effect |
|---|---|
| within 1.5 R | Countdown titles and an alarm while the fuse burns, then red warning titles at detonation. |
| within 3 R | The player's client clock is set to noon for `beam-seconds`, and a 5 × 5 array of beacons (sent to the client only) draws a beam straight up from the surface around the centre. |
| 0.5 R or less | Killed instantly. |
| 0.5 R to 0.8 R | Set on fire for `fire-far-seconds`. |
| 0.8 R to R | Set on fire for `fire-near-seconds`, and nauseated for 15 seconds. |

The crater is a circle of radius `crater-fraction` × R, cut straight down to bedrock, with sparse magma, lava and
fire on its floor. It is streamed over several ticks. Debris clumps land on the ground around the crater and are
set on fire. Creative and spectator players are not affected.

| Setting | Default | Meaning |
|---|---|---|
| `crater-fraction` | 0.45 | Crater radius as a fraction of R |
| `fire-far-seconds` | 20 | Burning at 0.5 R to 0.8 R |
| `fire-near-seconds` | 5 | Burning at 0.8 R to R |
| `beam-seconds` | 30 | How long the client-side time and beams last |
| `debris-count` | 3 × R | Number of debris clumps |
| `damage` | 60 | Entity damage at the centre, falling off with distance |

| Tier | TNT id | Reference | Radius (blocks) | Fuse (ticks) |
|---|---|---|---|---|
| 1 | `nuke_davy_crockett` | W54 family, about 20 tons | 6 | 100 |
| 2 | `nuke_little_boy` | Hiroshima, about 15 kt | 12 | 200 |
| 3 | `nuke_fat_man` | Nagasaki, about 21 kt | 14 | 220 |
| 4 | `nuke_joe_1` | RDS-1, about 22 kt | 15 | 240 |
| 5 | `nuke_ivy_king` | about 500 kt | 35 | 300 |
| 6 | `nuke_b83` | about 1.2 Mt | 45 | 340 |
| 7 | `nuke_ivy_mike` | about 10.4 Mt | 70 | 400 |
| 8 | `nuke_castle_bravo` | about 15 Mt | 85 | 420 |
| 9 | `nuke_mk17` | about 25 Mt | 105 | 480 |
| 10 | `nuke_tsar_bomba` | about 50 Mt | 150 | 600 |

Yields are approximate public figures; the radii, fuses and crater sizes are scaled for gameplay, not physics.

## Placement, timing and chains

- **Placing** records the placing player's horizontal facing as the TNT's direction (shaped charge and napalm
  use it), and shows the TNT's own texture on the block (see [RESOURCE_PACK.md](RESOURCE_PACK.md)).
- **Breaking** a placed TNT drops its custom item, not vanilla TNT.
- **Chains.** A blast ignites nearby TNT with a delay of `chain-ignite-delay-ticks` plus a distance-based step.
  Vanilla explosions that reach a placed TNT do the same. `settings.max-chain-depth` and
  `settings.max-detonations-per-second` stop a runaway chain.
- **Other TNT in a blast.** Only the standard blast ignites placed or vanilla TNT it reaches. Mechanics that carve their own
  shape (vitrify, terraform, sculpt, gravity well and the others) leave any TNT inside them in place.
- **Pistons** cannot move placed TNT, when `protection.protect-from-pistons` is on.
- **Protection.** No blast changes an `unbreakable-materials` block or a block inside an anchor field.
  WorldGuard and GriefPrevention claims are respected; see [Claims and integrations](#claims-and-integrations).

## Claims and integrations

When WorldGuard or GriefPrevention is installed, a blast leaves their protected ground alone:

- **WorldGuard:** a block inside a region whose `tnt` flag is set to deny is never changed by a blast. Chunks that no
  such region touches cost one lookup per tick and nothing per block.
- **GriefPrevention:** a block inside a claim is never changed by a blast. The check runs once per block column per
  tick: a radius-6 blast asks about 170 columns, and a nuclear crater asks once per column it covers, never once per block.

Both are switched on by default and can be turned off under `integrations:` in `config.yml`. With neither installed,
the checks are not run at all. Placing and breaking TNT in claims is already governed by those plugins' own event
handling, so MoreTNT+ does not repeat it.
