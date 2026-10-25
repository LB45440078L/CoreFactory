package io.corefactory.core.drill;

import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.process.Machine;
import io.corefactory.core.config.ConfigValues;
import org.bukkit.Material;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * The drills of the {@code drills:} section of processes.yml. Each entry names a drill component, its kind and its level.
 * The component gives the display name and the host block. A bad entry is logged and left out.
 */
public final class DrillCatalog {

    private final Map<String, DrillDefinition> drills;

    private DrillCatalog(Map<String, DrillDefinition> drills) {
        this.drills = Map.copyOf(drills);
    }

    /**
     * Reads {@code drills:} from processes.yml.
     *
     * @param root       the root of processes.yml as plain data, or null for no drills
     * @param components the components by id, from materials.yml and items.yml
     * @param log        receives one warning per skipped entry
     */
    public static DrillCatalog parse(Map<String, Object> root, Map<String, ComponentDefinition> components, Logger log) {
        Map<String, DrillDefinition> drills = new LinkedHashMap<>();
        Map<String, Object> section = ConfigValues.optionalSection(root, "drills");
        if (section == null) {
            return new DrillCatalog(drills);
        }
        for (String id : section.keySet()) {
            DrillDefinition drill = read(id, ConfigValues.optionalSection(section, id), components, log);
            if (drill != null) {
                drills.put(id, drill);
            }
        }
        return new DrillCatalog(drills);
    }

    /** Every drill, in the order of the file. */
    public Collection<DrillDefinition> drills() {
        return drills.values();
    }

    /** The drill with this component id. */
    public Optional<DrillDefinition> drill(String id) {
        return Optional.ofNullable(drills.get(id));
    }

    /** The drill with this component id, as a machine. */
    public Optional<Machine> machine(String id) {
        return drill(id).map(DrillDefinition::machine);
    }

    private static DrillDefinition read(String id, Map<String, Object> entry,
                                        Map<String, ComponentDefinition> components, Logger log) {
        if (entry == null) {
            warn(log, "drill '" + id + "' is not a section and is skipped.");
            return null;
        }
        Optional<DrillKind> kind = DrillKind.parse(ConfigValues.string(entry, "kind", ""));
        if (kind.isEmpty()) {
            warn(log, "drill '" + id + "' has no known kind (petroleum or ore) and is skipped.");
            return null;
        }
        int level = ConfigValues.integer(entry, "level", 0);
        if (level < 1 || level > DrillTable.MAX_LEVEL) {
            warn(log, "drill '" + id + "' needs a level from 1 to " + DrillTable.MAX_LEVEL + " and is skipped.");
            return null;
        }
        ComponentDefinition component = components.get(id);
        if (component == null) {
            warn(log, "drill '" + id + "' has no component in materials.yml and is skipped.");
            return null;
        }
        Material host = component.material();
        if (host == null) {
            warn(log, "drill '" + id + "' has no material in materials.yml and is skipped.");
            return null;
        }
        return new DrillDefinition(id, component.displayName(), host.name(), kind.get(), DrillTable.level(level));
    }

    private static void warn(Logger log, String message) {
        if (log != null) {
            log.warning("processes.yml: " + message);
        }
    }
}
