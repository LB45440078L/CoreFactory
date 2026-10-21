package io.corefactory.core.energy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WireRulesTest {

    private static Endpoint pylon(String id, int x, int z) {
        return new Endpoint(id, DeviceKind.PYLON, new Pos(x, 64, z));
    }

    private static Endpoint generator(String id, int x, int z) {
        return new Endpoint(id, DeviceKind.GENERATOR, new Pos(x, 70, z));
    }

    private static Endpoint consumer(String id, int x, int z) {
        return new Endpoint(id, DeviceKind.CONSUMER, new Pos(x, 66, z));
    }

    private static Endpoint accumulator(String id, int x, int z) {
        return new Endpoint(id, DeviceKind.ACCUMULATOR, new Pos(x, 66, z));
    }

    @Test
    void aGeneratorMayWireToAPylon() {
        assertThat(WireRules.validate(generator("g", 0, 0), pylon("p", 5, 0))).isEqualTo(WireCheck.OK);
    }

    @Test
    void aPylonMayWireToAPylon() {
        assertThat(WireRules.validate(pylon("a", 0, 0), pylon("b", 10, 0))).isEqualTo(WireCheck.OK);
    }

    @Test
    void aGeneratorMayNotWireToAGenerator() {
        assertThat(WireRules.validate(generator("g1", 0, 0), generator("g2", 2, 0))).isEqualTo(WireCheck.SAME_KIND);
    }

    @Test
    void aConsumerOrAnAccumulatorTakesNoWire() {
        assertThat(WireRules.validate(consumer("c", 1, 0), pylon("p", 0, 0))).isEqualTo(WireCheck.NOT_WIRABLE);
        assertThat(WireRules.validate(accumulator("a", 1, 0), generator("g", 0, 0))).isEqualTo(WireCheck.NOT_WIRABLE);
    }

    @Test
    void aDeviceMayNotWireToItself() {
        assertThat(WireRules.validate(pylon("p", 0, 0), pylon("p", 0, 0))).isEqualTo(WireCheck.SAME_DEVICE);
    }

    @Test
    void aWireOfTwentyFourBlocksIsTheLongestAllowed() {
        assertThat(WireRules.validate(pylon("a", 0, 0), pylon("b", 24, 0))).isEqualTo(WireCheck.OK);
        assertThat(WireRules.validate(pylon("a", 0, 0), pylon("b", 0, -24))).isEqualTo(WireCheck.OK);
    }

    @Test
    void aWireLongerThanTwentyFourBlocksIsRefused() {
        assertThat(WireRules.validate(pylon("a", 0, 0), pylon("b", 25, 0))).isEqualTo(WireCheck.TOO_LONG);
        assertThat(WireRules.validate(pylon("a", 0, 0), pylon("b", 17, 17))).isEqualTo(WireCheck.TOO_LONG);
    }

    @Test
    void theHeightDoesNotCountTowardTheLength() {
        assertThat(WireRules.validate(pylon("a", 0, 0), new Endpoint("b", DeviceKind.PYLON, new Pos(0, 200, 0))))
                .isEqualTo(WireCheck.OK);
    }

    @Test
    void theVerdictDoesNotDependOnTheOrderOfTheEnds() {
        Endpoint[][] pairs = {
            {generator("g", 0, 0), pylon("p", 5, 0)},
            {pylon("a", 0, 0), pylon("b", 30, 0)},
            {generator("g1", 0, 0), generator("g2", 1, 0)},
            {consumer("c", 0, 0), pylon("p", 0, 0)},
            {pylon("p", 0, 0), pylon("p", 0, 0)}
        };
        for (Endpoint[] pair : pairs) {
            assertThat(WireRules.validate(pair[0], pair[1])).isEqualTo(WireRules.validate(pair[1], pair[0]));
        }
    }

    @Test
    void aSecondWireBetweenTheSameEndsIsRefusedInEitherDirection() {
        Endpoint generator = generator("g", 0, 0);
        Endpoint pylon = pylon("p", 4, 0);
        assertThat(WireRules.validate(generator, pylon, List.of(new Wire("w", "g", "p"))))
                .isEqualTo(WireCheck.DUPLICATE);
        assertThat(WireRules.validate(pylon, generator, List.of(new Wire("w", "g", "p"))))
                .isEqualTo(WireCheck.DUPLICATE);
    }

    @Test
    void aWireBetweenOtherEndsIsNotADuplicate() {
        assertThat(WireRules.validate(generator("g", 0, 0), pylon("p", 4, 0), List.of(new Wire("w", "g", "q"))))
                .isEqualTo(WireCheck.OK);
    }

    @Test
    void theChecksRunInOrderSoTheFirstReasonIsReported() {
        assertThat(WireRules.validate(generator("g", 0, 0), generator("g", 0, 0), List.of()))
                .isEqualTo(WireCheck.SAME_DEVICE);
    }
}
