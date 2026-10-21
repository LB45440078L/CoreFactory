package io.corefactory.spigot.friends;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes {@code friends.yml} in the plugin's data folder. The file holds a {@code version} and the friends of each owner: a
 * {@code friends} section keyed by the owner's id, then by each friend's id, with the friend's name as the value. A write goes to a
 * {@code .tmp} file that is then moved over the real one, so a crash during a write leaves the last good file.
 */
public final class FriendStore {

    /** The version written now. */
    public static final int VERSION = 1;

    private static final String VERSION_KEY = "version";
    private static final String FRIENDS = "friends";

    private final File file;
    private final Logger log;

    /**
     * @param file the store file, normally {@code friends.yml} in the data folder
     * @param log  receives the skipped entries and any failed write
     */
    public FriendStore(File file, Logger log) {
        this.file = file;
        this.log = log;
    }

    /** The saved friends of each owner, or none when the file is missing or cannot be read. */
    public Map<UUID, Map<UUID, String>> read() {
        if (!file.isFile()) {
            return new HashMap<>();
        }
        try {
            return decode(Files.readString(file.toPath(), StandardCharsets.UTF_8), log::warning);
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not read " + file.getName() + ". Friends start empty.", e);
            return new HashMap<>();
        }
    }

    /** Writes the friends atomically: to a temporary file first, then moved over the store file. */
    public void write(Map<UUID, Map<UUID, String>> friends) {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        File temporary = new File(parent, file.getName() + ".tmp");
        try {
            Files.writeString(temporary.toPath(), encode(friends), StandardCharsets.UTF_8);
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

    /** The text of a store file holding the given friends. */
    static String encode(Map<UUID, Map<UUID, String>> friends) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(VERSION_KEY, VERSION);
        for (Map.Entry<UUID, Map<UUID, String>> owner : friends.entrySet()) {
            for (Map.Entry<UUID, String> friend : owner.getValue().entrySet()) {
                yaml.set(FRIENDS + "." + owner.getKey() + "." + friend.getKey(), friend.getValue() == null ? "" : friend.getValue());
            }
        }
        return yaml.saveToString();
    }

    /**
     * The friends of a store file's text. An entry whose ids do not parse is skipped and reported.
     *
     * @param text     the file's text
     * @param warnings receives one message for each skipped entry
     */
    static Map<UUID, Map<UUID, String>> decode(String text, Consumer<String> warnings) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(text));
        Map<UUID, Map<UUID, String>> friends = new HashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection(FRIENDS);
        if (root == null) {
            return friends;
        }
        for (String ownerKey : root.getKeys(false)) {
            ConfigurationSection owned = root.getConfigurationSection(ownerKey);
            UUID owner = uuidOf(ownerKey);
            if (owned == null || owner == null) {
                warnings.accept("friends.yml: an owner entry is not valid and is skipped.");
                continue;
            }
            Map<UUID, String> list = new HashMap<>();
            for (String friendKey : owned.getKeys(false)) {
                UUID friend = uuidOf(friendKey);
                if (friend == null) {
                    warnings.accept("friends.yml: a friend entry is not valid and is skipped.");
                    continue;
                }
                list.put(friend, owned.getString(friendKey, ""));
            }
            if (!list.isEmpty()) {
                friends.put(owner, list);
            }
        }
        return friends;
    }

    private static UUID uuidOf(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
