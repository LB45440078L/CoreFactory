package io.corefactory.paper.energy;

import io.corefactory.core.device.Footprint;
import io.corefactory.core.energy.DeviceKind;
import io.corefactory.core.energy.Pos;
import io.corefactory.paper.hologram.Hologram;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * An energy device placed on air: a pylon, an accumulator, or a generator ({@link GeneratorDevice}). It has no vanilla host block. Its
 * look is an item display, its clicks go to an interaction entity, and an optional hologram shows its state. The display and the
 * interaction are entities of the world and are recreated when they go missing; the device itself is the saved data, kept in
 * {@link DeviceRegistry}. The block is the device's position, and the component id of its item follows from its kind and level.
 */
public class EnergyDevice {

    private final String id;
    private final DeviceKind kind;
    private final Location block;
    private final int level;
    private UUID display;
    private UUID topDisplay;
    private UUID interaction;
    private Hologram hologram;

    /**
     * @param id    the device id, unique among the devices
     * @param kind  the device kind
     * @param block the block the device stands in; copied to whole block coordinates
     * @param level the level, 1 to 5
     */
    public EnergyDevice(String id, DeviceKind kind, Location block, int level) {
        this.id = id;
        this.kind = kind;
        this.block = new Location(block.getWorld(), block.getBlockX(), block.getBlockY(), block.getBlockZ());
        this.level = level;
    }

    /** The device id. */
    public String id() {
        return id;
    }

    /** The device kind. */
    public DeviceKind kind() {
        return kind;
    }

    /** The level, 1 to 5. */
    public int level() {
        return level;
    }

    /** The component id of the device's item, such as pylon_2 or accumulator_1. A generator names its own item. */
    public String componentId() {
        return switch (kind) {
            case PYLON -> EnergyLevels.componentId(EnergyLevels.PYLON, level);
            case ACCUMULATOR -> EnergyLevels.componentId(EnergyLevels.ACCUMULATOR, level);
            case GENERATOR -> throw new IllegalStateException("a generator is a GeneratorDevice, which names its own item");
            case CONSUMER -> throw new UnsupportedOperationException("no device item for " + kind);
        };
    }

    /**
     * The number of blocks the device stands: 2 for a tall device, whose block above the anchor is a part of it, 1 otherwise. The
     * block is the anchor, the lower block.
     */
    public int height() {
        return Footprint.heightOf(componentId());
    }

    /** A copy of the block the device stands in. */
    public Location block() {
        return block.clone();
    }

    /** The id of the world the device stands in. */
    public UUID world() {
        return block.getWorld().getUID();
    }

    /** The device's block as a position of the energy model. */
    public Pos pos() {
        return new Pos(block.getBlockX(), block.getBlockY(), block.getBlockZ());
    }

    /** The world the device stands in, or null when that world is not loaded. */
    public World loadedWorld() {
        return block.getWorld();
    }

    /** True when the chunk the device stands in is loaded, so its entities can exist. */
    public boolean isLoaded() {
        World world = block.getWorld();
        return world != null && world.isChunkLoaded(block.getBlockX() >> 4, block.getBlockZ() >> 4);
    }

    /** True when the device stands in the given chunk. */
    public boolean inChunk(Chunk chunk) {
        return block.getWorld() != null && block.getWorld().equals(chunk.getWorld())
                && block.getBlockX() >> 4 == chunk.getX() && block.getBlockZ() >> 4 == chunk.getZ();
    }

    /** The item display's id, or null when none is shown. */
    public UUID display() {
        return display;
    }

    void setDisplay(UUID display) {
        this.display = display;
    }

    /** The item display of the upper block of a tall device, or null when none is shown or the device is one block tall. */
    public UUID topDisplay() {
        return topDisplay;
    }

    void setTopDisplay(UUID topDisplay) {
        this.topDisplay = topDisplay;
    }

    /** The interaction entity's id, or null when none is made. */
    public UUID interaction() {
        return interaction;
    }

    void setInteraction(UUID interaction) {
        this.interaction = interaction;
    }

    /** The hologram over the device, or null when it has none. */
    public Hologram hologram() {
        return hologram;
    }

    void setHologram(Hologram hologram) {
        this.hologram = hologram;
    }
}
