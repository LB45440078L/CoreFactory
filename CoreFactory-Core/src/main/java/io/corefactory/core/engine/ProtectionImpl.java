package io.corefactory.core.engine;

import io.corefactory.api.ProtectionService;
import io.corefactory.core.config.CoreSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Blast protection: fixed unbreakable materials, plus the anti-TNT fields of anchor TNT.
 */
public final class ProtectionImpl implements ProtectionService {

    /** An active anchor field: its centre and radius. */
    public record Field(Location centre, double radius) {
    }

    private Set<Material> unbreakable;
    private final Map<String, Field> anchors = new LinkedHashMap<>();
    private final List<ClaimGuard> guards = new ArrayList<>();

    public ProtectionImpl(CoreSettings settings) {
        this.unbreakable = settings.unbreakable();
    }

    public void reconfigure(CoreSettings settings) {
        this.unbreakable = settings.unbreakable();
    }

    @Override
    public boolean isProtected(Block block) {
        // Cheapest checks first, and no Location is allocated unless an anchor exists.
        if (unbreakable.contains(block.getType()) || (!anchors.isEmpty() && inAnchorField(block.getLocation()))) {
            return true;
        }
        for (ClaimGuard guard : guards) {
            if (guard.protects(block)) {
                return true;
            }
        }
        return false;
    }

    /** Adds a claim or region system. Call before the first blast; the list is read on every block. */
    public void addGuard(ClaimGuard guard) {
        guards.add(guard);
    }

    /** Passes the new tick on to every guard so it can drop its per-tick caches. */
    public void beginTick() {
        for (ClaimGuard guard : guards) {
            guard.beginTick();
        }
    }

    @Override
    public boolean inAnchorField(Location location) {
        if (anchors.isEmpty()) {
            return false;
        }
        for (Field field : anchors.values()) {
            if (field.centre().getWorld() == location.getWorld()
                    && field.centre().distanceSquared(location) <= field.radius() * field.radius()) {
                return true;
            }
        }
        return false;
    }

    /** Registers the anchor field of the TNT at {@code key}. */
    public void addAnchor(String key, Location centre, double radius) {
        anchors.put(key, new Field(centre, radius));
    }

    public void removeAnchor(String key) {
        anchors.remove(key);
    }

    public void clearAnchors() {
        anchors.clear();
    }
}
