package io.corefactory.core.hologram;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.MachineState;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessEngine;
import io.corefactory.core.process.Stack;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class HologramRulesTest {

    private static final ItemRef RAW = ItemRef.item("raw");
    private static final ItemRef CRUSHED = ItemRef.item("crushed");
    private static final ItemRef COAL = ItemRef.material("COAL");

    private static final String YAML = """
            machines:
              crusher:
                host: DISPENSER
                slots: {input: 2, output: 3, fuel: 0}
              roaster:
                host: FURNACE
                slots: {input: 2, output: 3, fuel: 1}
              press:
                host: DISPENSER
                slots: {input: 1, output: 1, fuel: 0}
            processes:
              crush:
                machine: crusher
                ticks: 3
                inputs: ["item:raw:2"]
                outputs:
                  - {item: item:crushed, amount: 2}
              roast:
                machine: roaster
                ticks: 2
                fuel: true
                inputs: ["item:crushed:1"]
                outputs: [{item: item:crushed, amount: 1}]
            """;

    private ProcessEngine engine;

    @BeforeEach
    void setUp() {
        ProcessCatalog catalog = ProcessCatalog.parse(YamlConfiguration.loadConfiguration(new StringReader(YAML)),
                Logger.getLogger("HologramRulesTest"));
        engine = new ProcessEngine(catalog, ProcessEngine.FUEL, ref -> 64);
    }

    @Test
    void runningProcessShowsEvenWithoutInputs() {
        MachineState state = crusher(0);
        state.start("crush");

        assertThat(HologramRules.shouldShow(engine, state)).isTrue();
    }

    @Test
    void idleMachineWithInputsForAProcessShows() {
        assertThat(HologramRules.shouldShow(engine, crusher(2))).isTrue();
    }

    @Test
    void idleMachineShortOfInputsDoesNotShow() {
        assertThat(HologramRules.shouldShow(engine, crusher(1))).isFalse();
    }

    @Test
    void fuelProcessWithoutFuelDoesNotShow() {
        MachineState roaster = roaster(false);
        assertThat(HologramRules.shouldShow(engine, roaster)).isFalse();

        assertThat(HologramRules.shouldShow(engine, roaster(true))).isTrue();
    }

    @Test
    void machineWithoutProcessesNeverShows() {
        List<Stack> inputs = slots(1);
        inputs.get(0).set(RAW, 5);
        MachineState press = new MachineState("press", inputs, slots(1), List.of());

        assertThat(HologramRules.shouldShow(engine, press)).isFalse();
    }

    @Test
    void machineThatFinishedAndIsIdleDoesNotShow() {
        MachineState state = crusher(2);
        Random zero = new Random() {
            @Override
            public double nextDouble() {
                return 0.0;
            }
        };
        ProcessEngine.Step step = ProcessEngine.Step.IDLE;
        for (int i = 0; i < 10 && step != ProcessEngine.Step.COMPLETED; i++) {
            step = engine.tick(state, zero);
        }

        assertThat(step).isEqualTo(ProcessEngine.Step.COMPLETED);
        assertThat(state.processId()).isNull();
        assertThat(HologramRules.shouldShow(engine, state)).isFalse();
    }

    private static MachineState crusher(int rawCount) {
        List<Stack> inputs = slots(2);
        if (rawCount > 0) {
            inputs.get(0).set(RAW, rawCount);
        }
        return new MachineState("crusher", inputs, slots(3), List.of());
    }

    private static MachineState roaster(boolean withFuel) {
        List<Stack> inputs = slots(2);
        inputs.get(0).set(CRUSHED, 1);
        List<Stack> fuel = slots(1);
        if (withFuel) {
            fuel.get(0).set(COAL, 1);
        }
        return new MachineState("roaster", inputs, slots(3), fuel);
    }

    private static List<Stack> slots(int size) {
        List<Stack> slots = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            slots.add(new Slot());
        }
        return slots;
    }

    /** A slot held in memory, so the rule is tested with plain objects. */
    private static final class Slot implements Stack {

        private ItemRef ref;
        private int count;

        @Override
        public ItemRef ref() {
            return count > 0 ? ref : null;
        }

        @Override
        public int count() {
            return count;
        }

        @Override
        public void set(ItemRef ref, int count) {
            this.ref = count > 0 ? ref : null;
            this.count = Math.max(0, count);
        }
    }
}
