package io.corefactory.paper.machine;

import io.corefactory.core.process.ItemRef;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How a slot's stack is identified as an item. A component item is identified by the component id stored on it, so a stack that was
 * renamed, given lore or merged still counts as that item. A vanilla item is identified by its material and only while it is plain.
 * The live stack is a server object, so the decision is tested on its outcome: whether the stack is plain and what it reads as.
 */
class StorageSlotIdentityTest {

    private static final ItemRef URANIUM = ItemRef.item("enriched_uranium");
    private static final ItemRef OTHER = ItemRef.item("nuclear_casing");
    private static final ItemRef COAL = ItemRef.material("COAL");

    @Test
    void aComponentStackWithChangedMetaStillCountsAsItsId() {
        assertThat(StorageSlot.identify(URANIUM, () -> false)).isEqualTo(URANIUM);
    }

    @Test
    void aComponentStackIsIdentifiedWithoutAPlainCheck() {
        assertThat(StorageSlot.identify(URANIUM, () -> {
            throw new AssertionError("a component item is not compared with a plain stack");
        })).isEqualTo(URANIUM);
    }

    @Test
    void differentComponentIdsStayDifferentItems() {
        assertThat(StorageSlot.identify(URANIUM, () -> false)).isNotEqualTo(OTHER);
        assertThat(StorageSlot.identify(OTHER, () -> false)).isEqualTo(OTHER);
    }

    @Test
    void aPlainVanillaStackReadsAsItsMaterial() {
        assertThat(StorageSlot.identify(COAL, () -> true)).isEqualTo(COAL);
    }

    @Test
    void aChangedVanillaStackReadsAsForeign() {
        assertThat(StorageSlot.identify(COAL, () -> false)).isNull();
    }
}
