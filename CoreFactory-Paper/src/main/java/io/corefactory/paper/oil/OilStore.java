package io.corefactory.paper.oil;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.oil.Fluid;
import io.corefactory.core.oil.OilPart;
import io.corefactory.core.water.Face;
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
 * Reads and writes {@code oil.yml} in the plugin's data folder. The file holds a {@code version} and the tubes as numbered entries. Each
 * tube keeps its fluid and its litres, when it holds any. A write goes to a {@code .tmp} file that is moved over the real one, so a crash
 * during a write leaves the last good file. An entry that cannot be read is skipped and reported.
 */
public final class OilStore {

    /** The version written now. Version 2 keeps each tube's fluid and litres; version 1 files read as empty tubes. */
    public static final int VERSION = 2;

    private static final String VERSION_KEY = "version";
    private static final String DEVICES = "devices";

    private final File file;
    private final Logger log;

    /**
     * @param file the store file, normally {@code oil.yml} in the data folder
     * @param log  receives the skipped entries and any failed write
     */
    public OilStore(File file, Logger log) {
        this.file = file;
        this.log = log;
    }

    /**
     * The saved tubes, or none when the file is missing or cannot be read.
     *
     * @param tubeCapacityLitres the litres each tube holds, from the oil settings; a saved amount above it is cut to it
     */
    public List<OilDevice> read(double tubeCapacityLitres) {
        if (!file.isFile()) {
            return List.of();
        }
        try {
            return decode(Files.readString(file.toPath(), StandardCharsets.UTF_8), tubeCapacityLitres, log::warning);
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not read " + file.getName() + ". Oil tubes start empty.", e);
            return List.of();
        }
    }

    /** Writes the tubes atomically: to a temporary file first, then moved over the store file. */
    public void write(List<OilDevice> devices) {
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

    /** The text of a store file holding the given tubes. */
    static String encode(List<OilDevice> devices) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(VERSION_KEY, VERSION);
        int index = 0;
        for (OilDevice device : devices) {
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
            Optional<Fluid> fluid = device.tank().fluid();
            if (fluid.isPresent()) {
                yaml.set(base + "fluid", fluid.get().id());
                yaml.set(base + "litres", device.tank().litres());
            }
        }
        return yaml.saveToString();
    }

    /**
     * The tubes of a store file's text. An entry with a missing or invalid field is skipped and reported. A splitter with no face is skipped
     * too, and a splitter's output is the default of its input when none is saved. A pipe has no face. A tube's fluid is restored when the
     * entry names a fluid and a positive amount.
     *
     * @param text                the file's text
     * @param tubeCapacityLitres  the litres each tube holds
     * @param warnings            receives one message for each skipped entry
     */
    static List<OilDevice> decode(String text, double tubeCapacityLitres, Consumer<String> warnings) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(text));
        List<OilDevice> devices = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(DEVICES);
        if (root == null) {
            return devices;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection entry = root.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            Optional<OilDevice> device = deviceOf(entry, tubeCapacityLitres);
            if (device.isEmpty()) {
                warnings.accept("oil.yml: an oil entry is not valid and is skipped.");
                continue;
            }
            devices.add(device.get());
        }
        return devices;
    }

    private static Optional<OilDevice> deviceOf(ConfigurationSection entry, double tubeCapacityLitres) {
        Optional<OilPart> part = OilPart.ofComponent(entry.getString("type"));
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
        if (!part.get().splitter()) {
            face = null;
        }
        OilDevice device = new OilDevice(id, part.get(), owner, world,
                new Pos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z")), face, tubeCapacityLitres);
        if (part.get().splitter()) {
            device.setOutput(outputOf(entry.getString("output"), face));
        }
        fluidOf(device, entry);
        return Optional.of(device);
    }

    /** Refills a tube's tank from its saved fluid and litres. The tank takes no more than its capacity. */
    private static void fluidOf(OilDevice device, ConfigurationSection entry) {
        Optional<Fluid> fluid = Fluid.ofId(entry.getString("fluid"));
        double litres = entry.getDouble("litres");
        if (fluid.isPresent() && Double.isFinite(litres) && litres > 0) {
            device.tank().accept(fluid.get(), litres);
        }
    }

    /** A splitter's output as saved, or the output a splitter of that input gets when none is saved or the saved one is on its input's line. */
    private static Face outputOf(String text, Face input) {
        Face saved = faceOf(text);
        if (saved != null && saved != input && saved != input.opposite()) {
            return saved;
        }
        return OilDevice.defaultOutput(input);
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
