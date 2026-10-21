package io.corefactory.spigot.hologram;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One hologram: a stack of invisible armour stands, one per line. Each stand's custom name carries its line's text, and the
 * lines go downward from the position, {@link #LINE_SPACING} blocks apart. Only its viewers see it, the players the
 * {@link HologramService} has sent it to. Create holograms through {@link HologramService#create}.
 */
public final class Hologram {

    /** The distance in blocks between one line and the next, going down. */
    public static final double LINE_SPACING = 0.25;

    private final List<Integer> entityIds;
    private final List<UUID> uuids;
    private final Location position;
    private final Set<UUID> viewers = new HashSet<>();
    private List<String> lines;

    Hologram(List<Integer> entityIds, List<UUID> uuids, Location position, List<String> lines) {
        this.entityIds = List.copyOf(entityIds);
        this.uuids = List.copyOf(uuids);
        this.position = position;
        this.lines = fit(lines);
    }

    /** The number of lines, fixed when the hologram is made. */
    public int lineCount() {
        return entityIds.size();
    }

    /** The entity id a line's armour stand uses. It exists only on the wire, never on the server. */
    public int entityId(int line) {
        return entityIds.get(line);
    }

    /** The unique id a line's armour stand is spawned with. */
    UUID uuid(int line) {
        return uuids.get(line);
    }

    /** A copy of the point the top line stands at. */
    public Location position() {
        return position.clone();
    }

    /** A copy of the point a line stands at: the position, lowered by the line's number times {@link #LINE_SPACING}. */
    public Location linePosition(int line) {
        return position.clone().subtract(0, LINE_SPACING * line, 0);
    }

    /** The text of a line, with ampersand colour codes. */
    public String text(int line) {
        return lines.get(line);
    }

    /** The text of every line, top first, with ampersand colour codes. */
    public List<String> lines() {
        return lines;
    }

    /**
     * Sets the text of the lines. The number of lines is fixed when the hologram is made: a longer list is cut to it, and a
     * shorter one is padded with empty lines. The viewers see it at the next name update.
     */
    public void setLines(List<String> text) {
        this.lines = fit(text);
    }

    World world() {
        return position.getWorld();
    }

    /** The viewers the hologram is shown to. The set is live: the service adds and removes viewers through it. */
    Set<UUID> viewers() {
        return viewers;
    }

    private List<String> fit(List<String> text) {
        List<String> fitted = new ArrayList<>();
        for (int line = 0; line < entityIds.size(); line++) {
            String value = text != null && line < text.size() ? text.get(line) : null;
            fitted.add(value == null ? "" : value);
        }
        return List.copyOf(fitted);
    }
}
