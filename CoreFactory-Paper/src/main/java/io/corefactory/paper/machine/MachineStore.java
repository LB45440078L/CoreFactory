package io.corefactory.paper.machine;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.device.Footprint;
import io.corefactory.core.display.PlacedDisplays;
import io.corefactory.core.drill.DrillCatalog;
import io.corefactory.core.drill.DrillDefinition;
import io.corefactory.core.drill.DrillSelection;
import io.corefactory.core.drill.DrillState;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Machine;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessEngine;
import io.corefactory.core.upgrade.LabLevels;
import io.corefactory.core.water.Face;
import io.corefactory.paper.chunks.ChunkKey;
import io.corefactory.paper.chunks.ChunkKeys;
import io.corefactory.paper.device.DevicePart;
import io.corefactory.paper.hologram.HologramService;
import io.corefactory.paper.screen.DeviceScreen;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The registry of placed machines, keyed by host block, with YAML persistence in {@code machines.yml}. Each machine keeps
 * its position, its type, its progress and its process there; its items are in its host block's barrel. Its item display
 * is removed with it.
 */
public final class MachineStore {

    /**
     * Builds the menu of a machine at its host block. The research console has its own; every other machine and drill uses
     * {@link MachineMenu}.
     */
    public interface MenuFactory {

        /**
         * @param slots the barrel order of the machine, matching the layout
         * @param fuels the items its fuel slot takes
         */
        MachineMenu create(Machine machine, MachineLayout layout, Location site, MachineSlots slots, Set<ItemRef> fuels);
    }

    private final CoreFactoryCore core;
    private final ProcessCatalog catalog;
    private final DrillCatalog drills;
    private final MenuFactory menus;
    private final File file;
    private final Logger log;
    private final PlacedDisplays displays;
    private final HologramService holograms;
    private final Map<String, PlacedMachine> byKey = new LinkedHashMap<>();
    private final List<Map<String, Object>> preserved = new ArrayList<>();
    private Consumer<PlacedMachine> removed = placed -> { };

    public MachineStore(CoreFactoryCore core, ProcessCatalog catalog, DrillCatalog drills, MenuFactory menus, File file,
                        Logger log, HologramService holograms) {
        this.core = core;
        this.catalog = catalog;
        this.drills = drills;
        this.menus = menus;
        this.file = file;
        this.log = log;
        this.holograms = holograms;
        this.displays = new PlacedDisplays(core.settings().displayScale());
    }

    /**
     * Sets what is told when a machine leaves the registry, whether it is removed or replaced. It is told before the machine's
     * items are dropped, so it can still read the host block.
     */
    public void onRemoved(Consumer<PlacedMachine> listener) {
        this.removed = listener;
    }

    /** Every placed machine. */
    public Collection<PlacedMachine> all() {
        return List.copyOf(byKey.values());
    }

    /** The chunk of every placed machine and drill, for the chunk keeper. A machine whose block has no world gives none. */
    public Set<ChunkKey> chunkKeys() {
        Set<ChunkKey> keys = new HashSet<>();
        for (PlacedMachine placed : all()) {
            ChunkKeys.of(placed.block()).ifPresent(keys::add);
        }
        return keys;
    }

    /**
     * The machine a block belongs to, if any: the machine whose host block this is, or a tall drill whose upper block it is. Both
     * blocks of a tall drill lead to the same machine.
     */
    public Optional<PlacedMachine> at(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        return DevicePart.covering(location, spot -> Optional.ofNullable(byKey.get(key(spot))),
                placed -> Footprint.heightOf(placed.machine().id()));
    }

    /**
     * Registers a machine at a host block and shows its display and its screen. The screen goes on the front face, the one toward the
     * player who placed the machine. A machine already registered there is removed first, without its items, since its block is being
     * replaced.
     *
     * @param front the horizontal face the screen is on
     */
    public PlacedMachine place(Location block, Machine machine, UUID owner, Face front) {
        Location spot = blockOf(block);
        Optional.ofNullable(byKey.get(key(spot))).ifPresent(this::forget);
        Material host = Material.matchMaterial(machine.host());
        PlacedMachine placed = create(spot, machine, host);
        placed.setOwner(owner);
        placed.setFront(front);
        placed.markSettled();
        placed.setDisplay(spawnDisplay(placed));
        placed.setTopDisplay(spawnTopDisplay(placed));
        placed.setScreen(spawnScreen(placed));
        byKey.put(key(spot), placed);
        return placed;
    }

    /**
     * Removes a machine that is gone from the world, or that a player broke. Its display goes, and its items drop at the
     * block: the barrel's items and the stacks still waiting in the file. The barrel is emptied first, so the block does not
     * drop its contents a second time when it goes.
     *
     * @param dropMachine true to drop the machine's own item as well as its items
     */
    public void remove(PlacedMachine placed, boolean dropMachine) {
        forget(placed);
        placed.menu().push();
        placed.menu().closeViewers();
        List<ItemStack> contents = new ArrayList<>(placed.menu().drops());
        contents.addAll(placed.legacy().values());
        placed.menu().clearBarrel();
        placed.legacy().clear();
        Location drop = placed.block().clone().add(0.5, 0.5, 0.5);
        World world = placed.block().getWorld();
        if (world == null) {
            return;
        }
        if (dropMachine) {
            ComponentDefinition definition = core.componentDefinitions().get(placed.machine().id());
            if (definition != null) {
                world.dropItemNaturally(drop, core.items().component(definition, 1));
            }
        }
        for (ItemStack item : contents) {
            world.dropItemNaturally(drop, item);
        }
    }

    /**
     * Sets a lab machine's level and draws it with the model of that level, in place of its old display. Its items, progress and
     * process stay as they are. The file is written at once, so the level is kept.
     */
    public void upgrade(PlacedMachine placed, int level) {
        placed.setLevel(level);
        displays.remove(placed.display());
        placed.setDisplay(spawnDisplay(placed));
        save();
    }

    /**
     * Shows the display again when it is missing, for example after startup removed it. A tall drill's upper display too, and the
     * screen when one of its two entities is missing.
     */
    public void ensureDisplay(PlacedMachine placed) {
        if (placed.display() == null || Bukkit.getEntity(placed.display()) == null) {
            placed.setDisplay(spawnDisplay(placed));
        }
        if (placed.topDisplay() == null || Bukkit.getEntity(placed.topDisplay()) == null) {
            placed.setTopDisplay(spawnTopDisplay(placed));
        }
        if (placed.screen() == null || !placed.screen().present()) {
            DeviceScreen partial = placed.screen();
            if (partial != null) {
                partial.remove();
            }
            placed.setScreen(spawnScreen(placed));
        }
    }

    /**
     * Brings a machine loaded from an older machines.yml onto the barrel storage. Once its chunk is loaded, the block of its
     * older host becomes a barrel, and the stacks the file still holds move into the barrel slots that are empty. A stack whose
     * slot is taken stays in the file and is logged, so nothing is overwritten. Does nothing until the chunk is loaded.
     */
    public void settle(PlacedMachine placed) {
        if (placed.settled() || !placed.isLoaded()) {
            return;
        }
        Block block = placed.block().getBlock();
        String legacyHost = BarrelMigration.legacyHost(placed.machine().id()).orElse(null);
        if (legacyHost != null && block.getType().name().equals(legacyHost)) {
            dropContents(block);
            block.setType(Material.BARREL, false);
            placed.menu().forgetBarrel();
            log.info("machines.yml: the " + placed.machine().id() + " at " + describe(placed.block())
                    + " stood on a " + legacyHost + "; it now stands on a barrel.");
        }
        Optional<Inventory> held = placed.menu().barrel().inventory();
        if (held.isEmpty()) {
            return;
        }
        if (!placed.legacy().isEmpty()) {
            Inventory barrel = held.get();
            IntPredicate free = index -> Barrel.has(barrel, index) && ItemRefs.isEmpty(Barrel.itemAt(barrel, index));
            BarrelMigration.Plan<Integer, ItemStack> plan = BarrelMigration.plan(placed.legacy(),
                    placed.menu()::barrelIndexOf, free);
            for (Map.Entry<Integer, ItemStack> move : plan.moved().entrySet()) {
                Barrel.setItemAt(barrel, move.getKey(), move.getValue());
            }
            log.info("machines.yml: moved " + plan.moved().size() + " stored stack(s) of the " + placed.machine().id()
                    + " at " + describe(placed.block()) + " into its barrel.");
            if (!plan.kept().isEmpty()) {
                log.warning("machines.yml: " + plan.kept().size() + " stored stack(s) of the " + placed.machine().id()
                        + " at " + describe(placed.block()) + " were kept in machines.yml, since their barrel slots are taken.");
            }
            placed.legacy().clear();
            placed.legacy().putAll(plan.kept());
            if (!plan.moved().isEmpty()) {
                save();
            }
        }
        placed.markSettled();
    }

    /**
     * Forgets the live barrels of the machines in a chunk that is unloading, so they are read again after it loads. A menu's pending
     * edits are written into its barrel first. The menus are then closed, since a machine in an unloaded chunk does not run and
     * nothing a player moves in its menu could reach the barrel. Their holograms go too.
     */
    public void unloaded(Chunk chunk) {
        for (PlacedMachine placed : byKey.values()) {
            Location at = placed.block();
            if (at.getWorld() != null && at.getWorld().equals(chunk.getWorld())
                    && at.getBlockX() >> 4 == chunk.getX() && at.getBlockZ() >> 4 == chunk.getZ()) {
                placed.menu().forgetBarrel();
                placed.menu().closeViewers();
                releaseHologram(placed);
            }
        }
    }

    /** Hides a machine's hologram from its viewers and detaches it from the machine. Does nothing when it has none. */
    void releaseHologram(PlacedMachine placed) {
        if (placed.hologram() != null) {
            holograms.remove(placed.hologram());
            placed.setHologram(null);
        }
    }

    /** Loads the machines from {@code machines.yml}. Call once, after the worlds are loaded. */
    public void load() {
        byKey.clear();
        preserved.clear();
        if (!file.exists()) {
            return;
        }
        ConfigurationSection root = YamlConfiguration.loadConfiguration(file).getConfigurationSection("machines");
        if (root == null) {
            return;
        }
        for (String index : root.getKeys(false)) {
            ConfigurationSection entry = root.getConfigurationSection(index);
            if (entry == null) {
                continue;
            }
            UUID worldId = parseUuid(entry.getString("world"));
            World world = worldId == null ? null : Bukkit.getWorld(worldId);
            if (world == null) {
                preserved.add(entry.getValues(true));
                continue;
            }
            Optional<Machine> machine = machineOf(entry.getString("id", ""));
            if (machine.isEmpty()) {
                log.warning("machines.yml: a machine of unknown id '" + entry.getString("id") + "' is dropped.");
                continue;
            }
            Location at = new Location(world, entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
            PlacedMachine placed = create(at, machine.get(), Material.matchMaterial(machine.get().host()));
            ConfigurationSection slots = entry.getConfigurationSection("slots");
            if (slots != null) {
                for (String slot : slots.getKeys(false)) {
                    ItemStack item = slots.getItemStack(slot);
                    if (item != null && slot.matches("\\d+")) {
                        placed.legacy().put(Integer.parseInt(slot), item);
                    }
                }
            }
            placed.state().restore(entry.getString("process"), entry.getInt("progress"));
            if (placed.drill() != null) {
                placed.drill().restore(entry.getDouble("cycle"));
                placed.setSelection(new DrillSelection(entry.getStringList("ignore")));
            }
            placed.setOwner(parseUuid(entry.getString("owner")));
            placed.setFront(parseFace(entry.getString("front")));
            placed.setScreen(DeviceScreen.restore(parseUuid(entry.getString("screen-panel")),
                    parseUuid(entry.getString("screen-text")), parseUuid(entry.getString("screen-bezel"))));
            UUID display = parseUuid(entry.getString("display"));
            if (placed.drill() == null) {
                // An entry without a level, from an older file, is level I. A higher level is drawn with its own model, so the
                // display saved for level I is removed and the next display check spawns the right one.
                placed.setLevel(LabLevels.clamp(entry.getInt("level", LabLevels.MIN_LEVEL)));
                if (placed.level() != LabLevels.MIN_LEVEL) {
                    displays.remove(display);
                    display = null;
                }
            }
            placed.setDisplay(display);
            placed.setTopDisplay(parseUuid(entry.getString("display-top")));
            byKey.put(key(at), placed);
        }
    }

    /** Writes every machine to {@code machines.yml}: its position, type, progress, process and display. */
    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        int index = 0;
        for (PlacedMachine placed : byKey.values()) {
            String base = "machines." + index++ + ".";
            Location at = placed.block();
            yaml.set(base + "world", at.getWorld().getUID().toString());
            yaml.set(base + "x", at.getBlockX());
            yaml.set(base + "y", at.getBlockY());
            yaml.set(base + "z", at.getBlockZ());
            yaml.set(base + "id", placed.machine().id());
            yaml.set(base + "progress", placed.state().progress());
            yaml.set(base + "level", placed.level());
            if (placed.owner() != null) {
                yaml.set(base + "owner", placed.owner().toString());
            }
            yaml.set(base + "front", placed.front().name());
            if (placed.screen() != null && placed.screen().panel() != null) {
                yaml.set(base + "screen-panel", placed.screen().panel().toString());
            }
            if (placed.screen() != null && placed.screen().text() != null) {
                yaml.set(base + "screen-text", placed.screen().text().toString());
            }
            if (placed.screen() != null && placed.screen().bezel() != null) {
                yaml.set(base + "screen-bezel", placed.screen().bezel().toString());
            }
            if (placed.state().processId() != null) {
                yaml.set(base + "process", placed.state().processId());
            }
            if (placed.drill() != null) {
                yaml.set(base + "cycle", placed.drill().progress());
                if (!placed.selection().serialise().isEmpty()) {
                    yaml.set(base + "ignore", placed.selection().serialise());
                }
            }
            if (placed.display() != null) {
                yaml.set(base + "display", placed.display().toString());
            }
            if (placed.topDisplay() != null) {
                yaml.set(base + "display-top", placed.topDisplay().toString());
            }
            for (Map.Entry<Integer, ItemStack> slot : placed.legacy().entrySet()) {
                yaml.set(base + "slots." + slot.getKey(), slot.getValue());
            }
        }
        for (Map<String, Object> raw : preserved) {
            yaml.createSection("machines." + index++, raw);
        }
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not save machines to " + file.getName(), e);
        }
    }

    /** Removes a machine's display, hologram and registry entry, leaving its items where they are. */
    private void forget(PlacedMachine placed) {
        byKey.remove(key(placed.block()));
        displays.remove(placed.display());
        displays.remove(placed.topDisplay());
        if (placed.screen() != null) {
            placed.screen().remove();
            placed.setScreen(null);
        }
        releaseHologram(placed);
        removed.accept(placed);
    }

    /** The machine of a component id: a lab tool from processes.yml, or a drill from its own section. */
    private Optional<Machine> machineOf(String id) {
        Optional<Machine> machine = catalog.machine(id);
        return machine.isPresent() ? machine : drills.machine(id);
    }

    private PlacedMachine create(Location at, Machine machine, Material host) {
        Optional<DrillDefinition> drill = drills.drill(machine.id());
        MachineLayout layout = drill.isPresent()
                ? MachineLayouts.drill(machine.name(), drill.get().kind())
                : MachineLayouts.of(machine);
        if (layout.inputSlots().size() != machine.inputSlots() || layout.outputSlots().size() != machine.outputSlots()
                || layout.fuelSlots().size() != machine.fuelSlots()) {
            log.warning("The menu of machine '" + machine.id() + "' does not have the slots processes.yml gives it.");
        }
        MachineSlots slots = new MachineSlots(layout.inputSlots().size(), layout.outputSlots().size(),
                layout.fuelSlots().size());
        Set<ItemRef> fuels = drill.isPresent() ? Set.of() : ProcessEngine.FUEL;
        PlacedMachine placed = new PlacedMachine(at, machine, host, menus.create(machine, layout, at, slots, fuels));
        drill.ifPresent(definition -> {
            placed.setDrill(new DrillState(definition));
            placed.setMining(new DrillMining());
        });
        return placed;
    }

    private UUID spawnDisplay(PlacedMachine placed) {
        ComponentDefinition definition = core.componentDefinitions().get(placed.machine().id());
        ItemStack item = definition == null
                ? new ItemStack(placed.host() == null ? Material.STONE : placed.host())
                : levelModel(core.items().machine(definition), placed);
        return displays.spawn(placed.block(), item);
    }

    /**
     * The display item of a lab machine at a level above I: the same item, with the model of that level ({@code machine/<id>_2} to
     * {@code machine/<id>_5}). Level I, and every drill, keep the machine's own model.
     */
    private static ItemStack levelModel(ItemStack item, PlacedMachine placed) {
        if (placed.drill() != null || placed.level() == LabLevels.MIN_LEVEL) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String key = ItemFactory.displayKey(LabLevels.modelId(placed.machine().id(), placed.level()));
            meta.setItemModel(new NamespacedKey(ItemFactory.NAMESPACE, key));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * The display of the block above a tall drill's host block: the upper half of its model. Null for a machine one block tall, or
     * when its component is not defined.
     */
    private UUID spawnTopDisplay(PlacedMachine placed) {
        if (Footprint.heightOf(placed.machine().id()) <= Footprint.SHORT) {
            return null;
        }
        ComponentDefinition definition = core.componentDefinitions().get(placed.machine().id());
        if (definition == null) {
            return null;
        }
        ItemStack item = DevicePart.topItem(core.items().machine(definition), placed.machine().id());
        return displays.spawn(placed.block().clone().add(0, Footprint.SHORT, 0), item);
    }

    /** The screen of a machine on its front face. Its height is the machine's footprint, so a tall drill's panel spans both blocks. */
    private DeviceScreen spawnScreen(PlacedMachine placed) {
        return DeviceScreen.create(placed.block().getWorld(), placed.block(), placed.front(),
                Footprint.heightOf(placed.machine().id()));
    }

    /** Drops what an older host holds at its block, so that converting the block to a barrel loses nothing. */
    private static void dropContents(Block block) {
        if (!(block.getState() instanceof Container container)) {
            return;
        }
        Location drop = block.getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack item : container.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                block.getWorld().dropItemNaturally(drop, item);
            }
        }
        container.getInventory().clear();
    }

    private static String describe(Location location) {
        return location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
    }

    private static Location blockOf(Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static String key(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":"
                + location.getBlockZ();
    }

    private static Face parseFace(String text) {
        if (text == null) {
            return null;
        }
        try {
            return Face.valueOf(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static UUID parseUuid(String text) {
        if (text == null) {
            return null;
        }
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
