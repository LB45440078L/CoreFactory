package io.corefactory.paper.oil;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.oil.FluidTank;
import io.corefactory.core.oil.OilPart;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.WaterOrientation;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A placed oil tube as the service keeps it: its part, its block, the player who placed it, a splitter's input face (or null for a pipe),
 * its output, and the tank of fluid it holds. Its entities are its display, its arms and its interaction; they are recreated when they go
 * missing. The configuration and the tank are saved; the entities and the solved shape are not.
 */
public final class OilDevice {

    private final String id;
    private final OilPart part;
    private final UUID owner;
    private final UUID world;
    private final Pos pos;
    private final Face face;
    private final FluidTank tank;
    private Face output;
    private Set<Face> shown = Set.of();
    private UUID display;
    private final Map<Face, UUID> arms = new EnumMap<>(Face.class);
    private UUID interaction;

    /**
     * @param id                 the device id, a random UUID
     * @param part               the part
     * @param owner              the id of the player who placed it
     * @param world              the id of the world it stands in
     * @param pos                its block
     * @param face               a splitter's input face; null for a pipe
     * @param tubeCapacityLitres the litres its tank holds, from the oil settings
     */
    public OilDevice(String id, OilPart part, UUID owner, UUID world, Pos pos, Face face, double tubeCapacityLitres) {
        this.id = id;
        this.part = part;
        this.owner = owner;
        this.world = world;
        this.pos = pos;
        this.face = face;
        this.tank = new FluidTank(tubeCapacityLitres);
        this.output = part.splitter() && face != null ? defaultOutput(face) : null;
    }

    /** The output a splitter gets when none is given: across a horizontal input, and south for a vertical one. */
    static Face defaultOutput(Face input) {
        return input.horizontal() ? WaterOrientation.defaultOutput(input) : Face.SOUTH;
    }

    /** The device id. */
    public String id() {
        return id;
    }

    /** The part: pipe or splitter. */
    public OilPart part() {
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

    /** A splitter's input face; null for a pipe. An input may be vertical. */
    public Face face() {
        return face;
    }

    /** A splitter's output: a face on another line than its input. Null for a pipe. */
    public Face output() {
        return output;
    }

    void setOutput(Face output) {
        this.output = output;
    }

    /**
     * The tank of the tube: the fluid it holds and the litres. Its fluid and litres are saved with the tube.
     */
    FluidTank tank() {
        return tank;
    }

    /** Empties the tank: the fluid it holds is given out in full, so the tube holds none afterwards. */
    void empty() {
        tank.fluid().ifPresent(fluid -> tank.draw(fluid, tank.litres()));
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
