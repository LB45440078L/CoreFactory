package io.corefactory.core.geology;

import org.bukkit.Material;
import io.corefactory.core.config.ConfigValues;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * One ore type from {@code ores.yml}: the host block an ore block is placed as, what mining drops, the blocks an ore
 * block may replace and the height range it is shown in. Immutable. Built by {@link #parse}, which reads the file; the
 * constructor checks the ranges so a definition in code is held to the same rules.
 *
 * @param id      the key under {@code ores:}
 * @param name    display name, with ampersand colour codes
 * @param host    material name of the vanilla block the ore occupies in the world
 * @param drop    component id given when the ore is mined, or null for a fluid ore, which gives no item
 * @param dropMin fewest items one mined block gives; 0 for a fluid ore
 * @param dropMax most items one mined block gives; 0 for a fluid ore
 * @param replace material names an ore block may replace
 * @param minY    lowest y an ore block may be placed at
 * @param maxY    highest y an ore block may be placed at
 * @param volume  litres of petroleum one block holds, for a fluid ore such as a deposit; 0 for a count ore
 */
public record OreDefinition(
        String id,
        String name,
        String host,
        String drop,
        int dropMin,
        int dropMax,
        Set<String> replace,
        int minY,
        int maxY,
        int volume
) {

    /** The lowest y a registry entry can store. Matches the overworld's bottom. */
    public static final int MIN_Y = -64;

    /** The highest y a registry entry can store (the 10-bit offset from {@link #MIN_Y} tops out here). */
    public static final int MAX_Y = 959;

    /** A count ore: the same as the full constructor with no volume. */
    public OreDefinition(String id, String name, String host, String drop, int dropMin, int dropMax, Set<String> replace,
                         int minY, int maxY) {
        this(id, name, host, drop, dropMin, dropMax, replace, minY, maxY, 0);
    }

    public OreDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("has no id");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("has no host");
        }
        if (drop == null || drop.isBlank()) {
            if (volume <= 0) {
                throw new IllegalArgumentException("has no drop");
            }
            if (dropMin != 0 || dropMax != 0) {
                throw new IllegalArgumentException("has a drop-amount but no drop");
            }
            drop = null;
        } else if (dropMin < 1 || dropMin > dropMax) {
            throw new IllegalArgumentException("has an invalid drop-amount (need 1 <= min <= max)");
        }
        if (replace == null || replace.isEmpty()) {
            throw new IllegalArgumentException("has an empty replace list");
        }
        if (minY < MIN_Y || maxY > MAX_Y || minY >= maxY) {
            throw new IllegalArgumentException("has an invalid y range (need " + MIN_Y + " <= min-y < max-y <= "
                    + MAX_Y + ")");
        }
        if (volume < 0) {
            throw new IllegalArgumentException("has a negative volume");
        }
        name = name == null || name.isBlank() ? id : name;
        replace = Set.copyOf(replace);
    }

    /** True for a fluid ore, such as the crude petroleum deposit: a drill pumps it as litres, and mining gives no item. */
    public boolean fluid() {
        return drop == null;
    }

    /**
     * Reads every ore under the {@code ores:} section, in file order. An entry with a missing or bad field is logged
     * and skipped; the rest still load. Keys the ore no longer uses are ignored.
     *
     * @param ores the {@code ores:} section of {@code ores.yml} as plain data, or null when the file has none
     * @param log  receives one warning per skipped entry
     * @return the definitions in file order; an ore's position in this list is its index in the registry
     */
    public static List<OreDefinition> parse(Map<String, Object> ores, Logger log) {
        List<OreDefinition> out = new ArrayList<>();
        if (ores == null) {
            log.warning("ores.yml has no 'ores:' section; no ores were loaded.");
            return out;
        }
        for (String id : ores.keySet()) {
            Map<String, Object> section = ConfigValues.optionalSection(ores, id);
            if (section == null) {
                log.warning("ores.yml: '" + id + "' is not a section and was skipped.");
                continue;
            }
            if (out.size() >= 256) {
                log.warning("ores.yml: more than 256 ores; '" + id + "' and the rest were skipped.");
                break;
            }
            try {
                out.add(read(id, section));
            } catch (IllegalArgumentException e) {
                log.warning("ores.yml: '" + id + "' " + e.getMessage() + " and was skipped.");
            }
        }
        return out;
    }

    private static OreDefinition read(String id, Map<String, Object> section) {
        String host = block(ConfigValues.string(section, "host", null), "host");
        String drop = ConfigValues.string(section, "drop", null);
        boolean fluid = drop == null || drop.isBlank();
        int[] dropAmount = fluid ? new int[]{0, 0} : pair(section, "drop-amount");
        Set<String> replace = new LinkedHashSet<>();
        List<String> names = ConfigValues.stringList(section, "replace", List.of());
        if (names.isEmpty()) {
            throw new IllegalArgumentException("has an empty replace list");
        }
        for (String name : names) {
            replace.add(block(name, "replace entry"));
        }
        int minY = integer(section, "min-y");
        int maxY = integer(section, "max-y");
        return new OreDefinition(id, ConfigValues.string(section, "name", id), host, drop, dropAmount[0], dropAmount[1],
                replace, minY, maxY, ConfigValues.integer(section, "volume", 0));
    }

    private static String block(String name, String what) {
        Material material = name == null ? null : Material.matchMaterial(name);
        if (material == null) {
            throw new IllegalArgumentException("has an unknown block for " + what + " '" + name + "'");
        }
        return material.name();
    }

    private static int integer(Map<String, Object> section, String key) {
        if (!ConfigValues.isInteger(section, key)) {
            throw new IllegalArgumentException("has no integer '" + key + "'");
        }
        return ConfigValues.integer(section, key, 0);
    }

    private static int[] pair(Map<String, Object> section, String key) {
        List<Integer> values = ConfigValues.integerList(section, key, List.of());
        if (values.size() != 2) {
            throw new IllegalArgumentException("needs '" + key + "' as [min, max]");
        }
        return new int[]{values.get(0), values.get(1)};
    }
}
