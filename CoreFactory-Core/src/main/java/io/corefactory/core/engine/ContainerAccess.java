package io.corefactory.core.engine;

import java.util.Locale;
import java.util.Set;

/**
 * The rule that decides whether a logistics device may put items into a container that a claim system protects. The device's owner
 * is the player who placed it, when one is stored; a device with no stored owner has no trust to prove, so a protected container
 * refuses it. An unprotected container is always open to a device. Pure.
 */
public final class ContainerAccess {

    /**
     * The claim trust levels that let a player put items into a container: container (inventory), build, edit and manage. Access alone,
     * which opens doors and buttons, does not.
     */
    private static final Set<String> CONTAINER_LEVELS = Set.of("INVENTORY", "CONTAINER", "BUILD", "EDIT", "MANAGE");

    private ContainerAccess() {
    }

    /**
     * True when a claim trust level lets its holder put items into a container.
     *
     * @param level the level's name, such as {@code Inventory} or {@code Build}; null is no level
     */
    public static boolean grantsContainer(String level) {
        return level != null && CONTAINER_LEVELS.contains(level.toUpperCase(Locale.ROOT));
    }

    /**
     * True when a device may put items into a container.
     *
     * @param protectedHere true when a claim system protects the container
     * @param hasOwner      true when the device has a stored owner
     * @param ownerTrusted  true when that owner is trusted with containers there; ignored without an owner
     */
    public static boolean permits(boolean protectedHere, boolean hasOwner, boolean ownerTrusted) {
        return !protectedHere || (hasOwner && ownerTrusted);
    }
}
