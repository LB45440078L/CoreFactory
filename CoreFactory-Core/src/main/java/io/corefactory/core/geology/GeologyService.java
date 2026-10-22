package io.corefactory.core.geology;

import java.util.List;

/**
 * The ores of the server. Holds the parsed ore list; its order gives the ore indices the block registry stores.
 */
public final class GeologyService {

    private final List<OreDefinition> ores;

    /**
     * @param ores the ores in file order; at most 256, since a registry entry stores the index in 8 bits
     * @throws IllegalArgumentException when there are more than 256 ores
     */
    public GeologyService(List<OreDefinition> ores) {
        if (ores.size() > 256) {
            throw new IllegalArgumentException("at most 256 ores are supported, got " + ores.size());
        }
        this.ores = List.copyOf(ores);
    }

    /** The ores, in index order. */
    public List<OreDefinition> ores() {
        return ores;
    }

    /** The ore at an index. */
    public OreDefinition ore(int index) {
        return ores.get(index);
    }

    /** The index of the ore with this id, or -1 when no ore has it. */
    public int indexOf(String id) {
        for (int index = 0; index < ores.size(); index++) {
            if (ores.get(index).id().equals(id)) {
                return index;
            }
        }
        return -1;
    }
}
