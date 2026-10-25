package io.corefactory.paper.machine;

import io.corefactory.core.logistics.CrafterGrid;
import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.PickRule;
import io.corefactory.core.logistics.Port;
import io.corefactory.core.process.Stack;
import io.corefactory.paper.hook.ContainerProtection;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A crafter as a logistics port over its nine grid slots. The slot rules are {@link CrafterGrid}'s; this class reads the live crafter,
 * tells the grid which slots the crafter has disabled, and asks the claim systems on each put and take, as {@link VanillaContainerPort}
 * does for a chest. A picker takes an ingredient out of the grid, and a belt or picker puts one in, so the grid fills and the crafter
 * crafts from it. The result is not held in the grid: the crafter ejects it into the block it faces, as vanilla does.
 *
 * <p>The crafter is read live on each call, so an unloaded chunk, or a block that is no longer a crafter, gives and takes nothing.
 */
public final class CrafterPort implements Port {

    private final UUID world;
    private final int x;
    private final int y;
    private final int z;
    private final ItemRefs refs;
    private final ContainerProtection protection;
    private final UUID putOwner;
    private final UUID takeOwner;

    private CrafterPort(UUID world, int x, int y, int z, ItemRefs refs, ContainerProtection protection, UUID putOwner, UUID takeOwner) {
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
     * The port of a block, or empty when the block is not a crafter or has no grid of nine slots.
     *
     * @param block      the block
     * @param refs       converts the items to references
     * @param protection the claim systems that decide whether a device may put or take
     * @param putOwner   the owner of the devices that put items into the crafter, or null when none is known
     * @param takeOwner  the owner of the pickers that take items from the crafter, or null when none is known
     */
    public static Optional<CrafterPort> of(Block block, ItemRefs refs, ContainerProtection protection, UUID putOwner, UUID takeOwner) {
        Objects.requireNonNull(block, "block");
        if (!StorageKinds.isCrafter(block.getType().name()) || !(block.getState() instanceof Crafter crafter)) {
            return Optional.empty();
        }
        if (crafter.getInventory().getSize() != CrafterGrid.SLOTS) {
            return Optional.empty();
        }
        return Optional.of(new CrafterPort(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), refs, protection,
                putOwner, takeOwner));
    }

    @Override
    public boolean accepts(Item item) {
        Optional<Crafter> crafter = live();
        return crafter.isPresent() && permitted(crafter.get(), putOwner) && grid(crafter.get()).accepts(item);
    }

    @Override
    public void accept(Item item) {
        Crafter crafter = live().orElseThrow(() -> new IllegalStateException("the crafter is not loaded: " + item.id()));
        if (!permitted(crafter, putOwner)) {
            throw new IllegalStateException("a claim protects the crafter of " + item.id());
        }
        grid(crafter).accept(item);
    }

    /** The item a picker may take next, when the claim check lets the picker's owner take from the crafter; null otherwise. */
    @Override
    public Item peekOutput() {
        Optional<Crafter> crafter = live();
        if (crafter.isEmpty()) {
            return null;
        }
        Item item = grid(crafter.get()).peekOutput();
        return PickRule.decide(item, permitted(crafter.get(), takeOwner)) == PickRule.Verdict.TAKE ? item : null;
    }

    @Override
    public Item takeOutput() {
        Crafter crafter = live().orElseThrow(() -> new IllegalStateException("the crafter is not loaded"));
        CrafterGrid grid = grid(crafter);
        Item item = grid.peekOutput();
        if (item == null) {
            return null;
        }
        if (PickRule.decide(item, permitted(crafter, takeOwner)) != PickRule.Verdict.TAKE) {
            throw new IllegalStateException("a claim protects the crafter of " + item.id());
        }
        return grid.takeOutput();
    }

    @Override
    public int storedCount() {
        return live().map(crafter -> grid(crafter).storedCount()).orElse(0);
    }

    /** The grid over the crafter's live slots, with the slots its setting disables. */
    private CrafterGrid grid(Crafter crafter) {
        List<Stack> slots = new ArrayList<>();
        boolean[] disabled = new boolean[CrafterGrid.SLOTS];
        for (int index = 0; index < CrafterGrid.SLOTS; index++) {
            slots.add(new StorageSlot(crafter.getInventory(), refs, index));
            disabled[index] = crafter.isSlotDisabled(index);
        }
        return new CrafterGrid(slots, disabled, refs::limit);
    }

    /** True when the claim systems let the owner put items into or take items from the crafter, read now. */
    private boolean permitted(Crafter crafter, UUID who) {
        return protection.permits(crafter.getLocation(), who);
    }

    /** The crafter at the block, read now: empty when its world or chunk is not loaded, or it is no longer a crafter. */
    private Optional<Crafter> live() {
        World loaded = Bukkit.getWorld(world);
        if (loaded == null || !loaded.isChunkLoaded(x >> 4, z >> 4)) {
            return Optional.empty();
        }
        Block block = loaded.getBlockAt(x, y, z);
        if (!StorageKinds.isCrafter(block.getType().name())) {
            return Optional.empty();
        }
        return block.getState() instanceof Crafter crafter ? Optional.of(crafter) : Optional.empty();
    }
}
