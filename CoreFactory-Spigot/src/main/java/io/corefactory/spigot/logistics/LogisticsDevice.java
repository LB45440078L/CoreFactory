package io.corefactory.spigot.logistics;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.BeltShape;
import io.corefactory.core.logistics.Dir;
import io.corefactory.core.logistics.LogisticsLevel;
import io.corefactory.spigot.hologram.Hologram;

import java.util.UUID;

/**
 * A belt, picker or splitter as the service keeps it: its block, its direction, its level, its owner (the player who placed it, or
 * null for a device saved before owners were kept) and, for a splitter, its filter. Its entities are its item display, its
 * interaction and, for a picker or splitter, its hologram; they are recreated when they go missing. The configuration is saved; the
 * entities are not.
 */
public final class LogisticsDevice {

    private final String id;
    private final LogisticsIds.Kind kind;
    private final UUID world;
    private final Pos pos;
    private final LogisticsLevel level;
    private final Dir dir;
    private final UUID owner;
    private SplitterFilter filter = SplitterFilter.empty();
    private BeltShape shape = BeltShape.STRAIGHT;
    private Dir input;
    private UUID display;
    private UUID interaction;
    private Hologram hologram;

    /**
     * @param id    the device id: a random UUID for a belt, picker or splitter
     * @param kind  the kind
     * @param world the id of the world the device stands in
     * @param pos   the device's block
     * @param level its level
     * @param dir   for a belt, its flow; for a picker or a splitter, the direction from the device to its input (for a splitter,
     *              the direction its items travel)
     * @param owner the id of the player who placed the device, or null when none is known
     */
    public LogisticsDevice(String id, LogisticsIds.Kind kind, UUID world, Pos pos, LogisticsLevel level, Dir dir, UUID owner) {
        this.id = id;
        this.kind = kind;
        this.world = world;
        this.pos = pos;
        this.level = level;
        this.dir = dir;
        this.owner = owner;
    }

    /** The device id. */
    public String id() {
        return id;
    }

    /** The kind: belt, picker or splitter. */
    public LogisticsIds.Kind kind() {
        return kind;
    }

    /** The id of the world the device stands in. */
    public UUID world() {
        return world;
    }

    /** The device's block. */
    public Pos pos() {
        return pos;
    }

    /** The level. */
    public LogisticsLevel level() {
        return level;
    }

    /** For a belt, its flow; for a picker or splitter, its input direction. */
    public Dir dir() {
        return dir;
    }

    /**
     * The id of the player who placed the device, or null when none is stored. Its container deliveries are checked against that
     * player's trust in the claims there.
     */
    public UUID owner() {
        return owner;
    }

    /** The filter of a splitter; empty for any other kind. */
    public SplitterFilter filter() {
        return filter;
    }

    void setFilter(SplitterFilter filter) {
        this.filter = filter;
    }

    /** The shape of a belt as last computed; a straight belt for any other kind. */
    public BeltShape shape() {
        return shape;
    }

    void setShape(BeltShape shape) {
        this.shape = shape;
    }

    /** The side a belt takes its items from, as last computed; null for any other kind. */
    public Dir input() {
        return input;
    }

    void setInput(Dir input) {
        this.input = input;
    }

    /** The item display's id, or null when none is shown. */
    public UUID display() {
        return display;
    }

    void setDisplay(UUID display) {
        this.display = display;
    }

    /** The interaction entity's id, or null when none is made. */
    public UUID interaction() {
        return interaction;
    }

    void setInteraction(UUID interaction) {
        this.interaction = interaction;
    }

    /** The hologram over a picker or splitter, or null when it has none. */
    public Hologram hologram() {
        return hologram;
    }

    void setHologram(Hologram hologram) {
        this.hologram = hologram;
    }

    /** The key of the device's block: the world and the three coordinates. */
    public String blockKey() {
        return world + ":" + pos.x() + ":" + pos.y() + ":" + pos.z();
    }
}
