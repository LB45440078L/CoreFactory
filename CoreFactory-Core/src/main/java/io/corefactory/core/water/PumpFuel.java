package io.corefactory.core.water;

import io.corefactory.core.oil.Fluid;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.ProcessEngine;

/**
 * The fuel of a pump at levels 1 and 2. A fuel item burns for {@link #ITEM_TICKS} ticks while the pump is wanted, one after another, and
 * a pump that is not wanted burns nothing. Level 1 takes coal or charcoal. Level 2 burns diesel that it draws from the oil network,
 * a litre a minute, so it keeps no items. Pure: the pump keeps the count and the ticks left on the item it burns.
 */
public final class PumpFuel {

    /** Ticks one fuel item burns: 60 seconds. */
    public static final int ITEM_TICKS = 1200;

    /** The most fuel items a pump holds. */
    public static final int CAPACITY = 16;

    /** The diesel a level 2 pump draws from the oil network each tick it runs: one litre a minute, the burn of the item it replaces. */
    public static final double DIESEL_LITRES_PER_TICK = 1.0 / ITEM_TICKS;

    /** The fluid a level 2 pump burns. */
    public static final Fluid FLUID = Fluid.DIESEL;

    private PumpFuel() {
    }

    /**
     * The fuel a pump holds: its items, and the ticks left on the item it is burning.
     *
     * @param items     the fuel items held, 0 to {@link #CAPACITY}
     * @param ticksLeft ticks left on the item burning, 0 when none burns
     */
    public record Burn(int items, int ticksLeft) {

        public Burn {
            if (items < 0 || items > CAPACITY) {
                throw new IllegalArgumentException("fuel items must be 0 to " + CAPACITY + ", got " + items);
            }
            if (ticksLeft < 0 || ticksLeft > ITEM_TICKS) {
                throw new IllegalArgumentException("ticks left must be 0 to " + ITEM_TICKS + ", got " + ticksLeft);
            }
        }

        /** An empty pump: no items and no burn. */
        public static Burn empty() {
            return new Burn(0, 0);
        }

        /**
         * The fuel the pump still has: the items held, plus the item burning now, which has left the held count as it caught.
         */
        public int held() {
            return items + (ticksLeft > 0 ? 1 : 0);
        }
    }

    /**
     * What one tick did to the fuel: the new state, and whether the pump runs this tick.
     *
     * @param burn    the fuel after the tick
     * @param running true when the pump has fuel to burn this tick
     */
    public record Step(Burn burn, boolean running) {
    }

    /**
     * Advances the fuel by one tick. When the pump is wanted and the item it burns has ticks left, that item burns one tick. When the
     * item has run out, the next item is taken from the count, and it burns this tick. The pump runs while an item burns. When it is
     * not wanted, nothing burns.
     *
     * @param burn   the fuel before the tick
     * @param wanted true when the pump has a source and would run if it had fuel
     */
    public static Step step(Burn burn, boolean wanted) {
        if (!wanted) {
            return new Step(burn, false);
        }
        if (burn.ticksLeft() > 0) {
            return new Step(new Burn(burn.items(), burn.ticksLeft() - 1), true);
        }
        if (burn.items() > 0) {
            return new Step(new Burn(burn.items() - 1, ITEM_TICKS - 1), true);
        }
        return new Step(burn, false);
    }

    /**
     * How many fuel items a pump takes from a stack the player offers: as many as fit, up to its capacity.
     *
     * @param items   the items the pump holds
     * @param offered the items in the player's hand
     * @return the number taken, from 0 to {@code offered}
     */
    public static int accept(int items, int offered) {
        int room = Math.max(0, CAPACITY - Math.max(0, items));
        return Math.max(0, Math.min(room, offered));
    }

    /**
     * True when a pump at this level takes this item as fuel: coal or charcoal at level 1. Level 2 takes no item, since it draws
     * diesel from the oil network, and levels 3 to 5 draw power.
     *
     * @param level the pump's level
     * @param item  the item's reference, or null for an empty hand
     */
    public static boolean accepts(int level, ItemRef item) {
        if (item == null) {
            return false;
        }
        return level == 1 && ProcessEngine.FUEL.contains(item);
    }

    /** True when a pump at this level burns a fluid from the oil network instead of an item: level 2. */
    public static boolean drawsFluid(int level) {
        return level == 2;
    }

    /**
     * The litres of diesel a pump wants from the oil network in a tick it runs: {@link #DIESEL_LITRES_PER_TICK} at level 2, 0 at any
     * other level.
     *
     * @param level the pump's level
     */
    public static double litresWanted(int level) {
        return drawsFluid(level) ? DIESEL_LITRES_PER_TICK : 0.0;
    }
}
