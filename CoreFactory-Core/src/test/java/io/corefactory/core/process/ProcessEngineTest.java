package io.corefactory.core.process;

import io.corefactory.core.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessEngineTest {

    private static final ItemRef RAW = ItemRef.item("raw");
    private static final ItemRef CRUSHED = ItemRef.item("crushed");
    private static final ItemRef SLAG = ItemRef.item("slag");
    private static final ItemRef CALCINE = ItemRef.item("calcine");
    private static final ItemRef COAL = ItemRef.material("COAL");
    private static final ItemRef STONE = ItemRef.material("STONE");

    private static final String YAML = """
            machines:
              crusher:
                host: DISPENSER
                slots: {input: 2, output: 3, fuel: 0}
              roaster:
                host: FURNACE
                slots: {input: 2, output: 3, fuel: 1}
            processes:
              crush:
                machine: crusher
                ticks: 3
                inputs: ["item:raw:2"]
                outputs:
                  - {item: item:crushed, amount: 2}
                  - {item: item:slag, amount: 1, chance: 0.5}
              pair:
                machine: crusher
                ticks: 2
                inputs: ["item:raw:1", "material:STONE:1"]
                outputs: [{item: item:crushed, amount: 1}]
              roast:
                machine: roaster
                ticks: 2
                fuel: true
                inputs: ["item:crushed:1"]
                outputs: [{item: item:calcine, amount: 1}]
              fail_chance:
                machine: roaster
                ticks: 1
                inputs: ["material:STONE:1"]
                outputs: [{item: item:slag, amount: 1, chance: 0}]
            """;

    private ProcessCatalog catalog;
    private ProcessEngine engine;

    @BeforeEach
    void setUp() {
        catalog = ProcessCatalog.parse(TestConfig.yaml(YAML),
                Logger.getLogger("ProcessEngineTest"));
        engine = new ProcessEngine(catalog, ProcessEngine.FUEL, ref -> 64);
    }

    private static MachineState crusher(List<Stack> inputs, List<Stack> outputs) {
        return new MachineState("crusher", inputs, outputs, List.of());
    }

    private static MachineState roaster(List<Stack> inputs, List<Stack> outputs, List<Stack> fuel) {
        return new MachineState("roaster", inputs, outputs, fuel);
    }

    private static Random fixed(double value) {
        return new Random() {
            @Override
            public double nextDouble() {
                return value;
            }
        };
    }

    @Test
    void selectsNothingWithoutItems() {
        MachineState state = crusher(MemoryStack.slots(2), MemoryStack.slots(3));

        assertThat(engine.select(state)).isEmpty();
        assertThat(engine.tick(state, fixed(0.0))).isEqualTo(ProcessEngine.Step.IDLE);
        assertThat(state.processId()).isNull();
    }

    @Test
    void selectsTheFirstProcessWhoseInputsArePresentInFull() {
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, RAW, 1);
        MachineState state = crusher(inputs, MemoryStack.slots(3));

        assertThat(engine.select(state)).isEmpty();

        MemoryStack.fill(inputs, 1, STONE, 1);
        assertThat(engine.select(state).orElseThrow().id()).isEqualTo("pair");
    }

    @Test
    void countsAcrossSlotsAndRunsTheTicksThenCompletes() {
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, RAW, 1);
        MemoryStack.fill(inputs, 1, RAW, 1);
        List<Stack> outputs = MemoryStack.slots(3);
        MachineState state = crusher(inputs, outputs);

        assertThat(engine.tick(state, fixed(0.0))).isEqualTo(ProcessEngine.Step.RUNNING);
        assertThat(state.processId()).isEqualTo("crush");
        assertThat(state.progress()).isEqualTo(1);
        assertThat(inputs.get(0).count()).isEqualTo(1);

        assertThat(engine.tick(state, fixed(0.0))).isEqualTo(ProcessEngine.Step.RUNNING);
        assertThat(engine.tick(state, fixed(0.0))).isEqualTo(ProcessEngine.Step.COMPLETED);

        assertThat(inputs.get(0).ref()).isNull();
        assertThat(inputs.get(1).ref()).isNull();
        assertThat(outputs.get(0).ref()).isEqualTo(CRUSHED);
        assertThat(outputs.get(0).count()).isEqualTo(2);
        assertThat(state.processId()).isNull();
        assertThat(state.progress()).isZero();
    }

    @Test
    void keepsTheInputsUntilThePassEnds() {
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, RAW, 2);
        MachineState state = crusher(inputs, MemoryStack.slots(3));

        engine.tick(state, fixed(0.0));
        engine.tick(state, fixed(0.0));

        assertThat(inputs.get(0).count()).isEqualTo(2);
    }

    @Test
    void chanceOutputsAreRolledOnceAtTheEnd() {
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, RAW, 2);
        List<Stack> outputs = MemoryStack.slots(3);
        MachineState state = crusher(inputs, outputs);

        engine.tick(state, fixed(0.0));
        engine.tick(state, fixed(0.0));
        engine.tick(state, fixed(0.6));

        assertThat(outputs.get(0).count()).isEqualTo(2);
        assertThat(outputs.get(1).ref()).isNull();
    }

    @Test
    void chanceOutputIsMadeWhenTheRollIsBelowTheChance() {
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, RAW, 2);
        List<Stack> outputs = MemoryStack.slots(3);
        MachineState state = crusher(inputs, outputs);

        engine.tick(state, fixed(0.0));
        engine.tick(state, fixed(0.0));
        engine.tick(state, fixed(0.49));

        assertThat(outputs.get(1).ref()).isEqualTo(SLAG);
        assertThat(outputs.get(1).count()).isEqualTo(1);
    }

    @Test
    void zeroChanceNeverMakesItsOutput() {
        assertThat(engine.roll(catalog.process("fail_chance").orElseThrow(), fixed(0.0))).isEmpty();
        assertThat(engine.roll(catalog.process("fail_chance").orElseThrow(), fixed(0.999))).isEmpty();
    }

    @Test
    void waitsWhenTheRolledProductsDoNotFitAndDoesNotRollAgain() {
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, RAW, 2);
        List<Stack> outputs = MemoryStack.slots(3);
        MemoryStack.fill(outputs, 0, STONE, 64);
        MemoryStack.fill(outputs, 1, STONE, 64);
        MemoryStack.fill(outputs, 2, STONE, 64);
        MachineState state = crusher(inputs, outputs);
        int[] rolls = {0};
        Random counting = new Random() {
            @Override
            public double nextDouble() {
                rolls[0]++;
                return 0.0;
            }
        };

        assertThat(engine.tick(state, counting)).isEqualTo(ProcessEngine.Step.RUNNING);
        assertThat(engine.tick(state, counting)).isEqualTo(ProcessEngine.Step.RUNNING);
        assertThat(engine.tick(state, counting)).isEqualTo(ProcessEngine.Step.BLOCKED);
        assertThat(engine.tick(state, counting)).isEqualTo(ProcessEngine.Step.BLOCKED);

        assertThat(rolls[0]).isEqualTo(2);
        assertThat(inputs.get(0).count()).isEqualTo(2);
        assertThat(state.pending()).isNotNull();
        assertThat(state.processId()).isEqualTo("crush");

        outputs.get(0).set(null, 0);
        outputs.get(1).set(null, 0);
        assertThat(engine.tick(state, counting)).isEqualTo(ProcessEngine.Step.COMPLETED);
        assertThat(rolls[0]).isEqualTo(2);
        assertThat(outputs.get(0).ref()).isEqualTo(CRUSHED);
        assertThat(outputs.get(1).ref()).isEqualTo(SLAG);
        assertThat(inputs.get(0).ref()).isNull();
    }

    @Test
    void stackLimitDecidesWhetherProductsFit() {
        ProcessEngine small = new ProcessEngine(catalog, ProcessEngine.FUEL, ref -> 1);
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, RAW, 2);
        List<Stack> outputs = MemoryStack.slots(2);
        MachineState state = crusher(inputs, outputs);

        small.tick(state, fixed(0.0));
        small.tick(state, fixed(0.0));
        assertThat(small.tick(state, fixed(0.0))).isEqualTo(ProcessEngine.Step.BLOCKED);
        assertThat(small.fits(outputs, List.of(new Portion(CRUSHED, 2)))).isTrue();
        assertThat(small.fits(outputs, List.of(new Portion(CRUSHED, 3)))).isFalse();
    }

    @Test
    void depositsIntoPartlyFilledStacksBeforeEmptySlots() {
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, RAW, 2);
        List<Stack> outputs = MemoryStack.slots(3);
        MemoryStack.fill(outputs, 0, CRUSHED, 60);
        MachineState state = crusher(inputs, outputs);

        engine.tick(state, fixed(0.0));
        engine.tick(state, fixed(0.0));
        assertThat(engine.tick(state, fixed(0.0))).isEqualTo(ProcessEngine.Step.COMPLETED);

        assertThat(outputs.get(0).count()).isEqualTo(62);
        assertThat(outputs.get(1).ref()).isEqualTo(SLAG);
    }

    @Test
    void removedInputsAbortThePass() {
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, RAW, 2);
        MachineState state = crusher(inputs, MemoryStack.slots(3));
        engine.tick(state, fixed(0.0));
        engine.tick(state, fixed(0.0));

        inputs.get(0).set(null, 0);
        assertThat(engine.tick(state, fixed(0.0))).isEqualTo(ProcessEngine.Step.IDLE);
        assertThat(state.processId()).isNull();
        assertThat(state.progress()).isZero();
    }

    @Test
    void fuelIsRequiredAndBurnsOncePerPass() {
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, CRUSHED, 1);
        List<Stack> fuel = MemoryStack.slots(1);
        List<Stack> outputs = MemoryStack.slots(3);
        MachineState state = roaster(inputs, outputs, fuel);

        assertThat(engine.select(state)).isEmpty();

        MemoryStack.fill(fuel, 0, COAL, 3);
        assertThat(engine.tick(state, fixed(0.0))).isEqualTo(ProcessEngine.Step.RUNNING);
        assertThat(fuel.get(0).count()).isEqualTo(3);
        assertThat(engine.tick(state, fixed(0.0))).isEqualTo(ProcessEngine.Step.COMPLETED);

        assertThat(fuel.get(0).count()).isEqualTo(2);
        assertThat(fuel.get(0).ref()).isEqualTo(COAL);
        assertThat(outputs.get(0).ref()).isEqualTo(CALCINE);
    }

    @Test
    void runsOutOfFuelBetweenPassesAndStopsAtTheNextStart() {
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, CRUSHED, 2);
        List<Stack> fuel = MemoryStack.slots(1);
        MemoryStack.fill(fuel, 0, ItemRef.material("CHARCOAL"), 1);
        MachineState state = roaster(inputs, MemoryStack.slots(3), fuel);

        engine.tick(state, fixed(0.0));
        assertThat(engine.tick(state, fixed(0.0))).isEqualTo(ProcessEngine.Step.COMPLETED);
        assertThat(engine.select(state)).isEmpty();
        assertThat(engine.tick(state, fixed(0.0))).isEqualTo(ProcessEngine.Step.IDLE);
    }

    @Test
    void fuelRemovedMidPassAbortsWithTheInputsKept() {
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, CRUSHED, 1);
        List<Stack> fuel = MemoryStack.slots(1);
        MemoryStack.fill(fuel, 0, COAL, 1);
        MachineState state = roaster(inputs, MemoryStack.slots(3), fuel);

        engine.tick(state, fixed(0.0));
        fuel.get(0).set(null, 0);

        assertThat(engine.tick(state, fixed(0.0))).isEqualTo(ProcessEngine.Step.IDLE);
        assertThat(inputs.get(0).count()).isEqualTo(1);
    }

    @Test
    void restoredProgressResumesAndRollsWhenItEnds() {
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, RAW, 2);
        List<Stack> outputs = MemoryStack.slots(3);
        MachineState state = crusher(inputs, outputs);
        state.restore("crush", 2);

        assertThat(engine.tick(state, fixed(0.0))).isEqualTo(ProcessEngine.Step.COMPLETED);
        assertThat(outputs.get(0).count()).isEqualTo(2);
    }

    @Test
    void duplicateIngredientsNeedTheirTotal() {
        ProcessEngine other = new ProcessEngine(ProcessCatalog.parse(
                TestConfig.yaml("""
                        machines:
                          m:
                            host: DISPENSER
                            slots: {input: 2, output: 1, fuel: 0}
                        processes:
                          twice:
                            machine: m
                            ticks: 1
                            inputs: ["item:raw:1", "item:raw:2"]
                            outputs: [{item: item:out, amount: 1}]
                        """), Logger.getLogger("x")), ProcessEngine.FUEL, ref -> 64);
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, RAW, 2);
        MachineState state = new MachineState("m", inputs, MemoryStack.slots(1), List.of());

        assertThat(other.select(state)).isEmpty();
        MemoryStack.fill(inputs, 1, RAW, 1);
        assertThat(other.tick(state, fixed(0.0))).isEqualTo(ProcessEngine.Step.COMPLETED);
        assertThat(inputs.get(0).ref()).isNull();
        assertThat(inputs.get(1).ref()).isNull();
    }

    @Test
    void seededRandomGivesTheSameRunEveryTime() {
        int first = passesWithSeed(42);
        assertThat(passesWithSeed(42)).isEqualTo(first);
        assertThat(first).isBetween(1, 200);
    }

    private int passesWithSeed(long seed) {
        Random random = new Random(seed);
        int slagMade = 0;
        for (int pass = 0; pass < 200; pass++) {
            List<Stack> inputs = MemoryStack.slots(2);
            MemoryStack.fill(inputs, 0, RAW, 2);
            List<Stack> outputs = MemoryStack.slots(3);
            MachineState state = crusher(inputs, outputs);
            ProcessEngine.Step step;
            do {
                step = engine.tick(state, random);
            } while (step != ProcessEngine.Step.COMPLETED);
            if (outputs.get(1).ref() != null) {
                slagMade++;
            }
        }
        return slagMade;
    }

    @Test
    void selectionFollowsFileOrderAndOnlyMachinesRunTheirOwnProcesses() {
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, CRUSHED, 1);
        MachineState state = roaster(inputs, MemoryStack.slots(3), MemoryStack.slots(1));
        MemoryStack.fill(state.fuel(), 0, COAL, 1);

        assertThat(engine.select(state).orElseThrow().id()).isEqualTo("roast");
        assertThat(catalog.processesFor("roaster")).hasSize(2);
    }
}
