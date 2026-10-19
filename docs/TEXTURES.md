# Textures

Every TNT type and every crafting component has its own 16 × 16 texture. The textures are drawn by the ResourcePack
module from the `texture:` block of each entry in `tnts.yml` and `items.yml`, packed into a resource pack, and shown
in the game through an item model. This page covers what a texture must be, the tools that build and check it, the
steps to change, add or hand-draw one, and a table of every current texture.

For the pack's files, hosting and pack format, see [RESOURCE_PACK.md](RESOURCE_PACK.md). This page is about the
textures themselves.

## 1. Specification

### The output

| Item | Requirement |
|---|---|
| Size | 16 × 16 pixels. The renderer always writes this size, and Minecraft's block model expects it. |
| Format | PNG. The generated files are 32-bit ARGB, fully opaque. |
| Location in the pack | `assets/moretntplus/textures/block/tnt/<id>.png` for a TNT, `…/block/component/<id>.png` for a component. |
| Name | The entry's key in `tnts.yml` or `items.yml`. It must match the item id used by the plugin. |
| Pack icon | `pack.png`, 128 × 128, generated from the warning pattern. Replace it by hand if you want a different icon. |

### The `texture:` block

Each entry in `tnts.yml` and `items.yml` can have one `texture:` block:

```yaml
texture: {base: "#3A4A2A", accent: "#E8FF60", pattern: nuclear}
```

| Key | Default | Meaning |
|---|---|---|
| `base` | `#808080` (grey) | The main colour, 6-digit hex. |
| `accent` | `#FFFFFF` (white) | The colour of the motif that the pattern paints over the base. |
| `pattern` | `stripes` | The motif. Names are case-insensitive. |

A missing `texture:` block gives the defaults, so the item still ships with a texture. The builder writes one texture for
every entry in both files.

### Colours

- Write a colour as six hex digits, with or without the `#`: `#E8FF60` or `E8FF60`. Case does not matter.
- **Three-digit forms are not supported.** `#FFF` is read as the number `0xFFF`, which is nearly black, not white. Always
  write six digits.
- A value that cannot be read falls back to the default for that key: `base` to grey, `accent` to white.
- The renderer adds a little noise to every pixel, about ±9 per channel, and darkens the outer ring of pixels by a further
  26. That is what makes a flat colour read as a block. The noise is seeded with the entry's id, so the same id always
  gives the same texture.

### Patterns

The pattern decides which pixels take the accent colour. The base colour fills the rest.

| Pattern | What it paints |
|---|---|
| `stripes` | Diagonal bands. This is also what any unknown pattern name gives. |
| `dots` | A dot on every fourth pixel in each direction. |
| `checker` | Blocks of four by four pixels, alternating. |
| `rings` | Concentric rings around the centre. |
| `cross` | A plus sign through the centre. |
| `hazard` | Diagonal bands, three pixels wide, alternating. Used for warning markings. |
| `circuit` | Vertical lines every fourth column, and horizontal lines every fourth row with random gaps. |
| `flame` | A jagged band that rises from the bottom, the way flames do. |
| `bio` | Scattered specks, for biological or toxic items. |
| `glyph` | A small diamond outline, as a symbol. |
| `nuclear` | A central disc and six alternating sectors, the radiation sign. |

A pattern name that does not exist is not an error. It renders as `stripes` and nothing is logged, so check the spelling.

## 2. Tools

| Tool | What it does | Where |
|---|---|---|
| Maven | Builds the pack zip in the `package` phase. | `mvn package`, output `MoreTNTPlus-ResourcePack/target/MoreTNTPlus-ResourcePack-1.0.0.zip` |
| `ResourcePackBuilder` | Writes the zip from the two YAML files. It can be run alone, for a quick rebuild. | `io.moretntplus.resourcepack.ResourcePackBuilder` (usage in [RESOURCE_PACK.md](RESOURCE_PACK.md#building-the-pack)) |
| `TextureRenderer` | Draws one texture in code: `render(base, accent, pattern, seed)`. Use it to try a combination without building the pack. | `MoreTNTPlus-ResourcePack/src/main/java/io/moretntplus/resourcepack/TextureRenderer.java` |
| `TextureRendererTest` | Checks the size, colour parsing and fallback, and that the output is the same for the same seed. Run by `mvn verify`. | `MoreTNTPlus-ResourcePack/src/test/…` |
| A PNG viewer | Shows the result. Open the PNG from the zip after unzipping it. | Any viewer. |
| An image editor | Only for hand-drawn art. Use a 16 × 16 canvas, with nearest-neighbour zoom, and save as PNG. | Aseprite, GIMP, Paint.NET or similar. |

To look at the current textures without a server:

```bash
mvn -q package -pl MoreTNTPlus-Api,MoreTNTPlus-Core,MoreTNTPlus-Spigot,MoreTNTPlus-ResourcePack
unzip -o MoreTNTPlus-ResourcePack/target/MoreTNTPlus-ResourcePack-1.0.0.zip \
      "assets/moretntplus/textures/block/*" -d scratch/textures
```

The PNGs are then in `scratch/textures/assets/moretntplus/textures/block/tnt/` and `…/component/`. Open one of them to
check it.

To see the item in the game, you need the resource pack loaded on your client (see the hosting steps in
[RESOURCE_PACK.md](RESOURCE_PACK.md#hosting-the-pack)), then `/mtnt give <id>` for a TNT or a component. `/mtnt list`
prints every id.

## 3. Processes

### Change the colours or pattern of an existing item

1. Edit the `texture:` block of the item in `tnts.yml` or `items.yml`.
2. Check the colours are six digits and the pattern is in the table above.
3. Rebuild the pack: `mvn package` (or the `ResourcePackBuilder` command).
4. Preview it: unzip the PNG as shown above, or call `TextureRenderer.render` with the same arguments.
5. Host the new zip, update `resource-pack-sha1` in `server.properties`, and restart the server. Players must re-accept
   the pack, or relog, to see the change.

### Add a texture for a new TNT or component

1. Add the entry to `tnts.yml` (or `items.yml`) with a `texture:` block, as shown in section 1. The key you choose is the
   item id; the plugin sets the item model to `moretntplus:tnt/<id>` (or `moretntplus:component/<id>`) on every item it
   creates, so the texture appears on its own.
2. Rebuild the pack, and check the PNG exists in the zip under the same name.
3. If the item is a TNT that should look custom on the ground, nothing more is needed: the placed display uses the same
   model. Confirm `display.custom-textures` is `true` in `config.yml`.

Do not rename a key once it is in use. The key is the item id: items already in players' hands stop being recognised, and
their item model no longer exists in the pack.

### Use hand-drawn art

Hand-drawn PNGs are supported by the pack format, but the pack is rebuilt from the YAML every time, and there is no
override folder yet. Until there is one:

1. Draw the texture as a 16 × 16 PNG, and keep its name, such as `tnt/nuke_davy_crockett.png`.
2. Build the pack, then replace the matching PNG inside the zip (zip tools such as 7-Zip can do this).
3. Keep your art in a folder of your own, and copy it into the zip after each rebuild. Each `mvn package` overwrites
   the zip.

The model is a plain cube with the texture on every face, so an image that is only meant for one face will repeat on
all six.

### Check it in the game

| Symptom | Likely cause |
|---|---|
| The item looks like plain TNT or a plain material | The pack is not applied: it was rejected, the pack format does not match the server, or the client has not accepted it. |
| Pink and black checkerboard | The item model names a texture the pack does not contain. Usually a typo in the id, or a rebuilt pack that lost a file. |
| The item shows, but the ground block is vanilla TNT | `display.custom-textures` is `false`, or the display was not spawned. See [MECHANICS.md](MECHANICS.md). |
| Colours look wrong | A three-digit hex, or a value that is not hex and fell back to the default. Use six digits. |

## 4. Every texture

These tables are generated from `tnts.yml` and `items.yml`, so they match the shipped configuration. The `id` column is
the file name in the pack.

### TNT types

| id | Name | Base | Accent | Pattern |
|---|---|---|---|---|
| `chain_reaction_tnt` | Chain Reaction TNT | `#B5362A` | `#FFB347` | stripes |
| `shaped_charge_tnt` | Shaped Charge TNT | `#C9A227` | `#3A3010` | rings |
| `gravity_well_tnt` | Gravity Well TNT | `#2A1442` | `#B57BFF` | rings |
| `implosion_tnt` | Implosion TNT | `#1A1A1E` | `#5C5C66` | checker |
| `fuse_linked_tnt` | Fuse-Linked TNT | `#8C1D1D` | `#FF5555` | circuit |
| `delayed_cascade_tnt` | Cluster Cascade TNT | `#4A4F55` | `#D0D6DC` | dots |
| `four_dimensional_tnt` | 4-Dimensional TNT | `#2A0A3A` | `#E070FF` | glyph |
| `vitrify_tnt` | Vitrify TNT | `#A8E8F0` | `#2A1A40` | dots |
| `liquefy_tnt` | Liquefy TNT | `#5A1A00` | `#FF8A1A` | flame |
| `terraform_tnt` | Terraform TNT | `#6B4A2A` | `#3E7A2A` | stripes |
| `randomize_tnt` | Randomize TNT | `#505050` | `#E040E0` | checker |
| `sculpt_tnt` | Sculpt TNT | `#2A6E8A` | `#A8E0FF` | rings |
| `concussion_tnt` | Concussion TNT | `#D8C84A` | `#3A3A10` | dots |
| `launch_tnt` | Launch TNT | `#3AAA6A` | `#D8FFE8` | stripes |
| `teleport_tnt` | Teleport TNT | `#1A0A2E` | `#8A4DFF` | glyph |
| `gravity_flip_tnt` | Gravity-Flip TNT | `#1E4A6A` | `#E8F8FF` | rings |
| `conditional_tnt` | Conditional TNT | `#3A3A3A` | `#FF3A3A` | circuit |
| `proximity_tnt` | Proximity TNT | `#4A0A0A` | `#FFD000` | glyph |
| `pulse_tnt` | Pulse TNT | `#8A1A1A` | `#FF6060` | circuit |
| `mirror_tnt` | Mirror TNT | `#C8E8F0` | `#3A6A7A` | checker |
| `shockwave_tnt` | Shockwave TNT | `#2A2A3A` | `#FFD8A0` | rings |
| `debris_tnt` | Debris TNT | `#5A5A5A` | `#9A8A6A` | dots |
| `echo_tnt` | Echo TNT | `#3A1A5A` | `#D0A0FF` | rings |
| `silent_tnt` | Silent TNT | `#262626` | `#4A4A4A` | checker |
| `harvest_tnt` | Harvest TNT | `#2A6A3A` | `#E8D050` | stripes |
| `sorting_tnt` | Sorting TNT | `#34507A` | `#A0C8FF` | checker |
| `marker_tnt` | Marker TNT | `#1E3A5A` | `#55FFFF` | cross |
| `anchor_tnt` | Anchor TNT | `#2A1A50` | `#9AF0FF` | cross |
| `biological_tnt` | Biological TNT | `#2A5A1A` | `#9AFF40` | bio |
| `napalm_tnt` | Napalm TNT | `#8A2A0A` | `#FFB020` | flame |
| `nuke_davy_crockett` | Davy Crockett Warhead | `#3A4A2A` | `#E8FF60` | nuclear |
| `nuke_little_boy` | Little Boy | `#3A4A2A` | `#E8FF60` | nuclear |
| `nuke_fat_man` | Fat Man | `#4A4A2A` | `#FFE860` | nuclear |
| `nuke_joe_1` | Joe-1 (RDS-1) | `#4A3A20` | `#FFD060` | nuclear |
| `nuke_ivy_king` | Ivy King | `#5A3A18` | `#FFA030` | nuclear |
| `nuke_b83` | B83 Gravity Bomb | `#5A2A18` | `#FF8040` | nuclear |
| `nuke_ivy_mike` | Ivy Mike | `#6A2A1A` | `#FF6030` | nuclear |
| `nuke_castle_bravo` | Castle Bravo | `#6A1A1A` | `#FF4A20` | nuclear |
| `nuke_mk17` | Mk-17 | `#5A0A0A` | `#FF3A3A` | nuclear |
| `nuke_tsar_bomba` | Tsar Bomba | `#2A0A0A` | `#FF2A2A` | nuclear |

### Components

| id | Name | Base | Accent | Pattern |
|---|---|---|---|---|
| `fuse_linker` | Fuse Linker | `#3A3F4A` | `#E0302A` | circuit |
| `fuse_wire` | Fuse Wire | `#C07040` | `#FFB070` | stripes |
| `shaped_charge_lens` | Shaped Charge Lens | `#A8E0F0` | `#2F7F9F` | rings |
| `gravity_core` | Gravity Core | `#1E1030` | `#9A4DFF` | rings |
| `vitrifier_flux` | Vitrifier Flux | `#E8D8A0` | `#FFFFFF` | dots |
| `sorting_magnet` | Sorting Magnet | `#505A70` | `#A0B0D0` | checker |
| `anchor_crystal` | Anchor Crystal | `#7B5CC4` | `#D8C8FF` | cross |
| `pulse_capacitor` | Pulse Capacitor | `#7A1010` | `#FF4040` | stripes |
| `quantum_detonator` | Quantum Detonator | `#222222` | `#FFD000` | glyph |
| `biohazard_culture` | Biohazard Culture | `#3B7A2A` | `#A8FF60` | bio |
| `napalm_gel` | Napalm Gel | `#B84A10` | `#FFB030` | flame |
| `anchor_plate` | Anchor Plate | `#9AA0A8` | `#D0D4DA` | checker |
| `enriched_uranium` | Enriched Uranium | `#4E5A3A` | `#A6FF3A` | hazard |
| `plutonium_pit` | Plutonium Pit | `#5A3A70` | `#E8A0FF` | rings |
| `tritium_booster` | Tritium Booster | `#1E6A70` | `#7FFFE0` | dots |
| `neutron_initiator` | Neutron Initiator | `#6A4A9A` | `#FFE070` | cross |
| `nuclear_casing` | Nuclear Casing | `#2E3238` | `#FFB020` | hazard |
| `thermonuclear_core` | Thermonuclear Core | `#7A1020` | `#FF6A6A` | rings |
