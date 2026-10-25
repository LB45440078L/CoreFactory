package io.corefactory.paper.vertical;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.vertical.Belt;
import io.corefactory.core.vertical.VerticalDir;

import java.util.UUID;

/**
 * One vertical transport belt as the service keeps it: its block, its level, its direction, and the player who placed it. Its
 * direction is the direction of its whole tower, and changes only through the service. Its display and interaction are not saved.
 */
public final class VerticalBelt {

    private final String id;
    private final UUID world;
    private final Pos pos;
    private final int level;
    private final UUID owner;
    private VerticalDir direction;
    private UUID display;
    private UUID interaction;

    /**
     * @param id        the belt's id: a random UUID
     * @param world     the id of the world it stands in
     * @param pos       its block
     * @param level     its level, 1 to 3
     * @param direction its carry direction
     * @param owner     the id of the player who placed it, or null when none is known
     */
    VerticalBelt(String id, UUID world, Pos pos, int level, VerticalDir direction, UUID owner) {
        this.id = id;
        this.world = world;
        this.pos = pos;
        this.level = level;
        this.direction = direction;
        this.owner = owner;
    }

    /** The belt's id. */
    public String id() {
        return id;
    }

    /** The id of the world it stands in. */
    public UUID world() {
        return world;
    }

    /** Its block. */
    public Pos pos() {
        return pos;
    }

    /** Its level, 1 to 3. */
    public int level() {
        return level;
    }

    /** The id of the player who placed it, or null when none is known. */
    public UUID owner() {
        return owner;
    }

    /** Its carry direction, the direction of its tower. */
    public VerticalDir direction() {
        return direction;
    }

    /** The belt as the tower rules read it. */
    Belt belt() {
        return new Belt(direction, level);
    }

    void setDirection(VerticalDir direction) {
        this.direction = direction;
    }

    /** The id of the item display that shows the belt, or null when none is shown. */
    public UUID display() {
        return display;
    }

    void setDisplay(UUID display) {
        this.display = display;
    }

    /** The id of the interaction that takes its clicks, or null when none is made. */
    public UUID interaction() {
        return interaction;
    }

    void setInteraction(UUID interaction) {
        this.interaction = interaction;
    }
}
