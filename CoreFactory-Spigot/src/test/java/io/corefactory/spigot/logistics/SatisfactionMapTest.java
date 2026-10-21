package io.corefactory.spigot.logistics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SatisfactionMapTest {

    @Test
    void aLoadedNodeTakesTheSatisfactionOfItsConsumer() {
        Map<String, Double> map = SatisfactionMap.build(
                List.of(new SatisfactionMap.Node("belt:1:64:1", "world:1:64:1", true)),
                id -> "world:1:64:1".equals(id) ? 0.5 : 0.0);
        assertThat(map).containsEntry("belt:1:64:1", 0.5);
    }

    @Test
    void anUnloadedNodeIsZeroAndTheEnergyIsNotAskedForIt() {
        AtomicInteger asked = new AtomicInteger();
        Map<String, Double> map = SatisfactionMap.build(
                List.of(new SatisfactionMap.Node("picker:p", "world:2:64:2", false)),
                id -> {
                    asked.incrementAndGet();
                    return 1.0;
                });
        assertThat(map).containsEntry("picker:p", 0.0);
        assertThat(asked.get()).isZero();
    }

    @Test
    void aValueOutsideZeroToOneIsClampedAndNaNIsZero() {
        assertThat(SatisfactionMap.clamp(1.7)).isEqualTo(1.0);
        assertThat(SatisfactionMap.clamp(-0.2)).isEqualTo(0.0);
        assertThat(SatisfactionMap.clamp(Double.NaN)).isEqualTo(0.0);
        assertThat(SatisfactionMap.clamp(0.25)).isCloseTo(0.25, within(1e-12));
    }

    @Test
    void aConsumerTheEnergyDidNotSeeIsZero() {
        Map<String, Double> map = SatisfactionMap.build(
                List.of(new SatisfactionMap.Node("splitter:s", "world:3:64:3", true)), id -> 0.0);
        assertThat(map).containsEntry("splitter:s", 0.0);
    }

    @Test
    void everyNodeGetsOneEntry() {
        Map<String, Double> map = SatisfactionMap.build(List.of(
                new SatisfactionMap.Node("a", "ca", true),
                new SatisfactionMap.Node("b", "cb", false),
                new SatisfactionMap.Node("c", "cc", true)), id -> 1.0);
        assertThat(map).hasSize(3).containsEntry("a", 1.0).containsEntry("b", 0.0).containsEntry("c", 1.0);
    }
}
