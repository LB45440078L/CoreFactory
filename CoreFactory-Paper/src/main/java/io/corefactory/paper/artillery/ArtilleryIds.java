package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryLevel;
import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ChargeTier;
import io.corefactory.core.item.ItemFactory;
import org.bukkit.NamespacedKey;

import java.util.Locale;
import java.util.Optional;

/** The ids of the artillery pieces: their items, one per level, the device type they are saved under, and the marker their entities carry. */
public final class ArtilleryIds {

    /** The prefix of a piece's component id; the level follows it, such as {@code artillery_3}. */
    public static final String ITEM_PREFIX = "artillery_";

    /** The device type the pieces are saved under in {@code artillery.yml}. */
    public static final String DEVICE_TYPE = "artillery";

    /** The component id of the Artillery Command Center's item, which places a command center. Its model is {@code machine/command_center}. */
    public static final String COMMAND_CENTER = "command_center";

    /** The suffix of a shell's component id; the shell's name in lower case comes first, such as {@code explosive_shell}. */
    static final String SHELL_SUFFIX = "_shell";

    /** The prefix of a charge's component id; the tier number follows it, such as {@code tier2_explosive_charge}. */
    static final String CHARGE_PREFIX = "tier";

    /** The suffix of a charge's component id. */
    static final String CHARGE_SUFFIX = "_explosive_charge";

    /** The device type the command centers are saved under in {@code artillery.yml}. */
    public static final String COMMAND_CENTER_TYPE = "command-center";

    /** The marker on the entities of a piece, so entities left behind by an earlier run are found and removed. */
    static final NamespacedKey MARK = new NamespacedKey(ItemFactory.NAMESPACE, "artillery");

    private ArtilleryIds() {
    }

    /** The component id of the item of a level. */
    public static String itemOf(ArtilleryLevel level) {
        return ITEM_PREFIX + level.number();
    }

    /** The component id of the item of a shell, such as {@code cluster_shell}, which materials.yml defines. */
    public static String shellItemOf(ArtilleryShell shell) {
        return shell.name().toLowerCase(Locale.ROOT) + SHELL_SUFFIX;
    }

    /** The component id of the item of a charge tier, such as {@code tier1_explosive_charge}, which materials.yml defines. */
    public static String chargeItemOf(ChargeTier tier) {
        return CHARGE_PREFIX + (tier.ordinal() + 1) + CHARGE_SUFFIX;
    }

    /**
     * The level of a piece's item.
     *
     * @param componentId the component id, such as {@code artillery_2}; null is no piece
     * @return the level, or empty when the id is no piece's item
     */
    public static Optional<ArtilleryLevel> levelOfItem(String componentId) {
        if (componentId == null || !componentId.startsWith(ITEM_PREFIX)) {
            return Optional.empty();
        }
        String number = componentId.substring(ITEM_PREFIX.length());
        for (ArtilleryLevel level : ArtilleryLevel.values()) {
            if (String.valueOf(level.number()).equals(number)) {
                return Optional.of(level);
            }
        }
        return Optional.empty();
    }

    /** True when the component id is the item of a piece. */
    public static boolean isItem(String componentId) {
        return levelOfItem(componentId).isPresent();
    }

    /** True when the component id is the item of a command center. */
    public static boolean isCommandCenter(String componentId) {
        return COMMAND_CENTER.equals(componentId);
    }
}
