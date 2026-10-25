package io.corefactory.core.config;

import io.corefactory.api.MechanicSettings;
import io.corefactory.api.TntDefinition;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.recipe.RecipeSpec;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Reads {@code tnts.yml} and {@code items.yml} into immutable definitions. The files arrive as plain maps and lists, as a YAML
 * parser gives them. Bad entries are reported and skipped, so one typo never stops the rest of the configuration from loading.
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

    /**
     * @param tntFile     the whole of {@code tnts.yml} as plain data, with its {@code tnts:} section; null when the file is absent
     * @param itemFile    the whole of {@code items.yml} as plain data, with its {@code components:} section; may be null
     * @param knownMechanics the mechanic ids a TNT type may name
     * @param log         receives one warning per skipped entry
     */
    public static Definitions parse(Map<String, Object> tntFile, Map<String, Object> itemFile,
                                    Set<String> knownMechanics, Logger log) {
        Map<String, ComponentDefinition> components = parseComponents(itemFile, log);
        Map<String, TntDefinition> tnts = new LinkedHashMap<>();
        Map<String, RecipeSpec> recipes = new LinkedHashMap<>();

        Map<String, Object> root = ConfigValues.optionalSection(tntFile, "tnts");
        if (root == null) {
            log.warning("tnts.yml has no 'tnts:' section; no TNT types were loaded.");
            return new Definitions(tnts, recipes, components);
        }
        for (String id : root.keySet()) {
            Map<String, Object> section = ConfigValues.optionalSection(root, id);
            if (section == null) {
                log.warning("tnts.yml: '" + id + "' is not a section and was skipped.");
                continue;
            }
            String mechanic = ConfigValues.string(section, "mechanic", "standard").toLowerCase(Locale.ROOT);
            if (!knownMechanics.contains(mechanic)) {
                log.warning("tnts.yml: '" + id + "' uses unknown mechanic '" + mechanic + "' and was skipped.");
                continue;
            }
            Set<String> effects = new LinkedHashSet<>();
            for (String effect : ConfigValues.stringList(section, "effects", List.of())) {
                String name = effect.toLowerCase(Locale.ROOT);
                if (KNOWN_EFFECTS.contains(name)) {
                    effects.add(name);
                } else {
                    log.warning("tnts.yml: '" + id + "' has unknown effect '" + effect + "' (ignored).");
                }
            }
            Material material = Material.matchMaterial(ConfigValues.string(section, "material", "TNT"));
            if (material == null) {
                log.warning("tnts.yml: '" + id + "' has an unknown material; TNT is used instead.");
                material = Material.TNT;
            }
            TntDefinition definition = new TntDefinition(
                    id,
                    ConfigValues.string(section, "name", id),
                    ConfigValues.stringList(section, "lore", List.of()),
                    material,
                    Math.max(1, ConfigValues.integer(section, "fuse-ticks", 80)),
                    Math.max(0.5, ConfigValues.number(section, "radius", 4.0)),
                    mechanic,
                    new MechanicSettings(ConfigValues.section(section, "settings")),
                    effects);
            tnts.put(id, definition);

            RecipeSpec recipe = parseRecipe(ConfigValues.optionalSection(section, "recipe"), log, id);
            if (recipe != null) {
                recipes.put(id, recipe);
            }
        }
        return new Definitions(tnts, recipes, components);
    }

    private static Map<String, ComponentDefinition> parseComponents(Map<String, Object> itemFile, Logger log) {
        Map<String, ComponentDefinition> out = new LinkedHashMap<>();
        Map<String, Object> root = ConfigValues.optionalSection(itemFile, "components");
        if (root == null) {
            return out;
        }
        for (String id : root.keySet()) {
            Map<String, Object> section = ConfigValues.optionalSection(root, id);
            if (section == null) {
                continue;
            }
            Material material = Material.matchMaterial(ConfigValues.string(section, "material", "PAPER"));
            if (material == null) {
                log.warning("items.yml: '" + id + "' has an unknown material and was skipped.");
                continue;
            }
            RecipeSpec recipe = parseRecipe(ConfigValues.optionalSection(section, "recipe"), log, id);
            out.put(id, new ComponentDefinition(id, ConfigValues.string(section, "name", id),
                    ConfigValues.stringList(section, "lore", List.of()), material, recipe));
        }
        return out;
    }

    private static RecipeSpec parseRecipe(Map<String, Object> section, Logger log, String id) {
        if (section == null) {
            return null;
        }
        int amount = Math.max(1, ConfigValues.integer(section, "amount", 1));
        String type = ConfigValues.string(section, "type", "shaped").toLowerCase(Locale.ROOT);
        if (type.equals("shapeless")) {
            return new RecipeSpec(false, List.of(), Map.of(), ConfigValues.stringList(section, "ingredients", List.of()), amount);
        }
        if (!type.equals("shaped")) {
            log.warning("Recipe of '" + id + "' has unknown type '" + type + "' and was skipped.");
            return null;
        }
        Map<Character, String> ingredients = new HashMap<>();
        Map<String, Object> map = ConfigValues.optionalSection(section, "ingredients");
        if (map != null) {
            for (String key : map.keySet()) {
                String symbol = ConfigValues.string(map, key, null);
                if (key.length() == 1 && symbol != null) {
                    ingredients.put(key.charAt(0), symbol);
                } else {
                    log.warning("Recipe of '" + id + "' has an invalid ingredient key '" + key + "'.");
                }
            }
        }
        return new RecipeSpec(true, ConfigValues.stringList(section, "pattern", List.of()), ingredients, List.of(), amount);
    }
}
