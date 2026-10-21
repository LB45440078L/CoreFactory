package io.corefactory.core.geology;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * One ore type from {@code ores.yml}: the host block it is placed as, what mining drops, the blocks its veins may
 * replace and the range they are laid in. Immutable. Built by {@link #parse}, which reads the file; the constructor
 * checks the ranges so a definition in code is held to the same rules.
 *
 * @param id            the key under {@code ores:}
 * @param name          display name, with ampersand colour codes
 * @param host          material name of the vanilla block the ore occupies in the world
 * @param drop          component id given when the ore is mined
 * @param dropMin       fewest items one mined block gives
 * @param dropMax       most items one mined block gives
 * @param replace       material names a vein may replace
 * @param minY          lowest y a vein may reach
 * @param maxY          highest y a vein may reach
 * @param veinsPerChunk expected number of veins per chunk
 * @param veinSizeMin   fewest blocks in one vein
 * @param veinSizeMax   most blocks in one vein
 * @param volume        units of petroleum one block holds, for a volume ore such as a deposit; 0 for a count ore
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
        double veinsPerChunk,
        int veinSizeMin,
        int veinSizeMax,
        int volume
) {

    /** The lowest y a registry entry can store. Matches the overworld's bottom. */
    public static final int MIN_Y = -64;

    /** The highest y a registry entry can store (the 10-bit offset from {@link #MIN_Y} tops out here). */
    public static final int MAX_Y = 959;

    /** A count ore: the same as the full constructor with no volume. */
    public OreDefinition(String id, String name, String host, String drop, int dropMin, int dropMax, Set<String> replace,
                         int minY, int maxY, double veinsPerChunk, int veinSizeMin, int veinSizeMax) {
        this(id, name, host, drop, dropMin, dropMax, replace, minY, maxY, veinsPerChunk, veinSizeMin, veinSizeMax, 0);
    }

    public OreDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("has no id");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("has no host");
        }
        if (drop == null || drop.isBlank()) {
            throw new IllegalArgumentException("has no drop");
        }
        if (replace == null || replace.isEmpty()) {
            throw new IllegalArgumentException("has an empty replace list");
        }
        if (dropMin < 1 || dropMin > dropMax) {
            throw new IllegalArgumentException("has an invalid drop-amount (need 1 <= min <= max)");
        }
        if (minY < MIN_Y || maxY > MAX_Y || minY >= maxY) {
            throw new IllegalArgumentException("has an invalid y range (need " + MIN_Y + " <= min-y < max-y <= "
                    + MAX_Y + ")");
        }
        if (Double.isNaN(veinsPerChunk) || Double.isInfinite(veinsPerChunk) || veinsPerChunk < 0) {
            throw new IllegalArgumentException("has an invalid veins-per-chunk");
        }
        if (veinSizeMin < 1 || veinSizeMin > veinSizeMax) {
            throw new IllegalArgumentException("has an invalid vein-size (need 1 <= min <= max)");
        }
        if (volume < 0) {
            throw new IllegalArgumentException("has a negative volume");
        }
        name = name == null || name.isBlank() ? id : name;
        replace = Set.copyOf(replace);
    }

    /**
     * This ore with its own host block also counted as replaceable. Retrofit uses it to record ores that were already
     * placed, so a plan can find them again. The planner stays deterministic, so the same positions come back.
     */
    public OreDefinition withHostReplaceable() {
        Set<String> widened = new LinkedHashSet<>(replace);
        widened.add(host);
        return new OreDefinition(id, name, host, drop, dropMin, dropMax, widened, minY, maxY, veinsPerChunk,
                veinSizeMin, veinSizeMax, volume);
    }

    /**
     * Reads every ore under the {@code ores:} section, in file order. An entry with a missing or bad field is logged
     * and skipped; the rest still load.
     *
     * @param ores the {@code ores:} section of {@code ores.yml}, or null when the file has none
     * @param log  receives one warning per skipped entry
     * @return the definitions in file order; an ore's position in this list is its index in the registry
     */
    public static List<OreDefinition> parse(ConfigurationSection ores, Logger log) {
        List<OreDefinition> out = new ArrayList<>();
        if (ores == null) {
            log.warning("ores.yml has no 'ores:' section; no ores were loaded.");
            return out;
        }
        for (String id : ores.getKeys(false)) {
            ConfigurationSection section = ores.getConfigurationSection(id);
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

    private static OreDefinition read(String id, ConfigurationSection section) {
        String host = block(section.getString("host"), "host");
        String drop = section.getString("drop");
        if (drop == null || drop.isBlank()) {
            throw new IllegalArgumentException("has no drop");
        }
        int[] dropAmount = pair(section, "drop-amount");
        int[] veinSize = pair(section, "vein-size");
        Set<String> replace = new LinkedHashSet<>();
        List<String> names = section.getStringList("replace");
        if (names.isEmpty()) {
            throw new IllegalArgumentException("has an empty replace list");
        }
        for (String name : names) {
            replace.add(block(name, "replace entry"));
        }
        int minY = integer(section, "min-y");
        int maxY = integer(section, "max-y");
        Object veins = section.get("veins-per-chunk");
        if (!(veins instanceof Number number)) {
            throw new IllegalArgumentException("has no veins-per-chunk");
        }
        return new OreDefinition(id, section.getString("name", id), host, drop, dropAmount[0], dropAmount[1],
                replace, minY, maxY, number.doubleValue(), veinSize[0], veinSize[1], section.getInt("volume", 0));
    }

    private static String block(String name, String what) {
        Material material = name == null ? null : Material.matchMaterial(name);
        if (material == null) {
            throw new IllegalArgumentException("has an unknown block for " + what + " '" + name + "'");
        }
        return material.name();
    }

    private static int integer(ConfigurationSection section, String key) {
        if (!section.isInt(key)) {
            throw new IllegalArgumentException("has no integer '" + key + "'");
        }
        return section.getInt(key);
    }

    private static int[] pair(ConfigurationSection section, String key) {
        List<Integer> values = section.getIntegerList(key);
        if (values.size() != 2) {
            throw new IllegalArgumentException("needs '" + key + "' as [min, max]");
        }
        return new int[]{values.get(0), values.get(1)};
    }
}
