package io.moretntplus.core.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.Locale;
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
        String dataFileName
) {

    public static CoreSettings from(ConfigurationSection config) {
        ConfigurationSection settings = section(config, "settings");
        ConfigurationSection performance = section(config, "performance");
        ConfigurationSection protection = section(config, "protection");
        ConfigurationSection display = section(config, "display");
        ConfigurationSection placement = section(config, "placement");
        ConfigurationSection persistence = section(config, "persistence");

        Set<String> disabled = settings.getStringList("disabled-worlds").stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        Set<Material> unbreakable = protection.getStringList("unbreakable-materials").stream()
                .map(Material::matchMaterial)
                .filter(material -> material != null)
                .collect(Collectors.toUnmodifiableSet());

        return new CoreSettings(
                settings.getBoolean("debug", false),
                disabled,
                Math.max(1, settings.getInt("max-chain-depth", 6)),
                Math.max(1, settings.getInt("max-detonations-per-second", 60)),
                Math.max(500, performance.getInt("blocks-per-tick", 25_000)),
                Math.max(0, performance.getInt("instant-threshold", 4_000)),
                Math.max(0, performance.getInt("chain-ignite-delay-ticks", 6)),
                Math.max(50, performance.getInt("particles-per-tick", 1_500)),
                Math.max(8.0, performance.getDouble("effect-view-distance", 96.0)),
                unbreakable,
                protection.getBoolean("protect-from-pistons", true),
                display.getBoolean("custom-textures", true),
                (float) display.getDouble("display-scale", 1.002),
                placement.getBoolean("direction-from-player", true),
                Math.max(20, persistence.getInt("save-interval-seconds", 300) * 20),
                persistence.getString("file", "placed-tnt.yml")
        );
    }

    /** Returns the named subsection, or an empty one, so callers never null-check. */
    static ConfigurationSection section(ConfigurationSection parent, String key) {
        ConfigurationSection section = parent.getConfigurationSection(key);
        return section != null ? section : new MemoryConfiguration();
    }
}
