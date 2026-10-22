# The science of CoreFactory

CoreFactory has a production chain modelled on the real nuclear fuel cycle, a research tree that gates every bomb, and a
set of exotic weapons at the top. This guide follows the chain from the ground to the warhead. The numbers and recipes
are in `processes.yml`, `materials.yml`, `research.yml` and `ores.yml`, which you can change.

## 1. Find the ore

<img src="images/ores/uraninite_ore.png" width="48" height="48" align="absmiddle">

Ore is laid in deposits on top of the vanilla terrain. A deposit is a circular patch of one ore, with a remaining amount,
and its blocks are that ore's host block, about 30 percent of the stone in the patch, so a patch shows as scattered ore
blocks over stone. The deposits are planned from the world seed, so the same seed always gives the same deposits. Mining a
placed ore block gives its raw mineral and nothing else. Drills take raw minerals from a deposit, one for each unit they
take. The planning numbers are in the `deposits` section of `config.yml`, and `/cf geology deposits [radius]` lists the
deposits near you. See `ores.yml` for the height band and the host block of each ore.

| Ore | Found | Raw mineral | Why it matters |
|---|---|---|---|
| <img src="images/ores/uraninite_ore.png" width="24" height="24" align="absmiddle"> Uraninite | deep | <img src="images/components/raw_uraninite.png" width="24" height="24" align="absmiddle"> Raw uraninite | The main uranium ore. Black, pitchy, faintly warm. |
| <img src="images/ores/pitchblende_ore.png" width="24" height="24" align="absmiddle"> Pitchblende | very deep | <img src="images/components/raw_pitchblende.png" width="24" height="24" align="absmiddle"> Raw pitchblende | Almost pure uranium oxide. |
| <img src="images/ores/monazite_ore.png" width="24" height="24" align="absmiddle"> Monazite | high up | <img src="images/components/raw_monazite.png" width="24" height="24" align="absmiddle"> Raw monazite | Rare earths and thorium. |
| <img src="images/ores/thorite_ore.png" width="24" height="24" align="absmiddle"> Thorite | very deep | <img src="images/components/raw_thorite.png" width="24" height="24" align="absmiddle"> Raw thorite | Thorium-232, the fertile isotope. |
| <img src="images/ores/galena_ore.png" width="24" height="24" align="absmiddle"> Galena | mid | <img src="images/components/raw_galena.png" width="24" height="24" align="absmiddle"> Raw galena | Lead, for shielding. |
| <img src="images/ores/cinnabar_ore.png" width="24" height="24" align="absmiddle"> Cinnabar | mid | <img src="images/components/raw_cinnabar.png" width="24" height="24" align="absmiddle"> Raw cinnabar | Mercury. Roasting frees it. |
| <img src="images/ores/fluorite_ore.png" width="24" height="24" align="absmiddle"> Fluorite | wide | <img src="images/components/raw_fluorite.png" width="24" height="24" align="absmiddle"> Raw fluorite | Fluorine, for converting uranium into a gas. |
| <img src="images/ores/crude_petroleum_deposit.png" width="24" height="24" align="absmiddle"> Crude petroleum deposit | deep, from y -64 to y 0 | <img src="images/components/petroleum.png" width="24" height="24" align="absmiddle"> Petroleum | Fuel for the ore drills. A petroleum drill draws the deposit down, one petroleum for each unit it takes. |

Ores that were already in your world before the plugin was installed are laid the first time their chunks load after the
upgrade. Each chunk is laid once, so a restart adds no ore blocks. There is no retrofit command.

## 2. The uranium chain

<img src="images/components/yellowcake.png" width="48" height="48" align="absmiddle">

Uranium does not split as it comes out of the ground. It has to be concentrated, converted into a gas, and enriched
until the U-235 is a larger share. Each step below is a machine.

1. <img src="images/components/crusher.png" width="24" height="24" align="absmiddle"> **Crusher.** Raw ore becomes crushed grit (two crushed per raw).
2. <img src="images/components/roaster.png" width="24" height="24" align="absmiddle"> **Roasting kiln.** Crushed uranium ore is roasted with fuel (coal or charcoal). It gives <img src="images/components/uranium_calcine.png" width="24" height="24" align="absmiddle"> *uranium calcine* and some
   <img src="images/components/slag.png" width="24" height="24" align="absmiddle"> slag. Roasting drives off volatiles and oxidises the uranium.
3. <img src="images/components/separator.png" width="24" height="24" align="absmiddle"> **Centrifugal separator.** Calcine is separated by density into <img src="images/components/yellowcake.png" width="24" height="24" align="absmiddle"> **yellowcake** and tailings. Yellowcake is U3O8, the
   concentrated uranium oxide.
4. <img src="images/components/converter.png" width="24" height="24" align="absmiddle"> **Fluorination converter.** Yellowcake and <img src="images/components/crushed_fluorite.png" width="24" height="24" align="absmiddle"> crushed fluorite react, with fuel, into a <img src="images/components/uf6_cylinder.png" width="24" height="24" align="absmiddle"> **UF6 cylinder**. Uranium
   hexafluoride turns into a gas at modest temperature, and its isotopes can be separated in a gas, which is why the cycle uses it.
5. <img src="images/components/enrichment_cascade.png" width="24" height="24" align="absmiddle"> **Enrichment cascade.** Eight UF6 cylinders enter a bank of gas centrifuges. Heavier U-238 spins outward and lighter
   U-235 collects. The cascade gives <img src="images/components/enriched_uranium.png" width="24" height="24" align="absmiddle"> **enriched uranium** (U-235 metal) and two cylinders of <img src="images/components/depleted_uranium.png" width="24" height="24" align="absmiddle"> **depleted uranium**.

Enriched uranium is the fuel of the first warheads. Depleted uranium is dense and cheap, and is kept as a by-product.

## 3. Thorium, U-233 and plutonium

<img src="images/components/u233_oxide.png" width="48" height="48" align="absmiddle">

<img src="images/ores/monazite_ore.png" width="24" height="24" align="absmiddle"> Monazite and <img src="images/ores/thorite_ore.png" width="24" height="24" align="absmiddle"> thorite do not fission on their own. They breed. In the <img src="images/components/transmuter.png" width="24" height="24" align="absmiddle"> **neutron transmuter**, a <img src="images/components/thorium_concentrate.png" width="24" height="24" align="absmiddle"> thorium concentrate or a
rare-earth concentrate is put in a neutron flux with a <img src="images/components/neutron_flux_cell.png" width="24" height="24" align="absmiddle"> flux cell. Thorium-232 absorbs a neutron, becomes thorium-233,
decays twice, and becomes <img src="images/components/u233_oxide.png" width="24" height="24" align="absmiddle"> **U-233 oxide**, a fissile isotope. Monazite gives U-233 only half of the time, so it is the
harder source.

<img src="images/components/yellowcake.png" width="24" height="24" align="absmiddle"> Yellowcake breeds <img src="images/components/plutonium_oxide.png" width="24" height="24" align="absmiddle"> **plutonium oxide** in the same transmuter, and a <img src="images/components/plutonium_pit.png" width="24" height="24" align="absmiddle"> plutonium pit is pressed from four plutonium oxide pellets, a
nether star and a block of gold. Plutonium is what the implosion designs (<img src="images/tnt/nuke_fat_man.png" width="24" height="24" align="absmiddle"> Fat Man, <img src="images/tnt/nuke_joe_1.png" width="24" height="24" align="absmiddle"> Joe-1 and the thermonuclear secondaries) need.

## 4. Hydrogen and particle physics

<img src="images/components/deuterium_cell.png" width="48" height="48" align="absmiddle">

The <img src="images/components/purifier.png" width="24" height="24" align="absmiddle"> purification still does two things.

- **Electrolysis.** Two buckets of water become a <img src="images/components/deuterium_cell.png" width="24" height="24" align="absmiddle"> **deuterium cell**. Deuterium is hydrogen with a neutron. It is the
  fuel of the fusion stages and of the <img src="images/components/tritium_booster.png" width="24" height="24" align="absmiddle"> tritium booster.
- **Zone refining.** Iron ingots are refined into <img src="images/components/purified_iron.png" width="24" height="24" align="absmiddle"> **purified iron**, and coal blocks into <img src="images/components/high_purity_graphite.png" width="24" height="24" align="absmiddle"> **high-purity graphite** (a
  moderator). Both are needed for magnets and <img src="images/components/neutron_flux_cell.png" width="24" height="24" align="absmiddle"> flux cells.

The <img src="images/components/particle_accelerator.png" width="24" height="24" align="absmiddle"> particle accelerator takes deuterium and a <img src="images/components/superconducting_magnet.png" width="24" height="24" align="absmiddle"> superconducting magnet, and makes **antiprotons**. <img src="images/components/antiproton_capsule.png" width="24" height="24" align="absmiddle"> Antiproton capsules
are the start of antimatter. In the <img src="images/components/antimatter_condenser.png" width="24" height="24" align="absmiddle"> **antimatter condenser**, two capsules and a flux cell are held in a field for a
long time (about two minutes), and condense into <img src="images/components/antimatter_condensate.png" width="24" height="24" align="absmiddle"> **antimatter condensate**: a few atoms of it, enough for one bomb.

## 5. Exotic matter

<img src="images/components/tesseract_lattice.png" width="48" height="48" align="absmiddle">

The accelerator can also run neutron-rich material into a <img src="images/components/neutronium_slug.png" width="24" height="24" align="absmiddle"> **neutronium slug**, which is heavier than the rest of your
inventory together. The <img src="images/components/tesseract_lattice.png" width="24" height="24" align="absmiddle"> **tesseract lattice** is built by hand from magnets, antiprotons and graphite. It is the
hardest item in the game to make by hand.

## 6. Research

<img src="images/components/research_pack_basic.png" width="48" height="48" align="absmiddle">

The research tree is one line. Each node needs the one before it, and no node can be skipped. A node is finished by
feeding packs into a <img src="images/components/research_console.png" width="24" height="24" align="absmiddle"> **research console**. The packs come from the <img src="images/components/spectrometer.png" width="24" height="24" align="absmiddle"> **spectrometer**, which reads a sample and prints a
pack. Each pack has its own colour and its own cost:

| Pack | Made from |
|---|---|
| <img src="images/components/research_pack_basic.png" width="24" height="24" align="absmiddle"> Basic | <img src="images/components/crushed_galena.png" width="24" height="24" align="absmiddle"> crushed galena or <img src="images/ores/cinnabar_ore.png" width="24" height="24" align="absmiddle"> cinnabar |
| <img src="images/components/research_pack_chemical.png" width="24" height="24" align="absmiddle"> Chemical | <img src="images/components/purified_iron.png" width="24" height="24" align="absmiddle"> purified iron and mercury |
| <img src="images/components/research_pack_nuclear.png" width="24" height="24" align="absmiddle"> Nuclear | <img src="images/components/yellowcake.png" width="24" height="24" align="absmiddle"> yellowcake |
| <img src="images/components/research_pack_plasma.png" width="24" height="24" align="absmiddle"> Plasma | deuterium and a <img src="images/components/tritium_booster.png" width="24" height="24" align="absmiddle"> tritium booster |
| <img src="images/components/research_pack_exotic.png" width="24" height="24" align="absmiddle"> Exotic | <img src="images/components/antiproton_capsule.png" width="24" height="24" align="absmiddle"> antiproton capsules |

Progress is saved permanently, in SQLite (one file, one server) or PostgreSQL (shared by several servers, set in
`config.yml`). A gated item cannot be crafted, placed or lit until its node is finished. The gate exists so that nobody
can take a bomb from another player, or hack one together, without the research.

The nodes, in order: laboratory basics, radiation safety, metallurgy, chemical separation, fluorine chemistry, nuclear
fission, hydrogen processes, materials purification, plasma physics, thermonuclear engineering, isotope transmutation,
particle physics, antimatter physics, exotic matter, tesseract geometry.

The drills and the solar panel are on the same line. Petroleum drills I and II come with Chemical Separation, and ore drill I with Fluorine Chemistry. The higher levels follow the line, up to the thermonuclear node. The solar panel comes with Metallurgy. See [CRAFTING.md](CRAFTING.md) for the recipes.

## 7. Warheads

<img src="images/tnt/nuke_fat_man.png" width="48" height="48" align="absmiddle">

| Weapon | Node | Yield | What it does |
|---|---|---|---|
| <img src="images/tnt/nuke_davy_crockett.png" width="24" height="24" align="absmiddle"> Davy Crockett, <img src="images/tnt/nuke_little_boy.png" width="24" height="24" align="absmiddle"> Little Boy, <img src="images/tnt/nuke_fat_man.png" width="24" height="24" align="absmiddle"> Fat Man, <img src="images/tnt/nuke_joe_1.png" width="24" height="24" align="absmiddle"> Joe-1 | nuclear fission | 20 t to 22 kt | The fission bombs. |
| <img src="images/tnt/nuke_ivy_king.png" width="24" height="24" align="absmiddle"> Ivy King | hydrogen processes | 0.5 Mt | Boosted fission. |
| <img src="images/tnt/nuke_ivy_mike.png" width="24" height="24" align="absmiddle"> Ivy Mike, <img src="images/tnt/nuke_castle_bravo.png" width="24" height="24" align="absmiddle"> Castle Bravo | plasma physics | 10 to 15 Mt | Thermonuclear. Show beacon beams to nearby clients. |
| <img src="images/tnt/nuke_mk17.png" width="24" height="24" align="absmiddle"> Mk-17, <img src="images/tnt/nuke_tsar_bomba.png" width="24" height="24" align="absmiddle"> Tsar Bomba | thermonuclear engineering | 25 and 50 Mt | The largest deliverable and tested weapons. |
| <img src="images/tnt/nuke_b83.png" width="24" height="24" align="absmiddle"> B83 gravity bomb | isotope transmutation | 1.2 Mt | A strategic weapon. |
| <img src="images/tnt/exotic_neutron_bomb.png" width="24" height="24" align="absmiddle"> Exotic neutron bomb | exotic matter | 5 kt | A small blast. Kills by radiation dose, far beyond the blast. A full suit holds the dose off. |
| <img src="images/tnt/exotic_antimatter_bomb.png" width="24" height="24" align="absmiddle"> Antimatter bomb | antimatter physics | 180 Mt | A pinch of condensed antimatter and a huge explosion. |
| <img src="images/tnt/four_dimensional_tnt.png" width="24" height="24" align="absmiddle"> 4-dimensional bomb | tesseract geometry | 1000 Mt | The strongest weapon in the game. Its recipe is the hardest. |

The fallout of every bomb is described in [MECHANICS.md](MECHANICS.md), and the protection (the Radioactive Suit and
the <img src="images/components/geiger_counter.png" width="24" height="24" align="absmiddle"> Geiger counter) in the same place.

## 8. Machines at a glance

<img src="images/components/crusher.png" width="48" height="48" align="absmiddle">

| Machine | Does | Fuel |
|---|---|---|
| <img src="images/components/crusher.png" width="24" height="24" align="absmiddle"> Crusher | Raw ore to crushed ore | no |
| <img src="images/components/roaster.png" width="24" height="24" align="absmiddle"> Roasting kiln | Crushed ore to calcine, lead, mercury | yes |
| <img src="images/components/separator.png" width="24" height="24" align="absmiddle"> Centrifugal separator | Calcine to <img src="images/components/yellowcake.png" width="24" height="24" align="absmiddle"> yellowcake; <img src="images/ores/monazite_ore.png" width="24" height="24" align="absmiddle"> monazite and <img src="images/ores/thorite_ore.png" width="24" height="24" align="absmiddle"> thorite to concentrates | no |
| <img src="images/components/purifier.png" width="24" height="24" align="absmiddle"> Purification still | Water to deuterium; iron to <img src="images/components/purified_iron.png" width="24" height="24" align="absmiddle"> purified iron; coal to graphite | no |
| <img src="images/components/converter.png" width="24" height="24" align="absmiddle"> Fluorination converter | <img src="images/components/yellowcake.png" width="24" height="24" align="absmiddle"> Yellowcake and <img src="images/ores/fluorite_ore.png" width="24" height="24" align="absmiddle"> fluorite to UF6 | yes |
| <img src="images/components/enrichment_cascade.png" width="24" height="24" align="absmiddle"> Enrichment cascade | UF6 to enriched and <img src="images/components/depleted_uranium.png" width="24" height="24" align="absmiddle"> depleted uranium | no |
| <img src="images/components/transmuter.png" width="24" height="24" align="absmiddle"> Neutron transmuter | Thorium, <img src="images/ores/monazite_ore.png" width="24" height="24" align="absmiddle"> monazite and <img src="images/components/yellowcake.png" width="24" height="24" align="absmiddle"> yellowcake to U-233 and plutonium | no |
| <img src="images/components/spectrometer.png" width="24" height="24" align="absmiddle"> Spectrometer | Samples to research packs | no |
| <img src="images/components/particle_accelerator.png" width="24" height="24" align="absmiddle"> Particle accelerator | Deuterium to antiprotons; <img src="images/components/neutron_flux_cell.png" width="24" height="24" align="absmiddle"> flux cells to neutronium | no |
| <img src="images/components/antimatter_condenser.png" width="24" height="24" align="absmiddle"> Antimatter condenser | Antiprotons to <img src="images/components/antimatter_condensate.png" width="24" height="24" align="absmiddle"> antimatter condensate | no |
| <img src="images/components/research_console.png" width="24" height="24" align="absmiddle"> Research console | Packs to finished research | no |
| <img src="images/components/induction_furnace.png" width="24" height="24" align="absmiddle"> Induction furnace | Copper and <img src="images/components/zinc_ingot.png" width="24" height="24" align="absmiddle"> zinc ingots to <img src="images/components/brass_ingot.png" width="24" height="24" align="absmiddle"> brass ingots | Electric |
| <img src="images/components/powder_mill.png" width="24" height="24" align="absmiddle"> Powder mill | <img src="images/components/charcoal.png" width="24" height="24" align="absmiddle"> Charcoal, <img src="images/components/sulfur.png" width="24" height="24" align="absmiddle"> sulfur and <img src="images/components/high_purity_potassium_nitrate.png" width="24" height="24" align="absmiddle"> high-purity potassium nitrate to <img src="images/components/high_grade_gunpowder.png" width="24" height="24" align="absmiddle"> high-grade gunpowder | Electric |
| <img src="images/components/cartridge_press.png" width="24" height="24" align="absmiddle"> Cartridge press | <img src="images/components/brass_ingot.png" width="24" height="24" align="absmiddle"> Brass ingots to <img src="images/components/empty_shell.png" width="24" height="24" align="absmiddle"> empty shells | Electric, 80 kW |
| <img src="images/components/bullet_swaging_machine.png" width="24" height="24" align="absmiddle"> Bullet swaging machine | Lead, with iron, blaze powder or <img src="images/components/depleted_uranium.png" width="24" height="24" align="absmiddle"> depleted uranium, to <img src="images/components/bullet_regular.png" width="24" height="24" align="absmiddle"> bullets of four kinds | Electric |
| <img src="images/components/ammunition_assembly.png" width="24" height="24" align="absmiddle"> Ammunition assembly | An <img src="images/components/empty_shell.png" width="24" height="24" align="absmiddle"> empty shell, a <img src="images/components/bullet_regular.png" width="24" height="24" align="absmiddle"> bullet and <img src="images/components/high_grade_gunpowder.png" width="24" height="24" align="absmiddle"> gunpowder to a <img src="images/components/round_regular.png" width="24" height="24" align="absmiddle"> round | Electric |
| <img src="images/components/metal_lathe.png" width="24" height="24" align="absmiddle"> Metal lathe | Iron and <img src="images/components/purified_iron.png" width="24" height="24" align="absmiddle"> purified iron to a <img src="images/components/precision_barrel.png" width="24" height="24" align="absmiddle"> precision barrel, and amethyst with <img src="images/components/high_purity_graphite.png" width="24" height="24" align="absmiddle"> high-purity graphite to <img src="images/components/optical_lens.png" width="24" height="24" align="absmiddle"> optical lenses | Electric |
| <img src="images/components/component_assembly.png" width="24" height="24" align="absmiddle"> Mechanical component assembly | The <img src="images/components/steel_frame.png" width="24" height="24" align="absmiddle"> steel frame, <img src="images/components/turret_mechanism.png" width="24" height="24" align="absmiddle"> turret mechanism, <img src="images/components/ammunition_feed.png" width="24" height="24" align="absmiddle"> ammunition feed and <img src="images/components/targeting_unit.png" width="24" height="24" align="absmiddle"> targeting unit, and two <img src="images/components/circuit_board.png" width="24" height="24" align="absmiddle"> circuit boards with glowstone and <img src="images/components/brass_ingot.png" width="24" height="24" align="absmiddle"> brass to a <img src="images/components/laser_emitter.png" width="24" height="24" align="absmiddle"> laser emitter, and the upgrade kits <img src="images/components/turret_upgrade_2.png" width="24" height="24" align="absmiddle"> II and <img src="images/components/turret_upgrade_3.png" width="24" height="24" align="absmiddle"> III | Electric, 40 kW |
| <img src="images/components/heavy_weapons_assembly.png" width="24" height="24" align="absmiddle"> Heavy weapons assembly | The six turret parts and <img src="images/components/brass_ingot.png" width="24" height="24" align="absmiddle"> brass to a <img src="images/components/machine_gun_turret.png" width="24" height="24" align="absmiddle"> machine gun turret, or, with four optical lenses and two laser emitters, a <img src="images/components/laser_turret.png" width="24" height="24" align="absmiddle"> laser turret, and the upgrade kits <img src="images/components/turret_upgrade_4.png" width="24" height="24" align="absmiddle"> IV and <img src="images/components/turret_upgrade_5.png" width="24" height="24" align="absmiddle"> V | Electric, 250 kW |

Each machine has its own screen. Put items in the input slots, take them from the output slots, and keep fuel in the
fuel slot where a machine asks for it. Machines only work in loaded chunks. Each machine stands on a barrel that holds its items, and hoppers can feed it and take its products. See [MECHANICS.md](MECHANICS.md#lab-tools-hoppers-and-barrels).

## 9. Petroleum and drills

<img src="images/components/petroleum_drill_3.png" width="48" height="48" align="absmiddle">

Crude petroleum is a second resource. Its deposits are patches of the `crude_petroleum_deposit` ore in the stone, planned and
laid like the solid ores, each with a remaining amount. A petroleum drill draws a deposit down, one petroleum for each unit
it takes, until the deposit is empty.

Two kinds of drill work a square below themselves, and each kind has five levels. The levels are set in `DrillTable`
in the code. `processes.yml` names each drill's kind and level, and `materials.yml` gives its name and host block.

| Level | Square | Depth (layers) | Cycle (ticks) | Items per minute at full power |
|---|---|---|---|---|
| 1 | 10 by 10 | 48 | 600 | 60 |
| 2 | 14 by 14 | 96 | 480 | 90 |
| 3 | 20 by 20 | 144 | 360 | 150 |
| 4 | 28 by 28 | 200 | 240 | 240 |
| 5 | 40 by 40 | 280 | 120 | 360 |

The rates are set by `drill-rates` in `config.yml`: 1.0, 1.5, 2.5, 4.0 and 6.0 items per second for levels 1 to 5. The
square sets which deposits a drill finds. The depth is shown on the drill's info item, but it does not limit anything. The
cycle no longer sets the output. It counts the fuel use while a drill runs, and it sets the menu's progress bar.

- **Mining:** a drill works one deposit at a time. It keeps its current deposit while that has something left, and otherwise
  moves to the deposit in its square with the most left. Each unit taken from a deposit is one item, and a full output stops
  the drill.
- **Petroleum drills** run on the strongest of a solar panel (its power over 15), redstone (its power over 15) or charcoal (0.35).
  Charcoal burns one item every 8 cycles, and only while charcoal is the source of power.
- **Ore drills** run on petroleum. Efficiency is 1 while petroleum is in the fuel slot, and one petroleum burns every
  2 cycles while the drill runs. They mine the solid ore deposits in their square.

The chain runs from deposit to petroleum, from petroleum to the fuel of the ore drills, and from the ore drills to the
raw minerals that start the process chain above.

## 10. Ore blocks and the scanner

Each special ore has a placeable block item: the host block, named after the ore. Placing it registers the block as that
ore. Mining a placed ore block gives the raw mineral, and silk touch gives the block item back.

A patch block that the world generator laid is not registered. Breaking one by hand gives its ordinary drop (glazed
terracotta drops itself, and coal ore drops coal), not raw ore. Drills are the only source of raw minerals from a deposit.

The Ore Density Scanner can still be crafted after Laboratory Basics, but it does nothing. Right-clicking it shows no map
and no summary.

## 11. Brass, gunpowder and ammunition

A second chain starts from the new ores. <img src="images/components/raw_saltpeter.png" width="24" height="24" align="absmiddle"> Saltpeter becomes <img src="images/components/high_purity_potassium_nitrate.png" width="24" height="24" align="absmiddle"> high-purity potassium nitrate, the oxidiser of the powder. <img src="images/components/sulfur.png" width="24" height="24" align="absmiddle"> Sulfur and <img src="images/components/charcoal.png" width="24" height="24" align="absmiddle"> charcoal are the other two ingredients, and charcoal comes from coal. Zinc and copper make <img src="images/components/brass_ingot.png" width="24" height="24" align="absmiddle"> brass. Brass gives the cartridge cases and the mechanisms of the turret. Lead, with a second metal or a special material, makes the <img src="images/components/bullet_regular.png" width="24" height="24" align="absmiddle"> bullets, and a bullet, a <img src="images/components/empty_shell.png" width="24" height="24" align="absmiddle"> shell and <img src="images/components/high_grade_gunpowder.png" width="24" height="24" align="absmiddle"> gunpowder make a <img src="images/components/round_regular.png" width="24" height="24" align="absmiddle"> round. The steps and the machines are in [MECHANICS.md](MECHANICS.md#chemistry-and-ammunition), and the order a player follows is in the player guide, [Ammunition and the machine gun turret](PLAYER_GUIDE.md#ammunition-and-the-machine-gun-turret).

The four rounds differ in what they do. <img src="images/components/round_regular.png" width="24" height="24" align="absmiddle"> Regular is the baseline. <img src="images/components/round_armor_piercing.png" width="24" height="24" align="absmiddle"> Armour-piercing ignores part of the target's armour. <img src="images/components/round_incendiary.png" width="24" height="24" align="absmiddle"> Incendiary sets the target alight. <img src="images/components/round_depleted_uranium.png" width="24" height="24" align="absmiddle"> Depleted uranium ignores some armour, and its spread is tighter. A round adds no damage of its own: every round hits for the damage of its turret's level.

## 12. The machine gun turret

The <img src="images/components/machine_gun_turret.png" width="24" height="24" align="absmiddle"> turret is the end of the chain: it is made in the <img src="images/components/heavy_weapons_assembly.png" width="24" height="24" align="absmiddle"> heavy weapons assembly from six parts, and it fires at hostile monsters
in its range. It needs power, and it takes rounds by hand or by belt. It has five levels, and the upgrade kits in section 14 take it up one level at a time. Its settings are the `turrets` section of `config.yml`, and
its rules are in [MECHANICS.md](MECHANICS.md#machine-gun-turret).

## 13. The laser turret

The <img src="images/components/laser_turret.png" width="24" height="24" align="absmiddle"> laser turret is the other turret, and it needs no ammunition. It fires a beam, not rounds, and it reaches 40 blocks against the machine gun's 24. It is made in the <img src="images/components/heavy_weapons_assembly.png" width="24" height="24" align="absmiddle"> heavy weapons assembly from a steel frame, a targeting unit and a turret mechanism, with four <img src="images/components/optical_lens.png" width="24" height="24" align="absmiddle"> optical lenses and two <img src="images/components/laser_emitter.png" width="24" height="24" align="absmiddle"> laser emitters. The lenses come from the metal lathe, from amethyst and high-purity graphite. The emitters come from the component assembly, from circuit boards, glowstone dust and brass. The turret draws 60 kW at level 1, and each beam does 12 damage at level 1, which armour does not reduce. It has the same five levels as the machine gun, and a level 5 laser's beam is blue. Its rules are in [MECHANICS.md](MECHANICS.md#laser-turret), and its numbers are the `laser-turret` section of `config.yml`.

## 14. Upgrade kits

Four kits move a turret up its five levels, one level for each kit. Kits II and III are made in the <img src="images/components/component_assembly.png" width="24" height="24" align="absmiddle"> component assembly and are researched at Materials Purification. Kits IV and V are made in the <img src="images/components/heavy_weapons_assembly.png" width="24" height="24" align="absmiddle"> heavy weapons assembly and are researched at Tesseract Geometry. Kit III and the kits after it make a turret two blocks tall. The kits' recipes, their numbers and the level table are in [MECHANICS.md](MECHANICS.md#upgrading-a-turret).

