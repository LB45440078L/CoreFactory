package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Checks the puffs of a shell's trail, the crossing of a periodic interval, and the whistle's timing. */
class ArtilleryTrailTest {

    private static final double EPS = 1e-9;

    private static final Position ORIGIN = new Position(0.0, 0.0, 0.0);

    @Test
    void puffsAreSpacedEvenlyAndEndOnTheShell() {
        List<Position> puffs = ArtilleryTrail.puffs(ORIGIN, new Position(6.0, 0.0, 0.0), 3);

        assertThat(puffs).hasSize(3);
        assertThat(puffs.get(0).x()).isCloseTo(2.0, within(EPS));
        assertThat(puffs.get(1).x()).isCloseTo(4.0, within(EPS));
        assertThat(puffs.get(2).x()).isCloseTo(6.0, within(EPS));
    }

    @Test
    void aFastShellLeavesNoMorePuffsThanTheCap() {
        List<Position> puffs = ArtilleryTrail.puffs(ORIGIN, new Position(30.0, 0.0, 0.0), 3);

        assertThat(puffs).hasSize(3);
        assertThat(puffs.get(0).x()).isCloseTo(10.0, within(EPS));
        assertThat(puffs.get(2).x()).isCloseTo(30.0, within(EPS));
    }

    @Test
    void aShellThatDidNotMoveOrAShellWithNoPuffsLeavesNoTrail() {
        assertThat(ArtilleryTrail.puffs(ORIGIN, ORIGIN, 3)).isEmpty();
        assertThat(ArtilleryTrail.puffs(ORIGIN, new Position(6.0, 0.0, 0.0), 0)).isEmpty();
    }

    @Test
    void thePuffsFollowTheDiagonalOfTheStep() {
        List<Position> puffs = ArtilleryTrail.puffs(ORIGIN, new Position(3.0, 4.0, 0.0), 1);

        assertThat(puffs).hasSize(1);
        assertThat(puffs.get(0).x()).isCloseTo(3.0, within(EPS));
        assertThat(puffs.get(0).y()).isCloseTo(4.0, within(EPS));
    }

    @Test
    void anIntervalIsCrossedOnceWhenAStepEndsOnOrPastIt() {
        assertThat(ArtilleryTrail.crossed(30, 40, 40)).isTrue();
        assertThat(ArtilleryTrail.crossed(70, 80, 40)).isTrue();
        assertThat(ArtilleryTrail.crossed(0, 10, 40)).isFalse();
        assertThat(ArtilleryTrail.crossed(40, 50, 40)).isFalse();
    }

    @Test
    void aWhistleSoundsEveryIntervalAfterTheLaunch() {
        assertThat(ArtilleryTrail.whistleDue(0, 40)).isFalse();
        assertThat(ArtilleryTrail.whistleDue(40, 40)).isTrue();
        assertThat(ArtilleryTrail.whistleDue(41, 40)).isFalse();
        assertThat(ArtilleryTrail.whistleDue(80, 40)).isTrue();
    }
}
