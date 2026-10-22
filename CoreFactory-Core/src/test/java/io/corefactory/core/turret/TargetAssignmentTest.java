package io.corefactory.core.turret;

import io.corefactory.core.turret.TargetAssignment.TurretSlot;
import io.corefactory.core.turret.TargetRules.Point;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TargetAssignmentTest {

    private static final UUID NEAR = new UUID(0, 1);
    private static final UUID MIDDLE = new UUID(0, 2);
    private static final UUID FAR = new UUID(0, 3);

    private static TurretSlot slot(String turret, int cannons, List<UUID> claimed, List<UUID> candidates) {
        return new TurretSlot(turret, cannons, claimed, candidates);
    }

    @Test
    void twoTurretsAndTwoMobsTakeDifferentMobs() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                slot("a", 1, List.of(), List.of(NEAR, MIDDLE)),
                slot("b", 1, List.of(), List.of(NEAR, MIDDLE))));

        assertThat(answer.get("a")).containsExactly(NEAR);
        assertThat(answer.get("b")).containsExactly(MIDDLE);
    }

    @Test
    void theThirdTurretSharesWhenTwoMobsAreAllClaimed() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                slot("a", 1, List.of(), List.of(NEAR, MIDDLE)),
                slot("b", 1, List.of(), List.of(NEAR, MIDDLE)),
                slot("c", 1, List.of(), List.of(NEAR, MIDDLE))));

        assertThat(answer.get("a")).containsExactly(NEAR);
        assertThat(answer.get("b")).containsExactly(MIDDLE);
        assertThat(answer.get("c")).containsExactly(NEAR);
    }

    @Test
    void aSharingTurretTakesTheMobFewestTurretsHold() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                slot("a", 1, List.of(), List.of(NEAR, MIDDLE)),
                slot("b", 1, List.of(), List.of(NEAR, MIDDLE)),
                slot("c", 1, List.of(), List.of(NEAR, MIDDLE)),
                slot("d", 1, List.of(), List.of(NEAR, MIDDLE))));

        assertThat(answer.get("c")).containsExactly(NEAR);
        assertThat(answer.get("d")).containsExactly(MIDDLE);
    }

    @Test
    void aStickyValidClaimIsKeptOverANearerUnclaimedMob() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                slot("a", 1, List.of(MIDDLE), List.of(NEAR, MIDDLE))));

        assertThat(answer.get("a")).containsExactly(MIDDLE);
    }

    @Test
    void aClaimThatIsNoLongerACandidateIsReleased() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                slot("a", 1, List.of(FAR), List.of(NEAR, MIDDLE))));

        assertThat(answer.get("a")).containsExactly(NEAR);
    }

    @Test
    void aClaimThatIsNoLongerACandidateLeavesNothingWhenNoOtherMobIsInReach() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(slot("a", 1, List.of(FAR), List.of())));

        assertThat(answer.get("a")).isEmpty();
    }

    @Test
    void aTurretWithTwoCannonsTakesTwoDifferentMobsWhenThereAreTwo() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                slot("a", 2, List.of(), List.of(NEAR, MIDDLE))));

        assertThat(answer.get("a")).containsExactly(NEAR, MIDDLE);
    }

    @Test
    void aStickyClaimStaysFirstAndTheFreeCannonTakesTheNearestOtherMob() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                slot("a", 2, List.of(MIDDLE), List.of(NEAR, MIDDLE))));

        assertThat(answer.get("a")).containsExactly(MIDDLE, NEAR);
    }

    @Test
    void aTurretWithOneCannonNeverTakesASecondTarget() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                slot("a", 1, List.of(), List.of(NEAR, MIDDLE))));

        assertThat(answer.get("a")).containsExactly(NEAR);
    }

    @Test
    void aTurretWithTwoCannonsAndOneMobTakesItOnce() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                slot("a", 2, List.of(), List.of(NEAR))));

        assertThat(answer.get("a")).containsExactly(NEAR);
    }

    @Test
    void aTurretWithNoCandidateGetsNothing() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                slot("a", 2, List.of(), List.of()),
                slot("b", 1, List.of(), List.of(NEAR))));

        assertThat(answer.get("a")).isEmpty();
        assertThat(answer.get("b")).containsExactly(NEAR);
    }

    @Test
    void aTurretWithNoCannonsGetsNothing() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                slot("a", 0, List.of(), List.of(NEAR))));

        assertThat(answer.get("a")).isEmpty();
    }

    @Test
    void tiesAreBrokenByTurretIdWhateverTheInputOrder() {
        TurretSlot a = slot("a", 1, List.of(), List.of(NEAR, MIDDLE));
        TurretSlot b = slot("b", 1, List.of(), List.of(NEAR, MIDDLE));

        Map<String, List<UUID>> forward = TargetAssignment.assign(List.of(a, b));
        Map<String, List<UUID>> backward = TargetAssignment.assign(List.of(b, a));

        assertThat(forward).isEqualTo(backward);
        assertThat(forward.get("a")).containsExactly(NEAR);
        assertThat(forward.get("b")).containsExactly(MIDDLE);
    }

    @Test
    void aMobHeldByTwoTurretsStaysWithTheLowerIdAsASticky() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                slot("b", 1, List.of(NEAR), List.of(NEAR, MIDDLE)),
                slot("a", 1, List.of(NEAR), List.of(NEAR, MIDDLE))));

        assertThat(answer.get("a")).containsExactly(NEAR);
        assertThat(answer.get("b")).containsExactly(MIDDLE);
    }

    @Test
    void theAnswerHasOneEntryPerTurretInIdOrder() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                slot("z", 1, List.of(), List.of(NEAR)),
                slot("m", 1, List.of(), List.of())));

        assertThat(answer.keySet()).containsExactly("m", "z");
    }

    @Test
    void noTurretsGiveAnEmptyAnswer() {
        assertThat(TargetAssignment.assign(List.of())).isEmpty();
    }

    @Test
    void aSecondCannonPrefersAMobWithinTheSpacingOfItsFirstTargetOverANearerMobToTheTurret() {
        Map<UUID, Point> places = Map.of(NEAR, new Point(0, 0, 0), FAR, new Point(10, 0, 0), MIDDLE, new Point(3, 0, 0));

        Map<String, List<UUID>> answer = TargetAssignment.assign(
                List.of(slot("a", 2, List.of(), List.of(NEAR, FAR, MIDDLE))), 6.0, places::get);

        assertThat(answer.get("a")).containsExactly(NEAR, MIDDLE);
    }

    @Test
    void aMobExactlyAtTheSpacingCountsAsWithinIt() {
        Map<UUID, Point> places = Map.of(NEAR, new Point(0, 0, 0), FAR, new Point(10, 0, 0), MIDDLE, new Point(6, 0, 0));

        Map<String, List<UUID>> answer = TargetAssignment.assign(
                List.of(slot("a", 2, List.of(), List.of(NEAR, FAR, MIDDLE))), 6.0, places::get);

        assertThat(answer.get("a")).containsExactly(NEAR, MIDDLE);
    }

    @Test
    void withNoMobWithinTheSpacingASecondCannonTakesTheNearestUnclaimed() {
        Map<UUID, Point> places = Map.of(NEAR, new Point(0, 0, 0), MIDDLE, new Point(10, 0, 0), FAR, new Point(20, 0, 0));

        Map<String, List<UUID>> answer = TargetAssignment.assign(
                List.of(slot("a", 2, List.of(), List.of(NEAR, MIDDLE, FAR))), 6.0, places::get);

        assertThat(answer.get("a")).containsExactly(NEAR, MIDDLE);
    }

    @Test
    void twoTurretsWithTwoCannonsEachTakeFourDifferentMobsWhenThereAreFour() {
        UUID fourth = new UUID(0, 4);
        Map<UUID, Point> places = Map.of(NEAR, new Point(0, 0, 0), MIDDLE, new Point(1, 0, 0), FAR, new Point(4, 0, 0),
                fourth, new Point(30, 0, 0));
        List<UUID> mobs = List.of(NEAR, MIDDLE, FAR, fourth);

        Map<String, List<UUID>> answer = TargetAssignment.assign(
                List.of(slot("a", 2, List.of(), mobs), slot("b", 2, List.of(), mobs)), 6.0, places::get);

        assertThat(answer.get("a")).containsExactly(NEAR, FAR);
        assertThat(answer.get("b")).containsExactly(MIDDLE, fourth);
    }

    @Test
    void aSecondCannonDoesNotTakeAMobAnotherTurretsFirstCannonNeeds() {
        Map<UUID, Point> places = Map.of(NEAR, new Point(0, 0, 0), MIDDLE, new Point(2, 0, 0));

        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                slot("a", 2, List.of(), List.of(NEAR, MIDDLE)),
                slot("b", 1, List.of(), List.of(MIDDLE))), 6.0, places::get);

        assertThat(answer.get("b")).containsExactly(MIDDLE);
        assertThat(answer.get("a")).containsExactly(NEAR, MIDDLE);
    }

    @Test
    void withoutPlacesTheSpacingIsNotPreferred() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(
                List.of(slot("a", 2, List.of(), List.of(NEAR, FAR, MIDDLE))), 6.0, id -> null);

        assertThat(answer.get("a")).containsExactly(NEAR, FAR);
    }

    @Test
    void aReservedMobIsNotGivenWhileAnotherCandidateIsFree() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(
                List.of(slot("a", 1, List.of(), List.of(NEAR, MIDDLE))), Set.of(NEAR), 0.0, id -> null);

        assertThat(answer.get("a")).containsExactly(MIDDLE);
    }

    @Test
    void aReservedMobIsGivenWhenItIsTheOnlyCandidate() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(
                List.of(slot("a", 1, List.of(), List.of(NEAR))), Set.of(NEAR), 0.0, id -> null);

        assertThat(answer.get("a")).containsExactly(NEAR);
    }

    @Test
    void aCannonLosesAClaimOnAReservedMobForAFreeOne() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(
                List.of(slot("a", 1, List.of(NEAR), List.of(NEAR, MIDDLE))), Set.of(NEAR), 0.0, id -> null);

        assertThat(answer.get("a")).containsExactly(MIDDLE);
    }

    @Test
    void aSecondCannonDoesNotTakeAReservedMobWithinTheSpacing() {
        Map<UUID, Point> places = Map.of(NEAR, new Point(0, 0, 0), MIDDLE, new Point(2, 0, 0), FAR, new Point(4, 0, 0));

        Map<String, List<UUID>> answer = TargetAssignment.assign(
                List.of(slot("a", 2, List.of(), List.of(NEAR, MIDDLE, FAR))), Set.of(MIDDLE), 6.0, places::get);

        assertThat(answer.get("a")).containsExactly(NEAR, FAR);
    }
}
