package io.corefactory.spigot.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.logging.Level;

/**
 * Brings existing data files up to date with the defaults shipped in the jar. The plugin copies its data files only when
 * they are missing, so a server that was set up earlier would never see new entries such as new TNT types, machines,
 * ores or research nodes. This adds every entry the defaults have and the file does not, and keeps a copy of the file
 * before it is rewritten. Entries that are already in the file are never changed; when one differs from the shipped
 * default, the log names it so an administrator can delete it to take the new default.
 * <p>
 * processes.yml is the exception: its machines, processes and drills are tied to the code, so it is not merged. Its version
 * line decides whether the server's copy is replaced with the shipped one (see {@link DataVersion}).
 */
public final class DefaultsMerger {

    private static final String PROCESSES = "processes.yml";

    /** A data section that holds one entry per id. */
    private record Section(String file, String section) {
    }

    private static final List<Section> SECTIONS = List.of(
            new Section("tnts.yml", "tnts"),
            new Section("items.yml", "components"),
            new Section("materials.yml", "components"),
            new Section("ores.yml", "ores"),
            new Section("research.yml", "nodes"));

    private DefaultsMerger() {
    }

    /** Merges every data file that exists in the data folder. Call before the files are read. */
    public static void merge(JavaPlugin plugin) {
        refreshProcesses(plugin);
        Map<String, List<String>> byFile = new LinkedHashMap<>();
        for (Section section : SECTIONS) {
            byFile.computeIfAbsent(section.file(), key -> new ArrayList<>()).add(section.section());
        }
        for (Map.Entry<String, List<String>> entry : byFile.entrySet()) {
            try {
                mergeFile(plugin, entry.getKey(), entry.getValue());
            } catch (IOException | RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Could not update " + entry.getKey() + " with new defaults.", e);
            }
        }
    }

    /**
     * Replaces the server's processes.yml with the shipped one when its version is missing or older, after a backup. A newer
     * file is kept and reported. Logs one warning when the file is replaced.
     */
    private static void refreshProcesses(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), PROCESSES);
        if (!file.isFile() || plugin.getResource(PROCESSES) == null) {
            return;
        }
        try {
            OptionalInt server = versionOf(file);
            DataVersion.Action action = DataVersion.decide(server, DataVersion.CURRENT);
            if (action == DataVersion.Action.REPLACE) {
                backUp(file);
                try (InputStream bundled = plugin.getResource(PROCESSES)) {
                    Files.copy(bundled, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                plugin.getLogger().warning("processes.yml was from an older version and was replaced with this version's "
                        + "defaults. The previous file is kept as processes.yml.bak.");
            } else if (action == DataVersion.Action.NEWER) {
                plugin.getLogger().warning("processes.yml is from a newer version (" + server.getAsInt() + ") than this "
                        + "plugin (" + DataVersion.CURRENT + "). It was left as it is.");
            }
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Could not check processes.yml against this version.", e);
        }
    }

    private static OptionalInt versionOf(File file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String first = reader.readLine();
            return first == null ? OptionalInt.empty() : DataVersion.parse(first);
        }
    }

    private static void mergeFile(JavaPlugin plugin, String name, List<String> sections) throws IOException {
        File file = new File(plugin.getDataFolder(), name);
        InputStream bundled = plugin.getResource(name);
        if (!file.isFile() || bundled == null) {
            return;
        }
        YamlConfiguration defaults;
        try (InputStreamReader reader = new InputStreamReader(bundled, StandardCharsets.UTF_8)) {
            defaults = YamlConfiguration.loadConfiguration(reader);
        }
        YamlConfiguration current = YamlConfiguration.loadConfiguration(file);
        Result result = mergeInto(defaults, current, sections);
        List<String> added = result.added();
        List<String> differing = result.differing();
        if (!added.isEmpty()) {
            backUp(file);
            current.save(file);
            plugin.getLogger().info("Added " + added.size() + " new entries to " + name + ": " + String.join(", ", added)
                    + ". A copy of the previous file is kept as " + name + ".bak.");
        }
        if (!differing.isEmpty()) {
            plugin.getLogger().warning(name + " has entries that differ from this version's defaults: "
                    + String.join(", ", differing) + ". They were kept as they are. Delete an entry from the file to take "
                    + "the new default on the next start.");
        }
    }

    /** What a merge did: the ids it added, and the ids the file already has that differ from the defaults. */
    record Result(List<String> added, List<String> differing) {
    }

    /**
     * Adds to {@code current} every entry of {@code defaults} that it does not have, in the given sections. Entries that
     * exist are left alone; the ones that differ are reported. Pure, so it can be tested without a server.
     */
    static Result mergeInto(YamlConfiguration defaults, YamlConfiguration current, List<String> sections) {
        List<String> added = new ArrayList<>();
        List<String> differing = new ArrayList<>();
        for (String section : sections) {
            ConfigurationSection shipped = defaults.getConfigurationSection(section);
            if (shipped == null) {
                continue;
            }
            for (String id : shipped.getKeys(false)) {
                ConfigurationSection value = shipped.getConfigurationSection(id);
                if (value == null) {
                    continue;
                }
                String path = section + "." + id;
                if (!current.contains(path)) {
                    current.createSection(path, value.getValues(true));
                    added.add(id);
                } else if (!sameValues(value, current.getConfigurationSection(path))) {
                    differing.add(id);
                }
            }
        }
        return new Result(added, differing);
    }

    /**
     * True when both sections hold the same leaf values under the same keys. Nested sections are compared through their
     * leaves, because a section object never equals another section object.
     */
    private static boolean sameValues(ConfigurationSection shipped, ConfigurationSection current) {
        if (current == null) {
            return false;
        }
        for (String key : shipped.getKeys(true)) {
            if (!shipped.isConfigurationSection(key) && !Objects.equals(shipped.get(key), current.get(key))) {
                return false;
            }
        }
        for (String key : current.getKeys(true)) {
            if (!current.isConfigurationSection(key) && !shipped.contains(key)) {
                return false;
            }
        }
        return true;
    }

    private static void backUp(File file) throws IOException {
        Files.copy(file.toPath(), file.toPath().resolveSibling(file.getName() + ".bak"),
                StandardCopyOption.REPLACE_EXISTING);
    }
}
