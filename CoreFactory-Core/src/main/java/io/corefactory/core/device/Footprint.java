package io.corefactory.core.device;

import java.util.Set;

/**
 * The footprint of a placed device: how many blocks tall it stands. A device stands one block tall unless its component id is in
 * {@link #TALL_IDS}, in which case it stands two blocks tall, like a door. Its anchor is the lower block, which is the device's
 * position; the upper block is a part of the same device. Pure, so the rule is tested without a server.
 */
public final class Footprint {

    /**
     * The component ids of the devices that stand two blocks tall: the generators except solar, and drill levels IV and V. Change
     * this set to change which devices are tall. A drill or generator placed with another id stands one block tall.
     */
    public static final Set<String> TALL_IDS = Set.of(
            "wind_generator",
            "charcoal_generator",
            "petroleum_generator",
            "diesel_generator",
            "nuclear_generator",
            "ore_drill_4",
            "ore_drill_5",
            "petroleum_drill_4",
            "petroleum_drill_5");

    /** The height of a device that stands one block tall. */
    public static final int SHORT = 1;

    /** The height of a device that stands two blocks tall. */
    public static final int TALL = 2;

    /** The tallest footprint of any device. A lookup from a block looks this many blocks down for its anchor. */
    public static final int MAX_HEIGHT = TALL;

    private Footprint() {
    }

    /**
     * The number of blocks a device with this component id stands.
     *
     * @param componentId the component id, such as {@code ore_drill_4}; null is one block
     * @return {@link #TALL} for a tall device, {@link #SHORT} otherwise
     */
    public static int heightOf(String componentId) {
        return componentId != null && TALL_IDS.contains(componentId) ? TALL : SHORT;
    }

    /**
     * Whether a device whose anchor is {@code depth} blocks below a block covers that block.
     *
     * @param height the height of the device, from {@link #heightOf}
     * @param depth  how many blocks below the block its anchor is; 0 when the block is the anchor itself
     * @return true when the device's blocks include the block
     */
    public static boolean covers(int height, int depth) {
        return depth >= 0 && depth < height;
    }
}
