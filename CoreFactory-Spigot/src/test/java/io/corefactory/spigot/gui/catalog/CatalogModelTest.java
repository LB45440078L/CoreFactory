package io.corefactory.spigot.gui.catalog;

import io.corefactory.api.TntDefinition;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.process.Machine;
import io.corefactory.spigot.ShippedContent;
import io.corefactory.spigot.gui.catalog.CatalogEntry.Kind;
import io.corefactory.spigot.gui.catalog.CatalogModel.Shelf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Builds the catalogue from the shipped data files, the way the plugin loads them, and checks that every item the
 * plugin defines is in exactly one category. Nothing here starts a server.
 */
class CatalogModelTest {

    private static final List<String> RADIATION_GEAR = List.of("radioactive_helmet", "radioactive_chestplate",
            "radioactive_leggings", "radioactive_boots", "geiger_counter");
    private static final List<String> DRILLS = List.of("petroleum_drill_1", "petroleum_drill_2", "petroleum_drill_3",
            "petroleum_drill_4", "petroleum_drill_5", "ore_drill_1", "ore_drill_2", "ore_drill_3", "ore_drill_4",
            "ore_drill_5");
    private static final List<String> ELECTRICITY = List.of("energy_wire", "pylon_1", "pylon_2", "pylon_3",
            "accumulator_1", "accumulator_2", "accumulator_3");
    private static final List<String> GENERATORS = List.of("solar_panel", "wind_generator", "charcoal_generator",
            "petroleum_generator", "diesel_generator", "nuclear_generator", "diesel", "nuclear_fuel_rod");
    private static final List<String> BELTS = List.of("belt_1", "belt_2", "belt_3");
    private static final List<String> PICKERS = List.of("picker_1", "picker_2", "picker_3");
    private static final List<String> SPLITTERS = List.of("splitter_1", "splitter_2", "splitter_3");

    private static ShippedContent.Loaded content;
    private static Map<String, TntDefinition> tnts;
    private static List<CatalogCategory> categories;

    @BeforeAll
    static void build() {
        content = ShippedContent.load();
        tnts = content.definitions().tnts();
        categories = CatalogModel.build(
                tnts.values(),
                content.definitions().components().keySet(),
                content.processes().machines().stream().map(Machine::id).toList(),
                content.ores().stream().map(OreDefinition::id).toList(),
                ShippedContent.itemComponentIds());
    }

    @Test
    void theTabsAreInTheOrderTheyAreShown() {
        assertThat(categories).extracting(CatalogCategory::title).containsExactly(
                "Nuclear warheads", "Exotic matter series", "Other TNT", "Crafting parts", "Radiation gear",
                "Science materials", "Research packs", "Lab machines", "Drills", "Electricity", "Generators",
                "Belts", "Pickers", "Splitters", "Ammunition", "Defences", "Ores");
    }

    @Test
    void everyDefinedItemAppearsExactlyOnce() {
        List<CatalogEntry> all = categories.stream().flatMap(category -> category.entries().stream()).toList();

        assertThat(all).doesNotHaveDuplicates();
        assertThat(all.stream().map(CatalogEntry::id).collect(Collectors.toSet())).as("ids across kinds")
                .hasSize(all.size());
        assertThat(idsOf(all, Kind.TNT)).as("TNT types")
                .containsExactlyInAnyOrderElementsOf(tnts.keySet());
        assertThat(idsOf(all, Kind.COMPONENT, Kind.MACHINE, Kind.RESEARCH_PACK)).as("components")
                .containsExactlyInAnyOrderElementsOf(content.definitions().components().keySet());
        assertThat(idsOf(all, Kind.ORE)).as("ore blocks")
                .containsExactlyInAnyOrderElementsOf(oreIds());
    }

    @Test
    void noCategoryIsEmpty() {
        assertThat(categories).allSatisfy(category -> assertThat(category.entries()).as(category.title()).isNotEmpty());
    }

    @Test
    void eachIconIsOneOfItsCategorysEntries() {
        assertThat(categories).allSatisfy(category -> assertThat(category.icon()).as(category.title()).isPresent());
    }

    @Test
    void eachCategoryHoldsItsKindOfItem() {
        assertThat(kindsOf("Nuclear warheads")).containsOnly(Kind.TNT);
        assertThat(kindsOf("Exotic matter series")).containsOnly(Kind.TNT);
        assertThat(kindsOf("Other TNT")).containsOnly(Kind.TNT);
        assertThat(kindsOf("Crafting parts")).containsOnly(Kind.COMPONENT);
        assertThat(kindsOf("Radiation gear")).containsOnly(Kind.COMPONENT);
        assertThat(kindsOf("Science materials")).containsOnly(Kind.COMPONENT);
        assertThat(kindsOf("Research packs")).containsOnly(Kind.RESEARCH_PACK);
        assertThat(kindsOf("Lab machines")).containsOnly(Kind.MACHINE, Kind.COMPONENT);
        assertThat(kindsOf("Drills")).containsOnly(Kind.COMPONENT);
        assertThat(kindsOf("Electricity")).containsOnly(Kind.COMPONENT);
        assertThat(kindsOf("Generators")).containsOnly(Kind.COMPONENT);
        assertThat(kindsOf("Belts")).containsOnly(Kind.COMPONENT);
        assertThat(kindsOf("Pickers")).containsOnly(Kind.COMPONENT);
        assertThat(kindsOf("Splitters")).containsOnly(Kind.COMPONENT);
        assertThat(kindsOf("Ores")).containsOnly(Kind.ORE);
    }

    @Test
    void theIconsAreDrawnFromTheirOwnCategory() {
        assertThat(categories).extracting(category -> category.icon().map(CatalogEntry::id).orElse(""))
                .containsExactly("nuke_little_boy", "exotic_antimatter_bomb", "chain_reaction_tnt", "nuclear_casing",
                        "geiger_counter", "yellowcake", "research_pack_basic", "separator", "ore_drill_1", "pylon_1",
                        "wind_generator", "belt_1", "picker_1", "splitter_1", "round_regular", "machine_gun_turret",
                        "uraninite_ore");
    }

    @Test
    void theNuclearWarheadsAreTheTenNukesInTierOrder() {
        assertThat(idsIn("Nuclear warheads")).containsExactly(
                "nuke_davy_crockett", "nuke_little_boy", "nuke_fat_man", "nuke_joe_1", "nuke_ivy_king",
                "nuke_b83", "nuke_ivy_mike", "nuke_castle_bravo", "nuke_mk17", "nuke_tsar_bomba");
    }

    @Test
    void theExoticSeriesHoldsBothExoticBombsAndTheFourDimensionalBomb() {
        assertThat(idsIn("Exotic matter series")).containsExactly(
                "exotic_neutron_bomb", "exotic_antimatter_bomb", "four_dimensional_tnt");
    }

    @Test
    void theAntimatterAndFourDimensionalBombsAreLoadedDefinitions() {
        assertThat(tnts).containsKeys("exotic_antimatter_bomb", "four_dimensional_tnt");
    }

    @Test
    void otherTntIsTheRestOfTheTypesGroupedByMechanicFamily() {
        List<String> other = idsIn("Other TNT");

        assertThat(other).as("TNT types outside the nuclear and exotic series")
                .hasSize(tnts.size() - 13)
                .noneMatch(id -> id.startsWith("nuke_") || id.startsWith("exotic_")
                        || id.equals("four_dimensional_tnt"));
        List<MechanicFamily> families = other.stream()
                .map(id -> MechanicFamily.of(tnts.get(id).mechanic()))
                .toList();
        assertThat(families).isSorted();
        assertThat(families).doesNotContain(MechanicFamily.UNCLASSIFIED);
    }

    @Test
    void everyMechanicOfAShippedTntHasAFamily() {
        for (TntDefinition tnt : tnts.values()) {
            assertThat(MechanicFamily.of(tnt.mechanic())).as("family of " + tnt.id())
                    .isNotEqualTo(MechanicFamily.UNCLASSIFIED);
        }
    }

    @Test
    void craftingPartsAreTheItemsFileComponentsWithoutTheRadiationGear() {
        Set<String> expected = new HashSet<>(ShippedContent.itemComponentIds());
        expected.removeAll(RADIATION_GEAR);

        assertThat(idsIn("Crafting parts")).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void radiationGearIsItsOwnCategory() {
        assertThat(idsIn("Radiation gear")).containsExactlyInAnyOrderElementsOf(RADIATION_GEAR);
    }

    @Test
    void scienceMaterialsAreWhatIsLeftOfTheMaterialsFile() {
        Set<String> expected = new HashSet<>(content.definitions().components().keySet());
        expected.removeAll(ShippedContent.itemComponentIds());
        expected.removeAll(machineIds());
        expected.removeIf(id -> id.startsWith("research_pack_"));
        expected.removeAll(DRILLS);
        expected.removeAll(ELECTRICITY);
        expected.removeAll(GENERATORS);
        expected.removeAll(BELTS);
        expected.removeAll(PICKERS);
        expected.removeAll(SPLITTERS);
        expected.removeAll(CatalogModel.AMMUNITION_IDS);
        expected.removeIf(id -> id.startsWith("bullet_") || id.startsWith("round_"));
        expected.removeAll(CatalogModel.DEFENCE_IDS);
        expected.remove("ore_density_scanner");

        assertThat(idsIn("Science materials")).containsExactlyInAnyOrderElementsOf(expected);
        assertThat(idsIn("Science materials")).contains("raw_uraninite", "crushed_galena", "yellowcake",
                "petroleum", "deuterium_cell", "purified_iron", "antimatter_condensate");
    }

    @Test
    void theMachinesAreEveryProcessMachineAndTheResearchConsoleAndTheScanner() {
        assertThat(idsIn("Lab machines")).containsExactlyElementsOf(
                List.copyOf(machineIdsWithScanner()));
    }

    @Test
    void theResearchPacksAreTheResearchPackComponents() {
        Set<String> expected = content.definitions().components().keySet().stream()
                .filter(id -> id.startsWith("research_pack_"))
                .collect(Collectors.toSet());

        assertThat(idsIn("Research packs")).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void theDrillsArePetroleumAndOreDrillsOneToFive() {
        assertThat(idsIn("Drills")).containsExactlyElementsOf(DRILLS);
    }

    @Test
    void theElectricityIsTheWireThePylonsAndTheAccumulators() {
        assertThat(idsIn("Electricity")).containsExactlyElementsOf(ELECTRICITY);
    }

    @Test
    void theGeneratorsAreTheSixGeneratorsAndTheTwoFuels() {
        assertThat(idsIn("Generators")).containsExactlyElementsOf(GENERATORS);
    }

    @Test
    void theBeltsPickersAndSplittersAreTheirThreeTiers() {
        assertThat(idsIn("Belts")).containsExactlyElementsOf(BELTS);
        assertThat(idsIn("Pickers")).containsExactlyElementsOf(PICKERS);
        assertThat(idsIn("Splitters")).containsExactlyElementsOf(SPLITTERS);
    }

    @Test
    void aNewTierOfADeviceJoinsItsFamilysCategory() {
        assertThat(CatalogModel.componentShelf("pylon_4", Set.of())).isEqualTo(Shelf.ELECTRICITY);
        assertThat(CatalogModel.componentShelf("belt_4", Set.of())).isEqualTo(Shelf.BELTS);
        assertThat(CatalogModel.componentShelf("picker_4", Set.of())).isEqualTo(Shelf.PICKERS);
        assertThat(CatalogModel.componentShelf("splitter_4", Set.of())).isEqualTo(Shelf.SPLITTERS);
        assertThat(CatalogModel.componentShelf("ore_drill_6", Set.of())).isEqualTo(Shelf.DRILLS);
        assertThat(CatalogModel.componentShelf("research_pack_future", Set.of()))
                .isEqualTo(Shelf.RESEARCH_PACKS);
    }

    @Test
    void aComponentOfItemsFileIsCraftingUnlessItIsGearOrADevice() {
        assertThat(CatalogModel.componentShelf("fuse_linker", Set.of("fuse_linker"))).isEqualTo(Shelf.CRAFTING);
        assertThat(CatalogModel.componentShelf("fuse_linker", Set.of())).isEqualTo(Shelf.SCIENCE);
        assertThat(CatalogModel.componentShelf("geiger_counter", Set.of("geiger_counter")))
                .isEqualTo(Shelf.RADIATION);
    }

    @Test
    void theLaserTurretAndItsPartsAreDefencesEvenWhenItemsFileDefinesThem() {
        assertThat(CatalogModel.componentShelf("laser_turret", Set.of("laser_turret"))).isEqualTo(Shelf.DEFENCES);
        assertThat(CatalogModel.componentShelf("laser_emitter", Set.of())).isEqualTo(Shelf.DEFENCES);
        assertThat(CatalogModel.componentShelf("optical_lens", Set.of())).isEqualTo(Shelf.DEFENCES);
        assertThat(CatalogModel.DEFENCE_IDS).contains("machine_gun_turret", "laser_turret", "laser_emitter", "optical_lens");
    }

    @Test
    void theOresAreOneBlockPerOreInFileOrder() {
        assertThat(idsIn("Ores")).containsExactlyElementsOf(oreIds());
    }

    @Test
    void theFamilyOfAMechanicFollowsTheDocumentedGroups() {
        assertThat(MechanicFamily.of("standard")).isEqualTo(MechanicFamily.VISUAL);
        assertThat(MechanicFamily.of("four_dimensional")).isEqualTo(MechanicFamily.STRUCTURAL);
        assertThat(MechanicFamily.of("neutron")).isEqualTo(MechanicFamily.NUCLEAR);
        assertThat(MechanicFamily.of("no_such_mechanic")).isEqualTo(MechanicFamily.UNCLASSIFIED);
    }

    private static CatalogCategory category(String title) {
        return categories.stream()
                .filter(category -> category.title().equals(title))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No category " + title));
    }

    private static List<String> idsIn(String title) {
        return category(title).entries().stream().map(CatalogEntry::id).toList();
    }

    private static Set<Kind> kindsOf(String title) {
        return category(title).entries().stream().map(CatalogEntry::kind).collect(Collectors.toSet());
    }

    private static Set<String> idsOf(List<CatalogEntry> entries, Kind... kinds) {
        Set<Kind> wanted = Set.of(kinds);
        return entries.stream()
                .filter(entry -> wanted.contains(entry.kind()))
                .map(CatalogEntry::id)
                .collect(Collectors.toSet());
    }

    private static List<String> oreIds() {
        return content.ores().stream().map(OreDefinition::id).toList();
    }

    private static Set<String> machineIds() {
        Set<String> ids = new LinkedHashSet<>();
        content.processes().machines().forEach(machine -> ids.add(machine.id()));
        ids.add("research_console");
        return ids;
    }

    private static Set<String> machineIdsWithScanner() {
        Set<String> ids = machineIds();
        ids.add("ore_density_scanner");
        return ids;
    }
}
