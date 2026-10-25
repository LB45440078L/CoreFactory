package io.corefactory.paper.machine;

import io.corefactory.paper.config.YamlMaps;
import io.corefactory.core.drill.DrillCatalog;
import io.corefactory.core.drill.DrillDefinition;
import io.corefactory.core.drill.DrillKind;
import io.corefactory.core.drill.DrillTable;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Machine;
import io.corefactory.paper.ShippedContent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/** The drills of the shipped processes.yml and materials.yml, read as the plugin reads them. Nothing here starts a server. */
class DrillCatalogTest {

    private static final Logger LOG = Logger.getLogger("DrillCatalogTest");

    private static DrillCatalog catalog;

    @BeforeAll
    static void load() throws Exception {
        YamlConfiguration processes;
        try (Reader reader = new InputStreamReader(
                DrillCatalogTest.class.getClassLoader().getResourceAsStream("processes.yml"), StandardCharsets.UTF_8)) {
            processes = YamlConfiguration.loadConfiguration(reader);
        }
        catalog = DrillCatalog.parse(YamlMaps.toMap(processes), ShippedContent.load().definitions().components(), LOG);
    }

    @Test
    void theShippedFileDefinesTenDrills() {
        Set<String> ids = Set.of("petroleum_drill_1", "petroleum_drill_2", "petroleum_drill_3", "petroleum_drill_4",
                "petroleum_drill_5", "ore_drill_1", "ore_drill_2", "ore_drill_3", "ore_drill_4", "ore_drill_5");
        assertThat(catalog.drills()).hasSize(10);
        assertThat(catalog.drills()).extracting(DrillDefinition::id).containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test
    void eachDrillHasTheKindAndLevelItsIdNames() {
        for (int level = 1; level <= DrillTable.MAX_LEVEL; level++) {
            DrillDefinition petroleum = catalog.drill("petroleum_drill_" + level).orElseThrow();
            assertThat(petroleum.kind()).isEqualTo(DrillKind.PETROLEUM);
            assertThat(petroleum.spec().level()).isEqualTo(level);
            DrillDefinition ore = catalog.drill("ore_drill_" + level).orElseThrow();
            assertThat(ore.kind()).isEqualTo(DrillKind.ORE);
            assertThat(ore.spec().level()).isEqualTo(level);
        }
    }

    @Test
    void everyDrillUsesTheSpecOfItsLevelFromTheTable() {
        for (DrillDefinition drill : catalog.drills()) {
            assertThat(drill.spec()).isEqualTo(DrillTable.level(drill.spec().level()));
        }
    }

    @Test
    void everyDrillStandsOnABarrelSoItsMenuHasTheSlotsItNeeds() {
        for (DrillDefinition drill : catalog.drills()) {
            assertThat(drill.host()).as(drill.id()).isEqualTo(MachineSlots.HOST);
        }
    }

    @Test
    void everyLabToolStandsOnABarrelToo() {
        for (Machine machine : ShippedContent.load().processes().machines()) {
            assertThat(machine.host()).as(machine.id()).isEqualTo(MachineSlots.HOST);
        }
    }

    @Test
    void theFuelOfAPetroleumDrillIsCharcoalAndOfAnOreDrillIsPetroleum() {
        assertThat(catalog.drill("petroleum_drill_3").orElseThrow().fuel()).contains(ItemRef.material("CHARCOAL"));
        assertThat(catalog.drill("ore_drill_3").orElseThrow().fuel()).isEmpty();
    }

    @Test
    void aPetroleumDrillIsAMachineWithOneCharcoalSlotAndAnOreDrillWithTheOutputGrid() {
        for (DrillDefinition drill : catalog.drills()) {
            Machine machine = catalog.machine(drill.id()).orElseThrow();
            assertThat(machine.inputSlots()).as(drill.id()).isZero();
            if (drill.kind() == DrillKind.PETROLEUM) {
                assertThat(machine.fuelSlots()).as(drill.id()).isEqualTo(1);
                assertThat(machine.outputSlots()).as(drill.id()).isZero();
            } else {
                assertThat(machine.fuelSlots()).as(drill.id()).isZero();
                assertThat(machine.outputSlots()).as(drill.id()).isEqualTo(DrillTable.OUTPUT_SLOTS);
            }
        }
    }

    @Test
    void theDrillsAreNotLabToolsInProcessesYml() {
        var processes = ShippedContent.load().processes();
        for (DrillDefinition drill : catalog.drills()) {
            assertThat(processes.machine(drill.id())).as(drill.id()).isEmpty();
        }
    }

    @Test
    void aBadEntryIsLeftOutAndTheRestStillLoad() {
        String yaml = """
                drills:
                  petroleum_drill_1:
                    kind: petroleum
                    level: 1
                  petroleum_drill_9:
                    kind: petroleum
                    level: 9
                  ore_drill_2:
                    kind: gas
                    level: 2
                  ore_drill_3:
                    kind: ore
                    level: 3
                  not_a_drill:
                    kind: ore
                    level: 1
                """;
        YamlConfiguration root = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        DrillCatalog partial = DrillCatalog.parse(YamlMaps.toMap(root), ShippedContent.load().definitions().components(), LOG);
        assertThat(partial.drills()).extracting(DrillDefinition::id)
                .containsExactlyInAnyOrder("petroleum_drill_1", "ore_drill_3");
    }

    @Test
    void aMissingSectionMakesNoDrills() {
        assertThat(DrillCatalog.parse(null, ShippedContent.load().definitions().components(), LOG).drills()).isEmpty();
        assertThat(DrillCatalog.parse(YamlMaps.toMap(new YamlConfiguration()), ShippedContent.load().definitions().components(), LOG)
                .drills()).isEmpty();
    }

    @Test
    void theKindsParseFromTheirIds() {
        assertThat(DrillKind.parse("petroleum")).contains(DrillKind.PETROLEUM);
        assertThat(DrillKind.parse("ore")).contains(DrillKind.ORE);
        assertThat(DrillKind.parse("gas")).isEmpty();
    }
}
