package io.corefactory.core.item;

import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Which plugin items may become a block. A plugin item has one of three identities: a TNT type, an ore block item or a
 * component. A TNT type and an ore block item place through their own listeners. A component places only when it is a machine,
 * whose listener places and registers its host. Every other component is refused, so its vanilla material never lands in the
 * world. A plain item, with no plugin identity, is not restricted here. The rules are pure: the machine test is given by the
 * caller, so they are tested without a server.
 */
public final class PlaceableItems {

    /** The component id of the solar panel: a solar generator, placed on air as a device. */
    public static final String SOLAR_PANEL = "solar_panel";

    /**
     * The component ids of the pylons, accumulators and generators, placed on air by their own listener. A solar panel is a
     * generator device now, so it is placed here and never as a daylight detector.
     */
    public static final Set<String> ENERGY_DEVICES = Set.of(
            "pylon_1", "pylon_2", "pylon_3", "pylon_4", "pylon_5",
            "accumulator_1", "accumulator_2", "accumulator_3", "accumulator_4", "accumulator_5",
            SOLAR_PANEL, "wind_generator", "charcoal_generator", "petroleum_generator", "diesel_generator",
            "nuclear_generator");

    /**
     * The component ids of the belts, belt duplexes, pickers and splitters, placed on air by the logistics listener. Their items are
     * vanilla items with a plugin identity, so they are placed only as devices, never as their vanilla material.
     */
    public static final Set<String> LOGISTICS_DEVICES = Set.of(
            "belt_1", "belt_2", "belt_3", "belt_duplex_1", "belt_duplex_2", "belt_duplex_3",
            "picker_1", "picker_2", "picker_3", "splitter_1", "splitter_2", "splitter_3");

    /** The component ids of the vertical transport belts, placed on air by the vertical belt listener. */
    public static final Set<String> VERTICAL_DEVICES = Set.of("vertical_belt_1", "vertical_belt_2", "vertical_belt_3");

    private PlaceableItems() {
    }

    /**
     * Whether a component id is an item that is placed on air as a device: an energy device or a logistics device. Such an item
     * is placed by its own listener, in the main hand only.
     *
     * @param id the component id
     */
    public static boolean isDevice(String id) {
        return ENERGY_DEVICES.contains(id) || LOGISTICS_DEVICES.contains(id) || VERTICAL_DEVICES.contains(id);
    }

    /** The identity kind of a plugin item. A null kind stands for a plain item with no plugin identity. */
    public enum Kind {
        /** A TNT type: placed through the TNT placement. */
        TNT,
        /** An ore block item: placed through the geology placement. */
        ORE,
        /** A component: a machine, the solar panel, or any other component. */
        COMPONENT
    }

    /**
     * The identity of a plugin item: its kind and its id.
     *
     * @param kind the kind
     * @param id   the TNT, ore or component id
     */
    public record Identity(Kind kind, String id) {
    }

    /**
     * The plugin identity of an item, or empty for a plain item.
     *
     * @param items the item factory that reads the identity
     * @param item  the item
     * @return the identity; a TNT id, an ore id and a component id are checked in that order
     */
    public static Optional<Identity> identityOf(ItemFactory items, ItemStack item) {
        Optional<String> tnt = items.tntId(item);
        if (tnt.isPresent()) {
            return Optional.of(new Identity(Kind.TNT, tnt.get()));
        }
        Optional<String> ore = items.oreId(item);
        if (ore.isPresent()) {
            return Optional.of(new Identity(Kind.ORE, ore.get()));
        }
        return items.componentId(item).map(id -> new Identity(Kind.COMPONENT, id));
    }

    /**
     * Whether a plugin item may be placed as a block at all.
     *
     * @param kind    the item's identity kind, or null for a plain item
     * @param id      the item's id, a TNT id, an ore id or a component id
     * @param machine true when an id names a machine (a lab tool or a drill)
     * @return true when the item may be placed; a plain item is always allowed here
     */
    public static boolean allowed(Kind kind, String id, Predicate<String> machine) {
        if (kind == null) {
            return true;
        }
        return switch (kind) {
            case TNT, ORE -> true;
            case COMPONENT -> isDevice(id) || machine.test(id);
        };
    }

    /**
     * Whether a player may place an item by hand, against a machine they sneak-click, when the item is a block.
     *
     * @param block true when the item's material is a block
     * @param kind  the item's identity kind, or null for a plain item
     * @return true for a plain block only; a TNT type, an ore and any component, the solar panel included, need their own placement
     */
    public static boolean placedByHand(boolean block, Kind kind) {
        return block && kind == null;
    }
}
