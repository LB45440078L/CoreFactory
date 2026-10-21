# API

CoreFactory can be extended by another plugin, or by a mechanic of your own, through the **Api** module. It has no
implementation, only contracts, and it depends on Spigot-API alone.

## Adding the dependency

The Api is published from this build as `io.corefactory:CoreFactory-Api`. Until you publish it, install it locally:

```bash
mvn install -pl CoreFactory-Api
```

```xml
<dependency>
  <groupId>io.corefactory</groupId>
  <artifactId>CoreFactory-Api</artifactId>
  <version>1.0.0</version>
  <scope>provided</scope>
</dependency>
```

Declare `CoreFactory` as a dependency (`depend:` or `softdepend:`) of your plugin, so it loads first.

## Getting the service facade

```java
CoreFactoryApi api = CoreFactoryProvider.get();   // null until CoreFactory has enabled
if (api == null) return;
```

From a mechanic, use `context.api()`.

## What the facade gives you

| Method | Use it to |
|---|---|
| `definitions()`, `definition(id)` | Read the loaded TNT types. |
| `mechanics()`, `mechanic(id)` | Read the registered mechanics. |
| `placedAt(location)`, `placedTnts()` | Find placed custom TNT and its state (links, target, direction). |
| `tntIdOf(item)` | Identify a TNT item. |
| `createTntItem(id, amount)`, `createComponentItem(id, amount)` | Create items. |
| `submitBlockChanges(world, set, onComplete)` | Change blocks through the staged pipeline. |
| `submitBlockGenerator(world, generator, onComplete)` | Stream very large changes. |
| `ignite(placed, source)` | Light a placed TNT as if a player had. |
| `detonate(id, origin, source, direction, depth)` | Run a detonation directly. |
| `scheduleLater(ticks, task)` | Run on the main thread later. |
| `clientEffects()` | Per-player clock, weather and block views (`setClientTime`, `resetClientTime`, `setClientWeather`, `showBlocks`, `restoreBlocks`). |
| `protection()` | Ask whether a block may be changed. |
| `currentTick()`, `plugin()` | The tick counter and your scheduling owner. |

Always call these on the main thread.

## Writing a mechanic

A mechanic is a class that implements `io.corefactory.api.Mechanic`:

```java
public final class HealingBurst implements Mechanic {

    @Override public String id() { return "healing_burst"; }

    @Override public String summary() { return "Heals every player in range instead of hurting them."; }

    @Override
    public void detonate(DetonationContext ctx) {
        double radius = ctx.tnt().radius();
        double amount = ctx.settings().getDouble("heal", 4.0);
        for (Entity entity : ctx.world().getNearbyEntities(ctx.origin(), radius, radius, radius)) {
            if (entity instanceof Player player) {
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + amount));
            }
        }
    }
}
```

Register it from your plugin's `onEnable`, after CoreFactory has enabled (declare `CoreFactory` in your `depend` or
`softdepend`):

```java
CoreFactoryApi api = CoreFactoryProvider.get();
if (api != null) {
    api.registerMechanic(new HealingBurst());
}
```

`registerMechanic` adds the mechanic, or replaces the built-in mechanic with the same id, and re-reads the TNT
configuration, so a TNT type that names it becomes usable at once and its recipe is registered. The same happens on
`/cf reload`. Give a TNT type that names it in `tnts.yml`:

```yaml
healing_tnt:
  name: "&aHealing TNT"
  material: TNT
  fuse-ticks: 60
  radius: 8
  mechanic: healing_burst
  settings:
    heal: 6
```

### Rules for a mechanic

- Keep it **stateless**. All per-type numbers come from `ctx.settings()`.
- Change blocks only through `submitBlockChanges` or `submitBlockGenerator`. That keeps protection, staging and the
  placed-TNT registry consistent.
- Respect `ctx.api().protection().isProtected(block)` before writing a block, and skip TNT blocks you do not own.
- Do long work across ticks (`scheduleLater`, or a `BlockGenerator`), never in one tick.
- For ignition behaviour, override `ignitionMode()`:
  - `PRIMED_ENTITY` (default): the vanilla-style fuse.
  - `DELAYED`: no entity; runs after the fuse.
  - `PERSISTENT`: the block stays and the mechanic runs at once.
  - `INERT`: lighting does nothing.
- For periodic work on placed blocks, return an interval from `placedTickInterval()` and implement
  `onPlacedTick`. Use multiples of 5 ticks.
- For warnings at ignition, implement `onIgnite`.
- For items dropped by a player, implement `onDropped` (napalm uses this to burst on landing).

### Block changes

```java
BlockChangeSet set = new BlockChangeSet();
set.set(x, y, z, Material.AIR.createBlockData());
ctx.api().submitBlockChanges(ctx.world(), set, () -> { /* done */ });
```

For a very large change, implement `BlockGenerator`: its `next(out, limit)` appends at most `limit` writes and returns
`true` while more remain. The core calls it once per tick.

## Events

Both are cancellable Bukkit events in `io.corefactory.api.event`:

| Event | Fired | Use it to |
|---|---|---|
| `TntIgniteEvent` | Before a placed TNT is lit. Cancelling leaves it untouched. | Protect areas, log ignitions, change the source. |
| `TntDetonateEvent` | Before a mechanic runs. Cancelling makes it a dud. | Veto or log detonations; inspect the origin and depth. |

```java
@EventHandler
public void onDetonate(TntDetonateEvent event) {
    if (event.getOrigin().getWorld().getName().equals("lobby")) {
        event.setCancelled(true);
    }
}
```

## Extension points inside the plugin

The Api module is the stable surface. The classes below are inside the plugin, in Core and Spigot. A developer who
works on the plugin or forks it can use them. They may change between releases, so an addon should not depend on them.

| Type | Module | What it does |
|---|---|---|
| `Visuals` | Core, `engine` | A functional interface with one method, `play(String effect, Location at, double scale)`. The core asks for a named effect and the platform draws it. `Visuals.NONE` shows nothing. The Spigot plugin supplies `SpriteEffects`, which implements it, with `CoreFactoryCore.setVisuals`. |
| `VisualCatalog` | Core, `engine` | The 22 named effects, their shapes and sprites. `VisualCatalog.find(name)` returns one, and `VisualCatalog.placements(effect, scale)` lays out its sprites. A new effect is one entry in `EFFECTS`, plus its sprites in `SPRITES`. |
| `ProcessCatalog` | Core, `process` | The lab tool kinds and processes of `processes.yml`. `ProcessCatalog.parse(ConfigurationSection, Logger)` reads them. Query with `machines()`, `machine(id)`, `process(id)`, `processesFor(machineId)`, `acceptsInput(machineId, ItemRef)` and `inputsOf(machineId)`. |
| `DrillCatalog` | Core, `drill` | The drills of `processes.yml` (`drills:`). `DrillCatalog.parse(ConfigurationSection, Map<String, ComponentDefinition>, Logger)` reads them. Query with `drills()`, `drill(id)` and `machine(id)`. `DrillTable.level(n)` gives a level's area, depth and cycle. |
| `HologramService` | Spigot, `hologram` | Creates and removes the progress holograms. `create(Location, List<String>)` returns an `Optional<Hologram>` with one line per text, top first. `remove(Hologram)` removes one, `spotAbove(Location)` gives the point 1.6 blocks above a block, and `enabled()` reports the `holograms.enabled` setting. `Hologram.setLines(List<String>)` changes the text of the lines, with `&` colour codes. |
| `Database` and `StorageSettings` | Storage | `Database.open(StorageSettings, Logger)` opens the pool and runs the migrations. `query(SqlFunction)` and `execute(SqlConsumer)` run work on a pooled connection, and `close()` releases the pool. `StorageSettings` is the record of the `storage:` section. |

Two rules hold for each of these. The catalogs, the visuals and the holograms are used on the main thread, as the Api is.
The storage calls run on the background thread that the research store uses, so a `Database` call must not block the main thread.
Do not write to the registries or the data files directly. Use the catalog's own parse and query methods, so the checks
that the plugin runs on start still apply.

For a new effect, a developer adds an entry to `VisualCatalog.EFFECTS`, and a test in `VisualCatalogTest` checks its
sprite count. For a new drill level, the code in `DrillTable` is the only place that holds the numbers: the drill's
`processes.yml` entry names its kind and level, and `DrillTable` gives the rest.

## Stability

The Api module is what addons should depend on. `CoreFactoryCore`, the `engine` and `mechanics` packages, and the
Spigot module are implementation: they may change between releases. Every Api type is a `public` contract with
Javadoc; the record components of `TntDefinition` and `DetonationContext` are part of it.

The power network and the logistics devices are not part of the Api. The facade has no method for pylons, accumulators,
generators, wires, belts, pickers or splitters. The pure models (`io.corefactory.core.energy` and
`io.corefactory.core.logistics`) and the Bukkit side (`io.corefactory.spigot.energy` and `io.corefactory.spigot.logistics`)
are internal to the plugin, and they can change between releases.
