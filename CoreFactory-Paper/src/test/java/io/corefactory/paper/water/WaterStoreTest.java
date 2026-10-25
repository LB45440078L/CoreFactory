package io.corefactory.paper.water;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.PumpFuel;
import io.corefactory.core.water.WaterPart;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WaterStoreTest {

    private static final UUID OWNER = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID WORLD = UUID.fromString("66666666-7777-8888-9999-000000000000");

    @Test
    void aPumpAndASplitterAndAPipeRoundTrip() {
        List<WaterDevice> saved = List.of(
                new WaterDevice("p1", WaterPart.PUMP, OWNER, WORLD, new Pos(1, 64, 2), Face.SOUTH, 3, new PumpFuel.Burn(5, 100)),
                new WaterDevice("t1", WaterPart.TEE, OWNER, WORLD, new Pos(3, 64, 2), Face.WEST, 1, PumpFuel.Burn.empty()),
                new WaterDevice("pi", WaterPart.PIPE, OWNER, WORLD, new Pos(4, 64, 2), null, 1, PumpFuel.Burn.empty()));

        List<WaterDevice> back = WaterStore.decode(WaterStore.encode(saved), warning -> {
            throw new AssertionError("nothing should be skipped: " + warning);
        });

        assertThat(back).hasSize(3);
        WaterDevice pump = back.get(0);
        assertThat(pump.id()).isEqualTo("p1");
        assertThat(pump.part()).isEqualTo(WaterPart.PUMP);
        assertThat(pump.owner()).isEqualTo(OWNER);
        assertThat(pump.world()).isEqualTo(WORLD);
        assertThat(pump.pos()).isEqualTo(new Pos(1, 64, 2));
        assertThat(pump.face()).isEqualTo(Face.SOUTH);
        assertThat(pump.level()).isEqualTo(3);
        assertThat(pump.burn()).isEqualTo(new PumpFuel.Burn(5, 100));
        assertThat(back.get(1).part()).isEqualTo(WaterPart.TEE);
        assertThat(back.get(1).face()).isEqualTo(Face.WEST);
        assertThat(back.get(2).face()).isNull();
    }

    @Test
    void aSplitterWithoutAnyFaceIsSkipped() {
        String text = entry("t1", "water_splitter_t", "", "0", "0");
        List<String> warnings = new ArrayList<>();

        assertThat(WaterStore.decode(text, warnings::add)).isEmpty();
        assertThat(warnings).hasSize(1);
    }

    @Test
    void aVerticalSplitterKeepsItsInputAndOutput() {
        String text = entry("t1", "water_splitter_t", "up", "0", "0");
        List<String> warnings = new ArrayList<>();

        WaterDevice device = WaterStore.decode(text, warnings::add).get(0);

        assertThat(device.face()).isEqualTo(io.corefactory.core.water.Face.UP);
        assertThat(device.output()).isEqualTo(io.corefactory.core.water.Face.SOUTH);
        assertThat(warnings).isEmpty();
    }

    @Test
    void aPumpWithoutAFaceFacesNorthAndItsLevelAndFuelAreClamped() {
        String text = entry("p1", "water_pump", null, "9", "40");
        List<String> warnings = new ArrayList<>();

        List<WaterDevice> devices = WaterStore.decode(text, warnings::add);

        assertThat(warnings).isEmpty();
        assertThat(devices.get(0).face()).isEqualTo(Face.NORTH);
        assertThat(devices.get(0).level()).isEqualTo(5);
        assertThat(devices.get(0).burn().items()).isEqualTo(PumpFuel.CAPACITY);
    }

    @Test
    void anUnknownPartIsSkippedAndTheRestStay() {
        String text = """
                version: 1
                devices:
                  0:
                    type: water_bogus
                    id: x
                    owner: %s
                    world: %s
                    x: 0
                    y: 0
                    z: 0
                  1:
                    type: water_pipe
                    id: pi
                    owner: %s
                    world: %s
                    x: 1
                    y: 0
                    z: 0
                """.formatted(OWNER, WORLD, OWNER, WORLD);
        List<String> warnings = new ArrayList<>();

        List<WaterDevice> devices = WaterStore.decode(text, warnings::add);

        assertThat(devices).extracting(WaterDevice::id).containsExactly("pi");
        assertThat(warnings).hasSize(1);
    }

    private static String entry(String id, String type, String face, String level, String fuel) {
        String faceLine = face == null ? "" : "    face: " + face + "\n";
        return """
                version: 1
                devices:
                  0:
                    type: %s
                    id: %s
                    owner: %s
                    world: %s
                    x: 0
                    y: 64
                    z: 0
                %s    level: %s
                    fuel: %s
                    burn-ticks: 0
                """.formatted(type, id, OWNER, WORLD, faceLine, level, fuel);
    }
}
