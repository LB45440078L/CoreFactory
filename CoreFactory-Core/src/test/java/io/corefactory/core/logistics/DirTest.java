package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static io.corefactory.core.logistics.Fixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DirTest {

    @ParameterizedTest
    @CsvSource({"NORTH,SOUTH", "EAST,WEST", "SOUTH,NORTH", "WEST,EAST"})
    void oppositeReversesTheDirection(Dir dir, Dir expected) {
        assertThat(dir.opposite()).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(Dir.class)
    void oppositeIsAnInvolution(Dir dir) {
        assertThat(dir.opposite().opposite()).isEqualTo(dir);
    }

    @ParameterizedTest
    @CsvSource({"NORTH,WEST", "WEST,SOUTH", "SOUTH,EAST", "EAST,NORTH"})
    void turnLeftIsCounterClockwiseFromAbove(Dir dir, Dir expected) {
        assertThat(dir.turnLeft()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"NORTH,EAST", "EAST,SOUTH", "SOUTH,WEST", "WEST,NORTH"})
    void turnRightIsClockwiseFromAbove(Dir dir, Dir expected) {
        assertThat(dir.turnRight()).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(Dir.class)
    void turningLeftThenRightIsIdentity(Dir dir) {
        assertThat(dir.turnLeft().turnRight()).isEqualTo(dir);
        assertThat(dir.turnRight().turnLeft()).isEqualTo(dir);
    }

    @ParameterizedTest
    @CsvSource({"NORTH,0,-1", "EAST,1,0", "SOUTH,0,1", "WEST,-1,0"})
    void stepMovesOneBlockAndKeepsTheHeight(Dir dir, int dx, int dz) {
        assertThat(dir.step(at(5, 5))).isEqualTo(new Pos(5 + dx, 64, 5 + dz));
    }

    @Test
    void stepKeepsTheYLevelOfTheStart() {
        assertThat(Dir.EAST.step(new Pos(0, 100, 0))).isEqualTo(new Pos(1, 100, 0));
    }

    @Test
    void cardinalYawsPlaceTheOppositeOfTheLook() {
        assertThat(Dir.fromPlacement(0)).isEqualTo(Dir.NORTH);
        assertThat(Dir.fromPlacement(90)).isEqualTo(Dir.EAST);
        assertThat(Dir.fromPlacement(180)).isEqualTo(Dir.SOUTH);
        assertThat(Dir.fromPlacement(270)).isEqualTo(Dir.WEST);
    }

    @Test
    void lookingEastPlacesABeltFlowingWest() {
        assertThat(Dir.fromPlacement(270)).isEqualTo(Dir.WEST);
    }

    @Test
    void lookingSouthPlacesABeltFlowingNorth() {
        assertThat(Dir.fromPlacement(0)).isEqualTo(Dir.NORTH);
    }

    @ParameterizedTest
    @CsvSource({
            "44.9,NORTH",
            "45,EAST",
            "134.9,EAST",
            "135,SOUTH",
            "224.9,SOUTH",
            "225,WEST",
            "314.9,WEST",
            "315,NORTH"
    })
    void boundaryYawsBelongToTheCardinalWithTheLargerYaw(double yaw, Dir expected) {
        assertThat(Dir.fromPlacement(yaw)).isEqualTo(expected);
    }

    @Test
    void negativeAndLargeYawsAreNormalisedToOneTurn() {
        assertThat(Dir.fromPlacement(-90)).isEqualTo(Dir.fromPlacement(270));
        assertThat(Dir.fromPlacement(450)).isEqualTo(Dir.fromPlacement(90));
        assertThat(Dir.fromPlacement(-360)).isEqualTo(Dir.NORTH);
    }

    @Test
    void aNonFiniteYawIsRefused() {
        assertThatThrownBy(() -> Dir.fromPlacement(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Dir.fromPlacement(Double.POSITIVE_INFINITY)).isInstanceOf(IllegalArgumentException.class);
    }
}
