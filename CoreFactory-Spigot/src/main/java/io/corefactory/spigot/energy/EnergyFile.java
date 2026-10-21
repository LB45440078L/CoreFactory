package io.corefactory.spigot.energy;

import io.corefactory.core.energy.ChartMode;
import io.corefactory.core.energy.GeneratorFuel;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.energy.Pos;
import io.corefactory.spigot.energy.StoredEnergy.StoredAccumulator;
import io.corefactory.spigot.energy.StoredEnergy.StoredChart;
import io.corefactory.spigot.energy.StoredEnergy.StoredGenerator;
import io.corefactory.spigot.energy.StoredEnergy.StoredPylon;
import io.corefactory.spigot.energy.StoredEnergy.StoredWire;
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
 * The format of {@code energy.yml}: a {@code version} (2 now), the next grid id, and the pylons, accumulators, generators, wires and
 * charts as lists of numbered entries. Energy is in Wh, time in seconds. A file with no version is in the old units, which have no
 * conversion: its stored accumulator energy, the time left on the burning items and the generators' totals are reset to zero, and one
 * line says so. Positions, levels, fuel counts, wires and pylons are kept. The text is read and written as a string, so the round trip
 * is tested with an in-memory file. An entry that cannot be read is skipped and reported.
 */
public final class EnergyFile {

    /** The version written now. A file with a lower or no version is in the old units. */
    public static final int VERSION = 2;

    /** The line written when an old file's energy is reset. */
    public static final String LEGACY_RESET = "energy.yml is in the old units, which cannot be converted: the stored accumulator "
            + "energy, the burning time of the generators and their produced totals are reset to zero.";

    private static final String VERSION_KEY = "version";
    private static final String NEXT_GRID_ID = "next-grid-id";
    private static final String PYLONS = "pylons";
    private static final String ACCUMULATORS = "accumulators";
    private static final String GENERATORS = "generators";
    private static final String WIRES = "wires";
    private static final String CHARTS = "charts";

    private EnergyFile() {
    }

    /** The text of an energy file holding the given state. */
    public static String write(StoredEnergy energy) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(VERSION_KEY, VERSION);
        yaml.set(NEXT_GRID_ID, energy.nextGridId());
        int index = 0;
        for (StoredPylon pylon : energy.pylons()) {
            String base = PYLONS + "." + index++ + ".";
            yaml.set(base + "id", pylon.id());
            yaml.set(base + "world", pylon.world().toString());
            position(yaml, base, pylon.pos());
            yaml.set(base + "level", pylon.level());
            if (pylon.grid() != StoredEnergy.NO_GRID) {
                yaml.set(base + "grid", pylon.grid());
            }
        }
        index = 0;
        for (StoredAccumulator accumulator : energy.accumulators()) {
            String base = ACCUMULATORS + "." + index++ + ".";
            yaml.set(base + "id", accumulator.id());
            yaml.set(base + "world", accumulator.world().toString());
            position(yaml, base, accumulator.pos());
            yaml.set(base + "level", accumulator.level());
            yaml.set(base + "stored-wh", accumulator.stored());
            yaml.set(base + "discharging", accumulator.discharging());
        }
        index = 0;
        for (StoredGenerator generator : energy.generators()) {
            String base = GENERATORS + "." + index++ + ".";
            yaml.set(base + "id", generator.id());
            yaml.set(base + "world", generator.world().toString());
            position(yaml, base, generator.pos());
            yaml.set(base + "kind", generator.kind().name());
            yaml.set(base + "fuel", generator.fuel());
            yaml.set(base + "remaining-seconds", generator.remainingSeconds());
            yaml.set(base + "total-wh", generator.totalWh());
        }
        index = 0;
        for (StoredWire wire : energy.wires()) {
            String base = WIRES + "." + index++ + ".";
            yaml.set(base + "id", wire.id());
            yaml.set(base + "from", wire.from());
            yaml.set(base + "to", wire.to());
        }
        index = 0;
        for (StoredChart chart : energy.charts()) {
            String base = CHARTS + "." + index++ + ".";
            yaml.set(base + "name", chart.name());
            yaml.set(base + "world", chart.world().toString());
            position(yaml, base, chart.pos());
            yaml.set(base + "grid", chart.grid());
            yaml.set(base + "mode", chart.mode().name());
        }
        return yaml.saveToString();
    }

    /**
     * The state of an energy file's text. An entry with a missing or invalid field is skipped and reported to the warnings. A file
     * with no version is in the old units, and is migrated by {@link #resetLegacyUnits(StoredEnergy)}, with one warning.
     *
     * @param text     the file's text
     * @param warnings receives one message for each skipped entry, and the legacy reset line
     */
    public static StoredEnergy read(String text, Consumer<String> warnings) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(text));
        List<StoredPylon> pylons = new ArrayList<>();
        for (ConfigurationSection entry : entries(yaml, PYLONS)) {
            UUID world = parseUuid(entry.getString("world"));
            Integer level = levelOf(entry, 1, 3);
            if (entry.getString("id") == null || world == null || level == null) {
                warnings.accept("energy.yml: a pylon entry is not valid and is skipped.");
                continue;
            }
            int grid = entry.getInt("grid", StoredEnergy.NO_GRID);
            pylons.add(new StoredPylon(entry.getString("id"), world, position(entry), level,
                    grid < 0 ? StoredEnergy.NO_GRID : grid));
        }
        List<StoredAccumulator> accumulators = new ArrayList<>();
        for (ConfigurationSection entry : entries(yaml, ACCUMULATORS)) {
            UUID world = parseUuid(entry.getString("world"));
            Integer level = levelOf(entry, 1, 3);
            if (entry.getString("id") == null || world == null || level == null) {
                warnings.accept("energy.yml: an accumulator entry is not valid and is skipped.");
                continue;
            }
            accumulators.add(new StoredAccumulator(entry.getString("id"), world, position(entry), level,
                    Math.max(0.0, entry.getDouble("stored-wh")), entry.getBoolean("discharging")));
        }
        List<StoredGenerator> generators = new ArrayList<>();
        for (ConfigurationSection entry : entries(yaml, GENERATORS)) {
            UUID world = parseUuid(entry.getString("world"));
            GeneratorKind kind = kindOf(entry.getString("kind"));
            if (entry.getString("id") == null || world == null || kind == null) {
                warnings.accept("energy.yml: a generator entry is not valid and is skipped.");
                continue;
            }
            generators.add(new StoredGenerator(entry.getString("id"), world, position(entry), kind,
                    GeneratorFuel.countOf(entry.getInt("fuel")), Math.max(0.0, entry.getDouble("remaining-seconds")),
                    Math.max(0.0, entry.getDouble("total-wh"))));
        }
        List<StoredWire> wires = new ArrayList<>();
        for (ConfigurationSection entry : entries(yaml, WIRES)) {
            if (entry.getString("id") == null || entry.getString("from") == null || entry.getString("to") == null) {
                warnings.accept("energy.yml: a wire entry is not valid and is skipped.");
                continue;
            }
            wires.add(new StoredWire(entry.getString("id"), entry.getString("from"), entry.getString("to")));
        }
        List<StoredChart> charts = new ArrayList<>();
        for (ConfigurationSection entry : entries(yaml, CHARTS)) {
            UUID world = parseUuid(entry.getString("world"));
            ChartMode mode = modeOf(entry.getString("mode"));
            if (entry.getString("name") == null || world == null || mode == null || !entry.contains("grid")) {
                warnings.accept("energy.yml: a chart entry is not valid and is skipped.");
                continue;
            }
            charts.add(new StoredChart(entry.getString("name"), world, position(entry), entry.getInt("grid"), mode));
        }
        StoredEnergy state = new StoredEnergy(pylons, accumulators, generators, wires,
                Math.max(0, yaml.getInt(NEXT_GRID_ID, 0)), charts);
        if (yaml.getInt(VERSION_KEY, 1) >= VERSION) {
            return state;
        }
        warnings.accept(LEGACY_RESET);
        return resetLegacyUnits(state);
    }

    /**
     * The migration of a file in the old units: the stored accumulator energy, the burning time, and the produced totals of the
     * generators are zero. Everything else is kept.
     *
     * @param legacy the state read from the old file
     */
    public static StoredEnergy resetLegacyUnits(StoredEnergy legacy) {
        List<StoredAccumulator> accumulators = new ArrayList<>();
        for (StoredAccumulator accumulator : legacy.accumulators()) {
            accumulators.add(new StoredAccumulator(accumulator.id(), accumulator.world(), accumulator.pos(), accumulator.level(),
                    0.0, false));
        }
        List<StoredGenerator> generators = new ArrayList<>();
        for (StoredGenerator generator : legacy.generators()) {
            generators.add(new StoredGenerator(generator.id(), generator.world(), generator.pos(), generator.kind(),
                    generator.fuel(), 0.0, 0.0));
        }
        return new StoredEnergy(legacy.pylons(), accumulators, generators, legacy.wires(), legacy.nextGridId(), legacy.charts());
    }

    private static GeneratorKind kindOf(String text) {
        if (text == null) {
            return null;
        }
        try {
            return GeneratorKind.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ChartMode modeOf(String text) {
        if (text == null) {
            return null;
        }
        try {
            return ChartMode.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
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

    private static Integer levelOf(ConfigurationSection entry, int min, int max) {
        int level = entry.getInt("level", 0);
        return level >= min && level <= max ? level : null;
    }

    private static UUID parseUuid(String text) {
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
