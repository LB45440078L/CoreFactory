package io.corefactory.core.config;

import io.corefactory.api.MechanicSettings;
import io.corefactory.api.TntDefinition;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.recipe.RecipeSpec;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Reads {@code tnts.yml} and {@code items.yml} into immutable definitions. Bad entries are reported and skipped,
 * so one typo never stops the rest of the configuration from loading.
 */
public final class DefinitionParser {

    /** The effect modifiers a TNT may declare. */
    public static final Set<String> KNOWN_EFFECTS = Set.of("shockwave_ring", "debris", "echo", "silent");

    /**
     * @param tnts        TNT definitions by id
     * @param tntRecipes  recipes of the TNT types that declare one
     * @param components  component definitions by id
     */
    public record Definitions(
            Map<String, TntDefinition> tnts,
            Map<String, RecipeSpec> tntRecipes,
            Map<String, ComponentDefinition> components
    ) {
    }

    private DefinitionParser() {
    }

    public static Definitions parse(ConfigurationSection tntSection, ConfigurationSection itemSection,
                                    Set<String> knownMechanics, Logger log) {
        Map<String, ComponentDefinition> components = parseComponents(itemSection, log);
        Map<String, TntDefinition> tnts = new LinkedHashMap<>();
        Map<String, RecipeSpec> recipes = new LinkedHashMap<>();

        ConfigurationSection root = tntSection == null ? null : tntSection.getConfigurationSection("tnts");
        if (root == null) {
            log.warning("tnts.yml has no 'tnts:' section; no TNT types were loaded.");
            return new Definitions(tnts, recipes, components);
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                log.warning("tnts.yml: '" + id + "' is not a section and was skipped.");
                continue;
            }
            String mechanic = section.getString("mechanic", "standard").toLowerCase(Locale.ROOT);
            if (!knownMechanics.contains(mechanic)) {
                log.warning("tnts.yml: '" + id + "' uses unknown mechanic '" + mechanic + "' and was skipped.");
                continue;
            }
            Set<String> effects = new LinkedHashSet<>();
            for (String effect : section.getStringList("effects")) {
                String name = effect.toLowerCase(Locale.ROOT);
                if (KNOWN_EFFECTS.contains(name)) {
                    effects.add(name);
                } else {
                    log.warning("tnts.yml: '" + id + "' has unknown effect '" + effect + "' (ignored).");
                }
            }
            Material material = Material.matchMaterial(section.getString("material", "TNT"));
            if (material == null) {
                log.warning("tnts.yml: '" + id + "' has an unknown material; TNT is used instead.");
                material = Material.TNT;
            }
            TntDefinition definition = new TntDefinition(
                    id,
                    section.getString("name", id),
                    section.getStringList("lore"),
                    material,
                    Math.max(1, section.getInt("fuse-ticks", 80)),
                    Math.max(0.5, section.getDouble("radius", 4.0)),
                    mechanic,
                    new MechanicSettings(toMap(section.getConfigurationSection("settings"))),
                    effects);
            tnts.put(id, definition);

            RecipeSpec recipe = parseRecipe(section.getConfigurationSection("recipe"), log, id);
            if (recipe != null) {
                recipes.put(id, recipe);
            }
        }
        return new Definitions(tnts, recipes, components);
    }

    private static Map<String, ComponentDefinition> parseComponents(ConfigurationSection itemSection, Logger log) {
        Map<String, ComponentDefinition> out = new LinkedHashMap<>();
        ConfigurationSection root = itemSection == null ? null : itemSection.getConfigurationSection("components");
        if (root == null) {
            return out;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            Material material = Material.matchMaterial(section.getString("material", "PAPER"));
            if (material == null) {
                log.warning("items.yml: '" + id + "' has an unknown material and was skipped.");
                continue;
            }
            RecipeSpec recipe = parseRecipe(section.getConfigurationSection("recipe"), log, id);
            out.put(id, new ComponentDefinition(id, section.getString("name", id),
                    section.getStringList("lore"), material, recipe));
        }
        return out;
    }

    private static RecipeSpec parseRecipe(ConfigurationSection section, Logger log, String id) {
        if (section == null) {
            return null;
        }
        int amount = Math.max(1, section.getInt("amount", 1));
        String type = section.getString("type", "shaped").toLowerCase(Locale.ROOT);
        if (type.equals("shapeless")) {
            return new RecipeSpec(false, List.of(), Map.of(), section.getStringList("ingredients"), amount);
        }
        if (!type.equals("shaped")) {
            log.warning("Recipe of '" + id + "' has unknown type '" + type + "' and was skipped.");
            return null;
        }
        Map<Character, String> ingredients = new HashMap<>();
        ConfigurationSection map = section.getConfigurationSection("ingredients");
        if (map != null) {
            for (String key : map.getKeys(false)) {
                if (key.length() == 1 && map.getString(key) != null) {
                    ingredients.put(key.charAt(0), map.getString(key));
                } else {
                    log.warning("Recipe of '" + id + "' has an invalid ingredient key '" + key + "'.");
                }
            }
        }
        return new RecipeSpec(true, section.getStringList("pattern"), ingredients, List.of(), amount);
    }

    /** Converts a configuration section into plain maps and lists, the form {@link MechanicSettings} reads. */
    static Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (section == null) {
            return out;
        }
        for (String key : section.getKeys(false)) {
            out.put(key, unwrap(section.get(key)));
        }
        return out;
    }

    private static Object unwrap(Object value) {
        if (value instanceof ConfigurationSection section) {
            return toMap(section);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((key, element) -> out.put(String.valueOf(key), unwrap(element)));
            return out;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(DefinitionParser::unwrap).toList();
        }
        return value;
    }
}
