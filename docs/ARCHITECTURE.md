# Architecture

## Modules and their dependencies

```
MoreTNTPlus (parent pom, profiles per Spigot-API version)
├── MoreTNTPlus-Api          Spigot-API only
│     public contracts: Mechanic, TntDefinition, MechanicSettings, DetonationContext,
│     PlacedTnt, BlockChangeSet, BlockGenerator, BlockEditor, ClientEffects,
│     ProtectionService, MoreTntPlusApi, MoreTntPlusProvider, TntIgniteEvent, TntDetonateEvent
├── MoreTNTPlus-Core         Api + Spigot-API
│     MoreTntPlusCore (the implementation of MoreTntPlusApi), TntManager, Blast, BlockJobs,
│     Effects, Explosions, Shapes, Tasks, ProtectionImpl, PlacedTntStore, PlacedDisplays,
│     ItemFactory, RecipeRegistrar, DefinitionParser, CoreSettings, Messages,
│     mechanics/* (every built-in mechanic)
├── MoreTNTPlus-Spigot       Api + Core + Spigot-API + WorldEdit (provided) + PacketEvents (provided)
│     the plugin jar. Shades Api and Core. Holds plugin.yml, the three config files,
│     WorldEditBlockEditor, PacketClientEffects, listeners, /mtnt, the catalogue GUI.
└── MoreTNTPlus-ResourcePack Build-time only. snakeyaml. Draws textures; writes the pack zip.
```

The rule that keeps this clean: **Api has no implementation, Core has no WorldEdit or PacketEvents, and only Spigot
touches those two libraries.** Core reaches the block pipeline through `BlockEditor` and the client through
`ClientEffects`, both defined in Api and implemented in Spigot. That is why a future port to another editor needs
one new class, not a change to any mechanic.

## Runtime composition

`MoreTntPlusPlugin.onEnable`:

1. Checks that `packetevents` and one of `WorldEdit` / `FastAsyncWorldEdit` are loaded; disables itself otherwise.
2. Creates `WorldEditBlockEditor` (block batches) and `PacketClientEffects` (per-player time and block views).
3. Creates `MoreTntPlusCore(plugin, editor, clientEffects)`.
4. Loads `config.yml`, `tnts.yml`, `items.yml` and the placed-TNT registry; registers recipes.
5. Publishes the core through `MoreTntPlusProvider`, registers the listeners and `/mtnt`, and starts the per-tick task.

`onDisable` saves the placed-TNT registry, stops the block pipeline, and clears the provider.

## Ignition and detonation

```
flint / fire / lightning / redstone / explosion
        │
        ▼
IgnitionListener  ──►  MoreTntPlusCore.ignite(PlacedTnt, source)
                                  │
                       TntManager.ignite
                       ├─ fire TntIgniteEvent (cancellable)
                       ├─ mechanic.onIgnite(...)           (warnings, e.g. nuclear countdown)
                       └─ by IgnitionMode:
                            PRIMED_ENTITY  remove the block, spawn a tagged TNTPrimed
                            DELAYED        remove the block, schedule detonate after the fuse
                            PERSISTENT     detonate now (cooldown prevents relay loops)
                            INERT          nothing

TNTPrimed explodes ──► IgnitionListener.onEntityExplode ──► TntManager.onEntityExplode
                              ├─ our tag present: cancel vanilla explosion, remove entity,
                              │   detonate(definition, origin, source, direction, placed)
                              └─ otherwise: pull placed TNT out of the block list, ignite it later

TntManager.detonate:
   depth limit, rate limit, anchor-field check, fire TntDetonateEvent (cancellable),
   then mechanic.detonate(DetonationContext)
```

A **primed TNT** is a real `TNTPrimed` entity, so the fuse, sound and smoke are vanilla. Its identity is stored in
its persistent data (`moretntplus:primed_tnt_id`, and the direction if any). The `PlacedTnt` snapshot it came from
travels in a map keyed by the entity's id, so a teleport target or a fuse link is still known at detonation.

## The block pipeline

Mechanics never write blocks directly. They build a `BlockChangeSet` (primitive arrays of coordinates and
`BlockData`) or a `BlockGenerator` (a lazy source of such sets), and hand it to `submitBlockChanges` or
`submitBlockGenerator`.

```
mechanic ──► BlockChangeSet / BlockGenerator
                     │
              BlockJobs.submit
              ├─ size ≤ instant-threshold: write now
              └─ else: a job, advanced by up to blocks-per-tick writes each tick
                     │
              before each write: placed TNT at that position is unregistered (OverwriteHook)
                     │
              BlockEditor.apply(world, set, from, to)   ── WorldEditBlockEditor: one edit session per slice,
                                                          fast mode, closed to flush
```

A nuclear crater is a generator: it produces each column on demand, so a crater hundreds of blocks across never exists as
one object in memory.

## Placed TNT

`PlacedTntStore` maps each block position (world UUID and integer coordinates) to a `PlacedTnt`: type id,
direction, target, fuse links, owner, display id, and the proximity arming state. It is saved to
`placed-tnt.yml` periodically and on shutdown, and loaded on enable. Anchor TNT also registers its field in
`ProtectionImpl`.

Two checks keep the registry honest. Every 100 ticks, entries whose block is no longer TNT are dropped (for
example after an unmanaged removal). And the block pipeline unregisters any placed TNT it overwrites.

## Per-tick work

`MoreTntPlusCore.tick()` runs once per server tick. It resets the particle budget, runs each placed mechanic's
`onPlacedTick` at its interval (every 5 ticks it checks which are due), drops stale registry entries every 100
ticks, and saves on schedule. `BlockJobs` runs its own task only while there is work to do.

## Why it looks like this

- **One pipeline for every block write.** Staging, protection and the placed-TNT bookkeeping live in one place, so
  mechanics stay short and cannot forget any of them.
- **Mechanics are stateless.** All numbers arrive in `DetonationContext.settings()`, so one class serves every tier
  of the nuclear family, and a server admin can add a TNT type by writing YAML.
- **Effects are modifiers, not subclasses.** Shockwave, debris, echo and silent are flags on a standard blast.
- **Client-only effects stay client-only.** Daytime, weather and beams are PacketEvents packets. They never touch the
  world, so restoring a player's view is just re-sending the real blocks and weather. The clock hold is the one effect
  that also rewrites the server's own time packets on their way to that player, because the server would otherwise
  undo it once a second.
