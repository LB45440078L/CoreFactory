package io.corefactory.paper.screen;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.text.Text;
import io.corefactory.core.water.Face;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * The small screen of a device: a dark frame and a black panel on its front face, with a text display in front of the panel that shows
 * the device's rows. All three are display entities, owned by the device: the device makes them, shows its rows, and removes them. The
 * entities are marked with the anchor of their device, so entities left by an earlier run are found and removed. A screen remembers the
 * text it shows and writes it only when it changes.
 */
public final class DeviceScreen {

    /** The most rows a screen shows: the rows that fit the panel at the text scale, with a little room at its ends. */
    public static final int MAX_ROWS = 11;

    private static final NamespacedKey MARK = new NamespacedKey(ItemFactory.NAMESPACE, "device_screen");

    private final UUID panel;
    private final UUID text;
    private final UUID bezel;
    private List<String> shown;

    private DeviceScreen(UUID panel, UUID text, UUID bezel) {
        this.panel = panel;
        this.text = text;
        this.bezel = bezel;
    }

    /**
     * Makes a screen on a device's front face, with its frame, its panel and its text, and shows no lines yet.
     *
     * @param world  the world the device stands in
     * @param block  the device's anchor block, the lower block
     * @param front  the horizontal face the screen is on
     * @param height the number of blocks the device stands
     * @return the new screen
     */
    public static DeviceScreen create(World world, Location block, Face front, int height) {
        ScreenGeometry.Layout layout = ScreenGeometry.of(new Pos(block.getBlockX(), block.getBlockY(), block.getBlockZ()), front, height);
        String anchor = anchorKey(block);
        BlockDisplay bezel = world.spawn(at(world, layout.bezel()), BlockDisplay.class, entity -> {
            entity.setBlock(Material.POLISHED_BLACKSTONE.createBlockData());
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setTransformation(new Transformation(
                    translation(layout.bezelTranslation()),
                    rotation(layout.yaw()),
                    new Vector3f((float) ScreenGeometry.FRAME_WIDTH, (float) ScreenGeometry.FRAME_HEIGHT,
                            (float) ScreenGeometry.FRAME_THICKNESS),
                    new Quaternionf()));
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(MARK, PersistentDataType.STRING, anchor);
        });
        BlockDisplay panel = world.spawn(at(world, layout.panel()), BlockDisplay.class, entity -> {
            entity.setBlock(Material.BLACK_CONCRETE.createBlockData());
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setTransformation(new Transformation(
                    translation(layout.translation()),
                    rotation(layout.yaw()),
                    new Vector3f((float) ScreenGeometry.WIDTH, (float) ScreenGeometry.HEIGHT, (float) ScreenGeometry.THICKNESS),
                    new Quaternionf()));
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(MARK, PersistentDataType.STRING, anchor);
        });
        float scale = (float) ScreenGeometry.TEXT_SCALE;
        TextDisplay text = world.spawn(at(world, layout.text()), TextDisplay.class, entity -> {
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setShadowed(false);
            entity.setSeeThrough(false);
            entity.setDefaultBackground(false);
            entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            entity.setLineWidth(ScreenGeometry.LINE_WIDTH);
            entity.setTransformation(new Transformation(new Vector3f(), rotation(layout.yaw()),
                    new Vector3f(scale, scale, scale), new Quaternionf()));
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(MARK, PersistentDataType.STRING, anchor);
        });
        return new DeviceScreen(panel.getUniqueId(), text.getUniqueId(), bezel.getUniqueId());
    }

    /**
     * A screen whose entities were saved with their device. Any id may be null, when the device saved none; then {@link #present}
     * is false and the device makes the screen again.
     *
     * @param panel the panel's entity id, or null
     * @param text  the text's entity id, or null
     * @param bezel the frame's entity id, or null
     */
    public static DeviceScreen restore(UUID panel, UUID text, UUID bezel) {
        return new DeviceScreen(panel, text, bezel);
    }

    /**
     * The anchor a screen entity was made for, in the form {@code world:x:y:z} that a device registry keys its devices by, or null when the
     * entity is not a screen.
     */
    public static String anchorOf(Entity entity) {
        return entity.getPersistentDataContainer().get(MARK, PersistentDataType.STRING);
    }

    /** The panel's entity id, or null when none is kept. */
    public UUID panel() {
        return panel;
    }

    /** The text's entity id, or null when none is kept. */
    public UUID text() {
        return text;
    }

    /** The frame's entity id, or null when none is kept. */
    public UUID bezel() {
        return bezel;
    }

    /** True when the frame, the panel and the text all exist in the world. */
    public boolean present() {
        return live(bezel) != null && live(panel) != null && live(text) != null;
    }

    /**
     * Shows the rows on the text: the first {@link #MAX_ROWS}, each read by {@link Text#parse}. The text is written only when it differs
     * from what the screen shows. Does nothing while the text is not in the world.
     *
     * @param rows the rows, top first, in MiniMessage or with ampersand codes
     */
    public void setLines(List<String> rows) {
        List<String> next = List.copyOf(rows.subList(0, Math.min(rows.size(), MAX_ROWS)));
        if (next.equals(shown)) {
            return;
        }
        if (!(live(text) instanceof TextDisplay display)) {
            return;
        }
        display.text(join(next));
        shown = next;
    }

    /** Removes the frame, the panel and the text from the world, whichever of them exist. */
    public void remove() {
        removeEntity(bezel);
        removeEntity(panel);
        removeEntity(text);
        shown = null;
    }

    /** The anchor key of a block in the form {@code world:x:y:z}, the form the device registry keys by. */
    static String anchorKey(Location block) {
        return block.getWorld().getUID() + ":" + block.getBlockX() + ":" + block.getBlockY() + ":" + block.getBlockZ();
    }

    private static Location at(World world, ScreenGeometry.Vec point) {
        return new Location(world, point.x(), point.y(), point.z());
    }

    private static Vector3f translation(ScreenGeometry.Vec translation) {
        return new Vector3f((float) translation.x(), (float) translation.y(), (float) translation.z());
    }

    private static Quaternionf rotation(double yaw) {
        return new Quaternionf().rotationY((float) yaw);
    }

    private static Component join(List<String> lines) {
        List<Component> parts = lines.stream().map(Text::parse).toList();
        return Component.join(JoinConfiguration.separator(Component.newline()), parts);
    }

    private static Entity live(UUID id) {
        if (id == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(id);
        return entity != null && entity.isValid() ? entity : null;
    }

    private static void removeEntity(UUID id) {
        Entity entity = live(id);
        if (entity != null) {
            entity.remove();
        }
    }
}
