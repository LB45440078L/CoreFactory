package io.corefactory.core.process;

/**
 * One slot of a machine as the process engine sees it. The Spigot side implements it over an inventory slot, so the
 * engine never touches Bukkit types and can be tested with plain objects.
 */
public interface Stack {

    /** The item in the slot, or null when the slot is empty. */
    ItemRef ref();

    /** How many items the slot holds. Zero when it is empty. */
    int count();

    /** Puts {@code count} items of {@code ref} in the slot. A count of zero or less empties the slot. */
    void set(ItemRef ref, int count);
}
