# API

MoreTNT+ can be extended by another plugin, or by a mechanic of your own, through the **Api** module. It has no
implementation, only contracts, and it depends on Spigot-API alone.

## Adding the dependency

The Api is published from this build as `io.moretntplus:MoreTNTPlus-Api`. Until you publish it, install it locally:

```bash
mvn install -pl MoreTNTPlus-Api
```

```xml
<dependency>
  <groupId>io.moretntplus</groupId>
  <artifactId>MoreTNTPlus-Api</artifactId>
  <version>1.0.0</version>
  <scope>provided</scope>
</dependency>
```

Declare `MoreTNTPlus` as a dependency (`depend:` or `softdepend:`) of your plugin, so it loads first.

## Getting the service facade

```java
MoreTntPlusApi api = MoreTntPlusProvider.get();   // null until MoreTNT+ has enabled
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

A mechanic is a class that implements `io.moretntplus.api.Mechanic`:

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

Register it from your plugin's `onEnable`, after MoreTNT+ has enabled (declare `MoreTNTPlus` in your `depend` or
`softdepend`):

```java
MoreTntPlusApi api = MoreTntPlusProvider.get();
if (api != null) {
    api.registerMechanic(new HealingBurst());
}
```

`registerMechanic` adds the mechanic, or replaces the built-in mechanic with the same id, and re-reads the TNT
configuration, so a TNT type that names it becomes usable at once and its recipe is registered. The same happens on
`/mtnt reload`. Give a TNT type that names it in `tnts.yml`:

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

Both are cancellable Bukkit events in `io.moretntplus.api.event`:

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

## Stability

The Api module is what addons should depend on. `MoreTntPlusCore`, the `engine` and `mechanics` packages, and the
Spigot module are implementation: they may change between releases. Every Api type is a `public` contract with
Javadoc; the record components of `TntDefinition` and `DetonationContext` are part of it.
