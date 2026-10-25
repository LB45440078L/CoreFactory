package io.corefactory.paper.oil;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.oil.Fluid;
import io.corefactory.core.oil.OilPart;
import io.corefactory.core.water.Face;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OilStoreTest {

    private static final UUID OWNER = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID WORLD = UUID.fromString("66666666-7777-8888-9999-000000000000");
    private static final double CAPACITY = 100.0;

    @Test
    void aPipeAndASplitterAndACrossRoundTrip() {
        List<OilDevice> saved = List.of(
                new OilDevice("pi", OilPart.PIPE, OWNER, WORLD, new Pos(1, 64, 2), null, CAPACITY),
                new OilDevice("t1", OilPart.TEE, OWNER, WORLD, new Pos(3, 64, 2), Face.WEST, CAPACITY),
                new OilDevice("x1", OilPart.CROSS, OWNER, WORLD, new Pos(5, 64, 2), Face.UP, CAPACITY));
        saved.get(1).setOutput(Face.NORTH);

        List<OilDevice> back = OilStore.decode(OilStore.encode(saved), CAPACITY, warning -> {
            throw new AssertionError("nothing should be skipped: " + warning);
        });

        assertThat(back).hasSize(3);
        assertThat(back.get(0).id()).isEqualTo("pi");
        assertThat(back.get(0).part()).isEqualTo(OilPart.PIPE);
        assertThat(back.get(0).owner()).isEqualTo(OWNER);
        assertThat(back.get(0).world()).isEqualTo(WORLD);
        assertThat(back.get(0).pos()).isEqualTo(new Pos(1, 64, 2));
        assertThat(back.get(0).face()).isNull();
        assertThat(back.get(0).output()).isNull();
        assertThat(back.get(1).part()).isEqualTo(OilPart.TEE);
        assertThat(back.get(1).face()).isEqualTo(Face.WEST);
        assertThat(back.get(1).output()).isEqualTo(Face.NORTH);
        assertThat(back.get(2).part()).isEqualTo(OilPart.CROSS);
        assertThat(back.get(2).face()).isEqualTo(Face.UP);
        assertThat(back.get(2).output()).isEqualTo(Face.SOUTH);
    }

    @Test
    void aTubesFluidAndLitresRoundTrip() {
        OilDevice tube = new OilDevice("pi", OilPart.PIPE, OWNER, WORLD, new Pos(1, 64, 2), null, CAPACITY);
        tube.tank().accept(Fluid.DIESEL, 42.5);

        OilDevice back = OilStore.decode(OilStore.encode(List.of(tube)), CAPACITY, warning -> {
            throw new AssertionError("nothing should be skipped: " + warning);
        }).get(0);

        assertThat(back.tank().fluid()).contains(Fluid.DIESEL);
        assertThat(back.tank().litres()).isEqualTo(42.5);
    }

    @Test
    void anEmptyTubeSavesNoFluid() {
        OilDevice tube = new OilDevice("pi", OilPart.PIPE, OWNER, WORLD, new Pos(1, 64, 2), null, CAPACITY);

        OilDevice back = OilStore.decode(OilStore.encode(List.of(tube)), CAPACITY, warning -> {
            throw new AssertionError("nothing should be skipped: " + warning);
        }).get(0);

        assertThat(back.tank().fluid()).isEmpty();
        assertThat(back.tank().litres()).isZero();
    }

    @Test
    void aSavedAmountAboveTheCapacityIsCutToIt() {
        OilDevice tube = new OilDevice("pi", OilPart.PIPE, OWNER, WORLD, new Pos(1, 64, 2), null, 500.0);
        tube.tank().accept(Fluid.PETROLEUM, 300.0);

        OilDevice back = OilStore.decode(OilStore.encode(List.of(tube)), CAPACITY, warning -> {
            throw new AssertionError("nothing should be skipped: " + warning);
        }).get(0);

        assertThat(back.tank().litres()).isEqualTo(CAPACITY);
    }

    @Test
    void aSplitterWithoutAnyFaceIsSkipped() {
        String text = entry("t1", "oil_splitter_t", null);
        List<String> warnings = new ArrayList<>();

        assertThat(OilStore.decode(text, CAPACITY, warnings::add)).isEmpty();
        assertThat(warnings).hasSize(1);
    }

    @Test
    void aSavedOutputOnTheInputsLineIsReplacedByTheDefault() {
        String text = entry("t1", "oil_splitter_t", "west", "west");
        List<String> warnings = new ArrayList<>();

        OilDevice device = OilStore.decode(text, CAPACITY, warnings::add).get(0);

        assertThat(device.output()).isEqualTo(Face.SOUTH);
        assertThat(warnings).isEmpty();
    }

    @Test
    void aWaterPartIsSkippedAndTheRestStay() {
        String text = """
                version: 1
                devices:
                  0:
                    type: water_pipe
                    id: water
                    owner: %s
                    world: %s
                    x: 0
                    y: 0
                    z: 0
                  1:
                    type: oil_pipe
                    id: oil
                    owner: %s
                    world: %s
                    x: 1
                    y: 0
                    z: 0
                """.formatted(OWNER, WORLD, OWNER, WORLD);
        List<String> warnings = new ArrayList<>();

        List<OilDevice> devices = OilStore.decode(text, CAPACITY, warnings::add);

        assertThat(devices).extracting(OilDevice::id).containsExactly("oil");
        assertThat(warnings).hasSize(1);
    }

    private static String entry(String id, String type, String face) {
        return entry(id, type, face, null);
    }

    private static String entry(String id, String type, String face, String output) {
        String faceLine = face == null ? "" : "    face: " + face + "\n";
        String outputLine = output == null ? "" : "    output: " + output + "\n";
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
                %s%s
                """.formatted(type, id, OWNER, WORLD, faceLine, outputLine);
    }
}
