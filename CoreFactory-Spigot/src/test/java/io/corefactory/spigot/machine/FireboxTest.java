package io.corefactory.spigot.machine;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Machine;
import io.corefactory.core.process.ProcessEngine;
import io.corefactory.core.process.Stack;
import io.corefactory.spigot.ShippedContent;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The firebox indicator's rule and the kiln's layout that holds it. Nothing here starts a server. */
class FireboxTest {

    private static MachineLayout kiln;

    @BeforeAll
    static void load() {
        Machine machine = ShippedContent.load().processes().machine("roaster").orElseThrow();
        kiln = MachineLayouts.of(machine);
    }

    @Test
    void theFireboxIsLitOnlyWhileARoastAdvancesWithFuelInTheSlots() {
        assertThat(Firebox.lit(true, 1)).isTrue();
        assertThat(Firebox.lit(true, 5)).isTrue();
        assertThat(Firebox.lit(true, 0)).as("a roast with no fuel").isFalse();
        assertThat(Firebox.lit(false, 1)).as("no roast advanced this tick").isFalse();
        assertThat(Firebox.lit(false, 0)).isFalse();
    }

    @Test
    void theFireboxCountsOnlyTheFuelItemsItsSlotsHold() {
        List<Stack> fuel = List.of(
                new MemoryStack(ItemRef.material("COAL"), 3),
                new MemoryStack(null, 0),
                new MemoryStack(ItemRef.material("CHARCOAL"), 2));
        assertThat(Firebox.fuelItems(fuel, ProcessEngine.FUEL)).isEqualTo(5);

        List<Stack> notFuel = List.of(new MemoryStack(ItemRef.item("crushed_uraninite"), 2));
        assertThat(Firebox.fuelItems(notFuel, ProcessEngine.FUEL)).isZero();
        assertThat(Firebox.fuelItems(List.of(), ProcessEngine.FUEL)).isZero();
    }

    @Test
    void theIndicatorIsBlazePowderWhenLitAndGunpowderWhenNot() {
        assertThat(Firebox.material(true)).isEqualTo(Material.BLAZE_POWDER);
        assertThat(Firebox.material(false)).isEqualTo(Material.GUNPOWDER);
    }

    @Test
    void theKilnHasItsFireboxInSlotThirteenAndNoGlowstone() {
        assertThat(kiln.fireboxSlot()).isEqualTo(13);
        assertThat(kiln.kindAt(13)).isEqualTo(MachineLayout.Kind.FIREBOX);
        assertThat(kiln.inputSlots()).doesNotContain(13);
        assertThat(kiln.decorAt(14)).as("slot 14 is a plain frame pane").isEqualTo(Material.BROWN_STAINED_GLASS_PANE);
        for (int slot = 0; slot < kiln.size(); slot++) {
            assertThat(kiln.decorAt(slot)).as("slot " + slot).isNotEqualTo(Material.GLOWSTONE);
        }
    }

    /** A slot held in memory, emptied when its count is zero or less. */
    private static final class MemoryStack implements Stack {

        private final ItemRef ref;
        private final int count;

        MemoryStack(ItemRef ref, int count) {
            this.ref = ref;
            this.count = count;
        }

        @Override
        public ItemRef ref() {
            return count > 0 ? ref : null;
        }

        @Override
        public int count() {
            return count;
        }

        @Override
        public void set(ItemRef ref, int count) {
            throw new UnsupportedOperationException("read only");
        }
    }
}
