package io.corefactory.paper.logistics;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Dir;
import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.LogisticsLevel;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The format of {@code logistics.yml}: the belts, pickers, splitters and belt duplexes, and the items on the belts. Each list is numbered
 * from 0. A splitter keeps its two filter lists and the item it holds, and a belt duplex the item it holds; an item is written as
 * {@code MATERIAL:CHARCOAL} or
 * {@code COMPONENT:belt_1}. A belt, picker or splitter may keep the id of the player who placed it under {@code owner}; an entry
 * without that key has no owner. The text is read and written as a string, so the round trip is tested without a server. An entry
 * that cannot be read is skipped and reported.
 */
public final class LogisticsFile {

    private static final String BELTS = "belts";
    private static final String PICKERS = "pickers";
    private static final String SPLITTERS = "splitters";
    private static final String DUPLEXES = "duplexes";
    private static final String ITEMS = "items";

    private LogisticsFile() {
    }

    /**
     * A belt unit.
     *
     * @param world the world's id
     * @param pos   the belt's block
     * @param flow  the direction its items move in
     * @param level its level
     * @param owner the id of the player who placed it, or null when none was stored
     */
    public record Belt(UUID world, Pos pos, Dir flow, LogisticsLevel level, UUID owner) {

        /** A belt with no stored owner. */
        public Belt(UUID world, Pos pos, Dir flow, LogisticsLevel level) {
            this(world, pos, flow, level, null);
        }
    }

    /**
     * A picker.
     *
     * @param id    the picker's id
     * @param world the world's id
     * @param pos   the picker's block
     * @param inDir the direction from the picker to its input
     * @param level its level
     * @param owner the id of the player who placed it, or null when none was stored
     */
    public record Picker(String id, UUID world, Pos pos, Dir inDir, LogisticsLevel level, UUID owner) {

        /** A picker with no stored owner. */
        public Picker(String id, UUID world, Pos pos, Dir inDir, LogisticsLevel level) {
            this(id, world, pos, inDir, level, null);
        }
    }

    /**
     * A splitter.
     *
     * @param id    the splitter's id
     * @param world the world's id
     * @param pos   the splitter's block
     * @param inDir the direction its items travel
     * @param level its level
     * @param left  the types sent left
     * @param right the types sent right
     * @param held  the item it holds, or null when it holds none
     * @param owner the id of the player who placed it, or null when none was stored
     */
    public record Splitter(String id, UUID world, Pos pos, Dir inDir, LogisticsLevel level, List<Item> left, List<Item> right,
                           Item held, UUID owner) {

        /** A splitter with no stored owner. */
        public Splitter(String id, UUID world, Pos pos, Dir inDir, LogisticsLevel level, List<Item> left, List<Item> right,
                        Item held) {
            this(id, world, pos, inDir, level, left, right, held, null);
        }
    }

    /**
     * A belt duplex: two cells wide, its anchor at {@code pos} and its second cell at {@code pos + right(flow)}.
     *
     * @param id    the duplex's id
     * @param world the world's id
     * @param pos   the anchor block, the cell the duplex was placed on
     * @param flow  the direction its items travel
     * @param level its level
     * @param held  the item it holds, or null when it holds none
     * @param owner the id of the player who placed it, or null when none was stored
     */
    public record Duplex(String id, UUID world, Pos pos, Dir flow, LogisticsLevel level, Item held, UUID owner) {
    }

    /**
     * An item on a belt.
     *
     * @param world    the world's id
     * @param pos      the belt's block
     * @param item     the item
     * @param position its place along the belt, from 0 at the entry to 1 at the exit
     */
    public record Carried(UUID world, Pos pos, Item item, double position) {
    }

    /**
     * The whole file.
     *
     * @param belts     the belts
     * @param pickers   the pickers
     * @param splitters the splitters
     * @param duplexes  the belt duplexes
     * @param items     the items on the belts
     */
    public record State(List<Belt> belts, List<Picker> pickers, List<Splitter> splitters, List<Duplex> duplexes,
                        List<Carried> items) {

        public State {
            belts = List.copyOf(belts);
            pickers = List.copyOf(pickers);
            splitters = List.copyOf(splitters);
            duplexes = List.copyOf(duplexes);
            items = List.copyOf(items);
        }

        /** A state with no belt duplexes. */
        public State(List<Belt> belts, List<Picker> pickers, List<Splitter> splitters, List<Carried> items) {
            this(belts, pickers, splitters, List.of(), items);
        }

        /** No devices and no items. */
        public static State empty() {
            return new State(List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }

    /** The text of a logistics file holding the given state. */
    public static String write(State state) {
        YamlConfiguration yaml = new YamlConfiguration();
        int index = 0;
        for (Belt belt : state.belts()) {
            String base = BELTS + "." + index++ + ".";
            yaml.set(base + "world", belt.world().toString());
            position(yaml, base, belt.pos());
            yaml.set(base + "flow", belt.flow().name());
            yaml.set(base + "level", belt.level().name());
            owner(yaml, base, belt.owner());
        }
        index = 0;
        for (Picker picker : state.pickers()) {
            String base = PICKERS + "." + index++ + ".";
            yaml.set(base + "id", picker.id());
            yaml.set(base + "world", picker.world().toString());
            position(yaml, base, picker.pos());
            yaml.set(base + "in", picker.inDir().name());
            yaml.set(base + "level", picker.level().name());
            owner(yaml, base, picker.owner());
        }
        index = 0;
        for (Splitter splitter : state.splitters()) {
            String base = SPLITTERS + "." + index++ + ".";
            yaml.set(base + "id", splitter.id());
            yaml.set(base + "world", splitter.world().toString());
            position(yaml, base, splitter.pos());
            yaml.set(base + "in", splitter.inDir().name());
            yaml.set(base + "level", splitter.level().name());
            yaml.set(base + "left", texts(splitter.left()));
            yaml.set(base + "right", texts(splitter.right()));
            if (splitter.held() != null) {
                yaml.set(base + "held", text(splitter.held()));
            }
            owner(yaml, base, splitter.owner());
        }
        index = 0;
        for (Duplex duplex : state.duplexes()) {
            String base = DUPLEXES + "." + index++ + ".";
            yaml.set(base + "id", duplex.id());
            yaml.set(base + "world", duplex.world().toString());
            position(yaml, base, duplex.pos());
            yaml.set(base + "flow", duplex.flow().name());
            yaml.set(base + "level", duplex.level().name());
            if (duplex.held() != null) {
                yaml.set(base + "held", text(duplex.held()));
            }
            owner(yaml, base, duplex.owner());
        }
        index = 0;
        for (Carried carried : state.items()) {
            String base = ITEMS + "." + index++ + ".";
            yaml.set(base + "world", carried.world().toString());
            position(yaml, base, carried.pos());
            yaml.set(base + "item", text(carried.item()));
            yaml.set(base + "position", carried.position());
        }
        return yaml.saveToString();
    }

    /**
     * The state of a logistics file's text. An entry with a missing or invalid field is skipped and reported to the warnings.
     *
     * @param text     the file's text
     * @param warnings receives one message for each skipped entry
     */
    public static State read(String text, Consumer<String> warnings) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(text));
        List<Belt> belts = new ArrayList<>();
        for (ConfigurationSection entry : entries(yaml, BELTS)) {
            UUID world = uuid(entry.getString("world"));
            Dir flow = dir(entry.getString("flow"));
            LogisticsLevel level = level(entry.getString("level"));
            if (world == null || flow == null || level == null) {
                warnings.accept("logistics.yml: a belt entry is not valid and is skipped.");
                continue;
            }
            belts.add(new Belt(world, position(entry), flow, level, ownerOf(entry, "belt", warnings)));
        }
        List<Picker> pickers = new ArrayList<>();
        for (ConfigurationSection entry : entries(yaml, PICKERS)) {
            UUID world = uuid(entry.getString("world"));
            Dir in = dir(entry.getString("in"));
            LogisticsLevel level = level(entry.getString("level"));
            if (entry.getString("id") == null || world == null || in == null || level == null) {
                warnings.accept("logistics.yml: a picker entry is not valid and is skipped.");
                continue;
            }
            pickers.add(new Picker(entry.getString("id"), world, position(entry), in, level, ownerOf(entry, "picker", warnings)));
        }
        List<Splitter> splitters = new ArrayList<>();
        for (ConfigurationSection entry : entries(yaml, SPLITTERS)) {
            UUID world = uuid(entry.getString("world"));
            Dir in = dir(entry.getString("in"));
            LogisticsLevel level = level(entry.getString("level"));
            List<Item> left = items(entry.getStringList("left"));
            List<Item> right = items(entry.getStringList("right"));
            Item held = entry.contains("held") ? item(entry.getString("held")) : null;
            boolean heldOk = !entry.contains("held") || held != null;
            if (entry.getString("id") == null || world == null || in == null || level == null || !heldOk) {
                warnings.accept("logistics.yml: a splitter entry is not valid and is skipped.");
                continue;
            }
            splitters.add(new Splitter(entry.getString("id"), world, position(entry), in, level, left, right, held,
                    ownerOf(entry, "splitter", warnings)));
        }
        List<Duplex> duplexes = new ArrayList<>();
        for (ConfigurationSection entry : entries(yaml, DUPLEXES)) {
            UUID world = uuid(entry.getString("world"));
            Dir flow = dir(entry.getString("flow"));
            LogisticsLevel level = level(entry.getString("level"));
            Item held = entry.contains("held") ? item(entry.getString("held")) : null;
            boolean heldOk = !entry.contains("held") || held != null;
            if (entry.getString("id") == null || world == null || flow == null || level == null || !heldOk) {
                warnings.accept("logistics.yml: a belt duplex entry is not valid and is skipped.");
                continue;
            }
            duplexes.add(new Duplex(entry.getString("id"), world, position(entry), flow, level, held,
                    ownerOf(entry, "belt duplex", warnings)));
        }
        List<Carried> items = new ArrayList<>();
        for (ConfigurationSection entry : entries(yaml, ITEMS)) {
            UUID world = uuid(entry.getString("world"));
            Item item = item(entry.getString("item"));
            if (world == null || item == null) {
                warnings.accept("logistics.yml: an item entry is not valid and is skipped.");
                continue;
            }
            items.add(new Carried(world, position(entry), item, entry.getDouble("position")));
        }
        return new State(belts, pickers, splitters, duplexes, items);
    }

    /** The text of an item: its kind, a colon and its id. */
    static String text(Item item) {
        return item.kind() + ":" + item.id();
    }

    /** The item a text names, or null when the text is not an item. */
    static Item item(String text) {
        if (text == null) {
            return null;
        }
        int colon = text.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String kind = text.substring(0, colon);
        String id = text.substring(colon + 1);
        try {
            return new Item(kind, id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static List<String> texts(List<Item> items) {
        List<String> texts = new ArrayList<>();
        for (Item item : items) {
            texts.add(text(item));
        }
        return texts;
    }

    private static List<Item> items(List<String> texts) {
        List<Item> items = new ArrayList<>();
        for (String text : texts) {
            Item item = item(text);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private static List<ConfigurationSection> entries(YamlConfiguration yaml, String section) {
        ConfigurationSection root = yaml.getConfigurationSection(section);
        List<ConfigurationSection> entries = new ArrayList<>();
        if (root == null) {
            return entries;
        }
        List<String> keys = new ArrayList<>(root.getKeys(false));
        keys.sort(Comparator.comparingInt(key -> key.matches("\\d+") ? Integer.parseInt(key) : Integer.MAX_VALUE));
        for (String key : keys) {
            ConfigurationSection entry = root.getConfigurationSection(key);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static void position(YamlConfiguration yaml, String base, Pos pos) {
        yaml.set(base + "x", pos.x());
        yaml.set(base + "y", pos.y());
        yaml.set(base + "z", pos.z());
    }

    private static Pos position(ConfigurationSection entry) {
        return new Pos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
    }

    /** Writes the owner of a device under its entry, when it has one. An entry with no owner has no {@code owner} key. */
    private static void owner(YamlConfiguration yaml, String base, UUID owner) {
        if (owner != null) {
            yaml.set(base + "owner", owner.toString());
        }
    }

    /**
     * The owner an entry names, or null when it names none. A text that is not an id is reported and read as no owner, so the device
     * is refused in every claim, as an owner-less device is.
     */
    private static UUID ownerOf(ConfigurationSection entry, String kind, Consumer<String> warnings) {
        String text = entry.getString("owner");
        if (text == null) {
            return null;
        }
        UUID owner = uuid(text);
        if (owner == null) {
            warnings.accept("logistics.yml: a " + kind + " has an owner that is not valid and is read as having none.");
        }
        return owner;
    }

    private static UUID uuid(String text) {
        if (text == null) {
            return null;
        }
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Dir dir(String text) {
        return text == null ? null : enumOf(Dir.class, text);
    }

    private static LogisticsLevel level(String text) {
        return text == null ? null : enumOf(LogisticsLevel.class, text);
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, String text) {
        try {
            return Enum.valueOf(type, text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
