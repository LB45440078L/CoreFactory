package io.corefactory.core.drill;

import io.corefactory.core.process.Machine;

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

    /**
     * The drill as a machine: a barrel-style host with no inputs and no fuel. An ore drill has {@link DrillTable#OUTPUT_SLOTS} outputs
     * for the ore it mines. A petroleum drill has no outputs, since it pumps its petroleum into the oil network.
     */
    public Machine machine() {
        int outputs = kind == DrillKind.ORE ? DrillTable.OUTPUT_SLOTS : 0;
        return new Machine(id, name, host, 0, outputs, 0);
    }
}
