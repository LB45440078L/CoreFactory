package io.corefactory.core.drill;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Which ores a drill mines. Each ore is either mined or ignored; every ore is mined by default. Only the ignored ids are
 * kept, and they are saved as a list. Mutable, since the drill's menu toggles one ore at a time.
 */
public final class DrillSelection {

    private final TreeSet<String> ignored;

    /** A selection that mines every ore. */
    public DrillSelection() {
        this(List.of());
    }

    /**
     * @param ignored the ore ids the drill ignores, as saved; blank and repeated ids are ignored
     */
    public DrillSelection(Collection<String> ignored) {
        this.ignored = new TreeSet<>();
        for (String id : ignored) {
            if (id != null && !id.isBlank()) {
                this.ignored.add(id);
            }
        }
    }

    /** True when the drill mines this ore. Every ore is mined unless it was ignored. */
    public boolean isMining(String oreId) {
        return !ignored.contains(oreId);
    }

    /**
     * Flips one ore between mined and ignored.
     *
     * @return true when the ore is now mined
     */
    public boolean toggle(String oreId) {
        if (!ignored.remove(oreId)) {
            ignored.add(oreId);
        }
        return isMining(oreId);
    }

    /** The ignored ore ids in alphabetical order, for saving. */
    public List<String> serialise() {
        return List.copyOf(ignored);
    }
}
