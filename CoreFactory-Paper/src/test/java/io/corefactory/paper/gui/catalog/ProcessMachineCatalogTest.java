package io.corefactory.paper.gui.catalog;

import io.corefactory.api.TntDefinition;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.process.Machine;
import io.corefactory.paper.ShippedContent;
import io.corefactory.paper.gui.catalog.CatalogEntry.Kind;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The eight process machines are catalogue entries of the machines shelf, as the other lab tools are. The shelf is the existing
 * "Lab machines" tab, which already holds every machine of processes.yml. Nothing here starts a server.
 */
class ProcessMachineCatalogTest {

    private static final List<String> NEW_MACHINES = List.of("induction_furnace", "powder_mill", "cartridge_press",
            "bullet_swaging_machine", "ammunition_assembly", "metal_lathe", "component_assembly", "heavy_weapons_assembly");

    private static List<CatalogCategory> categories;

    @BeforeAll
    static void build() {
        ShippedContent.Loaded content = ShippedContent.load();
        Map<String, TntDefinition> tnts = content.definitions().tnts();
        categories = CatalogModel.build(
                tnts.values(),
                content.definitions().components().keySet(),
                content.processes().machines().stream().map(Machine::id).toList(),
                content.ores().stream().map(OreDefinition::id).toList(),
                ShippedContent.itemComponentIds());
    }

    @Test
    void theEightMachinesAreMachineEntriesOfTheLabShelf() {
        CatalogCategory lab = categories.stream()
                .filter(category -> category.title().equals("Lab machines"))
                .findFirst()
                .orElseThrow();
        assertThat(lab.entries()).contains(new CatalogEntry(Kind.MACHINE, "induction_furnace"));
        for (String id : NEW_MACHINES) {
            assertThat(lab.entries()).as(id).contains(new CatalogEntry(Kind.MACHINE, id));
        }
    }

    @Test
    void eachNewMachineAppearsInExactlyOneCategory() {
        for (String id : NEW_MACHINES) {
            long count = categories.stream()
                    .flatMap(category -> category.entries().stream())
                    .filter(entry -> entry.id().equals(id))
                    .count();
            assertThat(count).as(id).isEqualTo(1);
        }
    }
}
