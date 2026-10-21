package io.corefactory.spigot.gui.catalog;

import io.corefactory.api.TntDefinition;
import io.corefactory.spigot.gui.catalog.CatalogEntry.Kind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Splits everything the plugin defines into the catalogue's categories. Every TNT type, component, machine, ore block
 * and research pack lands in exactly one category. The model reads ids and mechanics only, so it runs without a server.
 *
 * <p>A component is placed by the first rule that fits: radiation gear, research pack, the ore density scanner, drill,
 * electricity, generator, belt, picker, splitter, crafting part (defined by items.yml), and otherwise a science material
 * (defined by materials.yml). Machines are always lab machines.
 */
public final class CatalogModel {

    static final String NUKE_PREFIX = "nuke_";
    static final String EXOTIC_PREFIX = "exotic_";
    static final String FOUR_DIMENSIONAL = "four_dimensional_tnt";
    static final String RESEARCH_PACK_PREFIX = "research_pack_";
    static final String RESEARCH_CONSOLE = "research_console";
    static final String ORE_DENSITY_SCANNER = "ore_density_scanner";
    static final String ENERGY_WIRE = "energy_wire";

    /** The exotic series in order: the two E tiers, then the 4-dimensional bomb, the strongest of them all. */
    static final List<String> SERIES_ORDER = List.of("exotic_neutron_bomb", "exotic_antimatter_bomb", FOUR_DIMENSIONAL);

    /** The four pieces of the Radioactive Suit and the Geiger counter. */
    static final List<String> RADIATION_GEAR = List.of("radioactive_helmet", "radioactive_chestplate",
            "radioactive_leggings", "radioactive_boots", "geiger_counter");

    /** The power sources: the wind, solar, charcoal, petroleum, diesel and nuclear generators, and their fuel. */
    static final List<String> GENERATORS = List.of("solar_panel", "wind_generator", "charcoal_generator",
            "petroleum_generator", "diesel_generator", "nuclear_generator", "diesel", "nuclear_fuel_rod");

    static final List<String> DRILL_PREFIXES = List.of("petroleum_drill_", "ore_drill_");
    static final List<String> ELECTRICITY_PREFIXES = List.of("pylon_", "accumulator_");
    static final String BELT_PREFIX = "belt_";
    static final String PICKER_PREFIX = "picker_";
    static final String SPLITTER_PREFIX = "splitter_";

    /** The ammunition: the empty shell, the gunpowder, and the bullets and rounds of every kind. */
    static final List<String> AMMUNITION_IDS = List.of("empty_shell", "high_grade_gunpowder");
    static final List<String> AMMUNITION_PREFIXES = List.of("bullet_", "round_");

    /** The turrets and the precision parts they are built from: the machine gun, the laser turret, and the laser's emitter and lens. */
    static final List<String> DEFENCE_IDS = List.of("machine_gun_turret", "precision_barrel", "steel_frame",
            "turret_mechanism", "ammunition_feed", "targeting_unit", "laser_turret", "laser_emitter", "optical_lens",
            "turret_upgrade_2", "turret_upgrade_3", "turret_upgrade_4", "turret_upgrade_5");

    /**
     * The categories in the order the tabs show them, each with the id of the entry its tab icon is drawn from.
     * The icon is an entry of the same category.
     */
    enum Shelf {
        NUKES("Nuclear warheads", "nuke_little_boy"),
        EXOTIC("Exotic matter series", "exotic_antimatter_bomb"),
        OTHER_TNT("Other TNT", "chain_reaction_tnt"),
        CRAFTING("Crafting parts", "nuclear_casing"),
        RADIATION("Radiation gear", "geiger_counter"),
        SCIENCE("Science materials", "yellowcake"),
        RESEARCH_PACKS("Research packs", "research_pack_basic"),
        LAB("Lab machines", "separator"),
        DRILLS("Drills", "ore_drill_1"),
        ELECTRICITY("Electricity", "pylon_1"),
        GENERATORS("Generators", "wind_generator"),
        BELTS("Belts", "belt_1"),
        PICKERS("Pickers", "picker_1"),
        SPLITTERS("Splitters", "splitter_1"),
        AMMUNITION("Ammunition", "round_regular"),
        DEFENCES("Defences", "machine_gun_turret"),
        ORES("Ores", "uraninite_ore");

        private final String title;
        private final String iconId;

        Shelf(String title, String iconId) {
            this.title = title;
            this.iconId = iconId;
        }
    }

    private CatalogModel() {
    }

    /**
     * Builds the categories, in the order the tabs show them.
     *
     * @param tnts              the TNT types, in file order (the nuclear warheads keep this order, which is tier order)
     * @param componentIds      every component id, in file order
     * @param machineIds        the machines of the process catalog
     * @param oreIds            the ore ids, in file order
     * @param itemComponentIds  the component ids that items.yml defines; the rest come from materials.yml
     */
    public static List<CatalogCategory> build(Collection<TntDefinition> tnts, Collection<String> componentIds,
                                              Collection<String> machineIds, Collection<String> oreIds,
                                              Set<String> itemComponentIds) {
        List<TntDefinition> nukes = new ArrayList<>();
        List<TntDefinition> exotic = new ArrayList<>();
        List<TntDefinition> other = new ArrayList<>();
        for (TntDefinition tnt : tnts) {
            if (tnt.id().startsWith(NUKE_PREFIX)) {
                nukes.add(tnt);
            } else if (tnt.id().startsWith(EXOTIC_PREFIX) || tnt.id().equals(FOUR_DIMENSIONAL)) {
                exotic.add(tnt);
            } else {
                other.add(tnt);
            }
        }
        exotic.sort(Comparator.comparingInt((TntDefinition tnt) -> seriesRank(tnt.id())));
        other.sort(Comparator.comparing((TntDefinition tnt) -> MechanicFamily.of(tnt.mechanic())));

        Map<Shelf, List<CatalogEntry>> shelves = new EnumMap<>(Shelf.class);
        Set<String> machines = new LinkedHashSet<>(machineIds);
        machines.add(RESEARCH_CONSOLE);
        for (String id : machines) {
            add(shelves, Shelf.LAB, new CatalogEntry(Kind.MACHINE, id));
        }
        for (String id : componentIds) {
            if (machines.contains(id)) {
                continue;
            }
            Kind kind = id.startsWith(RESEARCH_PACK_PREFIX) ? Kind.RESEARCH_PACK : Kind.COMPONENT;
            add(shelves, componentShelf(id, itemComponentIds), new CatalogEntry(kind, id));
        }
        for (String id : oreIds) {
            add(shelves, Shelf.ORES, new CatalogEntry(Kind.ORE, id));
        }

        for (TntDefinition tnt : nukes) {
            add(shelves, Shelf.NUKES, new CatalogEntry(Kind.TNT, tnt.id()));
        }
        for (TntDefinition tnt : exotic) {
            add(shelves, Shelf.EXOTIC, new CatalogEntry(Kind.TNT, tnt.id()));
        }
        for (TntDefinition tnt : other) {
            add(shelves, Shelf.OTHER_TNT, new CatalogEntry(Kind.TNT, tnt.id()));
        }

        List<CatalogCategory> categories = new ArrayList<>();
        for (Shelf shelf : Shelf.values()) {
            categories.add(new CatalogCategory(shelf.title, shelf.iconId,
                    shelves.getOrDefault(shelf, List.of())));
        }
        return List.copyOf(categories);
    }

    /** The shelf a component belongs to. Pure: it reads the id and the items.yml ids only. */
    static Shelf componentShelf(String id, Set<String> itemComponentIds) {
        if (RADIATION_GEAR.contains(id)) {
            return Shelf.RADIATION;
        }
        if (id.startsWith(RESEARCH_PACK_PREFIX)) {
            return Shelf.RESEARCH_PACKS;
        }
        if (id.equals(ORE_DENSITY_SCANNER)) {
            return Shelf.LAB;
        }
        if (startsWithAny(id, DRILL_PREFIXES)) {
            return Shelf.DRILLS;
        }
        if (id.equals(ENERGY_WIRE) || startsWithAny(id, ELECTRICITY_PREFIXES)) {
            return Shelf.ELECTRICITY;
        }
        if (GENERATORS.contains(id)) {
            return Shelf.GENERATORS;
        }
        if (id.startsWith(BELT_PREFIX)) {
            return Shelf.BELTS;
        }
        if (id.startsWith(PICKER_PREFIX)) {
            return Shelf.PICKERS;
        }
        if (id.startsWith(SPLITTER_PREFIX)) {
            return Shelf.SPLITTERS;
        }
        if (AMMUNITION_IDS.contains(id) || startsWithAny(id, AMMUNITION_PREFIXES)) {
            return Shelf.AMMUNITION;
        }
        if (DEFENCE_IDS.contains(id)) {
            return Shelf.DEFENCES;
        }
        return itemComponentIds.contains(id) ? Shelf.CRAFTING : Shelf.SCIENCE;
    }

    private static boolean startsWithAny(String id, List<String> prefixes) {
        return prefixes.stream().anyMatch(id::startsWith);
    }

    private static void add(Map<Shelf, List<CatalogEntry>> shelves, Shelf shelf, CatalogEntry entry) {
        shelves.computeIfAbsent(shelf, key -> new ArrayList<>()).add(entry);
    }

    private static int seriesRank(String id) {
        int rank = SERIES_ORDER.indexOf(id);
        return rank < 0 ? SERIES_ORDER.size() : rank;
    }
}
