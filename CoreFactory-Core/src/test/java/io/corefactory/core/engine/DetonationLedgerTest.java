package io.corefactory.core.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DetonationLedgerTest {

    @Test
    void firstDetonationOfABlockIsClaimed() {
        DetonationLedger ledger = new DetonationLedger();

        assertThat(ledger.claim("world:1:64:1", 100L)).isTrue();
        assertThat(ledger.detonatedOn("world:1:64:1", 100L)).isTrue();
    }

    @Test
    void secondDetonationOfTheSameBlockOnTheSameTickIsRefused() {
        DetonationLedger ledger = new DetonationLedger();
        ledger.claim("world:1:64:1", 100L);

        assertThat(ledger.claim("world:1:64:1", 100L)).isFalse();
    }

    @Test
    void sameBlockDetonatesAgainOnALaterTick() {
        DetonationLedger ledger = new DetonationLedger();
        ledger.claim("world:1:64:1", 100L);

        assertThat(ledger.claim("world:1:64:1", 101L)).isTrue();
        assertThat(ledger.detonatedOn("world:1:64:1", 101L)).isTrue();
        assertThat(ledger.detonatedOn("world:1:64:1", 100L)).isFalse();
    }

    @Test
    void otherBlocksAreNotAffected() {
        DetonationLedger ledger = new DetonationLedger();
        ledger.claim("world:1:64:1", 100L);

        assertThat(ledger.claim("world:2:64:1", 100L)).isTrue();
        assertThat(ledger.detonatedOn("world:3:64:1", 100L)).isFalse();
    }

    @Test
    void oldEntriesDoNotBlockClaimsAfterTheMapGrows() {
        DetonationLedger ledger = new DetonationLedger();
        for (int i = 0; i < 300; i++) {
            ledger.claim("block:" + i, 10L);
        }

        assertThat(ledger.claim("block:0", 11L)).isTrue();
        assertThat(ledger.claim("block:0", 11L)).isFalse();
    }
}
