package io.corefactory.spigot.machine;

import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The recipe set of each process machine, and the lab effect each one shows when a pass completes. A machine id maps to its
 * recipes through {@link #recipes}, which reads processes.yml: a recipe added there runs in its machine with no Java change.
 * The power a machine asks for is its {@code power} key in processes.yml. A machine id with no row in the effect table
 * shows no effect.
 */
public final class ProcessFamily {

    /**
     * The visual a machine shows when a pass completes: the effect's name in {@code VisualCatalog}, and its size in blocks.
     *
     * @param effect the lab effect's name
     * @param scale  the effect's size in blocks
     */
    public record LabVisual(String effect, double scale) {
    }

    /** The lab effect of each process machine, keyed by the machine's id in processes.yml. */
    private static final Map<String, LabVisual> VISUALS = Map.ofEntries(
            Map.entry("crusher", new LabVisual("lab_crusher", 2.5)),
            Map.entry("roaster", new LabVisual("lab_roaster", 2.5)),
            Map.entry("separator", new LabVisual("lab_separator", 2.5)),
            Map.entry("purifier", new LabVisual("lab_purifier", 2.5)),
            Map.entry("converter", new LabVisual("lab_converter", 2.5)),
            Map.entry("enrichment_cascade", new LabVisual("lab_enrichment", 3.0)),
            Map.entry("transmuter", new LabVisual("lab_transmuter", 2.5)),
            Map.entry("spectrometer", new LabVisual("lab_spectrometer", 2.5)),
            Map.entry("particle_accelerator", new LabVisual("lab_accelerator", 6.0)),
            Map.entry("antimatter_condenser", new LabVisual("lab_condenser", 2.5)),
            Map.entry("crude_oil_refinery", new LabVisual("lab_purifier", 2.5)),
            Map.entry("induction_furnace", new LabVisual("lab_roaster", 2.5)),
            Map.entry("powder_mill", new LabVisual("lab_crusher", 2.5)),
            Map.entry("cartridge_press", new LabVisual("lab_crusher", 3.0)),
            Map.entry("bullet_swaging_machine", new LabVisual("lab_transmuter", 2.5)),
            Map.entry("ammunition_assembly", new LabVisual("lab_spectrometer", 2.5)),
            Map.entry("metal_lathe", new LabVisual("lab_separator", 2.5)),
            Map.entry("component_assembly", new LabVisual("lab_converter", 2.5)),
            Map.entry("heavy_weapons_assembly", new LabVisual("lab_enrichment", 3.0)));

    private ProcessFamily() {
    }

    /**
     * The recipe set of a machine: every process processes.yml names for it, in file order. Empty for a machine with no recipes,
     * such as the research console.
     *
     * @param catalog   the machines and processes read from processes.yml
     * @param machineId the machine id
     */
    public static List<ProcessDefinition> recipes(ProcessCatalog catalog, String machineId) {
        return catalog.processesFor(machineId);
    }

    /** The lab visual of a machine id, or empty when the machine has none. */
    public static Optional<LabVisual> labVisual(String machineId) {
        return Optional.ofNullable(VISUALS.get(machineId));
    }
}
