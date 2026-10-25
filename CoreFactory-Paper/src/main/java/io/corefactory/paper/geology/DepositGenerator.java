package io.corefactory.paper.geology;

import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DepositPlanner;
import io.corefactory.core.deposit.DepositSettings;
import io.corefactory.core.deposit.DepositStore;
import io.corefactory.core.deposit.OreBlocks;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.item.ItemFactory;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lays the deposits into a chunk the first time the chunk loads. The planner gives the deposits that touch the chunk;
 * each one is stored once, and then every block of its footprint that lies in its ore's height range and holds one of
 * the ore's replace materials becomes the ore's host block, with the chance {@link DepositSettings#oreBlockDensity()}.
 * The choice of each block is a hash of the deposit and the position (see {@link OreBlocks}), so the result never depends
 * on a random source.
 *
 * <p>A chunk carries a marker in its persistent data, set last, so it is laid once and also after a restart. Runs on the
 * main thread. One chunk writes at most {@link #MAX_WRITES_PER_CHUNK} blocks, which bounds the work of a load.
 */
public final class DepositGenerator {

    /** The id of the oil deposit in ores.yml. Every other ore is a solid ore. */
    static final String OIL_ORE = "crude_petroleum_deposit";

    /** The chunk marker: present once the chunk has been laid. */
    static final NamespacedKey MARKER = new NamespacedKey(ItemFactory.NAMESPACE, "deposits");

    /**
     * The most blocks one chunk's deposits may turn into ore. Only a deposit that covers the whole chunk, in a thick band of
     * replaceable stone, comes near it.
     */
    public static final int MAX_WRITES_PER_CHUNK = 4_096;

    private final DepositStore store;
    private final DepositSettings settings;
    private final List<String> solidOres;
    private final Map<String, Layer> layers;

    /** How one ore is laid: its host block, the materials it may replace, and its height range. */
    private record Layer(Material host, Set<Material> replace, int minY, int maxY) {
    }

    /**
     * @param store    where the deposits are kept; a deposit is inserted once, and its remaining amount is read from here
     * @param settings the planning constants and the ore block density
     * @param ores     the ores of the server, in file order; the oil deposit is the one with the id {@value #OIL_ORE}
     */
    public DepositGenerator(DepositStore store, DepositSettings settings, List<OreDefinition> ores) {
        this.store = Objects.requireNonNull(store, "store");
        this.settings = Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(ores, "ores");
        List<String> solid = new ArrayList<>();
        Map<String, Layer> byId = new HashMap<>();
        for (OreDefinition ore : ores) {
            if (!OIL_ORE.equals(ore.id())) {
                solid.add(ore.id());
            }
            Material host = Material.matchMaterial(ore.host());
            if (host == null || !host.isBlock()) {
                continue;
            }
            Set<Material> replace = new HashSet<>();
            for (String name : ore.replace()) {
                Material material = Material.matchMaterial(name);
                if (material != null) {
                    replace.add(material);
                }
            }
            byId.put(ore.id(), new Layer(host, Set.copyOf(replace), ore.minY(), ore.maxY()));
        }
        this.solidOres = List.copyOf(solid);
        this.layers = Map.copyOf(byId);
    }

    /**
     * Lays the deposits of a chunk, once. A chunk whose marker is set is left alone. Otherwise the deposits that touch
     * it are planned and stored, their ore blocks are placed, the host blocks that were already there are excluded from
     * being patch blocks (see {@link PatchExclusions}), and the marker is set last, so a chunk that failed part way is laid
     * again on its next load. Does nothing when there is no solid ore to plan.
     *
     * @param chunk a loaded chunk
     */
    public void populate(Chunk chunk) {
        PersistentDataContainer data = chunk.getPersistentDataContainer();
        if (data.has(MARKER, PersistentDataType.BYTE) || solidOres.isEmpty()) {
            return;
        }
        World world = chunk.getWorld();
        List<Deposit> planned = DepositPlanner.planChunk(world.getSeed(), world.getName(), chunk.getX(), chunk.getZ(),
                solidOres, OIL_ORE, settings);
        int writes = 0;
        Set<Integer> laid = new HashSet<>();
        List<Integer> preexisting = new ArrayList<>();
        for (Deposit deposit : planned) {
            store.insertIfAbsent(deposit);
            Deposit stored = store.find(deposit.id()).orElse(null);
            Layer layer = stored == null ? null : layers.get(stored.ore());
            if (layer != null) {
                writes += place(world, chunk, stored, layer, MAX_WRITES_PER_CHUNK - writes, laid, preexisting);
            }
        }
        PatchExclusions.addAll(chunk, preexisting.stream().mapToInt(Integer::intValue).toArray());
        data.set(MARKER, PersistentDataType.BYTE, (byte) 1);
    }

    /**
     * Turns the footprint's replaceable blocks of one deposit into its ore, column by column, up to the budget. Every
     * site of the deposit is visited, also past the budget, so a host block that was already there is always excluded.
     *
     * @param laid        the entries of the blocks this laying turned into ore, so another deposit does not exclude them
     * @param preexisting receives the entries of the sites that already held the host block when the chunk was laid
     * @return the number of blocks written
     */
    private int place(World world, Chunk chunk, Deposit deposit, Layer layer, int budget, Set<Integer> laid,
                      List<Integer> preexisting) {
        int minX = chunk.getX() * 16;
        int minZ = chunk.getZ() * 16;
        int fromX = Math.max(minX, deposit.centreX() - deposit.radius());
        int toX = Math.min(minX + 15, deposit.centreX() + deposit.radius());
        int fromZ = Math.max(minZ, deposit.centreZ() - deposit.radius());
        int toZ = Math.min(minZ + 15, deposit.centreZ() + deposit.radius());
        int fromY = Math.max(layer.minY(), world.getMinHeight());
        int toY = Math.min(layer.maxY(), world.getMaxHeight() - 1);
        double density = settings.oreBlockDensity();
        int written = 0;
        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                if (!deposit.contains(x, z)) {
                    continue;
                }
                for (int y = fromY; y <= toY; y++) {
                    if (!OreBlocks.shows(deposit.id(), x, y, z, density)) {
                        continue;
                    }
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    int key = PatchExclusions.pack(x, y, z);
                    if (type == layer.host()) {
                        if (!laid.contains(key)) {
                            preexisting.add(key);
                        }
                    } else if (written < budget && layer.replace().contains(type)) {
                        block.setType(layer.host(), false);
                        laid.add(key);
                        written++;
                    }
                }
            }
        }
        return written;
    }
}
