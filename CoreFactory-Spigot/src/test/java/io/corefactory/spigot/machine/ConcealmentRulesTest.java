package io.corefactory.spigot.machine;

import io.corefactory.spigot.machine.ConcealmentRules.Decision;
import io.corefactory.spigot.machine.ConcealmentRules.Viewer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The viewer decision of the barrel concealment, on plain values. Nothing here starts a server. */
class ConcealmentRulesTest {

    private static final UUID WORLD = UUID.randomUUID();
    private static final UUID OTHER_WORLD = UUID.randomUUID();
    private static final UUID ANNA = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CAROL = UUID.randomUUID();
    private static final UUID DAN = UUID.randomUUID();
    private static final double RANGE = 48.0;

    /** A player in the machine's world, at this distance in blocks. */
    private static Viewer at(UUID id, double blocks) {
        return new Viewer(id, WORLD, blocks * blocks);
    }

    @Test
    void withNobodyOnlineNothingChanges() {
        Decision decision = ConcealmentRules.decide(WORLD, RANGE, Set.of(), List.of());

        assertThat(decision.conceal()).isEmpty();
        assertThat(decision.refresh()).isEmpty();
        assertThat(decision.restore()).isEmpty();
    }

    @Test
    void aPlayerInRangeIsConcealedOnce() {
        Decision decision = ConcealmentRules.decide(WORLD, RANGE, Set.of(), List.of(at(ANNA, 10)));

        assertThat(decision.conceal()).containsExactly(ANNA);
        assertThat(decision.refresh()).isEmpty();
        assertThat(decision.restore()).isEmpty();
    }

    @Test
    void aConcealedPlayerInRangeIsRefreshedNotConcealedAgain() {
        Decision decision = ConcealmentRules.decide(WORLD, RANGE, Set.of(ANNA), List.of(at(ANNA, 10)));

        assertThat(decision.conceal()).isEmpty();
        assertThat(decision.refresh()).containsExactly(ANNA);
        assertThat(decision.restore()).isEmpty();
    }

    @Test
    void aConcealedPlayerWhoLeavesRangeIsRestored() {
        Decision decision = ConcealmentRules.decide(WORLD, RANGE, Set.of(ANNA), List.of(at(ANNA, 60)));

        assertThat(decision.conceal()).isEmpty();
        assertThat(decision.refresh()).isEmpty();
        assertThat(decision.restore()).containsExactly(ANNA);
    }

    @Test
    void aPlayerOutOfRangeIsNotConcealed() {
        Decision decision = ConcealmentRules.decide(WORLD, RANGE, Set.of(), List.of(at(BOB, 60)));

        assertThat(decision.conceal()).isEmpty();
        assertThat(decision.refresh()).isEmpty();
        assertThat(decision.restore()).isEmpty();
    }

    @Test
    void theRangeIsInclusive() {
        Decision onTheEdge = ConcealmentRules.decide(WORLD, RANGE, Set.of(), List.of(at(ANNA, 48)));
        Decision justOutside = ConcealmentRules.decide(WORLD, RANGE, Set.of(), List.of(at(BOB, 48.01)));

        assertThat(onTheEdge.conceal()).containsExactly(ANNA);
        assertThat(justOutside.conceal()).isEmpty();
    }

    @Test
    void aPlayerInAnotherWorldIsNeverInRange() {
        Viewer elsewhere = new Viewer(BOB, OTHER_WORLD, 4.0);

        Decision fresh = ConcealmentRules.decide(WORLD, RANGE, Set.of(), List.of(elsewhere));
        Decision leftBehind = ConcealmentRules.decide(WORLD, RANGE, Set.of(BOB), List.of(elsewhere));

        assertThat(fresh.conceal()).isEmpty();
        assertThat(leftBehind.conceal()).isEmpty();
        assertThat(leftBehind.refresh()).isEmpty();
        assertThat(leftBehind.restore()).containsExactly(BOB);
    }

    @Test
    void aConcealedPlayerWhoIsOfflineIsRestored() {
        Decision decision = ConcealmentRules.decide(WORLD, RANGE, Set.of(CAROL), List.of());

        assertThat(decision.restore()).containsExactly(CAROL);
        assertThat(decision.refresh()).isEmpty();
    }

    @Test
    void aMixedWorldIsDecidedPlayerByPlayer() {
        List<Viewer> online = List.of(at(ANNA, 10), at(BOB, 60), at(CAROL, 20), at(DAN, 70));

        Decision decision = ConcealmentRules.decide(WORLD, RANGE, Set.of(ANNA, BOB), online);

        assertThat(decision.refresh()).containsExactly(ANNA);
        assertThat(decision.restore()).containsExactly(BOB);
        assertThat(decision.conceal()).containsExactly(CAROL);
    }

    @Test
    void aNegativeRangePutsNobodyInRange() {
        Decision decision = ConcealmentRules.decide(WORLD, -1.0, Set.of(ANNA), List.of(at(ANNA, 0)));

        assertThat(decision.conceal()).isEmpty();
        assertThat(decision.refresh()).isEmpty();
        assertThat(decision.restore()).containsExactly(ANNA);
    }
}
