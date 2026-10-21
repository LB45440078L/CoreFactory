package io.corefactory.core.energy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GridGraphTest {

    private static EnergySnapshot snapshot(List<Pylon> pylons, List<GeneratorOutput> generators, List<Wire> wires) {
        return new EnergySnapshot(pylons, generators, wires, List.of(), List.of(), List.of(), GridRegistry.EMPTY);
    }

    private static Pylon pylon(String id, int x, int z) {
        return new Pylon(id, new Pos(x, 64, z), PylonType.SMALL);
    }

    private static GeneratorOutput generator(String id, int x, int z, double outputW) {
        return new GeneratorOutput(id, new Pos(x, 70, z), outputW);
    }

    @Test
    void aPylonWithNoWireIsAGridOfItsOwn() {
        List<GridGraph.Component> components = GridGraph.components(
                snapshot(List.of(pylon("a", 0, 0), pylon("b", 30, 0)), List.of(), List.of()));
        assertThat(components).hasSize(2);
        assertThat(components.get(0).pylonIds()).containsExactly("a");
        assertThat(components.get(1).pylonIds()).containsExactly("b");
    }

    @Test
    void aPylonWireJoinsTwoPylons() {
        List<GridGraph.Component> components = GridGraph.components(snapshot(
                List.of(pylon("a", 0, 0), pylon("b", 10, 0)), List.of(), List.of(new Wire("w", "a", "b"))));
        assertThat(components).hasSize(1);
        assertThat(components.get(0).pylonIds()).containsExactly("a", "b");
    }

    @Test
    void aGeneratorWiredToAPylonBelongsToItsGrid() {
        List<GridGraph.Component> components = GridGraph.components(snapshot(
                List.of(pylon("a", 0, 0)),
                List.of(generator("g", 3, 4, 500)),
                List.of(new Wire("w", "g", "a"))));
        assertThat(components).hasSize(1);
        assertThat(components.get(0).generatorIds()).containsExactly("g");
        assertThat(components.get(0).wireBlocks()).isCloseTo(5.0, within(1e-12));
    }

    @Test
    void aGeneratorWithNoWireBelongsToNoGrid() {
        List<GridGraph.Component> components = GridGraph.components(snapshot(
                List.of(pylon("a", 0, 0)), List.of(generator("g", 3, 0, 500)), List.of()));
        assertThat(components).hasSize(1);
        assertThat(components.get(0).generatorIds()).isEmpty();
        assertThat(components.get(0).wireBlocks()).isZero();
    }

    @Test
    void theWireBlocksAreTheLongestNearestWireOfAGenerator() {
        List<GridGraph.Component> components = GridGraph.components(snapshot(
                List.of(pylon("a", 0, 0), pylon("b", 20, 0)),
                List.of(generator("g1", 2, 0, 1), generator("g2", 18, 0, 1), generator("g3", 10, 3, 1)),
                List.of(new Wire("w1", "g1", "a"), new Wire("w2", "g2", "b"),
                        new Wire("w3", "g3", "a"), new Wire("w4", "g3", "b"))));
        assertThat(components).hasSize(1);
        assertThat(components.get(0).wireBlocks()).isCloseTo(Math.hypot(10, 3), within(1e-9));
    }

    @Test
    void aGeneratorToGeneratorWireIsIgnored() {
        List<GridGraph.Component> components = GridGraph.components(snapshot(
                List.of(pylon("a", 0, 0)),
                List.of(generator("g1", 2, 0, 1), generator("g2", 4, 0, 1)),
                List.of(new Wire("w1", "g1", "a"), new Wire("w2", "g2", "g1"))));
        assertThat(components.get(0).generatorIds()).containsExactly("g1");
    }

    @Test
    void aWireTooLongIsIgnored() {
        List<GridGraph.Component> components = GridGraph.components(snapshot(
                List.of(pylon("a", 0, 0)), List.of(generator("g", 25, 0, 1)), List.of(new Wire("w", "g", "a"))));
        assertThat(components.get(0).generatorIds()).isEmpty();
    }

    @Test
    void aWireToAnUnknownDeviceIsIgnored() {
        List<GridGraph.Component> components = GridGraph.components(snapshot(
                List.of(pylon("a", 0, 0), pylon("b", 9, 0)), List.of(), List.of(new Wire("w", "a", "ghost"))));
        assertThat(components).hasSize(2);
    }

    @Test
    void componentsAreOrderedBySmallestPylonId() {
        List<GridGraph.Component> components = GridGraph.components(snapshot(
                List.of(pylon("z", 0, 0), pylon("m", 40, 0), pylon("a", 80, 0)), List.of(), List.of()));
        assertThat(components).extracting(component -> component.pylonIds().get(0))
                .containsExactly("a", "m", "z");
    }
}
