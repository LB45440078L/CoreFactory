package io.corefactory.core.config;

import org.bukkit.Material;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The typed form of {@code config.yml}. Every value has a default, so a missing key never breaks the plugin.
 *
 * @param debug                   verbose console output
 * @param disabledWorlds          lower-case world names where TNT can neither be placed nor ignited
 * @param maxChainDepth           longest chain of detonations one ignition may start
 * @param maxDetonationsPerSecond global cap on detonations, a guard against runaway chains
 * @param blocksPerTick           block writes applied per tick by the staged pipeline
 * @param instantThreshold        change sets up to this size are applied in one go
 * @param chainIgniteDelayTicks   delay before a vanilla explosion ignites a custom TNT it reached
 * @param particlesPerTick        particle packets sent per tick across all effects
 * @param effectViewDistance      blocks within which players receive sounds and particles
 * @param unbreakable             materials no blast may change
 * @param protectFromPistons      cancel piston moves that would carry a placed TNT away from its display
 * @param customTextures          show the custom item model on placed TNT through an item display
 * @param displayScale            scale of the item display, slightly above 1 to hide the vanilla block
 * @param directionFromPlayer     record the placing player's facing as the TNT's direction
 * @param saveIntervalTicks       how often placed TNT is written to disk
 * @param dataFileName            file name of the placed TNT registry, inside the data folder
 * @param lockMaxAttempts         wrong PINs on one locked nuclear TNT before its keypad is locked out
 * @param lockoutSeconds          length of that lockout, in seconds
 */
public record CoreSettings(
        boolean debug,
        Set<String> disabledWorlds,
        int maxChainDepth,
        int maxDetonationsPerSecond,
        int blocksPerTick,
        int instantThreshold,
        int chainIgniteDelayTicks,
        int particlesPerTick,
        double effectViewDistance,
        Set<Material> unbreakable,
        boolean protectFromPistons,
        boolean customTextures,
        float displayScale,
        boolean directionFromPlayer,
        int saveIntervalTicks,
        String dataFileName,
        int lockMaxAttempts,
        int lockoutSeconds
) {

    /**
     * Reads the settings from the plain data of {@code config.yml}.
     *
     * @param config the root of the configuration; a null or empty map gives every default
     */
    public static CoreSettings from(Map<String, Object> config) {
        Map<String, Object> settings = ConfigValues.section(config, "settings");
        Map<String, Object> performance = ConfigValues.section(config, "performance");
        Map<String, Object> protection = ConfigValues.section(config, "protection");
        Map<String, Object> display = ConfigValues.section(config, "display");
        Map<String, Object> placement = ConfigValues.section(config, "placement");
        Map<String, Object> persistence = ConfigValues.section(config, "persistence");
        Map<String, Object> locks = ConfigValues.section(config, "locks");

        Set<String> disabled = ConfigValues.stringList(settings, "disabled-worlds", List.of()).stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        Set<Material> unbreakable = ConfigValues.stringList(protection, "unbreakable-materials", List.of()).stream()
                .map(Material::matchMaterial)
                .filter(material -> material != null)
                .collect(Collectors.toUnmodifiableSet());

        return new CoreSettings(
                ConfigValues.bool(settings, "debug", false),
                disabled,
                Math.max(1, ConfigValues.integer(settings, "max-chain-depth", 6)),
                Math.max(1, ConfigValues.integer(settings, "max-detonations-per-second", 60)),
                Math.max(500, ConfigValues.integer(performance, "blocks-per-tick", 25_000)),
                Math.max(0, ConfigValues.integer(performance, "instant-threshold", 4_000)),
                Math.max(0, ConfigValues.integer(performance, "chain-ignite-delay-ticks", 6)),
                Math.max(50, ConfigValues.integer(performance, "particles-per-tick", 1_500)),
                Math.max(8.0, ConfigValues.number(performance, "effect-view-distance", 96.0)),
                unbreakable,
                ConfigValues.bool(protection, "protect-from-pistons", true),
                ConfigValues.bool(display, "custom-textures", true),
                (float) ConfigValues.number(display, "display-scale", 1.002),
                ConfigValues.bool(placement, "direction-from-player", true),
                Math.max(20, ConfigValues.integer(persistence, "save-interval-seconds", 300) * 20),
                ConfigValues.string(persistence, "file", "placed-tnt.yml"),
                Math.max(1, ConfigValues.integer(locks, "max-attempts", 3)),
                Math.max(0, ConfigValues.integer(locks, "lockout-seconds", 30))
        );
    }
}
