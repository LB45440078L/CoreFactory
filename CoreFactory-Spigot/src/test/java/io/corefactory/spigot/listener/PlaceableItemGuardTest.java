package io.corefactory.spigot.listener;

import io.corefactory.core.drill.DrillCatalog;
import io.corefactory.core.item.PlaceableItems;
import io.corefactory.core.item.PlaceableItems.Kind;
import io.corefactory.spigot.ShippedContent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The placeable rule against the shipped machines and components: every machine and the solar panel may be placed, and the
 * components that have no placement of their own are refused. Nothing here starts a server, so the block materials are not
 * read; the guard applies the rule only to the block placements Bukkit reports.
 */
class PlaceableItemGuardTest {

    private static final Logger LOG = Logger.getLogger("PlaceableItemGuardTest");

    private static ShippedContent.Loaded content;
    private static Set<String> machines;

    @BeforeAll
    static void load() throws Exception {
        content = ShippedContent.load();
        machines = new HashSet<>();
        content.processes().machines().forEach(machine -> machines.add(machine.id()));
        YamlConfiguration processes;
        try (Reader reader = new InputStreamReader(
                PlaceableItemGuardTest.class.getClassLoader().getResourceAsStream("processes.yml"), StandardCharsets.UTF_8)) {
            processes = YamlConfiguration.loadConfiguration(reader);
        }
        DrillCatalog.parse(processes, content.definitions().components(), LOG).drills()
                .forEach(drill -> machines.add(drill.id()));
    }

    private static boolean allowed(String componentId) {
        return PlaceableItems.allowed(Kind.COMPONENT, componentId, machines::contains);
    }

    @Test
    void theShippedComponentsWithNoPlacementOfTheirOwnAreRefused() {
        assertThat(allowed("neutron_initiator")).isFalse();
        assertThat(allowed("pulse_capacitor")).isFalse();
        assertThat(allowed("gravity_core")).isFalse();
        assertThat(allowed("nuclear_casing")).isFalse();
        assertThat(allowed("anchor_plate")).isFalse();
        assertThat(allowed("high_purity_graphite")).isFalse();
        assertThat(allowed("vitrifier_flux")).isFalse();
        assertThat(allowed("hazmat_fabric")).isFalse();
    }

    @Test
    void everyShippedComponentThatIsNotAMachineOrTheSolarPanelIsRefused() {
        for (String id : content.definitions().components().keySet()) {
            boolean placeable = machines.contains(id) || PlaceableItems.SOLAR_PANEL.equals(id)
                    || PlaceableItems.ENERGY_DEVICES.contains(id) || PlaceableItems.LOGISTICS_DEVICES.contains(id);
            assertThat(allowed(id)).as(id).isEqualTo(placeable);
        }
    }

    @Test
    void theShippedMachinesAndTheSolarPanelArePlaceable() {
        assertThat(machines).contains("crusher", "converter", "petroleum_drill_1", "ore_drill_1");
        assertThat(allowed("crusher")).isTrue();
        assertThat(allowed("petroleum_drill_1")).isTrue();
        assertThat(allowed(PlaceableItems.SOLAR_PANEL)).isTrue();
    }

    @Test
    void thePylonsAndAccumulatorsArePlaceableAndTheWireIsNot() {
        assertThat(allowed("pylon_1")).isTrue();
        assertThat(allowed("accumulator_3")).isTrue();
        assertThat(allowed("energy_wire")).isFalse();
    }
}
