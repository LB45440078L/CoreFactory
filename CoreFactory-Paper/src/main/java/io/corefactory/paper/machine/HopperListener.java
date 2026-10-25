package io.corefactory.paper.machine;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.ProcessDefinition;
import io.corefactory.paper.hook.ContainerProtection;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Lets hoppers and droppers work with the lab tools. A move into a lab tool's barrel puts one item into the input or fuel slot
 * that takes it. A move out of a lab tool's barrel takes one output item, and only outputs ever leave. Any other move that
 * touches a lab tool is cancelled, since vanilla would put the item in any slot. A barrel that is not a registered lab tool is
 * not touched.
 *
 * <p>The server has already taken the item out of its source when the event fires, and it puts the item back when the
 * event is cancelled. A move the plugin completes therefore keeps the server's removal: the item goes into the
 * destination, verified, and the event's item is set to air so the server has nothing more to insert. A move the plugin
 * cannot complete is cancelled, and the server restores the source. See {@link HopperTransfer}.
 *
 * <p>A lab tool's output that a hopper moves into a plain inventory, such as a chest, is refused where a claim system protects that
 * inventory: a hopper has no stored owner, so no trust is proven there. A hopper that only moves between plain inventories is not
 * touched by this listener, and vanilla decides it.
 */
public final class HopperListener implements Listener {

    private final CoreFactoryCore core;
    private final MachineStore store;
    private final ItemRefs refs;
    private final ContainerProtection containers;

    /**
     * @param core       the core, which reads the item identities
     * @param store      the placed machines, which find the lab tools
     * @param containers the claim systems that decide whether a plain inventory may take a lab tool's output
     */
    public HopperListener(CoreFactoryCore core, MachineStore store, ContainerProtection containers) {
        this.core = core;
        this.store = store;
        this.refs = new ItemRefs(core);
        this.containers = containers;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent event) {
        PlacedMachine from = labOf(event.getSource());
        PlacedMachine to = labOf(event.getDestination());
        if (from == null && to == null) {
            return;
        }
        if (from != null && from.menu().hasPending() || to != null && to.menu().hasPending()) {
            event.setCancelled(true);
            return;
        }
        ItemStack moving = event.getItem();
        boolean moved = from == null ? intoLab(to, moving) : outOfLab(from, to, event.getDestination(), moving);
        if (moved) {
            event.setItem(new ItemStack(Material.AIR));
        } else {
            event.setCancelled(true);
        }
    }

    /** Puts one item from a plain inventory into a lab tool. False, and nothing changed, when no slot takes it. */
    private boolean intoLab(PlacedMachine to, ItemStack moving) {
        Optional<ItemRef> ref = refs.of(moving);
        Optional<Inventory> held = to.menu().barrel().inventory();
        if (ref.isEmpty() || held.isEmpty()) {
            return false;
        }
        Optional<MachineSlots.Role> role = roleFor(to, ref.get());
        OptionalInt slot = role.isPresent() ? slotFor(to, held.get(), role.get(), ref.get()) : OptionalInt.empty();
        if (HopperRules.intake(role, slot.isPresent()) == HopperRules.Action.REFUSE) {
            return false;
        }
        BarrelSlot target = new BarrelSlot(() -> to.menu().barrel().inventory(), refs, slot.getAsInt());
        return HopperTransfer.placeOne(moving.getAmount(), target, ref.get(), refs.limit(ref.get()));
    }

    /**
     * Puts one output item of a lab tool into another lab tool or into a plain inventory. False, and nothing changed, when the
     * item is not a product of the lab tool or the destination takes no more of it.
     */
    private boolean outOfLab(PlacedMachine from, PlacedMachine to, Inventory destination, ItemStack moving) {
        Optional<ItemRef> ref = refs.of(moving);
        if (ref.isEmpty() || roleFor(from, ref.get()).isPresent()) {
            return false;
        }
        boolean product = HopperRules.isProduct(ref.get(), productsOf(from));
        BarrelSlot target = to == null ? null : targetSlot(to, ref.get());
        OptionalInt index = to == null ? plainSlot(destination, ref.get()) : OptionalInt.empty();
        boolean takes = target != null || index.isPresent();
        if (HopperRules.outflow(product, takes) == HopperRules.Action.REFUSE) {
            return false;
        }
        if (to == null && !containers.permits(destination.getLocation(), null)) {
            return false;
        }
        BarrelSlot slot = target != null ? target
                : new BarrelSlot(() -> Optional.of(destination), refs, index.getAsInt());
        return HopperTransfer.placeOne(moving.getAmount(), slot, ref.get(), refs.limit(ref.get()));
    }

    /** The slot of another lab tool's barrel that takes the item, or null when no slot takes it. */
    private BarrelSlot targetSlot(PlacedMachine to, ItemRef ref) {
        Optional<Inventory> held = to.menu().barrel().inventory();
        Optional<MachineSlots.Role> role = held.isPresent() ? roleFor(to, ref) : Optional.empty();
        OptionalInt slot = role.isPresent() ? slotFor(to, held.get(), role.get(), ref) : OptionalInt.empty();
        return slot.isPresent() ? new BarrelSlot(() -> to.menu().barrel().inventory(), refs, slot.getAsInt()) : null;
    }

    private List<ProcessDefinition> productsOf(PlacedMachine machine) {
        return core.processes().processesFor(machine.machine().id());
    }

    private static Optional<MachineSlots.Role> roleFor(PlacedMachine machine, ItemRef item) {
        MachineMenu menu = machine.menu();
        return HopperRules.roleOf(ref -> menu.accepts(ref, MachineLayout.Kind.INPUT),
                ref -> menu.accepts(ref, MachineLayout.Kind.FUEL), item);
    }

    /** The barrel slot an item goes into in the given role: the first slot holding it with room, else the first empty one. */
    private OptionalInt slotFor(PlacedMachine machine, Inventory held, MachineSlots.Role role, ItemRef item) {
        List<Integer> indexes = machine.menu().slots().indexes(role).stream()
                .filter(index -> Barrel.has(held, index)).toList();
        List<ItemRef> refsHeld = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (int index : indexes) {
            ItemStack stack = Barrel.itemAt(held, index);
            refsHeld.add(refs.of(stack).orElse(null));
            counts.add(ItemRefs.isEmpty(stack) ? 0 : stack.getAmount());
        }
        OptionalInt position = HopperRules.insertionSlot(refsHeld, counts, item, refs.limit(item));
        return position.isPresent() ? OptionalInt.of(indexes.get(position.getAsInt())) : OptionalInt.empty();
    }

    /** The slot of a plain inventory an item goes into: the first slot holding it with room, else the first empty one. */
    private OptionalInt plainSlot(Inventory inventory, ItemRef item) {
        List<ItemRef> refsHeld = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (ItemStack stack : inventory.getContents()) {
            refsHeld.add(refs.of(stack).orElse(null));
            counts.add(ItemRefs.isEmpty(stack) ? 0 : stack.getAmount());
        }
        return HopperRules.insertionSlot(refsHeld, counts, item, refs.limit(item));
    }

    /** The registered lab tool whose barrel is this inventory, or null for any other inventory. Found by location. */
    private PlacedMachine labOf(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof BlockState state) {
            return store.at(state.getLocation()).orElse(null);
        }
        return null;
    }
}
