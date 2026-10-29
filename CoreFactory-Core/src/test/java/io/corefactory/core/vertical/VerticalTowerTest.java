package io.corefactory.core.vertical;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Dir;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerticalTowerTest {

    /** A five belt UP tower at x 0, z 0, y 0 to 4. */
    private static VerticalTower upTower() {
        Map<Pos, Belt> belts = new HashMap<>();
        for (int y = 0; y <= 4; y++) {
            belts.put(new Pos(0, y, 0), new Belt(VerticalDir.UP, 1));
        }
        return VerticalTowers.towersOf(belts).get(0);
    }

    /** A five belt DOWN tower at x 0, z 0, y 0 to 4. */
    private static VerticalTower downTower() {
        Map<Pos, Belt> belts = new HashMap<>();
        for (int y = 0; y <= 4; y++) {
            belts.put(new Pos(0, y, 0), new Belt(VerticalDir.DOWN, 1));
        }
        return VerticalTowers.towersOf(belts).get(0);
    }

    @Test
    void upInputEndCellIsBelowTheBottom() {
        assertThat(upTower().inputEndCell()).isEqualTo(new Pos(0, -1, 0));
    }

    @Test
    void downInputEndCellIsAboveTheTop() {
        assertThat(downTower().inputEndCell()).isEqualTo(new Pos(0, 5, 0));
    }

    @Test
    void upOutputCellIsAboveTheTop() {
        VerticalTower tower = upTower();

        assertThat(tower.outputEnd()).isEqualTo(new Pos(0, 4, 0));
        assertThat(tower.outputCell()).isEqualTo(new Pos(0, 5, 0));
    }

    @Test
    void downOutputCellIsBelowTheBottom() {
        VerticalTower tower = downTower();

        assertThat(tower.outputEnd()).isEqualTo(new Pos(0, 0, 0));
        assertThat(tower.outputCell()).isEqualTo(new Pos(0, -1, 0));
    }

    @Test
    void theInputOfAnUpTowerIsAtTheBottomAndOfADownTowerAtTheTop() {
        VerticalTower up = upTower();
        VerticalTower down = downTower();

        assertThat(up.carryBlock(new Pos(0, 0, 0))).isZero();
        assertThat(up.carryBlock(new Pos(0, 4, 0))).isEqualTo(4);
        assertThat(down.carryBlock(new Pos(0, 4, 0))).isZero();
        assertThat(down.carryBlock(new Pos(0, 0, 0))).isEqualTo(4);
    }

    @ParameterizedTest
    @EnumSource(Dir.class)
    void aMiddleBeltAcceptsFromEveryHorizontalSide(Dir side) {
        VerticalTower tower = upTower();
        Pos middle = new Pos(0, 2, 0);

        assertThat(tower.acceptsFrom(middle, side)).isTrue();
        assertThat(tower.inputCell(middle, side)).isEqualTo(side.step(middle));
    }

    @Test
    void anEndBeltAlsoAcceptsFromItsSides() {
        VerticalTower tower = downTower();

        assertThat(tower.acceptsFrom(new Pos(0, 4, 0), Dir.WEST)).isTrue();
        assertThat(tower.inputCell(new Pos(0, 4, 0), Dir.WEST)).isEqualTo(new Pos(-1, 4, 0));
    }

    @Test
    void aPositionOutsideTheTowerDoesNotAcceptAndHasNoInputCell() {
        VerticalTower tower = upTower();
        Pos outside = new Pos(0, 9, 0);

        assertThat(tower.acceptsFrom(outside, Dir.NORTH)).isFalse();
        assertThatThrownBy(() -> tower.inputCell(outside, Dir.NORTH)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theLevelOfAnUpTowerIsTheLevelOfItsTopBelt() {
        Map<Pos, Belt> belts = new HashMap<>();
        belts.put(new Pos(0, 0, 0), new Belt(VerticalDir.UP, 1));
        belts.put(new Pos(0, 1, 0), new Belt(VerticalDir.UP, 2));

        VerticalTower tower = VerticalTowers.towersOf(belts).get(0);

        assertThat(tower.level()).isEqualTo(2);
        assertThat(tower.outputEnd()).isEqualTo(new Pos(0, 1, 0));
    }
}
