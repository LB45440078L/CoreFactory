package io.corefactory.paper.audio;

import io.corefactory.core.drill.DrillKind;
import io.corefactory.core.energy.GeneratorKind;

import java.util.Map;
import java.util.Optional;

/**
 * The pack sound key of each device: a generator kind, a drill kind, or a process machine id. Each key names a sound in the
 * resource pack, see {@code SoundPack}. A process machine with no row in the table plays the generic lab sound. Pure, so the
 * table can be checked without a server.
 */
public final class SoundKeys {

    /** The generic sound of a process machine that has none of its own. */
    public static final String LAB = "lab_run";

    /** The sound of a cartridge press that finishes a cartridge. */
    static final String PRESS_STAMP = "press_stamp";

    /** The sound of a turret when it fires a bullet. */
    static final String TURRET_FIRE = "turret_fire";

    /** The sound of a turret while it reloads its magazine. */
    static final String TURRET_RELOAD = "turret_reload";

    /** The sound of a turret while it turns to face a target: a short servo whine. */
    static final String TURRET_ROTATE = "turret_rotate";

    /** The sound of a laser turret when it fires: a sci-fi laser shot. */
    static final String LASER_FIRE = "laser_fire";

    /** The sound of a laser turret while it charges before a shot: a short rising whine. */
    static final String LASER_CHARGE = "laser_charge";

    /** The sound of each process machine that has its own, keyed by the machine id in processes.yml. */
    static final Map<String, String> PROCESSES = Map.ofEntries(
            Map.entry("separator", "separator"),
            Map.entry("roaster", "roaster"),
            Map.entry("purifier", "purifier"),
            Map.entry("converter", "converter"),
            Map.entry("enrichment_cascade", "enrichment_cascade"),
            Map.entry("transmuter", "transmuter"),
            Map.entry("spectrometer", "spectrometer"),
            Map.entry("particle_accelerator", "particle_accelerator"),
            Map.entry("antimatter_condenser", "antimatter_condenser"),
            Map.entry("research_console", "research_console"),
            Map.entry("crude_oil_refinery", "crude_oil_refinery"),
            Map.entry("induction_furnace", "induction_hiss"),
            Map.entry("powder_mill", "mill_grind"),
            Map.entry("cartridge_press", "cartridge_press"),
            Map.entry("bullet_swaging_machine", "bullet_swaging_machine"),
            Map.entry("ammunition_assembly", "ammunition_assembly"),
            Map.entry("metal_lathe", "metal_lathe"),
            Map.entry("component_assembly", "component_assembly"),
            Map.entry("heavy_weapons_assembly", "heavy_weapons_assembly"));

    private SoundKeys() {
    }

    /**
     * The sound of a generator while it gives power: each kind has its own.
     *
     * @param kind the generator kind
     */
    public static String generator(GeneratorKind kind) {
        return switch (kind) {
            case WIND -> "wind_generator";
            case SOLAR -> "solar_panel";
            case CHARCOAL -> "charcoal_generator";
            case PETROLEUM -> "petroleum_generator";
            case DIESEL -> "diesel_generator";
            case NUCLEAR -> "nuclear_generator";
        };
    }

    /**
     * The sound of a drill while it runs: the ore drill and the petroleum drill each have their own.
     *
     * @param kind the drill kind
     */
    public static String drill(DrillKind kind) {
        return switch (kind) {
            case PETROLEUM -> "petroleum_drill";
            case ORE -> "ore_drill";
        };
    }

    /**
     * The sound of a process machine while it works, or the generic lab sound for a machine with none of its own.
     *
     * @param machineId the machine id in processes.yml
     */
    public static String process(String machineId) {
        return machineId == null ? LAB : PROCESSES.getOrDefault(machineId, LAB);
    }

    /**
     * The sound of a process machine when a pass completes. Only the cartridge press has one.
     *
     * @param machineId the machine id in processes.yml
     */
    public static Optional<String> completion(String machineId) {
        return "cartridge_press".equals(machineId) ? Optional.of(PRESS_STAMP) : Optional.empty();
    }

    /** The sound of a turret when it fires a bullet. */
    public static String turretFire() {
        return TURRET_FIRE;
    }

    /** The sound of a turret while it reloads. */
    public static String turretReload() {
        return TURRET_RELOAD;
    }

    /** The sound of a turret while it turns to face a target. */
    public static String turretRotate() {
        return TURRET_ROTATE;
    }

    /** The sound of a laser turret when it fires. */
    public static String laserFire() {
        return LASER_FIRE;
    }

    /** The sound of a laser turret while it charges before a shot. */
    public static String laserCharge() {
        return LASER_CHARGE;
    }
}
