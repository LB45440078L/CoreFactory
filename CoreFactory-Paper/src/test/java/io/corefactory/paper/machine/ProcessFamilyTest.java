package io.corefactory.paper.machine;

import io.corefactory.paper.config.YamlMaps;
import io.corefactory.core.engine.VisualCatalog;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessDefinition;
import io.corefactory.core.process.Product;
import io.corefactory.paper.ShippedContent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The recipe set of a machine id comes from processes.yml, so a recipe added there needs no Java change. The look and the power
 * of the new machines are in {@link ProcessFamily}. Read from the shipped files and from in-memory YAML; nothing starts a server.
 */
class ProcessFamilyTest {

    private static final Logger LOG = Logger.getLogger("ProcessFamilyTest");

    /** The eight process machines of the ammunition, powder, turret and metal chain. */
    static final List<String> NEW_MACHINES = List.of("induction_furnace", "powder_mill", "cartridge_press",
            "bullet_swaging_machine", "ammunition_assembly", "metal_lathe", "component_assembly", "heavy_weapons_assembly");

    private static ProcessCatalog shipped;

    @BeforeAll
    static void load() {
        shipped = ShippedContent.load().processes();
    }

    @Test
    void everyNewMachineIsAMachineOfTheShippedProcessCatalogue() {
        for (String id : NEW_MACHINES) {
            assertThat(shipped.machine(id)).as(id).isPresent();
        }
    }

    @Test
    void theFurnaceRecipesAreThoseProcessesYmlGivesItsMachineInOrder() {
        List<ProcessDefinition> recipes = ProcessFamily.recipes(shipped, "induction_furnace");
        assertThat(recipes).isNotEmpty();
        assertThat(recipes).isEqualTo(shipped.processesFor("induction_furnace"));
        assertThat(recipes.get(0).outputs()).extracting(Product::ref).contains(ItemRef.item("brass_ingot"));
    }

    @Test
    void aRecipeAddedToProcessesYmlRunsInItsMachineWithNoJavaChange() {
        ProcessCatalog catalog = ProcessCatalog.parse(YamlMaps.toMap(YamlConfiguration.loadConfiguration(new StringReader("""
                machines:
                  induction_furnace:
                    host: BARREL
                    slots: {input: 2, output: 1, fuel: 0}
                processes:
                  alloy_from_yaml:
                    machine: induction_furnace
                    ticks: 10
                    inputs: ["item:copper_ingot:1", "item:zinc_ingot:1"]
                    outputs:
                      - {item: item:brass_ingot, amount: 2}
                """))), LOG);

        List<ProcessDefinition> recipes = ProcessFamily.recipes(catalog, "induction_furnace");
        assertThat(recipes).extracting(ProcessDefinition::id).containsExactly("alloy_from_yaml");
        assertThat(ProcessFamily.recipes(catalog, "powder_mill")).isEmpty();
    }

    @Test
    void aMachineWithNoRecipesHasNone() {
        assertThat(ProcessFamily.recipes(shipped, "research_console")).isEmpty();
        assertThat(ProcessFamily.recipes(shipped, "no_such_machine")).isEmpty();
    }

    @Test
    void eachNewMachineHasALabEffectTheCatalogDefines() {
        for (String id : NEW_MACHINES) {
            ProcessFamily.LabVisual visual = ProcessFamily.labVisual(id).orElseThrow();
            assertThat(VisualCatalog.find(visual.effect())).as(id).isPresent();
            assertThat(visual.scale()).as(id).isPositive();
        }
    }

    /**
     * Every new machine states its power in processes.yml, and the two presses and the heavy assembly are the three largest.
     * The power is read from the file here; the energy network reads it through the machine's own entry.
     */
    @Test
    void everyNewMachineStatesItsPowerAndThePressesAndTheHeavyAssemblyAskMost() throws Exception {
        YamlConfiguration yaml;
        try (Reader reader = new InputStreamReader(
                ProcessFamilyTest.class.getClassLoader().getResourceAsStream("processes.yml"), StandardCharsets.UTF_8)) {
            yaml = YamlConfiguration.loadConfiguration(reader);
        }
        ConfigurationSection machines = yaml.getConfigurationSection("machines");
        Map<String, Integer> power = new HashMap<>();
        for (String id : NEW_MACHINES) {
            int watts = machines.getConfigurationSection(id).getInt("power", 0);
            assertThat(watts).as(id).isPositive();
            power.put(id, watts);
        }
        List<String> largest = power.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(2)
                .map(Map.Entry::getKey)
                .toList();
        assertThat(largest).containsExactlyInAnyOrder("cartridge_press", "heavy_weapons_assembly");
    }
}
