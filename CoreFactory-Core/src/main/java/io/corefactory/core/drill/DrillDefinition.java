package io.corefactory.core.drill;

import io.corefactory.core.process.Machine;
import io.corefactory.core.process.ItemRef;

/**
 * One placeable drill, read from the {@code drills:} section of processes.yml: its id and display name, the vanilla block
 * it stands on, its kind and its level.
 *
 * @param id    the component id, such as {@code petroleum_drill_1}
 * @param name  the display name, with colour codes
 * @param host  the material name of the block the drill occupies
 * @param kind  petroleum or ore
 * @param spec  the level's area, depth and cycle
 */
public record DrillDefinition(String id, String name, String host, DrillKind kind, DrillSpec spec) {

    /** The drill as a machine: a barrel-style host with one fuel slot and {@link DrillTable#OUTPUT_SLOTS} outputs. */
    public Machine machine() {
        return new Machine(id, name, host, 0, DrillTable.OUTPUT_SLOTS, 1);
    }

    /** The one item the fuel slot takes. */
    public ItemRef fuel() {
        return kind.fuel();
    }
}
