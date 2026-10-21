package io.corefactory.api.event;

import io.corefactory.api.PlacedTnt;
import io.corefactory.api.TntDefinition;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired before a placed custom TNT is ignited. Cancelling it leaves the block untouched. */
public final class TntIgniteEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PlacedTnt placed;
    private final TntDefinition definition;
    private final Entity source;
    private boolean cancelled;

    public TntIgniteEvent(PlacedTnt placed, TntDefinition definition, Entity source) {
        this.placed = placed;
        this.definition = definition;
        this.source = source;
    }

    public PlacedTnt getPlaced() {
        return placed;
    }

    public TntDefinition getDefinition() {
        return definition;
    }

    /** The entity that lit the TNT, or {@code null} for environmental causes. */
    public Entity getSource() {
        return source;
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
