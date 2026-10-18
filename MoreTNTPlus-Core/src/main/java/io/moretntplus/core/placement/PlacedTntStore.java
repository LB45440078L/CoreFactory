package io.moretntplus.core.placement;

import io.moretntplus.api.PlacedTnt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The registry of every placed custom TNT block, keyed by block position, with YAML persistence.
 */
public final class PlacedTntStore {

    private final Map<String, PlacedTnt> byKey = new HashMap<>();
    /** Entries read from disk whose world was not loaded yet. They are written back untouched, never dropped. */
    private final List<Map<?, ?>> preserved = new ArrayList<>();

    /** The position key of a block location: world UUID and integer block coordinates. */
    public static String key(Location location) {
        return key(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static String key(UUID world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    public Optional<PlacedTnt> get(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byKey.get(key(location)));
    }

    /** Registers a block. Replaces any entry already at that position. */
    public PlacedTnt put(Location blockLocation, String tntId) {
        Location block = new Location(blockLocation.getWorld(),
                blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ());
        PlacedTnt placed = new PlacedTnt(block, tntId);
        byKey.put(key(block), placed);
        return placed;
    }

    public Optional<PlacedTnt> remove(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byKey.remove(key(location)));
    }

    public Collection<PlacedTnt> all() {
        return Collections.unmodifiableCollection(byKey.values());
    }

    public int size() {
        return byKey.size();
    }

    public void save(File file, Logger log) {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Object> entries = new ArrayList<>(byKey.size() + preserved.size());
        for (PlacedTnt placed : byKey.values()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("block", toMap(placed.location()));
            entry.put("id", placed.tntId());
            if (placed.armed()) {
                entry.put("armed", true);
            }
            if (placed.direction() != null) {
                entry.put("direction", List.of(placed.direction().getX(), placed.direction().getY(),
                        placed.direction().getZ()));
            }
            if (placed.target() != null) {
                entry.put("target", toMap(placed.target()));
            }
            if (!placed.links().isEmpty()) {
                List<Map<String, Object>> links = new ArrayList<>();
                placed.links().forEach(link -> links.add(toMap(link)));
                entry.put("links", links);
            }
            if (placed.owner() != null) {
                entry.put("owner", placed.owner().toString());
            }
            if (placed.displayId() != null) {
                entry.put("display", placed.displayId().toString());
            }
            entries.add(entry);
        }
        entries.addAll(preserved);
        yaml.set("placed", entries);
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not save placed TNT to " + file.getName(), e);
        }
    }

    public void load(File file, Logger log) {
        byKey.clear();
        preserved.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        int skipped = 0;
        for (Map<?, ?> entry : yaml.getMapList("placed")) {
            Location block = fromMap(asMap(entry.get("block")));
            String id = entry.get("id") instanceof String s ? s : null;
            if (id == null) {
                skipped++;
                continue;
            }
            if (block == null) {
                // Its world is not loaded yet (a multiworld plugin, say): keep the entry as it is.
                preserved.add(entry);
                continue;
            }
            PlacedTnt placed = put(block, id);
            if (Boolean.TRUE.equals(entry.get("armed"))) {
                placed.setArmed(true);
            }
            if (entry.get("direction") instanceof List<?> d && d.size() == 3) {
                placed.setDirection(new Vector(number(d.get(0)), number(d.get(1)), number(d.get(2))));
            }
            if (entry.get("target") != null) {
                placed.setTarget(fromMap(asMap(entry.get("target"))));
            }
            if (entry.get("links") instanceof List<?> links) {
                for (Object link : links) {
                    Location target = fromMap(asMap(link));
                    if (target != null) {
                        placed.links().add(target);
                    }
                }
            }
            if (entry.get("owner") instanceof String owner) {
                placed.setOwner(parseUuid(owner));
            }
            if (entry.get("display") instanceof String display) {
                placed.setDisplayId(parseUuid(display));
            }
        }
        if (skipped > 0) {
            log.warning("Skipped " + skipped + " placed TNT entries with no type id.");
        }
        if (!preserved.isEmpty()) {
            log.warning(preserved.size() + " placed TNT entries belong to worlds that are not loaded; "
                    + "they are kept on disk and restored when the server next starts with those worlds.");
        }
    }

    private static Map<String, Object> toMap(Location location) {
        Map<String, Object> map = new HashMap<>();
        map.put("world", location.getWorld().getUID().toString());
        map.put("x", location.getBlockX());
        map.put("y", location.getBlockY());
        map.put("z", location.getBlockZ());
        return map;
    }

    private static Location fromMap(Map<?, ?> map) {
        if (map == null) {
            return null;
        }
        UUID worldId = map.get("world") instanceof String s ? parseUuid(s) : null;
        World world = worldId == null ? null : Bukkit.getWorld(worldId);
        if (world == null) {
            return null;
        }
        return new Location(world, number(map.get("x")), number(map.get("y")), number(map.get("z")));
    }

    private static Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> map ? map : null;
    }

    private static double number(Object value) {
        return value instanceof Number n ? n.doubleValue() : 0.0;
    }

    private static UUID parseUuid(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
