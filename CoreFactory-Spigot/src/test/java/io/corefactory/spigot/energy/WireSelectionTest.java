package io.corefactory.spigot.energy;

import io.corefactory.core.energy.DeviceKind;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.energy.Wire;
import io.corefactory.core.energy.WireCheck;
import io.corefactory.spigot.energy.WireSelection.Device;
import io.corefactory.spigot.energy.WireSelection.Verdict;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WireSelectionTest {

    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    private static final List<Wire> NONE = List.of();

    private static Device pylon(String id, int x) {
        return new Device(id, DeviceKind.PYLON, new Pos(x, 64, 0), WORLD, id);
    }

    private static Device generator(String id, int x) {
        return new Device(id, DeviceKind.GENERATOR, new Pos(x, 70, 0), WORLD, id);
    }

    private static Device accumulator(String id, int x) {
        return new Device(id, DeviceKind.ACCUMULATOR, new Pos(x, 66, 0), WORLD, id);
    }

    @Test
    void aFirstClickOnAWirableDeviceStartsAWire() {
        WireSelection selection = new WireSelection();
        WireSelection.Result result = selection.click(PLAYER, pylon("p", 0), false, 0, NONE);
        assertThat(result.verdict()).isEqualTo(Verdict.SELECTED);
        assertThat(selection.selected(PLAYER, 0)).contains(pylon("p", 0));
    }

    @Test
    void aFirstClickOnAnAccumulatorIsRefusedAsNotWirable() {
        WireSelection selection = new WireSelection();
        WireSelection.Result result = selection.click(PLAYER, accumulator("a", 0), false, 0, NONE);
        assertThat(result.verdict()).isEqualTo(Verdict.REFUSED);
        assertThat(result.check()).isEqualTo(WireCheck.NOT_WIRABLE);
        assertThat(selection.selected(PLAYER, 0)).isEmpty();
    }

    @Test
    void aSecondClickOnAGeneratorMakesTheWireAndClearsTheStart() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, pylon("p", 0), false, 0, NONE);
        WireSelection.Result result = selection.click(PLAYER, generator("g", 10), false, 5, NONE);
        assertThat(result.verdict()).isEqualTo(Verdict.MADE);
        assertThat(result.start()).isEqualTo(pylon("p", 0));
        assertThat(selection.selected(PLAYER, 5)).isEmpty();
    }

    @Test
    void theOrderOfTheTwoEndsDoesNotMatter() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, generator("g", 10), false, 0, NONE);
        assertThat(selection.click(PLAYER, pylon("p", 0), false, 1, NONE).verdict()).isEqualTo(Verdict.MADE);
    }

    @Test
    void aPylonToAPylonIsAllowedAndJoinsThem() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, pylon("p", 0), false, 0, NONE);
        WireSelection.Result result = selection.click(PLAYER, pylon("q", 4), false, 1, NONE);
        assertThat(result.verdict()).isEqualTo(Verdict.MADE);
        assertThat(selection.selected(PLAYER, 1)).isEmpty();
    }

    @Test
    void twoGeneratorsAreRefusedAndTheStartIsKept() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, generator("g", 0), false, 0, NONE);
        WireSelection.Result result = selection.click(PLAYER, generator("h", 4), false, 1, NONE);
        assertThat(result.verdict()).isEqualTo(Verdict.REFUSED);
        assertThat(result.check()).isEqualTo(WireCheck.SAME_KIND);
        assertThat(selection.selected(PLAYER, 1)).contains(generator("g", 0));
    }

    @Test
    void anAccumulatorNeverTakesAWireAndTheStartIsKept() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, pylon("p", 0), false, 0, NONE);
        WireSelection.Result result = selection.click(PLAYER, accumulator("a", 2), false, 1, NONE);
        assertThat(result.verdict()).isEqualTo(Verdict.REFUSED);
        assertThat(result.check()).isEqualTo(WireCheck.NOT_WIRABLE);
        assertThat(selection.selected(PLAYER, 1)).contains(pylon("p", 0));
    }

    @Test
    void aWireLongerThanTwentyFourBlocksIsRefusedAsTooLong() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, pylon("p", 0), false, 0, NONE);
        WireSelection.Result result = selection.click(PLAYER, generator("g", 25), false, 1, NONE);
        assertThat(result.verdict()).isEqualTo(Verdict.REFUSED);
        assertThat(result.check()).isEqualTo(WireCheck.TOO_LONG);
        assertThat(selection.selected(PLAYER, 1)).isPresent();
    }

    @Test
    void aClickOnTheStartItselfIsRefusedAsTheSameDevice() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, pylon("p", 0), false, 0, NONE);
        WireSelection.Result result = selection.click(PLAYER, pylon("p", 0), false, 1, NONE);
        assertThat(result.check()).isEqualTo(WireCheck.SAME_DEVICE);
        assertThat(selection.selected(PLAYER, 1)).isPresent();
    }

    @Test
    void aSneakingClickCancelsTheStart() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, pylon("p", 0), false, 0, NONE);
        WireSelection.Result cancelled = selection.click(PLAYER, pylon("p", 0), true, 1, NONE);
        assertThat(cancelled.verdict()).isEqualTo(Verdict.CANCELLED);
        assertThat(selection.selected(PLAYER, 1)).isEmpty();
    }

    @Test
    void aSneakingClickWithNoStartDoesNothing() {
        WireSelection selection = new WireSelection();
        assertThat(selection.click(PLAYER, pylon("p", 0), true, 0, NONE).verdict()).isEqualTo(Verdict.IDLE);
    }

    @Test
    void aStartTimesOutAfterSixtySecondsAndTheNextClickStartsAgain() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, pylon("p", 0), false, 0, NONE);
        assertThat(selection.selected(PLAYER, WireSelection.TIMEOUT_TICKS - 1)).isPresent();
        WireSelection.Result result = selection.click(PLAYER, generator("g", 10), false, WireSelection.TIMEOUT_TICKS, NONE);
        assertThat(result.verdict()).isEqualTo(Verdict.SELECTED);
        assertThat(result.start()).isEqualTo(generator("g", 10));
    }

    @Test
    void expireReturnsThePlayersWhoseStartTimedOut() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, pylon("p", 0), false, 0, NONE);
        selection.click(SECOND, pylon("q", 0), false, 500, NONE);
        assertThat(selection.expire(WireSelection.TIMEOUT_TICKS - 1)).isEmpty();
        assertThat(selection.expire(WireSelection.TIMEOUT_TICKS)).containsExactly(PLAYER);
        assertThat(selection.selected(SECOND, WireSelection.TIMEOUT_TICKS)).isPresent();
    }

    @Test
    void aPlayerWhoQuitLosesTheStart() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, pylon("p", 0), false, 0, NONE);
        selection.forget(PLAYER);
        assertThat(selection.selected(PLAYER, 1)).isEmpty();
    }

    @Test
    void aRemovedDeviceIsForgottenAsAStart() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, pylon("p", 0), false, 0, NONE);
        selection.forgetDevice("p");
        assertThat(selection.selected(PLAYER, 1)).isEmpty();
    }

    @Test
    void aWireToAnotherWorldIsRefusedAndTheStartKept() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, pylon("p", 0), false, 0, NONE);
        Device away = new Device("g", DeviceKind.GENERATOR, new Pos(5, 70, 0), OTHER, "g");
        assertThat(selection.click(PLAYER, away, false, 1, NONE).verdict()).isEqualTo(Verdict.OTHER_WORLD);
        assertThat(selection.selected(PLAYER, 1)).isPresent();
    }

    @Test
    void aSecondWireBetweenTheSameDevicesIsRefusedAsADuplicate() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, pylon("p", 0), false, 0, NONE);
        List<Wire> wired = List.of(new Wire("w1", "p", "g"));
        assertThat(selection.click(PLAYER, generator("g", 10), false, 1, wired).verdict()).isEqualTo(Verdict.DUPLICATE);
        assertThat(selection.selected(PLAYER, 1)).isPresent();
    }

    @Test
    void aWireAlreadyMadeTheOtherWayRoundIsAlsoADuplicate() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, generator("g", 10), false, 0, NONE);
        List<Wire> wired = List.of(new Wire("w1", "p", "g"));
        assertThat(selection.click(PLAYER, pylon("p", 0), false, 1, wired).verdict()).isEqualTo(Verdict.DUPLICATE);
    }

    @Test
    void theSelectionsOfTwoPlayersAreKeptApart() {
        WireSelection selection = new WireSelection();
        selection.click(PLAYER, pylon("p", 0), false, 0, NONE);
        selection.click(SECOND, generator("g", 3), false, 0, NONE);
        assertThat(selection.click(PLAYER, generator("g", 10), false, 1, NONE).verdict()).isEqualTo(Verdict.MADE);
        assertThat(selection.selected(SECOND, 1)).contains(generator("g", 3));
    }
}
