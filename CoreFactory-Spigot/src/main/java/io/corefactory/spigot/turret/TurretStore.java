package io.corefactory.spigot.turret;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.turret.RoundType;
import io.corefactory.core.turret.TargetPolicy;
import io.corefactory.core.turret.TurretLevels;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes {@code turrets.yml} in the plugin's data folder. The file holds a {@code version} and the turrets as numbered entries.
 * A write goes to a {@code .tmp} file that is then moved over the real one, so a crash during a write leaves the last good file. An entry
 * that cannot be read is skipped and reported.
 *
 * <p>An entry holds its {@code type} (the kind), its {@code level}, its {@code team-mode}, its {@code friendly-fire} switch, its per-turret
 * {@code targets} overrides, and its {@code players} stances by unique id. An entry written before the levels existed loads at level 1, with
 * no stances, the team spared, and a machine gun when it has no type.
 */
public final class TurretStore {

    /** The version written now. The fields added since version 1 are optional, so version 1 files still read. */
    public static final int VERSION = 1;

    private static final String VERSION_KEY = "version";
    private static final String TURRETS = "turrets";

    private final File file;
    private final Logger log;

    /**
     * @param file the store file, normally {@code turrets.yml} in the data folder
     * @param log  receives the skipped entries and any failed write
     */
    public TurretStore(File file, Logger log) {
        this.file = file;
        this.log = log;
    }

    /** The saved turrets, or none when the file is missing or cannot be read. */
    public List<StoredTurret> read() {
        if (!file.isFile()) {
            return List.of();
        }
        try {
            return decode(Files.readString(file.toPath(), StandardCharsets.UTF_8), log::warning);
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not read " + file.getName() + ". Turrets start empty.", e);
            return List.of();
        }
    }

    /** Writes the turrets atomically: to a temporary file first, then moved over the store file. */
    public void write(List<StoredTurret> turrets) {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        File temporary = new File(parent, file.getName() + ".tmp");
        try {
            Files.writeString(temporary.toPath(), encode(turrets), StandardCharsets.UTF_8);
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

    /** The text of a store file holding the given turrets. */
    static String encode(List<StoredTurret> turrets) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(VERSION_KEY, VERSION);
        int index = 0;
        for (StoredTurret turret : turrets) {
            String base = TURRETS + "." + index++ + ".";
            yaml.set(base + "type", turret.kind().deviceType());
            yaml.set(base + "id", turret.id());
            yaml.set(base + "owner", turret.owner().toString());
            yaml.set(base + "world", turret.world().toString());
            yaml.set(base + "x", turret.pos().x());
            yaml.set(base + "y", turret.pos().y());
            yaml.set(base + "z", turret.pos().z());
            yaml.set(base + "facing", turret.facing());
            yaml.set(base + "level", turret.level());
            if (turret.round() != null) {
                yaml.set(base + "round", turret.round().itemId());
            }
            yaml.set(base + "rounds", turret.rounds());
            yaml.set(base + "friendly-fire", turret.policy().friendlyFire());
            yaml.set(base + "team-mode", turret.policy().teamMode().name().toLowerCase(Locale.ROOT));
            for (Map.Entry<String, Boolean> override : turret.policy().overrides().entrySet()) {
                yaml.set(base + "targets." + override.getKey(), override.getValue());
            }
            for (Map.Entry<UUID, TargetPolicy.PlayerStance> stance : turret.policy().players().entrySet()) {
                yaml.set(base + "players." + stance.getKey(), stance.getValue().name().toLowerCase(Locale.ROOT));
            }
        }
        return yaml.saveToString();
    }

    /**
     * The turrets of a store file's text. An entry with a missing or invalid field, or a device type no turret has, is skipped and reported.
     * An entry with no type is a machine gun turret, as the file was written before the laser turret existed. An entry with no level is at
     * level 1.
     *
     * @param text     the file's text
     * @param warnings receives one message for each skipped entry
     */
    static List<StoredTurret> decode(String text, Consumer<String> warnings) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(text));
        List<StoredTurret> turrets = new ArrayList<>();
        for (ConfigurationSection entry : entries(yaml)) {
            UUID owner = uuidOf(entry.getString("owner"));
            UUID world = uuidOf(entry.getString("world"));
            Optional<TurretKind> kind = TurretKind.ofDeviceType(entry.getString("type", TurretKind.MACHINE_GUN.deviceType()));
            if (entry.getString("id") == null || owner == null || world == null || kind.isEmpty()) {
                warnings.accept("turrets.yml: a turret entry is not valid and is skipped.");
                continue;
            }
            RoundType round = kind.get() == TurretKind.MACHINE_GUN && entry.getString("round") != null
                    ? RoundType.ofItem(entry.getString("round")).orElse(null) : null;
            int rounds = round == null ? 0 : Math.max(0, entry.getInt("rounds"));
            int level = Math.max(1, Math.min(entry.getInt("level", 1), TurretLevels.MAX_LEVEL));
            turrets.add(new StoredTurret(entry.getString("id"), kind.get(), owner, world,
                    new Pos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z")),
                    entry.getDouble("facing"), round, rounds, level, policyOf(entry)));
        }
        return turrets;
    }

    /**
     * The targeting choices of an entry. An entry written before they existed keeps the defaults: no stances, and the team spared.
     */
    private static TargetPolicy policyOf(ConfigurationSection entry) {
        Map<String, Boolean> overrides = new HashMap<>();
        ConfigurationSection targets = entry.getConfigurationSection("targets");
        if (targets != null) {
            for (String key : targets.getKeys(false)) {
                overrides.put(key, targets.getBoolean(key));
            }
        }
        Map<UUID, TargetPolicy.PlayerStance> players = new HashMap<>();
        ConfigurationSection stances = entry.getConfigurationSection("players");
        if (stances != null) {
            for (String key : stances.getKeys(false)) {
                UUID player = uuidOf(key);
                Optional<TargetPolicy.PlayerStance> stance = stanceOf(stances.getString(key));
                if (player != null && stance.isPresent()) {
                    players.put(player, stance.get());
                }
            }
        }
        return new TargetPolicy(entry.getBoolean("friendly-fire", false), overrides, players, teamModeOf(entry.getString("team-mode")));
    }

    private static Optional<TargetPolicy.PlayerStance> stanceOf(String text) {
        if (text == null) {
            return Optional.empty();
        }
        for (TargetPolicy.PlayerStance stance : TargetPolicy.PlayerStance.values()) {
            if (stance.name().equalsIgnoreCase(text.trim())) {
                return Optional.of(stance);
            }
        }
        return Optional.empty();
    }

    private static TargetPolicy.TeamMode teamModeOf(String text) {
        if (text != null) {
            for (TargetPolicy.TeamMode mode : TargetPolicy.TeamMode.values()) {
                if (mode.name().equalsIgnoreCase(text.trim())) {
                    return mode;
                }
            }
        }
        return TargetPolicy.TeamMode.SPARE;
    }

    private static List<ConfigurationSection> entries(YamlConfiguration yaml) {
        ConfigurationSection root = yaml.getConfigurationSection(TURRETS);
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
