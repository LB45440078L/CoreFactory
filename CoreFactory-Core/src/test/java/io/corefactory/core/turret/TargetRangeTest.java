package io.corefactory.core.turret;

import io.corefactory.core.turret.TargetRules.Candidate;
import io.corefactory.core.turret.TargetRules.Point;
import io.corefactory.core.turret.TargetRules.Verdict;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TargetRangeTest {

    private static final TurretSettings SETTINGS = TurretSettings.defaults();
    private static final Point CENTRE = new Point(0.0, 1.5, 0.0);

    private static Candidate zombie() {
        return new Candidate(TargetKind.MONSTER, false, false, false, false, false, false, false, 99.0, true, "ZOMBIE",
                TargetRules.Relation.NONE);
    }

    private static Verdict verdictAt(Point nearest) {
        return TargetRules.verdict(zombie(), SETTINGS, TargetPolicy.DEFAULT, CENTRE, nearest);
    }

    @Test
    void aTargetJustInsideTheSphereIsValid() {
        Point inside = new Point(SETTINGS.range() - 0.01, 1.5, 0.0);

        assertThat(verdictAt(inside)).isEqualTo(Verdict.VALID);
    }

    @Test
    void aTargetExactlyOnTheSphereCountsAsInside() {
        Point edge = new Point(SETTINGS.range(), 1.5, 0.0);

        assertThat(verdictAt(edge)).isEqualTo(Verdict.VALID);
    }

    @Test
    void aTargetJustOutsideTheSphereIsOutOfRange() {
        Point outside = new Point(SETTINGS.range() + 0.01, 1.5, 0.0);

        assertThat(verdictAt(outside)).isEqualTo(Verdict.OUT_OF_RANGE);
    }

    @Test
    void theRangeIsASphereNotABox() {
        double side = SETTINGS.range() / Math.sqrt(2.0);

        assertThat(TargetRules.inRange(new Point(0, 0, 0), new Point(side - 0.01, 0, side - 0.01), SETTINGS.range())).isTrue();
        assertThat(TargetRules.inRange(new Point(0, 0, 0), new Point(17.0, 0, 17.0), SETTINGS.range())).isFalse();
    }

    @Test
    void aBigMobWhoseNearEdgeIsInRangeCountsEvenIfItsFeetAreNot() {
        Point min = new Point(10.0, 0.0, -1.0);
        Point max = new Point(30.0, 3.0, 1.0);
        Point feet = new Point(30.0, 0.0, 0.0);
        Point nearest = TargetRules.nearestPoint(CENTRE, min, max);

        assertThat(TargetRules.inRange(CENTRE, feet, SETTINGS.range())).isFalse();
        assertThat(nearest).isEqualTo(new Point(10.0, 1.5, 0.0));
        assertThat(verdictAt(nearest)).isEqualTo(Verdict.VALID);
    }

    @Test
    void theNearestPointClampsEachCoordinateToTheBox() {
        Point min = new Point(-2.0, 0.0, -2.0);
        Point max = new Point(2.0, 4.0, 2.0);

        assertThat(TargetRules.nearestPoint(new Point(0.5, 1.0, 0.5), min, max)).isEqualTo(new Point(0.5, 1.0, 0.5));
        assertThat(TargetRules.nearestPoint(new Point(9.0, -3.0, 0.0), min, max)).isEqualTo(new Point(2.0, 0.0, 0.0));
    }

    @Test
    void theSphereDecidesRangeWhateverTheCandidatesOwnDistanceSays() {
        Candidate farAway = new Candidate(TargetKind.MONSTER, false, false, false, false, false, false, false, 500.0, true, "ZOMBIE",
                TargetRules.Relation.NONE);

        assertThat(TargetRules.verdict(farAway, SETTINGS, TargetPolicy.DEFAULT, CENTRE, new Point(5.0, 1.5, 0.0)))
                .isEqualTo(Verdict.VALID);
    }

    @Test
    void theArcStillLimitsASphereTarget() {
        Candidate behind = new Candidate(TargetKind.MONSTER, false, false, false, false, false, false, false, 5.0, false, "ZOMBIE",
                TargetRules.Relation.NONE);

        assertThat(TargetRules.verdict(behind, SETTINGS, TargetPolicy.DEFAULT, CENTRE, new Point(5.0, 1.5, 0.0)))
                .isEqualTo(Verdict.OUT_OF_ARC);
    }

    @Test
    void theRangeOfTheSettingsIsTheRadius() {
        TurretSettings longer = LaserSettings.defaults().asTurret(SETTINGS);

        assertThat(TargetRules.verdict(zombie(), longer, TargetPolicy.DEFAULT, CENTRE, new Point(28.0, 1.5, 0.0)))
                .isEqualTo(Verdict.VALID);
        assertThat(TargetRules.verdict(zombie(), SETTINGS, TargetPolicy.DEFAULT, CENTRE, new Point(28.0, 1.5, 0.0)))
                .isEqualTo(Verdict.OUT_OF_RANGE);
    }
}
