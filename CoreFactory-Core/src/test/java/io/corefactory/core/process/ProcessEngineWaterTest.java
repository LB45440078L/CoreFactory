package io.corefactory.core.process;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/** A process whose input is a water bucket: the water network can stand in for the bucket. */
class ProcessEngineWaterTest {

    private static final ItemRef BUCKET = ItemRef.material("WATER_BUCKET");
    private static final ItemRef STEAM = ItemRef.item("steam");

    private static final String YAML = """
            machines:
              boiler:
                host: FURNACE
                slots: {input: 2, output: 2, fuel: 0}
            processes:
              distill:
                machine: boiler
                ticks: 2
                inputs: ["material:WATER_BUCKET:1"]
                outputs:
                  - {item: item:steam, amount: 1}
            """;

    private ProcessCatalog catalog;
    private ProcessEngine engine;

    @BeforeEach
    void setUp() {
        catalog = ProcessCatalog.parse(YamlConfiguration.loadConfiguration(new StringReader(YAML)),
                Logger.getLogger("ProcessEngineWaterTest"));
        engine = new ProcessEngine(catalog, ProcessEngine.FUEL, ref -> 64);
    }

    private static int count(List<Stack> slots, ItemRef ref) {
        int total = 0;
        for (Stack slot : slots) {
            if (ref.equals(slot.ref())) {
                total += slot.count();
            }
        }
        return total;
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
    void theWaterBucketIsWhatTheProcessNeedsWithoutWater() {
        assertThat(ProcessEngine.takesWater(catalog.process("distill").orElseThrow())).isTrue();
        MachineState state = new MachineState("boiler", MemoryStack.slots(2), MemoryStack.slots(2), List.of());

        assertThat(engine.select(state)).isEmpty();
        assertThat(engine.tick(state, fixed(0.0))).isEqualTo(ProcessEngine.Step.IDLE);
    }

    @Test
    void aDeliveringNetworkStartsTheRunWithoutABucket() {
        MachineState state = new MachineState("boiler", MemoryStack.slots(2), MemoryStack.slots(2), List.of());

        assertThat(engine.select(state, true)).isPresent();
        assertThat(engine.tick(state, fixed(0.0), true)).isEqualTo(ProcessEngine.Step.RUNNING);
        assertThat(engine.tick(state, fixed(0.0), true)).isEqualTo(ProcessEngine.Step.COMPLETED);
        assertThat(count(state.outputs(), STEAM)).isEqualTo(1);
    }

    @Test
    void aWaterRunLeavesAnyBucketInTheSlots() {
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, BUCKET, 1);
        MachineState state = new MachineState("boiler", inputs, MemoryStack.slots(2), List.of());

        engine.tick(state, fixed(0.0), true);
        engine.tick(state, fixed(0.0), true);

        assertThat(count(inputs, BUCKET)).isEqualTo(1);
    }

    @Test
    void aRunWithoutWaterTakesTheBucketAsBefore() {
        List<Stack> inputs = MemoryStack.slots(2);
        MemoryStack.fill(inputs, 0, BUCKET, 1);
        MachineState state = new MachineState("boiler", inputs, MemoryStack.slots(2), List.of());

        engine.tick(state, fixed(0.0));
        engine.tick(state, fixed(0.0));

        assertThat(count(inputs, BUCKET)).isZero();
    }
}
