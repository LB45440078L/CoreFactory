package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryLevel;
import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ChargeTier;
import io.corefactory.core.energy.Pos;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes {@code artillery.yml} in the plugin's data folder. The file holds a {@code version}, the artillery pieces and the Command
 * Centers as numbered entries, and the time of each player's last antimatter shot by unique id. A write goes to a {@code .tmp} file that is
 * then moved over the real one, so a crash during a write leaves the last good file. An entry that cannot be read is skipped and reported.
 */
public final class ArtilleryStore {

    /** The version written now. Version 3 adds the magazine of each piece; a version 2 piece has an empty one. */
    public static final int VERSION = 3;

    private static final String VERSION_KEY = "version";
    private static final String PIECES = "pieces";
    private static final String CENTERS = "command-centers";
    private static final String ANTIMATTER = "antimatter";

    /**
     * What the file holds: the pieces, the Command Centers, and the time of each player's last antimatter shot in milliseconds.
     *
     * @param pieces          the artillery pieces
     * @param centers         the Command Centers
     * @param antimatterShots the last antimatter shot of each player, by unique id
     */
    public record Saved(List<ArtilleryDevice> pieces, List<CommandCenter> centers, Map<UUID, Long> antimatterShots) {
    }

    private final File file;
    private final Logger log;

    /**
     * @param file the store file, normally {@code artillery.yml} in the data folder
     * @param log  receives the skipped entries and any failed write
     */
    public ArtilleryStore(File file, Logger log) {
        this.file = file;
        this.log = log;
    }

    /** The saved pieces, centers and antimatter times, or none when the file is missing or cannot be read. */
    public Saved read() {
        if (!file.isFile()) {
            return new Saved(List.of(), List.of(), Map.of());
        }
        try {
            return decode(Files.readString(file.toPath(), StandardCharsets.UTF_8), log::warning);
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not read " + file.getName() + ". Artillery starts empty.", e);
            return new Saved(List.of(), List.of(), Map.of());
        }
    }

    /** Writes the pieces atomically: to a temporary file first, then moved over the store file. */
    public void write(Saved saved) {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        File temporary = new File(parent, file.getName() + ".tmp");
        try {
            Files.writeString(temporary.toPath(), encode(saved), StandardCharsets.UTF_8);
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

    /** The text of a store file holding the given pieces, centers and antimatter times. */
    static String encode(Saved saved) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(VERSION_KEY, VERSION);
        int index = 0;
        for (ArtilleryDevice piece : saved.pieces()) {
            String base = PIECES + "." + index++ + ".";
            yaml.set(base + "type", ArtilleryIds.DEVICE_TYPE);
            yaml.set(base + "id", piece.id());
            yaml.set(base + "owner", piece.owner().toString());
            yaml.set(base + "world", piece.world().toString());
            yaml.set(base + "x", piece.pos().x());
            yaml.set(base + "y", piece.pos().y());
            yaml.set(base + "z", piece.pos().z());
            yaml.set(base + "level", piece.level().number());
            yaml.set(base + "name", piece.name());
            yaml.set(base + "on", piece.on());
            if (piece.shell() != null) {
                yaml.set(base + "shell", piece.shell().name());
            }
            yaml.set(base + "tier", piece.tier().name());
            if (piece.magazineShells() > 0) {
                yaml.set(base + "magazine-shells", piece.magazineShells());
            }
            if (piece.magazineCharges() > 0) {
                yaml.set(base + "magazine-charges", piece.magazineCharges());
            }
            if (piece.hasAim()) {
                yaml.set(base + "aim-x", piece.aimX());
                yaml.set(base + "aim-z", piece.aimZ());
                yaml.set(base + "elevation", piece.elevation());
            }
            yaml.set(base + "body-yaw", piece.bodyYaw());
            yaml.set(base + "barrel-pitch", piece.barrelPitch());
            if (piece.lastShotMillis() != ArtilleryDevice.NEVER) {
                yaml.set(base + "last-shot-millis", piece.lastShotMillis());
            }
        }
        index = 0;
        for (CommandCenter center : saved.centers()) {
            String base = CENTERS + "." + index++ + ".";
            yaml.set(base + "type", ArtilleryIds.COMMAND_CENTER_TYPE);
            yaml.set(base + "id", center.id());
            yaml.set(base + "owner", center.owner().toString());
            yaml.set(base + "world", center.world().toString());
            yaml.set(base + "x", center.pos().x());
            yaml.set(base + "y", center.pos().y());
            yaml.set(base + "z", center.pos().z());
            yaml.set(base + "name", center.name());
            if (center.shell() != null) {
                yaml.set(base + "shell", center.shell().name());
            }
            yaml.set(base + "tier", center.tier().name());
            if (center.hasAim()) {
                yaml.set(base + "aim-x", center.aimX());
                yaml.set(base + "aim-z", center.aimZ());
            }
            yaml.set(base + "elevation", center.elevation());
            if (!center.wallMaps().isEmpty()) {
                yaml.set(base + "map-ids", center.wallMaps());
            }
        }
        for (Map.Entry<UUID, Long> shot : saved.antimatterShots().entrySet()) {
            yaml.set(ANTIMATTER + "." + shot.getKey(), shot.getValue());
        }
        return yaml.saveToString();
    }

    /**
     * The pieces, centers and antimatter times of a store file's text. An entry with a missing or invalid field is skipped and reported.
     * An entry with no tier is at tier I, one with no shot is never fired, one with no name has the default name, and a center with no
     * elevation is at the default elevation.
     *
     * @param text     the file's text
     * @param warnings receives one message for each skipped entry
     */
    static Saved decode(String text, Consumer<String> warnings) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(text));
        List<ArtilleryDevice> pieces = new ArrayList<>();
        for (ConfigurationSection entry : entries(yaml, PIECES)) {
            UUID owner = uuidOf(entry.getString("owner"));
            UUID world = uuidOf(entry.getString("world"));
            ArtilleryLevel level = levelOf(entry.getInt("level", 0));
            if (entry.getString("id") == null || owner == null || world == null || level == null) {
                warnings.accept("artillery.yml: a piece entry is not valid and is skipped.");
                continue;
            }
            ArtilleryDevice piece = new ArtilleryDevice(entry.getString("id"), owner, world,
                    new Pos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z")), level);
            piece.setName(nameOf(entry.getString("name"), ArtilleryDevice.DEFAULT_NAME));
            piece.setOn(entry.getBoolean("on", false));
            piece.setShell(enumOf(ArtilleryShell.class, entry.getString("shell")));
            ChargeTier tier = enumOf(ChargeTier.class, entry.getString("tier"));
            piece.setTier(tier == null ? ChargeTier.TIER_I : tier);
            // A magazine holds the shell and the tier the piece is loaded with, so a piece with no shell has an empty one.
            boolean loaded = piece.shell() != null;
            piece.setMagazine(loaded ? Math.max(0, entry.getInt("magazine-shells", 0)) : 0,
                    loaded ? Math.max(0, entry.getInt("magazine-charges", 0)) : 0);
            if (entry.contains("aim-x") && entry.contains("aim-z") && entry.contains("elevation")) {
                piece.setAim(entry.getDouble("aim-x"), entry.getDouble("aim-z"), entry.getDouble("elevation"));
            }
            if (entry.contains("body-yaw")) {
                piece.setBodyYaw(entry.getDouble("body-yaw"));
            }
            if (entry.contains("barrel-pitch")) {
                piece.setBarrelPitch(entry.getDouble("barrel-pitch"));
            }
            if (entry.contains("last-shot-millis")) {
                piece.setLastShotMillis(entry.getLong("last-shot-millis"));
            }
            pieces.add(piece);
        }
        List<CommandCenter> centers = new ArrayList<>();
        for (ConfigurationSection entry : entries(yaml, CENTERS)) {
            UUID owner = uuidOf(entry.getString("owner"));
            UUID world = uuidOf(entry.getString("world"));
            if (entry.getString("id") == null || owner == null || world == null) {
                warnings.accept("artillery.yml: a command center entry is not valid and is skipped.");
                continue;
            }
            CommandCenter center = new CommandCenter(entry.getString("id"), owner, world,
                    new Pos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z")));
            center.setName(nameOf(entry.getString("name"), CommandCenter.DEFAULT_NAME));
            ChargeTier tier = enumOf(ChargeTier.class, entry.getString("tier"));
            center.setLoad(enumOf(ArtilleryShell.class, entry.getString("shell")), tier == null ? ChargeTier.TIER_I : tier);
            if (entry.contains("aim-x") && entry.contains("aim-z")) {
                center.setAim(entry.getDouble("aim-x"), entry.getDouble("aim-z"));
            }
            if (entry.contains("elevation")) {
                center.setElevation(entry.getDouble("elevation"));
            }
            center.setWallMaps(entry.getIntegerList("map-ids"));
            centers.add(center);
        }
        Map<UUID, Long> antimatter = new LinkedHashMap<>();
        ConfigurationSection shots = yaml.getConfigurationSection(ANTIMATTER);
        if (shots != null) {
            for (String key : shots.getKeys(false)) {
                UUID player = uuidOf(key);
                if (player != null) {
                    antimatter.put(player, shots.getLong(key));
                }
            }
        }
        return new Saved(pieces, centers, antimatter);
    }

    /** The name of an entry: its text, trimmed and cut to the longest name, or the default when it is blank or missing. */
    private static String nameOf(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        String trimmed = text.trim();
        return trimmed.length() > ArtilleryDevice.MAX_NAME_LENGTH ? trimmed.substring(0, ArtilleryDevice.MAX_NAME_LENGTH) : trimmed;
    }

    private static ArtilleryLevel levelOf(int number) {
        for (ArtilleryLevel level : ArtilleryLevel.values()) {
            if (level.number() == number) {
                return level;
            }
        }
        return null;
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, String name) {
        if (name == null) {
            return null;
        }
        for (E value : type.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(name.trim())) {
                return value;
            }
        }
        return null;
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
