package io.corefactory.paper.gui.catalog;

/**
 * The families of TNT mechanics, as docs/MECHANICS.md groups them. Declared in the order the catalogue lists them.
 *
 * <p>The plain {@code standard} blast is in {@link #VISUAL}: in tnts.yml it only ever carries an effect modifier
 * (shockwave, debris, echo or silent), so it is the visual and audio group.
 */
public enum MechanicFamily {

    STRUCTURAL("Structural and physics"),
    TERRAIN("Terrain morphing"),
    ENTITY("Entity and status"),
    TIMING("Timing and logic"),
    VISUAL("Visual and audio"),
    UTILITY("Utility and economy"),
    MILITARY("Military"),
    NUCLEAR("Nuclear"),
    UNCLASSIFIED("Unclassified");

    private final String title;

    MechanicFamily(String title) {
        this.title = title;
    }

    /** The family's name as shown to players. */
    public String title() {
        return title;
    }

    /** The family of a mechanic id. A mechanic that no family lists is {@link #UNCLASSIFIED}. */
    public static MechanicFamily of(String mechanicId) {
        return switch (mechanicId) {
            case "chain_reaction", "shaped_charge", "gravity_well", "implosion", "fuse_linked", "delayed_cascade",
                 "four_dimensional" -> STRUCTURAL;
            case "vitrify", "liquefy", "terraform", "randomize", "sculpt" -> TERRAIN;
            case "concussion", "launch", "teleport", "gravity_flip" -> ENTITY;
            case "conditional", "proximity_tnt", "pulse_tnt", "mirror_tnt" -> TIMING;
            case "standard" -> VISUAL;
            case "harvest", "sorting", "marker", "anchor" -> UTILITY;
            case "biological", "napalm" -> MILITARY;
            case "nuclear", "neutron" -> NUCLEAR;
            default -> UNCLASSIFIED;
        };
    }
}
