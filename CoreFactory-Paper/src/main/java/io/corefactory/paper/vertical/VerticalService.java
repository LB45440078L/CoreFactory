package io.corefactory.paper.vertical;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.energy.EnergyTable;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.Port;
import io.corefactory.core.vertical.Belt;
import io.corefactory.core.vertical.VerticalCarry;
import io.corefactory.core.vertical.VerticalDir;
import io.corefactory.core.vertical.VerticalLevels;
import io.corefactory.core.vertical.VerticalTower;
import io.corefactory.core.vertical.VerticalTowers;
import io.corefactory.paper.logistics.DisplayBudget;
import io.corefactory.paper.machine.ItemRefs;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Owns the vertical belts, the towers they make, and the items carried on the towers. The towers of a world are rebuilt with
 * {@link VerticalTowers#towersOf} when a belt is added, removed or turned, never per tick. Each tower keeps one {@link VerticalCarry}
 * and the items on it, in the carry's order. When a tower is rebuilt, each item keeps its height in the world if the new tower covers
 * that height, and is dropped as a world item at its block when it does not fit.
 *
 * <p>Each server tick, every tower in a loaded chunk moves its items one step and gives the item at its output end to the cell beyond
 * it: a regular belt, or a port of a machine, a chest or another storage. A tower's items are shown as small item displays along it.
 * A tower takes items through the port of each of its belts, which a regular belt or a picker feeds. The state is saved by the
 * logistics service, with {@link #save()}.
 */
public final class VerticalService {

    private static final int ENTITY_TICKS = 20;
    private static final double TICK_SECONDS = EnergyTable.SECONDS_PER_TICK;

    /**
     * An item on a tower, placed in its world: its column, and its height in the world.
     *
     * @param world the world
     * @param x     the column's x
     * @param y     the item's height
     * @param z     the column's z
     * @param item  the item
     */
    private record Carried(UUID world, int x, double y, int z, Item item) {
    }

    /**
     * What the menu of a belt shows about its tower.
     *
     * @param size      the tower's length in belts
     * @param level     the tower's level, the level of its output belt
     * @param carried   the items on the tower
     * @param direction the tower's direction
     */
    public record Info(int size, int level, int carried, VerticalDir direction) {
    }

    /** A display shown for one item on a tower: its entity, the item it shows and the block its anchor is in. */
    private static final class Shown {

        private final UUID display;
        private Item item;
        private int anchorY;

        private Shown(UUID display, Item item, int anchorY) {
            this.display = display;
            this.item = item;
            this.anchorY = anchorY;
        }
    }

    /** A tower as kept: its shape, its carry, the items in carry order (the first is nearest the output), and the displays shown. */
    private static final class Tower {

        private final VerticalTower shape;
        private final VerticalCarry carry;
        private final int level;
        private final UUID owner;
        private final List<Item> items = new ArrayList<>();
        private final List<Shown> shown = new ArrayList<>();

        private Tower(VerticalTower shape, int level, UUID owner) {
            this.shape = shape;
            this.carry = new VerticalCarry(shape.size());
            this.level = level;
            this.owner = owner;
        }

        /** The height in the world of a carry coordinate: bottom.y + p for UP, top.y + 1 - p for DOWN. */
        private double heightOf(double p) {
            return shape.direction() == VerticalDir.UP ? shape.bottom().y() + p : shape.top().y() + 1 - p;
        }

        /** The carry coordinate of a height, clamped to the tower: the inverse of {@link #heightOf}. */
        private double coordinateOf(double y) {
            double p = shape.direction() == VerticalDir.UP ? y - shape.bottom().y() : shape.top().y() + 1 - y;
            return Math.max(0.0, Math.min(shape.size(), p));
        }
    }

    /** The belts and towers of one world. */
    private static final class WorldState {

        private final Map<Pos, VerticalBelt> belts = new HashMap<>();
        private List<Tower> towers = new ArrayList<>();
        private Map<Pos, Tower> byBelt = new HashMap<>();
    }

    private final CoreFactoryCore core;
    private final VerticalStore store;
    private final VerticalLevels levels;
    private final VerticalEntities entities;
    private final DisplayBudget budget;
    private final Logger log;
    private final Map<String, VerticalBelt> byId = new LinkedHashMap<>();
    private final Map<UUID, WorldState> worlds = new HashMap<>();
    private long ticks;

    /**
     * @param core     the core, which builds the component items
     * @param refs     converts items to stacks
     * @param store    {@code vertical.yml}
     * @param settings the per-level speed and capacity, and the cap on item displays
     * @param log      receives warnings
     */
    public VerticalService(CoreFactoryCore core, ItemRefs refs, VerticalStore store, VerticalSettings settings, Logger log) {
        this.core = core;
        this.store = store;
        this.levels = settings.levels();
        this.entities = new VerticalEntities(core, refs);
        this.budget = new DisplayBudget(settings.maxDisplays());
        this.log = log;
    }

    /** Reads the saved belts and the items on their towers, and builds the towers. Call once, after the worlds are loaded. */
    public void load() {
        VerticalStore.State saved = store.read();
        for (VerticalStore.Saved belt : saved.belts()) {
            register(new VerticalBelt(belt.id(), belt.world(), belt.pos(), belt.level(), belt.direction(), belt.owner()));
        }
        Set<UUID> worldIds = new HashSet<>();
        for (VerticalBelt belt : byId.values()) {
            worldIds.add(belt.world());
        }
        int restored = 0;
        for (UUID world : worldIds) {
            List<Carried> carried = new ArrayList<>();
            for (VerticalStore.SavedItem item : saved.items()) {
                if (item.world().equals(world)) {
                    carried.add(new Carried(world, item.x(), item.y(), item.z(), item.item()));
                }
            }
            restored += carried.size();
            rebuild(world, carried);
        }
        if (restored < saved.items().size()) {
            log.warning("Vertical belts: " + (saved.items().size() - restored) + " item(s) on towers with no belt left are lost.");
        }
    }

    /**
     * One server tick of the towers: each tower in a loaded chunk moves its items, gives the item at its output end when the cell
     * beyond it takes it, and shows its items. Call from the logistics tick.
     *
     * @param neighbours what the logistics network offers at the output cells
     */
    public void tick(TowerNeighbours neighbours) {
        ticks++;
        if (ticks % ENTITY_TICKS == 0) {
            ensureEntities();
        }
        for (Map.Entry<UUID, WorldState> entry : worlds.entrySet()) {
            World world = Bukkit.getWorld(entry.getKey());
            if (world == null) {
                continue;
            }
            for (Tower tower : entry.getValue().towers) {
                Pos bottom = tower.shape.bottom();
                if (!world.isChunkLoaded(bottom.x() >> 4, bottom.z() >> 4)) {
                    continue;
                }
                tower.carry.step(TICK_SECONDS, levels.speed(tower.level), levels.capacity(tower.level));
                output(entry.getKey(), tower, neighbours);
                render(world, tower);
            }
        }
    }

    /** The belt whose block is this location, if any. */
    public Optional<VerticalBelt> at(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        WorldState state = worlds.get(location.getWorld().getUID());
        if (state == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(state.belts.get(new Pos(location.getBlockX(), location.getBlockY(), location.getBlockZ())));
    }

    /** The belt whose interaction this entity is, if any. */
    public Optional<VerticalBelt> byInteraction(UUID entity) {
        return byId.values().stream().filter(belt -> entity.equals(belt.interaction())).findFirst();
    }

    /** Whether a vertical belt stands at the cell of a world. */
    public boolean contains(UUID world, Pos cell) {
        WorldState state = worlds.get(world);
        return state != null && state.belts.containsKey(cell);
    }

    /** What the menu of a belt shows about its tower, or empty when the belt is not known. */
    public Optional<Info> infoOf(VerticalBelt belt) {
        WorldState state = worlds.get(belt.world());
        Tower tower = state == null ? null : state.byBelt.get(belt.pos());
        if (tower == null) {
            return Optional.empty();
        }
        return Optional.of(new Info(tower.shape.size(), tower.level, tower.items.size(), tower.shape.direction()));
    }

    /**
     * The port a regular belt or a picker gives items to at a tower's belt. It takes an item while the belt's entry has room on the
     * tower, and it never gives an item out.
     *
     * @return the port, or empty when the cell is not a belt of a tower
     */
    public Optional<Port> inputPort(UUID world, Pos cell) {
        WorldState state = worlds.get(world);
        Tower tower = state == null ? null : state.byBelt.get(cell);
        if (tower == null) {
            return Optional.empty();
        }
        int block = tower.shape.carryBlock(cell);
        return Optional.of(new Port() {
            @Override
            public boolean accepts(Item item) {
                return tower.carry.slotFor(block, levels.capacity(tower.level)) >= 0;
            }

            @Override
            public void accept(Item item) {
                int slot = tower.carry.place(block, levels.capacity(tower.level));
                if (slot >= 0) {
                    tower.items.add(slot, item);
                }
            }

            @Override
            public Item peekOutput() {
                return null;
            }

            @Override
            public Item takeOutput() {
                return null;
            }

            @Override
            public int storedCount() {
                return 0;
            }
        });
    }

    /**
     * Adds a belt that the placement checked, joins it to the towers it touches, and saves. A belt placed beside a tower takes that
     * tower's direction: the tower the player clicked first, then the other. Both neighbouring towers are turned to that direction, so
     * the merged tower has one direction.
     *
     * @param world   the world
     * @param pos     the block, which must be free
     * @param level   the belt's level, 1 to 3
     * @param owner   the id of the player who placed it, or null
     * @param clicked the block the player clicked when it is a belt of a tower, or null
     * @return the belt
     */
    public VerticalBelt add(UUID world, Pos pos, int level, UUID owner, Pos clicked) {
        WorldState state = state(world);
        Pos above = new Pos(pos.x(), pos.y() + 1, pos.z());
        Pos below = new Pos(pos.x(), pos.y() - 1, pos.z());
        VerticalBelt aboveBelt = state.belts.get(above);
        VerticalBelt belowBelt = state.belts.get(below);
        boolean clickedBelow = below.equals(clicked);
        Optional<VerticalDir> preferred = Optional.ofNullable(clickedBelow ? belowBelt : aboveBelt).map(VerticalBelt::direction);
        Optional<VerticalDir> other = Optional.ofNullable(clickedBelow ? aboveBelt : belowBelt).map(VerticalBelt::direction);
        VerticalDir direction = VerticalRules.adoptDirection(preferred, other);
        List<Carried> carried = carriedOf(world);
        if (aboveBelt != null) {
            cascade(state, above, direction);
        }
        if (belowBelt != null) {
            cascade(state, below, direction);
        }
        VerticalBelt belt = new VerticalBelt(UUID.randomUUID().toString(), world, pos, level, direction, owner);
        register(belt);
        rebuild(world, carried);
        ensureEntities();
        save();
        return belt;
    }

    /**
     * Removes a belt, with its entities, and rebuilds its towers. The items that stood on its block no longer fit and are dropped as
     * world items there; the others stay on their towers.
     *
     * @return the stacks to drop at the belt's block: its component item
     */
    /** Every vertical belt placed. */
    public List<VerticalBelt> all() {
        return List.copyOf(byId.values());
    }

    /**
     * Removes a vertical belt, and releases its entities. Returns the item it gives back.
     */
    public List<ItemStack> remove(VerticalBelt belt) {
        List<ItemStack> drops = new ArrayList<>();
        WorldState state = worlds.get(belt.world());
        if (state == null || state.belts.get(belt.pos()) != belt) {
            return drops;
        }
        List<Carried> carried = carriedOf(belt.world());
        state.belts.remove(belt.pos());
        byId.remove(belt.id());
        VerticalEntities.remove(belt.display());
        VerticalEntities.remove(belt.interaction());
        entities.deviceItem(belt.level()).ifPresent(drops::add);
        rebuild(belt.world(), carried);
        save();
        return drops;
    }

    /**
     * Sets the direction of the whole tower that holds a belt: every belt above and below it, the cascade. The items on the tower keep
     * their heights in the world, and move the new way.
     *
     * @return true when the direction changed
     */
    public boolean setDirection(VerticalBelt belt, VerticalDir direction) {
        WorldState state = worlds.get(belt.world());
        if (state == null || state.belts.get(belt.pos()) != belt || belt.direction() == direction) {
            return false;
        }
        List<Carried> carried = carriedOf(belt.world());
        cascade(state, belt.pos(), direction);
        rebuild(belt.world(), carried);
        for (VerticalBelt other : state.belts.values()) {
            entities.turnBelt(other.display(), other.direction());
        }
        save();
        return true;
    }

    /** Writes the belts and the items on the towers to {@code vertical.yml}. */
    public void save() {
        List<VerticalStore.Saved> belts = new ArrayList<>();
        for (VerticalBelt belt : byId.values()) {
            belts.add(new VerticalStore.Saved(belt.id(), belt.owner(), belt.world(), belt.pos(), belt.level(), belt.direction()));
        }
        List<VerticalStore.SavedItem> items = new ArrayList<>();
        for (UUID world : worlds.keySet()) {
            for (Carried carried : carriedOf(world)) {
                items.add(new VerticalStore.SavedItem(carried.world(), carried.x(), carried.y(), carried.z(), carried.item()));
            }
        }
        store.write(new VerticalStore.State(belts, items));
    }

    /** A chunk is unloading: the entities of its belts and the displays of its items go; the data stays. */
    public void unloaded(Chunk chunk) {
        for (VerticalBelt belt : byId.values()) {
            if (inChunk(belt.world(), belt.pos(), chunk)) {
                VerticalEntities.remove(belt.display());
                VerticalEntities.remove(belt.interaction());
                belt.setDisplay(null);
                belt.setInteraction(null);
            }
        }
        WorldState state = worlds.get(chunk.getWorld().getUID());
        if (state != null) {
            for (Tower tower : state.towers) {
                if (inChunk(chunk.getWorld().getUID(), tower.shape.bottom(), chunk)) {
                    releaseDisplays(tower);
                }
            }
        }
    }

    /** A chunk has loaded: its marked entities that nothing owns are removed. */
    public void sweep(Chunk chunk) {
        Set<UUID> ownedBelts = new HashSet<>();
        for (VerticalBelt belt : byId.values()) {
            if (belt.display() != null) {
                ownedBelts.add(belt.display());
            }
            if (belt.interaction() != null) {
                ownedBelts.add(belt.interaction());
            }
        }
        Set<UUID> shown = new HashSet<>();
        for (WorldState state : worlds.values()) {
            for (Tower tower : state.towers) {
                for (Shown item : tower.shown) {
                    shown.add(item.display);
                }
            }
        }
        entities.sweep(chunk, ownedBelts, shown);
    }

    /** Releases every entity of the belts and the item displays. Called when the plugin is disabled, after the state is saved. */
    public void shutdown() {
        for (VerticalBelt belt : byId.values()) {
            VerticalEntities.remove(belt.display());
            VerticalEntities.remove(belt.interaction());
            belt.setDisplay(null);
            belt.setInteraction(null);
        }
        for (WorldState state : worlds.values()) {
            for (Tower tower : state.towers) {
                releaseDisplays(tower);
            }
        }
    }

    /** Moves the items on a tower's output end to the cell beyond it, one at a time, when that cell takes them. */
    private void output(UUID world, Tower tower, TowerNeighbours neighbours) {
        if (tower.carry.waiting() == 0) {
            return;
        }
        Item item = tower.items.get(0);
        Pos cell = tower.shape.outputCell();
        boolean taken;
        if (neighbours.isRegularBelt(world, cell)) {
            taken = neighbours.putOnRegularBelt(world, cell, item);
        } else {
            taken = neighbours.portAt(world, cell, tower.owner)
                    .filter(port -> port.accepts(item))
                    .map(port -> {
                        port.accept(item);
                        return true;
                    })
                    .orElse(false);
        }
        if (taken) {
            tower.carry.output(1);
            tower.items.remove(0);
        }
    }

    /** Shows the items of a tower as displays along it, within the cap, and removes the displays of items that left. */
    private void render(World world, Tower tower) {
        Pos bottom = tower.shape.bottom();
        List<Double> positions = tower.carry.positions();
        int wanted = tower.items.size();
        for (int i = 0; i < wanted; i++) {
            double y = tower.heightOf(positions.get(i));
            int anchorY = (int) Math.floor(y);
            double dy = y - (anchorY + 0.5);
            Item item = tower.items.get(i);
            if (i >= tower.shown.size()) {
                Optional<ItemStack> stack = entities.stackOf(item);
                if (stack.isEmpty()) {
                    break;
                }
                DisplayBudget.Take take = budget.take();
                if (take == DisplayBudget.Take.REFUSED_WARN) {
                    log.warning("Vertical belts: the item displays on towers are capped at " + budget.cap()
                            + " (vertical-belt.max-item-displays). More items are carried but not shown until some leave the towers.");
                }
                if (take != DisplayBudget.Take.TAKEN) {
                    break;
                }
                UUID display = entities.spawnItem(new Location(world, bottom.x(), anchorY, bottom.z()), stack.get(), dy);
                tower.shown.add(new Shown(display, item, anchorY));
                continue;
            }
            Shown shown = tower.shown.get(i);
            if (!shown.item.equals(item)) {
                entities.stackOf(item).ifPresent(stack -> entities.setItem(shown.display, stack));
                shown.item = item;
            }
            if (shown.anchorY != anchorY) {
                entities.anchor(shown.display, new Location(world, bottom.x(), anchorY, bottom.z()));
                shown.anchorY = anchorY;
            }
            if (!entities.moveItem(shown.display, dy)) {
                releaseDisplays(tower);
                return;
            }
        }
        while (tower.shown.size() > wanted) {
            Shown extra = tower.shown.remove(tower.shown.size() - 1);
            VerticalEntities.remove(extra.display);
            budget.giveBack();
        }
    }

    /** Makes the missing entities of every belt in a loaded chunk: its display and its interaction. */
    private void ensureEntities() {
        for (VerticalBelt belt : byId.values()) {
            World world = Bukkit.getWorld(belt.world());
            if (world == null || !world.isChunkLoaded(belt.pos().x() >> 4, belt.pos().z() >> 4)) {
                continue;
            }
            Location block = new Location(world, belt.pos().x(), belt.pos().y(), belt.pos().z());
            if (belt.display() == null || Bukkit.getEntity(belt.display()) == null) {
                belt.setDisplay(entities.spawnBelt(block, entities.beltItem(belt.level()), belt.direction(), belt.id()));
            }
            if (belt.interaction() == null || Bukkit.getEntity(belt.interaction()) == null) {
                belt.setInteraction(entities.spawnInteraction(block, belt.id()));
            }
        }
    }

    /**
     * Rebuilds the towers of a world from its belts. The displays of the old towers are removed. Each carried item goes back on the new
     * tower that covers its height, at that height; an item that no tower covers, or that has no room, is dropped at its block.
     */
    private void rebuild(UUID world, List<Carried> carried) {
        WorldState state = state(world);
        for (Tower old : state.towers) {
            releaseDisplays(old);
        }
        List<VerticalTower> shapes = state.belts.isEmpty() ? List.of() : VerticalTowers.towersOf(geometryOf(state));
        List<Tower> towers = new ArrayList<>();
        Map<Pos, Tower> byBelt = new HashMap<>();
        for (VerticalTower shape : shapes) {
            VerticalBelt output = state.belts.get(shape.outputEnd());
            Tower tower = new Tower(shape, output.level(), output.owner());
            towers.add(tower);
            for (Pos position : shape.positions()) {
                byBelt.put(position, tower);
            }
        }
        state.towers = towers;
        state.byBelt = byBelt;
        for (Carried item : carried) {
            if (!restore(state, item)) {
                drop(item);
            }
        }
    }

    /** Puts a carried item back on the tower that covers its height in its column. False when none covers it or it has no room. */
    private boolean restore(WorldState state, Carried item) {
        for (Tower tower : state.towers) {
            Pos bottom = tower.shape.bottom();
            Pos top = tower.shape.top();
            if (bottom.x() != item.x() || bottom.z() != item.z() || item.y() < bottom.y() || item.y() > top.y() + 1) {
                continue;
            }
            int slot = tower.carry.place(tower.coordinateOf(item.y()), levels.capacity(tower.level));
            if (slot < 0) {
                return false;
            }
            tower.items.add(slot, item.item());
            return true;
        }
        return false;
    }

    /** Drops a carried item as a world item at the block it stood in. */
    private void drop(Carried item) {
        World world = Bukkit.getWorld(item.world());
        if (world == null) {
            log.warning("Vertical belts: an item was lost because its world is not loaded.");
            return;
        }
        Location spot = new Location(world, item.x() + 0.5, Math.floor(item.y()) + 0.5, item.z() + 0.5);
        entities.stackOf(item.item()).ifPresent(stack -> world.dropItemNaturally(spot, stack));
    }

    /** The items on the towers of a world, each at its height. */
    private List<Carried> carriedOf(UUID world) {
        List<Carried> carried = new ArrayList<>();
        WorldState state = worlds.get(world);
        if (state == null) {
            return carried;
        }
        for (Tower tower : state.towers) {
            Pos bottom = tower.shape.bottom();
            List<Double> positions = tower.carry.positions();
            for (int i = 0; i < positions.size(); i++) {
                carried.add(new Carried(world, bottom.x(), tower.heightOf(positions.get(i)), bottom.z(), tower.items.get(i)));
            }
        }
        return carried;
    }

    /** Sets the direction of the tower that holds a belt, in the belts' own state. The towers are rebuilt afterwards. */
    private static void cascade(WorldState state, Pos anyBelt, VerticalDir direction) {
        Map<Pos, Belt> changed = VerticalTowers.setDirection(VerticalTowers.towersOf(geometryOf(state)), anyBelt, direction);
        for (Map.Entry<Pos, Belt> entry : changed.entrySet()) {
            state.belts.get(entry.getKey()).setDirection(entry.getValue().direction());
        }
    }

    private static Map<Pos, Belt> geometryOf(WorldState state) {
        Map<Pos, Belt> geometry = new HashMap<>();
        for (Map.Entry<Pos, VerticalBelt> entry : state.belts.entrySet()) {
            geometry.put(entry.getKey(), entry.getValue().belt());
        }
        return geometry;
    }

    private void register(VerticalBelt belt) {
        byId.put(belt.id(), belt);
        state(belt.world()).belts.put(belt.pos(), belt);
    }

    private WorldState state(UUID world) {
        return worlds.computeIfAbsent(world, key -> new WorldState());
    }

    private void releaseDisplays(Tower tower) {
        for (Shown shown : tower.shown) {
            VerticalEntities.remove(shown.display);
            budget.giveBack();
        }
        tower.shown.clear();
    }

    private static boolean inChunk(UUID world, Pos pos, Chunk chunk) {
        return chunk.getWorld().getUID().equals(world) && pos.x() >> 4 == chunk.getX() && pos.z() >> 4 == chunk.getZ();
    }

    private static boolean inChunk(VerticalBelt belt, Chunk chunk) {
        return inChunk(belt.world(), belt.pos(), chunk);
    }
}
