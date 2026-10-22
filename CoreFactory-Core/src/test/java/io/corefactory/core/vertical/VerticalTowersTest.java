package io.corefactory.core.vertical;

import io.corefactory.core.energy.Pos;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerticalTowersTest {

    /** Belts at x, z for every y from yFrom to yTo inclusive. */
    private static void column(Map<Pos, Belt> belts, int x, int z, int yFrom, int yTo, VerticalDir direction, int level) {
        for (int y = yFrom; y <= yTo; y++) {
            belts.put(new Pos(x, y, z), new Belt(direction, level));
        }
    }

    @Test
    void contiguousBeltsFormOneTowerBottomToTop() {
        Map<Pos, Belt> belts = new HashMap<>();
        column(belts, 0, 0, 10, 12, VerticalDir.UP, 1);

        List<VerticalTower> towers = VerticalTowers.towersOf(belts);

        assertThat(towers).hasSize(1);
        VerticalTower tower = towers.get(0);
        assertThat(tower.positions()).containsExactly(new Pos(0, 10, 0), new Pos(0, 11, 0), new Pos(0, 12, 0));
        assertThat(tower.direction()).isEqualTo(VerticalDir.UP);
        assertThat(tower.bottom()).isEqualTo(new Pos(0, 10, 0));
        assertThat(tower.top()).isEqualTo(new Pos(0, 12, 0));
        assertThat(tower.size()).isEqualTo(3);
    }

    @Test
    void aGapSplitsTheColumnIntoTowers() {
        Map<Pos, Belt> belts = new HashMap<>();
        column(belts, 0, 0, 0, 2, VerticalDir.DOWN, 1);
        column(belts, 0, 0, 4, 5, VerticalDir.DOWN, 1);

        List<VerticalTower> towers = VerticalTowers.towersOf(belts);

        assertThat(towers).hasSize(2);
        assertThat(towers.get(0).positions()).containsExactly(new Pos(0, 0, 0), new Pos(0, 1, 0), new Pos(0, 2, 0));
        assertThat(towers.get(1).positions()).containsExactly(new Pos(0, 4, 0), new Pos(0, 5, 0));
    }

    @Test
    void aSingleBeltIsATowerOfOne() {
        Map<Pos, Belt> belts = new HashMap<>();
        belts.put(new Pos(3, 64, -7), new Belt(VerticalDir.DOWN, 2));

        List<VerticalTower> towers = VerticalTowers.towersOf(belts);

        assertThat(towers).hasSize(1);
        assertThat(towers.get(0).size()).isEqualTo(1);
        assertThat(towers.get(0).direction()).isEqualTo(VerticalDir.DOWN);
        assertThat(towers.get(0).level()).isEqualTo(2);
    }

    @Test
    void beltsInNeighbouringColumnsDoNotJoin() {
        Map<Pos, Belt> belts = new HashMap<>();
        column(belts, 0, 0, 0, 1, VerticalDir.UP, 1);
        column(belts, 1, 0, 0, 1, VerticalDir.UP, 1);
        column(belts, 0, 1, 0, 1, VerticalDir.UP, 1);

        assertThat(VerticalTowers.towersOf(belts)).hasSize(3);
    }

    @Test
    void noBeltsMeansNoTowers() {
        assertThat(VerticalTowers.towersOf(new HashMap<>())).isEmpty();
    }

    @Test
    void aRunThatMixesDirectionsIsRejected() {
        Map<Pos, Belt> belts = new HashMap<>();
        belts.put(new Pos(0, 0, 0), new Belt(VerticalDir.UP, 1));
        belts.put(new Pos(0, 1, 0), new Belt(VerticalDir.DOWN, 1));

        assertThatThrownBy(() -> VerticalTowers.towersOf(belts)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theLevelOfATowerIsTheLevelOfItsOutputEnd() {
        Map<Pos, Belt> up = new HashMap<>();
        up.put(new Pos(0, 0, 0), new Belt(VerticalDir.UP, 1));
        up.put(new Pos(0, 1, 0), new Belt(VerticalDir.UP, 3));
        Map<Pos, Belt> down = new HashMap<>();
        down.put(new Pos(0, 0, 0), new Belt(VerticalDir.DOWN, 1));
        down.put(new Pos(0, 1, 0), new Belt(VerticalDir.DOWN, 3));

        assertThat(VerticalTowers.towersOf(up).get(0).level()).isEqualTo(3);
        assertThat(VerticalTowers.towersOf(down).get(0).level()).isEqualTo(1);
    }

    @Test
    void settingTheBottomOfTwentyFourBeltsToUpMakesAllUp() {
        Map<Pos, Belt> belts = new HashMap<>();
        column(belts, 0, 0, 0, 23, VerticalDir.DOWN, 1);
        List<VerticalTower> towers = VerticalTowers.towersOf(belts);

        Map<Pos, Belt> changed = VerticalTowers.setDirection(towers, new Pos(0, 0, 0), VerticalDir.UP);

        assertThat(changed).hasSize(24);
        assertThat(changed.values()).allMatch(belt -> belt.direction() == VerticalDir.UP);
        List<VerticalTower> after = VerticalTowers.towersOf(changed);
        assertThat(after).hasSize(1);
        assertThat(after.get(0).size()).isEqualTo(24);
        assertThat(after.get(0).direction()).isEqualTo(VerticalDir.UP);
    }

    @Test
    void settingTheTopOfTwentyFourBeltsToDownMakesAllDown() {
        Map<Pos, Belt> belts = new HashMap<>();
        column(belts, 0, 0, 0, 23, VerticalDir.UP, 2);
        List<VerticalTower> towers = VerticalTowers.towersOf(belts);

        Map<Pos, Belt> changed = VerticalTowers.setDirection(towers, new Pos(0, 23, 0), VerticalDir.DOWN);

        assertThat(changed.values()).allMatch(belt -> belt.direction() == VerticalDir.DOWN && belt.level() == 2);
        assertThat(VerticalTowers.towersOf(changed).get(0).direction()).isEqualTo(VerticalDir.DOWN);
    }

    @Test
    void cascadeReachesBothEndsFromAMiddleBelt() {
        Map<Pos, Belt> belts = new HashMap<>();
        column(belts, 0, 0, 0, 23, VerticalDir.DOWN, 1);
        column(belts, 5, 5, 0, 1, VerticalDir.DOWN, 1);
        List<VerticalTower> towers = VerticalTowers.towersOf(belts);

        Map<Pos, Belt> changed = VerticalTowers.setDirection(towers, new Pos(0, 12, 0), VerticalDir.UP);

        assertThat(changed.get(new Pos(0, 0, 0)).direction()).isEqualTo(VerticalDir.UP);
        assertThat(changed.get(new Pos(0, 23, 0)).direction()).isEqualTo(VerticalDir.UP);
        assertThat(changed.get(new Pos(5, 0, 5)).direction()).isEqualTo(VerticalDir.DOWN);
        assertThat(changed.get(new Pos(5, 1, 5)).direction()).isEqualTo(VerticalDir.DOWN);
    }

    @Test
    void settingADirectionOutsideEveryTowerIsRejected() {
        Map<Pos, Belt> belts = new HashMap<>();
        column(belts, 0, 0, 0, 2, VerticalDir.UP, 1);
        List<VerticalTower> towers = VerticalTowers.towersOf(belts);

        assertThatThrownBy(() -> VerticalTowers.setDirection(towers, new Pos(0, 9, 0), VerticalDir.DOWN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void beltLevelMustBeOneToThree() {
        assertThat(new Belt(VerticalDir.UP, 1).level()).isEqualTo(1);
        assertThat(new Belt(VerticalDir.UP, 3).level()).isEqualTo(3);
        assertThatThrownBy(() -> new Belt(VerticalDir.UP, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Belt(VerticalDir.UP, 4)).isInstanceOf(IllegalArgumentException.class);
    }
}
