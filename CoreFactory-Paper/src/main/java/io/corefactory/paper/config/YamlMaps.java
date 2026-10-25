package io.corefactory.paper.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the YAML of Paper into the plain maps and lists that Core reads. A section becomes a map, a list becomes a list with
 * its sections converted too, and a scalar is kept as it is. Core never sees a Bukkit configuration type.
 */
public final class YamlMaps {

    private YamlMaps() {
    }

    /**
     * The section as plain data: every key of the section, with nested sections as maps and lists as lists.
     *
     * @param section the section to convert; null gives an empty map
     */
    public static Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (section == null) {
            return out;
        }
        for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
            out.put(entry.getKey(), plain(entry.getValue()));
        }
        return out;
    }

    /**
     * A YAML file read from disk as plain data.
     *
     * @param file the file to read; a missing file gives an empty map
     */
    public static Map<String, Object> loadFile(File file) {
        if (!file.isFile()) {
            return new LinkedHashMap<>();
        }
        return toMap(YamlConfiguration.loadConfiguration(file));
    }

    /**
     * One named section of a YAML file, read as plain data.
     *
     * @param file the file to read
     * @param name the top-level key of the section
     * @return the section's data, or null when the file or the section is missing, so callers can tell the two apart from an empty section
     */
    public static Map<String, Object> loadSection(File file, String name) {
        if (!file.isFile()) {
            return null;
        }
        ConfigurationSection section = YamlConfiguration.loadConfiguration(file).getConfigurationSection(name);
        return section == null ? null : toMap(section);
    }

    /**
     * Writes plain data to a YAML file, creating its folder when needed. Each entry of the data becomes a top-level key.
     *
     * @param file the file to write
     * @param data the entries to write, as {@link #loadFile} would read them back
     * @throws IOException when the file cannot be written
     */
    public static void writeFile(File file, Map<String, Object> data) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        data.forEach(yaml::set);
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        yaml.save(file);
    }

    private static Object plain(Object value) {
        if (value instanceof ConfigurationSection section) {
            return toMap(section);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((key, element) -> out.put(String.valueOf(key), plain(element)));
            return out;
        }
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object element : list) {
                out.add(plain(element));
            }
            return out;
        }
        return value;
    }
}
