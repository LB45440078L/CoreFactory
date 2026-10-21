package io.corefactory.spigot.achievement;

import io.corefactory.spigot.achievement.Achievement.Category;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Writes the achievements as a vanilla datapack, so they appear in the Advancements screen and the server announces
 * them with its own toasts and chat messages.
 *
 * <p>The datapack is {@code <world>/datapacks/CoreFactory-achievements}. It is rewritten on every start. The server
 * reads datapacks at startup, so the first start with a new datapack needs one restart before the tab shows.
 *
 * <p>Each achievement is one advancement with as many criteria as it has steps. A counter with a goal of 1000 has 20
 * steps; {@link AchievementTracker} awards a step each time the count passes another twentieth of the goal. A group
 * has one criterion per member, so it completes when the last member does.
 */
public final class AchievementDatapack {

    /** The namespace of the datapack's advancements. */
    public static final String NAMESPACE = "corefactory";

    /** The folder name of the datapack inside the world folder's {@code datapacks} directory. */
    public static final String FOLDER = "CoreFactory-achievements";

    /** Counters are split into at most this many steps, so the advancement stays small. */
    public static final int MAX_STEPS = 20;

    private static final String IMPOSSIBLE = "minecraft:impossible";

    private AchievementDatapack() {
    }

    /** The advancement at the top of the tab. */
    public static NamespacedKey root() {
        return new NamespacedKey(NAMESPACE, "root");
    }

    /** The advancement of one achievement. */
    public static NamespacedKey key(Achievement achievement) {
        return new NamespacedKey(NAMESPACE, "achievements/" + achievement.id());
    }

    /** The criterion that counts step {@code index}, from 1. */
    static String step(int index) {
        return "step_" + index;
    }

    /** The number of steps a non-group achievement is split into. */
    static int steps(Achievement achievement) {
        return Math.min(achievement.goal(), MAX_STEPS);
    }

    /**
     * The data pack format range the running server accepts. A data pack must declare it with {@code min_format} and
     * {@code max_format}, and the server reads the range from its own built-in data packs, so it is never guessed.
     */
    public record DataFormat(int min, int max) {

        String mcmeta() {
            return "{\"pack\":{\"description\":\"CoreFactory achievements, written by the plugin. Replaced on every start.\","
                    + "\"min_format\":" + min + ",\"max_format\":" + max + "}}\n";
        }
    }

    /** The server's own data packs. They declare the range the server accepts, in the same form we write. */
    private static final List<String> BUILT_IN_PACKS = List.of(
            "data/minecraft/datapacks/redstone_experiments/pack.mcmeta",
            "data/minecraft/datapacks/minecart_improvements/pack.mcmeta",
            "data/minecraft/datapacks/trade_rebalance/pack.mcmeta");

    /**
     * Reads the data pack format of the running server. The first source that has it wins:
     * <ol>
     *   <li>the server's own {@code bukkit} data pack in the world folder, which the server writes with the range it
     *       accepts (for example {@code min_format: 121.0});</li>
     *   <li>the built-in data packs in the server jar, read from the classpath;</li>
     *   <li>the server's {@code version.json}, which has {@code data_major}.</li>
     * </ol>
     * Empty if none can be read.
     */
    public static Optional<DataFormat> dataFormat(File worldFolder, ClassLoader loader) {
        Optional<DataFormat> fromWorld = readFormat(worldFolder.toPath().resolve("datapacks").resolve("bukkit")
                .resolve("pack.mcmeta"));
        if (fromWorld.isPresent()) {
            return fromWorld;
        }
        for (String name : BUILT_IN_PACKS) {
            Optional<DataFormat> format = resource(loader, name).flatMap(AchievementDatapack::formatOf);
            if (format.isPresent()) {
                return format;
            }
        }
        Optional<Integer> dataMajor = resource(loader, "version.json").flatMap(text -> number(text, "data_major"));
        return dataMajor.map(major -> new DataFormat(major, major));
    }

    /** Removes this datapack, so a broken or unknown format never reaches the server. */
    public static void remove(File worldFolder) throws IOException {
        deleteTree(worldFolder.toPath().resolve("datapacks").resolve(FOLDER));
    }

    private static Optional<DataFormat> readFormat(Path file) {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return formatOf(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Optional<DataFormat> formatOf(String text) {
        Optional<Integer> min = number(text, "min_format");
        Optional<Integer> max = number(text, "max_format");
        if (min.isPresent() && max.isPresent()) {
            return Optional.of(new DataFormat(min.get(), max.get()));
        }
        return Optional.empty();
    }

    /** Writes only the pack metadata, so the server can read a correct pack when it starts. */
    public static void writePackMeta(File worldFolder, DataFormat format) throws IOException {
        Path pack = worldFolder.toPath().resolve("datapacks").resolve(FOLDER);
        Files.createDirectories(pack);
        write(pack.resolve("pack.mcmeta"), format.mcmeta());
    }

    /**
     * Writes the datapack into the world folder, replacing the previous copy of this datapack only.
     *
     * @param worldFolder the main world's folder, which holds the {@code datapacks} directory
     * @param format      the data pack format range of the running server
     * @return the datapack folder
     */
    public static Path write(File worldFolder, AchievementCatalog catalog, DataFormat format) throws IOException {
        Path pack = worldFolder.toPath().resolve("datapacks").resolve(FOLDER);
        deleteTree(pack);
        // Since 1.21 the folder is singular: data/<namespace>/advancement, as the server's own data uses.
        Path advancements = pack.resolve("data").resolve(NAMESPACE).resolve("advancement");
        Files.createDirectories(advancements.resolve("achievements"));
        Files.createDirectories(advancements.resolve("category"));
        write(pack.resolve("pack.mcmeta"), format.mcmeta());
        write(advancements.resolve("root.json"), rootJson());
        for (Category category : Category.values()) {
            write(advancements.resolve("category").resolve(name(category) + ".json"), categoryJson(category));
        }
        for (Achievement achievement : catalog.all()) {
            write(advancements.resolve("achievements").resolve(achievement.id() + ".json"),
                    achievementJson(achievement, catalog));
        }
        return pack;
    }

    static String rootJson() {
        return "{\"display\":{\"icon\":{\"id\":\"minecraft:tnt\"},\"title\":{\"text\":\"CoreFactory\"},"
                + "\"description\":{\"text\":\"Forge, light and detonate custom TNT.\"},"
                + "\"background\":\"corefactory:textures/gui/advancements/background.png\",\"frame\":\"task\","
                + "\"show_toast\":false,\"announce_to_chat\":false,\"hidden\":false},"
                + "\"criteria\":{\"tick\":{\"trigger\":\"minecraft:tick\"}}}\n";
    }

    private static String categoryJson(Category category) {
        return "{\"parent\":\"" + NAMESPACE + ":root\",\"display\":{\"icon\":{\"id\":\"" + categoryIcon(category)
                + "\"},\"title\":{\"text\":" + quote(category.label()) + "},\"description\":{\"text\":"
                + quote("Achievements about " + category.label().toLowerCase(Locale.ROOT) + ".")
                + "},\"frame\":\"goal\",\"show_toast\":false,\"announce_to_chat\":false,\"hidden\":false},"
                + "\"criteria\":{\"done\":{\"trigger\":\"" + IMPOSSIBLE + "\"}}}\n";
    }

    private static String achievementJson(Achievement achievement, AchievementCatalog catalog) {
        StringBuilder json = new StringBuilder();
        json.append("{\"parent\":\"").append(NAMESPACE).append(":category/").append(name(achievement.category()))
                .append("\",\"display\":{\"icon\":").append(icon(achievement.icon()))
                .append(",\"title\":{\"text\":").append(quote(AchievementCatalog.plain(achievement.title())))
                .append("},\"description\":{\"text\":")
                .append(quote(AchievementCatalog.plain(achievement.description())))
                .append("},\"frame\":\"").append(achievement.major() ? "challenge" : "task")
                .append("\",\"show_toast\":true,\"announce_to_chat\":true,\"hidden\":false},\"criteria\":{");
        List<String> criteria = achievement.trigger() == Achievement.Trigger.GROUP
                ? catalog.members(achievement.param()).stream().map(Achievement::id).toList()
                : IntStream.rangeClosed(1, steps(achievement)).mapToObj(AchievementDatapack::step).toList();
        for (int i = 0; i < criteria.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(quote(criteria.get(i))).append(":{\"trigger\":\"").append(IMPOSSIBLE).append("\"}");
        }
        return json.append("}}\n").toString();
    }

    /** The icon as the advancement format takes it: the item, and its custom model when it has one. */
    static String icon(ItemStack item) {
        String id = item.getType().getKey().toString();
        ItemMeta meta = item.getItemMeta();
        NamespacedKey model = meta == null ? null : meta.getItemModel();
        if (model == null) {
            return "{\"id\":" + quote(id) + "}";
        }
        return "{\"id\":" + quote(id) + ",\"components\":{\"minecraft:item_model\":" + quote(model.toString()) + "}}";
    }

    private static String categoryIcon(Category category) {
        return switch (category) {
            case STARTING -> Material.CRAFTING_TABLE.getKey().toString();
            case FORGING -> Material.ANVIL.getKey().toString();
            case USING -> Material.REDSTONE.getKey().toString();
            case LIGHTING -> Material.FLINT_AND_STEEL.getKey().toString();
            case DETONATION -> Material.TNT.getKey().toString();
            case NUCLEAR -> Material.SHROOMLIGHT.getKey().toString();
            case CHAOS -> Material.TRIPWIRE_HOOK.getKey().toString();
            case SCIENCE -> Material.ENCHANTED_BOOK.getKey().toString();
            case MASTERY -> Material.NETHER_STAR.getKey().toString();
        };
    }

    private static String name(Category category) {
        return category.name().toLowerCase(Locale.ROOT);
    }

    /** A JSON string literal. */
    static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private static Optional<String> resource(ClassLoader loader, String name) {
        try (InputStream in = loader.getResourceAsStream(name)) {
            return in == null ? Optional.empty() : Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * The major number of a JSON format field, found by name. Accepts the forms servers write: {@code 121},
     * {@code 121.0}, and {@code [101, 1]} (major and minor, of which the major is used). Empty if the field is missing.
     */
    static Optional<Integer> number(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\\[?\\s*(\\d+)").matcher(json);
        return matcher.find() ? Optional.of(Integer.parseInt(matcher.group(1))) : Optional.empty();
    }

    private static void write(Path file, String text) throws IOException {
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private static void deleteTree(Path folder) throws IOException {
        if (!Files.exists(folder)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
