package io.corefactory.api.event;

import io.corefactory.api.TntDefinition;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;

/** Fired before a TNT mechanic runs. Cancelling it turns the detonation into nothing. */
public final class TntDetonateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final TntDefinition definition;
    private final Location origin;
    private final Entity source;
    private final Vector direction;
    private final int depth;
    private boolean cancelled;

    public TntDetonateEvent(TntDefinition definition, Location origin, Entity source, Vector direction, int depth) {
        this.definition = definition;
        this.origin = origin;
        this.source = source;
        this.direction = direction;
        this.depth = depth;
    }

    public TntDefinition getDefinition() {
        return definition;
    }

    public Location getOrigin() {
        return origin;
    }

    /** The entity that started the chain, or {@code null}. */
    public Entity getSource() {
        return source;
    }

    /** The placement direction, or {@code null}. */
    public Vector getDirection() {
        return direction;
    }

    /** Chain depth: 0 for a direct ignition. */
    public int getDepth() {
        return depth;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
