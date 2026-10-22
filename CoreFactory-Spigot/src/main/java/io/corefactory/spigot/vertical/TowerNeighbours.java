package io.corefactory.spigot.vertical;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.Port;

import java.util.Optional;
import java.util.UUID;

/**
 * What the logistics network offers a vertical tower at its output cell: a regular belt that takes the items at its entry, or the port
 * of a machine, a chest or another storage. The logistics service implements it, so the tower runtime needs no network type.
 */
public interface TowerNeighbours {

    /** Whether a regular belt of the logistics network stands at the cell. */
    boolean isRegularBelt(UUID world, Pos cell);

    /** Puts an item at the entry of the regular belt at the cell, when the belt has room. Returns false when it did not take it. */
    boolean putOnRegularBelt(UUID world, Pos cell, Item item);

    /** The port at the cell (a machine, a chest or another storage), or empty when the cell gives none. */
    Optional<Port> portAt(UUID world, Pos cell, UUID owner);
}
