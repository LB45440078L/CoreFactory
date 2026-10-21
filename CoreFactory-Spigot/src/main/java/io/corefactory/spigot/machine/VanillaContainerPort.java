package io.corefactory.spigot.machine;

import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.Port;
import io.corefactory.core.logistics.PortItems;
import io.corefactory.core.logistics.StorageRules;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import io.corefactory.spigot.hook.ContainerProtection;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * A vanilla storage block as a logistics port: a chest, a trapped chest, a barrel, a shulker box or a copper chest. A belt's end or a
 * picker's output puts one item into its inventory exactly when the inventory has room for it, by the rules of {@link StorageRules};
 * the insert is verified by {@link HopperTransfer#placeOne}, so the count rises by exactly the one item or the slot is left as it was.
 *
 * <p>The port is a sink only. It gives nothing: a picker takes from machines, belts and splitters, so a picker that has a chest at its
 * input takes nothing from it, and a splitter takes from its belt alone. The inventory is read live on each call, so a chest in an
 * unloaded chunk, or a block that is no longer storage, refuses every item. A shulker box refuses a shulker box item.
 *
 * <p>A claim system that protects the container must also let the device's owner in: {@link ContainerProtection} is asked on each
 * insert. The owner is the player who placed the device; a device with no stored owner passes a null owner, and a protected container
 * refuses it.
 */
public final class VanillaContainerPort implements Port {

    private final UUID world;
    private final int x;
    private final int y;
    private final int z;
    private final ItemRefs refs;
    private final ContainerProtection protection;
    private final UUID owner;

    private VanillaContainerPort(UUID world, int x, int y, int z, ItemRefs refs, ContainerProtection protection, UUID owner) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.refs = refs;
        this.protection = protection;
        this.owner = owner;
    }

    /**
     * The port of a block, or empty when the block is not a storage kind of {@link StorageKinds} or has no inventory.
     *
     * @param block      the block
     * @param refs       converts the items to references
     * @param protection the claim systems that decide whether the device may insert
     * @param owner      the device's owner, or null when it stores none
     */
    public static Optional<VanillaContainerPort> of(Block block, ItemRefs refs, ContainerProtection protection, UUID owner) {
        Objects.requireNonNull(block, "block");
        if (!StorageKinds.isStorage(block.getType().name()) || !(block.getState() instanceof Container)) {
            return Optional.empty();
        }
        return Optional.of(new VanillaContainerPort(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), refs,
                protection, owner));
    }

    @Override
    public boolean accepts(Item item) {
        Optional<Container> container = live();
        return container.isPresent() && permitted(container.get()) && slotOf(container.get(), PortItems.refOf(item)).isPresent();
    }

    @Override
    public void accept(Item item) {
        ItemRef ref = PortItems.refOf(item);
        Container container = live().orElseThrow(() -> new IllegalStateException("the container is not loaded: " + item.id()));
        if (!permitted(container)) {
            throw new IllegalStateException("a claim protects the container of " + item.id());
        }
        OptionalInt slot = slotOf(container, ref);
        if (slot.isEmpty() || !HopperTransfer.placeOne(1, new StorageSlot(container.getInventory(), refs, slot.getAsInt()),
                ref, refs.limit(ref))) {
            throw new IllegalStateException("the container does not take " + item.id());
        }
    }

    /** True when the claim systems let this device's owner put items into the container, read now. */
    private boolean permitted(Container container) {
        return protection.permits(container.getBlock(), owner);
    }

    @Override
    public Item peekOutput() {
        return null;
    }

    @Override
    public Item takeOutput() {
        return null;
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
