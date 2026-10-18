package io.moretntplus.api;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A custom TNT block that has been placed in the world, and the state attached to it.
 *
 * <p>Instances are owned by the core registry and mutated on the main thread only. The location is
 * always a block location (integer coordinates, zero yaw and pitch).
 */
public final class PlacedTnt {

    private final Location location;
    private final String tntId;
    private Vector direction;
    private Location target;
    private final List<Location> links = new ArrayList<>();
    private UUID owner;
    private UUID displayId;
    private boolean armed;
    private long armedAtTick;
    private int chainDepth;

    public PlacedTnt(Location location, String tntId) {
        this.location = location;
        this.tntId = tntId;
    }

    public Location location() {
        return location;
    }

    public String tntId() {
        return tntId;
    }

    /** Horizontal facing used by directional mechanics (shaped charge, napalm), or {@code null}. */
    public Vector direction() {
        return direction;
    }

    public void setDirection(Vector direction) {
        this.direction = direction;
    }

    /** Teleport destination of a teleport TNT, or {@code null} for a random offset. */
    public Location target() {
        return target;
    }

    public void setTarget(Location target) {
        this.target = target;
    }

    /** Other placed TNT this block is fuse-linked to. The list is live; copy it before iterating if you mutate it. */
    public List<Location> links() {
        return links;
    }

    public UUID owner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    /** The item display that shows this block's custom texture, or {@code null} when none is shown. */
    public UUID displayId() {
        return displayId;
    }

    public void setDisplayId(UUID displayId) {
        this.displayId = displayId;
    }

    /** Proximity TNT: whether the trap has finished arming. */
    public boolean armed() {
        return armed;
    }

    public void setArmed(boolean armed) {
        this.armed = armed;
    }

    /** Server tick at which a proximity TNT finished arming. */
    public long armedAtTick() {
        return armedAtTick;
    }

    /** How many chain links led to this TNT being ignited; a direct ignition is 0. */
    public int chainDepth() {
        return chainDepth;
    }

    public void setChainDepth(int chainDepth) {
        this.chainDepth = chainDepth;
    }

    public void setArmedAtTick(long armedAtTick) {
        this.armedAtTick = armedAtTick;
    }
}
