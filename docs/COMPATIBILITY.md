# Compatibility

## Supported targets

| Target | Maven profile | Compile | Enable test on a Paper server |
|---|---|---|---|
| Spigot-API 26.1 | `api-26.1` (default) | Passed | Passed on Paper 26.1.2 |
| Spigot-API 26.2 | `api-26.2` | Passed | Passed on Paper 26.2 |
| Spigot-API 26.3 | `api-26.3` | Passed | Passed on Paper 26.3 |
| Spigot-API 1.21.11 | `api-1.21.11` | Passed | Passed on Paper 1.21.11 |

**Hard requirements, all targets:**

- **Java 25** on the server. The bytecode targets release 25, so a server on Java 21 will refuse the plugin. Each
  of the four targets runs on Java 25, which is what was tested.
- **PacketEvents 2.14.0 or newer** (plugin name `packetevents`). The plugin disables itself without it.
- **WorldEdit 7.4.x or FastAsyncWorldEdit** (either; you do not need both). The plugin disables itself without one.
  FastAsyncWorldEdit must be a build made for your exact server version: FAWE 2.8.1 on Hangar stops at Minecraft 1.20.2
  and fails on newer servers. The plugin checks the editor at startup and logs whether it works. If it does not, it logs
  a SEVERE message and switches to slower Bukkit block writes, so blasts still work but more slowly.

The plugin uses the `api-version: '1.21'` marker, the lowest API level it relies on, so every server above it accepts
the plugin without the old-version warnings.

## What was verified, and how

- **Compilation** against each of the four Spigot-API versions, with WorldEdit 7.4.6 and PacketEvents 2.14.0, using
  Maven's profile for that target.
- **Unit tests** (16 tests across three modules) passed.
- **Runtime enable** on four Paper builds, 26.1.2, 26.2, 26.3 and 1.21.11, with WorldEdit 7.4.6 and PacketEvents
  2.14.0 installed. On each: the plugin enabled, registered all 58 crafting recipes (40 TNT and 18 components),
  loaded 40 TNT types and 18 components, and `/mtnt` ran its commands (`list`, `count`, `reload`, `help`, and the
  error messages for missing players and targets). Runs on 26.1.2 also covered disabling, which saved and shut down
  cleanly.
- The 26.2, 26.3 and 1.21.11 enable runs used the default-profile jar, built against Spigot-API 26.1; the per-target
  jars were compiled separately and are not the ones that ran.

**Not verified:**

- Gameplay, in a graphical client: none has connected to a test server. The catalogue GUI, the displays of placed TNT,
  the fuse linker, the napalm landing and the resource pack have not been seen on a real client. A headless client
  (mineflayer) has placed, lit and detonated TNT on Paper 26.1 and read back the client-side clock, rain and beacon
  blocks. Treat the rest as untested until you have played them.
- Live WorldEdit and PacketEvents calls were not exercised: only their enable and the plugin's startup path.
- The resource pack's pack format numbers: see [RESOURCE_PACK.md](RESOURCE_PACK.md#building-the-pack).
- WorldGuard runtime: the integration compiles against the WorldGuard 7.0.20 API, but no WorldGuard server jar could be
  downloaded for testing, so it has not been run on a server. GriefPrevention 16.18.4 was run: the guard attached and the
  plugin enabled with no warnings.
- Beacon beams and the viewer's sky: on a headless client (mineflayer, Paper 26.1), the beacons, their iron platforms,
  the sky light blocks, the noon hold, the slide back to the real time, the rain removal and their restoration were
  all read back from the client. The beam condition (a pyramid, then a clear path to the sky) comes from the Minecraft
  code, decompiled from 26.3. No graphical client has confirmed that the beams draw; a previous version of this
  feature sent the beacons without the pyramid and was never seen to draw.
- Lighting and blasts were exercised with a headless player (mineflayer, which supports Minecraft 26.1) on a Paper 26.1
  server, and direct calls on a Spigot 26.3 server. Flint and steel, a fire charge and redstone were tested on 26.1; a
  real client on a Spigot 26.3 server has not been used. Several types were not tested through a player: napalm needs a
  thrown item, and fuse-linked, anchor and marker are persistent or inert by design.

## Known limitations

These are real, and are part of the design unless noted.

| Area | Limitation |
|---|---|
| Claims | WorldGuard `tnt` flags and GriefPrevention claims are respected by blasts (see [MECHANICS.md](MECHANICS.md#claims-and-integrations)). Other region systems are not. |
| Placed-TNT visuals | The placed block is a vanilla TNT block; its custom look is an item display over it. Without the display (`custom-textures: false`) it looks like vanilla TNT. |
| Pistons | Pistons cannot move a placed custom TNT (when `protect-from-pistons` is on). They can still move other blocks around it. |
| Gravity flip | An approximation: players levitate, mobs have gravity switched off. Real inverted gravity is not possible on the server side. |
| Teleport | Moves entities only. Blocks are never moved. |
| Pulse | The redstone shell is placed with physics on, one block at a time, capped at 4 000 blocks, so that neighbours are powered. Expect a short burst of updates. |
| Beams | The nuclear beams are client-side beacon blocks sent with PacketEvents. Each beacon stands on a client-side iron platform, because a client draws a beam only from a beacon with a pyramid. Each column also gets a light block at the top of the world, because a beam stops at the first solid block above it. Nothing is placed in the world. |
| Viewer's clock | The clock hold is applied to the server's periodic time packets for that player, so mods or other plugins that rewrite time packets can fight it. |
| Napalm | Only a **thrown** napalm (an item the player drops) bursts on landing. A placed napalm bursts in place. |
| Debris | Falling blocks land and can be collected by breaking them; they are not picked up as items. |
| Fire | Fire blocks follow the normal rules: they burn out, and they can spread where the game lets them. |
| Nuclear | TNT inside the radius is not ignited; a nuclear blast leaves it in place or, inside the crater, leaves it standing. |
| Nuclear crater | Streamed over several ticks. A sphere cut reads columns down to bedrock, and the largest tier (Tsar Bomba) writes about two million blocks, estimated and not timed; on a large or unloaded area, the first pass loads chunks. On the test server the hole's centre column was cut within four seconds of the blast; the whole cut was not timed. Allow a few seconds on a strong host and longer on a weak one. |
| Chunks | Placed TNT and its display persist with their chunk. A display whose id is no longer in the registry is removed on the next enable. |
| GriefPrevention | If its claim method changes in a future release, the guard logs one warning and blasts skip the claim check until the next restart. |
| Recipes | Recipe ingredients match custom items by identity. A player who received a component before a config change keeps the old item only if its id still exists. |
| Textures | The generated textures are placeholders that tell the types apart, not commissioned art. Replace them as described in RESOURCE_PACK.md. |
| Concurrency | Everything runs on the main thread. There is no async block work. |

## Changing the supported set

To add a version: add a profile in the parent POM with its `spigot.api.version`, build it, and run the enable test
on that server. To drop one, remove its profile. The source tree does not branch by version; if a future API breaks
a call, the compile step names the call.
