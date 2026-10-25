package io.corefactory.paper.vertical;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Item;
import io.corefactory.core.vertical.VerticalDir;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes {@code vertical.yml} in the plugin's data folder. The file holds a {@code version}, the belts as numbered entries,
 * and the items carried on the towers as numbered entries, each at its height in the world. A write goes to a {@code .tmp} file that is
 * then moved over the real one, so a crash during a write leaves the last good file. An entry that cannot be read is skipped and
 * reported.
 */
public final class VerticalStore {

    /** The version written now. */
    public static final int VERSION = 1;

    private static final String VERSION_KEY = "version";
    private static final String BELTS = "belts";
    private static final String ITEMS = "items";

    /**
     * A belt as saved.
     *
     * @param id        the belt's id
     * @param owner     the player who placed it, or null
     * @param world     the world it stands in
     * @param pos       its block
     * @param level     its level
     * @param direction its carry direction
     */
    public record Saved(String id, UUID owner, UUID world, Pos pos, int level, VerticalDir direction) {
    }

    /**
     * An item carried on a tower as saved: its column, and its height in the world.
     *
     * @param world the world
     * @param x     the column's x
     * @param y     the item's height: its carry coordinate on the tower, measured in the world
     * @param z     the column's z
     * @param item  the item
     */
    public record SavedItem(UUID world, int x, double y, int z, Item item) {
    }

    /**
     * The whole file.
     *
     * @param belts the belts
     * @param items the items on the towers
     */
    public record State(List<Saved> belts, List<SavedItem> items) {

        /** An empty state. */
        public static State empty() {
            return new State(List.of(), List.of());
        }
    }

    private final File file;
    private final Logger log;

    /**
     * @param file the store file, normally {@code vertical.yml} in the data folder
     * @param log  receives the skipped entries and any failed write
     */
    public VerticalStore(File file, Logger log) {
        this.file = file;
        this.log = log;
    }

    /** The saved state, or an empty one when the file is missing or cannot be read. */
    public State read() {
        if (!file.isFile()) {
            return State.empty();
        }
        try {
            return decode(Files.readString(file.toPath(), StandardCharsets.UTF_8), log::warning);
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not read " + file.getName() + ". Vertical belts start empty.", e);
            return State.empty();
        }
    }

    /** Writes the state atomically: to a temporary file first, then moved over the store file. */
    public void write(State state) {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        File temporary = new File(parent, file.getName() + ".tmp");
        try {
            Files.writeString(temporary.toPath(), encode(state), StandardCharsets.UTF_8);
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not save " + file.getName(), e);
        }
    }

    /** The text of a store file holding the given state. */
    static String encode(State state) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(VERSION_KEY, VERSION);
        int index = 0;
        for (Saved belt : state.belts()) {
            String base = BELTS + "." + index++ + ".";
            yaml.set(base + "id", belt.id());
            if (belt.owner() != null) {
                yaml.set(base + "owner", belt.owner().toString());
            }
            yaml.set(base + "world", belt.world().toString());
            yaml.set(base + "x", belt.pos().x());
            yaml.set(base + "y", belt.pos().y());
            yaml.set(base + "z", belt.pos().z());
            yaml.set(base + "level", belt.level());
            yaml.set(base + "direction", belt.direction().name());
        }
        index = 0;
        for (SavedItem item : state.items()) {
            String base = ITEMS + "." + index++ + ".";
            yaml.set(base + "world", item.world().toString());
            yaml.set(base + "x", item.x());
            yaml.set(base + "y", item.y());
            yaml.set(base + "z", item.z());
            yaml.set(base + "kind", item.item().kind());
            yaml.set(base + "id", item.item().id());
        }
        return yaml.saveToString();
    }

    /**
     * The state of a store file's text. An entry with a missing or invalid field is skipped and reported.
     *
     * @param text     the file's text
     * @param warnings receives one message for each skipped entry
     */
    static State decode(String text, Consumer<String> warnings) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(text));
        List<Saved> belts = new ArrayList<>();
        for (ConfigurationSection entry : entries(yaml, BELTS)) {
            Optional<Saved> saved = belt(entry);
            if (saved.isEmpty()) {
                warnings.accept("vertical.yml: a vertical belt entry is not valid and is skipped.");
            } else {
                belts.add(saved.get());
            }
        }
        List<SavedItem> items = new ArrayList<>();
        for (ConfigurationSection entry : entries(yaml, ITEMS)) {
            Optional<SavedItem> saved = carried(entry);
            if (saved.isEmpty()) {
                warnings.accept("vertical.yml: an item entry on a tower is not valid and is skipped.");
            } else {
                items.add(saved.get());
            }
        }
        return new State(belts, items);
    }

    private static Optional<Saved> belt(ConfigurationSection entry) {
        UUID world = uuidOf(entry.getString("world"));
        UUID owner = entry.contains("owner") ? uuidOf(entry.getString("owner")) : null;
        Optional<VerticalDir> direction = directionOf(entry.getString("direction"));
        int level = entry.getInt("level", 0);
        if (entry.getString("id") == null || world == null || direction.isEmpty() || level < 1 || level > VerticalIds.MAX_LEVEL
                || (entry.contains("owner") && owner == null)) {
            return Optional.empty();
        }
        return Optional.of(new Saved(entry.getString("id"), owner, world,
                new Pos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z")), level, direction.get()));
    }

    private static Optional<SavedItem> carried(ConfigurationSection entry) {
        UUID world = uuidOf(entry.getString("world"));
        try {
            Item item = new Item(entry.getString("kind", ""), entry.getString("id", ""));
            if (world == null) {
                return Optional.empty();
            }
            return Optional.of(new SavedItem(world, entry.getInt("x"), entry.getDouble("y"), entry.getInt("z"), item));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    private static List<ConfigurationSection> entries(YamlConfiguration yaml, String key) {
        List<ConfigurationSection> entries = new ArrayList<>();
        ConfigurationSection section = yaml.getConfigurationSection(key);
        if (section == null) {
            return entries;
        }
        for (String name : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(name);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static Optional<VerticalDir> directionOf(String text) {
        if (text == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(VerticalDir.valueOf(text.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static UUID uuidOf(String text) {
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
