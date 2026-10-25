package io.corefactory.paper.gui.catalog;

/**
 * One item of the catalogue: the kind of item it is and its id. The kind decides which item the id builds.
 *
 * @param kind the kind of item
 * @param id   the id: a TNT type, a component, a machine, an ore or a research pack
 */
public record CatalogEntry(Kind kind, String id) {

    /** The kinds of catalogue item. */
    public enum Kind {
        /** A TNT type from tnts.yml. */
        TNT,
        /** A crafting part or science material from items.yml or materials.yml. */
        COMPONENT,
        /** A placeable machine from processes.yml, the research console included. */
        MACHINE,
        /** The placeable block of an ore from ores.yml. */
        ORE,
        /** A research pack from materials.yml, fed to the research console. */
        RESEARCH_PACK
    }
}
