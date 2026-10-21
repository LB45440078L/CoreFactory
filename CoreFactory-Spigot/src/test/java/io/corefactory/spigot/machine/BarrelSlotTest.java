package io.corefactory.spigot.machine;

import io.corefactory.core.process.ItemRef;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The out-of-range guard of a barrel slot: a slot the live inventory does not have reads as empty and is never written. The
 * inventory is a stand-in held in memory, so nothing here needs a server.
 */
class BarrelSlotTest {

    private static final ItemRef COAL = ItemRef.material("COAL");

    /** A container of the given size that records every slot it is written at, and fails a read past its end. */
    private static final class FakeContainer implements InvocationHandler {

        private final ItemStack[] items;
        private final List<Integer> writes = new ArrayList<>();

        FakeContainer(int size) {
            this.items = new ItemStack[size];
        }

        Inventory inventory() {
            return (Inventory) Proxy.newProxyInstance(FakeContainer.class.getClassLoader(),
                    new Class<?>[] {Inventory.class}, this);
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return switch (method.getName()) {
                case "getSize" -> items.length;
                case "getItem" -> items[(Integer) args[0]];
                case "setItem" -> {
                    writes.add((Integer) args[0]);
                    items[(Integer) args[0]] = (ItemStack) args[1];
                    yield null;
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "FakeContainer";
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }
    }

    @Test
    void aSlotPastTheEndOfANineSlotContainerReadsAsEmpty() {
        FakeContainer container = new FakeContainer(9);
        BarrelSlot slot = new BarrelSlot(() -> Optional.of(container.inventory()), new ItemRefs(null), 9);

        assertThat(slot.count()).isZero();
        assertThat(slot.ref()).isNull();
    }

    @Test
    void aSlotPastTheEndIsNeverWritten() {
        FakeContainer container = new FakeContainer(9);
        BarrelSlot slot = new BarrelSlot(() -> Optional.of(container.inventory()), new ItemRefs(null), 9);

        slot.set(COAL, 3);
        slot.set(null, 0);

        assertThat(container.writes).isEmpty();
    }

    @Test
    void aSlotInsideTheContainerStillReadsAndWrites() {
        FakeContainer container = new FakeContainer(9);
        BarrelSlot slot = new BarrelSlot(() -> Optional.of(container.inventory()), new ItemRefs(null), 8);

        assertThat(slot.count()).isZero();
        slot.set(null, 0);

        assertThat(container.writes).containsExactly(8);
    }

    @Test
    void aSlotOfAMissingInventoryIsAbsentToo() {
        BarrelSlot slot = new BarrelSlot(Optional::empty, new ItemRefs(null), 0);

        assertThat(slot.count()).isZero();
        assertThat(slot.ref()).isNull();
        slot.set(COAL, 1);
    }

    @Test
    void theBarrelHelpersTreatASlotPastTheEndAsAbsent() {
        FakeContainer container = new FakeContainer(9);
        Inventory inventory = container.inventory();

        assertThat(Barrel.has(inventory, 8)).isTrue();
        assertThat(Barrel.has(inventory, 9)).isFalse();
        assertThat(Barrel.has(inventory, -1)).isFalse();
        assertThat(Barrel.itemAt(inventory, 9)).isNull();
        Barrel.setItemAt(inventory, 9, null);
        assertThat(container.writes).isEmpty();
    }
}
