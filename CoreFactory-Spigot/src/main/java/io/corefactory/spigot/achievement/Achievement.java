package io.corefactory.spigot.achievement;

import org.bukkit.inventory.ItemStack;

/**
 * One achievement. A player unlocks it when the progress for its {@link #trigger} and {@link #param} reaches
 * {@link #goal}.
 *
 * @param id          a stable key, stored in the progress file; never reuse one
 * @param category    the section it is listed under in the achievements menu
 * @param group       the group this achievement counts toward, or {@code null}
 * @param title       the name, with {@code &} colour codes
 * @param description what the player has to do, with {@code &} colour codes
 * @param icon        the item shown in the menu
 * @param trigger     the kind of event that advances it
 * @param param       the id it watches, {@link AchievementCatalog#ANY} for any, or a number for {@link Trigger#CHAIN}
 * @param goal        how many matching events it takes
 * @param major       whether unlocking it also shows a title on screen
 */
public record Achievement(String id, Category category, String group, String title, String description,
                          ItemStack icon, Trigger trigger, String param, int goal, boolean major) {

    /** The sections of the achievements menu, in display order. */
    public enum Category {
        STARTING("Getting started"),
        FORGING("Forging"),
        USING("Using components"),
        LIGHTING("Lighting"),
        DETONATION("Detonations"),
        NUCLEAR("Nuclear"),
        CHAOS("Chaos"),
        SCIENCE("Science"),
        MASTERY("Mastery");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** What advances an achievement. */
    public enum Trigger {
        /** Crafting a TNT type or a component. */
        CRAFT,
        /** Using a component as an ingredient in a recipe. */
        USE,
        /** Lighting a placed TNT, by fire, flint, or a fire charge. */
        IGNITE,
        /** Setting off a TNT, by a player's ignition or a chain it started. */
        DETONATE,
        /** Setting off a nuclear TNT. */
        NUKE,
        /** A nuclear blast that killed at least three villagers near its centre. */
        VILLAGE,
        /** Standing within 1.5 times a nuclear radius when it blasts, and surviving ten seconds later. */
        SURVIVE,
        /** Linking two placed TNT with the fuse linker. */
        LINK,
        /** Dropping a TNT item, which bursts if it is napalm. */
        THROW,
        /** Mining an ore block, where {@code param} is the ore id. */
        MINE,
        /** Taking a product out of a machine's output slots, where {@code param} is the machine id. */
        PRODUCE,
        /** Taking an item out of a machine's output slots, where {@code param} is the item id. */
        MAKE,
        /** Finishing a research node, where {@code param} is the node id. */
        RESEARCH,
        /** Placing any machine. */
        PLACE,
        /** Placing a machine of a kind not placed before. Counts distinct kinds, so its goal is the number of kinds. */
        PLACE_EACH,
        /** A chain of detonations at least this deep, where {@code param} is the depth. */
        CHAIN,
        /** Every achievement in a group unlocked. {@code param} is the group name, or {@code *} for all others. */
        GROUP
    }
}
