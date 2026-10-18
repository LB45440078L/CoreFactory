# Resource pack

Every TNT and component has its own texture. The textures come in a resource pack, built from the same
`tnts.yml` and `items.yml` the plugin reads, so the names always match.

## How the textures reach the screen

The plugin sets an **item model** on each of its items, named `moretntplus:tnt/<id>` or
`moretntplus:component/<id>`. The resource pack defines what that name looks like:

| Pack file | Role |
|---|---|
| `assets/moretntplus/items/tnt/<id>.json` | Item definition: points the item at its model. |
| `assets/moretntplus/models/item/tnt/<id>.json` | Cube model, using the texture below for every face. |
| `assets/moretntplus/textures/block/tnt/<id>.png` | The 16×16 texture. |
| `pack.mcmeta` | Pack format and description. |
| `pack.png` | Pack icon. |

Items use that model in the inventory, in the hand, and in the catalogue. A **placed** TNT block is still a vanilla
TNT block, which the client draws with the vanilla texture. To show the custom look on the ground, the plugin puts an
**item display** over each placed block and scales it slightly above 1, so it covers the vanilla texture. It is
turned off with `display.custom-textures: false`; placed TNT then looks like vanilla TNT.

## Building the pack

`mvn package` builds the zip automatically:

```
MoreTNTPlus-ResourcePack/target/MoreTNTPlus-ResourcePack-1.0.0.zip
```

Set the pack format for your server before you package. It is a Maven property, so either edit the default in
`MoreTNTPlus-ResourcePack/pom.xml` or pass it on the command line:

```bash
mvn clean package -pl MoreTNTPlus-Api,MoreTNTPlus-Core,MoreTNTPlus-Spigot,MoreTNTPlus-ResourcePack -Dpack.format=75
```

| Server | Pack format to use |
|---|---|
| 1.21.11 | Check the table at the Minecraft wiki ("Pack format"). The shipped default is `75`; verify it for your version. |
| 26.1, 26.2, 26.3 | Check the same table. These are newer formats; set the number the wiki lists for your version. |

A pack whose format does not match the server can be rejected by the client or leave the custom models unapplied.
The pack format is the one thing in this project that must be confirmed per server version.

The builder can also be run directly, from the module's `target/classes` on the classpath:

```
io.moretntplus.resourcepack.ResourcePackBuilder <tnts.yml> <items.yml> <output.zip> <pack-format> <description>
```

## Hosting the pack

Upload the zip to a URL the players can reach, then set it in `server.properties`:

```properties
resource-pack=https://example.com/MoreTNTPlus-ResourcePack-1.0.0.zip
resource-pack-sha1=<sha1 of the zip>
require-resource-pack=false
```

Compute the hash with `certutil -hashfile <zip> SHA1` on Windows or `sha1sum <zip>` elsewhere.

## Changing the look

The textures are procedural: each TNT's `texture:` block picks two colours and a pattern. Change them in `tnts.yml`
(or `items.yml` for components), rebuild the pack, and host the new zip. For hand-drawn art, replace the PNG in
`textures/block/tnt/<id>.png` of the built zip with your own 16×16 image; keep the file name.

| Key | Meaning |
|---|---|
| `base` | Main colour, hex. |
| `accent` | Motif colour, hex. |
| `pattern` | `stripes`, `dots`, `hazard`, `rings`, `checker`, `cross`, `nuclear`, `flame`, `bio`, `circuit`, `glyph`. |

The renderer adds a little noise and a darker edge so each texture reads as a block. The patterns are placeholders
meant to tell the TNT types apart at a glance; they are not commissioned art.

## Limits

- Only the item and the placed-TNT display use the custom look. The vanilla TNT block that remains beneath, and the
  block-breaking particles, use vanilla TNT.
- A player without the resource pack sees the base material (`TNT`) and the item's name and lore, but not the model.
