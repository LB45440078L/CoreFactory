package io.corefactory.core.turret;

import java.util.Locale;
import java.util.Optional;

/**
 * The kinds of mob a turret may be set to fire at. A hostile mob that fits none of them is {@link #OTHER}, which no setting targets.
 * Each kind has its name in the {@code turrets.targets} list of config.yml.
 */
public enum TargetKind {

    MONSTER("monsters"),
    PHANTOM("phantoms"),
    SLIME("slimes"),
    GHAST("ghasts"),
    OTHER("");

    private final String configName;

    TargetKind(String configName) {
        this.configName = configName;
    }

    /** The name of this kind in the targets list, such as monsters. Empty for {@link #OTHER}. */
    public String configName() {
        return configName;
    }

    /** The kind named in the targets list, ignoring case, or empty when no kind has that name. */
    public static Optional<TargetKind> ofConfigName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String wanted = name.trim().toLowerCase(Locale.ROOT);
        for (TargetKind kind : values()) {
            if (kind != OTHER && kind.configName.equals(wanted)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
