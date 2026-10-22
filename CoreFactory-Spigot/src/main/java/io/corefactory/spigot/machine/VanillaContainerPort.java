package io.corefactory.spigot.machine;

import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.PickRule;
import io.corefactory.core.logistics.Port;
import io.corefactory.core.logistics.PortItems;
import io.corefactory.core.logistics.StorageRules;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import io.corefactory.spigot.hook.ContainerProtection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * A vanilla storage block as a logistics port: a chest, a trapped chest, a barrel, a shulker box or a copper chest. It is a sink and a
 * source. A belt's end or a picker's output puts one item into its inventory exactly when the inventory has room for it, by the rules of
 * {@link StorageRules}; the insert is verified by {@link HopperTransfer#placeOne}, so the count rises by exactly the one item or the slot
 * is left as it was. A picker at its input takes one item from the inventory: {@link #peekOutput} shows the first item in the inventory
 * the picker may take, and {@link #takeOutput} removes exactly that one item, verified by {@link HopperTransfer#takeOne}. A picker takes
 * nothing from a stack that is not a plain item (a named or enchanted one), so such a stack is skipped.
 *
 * <p>A double chest is one inventory of two halves. {@link Container#getInventory()} of either half is the combined inventory of both, so
 * a picker beside one half takes from, and a belt or picker puts into, the whole double chest. Its claim check therefore asks each half
 * in turn, and the whole inventory is open only when both halves are.
 *
 * <p>The inventory is read live on each call, so a chest in an unloaded chunk, or a block that is no longer storage, gives and takes
 * nothing and refuses every item. A shulker box refuses a shulker box item.
 *
 * <p>A claim system that protects the container is asked on each insert, for the owner of the device that puts, and on each take, for the
 * owner of the picker that takes; {@link ContainerProtection} decides both. A device with no stored owner passes a null owner, and a
 * protected container refuses it, so a picker with no owner takes nothing from a claimed container, and an unclaimed container is open.
 */
public final class VanillaContainerPort implements Port {

    private final UUID world;
    private final int x;
    private final int y;
    private final int z;
    private final ItemRefs refs;
    private final ContainerProtection protection;
    private final UUID putOwner;
    private final UUID takeOwner;

    private VanillaContainerPort(UUID world, int x, int y, int z, ItemRefs refs, ContainerProtection protection, UUID putOwner,
                                 UUID takeOwner) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.refs = refs;
        this.protection = protection;
        this.putOwner = putOwner;
        this.takeOwner = takeOwner;
    }

    /**
     * The port of a block, or empty when the block is not a storage kind of {@link StorageKinds} or has no inventory.
     *
     * @param block      the block
     * @param refs       converts the items to references
     * @param protection the claim systems that decide whether a device may put or take
     * @param putOwner   the owner of the devices that put items into the block, or null when none is known
     * @param takeOwner  the owner of the pickers that take items from the block, or null when none is known
     */
    public static Optional<VanillaContainerPort> of(Block block, ItemRefs refs, ContainerProtection protection, UUID putOwner,
                                                    UUID takeOwner) {
        Objects.requireNonNull(block, "block");
        if (!StorageKinds.isStorage(block.getType().name()) || !(block.getState() instanceof Container)) {
            return Optional.empty();
        }
        return Optional.of(new VanillaContainerPort(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), refs,
                protection, putOwner, takeOwner));
    }

    @Override
    public boolean accepts(Item item) {
        Optional<Container> container = live();
        return container.isPresent() && permitted(container.get(), putOwner)
                && slotOf(container.get(), PortItems.refOf(item)).isPresent();
    }

    @Override
    public void accept(Item item) {
        ItemRef ref = PortItems.refOf(item);
        Container container = live().orElseThrow(() -> new IllegalStateException("the container is not loaded: " + item.id()));
        if (!permitted(container, putOwner)) {
            throw new IllegalStateException("a claim protects the container of " + item.id());
        }
        OptionalInt slot = slotOf(container, ref);
        if (slot.isEmpty() || !HopperTransfer.placeOne(1, new StorageSlot(container.getInventory(), refs, slot.getAsInt()),
                ref, refs.limit(ref))) {
            throw new IllegalStateException("the container does not take " + item.id());
        }
    }

    /**
     * The first item the picker may take: the first plain stack of the inventory, when the claim check lets the picker's owner take
     * from the container. Null when the container holds nothing to take or the claim refuses the take. Read now, so the picker waits
     * while the container is closed to it.
     */
    @Override
    public Item peekOutput() {
        Optional<Container> container = live();
        if (container.isEmpty()) {
            return null;
        }
        OptionalInt index = takeableSlot(container.get());
        if (index.isEmpty()) {
            return null;
        }
        Item item = PortItems.itemOf(new StorageSlot(container.get().getInventory(), refs, index.getAsInt()).ref());
        return PickRule.decide(item, permitted(container.get(), takeOwner)) == PickRule.Verdict.TAKE ? item : null;
    }

    /**
     * Removes exactly the item {@link #peekOutput} shows, one item from its stack, and returns it. A removal the count does not confirm,
     * or one the claim refuses, throws: the picker then puts nothing, so the item is neither lost nor copied.
     */
    @Override
    public Item takeOutput() {
        Container container = live().orElseThrow(() -> new IllegalStateException("the container is not loaded"));
        OptionalInt index = takeableSlot(container);
        if (index.isEmpty()) {
            return null;
        }
        StorageSlot slot = new StorageSlot(container.getInventory(), refs, index.getAsInt());
        ItemRef ref = slot.ref();
        Item item = PortItems.itemOf(ref);
        if (PickRule.decide(item, permitted(container, takeOwner)) != PickRule.Verdict.TAKE) {
            throw new IllegalStateException("a claim protects the container of " + item.id());
        }
        if (!HopperTransfer.takeOne(slot, ref)) {
            throw new IllegalStateException("the container did not give " + item.id());
        }
        return item;
    }

    @Override
    public int storedCount() {
        Optional<Container> container = live();
        if (container.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ItemStack item : container.get().getInventory().getContents()) {
            if (!ItemRefs.isEmpty(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * True when the claim systems let the owner put items into or take items from the container, read now. A double chest is asked for
     * each of its halves, since its whole inventory is open through either one.
     *
     * @param container the container, of either half of a double chest or a single block
     * @param who       the device's owner, or null when it stores none
     */
    private boolean permitted(Container container, UUID who) {
        Inventory inventory = container.getInventory();
        if (inventory instanceof DoubleChestInventory combined && combined.getHolder() != null) {
            DoubleChest chest = combined.getHolder();
            return halfPermits(chest.getLeftSide(), who) && halfPermits(chest.getRightSide(), who);
        }
        return protection.permits(container.getLocation(), who);
    }

    /** True when the claim systems let the owner use one half of a double chest. A half with no known location is refused when claims apply. */
    private boolean halfPermits(InventoryHolder half, UUID who) {
        Location location = half instanceof BlockState state ? state.getLocation() : null;
        return protection.permits(location, who);
    }

    /** The first slot the picker may take from: a plain stack with at least one item, in slot order. */
    private OptionalInt takeableSlot(Container container) {
        Inventory inventory = container.getInventory();
        for (int index = 0; index < inventory.getSize(); index++) {
            if (new StorageSlot(inventory, refs, index).ref() != null) {
                return OptionalInt.of(index);
            }
        }
        return OptionalInt.empty();
    }

    /** The slot an item goes into, or empty when the container refuses it or has no room. */
    private OptionalInt slotOf(Container container, ItemRef ref) {
        if (StorageKinds.refuses(container.getType().name(), ref)) {
            return OptionalInt.empty();
        }
        Inventory inventory = container.getInventory();
        List<Stack> stacks = new ArrayList<>();
        for (int index = 0; index < inventory.getSize(); index++) {
            stacks.add(new StorageSlot(inventory, refs, index));
        }
        return StorageRules.slotFor(refs.limit(ref), stacks, ref);
    }

    /** The container at the block, read now: empty when its world or chunk is not loaded, or it is no longer a storage block. */
    private Optional<Container> live() {
        World loaded = Bukkit.getWorld(world);
        if (loaded == null || !loaded.isChunkLoaded(x >> 4, z >> 4)) {
            return Optional.empty();
        }
        Block block = loaded.getBlockAt(x, y, z);
        if (!StorageKinds.isStorage(block.getType().name())) {
            return Optional.empty();
        }
        return block.getState() instanceof Container container ? Optional.of(container) : Optional.empty();
    }
}
