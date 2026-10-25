package io.corefactory.paper.energy;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.energy.GeneratorFuel;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.item.PlaceableItems;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The items of the generators: a generator's own item, the fuel stacks a generator gives back, and the test of whether an item
 * may go into a generator's fuel slot. A vanilla charcoal is accepted only as a plain item, with no plugin identity. Petroleum and
 * diesel are not items: they come from the oil network.
 */
public final class GeneratorItems {

    private final CoreFactoryCore core;

    /** @param core the core, which builds the component items and reads their identity */
    public GeneratorItems(CoreFactoryCore core) {
        this.core = core;
    }

    /** The item of a generator's kind, one of it: empty when its component is not defined. */
    public Optional<ItemStack> component(GeneratorKind kind) {
        return componentNamed(GeneratorIds.componentId(kind), 1);
    }

    /**
     * The fuel items of a kind, in the given amount. Empty for a kind that takes no item, which is every kind but charcoal and nuclear,
     * or an amount below one.
     *
     * @param kind   the generator kind
     * @param amount the items, positive
     */
    public Optional<ItemStack> fuel(GeneratorKind kind, int amount) {
        if (amount < 1) {
            return Optional.empty();
        }
        return switch (kind) {
            case WIND, SOLAR, PETROLEUM, DIESEL -> Optional.empty();
            case CHARCOAL -> Optional.of(new ItemStack(Material.CHARCOAL, amount));
            case NUCLEAR -> GeneratorFuel.itemIdOf(kind).flatMap(id -> componentNamed(id, amount));
        };
    }

    /** The fuel a generator keeps given back as stacks of {@link GeneratorFuel#CAP} or fewer. */
    public List<ItemStack> fuelStacks(GeneratorKind kind, int count) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int size : GeneratorFuel.stacks(count)) {
            fuel(kind, size).ifPresent(stacks::add);
        }
        return stacks;
    }

    /**
     * Whether an item may go into a generator's fuel slot. A plugin component is named by its id; a vanilla item is named by its
     * material, and only when it carries no plugin identity at all.
     *
     * @param kind the generator kind
     * @param item the item the player holds or moves
     */
    public boolean accepts(GeneratorKind kind, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        Optional<PlaceableItems.Identity> identity = PlaceableItems.identityOf(core.items(), item);
        if (identity.isEmpty()) {
            return GeneratorFuel.accepts(kind, GeneratorFuel.Source.MATERIAL, item.getType().name());
        }
        return identity.get().kind() == PlaceableItems.Kind.COMPONENT
                && GeneratorFuel.accepts(kind, GeneratorFuel.Source.COMPONENT, identity.get().id());
    }

    private Optional<ItemStack> componentNamed(String id, int amount) {
        ComponentDefinition definition = core.componentDefinitions().get(id);
        return definition == null ? Optional.empty() : Optional.of(core.items().component(definition, amount));
    }
}
