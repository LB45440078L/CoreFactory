package io.corefactory.paper.achievement;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Each player's achievement progress and unlock times, kept in {@code achievements.yml}:
 *
 * <pre>
 * players:
 *   &lt;uuid&gt;:
 *     progress:  {craft_chain_reaction_tnt: 1, detonate_100: 37}
 *     unlocked:  {craft_chain_reaction_tnt: 1760000000000}
 * </pre>
 */
public final class AchievementStore {

    private final File file;
    private final Logger log;
    private final Map<UUID, PlayerRecord> players = new HashMap<>();
    private boolean dirty;

    private static final class PlayerRecord {
        private final Map<String, Integer> progress = new TreeMap<>();
        private final Map<String, Long> unlocked = new TreeMap<>();
    }

    public AchievementStore(File file, Logger log) {
        this.file = file;
        this.log = log;
    }

    public void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("players");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                log.warning("achievements.yml has a bad player id, skipped: " + key);
                continue;
            }
            PlayerRecord player = new PlayerRecord();
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section != null) {
                copyInts(section.getConfigurationSection("progress"), player.progress);
                copyLongs(section.getConfigurationSection("unlocked"), player.unlocked);
            }
            players.put(id, player);
        }
    }

    /** Writes the file if anything changed since the last save. */
    public void save() {
        if (!dirty) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerRecord> entry : players.entrySet()) {
            String base = "players." + entry.getKey() + ".";
            entry.getValue().progress.forEach((id, value) -> yaml.set(base + "progress." + id, value));
            entry.getValue().unlocked.forEach((id, time) -> yaml.set(base + "unlocked." + id, time));
        }
        try {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            yaml.save(file);
            dirty = false;
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not save achievements.yml", e);
        }
    }

    public int progress(UUID player, String achievement) {
        PlayerRecord record = players.get(player);
        return record == null ? 0 : record.progress.getOrDefault(achievement, 0);
    }

    /** Adds {@code amount} to the progress and returns the new total. */
    public int add(UUID player, String achievement, int amount) {
        PlayerRecord record = players.computeIfAbsent(player, id -> new PlayerRecord());
        int total = record.progress.getOrDefault(achievement, 0) + amount;
        record.progress.put(achievement, total);
        dirty = true;
        return total;
    }

    public boolean isUnlocked(UUID player, String achievement) {
        PlayerRecord record = players.get(player);
        return record != null && record.unlocked.containsKey(achievement);
    }

    public void unlock(UUID player, String achievement, long time) {
        PlayerRecord record = players.computeIfAbsent(player, id -> new PlayerRecord());
        record.unlocked.put(achievement, time);
        dirty = true;
    }

    /** Unlock times by achievement id, for one player. */
    public Map<String, Long> unlocked(UUID player) {
        PlayerRecord record = players.get(player);
        return record == null ? Map.of() : Map.copyOf(record.unlocked);
    }

    private static void copyInts(ConfigurationSection from, Map<String, Integer> to) {
        if (from == null) {
            return;
        }
        for (String key : from.getKeys(false)) {
            to.put(key, from.getInt(key));
        }
    }

    private static void copyLongs(ConfigurationSection from, Map<String, Long> to) {
        if (from == null) {
            return;
        }
        for (String key : from.getKeys(false)) {
            to.put(key, from.getLong(key));
        }
    }
}
