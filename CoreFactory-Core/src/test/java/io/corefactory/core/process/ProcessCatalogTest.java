package io.corefactory.core.process;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessCatalogTest {

    private static final Logger LOG = Logger.getLogger("ProcessCatalogTest");

    private static ProcessCatalog parse(String yaml) {
        return ProcessCatalog.parse(YamlConfiguration.loadConfiguration(new StringReader(yaml)), LOG);
    }

    @Test
    void readsMachinesAndProcessesWithTheirReferences() {
        ProcessCatalog catalog = parse("""
                machines:
                  crusher:
                    name: "&7Crusher"
                    host: DISPENSER
                    slots: {input: 1, output: 3, fuel: 0}
                  roaster:
                    host: FURNACE
                    slots: {input: 2, output: 3, fuel: 1}
                processes:
                  crush:
                    machine: crusher
                    ticks: 100
                    inputs: ["item:raw_uraninite:1"]
                    outputs:
                      - {item: item:crushed_uraninite, amount: 2}
                      - {item: item:slag, amount: 1, chance: 0.3}
                  roast:
                    machine: roaster
                    ticks: 200
                    fuel: true
                    inputs: ["item:crushed_uraninite:2", "material:WATER_BUCKET:1"]
                    outputs:
                      - {item: item:uranium_calcine, amount: 1}
                """);

        assertThat(catalog.machines()).extracting(Machine::id).containsExactly("crusher", "roaster");
        Machine crusher = catalog.machine("crusher").orElseThrow();
        assertThat(crusher.name()).isEqualTo("&7Crusher");
        assertThat(crusher.host()).isEqualTo("DISPENSER");
        assertThat(crusher.inputSlots()).isEqualTo(1);
        assertThat(crusher.outputSlots()).isEqualTo(3);
        assertThat(crusher.fuelSlots()).isZero();
        assertThat(catalog.machine("roaster").orElseThrow().name()).isEqualTo("roaster");

        ProcessDefinition crush = catalog.process("crush").orElseThrow();
        assertThat(crush.machine()).isEqualTo("crusher");
        assertThat(crush.ticks()).isEqualTo(100);
        assertThat(crush.fuel()).isFalse();
        assertThat(crush.inputs()).containsExactly(new Portion(ItemRef.item("raw_uraninite"), 1));
        assertThat(crush.outputs()).containsExactly(
                new Product(ItemRef.item("crushed_uraninite"), 2, 1.0),
                new Product(ItemRef.item("slag"), 1, 0.3));

        ProcessDefinition roast = catalog.process("roast").orElseThrow();
        assertThat(roast.fuel()).isTrue();
        assertThat(roast.inputs()).containsExactly(
                new Portion(ItemRef.item("crushed_uraninite"), 2),
                new Portion(ItemRef.material("WATER_BUCKET"), 1));
    }

    @Test
    void looksUpProcessesByMachineAndAcceptedInputs() {
        ProcessCatalog catalog = parse(CATALOG);

        assertThat(catalog.processesFor("crusher")).extracting(ProcessDefinition::id).containsExactly("crush_a", "crush_b");
        assertThat(catalog.processesFor("nothing")).isEmpty();
        assertThat(catalog.acceptsInput("crusher", ItemRef.item("raw_a"))).isTrue();
        assertThat(catalog.acceptsInput("crusher", ItemRef.item("crushed_a"))).isFalse();
        assertThat(catalog.acceptsInput("roaster", ItemRef.item("raw_a"))).isFalse();
        assertThat(catalog.inputsOf("crusher")).containsExactlyInAnyOrder(ItemRef.item("raw_a"), ItemRef.item("raw_b"));
        assertThat(catalog.inputsOf("unknown")).isEmpty();
    }

    @Test
    void skipsMachinesThatCannotBeUsed() {
        ProcessCatalog catalog = parse("""
                machines:
                  no_host:
                    slots: {input: 1, output: 1, fuel: 0}
                  not_a_section: text
                  good:
                    host: DISPENSER
                    slots: {input: 1, output: 1, fuel: 0}
                  negative:
                    host: DISPENSER
                    slots: {input: -1, output: 1, fuel: 0}
                """);

        assertThat(catalog.machines()).extracting(Machine::id).containsExactly("good");
    }

    @Test
    void skipsProcessesThatAreBroken() {
        ProcessCatalog catalog = parse("""
                machines:
                  crusher:
                    host: DISPENSER
                    slots: {input: 1, output: 1, fuel: 0}
                processes:
                  ok:
                    machine: crusher
                    ticks: 5
                    inputs: ["item:raw_a:1"]
                    outputs: [{item: item:out_a, amount: 1}]
                  unknown_machine:
                    machine: missing
                    ticks: 5
                    inputs: ["item:raw_a:1"]
                    outputs: [{item: item:out_a, amount: 1}]
                  no_ticks:
                    machine: crusher
                    ticks: 0
                    inputs: ["item:raw_a:1"]
                    outputs: [{item: item:out_a, amount: 1}]
                  fuel_without_slot:
                    machine: crusher
                    ticks: 5
                    fuel: true
                    inputs: ["item:raw_a:1"]
                    outputs: [{item: item:out_a, amount: 1}]
                  bad_input:
                    machine: crusher
                    ticks: 5
                    inputs: ["raw_a:1"]
                    outputs: [{item: item:out_a, amount: 1}]
                  input_without_amount:
                    machine: crusher
                    ticks: 5
                    inputs: ["item:raw_a"]
                    outputs: [{item: item:out_a, amount: 1}]
                  bad_chance:
                    machine: crusher
                    ticks: 5
                    inputs: ["item:raw_a:1"]
                    outputs: [{item: item:out_a, amount: 1, chance: 1.5}]
                  output_without_item:
                    machine: crusher
                    ticks: 5
                    inputs: ["item:raw_a:1"]
                    outputs: [{amount: 1}]
                  too_many_inputs:
                    machine: crusher
                    ticks: 5
                    inputs: ["item:raw_a:1", "item:raw_b:1"]
                    outputs: [{item: item:out_a, amount: 1}]
                  no_outputs:
                    machine: crusher
                    ticks: 5
                    inputs: ["item:raw_a:1"]
                """);

        assertThat(catalog.processes()).extracting(ProcessDefinition::id).containsExactly("ok");
    }

    @Test
    void readsAnEmptyOrMissingFile() {
        assertThat(ProcessCatalog.parse(null, LOG).machines()).isEmpty();
        assertThat(parse("other: 1\n").processes()).isEmpty();
    }

    @Test
    void parsesItemReferences() {
        assertThat(ItemRef.parse("item:yellowcake")).contains(ItemRef.item("yellowcake"));
        assertThat(ItemRef.parse(" material:COAL_BLOCK ")).contains(ItemRef.material("COAL_BLOCK"));
        assertThat(ItemRef.parse("material:COAL_BLOCK").orElseThrow().toString()).isEqualTo("material:COAL_BLOCK");
        assertThat(ItemRef.parse("yellowcake")).isEmpty();
        assertThat(ItemRef.parse("item:")).isEmpty();
        assertThat(ItemRef.parse(null)).isEmpty();
    }

    @Test
    void researchConsoleIsAMachineWithoutProcesses() {
        ProcessCatalog catalog = parse("""
                machines:
                  research_console:
                    name: "&bResearch Console"
                    host: BOOKSHELF
                    slots: {input: 6, output: 0, fuel: 0}
                """);

        Optional<Machine> console = catalog.machine("research_console");
        assertThat(console).isPresent();
        assertThat(console.get().inputSlots()).isEqualTo(6);
        assertThat(catalog.processesFor("research_console")).isEmpty();
        assertThat(catalog.inputsOf("research_console")).isEmpty();
        assertThat(List.copyOf(catalog.processes())).isEmpty();
    }

    private static final String CATALOG = """
            machines:
              crusher:
                host: DISPENSER
                slots: {input: 2, output: 2, fuel: 0}
              roaster:
                host: FURNACE
                slots: {input: 2, output: 2, fuel: 1}
            processes:
              crush_a:
                machine: crusher
                ticks: 4
                inputs: ["item:raw_a:1"]
                outputs: [{item: item:crushed_a, amount: 1}]
              crush_b:
                machine: crusher
                ticks: 4
                inputs: ["item:raw_b:2"]
                outputs: [{item: item:crushed_a, amount: 1}]
              roast_a:
                machine: roaster
                ticks: 4
                fuel: true
                inputs: ["item:crushed_a:1"]
                outputs: [{item: item:calcine, amount: 1}]
            """;
}
