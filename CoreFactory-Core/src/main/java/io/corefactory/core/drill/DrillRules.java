package io.corefactory.core.drill;

import io.corefactory.core.process.ItemRef;

import java.util.Map;
import java.util.Set;

/**
 * The drill rules that need no server: the efficiency a drill runs at, its status, the item a mined ore gives, and where a drill
 * may stand. Every drill is powered by the energy network, so its efficiency is the satisfaction the grid gives it. Pure, so each
 * rule is tested with plain numbers.
 */
public final class DrillRules {

    /** The ore id of a crude petroleum deposit in ores.yml. */
    public static final String DEPOSIT = "crude_petroleum_deposit";

    /** The ore id of a coal ore in ores.yml. A drill that mines it yields charcoal, one item per unit. */
    public static final String COAL = "coal_ore";

    /** The product of a mined coal ore: vanilla charcoal. */
    public static final ItemRef CHARCOAL = ItemRef.material("CHARCOAL");

    /** The ore id of a diamond ore in ores.yml. A drill that mines it yields vanilla diamond, one item per unit. */
    public static final String DIAMOND = "diamond_ore";

    /** The vanilla products of the ores that yield a vanilla item rather than their component. */
    private static final Map<String, ItemRef> VANILLA_PRODUCTS = Map.of(
            COAL, CHARCOAL,
            DIAMOND, ItemRef.material("DIAMOND"));

    private static final Set<String> HARD_BASE = Set.of(
            "STONE", "GRANITE", "DIORITE", "ANDESITE", "TUFF", "DEEPSLATE");

    private DrillRules() {
    }

    /**
     * The efficiency of a drill from the satisfaction its grid gives it: the satisfaction, clamped to 0 to 1. A drill whose grid gives
     * nothing has no efficiency, so it does not advance its cycle.
     *
     * @param satisfaction the share of its demand the energy network gave the drill, 0 to 1
     */
    public static double efficiency(double satisfaction) {
        if (Double.isNaN(satisfaction)) {
            return 0.0;
        }
        return Math.min(1.0, Math.max(0.0, satisfaction));
    }

    /**
     * The status of a drill, checked in this order: no power, then no room, then no deposit in its square, then an empty
     * deposit. A drill is running only when all four are in order.
     *
     * @param powered       true when its efficiency is positive
     * @param roomLeft      true when its output slots can take another item, or a petroleum drill's network takes more
     * @param targetInRange true when a deposit its kind and selection allow touches its square
     * @param working       true when the drill has a deposit with something left to work on
     */
    public static DrillStatus status(boolean powered, boolean roomLeft, boolean targetInRange, boolean working) {
        if (!powered) {
            return DrillStatus.NO_POWER;
        }
        if (!roomLeft) {
            return DrillStatus.FULL;
        }
        if (!targetInRange) {
            return DrillStatus.NO_TARGET;
        }
        return working ? DrillStatus.RUNNING : DrillStatus.EXHAUSTED;
    }

    /**
     * The item a drill yields from one mined ore unit: vanilla charcoal for coal, vanilla diamond for diamond ore, otherwise
     * the ore's drop component. Every ore of an ore drill follows the same power and target rules, at any level.
     *
     * @param oreId the ore's id in ores.yml
     * @param drop  the ore's drop component id
     */
    public static ItemRef mined(String oreId, String drop) {
        ItemRef vanilla = VANILLA_PRODUCTS.get(oreId);
        return vanilla != null ? vanilla : ItemRef.item(drop);
    }

    /**
     * True when a block of this material is hard base stone, so a drill may stand on it: stone, granite, diorite,
     * andesite, tuff or deepslate, the same blocks as the overworld base stone tag.
     */
    public static boolean hardBase(String material) {
        return material != null && HARD_BASE.contains(material);
    }
}
