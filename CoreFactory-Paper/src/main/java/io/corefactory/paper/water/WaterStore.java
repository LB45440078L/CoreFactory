package io.corefactory.paper.water;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.PumpFuel;
import io.corefactory.core.water.PumpLevels;
import io.corefactory.core.water.WaterOrientation;
import io.corefactory.core.water.WaterPart;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes {@code water.yml} in the plugin's data folder. The file holds a {@code version} and the devices as numbered entries.
 * A write goes to a {@code .tmp} file that is moved over the real one, so a crash during a write leaves the last good file. An entry that
 * cannot be read is skipped and reported.
 */
public final class WaterStore {

    /** The version written now. */
    public static final int VERSION = 1;

    private static final String VERSION_KEY = "version";
    private static final String DEVICES = "devices";

    private final File file;
    private final Logger log;

    /**
     * @param file the store file, normally {@code water.yml} in the data folder
     * @param log  receives the skipped entries and any failed write
     */
    public WaterStore(File file, Logger log) {
        this.file = file;
        this.log = log;
    }

    /** The saved devices, or none when the file is missing or cannot be read. */
    public List<WaterDevice> read() {
        if (!file.isFile()) {
            return List.of();
        }
        try {
            return decode(Files.readString(file.toPath(), StandardCharsets.UTF_8), log::warning);
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not read " + file.getName() + ". Water devices start empty.", e);
            return List.of();
        }
    }

    /** Writes the devices atomically: to a temporary file first, then moved over the store file. */
    public void write(List<WaterDevice> devices) {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        File temporary = new File(parent, file.getName() + ".tmp");
        try {
            Files.writeString(temporary.toPath(), encode(devices), StandardCharsets.UTF_8);
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

    /** The text of a store file holding the given devices. */
    static String encode(List<WaterDevice> devices) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(VERSION_KEY, VERSION);
        int index = 0;
        for (WaterDevice device : devices) {
            String base = DEVICES + "." + index++ + ".";
            yaml.set(base + "type", device.part().componentId());
            yaml.set(base + "id", device.id());
            yaml.set(base + "owner", device.owner().toString());
            yaml.set(base + "world", device.world().toString());
            yaml.set(base + "x", device.pos().x());
            yaml.set(base + "y", device.pos().y());
            yaml.set(base + "z", device.pos().z());
            if (device.face() != null) {
                yaml.set(base + "face", device.face().name().toLowerCase(Locale.ROOT));
            }
            if (device.output() != null) {
                yaml.set(base + "output", device.output().name().toLowerCase(Locale.ROOT));
            }
            yaml.set(base + "level", device.level());
            yaml.set(base + "fuel", device.burn().items());
            yaml.set(base + "burn-ticks", device.burn().ticksLeft());
        }
        return yaml.saveToString();
    }

    /**
     * The devices of a store file's text. An entry with a missing or invalid field is skipped and reported. A splitter with no face is
     * skipped too, and a splitter's output is the default of its input when none is saved. A pump with no face faces north.
     *
     * @param text     the file's text
     * @param warnings receives one message for each skipped entry
     */
    static List<WaterDevice> decode(String text, Consumer<String> warnings) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(text));
        List<WaterDevice> devices = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(DEVICES);
        if (root == null) {
            return devices;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection entry = root.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            Optional<WaterDevice> device = deviceOf(entry);
            if (device.isEmpty()) {
                warnings.accept("water.yml: a water entry is not valid and is skipped.");
                continue;
            }
            devices.add(device.get());
        }
        return devices;
    }

    private static Optional<WaterDevice> deviceOf(ConfigurationSection entry) {
        Optional<WaterPart> part = WaterPart.ofComponent(entry.getString("type"));
        UUID owner = uuidOf(entry.getString("owner"));
        UUID world = uuidOf(entry.getString("world"));
        String id = entry.getString("id");
        if (part.isEmpty() || owner == null || world == null || id == null) {
            return Optional.empty();
        }
        Face face = faceOf(entry.getString("face"));
        if (part.get().splitter() && face == null) {
            return Optional.empty();
        }
        if (part.get() == WaterPart.PUMP && face == null) {
            face = Face.NORTH;
        }
        if (part.get() == WaterPart.PIPE) {
            face = null;
        }
        int level = part.get() == WaterPart.PUMP ? Math.max(1, Math.min(entry.getInt("level", 1), PumpLevels.MAX_LEVEL)) : 1;
        int fuel = Math.max(0, Math.min(entry.getInt("fuel", 0), PumpFuel.CAPACITY));
        int ticks = Math.max(0, Math.min(entry.getInt("burn-ticks", 0), PumpFuel.ITEM_TICKS));
        WaterDevice device = new WaterDevice(id, part.get(), owner, world,
                new Pos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z")), face, level,
                new PumpFuel.Burn(fuel, ticks));
        if (part.get().splitter()) {
            device.setOutput(outputOf(entry.getString("output"), face));
        }
        return Optional.of(device);
    }

    /** A splitter's output as saved, or the output a splitter of that input gets when none is saved or the saved one is on its input's line. */
    private static Face outputOf(String text, Face input) {
        Face saved = faceOf(text);
        if (saved != null && saved != input && saved != input.opposite()) {
            return saved;
        }
        return input.horizontal() ? WaterOrientation.defaultOutput(input) : Face.SOUTH;
    }

    private static Face faceOf(String text) {
        if (text == null) {
            return null;
        }
        for (Face face : Face.values()) {
            if (face.name().equalsIgnoreCase(text.trim())) {
                return face;
            }
        }
        return null;
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
