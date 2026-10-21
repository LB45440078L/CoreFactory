package io.corefactory.core.geology;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeologyServiceTest {

    private static final Set<String> STONE = Set.of("STONE");

    private static OreDefinition ore(String id, String host, double veins, int size, Set<String> replace) {
        return new OreDefinition(id, id, host, "raw_" + id, 1, 2, replace, -32, 16, veins, size, size);
    }

    /** Every block is stone, so the two ores compete for the same blocks in every chunk. */
    private static final BlockSampler ALL_STONE = (x, y, z) -> "STONE";

    private static GeologyService crowded() {
        return new GeologyService(List.of(
                ore("first", "SMOOTH_BASALT", 6.0, 10, STONE),
                ore("second", "MUD_BRICKS", 6.0, 10, STONE)));
    }

    @Test
    void theEarlierOreKeepsEveryContestedBlock() {
        GeologyService service = crowded();

        List<Placement> plan = service.plan(11L, 0, 0, ALL_STONE);
        List<Placement> firstAlone = VeinPlanner.plan(11L, 0, 0, 0, service.ore(0), ALL_STONE);

        List<Placement> firstOnly = plan.stream().filter(p -> p.oreIndex() == 0).toList();
        assertThat(firstOnly).isEqualTo(firstAlone);
    }

    @Test
    void noBlockIsPlannedTwice() {
        GeologyService service = crowded();

        for (long seed = 0; seed < 20; seed++) {
            List<Placement> plan = service.plan(seed, 1, -1, ALL_STONE);
            Set<String> seen = new HashSet<>();
            for (Placement p : plan) {
                assertThat(seen.add(p.x() + "," + p.y() + "," + p.z())).isTrue();
            }
        }
    }

    @Test
    void theLaterOreFillsWhatTheEarlierOneLeaves() {
        GeologyService service = crowded();

        List<Placement> plan = service.plan(11L, 0, 0, ALL_STONE);

        assertThat(plan.stream().anyMatch(p -> p.oreIndex() == 1)).isTrue();
    }

    @Test
    void planIsDeterministic() {
        GeologyService service = crowded();

        assertThat(service.plan(5L, 2, 3, ALL_STONE)).isEqualTo(service.plan(5L, 2, 3, ALL_STONE));
        assertThat(service.plan(6L, 2, 3, ALL_STONE)).isNotEqualTo(service.plan(5L, 2, 3, ALL_STONE));
    }

    @Test
    void everyPlacementIsOnAReplaceableBlock() {
        GeologyService service = new GeologyService(List.of(
                ore("limestone", "SMOOTH_BASALT", 4.0, 6, Set.of("STONE", "DIRT"))));
        BlockSampler mixed = (x, y, z) -> (x + y + z) % 3 == 0 ? "STONE" : "GRAVEL";

        for (Placement p : service.plan(3L, 0, 0, mixed)) {
            assertThat(mixed.materialAt(p.x(), p.y(), p.z())).isEqualTo("STONE");
        }
    }

    @Test
    void includeHostsRePlansTheOreOwnHostBlocks() {
        GeologyService service = new GeologyService(List.of(
                ore("first", "SMOOTH_BASALT", 4.0, 6, STONE)));
        // Every block is already a host (a previous retrofit placed the ore everywhere).
        BlockSampler hosts = (x, y, z) -> "SMOOTH_BASALT";

        assertThat(service.plan(9L, 0, 0, hosts)).isEmpty();
        assertThat(service.plan(9L, 0, 0, hosts, true)).isNotEmpty()
                .allSatisfy(p -> assertThat(p.oreIndex()).isZero());
    }

    @Test
    void includeHostsDoesNotLetAnOreTakeAnotherOresHost() {
        GeologyService service = new GeologyService(List.of(
                ore("first", "SMOOTH_BASALT", 4.0, 6, STONE),
                ore("second", "MUD_BRICKS", 4.0, 6, STONE)));
        BlockSampler mud = (x, y, z) -> "MUD_BRICKS";

        for (Placement p : service.plan(9L, 0, 0, mud, true)) {
            assertThat(p.oreIndex()).isEqualTo(1);
        }
    }

    @Test
    void acceptsReplaceMaterialsAndOptionallyTheHost() {
        GeologyService service = crowded();

        assertThat(service.accepts(0, "STONE", false)).isTrue();
        assertThat(service.accepts(0, "SMOOTH_BASALT", false)).isFalse();
        assertThat(service.accepts(0, "SMOOTH_BASALT", true)).isTrue();
        assertThat(service.accepts(0, "AIR", true)).isFalse();
        assertThat(service.hostMaterials()).containsExactlyInAnyOrder("SMOOTH_BASALT", "MUD_BRICKS");
    }

    @Test
    void indexOfFindsAnOreByIdAndSaysMinusOneForAnUnknownId() {
        GeologyService service = crowded();

        assertThat(service.indexOf("first")).isZero();
        assertThat(service.indexOf("second")).isEqualTo(1);
        assertThat(service.indexOf("third")).isEqualTo(-1);
        assertThat(service.indexOf("First")).isEqualTo(-1);
    }

    @Test
    void tooManyOresAreRefused() {
        OreDefinition one = ore("one", "SMOOTH_BASALT", 1.0, 2, STONE);
        List<OreDefinition> many = java.util.Collections.nCopies(257, one);

        assertThatThrownBy(() -> new GeologyService(many)).isInstanceOf(IllegalArgumentException.class);
    }
}
