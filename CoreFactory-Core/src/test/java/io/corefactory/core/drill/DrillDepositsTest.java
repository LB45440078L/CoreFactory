package io.corefactory.core.drill;

import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DepositSettings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DrillDepositsTest {

    private static final String GALENA = "galena_ore";
    private static final String CRUDE = DrillRules.DEPOSIT;
    private static final Set<String> KNOWN = Set.of(GALENA, CRUDE, "coal_ore");

    /** A level 1 drill at (0, 60, 0): its square is x -5 to 4 and z -5 to 4. */
    private static final DrillBox SQUARE = DrillBox.around(0, 60, 0, DrillTable.level(1));

    private static Deposit deposit(long id, int x, int z, int radius, String ore, long remaining) {
        return new Deposit(id, "world", x, z, radius, ore, Math.max(remaining, 1_000), remaining);
    }

    @Test
    void theDepositWithTheLargestShareIsChosen() {
        Deposit small = deposit(1, 0, 0, 5, GALENA, 100);
        Deposit large = deposit(2, 0, 0, 5, GALENA, 300);
        assertThat(DrillDeposits.choose(List.of(small, large))).contains(large);
    }

    @Test
    void theShareCountsNotTheAmountLeft() {
        // 300000 of 500000 is a share of 0.6; 900 of 1000 is 0.9, so the smaller amount is the larger share.
        Deposit rich = new Deposit(1, "world", 0, 0, 5, GALENA, 500_000, 300_000);
        Deposit poor = new Deposit(2, "world", 0, 0, 5, GALENA, 1_000, 900);
        assertThat(DrillDeposits.choose(List.of(rich, poor))).contains(poor);
    }

    @Test
    void aTieOfShareGoesToTheLowerId() {
        Deposit five = new Deposit(5, "world", 0, 0, 5, GALENA, 1_000, 500);
        Deposit three = new Deposit(3, "world", 0, 0, 5, GALENA, 400, 200);
        assertThat(DrillDeposits.choose(List.of(five, three)).orElseThrow().id()).isEqualTo(3L);
        assertThat(DrillDeposits.choose(List.of(three, five)).orElseThrow().id()).isEqualTo(3L);
    }

    @Test
    void aDepositWithNothingLeftIsNeverChosen() {
        assertThat(DrillDeposits.choose(List.of(deposit(1, 0, 0, 5, GALENA, 0)))).isEmpty();
        Deposit empty = deposit(1, 0, 0, 5, GALENA, 0);
        Deposit few = deposit(2, 0, 0, 5, GALENA, 4);
        assertThat(DrillDeposits.choose(List.of(empty, few)).orElseThrow().id()).isEqualTo(2L);
        assertThat(DrillDeposits.choose(List.of())).isEmpty();
    }

    @Test
    void aDrillMovesToTheDepositWithTheLargestShareWhenAnotherOvertakesIt() {
        Deposit first = new Deposit(1, "world", 0, 0, 5, GALENA, 1_000, 900);
        Deposit second = new Deposit(2, "world", 0, 0, 5, GALENA, 1_000, 500);
        assertThat(DrillDeposits.choose(List.of(first, second))).contains(first);
        assertThat(DrillDeposits.choose(List.of(first.withRemaining(400), second))).contains(second);
    }

    @Test
    void aPetroleumDrillTakesOnlyCrudeDepositsAndAnOreDrillTakesEveryOtherOre() {
        assertThat(DrillDeposits.kindTakes(DrillKind.PETROLEUM, CRUDE)).isTrue();
        assertThat(DrillDeposits.kindTakes(DrillKind.PETROLEUM, GALENA)).isFalse();
        assertThat(DrillDeposits.kindTakes(DrillKind.ORE, CRUDE)).isFalse();
        assertThat(DrillDeposits.kindTakes(DrillKind.ORE, GALENA)).isTrue();
        assertThat(DrillDeposits.kindTakes(DrillKind.ORE, "coal_ore")).isTrue();
    }

    @Test
    void inTheSquareOnlyDepositsOfTheKindAndAKnownOreAreListed() {
        Deposit crude = deposit(1, 0, 0, 3, CRUDE, 50);
        Deposit galena = deposit(2, 1, 1, 3, GALENA, 50);
        Deposit unknown = deposit(3, 1, 1, 3, "moon_ore", 50);
        List<Deposit> ore = DrillDeposits.inSquare(List.of(crude, galena, unknown), SQUARE, DrillKind.ORE, KNOWN::contains);
        assertThat(ore).containsExactly(galena);
        List<Deposit> petroleum = DrillDeposits.inSquare(List.of(crude, galena, unknown), SQUARE, DrillKind.PETROLEUM,
                KNOWN::contains);
        assertThat(petroleum).containsExactly(crude);
    }

    @Test
    void aDepositTouchingTheEdgeOfTheSquareIsInRangeAndOneOutsideIsNot() {
        // The square's east edge is column 4. A radius of 5 from column 9 reaches column 4 exactly.
        Deposit touching = deposit(1, 9, 0, 5, GALENA, 50);
        Deposit outside = deposit(2, 10, 0, 5, GALENA, 50);
        assertThat(DrillDeposits.touches(touching, SQUARE)).isTrue();
        assertThat(DrillDeposits.touches(outside, SQUARE)).isFalse();
        assertThat(DrillDeposits.inSquare(List.of(touching, outside), SQUARE, DrillKind.ORE, KNOWN::contains))
                .containsExactly(touching);
    }

    @Test
    void aDepositCountsByItsNearestCornerOnTheDiagonal() {
        // The nearest block column of the square to (9, 9) is (4, 4): 5 blocks away on each axis, so 50 in all.
        assertThat(DrillDeposits.touches(deposit(1, 9, 9, 7, GALENA, 1), SQUARE)).isFalse();
        assertThat(DrillDeposits.touches(deposit(2, 9, 9, 8, GALENA, 1), SQUARE)).isTrue();
    }

    @Test
    void aDepositInsideTheSquareTouchesIt() {
        assertThat(DrillDeposits.touches(deposit(1, 0, 0, 1, GALENA, 1), SQUARE)).isTrue();
    }

    @Test
    void theSelectionRemovesTheOresTheDrillIgnores() {
        Deposit galena = deposit(1, 0, 0, 3, GALENA, 50);
        Deposit crude = deposit(2, 1, 1, 3, CRUDE, 50);
        DrillSelection ignoring = new DrillSelection(List.of(GALENA));
        assertThat(DrillDeposits.mineable(List.of(galena, crude), ignoring)).containsExactly(crude);
        assertThat(DrillDeposits.mineable(List.of(galena, crude), new DrillSelection())).containsExactly(galena, crude);
    }

    @Test
    void theSearchReachCoversTheLargestRadiusPlusTheSideOfTheSquare() {
        assertThat(DrillDeposits.reach(DrillTable.level(5))).isEqualTo(DepositSettings.MAX_RADIUS + 40);
        assertThat(DrillDeposits.reach(DrillTable.level(1))).isEqualTo(DepositSettings.MAX_RADIUS + 10);
    }

    @Test
    void theSearchReachFindsADepositOfTheLargestRadiusAtTheFarEdgeOfTheLargestSquare() {
        DrillSpec largest = DrillTable.level(5);
        DrillBox box = DrillBox.around(0, 60, 0, largest);
        int reach = DrillDeposits.reach(largest);
        // The square runs from x -20 to x +19. A deposit of the largest radius touches it from either side at 256 blocks out.
        Deposit east = deposit(1, box.maxX() + DepositSettings.MAX_RADIUS, 0, DepositSettings.MAX_RADIUS, GALENA, 50);
        Deposit west = deposit(2, box.minX() - DepositSettings.MAX_RADIUS, 0, DepositSettings.MAX_RADIUS, GALENA, 50);
        assertThat(DrillDeposits.touches(east, box)).isTrue();
        assertThat(DrillDeposits.touches(west, box)).isTrue();
        assertThat(Math.abs(east.centreX())).isLessThanOrEqualTo(reach);
        assertThat(Math.abs(west.centreX())).isLessThanOrEqualTo(reach);
    }

    @Test
    void aDrillKeepsItsOldDepositWhileItsBatchWaitsForAnOutput() {
        Deposit old = deposit(1, 0, 0, 5, GALENA, 100);
        Deposit chosen = deposit(2, 0, 0, 5, GALENA, 300);
        assertThat(DrillDeposits.afterBatch(Optional.of(chosen), old, 4)).contains(old);
    }

    @Test
    void aDrillMovesOnOnceItsBatchIsInTheOutputs() {
        Deposit old = deposit(1, 0, 0, 5, GALENA, 100);
        Deposit chosen = deposit(2, 0, 0, 5, GALENA, 300);
        assertThat(DrillDeposits.afterBatch(Optional.of(chosen), old, 0)).contains(chosen);
        assertThat(DrillDeposits.afterBatch(Optional.empty(), old, 0)).isEmpty();
    }

    @Test
    void aDrillMovesOnWhenItsOldDepositIsGone() {
        Deposit chosen = deposit(2, 0, 0, 5, GALENA, 300);
        assertThat(DrillDeposits.afterBatch(Optional.of(chosen), null, 4)).contains(chosen);
    }

    @Test
    void theChosenDepositIsEmptyWhenTheSquareHoldsNoPickableDeposit() {
        Optional<Deposit> none = DrillDeposits.choose(DrillDeposits.mineable(
                DrillDeposits.inSquare(List.of(deposit(1, 50, 50, 3, GALENA, 9)), SQUARE, DrillKind.ORE, KNOWN::contains),
                new DrillSelection()));
        assertThat(none).isEmpty();
    }
}
