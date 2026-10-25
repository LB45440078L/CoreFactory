package io.corefactory.paper.water;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.PumpFuel;
import io.corefactory.core.water.WaterOrientation;
import io.corefactory.core.water.WaterPart;
import io.corefactory.paper.hologram.Hologram;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A placed water block as the service keeps it: its part, its block, the player who placed it, its face (a splitter's input, a pump's
 * front, or null for a pipe), its level and its fuel. A pump also keeps its source count and whether it runs. Its entities are its
 * display, its interaction and, for a pump, its hologram; they are recreated when they go missing. The configuration is saved; the
 * entities and the solved values are not.
 */
public final class WaterDevice {

    private final String id;
    private final WaterPart part;
    private final UUID owner;
    private final UUID world;
    private final Pos pos;
    private final Face face;
    private Face output;
    private int level;
    private PumpFuel.Burn burn;
    private int source;
    private long sourceCheckedAt = -1L;
    private boolean running;
    private Set<Face> shown = Set.of();
    private UUID display;
    private UUID topDisplay;
    private final Map<Face, UUID> arms = new EnumMap<>(Face.class);
    private UUID interaction;
    private Hologram hologram;

    /**
     * @param id    the device id, a random UUID
     * @param part  the part
     * @param owner the id of the player who placed it
     * @param world the id of the world it stands in
     * @param pos   its block
     * @param face  a splitter's input face or a pump's front, horizontal; null for a pipe
     * @param level its level: 1 for anything but a pump
     * @param burn  the fuel it holds
     */
    public WaterDevice(String id, WaterPart part, UUID owner, UUID world, Pos pos, Face face, int level, PumpFuel.Burn burn) {
        this.id = id;
        this.part = part;
        this.owner = owner;
        this.world = world;
        this.pos = pos;
        this.face = face;
        this.level = level;
        this.burn = burn;
        this.output = part.splitter() && face != null ? defaultOutput(face) : null;
    }

    /** The output a splitter gets when none is given: across a horizontal input, and south for a vertical one. */
    private static Face defaultOutput(Face input) {
        return input.horizontal() ? WaterOrientation.defaultOutput(input) : Face.SOUTH;
    }

    /** A splitter's second axis: the face its outputs lie across, on another line than its input. Null for other parts. */
    public Face output() {
        return output;
    }

    void setOutput(Face output) {
        this.output = output;
    }

    /** The device id. */
    public String id() {
        return id;
    }

    /** The part: pipe, pump or splitter. */
    public WaterPart part() {
        return part;
    }

    /** The id of the player who placed it. */
    public UUID owner() {
        return owner;
    }

    /** The id of the world it stands in. */
    public UUID world() {
        return world;
    }

    /** Its block. */
    public Pos pos() {
        return pos;
    }

    /** A splitter's input face, or a pump's front; null for a pipe. A splitter's input may be vertical. */
    public Face face() {
        return face;
    }

    /** Its level: a pump's 1 to 5, and 1 for any other part. */
    public int level() {
        return level;
    }

    void setLevel(int level) {
        this.level = level;
    }

    /** The fuel it holds, at levels 1 and 2. */
    public PumpFuel.Burn burn() {
        return burn;
    }

    void setBurn(PumpFuel.Burn burn) {
        this.burn = burn;
    }

    /** The water blocks its source counted at the last check, up to the cap. */
    public int source() {
        return source;
    }

    /** The tick of the last source check, or -1 when none was made. */
    long sourceCheckedAt() {
        return sourceCheckedAt;
    }

    void setSource(int source, long tick) {
        this.source = source;
        this.sourceCheckedAt = tick;
    }

    /** True when the source reaches the minimum. */
    public boolean hasSource(int minimum) {
        return source >= minimum;
    }

    /** True when the pump runs in the current tick. */
    public boolean running() {
        return running;
    }

    void setRunning(boolean running) {
        this.running = running;
    }

    /** The faces where the solved network joined the block when the model was last drawn. */
    Set<Face> shown() {
        return shown;
    }

    void setShown(Set<Face> shown) {
        this.shown = shown;
    }

    /** The display entity's id, or null when none is shown. */
    UUID display() {
        return display;
    }

    void setDisplay(UUID display) {
        this.display = display;
    }

    /** The display of a tall pump's upper block, or null when there is none. */
    UUID topDisplay() {
        return topDisplay;
    }

    void setTopDisplay(UUID topDisplay) {
        this.topDisplay = topDisplay;
    }

    /** The arm displays of a junction, by the face each arm reaches. Empty for any other pipe. */
    Map<Face, UUID> arms() {
        return arms;
    }

    /** The interaction entity's id, or null when none is made. */
    UUID interaction() {
        return interaction;
    }

    void setInteraction(UUID interaction) {
        this.interaction = interaction;
    }

    /** The hologram over a pump, or null. */
    Hologram hologram() {
        return hologram;
    }

    void setHologram(Hologram hologram) {
        this.hologram = hologram;
    }

    /** The key of a block: the world id and the three coordinates. */
    public String blockKey() {
        return key(world, pos);
    }

    /**
     * The key of a block in a world, the same form as {@link #blockKey()}.
     *
     * @param world the id of the world
     * @param pos   the block
     */
    public static String key(UUID world, Pos pos) {
        return world + ":" + pos.x() + ":" + pos.y() + ":" + pos.z();
    }
}
