package io.corefactory.paper.hologram;

import io.corefactory.core.hologram.HologramLayout;
import io.corefactory.core.hologram.Tone;
import io.corefactory.core.text.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Owns the holograms. Each hologram is one {@link TextDisplay}, spawned non-persistent at its position: the game draws it to every player
 * within its view range, so the plugin sends no packets and checks no players. The text and the background are written to the display only
 * when they change. A display is not saved with its chunk, so one that a chunk unload took is spawned again when the chunk loads.
 *
 * <p>The holograms of devices are hidden from every player until {@link #setDevicesVisible} turns them on; a pylon's hologram is shown at
 * all times. The setting is kept in memory only, so it is off again after a restart.
 */
public final class HologramService implements Listener {

    /** The background of a working device: a faint, desaturated green, so the text on it stays easy to read. */
    private static final Color WORKING_BACKGROUND = Color.fromARGB(90, 70, 120, 84);

    /** The background of a stuck device: a faint, desaturated red, so the text on it stays easy to read. */
    private static final Color STUCK_BACKGROUND = Color.fromARGB(100, 130, 74, 74);

    /** No background, the same as a hologram with no tone. */
    private static final Color NO_BACKGROUND = Color.fromARGB(0, 0, 0, 0);

    private final boolean configured;
    private final float viewRange;
    private final Set<Hologram> live = new LinkedHashSet<>();
    private boolean devicesVisible;

    /**
     * @param enabled      false to create no holograms
     * @param viewDistance the distance in blocks within which a player sees a hologram, at least 1
     */
    public HologramService(boolean enabled, double viewDistance) {
        this.configured = enabled;
        this.viewRange = HologramLayout.viewRange(viewDistance);
    }

    /**
     * The point a hologram's bottom line stands at, above a device: the top of the device's block column plus a gap, centred on the block.
     *
     * @param block  the block the device is anchored at
     * @param height the number of blocks the device stands, from {@link io.corefactory.core.device.Footprint}
     */
    public static Location spotAbove(Location block, int height) {
        return block.clone().add(0.5, height + HologramLayout.GAP, 0.5);
    }

    /** True when holograms are configured on. */
    public boolean enabled() {
        return configured;
    }

    /**
     * Nothing to start: a display needs no repeating task, because its text and tone are written when they change and the game draws it.
     * Kept so the plugin's start-up reads as before.
     */
    public void start() {
        // No repeating work: see the class comment.
    }

    /** True when the holograms of devices are shown to everyone. Off until {@link #setDevicesVisible} turns them on. */
    public boolean devicesVisible() {
        return devicesVisible;
    }

    /**
     * Shows or hides the holograms of devices for everyone. Every live device hologram's display takes the setting at once, and a display
     * made later takes it when it is spawned. Pylon holograms are not changed.
     *
     * @param visible true to show the holograms of devices to every player in range
     */
    public void setDevicesVisible(boolean visible) {
        devicesVisible = visible;
        for (Hologram hologram : live) {
            Entity display = displayOf(hologram);
            if (!hologram.pylon() && display != null) {
                display.setVisibleByDefault(visible);
            }
        }
    }

    /**
     * Creates the hologram of a device, one line per text, from the point up, and spawns its display. It is hidden until the holograms of
     * devices are shown. Empty when holograms are off, the point has no world, or there are no lines.
     *
     * @param bottom the point the bottom line stands at
     * @param lines  the text of each line, top first
     */
    public Optional<Hologram> create(Location bottom, List<String> lines) {
        return make(bottom, lines, false);
    }

    /**
     * Creates the hologram of a pylon, one line per text, from the point up, and spawns its display. It is shown at all times. Empty when
     * holograms are off, the point has no world, or there are no lines.
     *
     * @param bottom the point the bottom line stands at
     * @param lines  the text of each line, top first
     */
    public Optional<Hologram> createPylon(Location bottom, List<String> lines) {
        return make(bottom, lines, true);
    }

    private Optional<Hologram> make(Location bottom, List<String> lines, boolean pylon) {
        if (!enabled() || bottom.getWorld() == null || lines.isEmpty()) {
            return Optional.empty();
        }
        Hologram[] made = new Hologram[1];
        Hologram hologram = new Hologram(bottom.clone(), lines, pylon, () -> apply(made[0]));
        made[0] = hologram;
        live.add(hologram);
        spawn(hologram);
        return Optional.of(hologram);
    }

    /** Removes a hologram's display. Does nothing for one already removed. */
    public void remove(Hologram hologram) {
        if (!live.remove(hologram)) {
            return;
        }
        Entity display = displayOf(hologram);
        if (display != null) {
            display.remove();
        }
        hologram.setDisplay(null);
    }

    /** Removes every display. Called when the plugin is disabled. */
    public void shutdown() {
        for (Hologram hologram : new ArrayList<>(live)) {
            remove(hologram);
        }
    }

    /** A chunk loaded: a hologram in it whose display is gone is spawned again. */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        for (Hologram hologram : new ArrayList<>(live)) {
            World world = hologram.world();
            if (world == null || !world.equals(chunk.getWorld())) {
                continue;
            }
            Location at = hologram.centre();
            if ((at.getBlockX() >> 4) == chunk.getX() && (at.getBlockZ() >> 4) == chunk.getZ() && displayOf(hologram) == null) {
                spawn(hologram);
            }
        }
    }

    /** Writes a hologram's text and tone to its display, each only when it differs from what the display shows. */
    private void apply(Hologram hologram) {
        Entity entity = displayOf(hologram);
        if (!(entity instanceof TextDisplay display)) {
            spawn(hologram);
            return;
        }
        String joined = hologram.joined();
        if (!joined.equals(hologram.shown())) {
            display.text(text(hologram.lines()));
            hologram.setShown(joined);
        }
        if (hologram.tone() != hologram.shownTone()) {
            display.setBackgroundColor(colourOf(hologram.tone()));
            hologram.setShownTone(hologram.tone());
        }
    }

    /** Spawns a hologram's display at its centre, if its chunk is loaded, and writes its text and tone to it. */
    private void spawn(Hologram hologram) {
        World world = hologram.world();
        if (world == null) {
            return;
        }
        Location at = hologram.centre();
        if (!world.isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) {
            return;
        }
        TextDisplay display = world.spawn(at, TextDisplay.class, shown -> {
            shown.setPersistent(false);
            shown.setVisibleByDefault(hologram.pylon() || devicesVisible);
            shown.setBillboard(Display.Billboard.CENTER);
            shown.setAlignment(TextDisplay.TextAlignment.CENTER);
            shown.setShadowed(false);
            shown.setSeeThrough(false);
            shown.setDefaultBackground(false);
            shown.setBackgroundColor(colourOf(hologram.tone()));
            shown.setViewRange(viewRange);
            shown.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                    new Vector3f(HologramLayout.SCALE), new Quaternionf()));
            shown.text(text(hologram.lines()));
        });
        hologram.setDisplay(display.getUniqueId());
        hologram.setShown(hologram.joined());
        hologram.setShownTone(hologram.tone());
    }

    /** The background colour of a tone. */
    static Color colourOf(Tone tone) {
        return switch (tone) {
            case WORKING -> WORKING_BACKGROUND;
            case STUCK -> STUCK_BACKGROUND;
            case NEUTRAL -> NO_BACKGROUND;
        };
    }

    /** The lines as one component: each line read by {@link Text#parse}, the lines separated by newlines. */
    static Component text(List<String> lines) {
        List<Component> parts = new ArrayList<>(lines.size());
        for (String line : lines) {
            parts.add(Text.parse(line));
        }
        return Component.join(JoinConfiguration.separator(Component.newline()), parts);
    }

    /** The display of a hologram, when it is spawned and still valid. */
    private static Entity displayOf(Hologram hologram) {
        if (hologram.display() == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(hologram.display());
        return entity != null && entity.isValid() ? entity : null;
    }
}
