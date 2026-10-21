package io.corefactory.core.geology;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import static org.assertj.core.api.Assertions.assertThat;

class VeinPlannerTest {

    private static final Set<String> STONE = Set.of("STONE", "DEEPSLATE");

    private static OreDefinition ore(double veinsPerChunk, int sizeMin, int sizeMax) {
        return new OreDefinition("test_ore", "Test", "SMOOTH_BASALT", "raw_test", 1, 1, STONE, -48, 24,
                veinsPerChunk, sizeMin, sizeMax);
    }

    /** Stone everywhere, so every position is replaceable. */
    private static final BlockSampler ALL_STONE = (x, y, z) -> "STONE";

    /** A mixed world: stone on a checker of columns, dirt elsewhere, with some stone pockets in the air. */
    private static final BlockSampler MIXED = (x, y, z) -> {
        if (Math.floorMod(x, 3) == 0) {
            return "STONE";
        }
        return Math.floorMod(y + z, 5) == 0 ? "DEEPSLATE" : "DIRT";
    };

    @Test
    void sameInputsGiveTheSamePlacements() {
        List<Placement> first = VeinPlanner.plan(42L, 3, -7, 2, ore(3.0, 4, 9), MIXED);
        List<Placement> second = VeinPlanner.plan(42L, 3, -7, 2, ore(3.0, 4, 9), MIXED);

        assertThat(first).isNotEmpty();
        assertThat(second).isEqualTo(first);
    }

    @Test
    void differentSeedGivesDifferentPlacements() {
        List<Placement> base = VeinPlanner.plan(1L, 0, 0, 0, ore(3.0, 4, 9), ALL_STONE);

        for (long seed = 2; seed < 12; seed++) {
            assertThat(VeinPlanner.plan(seed, 0, 0, 0, ore(3.0, 4, 9), ALL_STONE)).isNotEqualTo(base);
        }
    }

    @Test
    void differentChunkOrOreGivesADifferentStream() {
        List<Placement> here = VeinPlanner.plan(7L, 1, 1, 0, ore(2.0, 4, 9), ALL_STONE);

        assertThat(VeinPlanner.plan(7L, 2, 1, 0, ore(2.0, 4, 9), ALL_STONE)).isNotEqualTo(here);
        assertThat(VeinPlanner.plan(7L, 1, 2, 0, ore(2.0, 4, 9), ALL_STONE)).isNotEqualTo(here);
        assertThat(VeinPlanner.plan(7L, 1, 1, 1, ore(2.0, 4, 9), ALL_STONE)).isNotEqualTo(here);
    }

    @Test
    void neverPlacesIntoABlockOutsideTheReplaceSet() {
        OreDefinition ore = ore(4.0, 5, 12);

        for (int chunk = -5; chunk < 5; chunk++) {
            for (Placement p : VeinPlanner.plan(99L, chunk, chunk * 2, 0, ore, MIXED)) {
                assertThat(STONE.contains(MIXED.materialAt(p.x(), p.y(), p.z()))).isTrue();
            }
        }
    }

    @Test
    void neverPlacesWhenNothingIsReplaceable() {
        BlockSampler air = (x, y, z) -> "AIR";

        assertThat(VeinPlanner.plan(5L, 0, 0, 0, ore(5.0, 3, 6), air)).isEmpty();
    }

    @Test
    void placementsStayInTheChunkAndTheYRange() {
        OreDefinition ore = ore(6.0, 4, 10);

        for (int chunk = -3; chunk < 3; chunk++) {
            for (Placement p : VeinPlanner.plan(13L, chunk, -chunk, 0, ore, ALL_STONE)) {
                assertThat(p.x() >> 4).isEqualTo(chunk);
                assertThat(p.z() >> 4).isEqualTo(-chunk);
                assertThat(p.y()).isBetween(ore.minY(), ore.maxY());
                assertThat(p.oreIndex()).isZero();
            }
        }
    }

    @Test
    void placementsAreNotDuplicated() {
        List<Placement> placements = VeinPlanner.plan(21L, 0, 0, 0, ore(10.0, 8, 12), ALL_STONE);

        assertThat(placements).doesNotHaveDuplicates();
    }

    @Test
    void oneVeinOfOneBlockIsExactlyOneBlock() {
        List<Placement> placements = VeinPlanner.plan(8L, 4, 4, 0, ore(1.0, 1, 1), ALL_STONE);

        assertThat(placements).hasSize(1);
    }

    @Test
    void wholeVeinCountGivesExactlyThatManyVeins() {
        OreDefinition ore = ore(1.0, 3, 3);

        for (long seed = 0; seed < 50; seed++) {
            assertThat(VeinPlanner.plan(seed, 0, 0, 0, ore, ALL_STONE)).hasSize(3);
        }
    }

    @Test
    void zeroVeinsPerChunkPlansNothing() {
        assertThat(VeinPlanner.plan(3L, 0, 0, 0, ore(0.0, 3, 6), ALL_STONE)).isEmpty();
    }

    @Test
    void veinCountIsTheIntegerPartPlusAFractionRoll() {
        OreDefinition ore = ore(2.5, 1, 1);
        int total = 0;
        int chunks = 600;
        for (int chunk = 0; chunk < chunks; chunk++) {
            int count = VeinPlanner.plan(77L, chunk, -chunk, 0, ore, ALL_STONE).size();
            assertThat(count).isBetween(2, 3);
            total += count;
        }

        // Expected 2.5 per chunk; the sample mean stays well inside this band.
        assertThat((double) total / chunks).isBetween(2.4, 2.6);
    }

    @Test
    void veinCountRollsOnlyTheFraction() {
        SplittableRandom random = new SplittableRandom(1L);

        assertThat(VeinPlanner.veinCount(0.0, random)).isZero();
        assertThat(VeinPlanner.veinCount(4.0, random)).isEqualTo(4);
        assertThat(VeinPlanner.veinCount(1.5, random)).isBetween(1, 2);
    }

    @Test
    void singleVeinHasTheConfiguredSize() {
        OreDefinition ore = ore(1.0, 5, 5);

        for (long seed = 0; seed < 40; seed++) {
            assertThat(VeinPlanner.plan(seed, 2, 2, 0, ore, ALL_STONE)).hasSize(5);
        }
    }
}
