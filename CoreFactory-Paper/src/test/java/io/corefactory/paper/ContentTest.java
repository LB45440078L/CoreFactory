package io.corefactory.paper;

import io.corefactory.paper.config.YamlMaps;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Machine;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessDefinition;
import io.corefactory.core.process.Portion;
import io.corefactory.core.process.Product;
import io.corefactory.core.research.ResearchTree;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the shipped data files against each other: every reference resolves, every process names a machine, the
 * research line is in order, and every cost is a pack that exists. Nothing here starts a server.
 */
class ContentTest {

    private static final Logger LOG = Logger.getLogger("ContentTest");

    private static YamlConfiguration resource(String name) {
        try (Reader reader = new InputStreamReader(
                ContentTest.class.getClassLoader().getResourceAsStream(name), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read " + name, e);
        }
    }

    private static YamlConfiguration materials;
    private static YamlConfiguration items;
    private static YamlConfiguration tnts;
    private static YamlConfiguration processes;
    private static YamlConfiguration research;
    private static YamlConfiguration ores;
    private static Set<String> components;
    private static Set<String> tntIds;
    private static ProcessCatalog catalog;

    @BeforeAll
    static void load() {
        materials = resource("materials.yml");
        items = resource("items.yml");
        tnts = resource("tnts.yml");
        processes = resource("processes.yml");
        research = resource("research.yml");
        ores = resource("ores.yml");
        components = new HashSet<>();
        for (YamlConfiguration file : List.of(materials, items)) {
            ConfigurationSection section = file.getConfigurationSection("components");
            if (section != null) {
                components.addAll(section.getKeys(false));
            }
        }
        tntIds = new HashSet<>();
        ConfigurationSection tntSection = tnts.getConfigurationSection("tnts");
        if (tntSection != null) {
            tntIds.addAll(tntSection.getKeys(false));
        }
        catalog = ProcessCatalog.parse(YamlMaps.toMap(processes), LOG);
    }

    @Test
    void everyComponentIdIsUnique() {
        Set<String> seen = new HashSet<>();
        for (YamlConfiguration file : List.of(materials, items)) {
            for (String id : file.getConfigurationSection("components").getKeys(false)) {
                assertThat(seen.add(id)).as("component id defined twice: " + id).isTrue();
            }
        }
    }

    @Test
    void everyProcessIsValidAndNamesAnExistingMachine() {
        int declared = processes.getConfigurationSection("processes").getKeys(false).size();
        int machines = processes.getConfigurationSection("machines").getKeys(false).size();

        assertThat(catalog.processes()).as("processes skipped as invalid").hasSize(declared);
        assertThat(catalog.machines()).as("machines skipped as invalid").hasSize(machines);
        for (ProcessDefinition process : catalog.processes()) {
            assertThat(catalog.machine(process.machine())).as("machine of " + process.id()).isPresent();
        }
    }

    @Test
    void everyMachineHasAHostBlock() {
        for (Machine machine : catalog.machines()) {
            assertThat(machine.host()).as("host of " + machine.id()).isNotBlank();
        }
    }

    @Test
    void everyMachineIsAPlaceableComponentWithTheSameHost() {
        ConfigurationSection componentSection = materials.getConfigurationSection("components");
        for (Machine machine : catalog.machines()) {
            ConfigurationSection component = componentSection.getConfigurationSection(machine.id());
            assertThat(component).as("component for machine " + machine.id()).isNotNull();
            assertThat(component.getString("material")).as("host of " + machine.id()).isEqualTo(machine.host());
        }
    }

    @Test
    void everyItemReferenceInAProcessResolves() {
        for (ProcessDefinition process : catalog.processes()) {
            for (Portion input : process.inputs()) {
                assertResolves(input.ref(), process.id());
            }
            for (Product output : process.outputs()) {
                assertResolves(output.ref(), process.id());
            }
        }
    }

    @Test
    void everyReferenceInARecipeResolves() {
        for (YamlConfiguration file : List.of(materials, items)) {
            ConfigurationSection section = file.getConfigurationSection("components");
            for (String id : section.getKeys(false)) {
                ConfigurationSection ingredients = section.getConfigurationSection(id + ".recipe.ingredients");
                if (ingredients == null) {
                    continue;
                }
                for (String key : ingredients.getKeys(false)) {
                    String value = ingredients.getString(key, "");
                    if (value.startsWith("item:")) {
                        assertThat(components).as("recipe of " + id + " ingredient " + key)
                                .contains(value.substring("item:".length()));
                    } else if (value.startsWith("tnt:")) {
                        assertThat(tntIds).as("recipe of " + id + " ingredient " + key)
                                .contains(value.substring("tnt:".length()));
                    }
                }
            }
        }
    }

    @Test
    void everyOreDropIsAComponent() {
        ConfigurationSection section = ores.getConfigurationSection("ores");
        for (String id : section.getKeys(false)) {
            String drop = section.getString(id + ".drop");
            if (drop != null) {
                assertThat(components).as("drop of " + id).contains(drop);
            }
        }
    }

    @Test
    void everyResearchUnlockExists() {
        ConfigurationSection nodes = research.getConfigurationSection("nodes");
        Set<String> known = new HashSet<>(components);
        known.addAll(tntIds);
        for (String node : nodes.getKeys(false)) {
            for (String unlock : nodes.getStringList(node + ".unlocks")) {
                assertThat(known).as("unlock of " + node).contains(unlock);
            }
        }
    }

    @Test
    void everyResearchRequirementIsAnEarlierNode() {
        ConfigurationSection nodes = research.getConfigurationSection("nodes");
        Set<String> earlier = new HashSet<>();
        for (String node : nodes.getKeys(false)) {
            for (String required : nodes.getStringList(node + ".requires")) {
                assertThat(earlier).as("requirement " + required + " of " + node).contains(required);
            }
            earlier.add(node);
        }
    }

    @Test
    void theResearchTreeLoadsWithEveryNode() {
        ResearchTree tree = ResearchTree.parse(YamlMaps.toMap(research), LOG);

        assertThat(tree.nodes()).hasSize(research.getConfigurationSection("nodes").getKeys(false).size());
    }

    @Test
    void everyResearchCostIsAResearchPack() {
        ConfigurationSection nodes = research.getConfigurationSection("nodes");
        for (String node : nodes.getKeys(false)) {
            ConfigurationSection cost = nodes.getConfigurationSection(node + ".cost");
            if (cost == null) {
                continue;
            }
            for (String pack : cost.getKeys(false)) {
                assertThat(components).as("cost pack of " + node).contains(pack);
                assertThat(pack).as("cost of " + node).startsWith("research_pack_");
            }
        }
    }

    @Test
    void everyResearchPackIsMadeByAProcessOrIsAResearchPackComponent() {
        Set<String> made = new HashSet<>();
        for (ProcessDefinition process : catalog.processes()) {
            for (Product output : process.outputs()) {
                if (output.ref().kind() == ItemRef.Kind.ITEM) {
                    made.add(output.ref().name());
                }
            }
        }
        ConfigurationSection nodes = research.getConfigurationSection("nodes");
        for (String node : nodes.getKeys(false)) {
            ConfigurationSection cost = nodes.getConfigurationSection(node + ".cost");
            if (cost == null) {
                continue;
            }
            for (String pack : cost.getKeys(false)) {
                assertThat(made).as("pack " + pack + " is made by a process").contains(pack);
            }
        }
    }

    @Test
    void theResearchConsoleIsAMachineWithSixPackSlots() {
        Machine console = catalog.machine("research_console").orElseThrow();

        assertThat(console.inputSlots()).isEqualTo(6);
        assertThat(catalog.processesFor("research_console")).isEmpty();
    }

    @Test
    void theLastPylonsAndAccumulatorsAreMadeOnlyByTheHeavyAssembly() {
        ConfigurationSection componentSection = materials.getConfigurationSection("components");
        for (String id : List.of("pylon_4", "pylon_5", "accumulator_4", "accumulator_5")) {
            assertThat(componentSection.getConfigurationSection(id).contains("recipe")).as("recipe of " + id).isFalse();
            List<ProcessDefinition> makers = catalog.processes().stream()
                    .filter(process -> process.outputs().stream()
                            .anyMatch(product -> product.ref().kind() == ItemRef.Kind.ITEM && product.ref().name().equals(id)))
                    .toList();
            assertThat(makers).as("processes making " + id).hasSize(1);
            ProcessDefinition maker = makers.get(0);
            assertThat(maker.machine()).as("machine of " + maker.id()).isEqualTo("heavy_weapons_assembly");
            assertThat(maker.inputs().stream().map(Portion::ref).distinct().count())
                    .as("distinct inputs of " + maker.id()).isLessThanOrEqualTo(6);
        }
    }

    private static void assertResolves(ItemRef ref, String process) {
        if (ref.kind() == ItemRef.Kind.ITEM) {
            assertThat(components).as("component of " + process + ": " + ref).contains(ref.name());
        } else {
            assertThat(ref.name()).as("material of " + process).matches("[A-Z0-9_]+");
        }
    }
}
