package io.corefactory.core.turret;

import java.util.List;

/**
 * The entity types a turret's menu lists for its own targeting, each with the kind it counts as. The four kinds are listed on their own
 * as categories. An entity type that is not listed follows its kind. Names are the Bukkit entity type names; a name that the running server
 * does not know is simply never matched.
 */
public final class TargetCatalog {

    /**
     * One entity type of the menu.
     *
     * @param entity the Bukkit entity type's name, such as {@code ZOMBIE}
     * @param kind   the kind the entity counts as
     */
    public record Entry(String entity, TargetKind kind) {
    }

    /** The kinds the menu lists as categories, in display order. */
    public static final List<TargetKind> CATEGORIES = List.of(TargetKind.MONSTER, TargetKind.PHANTOM, TargetKind.SLIME, TargetKind.GHAST);

    /** The entity types the menu lists, in display order. Eighteen fit the menu beside its controls. */
    public static final List<Entry> ENTITIES = List.of(
            new Entry("ZOMBIE", TargetKind.MONSTER),
            new Entry("HUSK", TargetKind.MONSTER),
            new Entry("DROWNED", TargetKind.MONSTER),
            new Entry("SKELETON", TargetKind.MONSTER),
            new Entry("STRAY", TargetKind.MONSTER),
            new Entry("WITHER_SKELETON", TargetKind.MONSTER),
            new Entry("CREEPER", TargetKind.MONSTER),
            new Entry("SPIDER", TargetKind.MONSTER),
            new Entry("ENDERMAN", TargetKind.MONSTER),
            new Entry("WITCH", TargetKind.MONSTER),
            new Entry("BLAZE", TargetKind.MONSTER),
            new Entry("PILLAGER", TargetKind.MONSTER),
            new Entry("VINDICATOR", TargetKind.MONSTER),
            new Entry("RAVAGER", TargetKind.MONSTER),
            new Entry("PHANTOM", TargetKind.PHANTOM),
            new Entry("SLIME", TargetKind.SLIME),
            new Entry("MAGMA_CUBE", TargetKind.SLIME),
            new Entry("GHAST", TargetKind.GHAST));

    private TargetCatalog() {
    }
}
