package io.corefactory.core.turret;

import java.util.Locale;
import java.util.Optional;

/**
 * The four rounds a machine gun turret fires. Each has an item id, the id of its component, which is also the key of its values in
 * the {@code turrets.rounds} section of config.yml.
 */
public enum RoundType {

    REGULAR("round_regular", "regular"),
    ARMOR_PIERCING("round_armor_piercing", "armor-piercing"),
    INCENDIARY("round_incendiary", "incendiary"),
    DEPLETED_URANIUM("round_depleted_uranium", "depleted-uranium");

    private final String itemId;
    private final String configKey;

    RoundType(String itemId, String configKey) {
        this.itemId = itemId;
        this.configKey = configKey;
    }

    /** The component id of the round's item, such as round_regular. */
    public String itemId() {
        return itemId;
    }

    /** The key of this round's section under {@code turrets.rounds}, such as armor-piercing. */
    public String configKey() {
        return configKey;
    }

    /** The round of an item id, or empty when the id is no round. */
    public static Optional<RoundType> ofItem(String itemId) {
        if (itemId == null) {
            return Optional.empty();
        }
        for (RoundType type : values()) {
            if (type.itemId.equals(itemId)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /** The round named by a configuration key, ignoring case, or empty when no round has it. */
    public static Optional<RoundType> ofConfigKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        String wanted = key.trim().toLowerCase(Locale.ROOT);
        for (RoundType type : values()) {
            if (type.configKey.equals(wanted)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
