package io.corefactory.core.logistics;

import io.corefactory.core.engine.ContainerAccess;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PickRuleTest {

    private static final Item STONE = Item.material("STONE");

    @Test
    void aPickerTakesTheItemAnOpenSourceOffers() {
        assertThat(PickRule.decide(STONE, true)).isEqualTo(PickRule.Verdict.TAKE);
    }

    @Test
    void anEmptySourceYieldsNothing() {
        assertThat(PickRule.decide(null, true)).isEqualTo(PickRule.Verdict.EMPTY);
        assertThat(PickRule.decide(null, false)).isEqualTo(PickRule.Verdict.EMPTY);
    }

    @Test
    void aStrangersPickerCannotTakeFromAClaimedContainer() {
        boolean permitted = ContainerAccess.permits(true, true, false);

        assertThat(PickRule.decide(STONE, permitted)).isEqualTo(PickRule.Verdict.CLAIMED);
    }

    @Test
    void aPickerWithNoOwnerTakesNothingFromAClaimedContainer() {
        boolean permitted = ContainerAccess.permits(true, false, false);

        assertThat(PickRule.decide(STONE, permitted)).isEqualTo(PickRule.Verdict.CLAIMED);
    }

    @Test
    void aTrustedOwnersPickerTakesFromTheClaimedContainer() {
        boolean permitted = ContainerAccess.permits(true, true, true);

        assertThat(PickRule.decide(STONE, permitted)).isEqualTo(PickRule.Verdict.TAKE);
    }

    @Test
    void anUnclaimedContainerIsOpenToAPickerWithNoOwner() {
        boolean permitted = ContainerAccess.permits(false, false, false);

        assertThat(PickRule.decide(STONE, permitted)).isEqualTo(PickRule.Verdict.TAKE);
    }
}
