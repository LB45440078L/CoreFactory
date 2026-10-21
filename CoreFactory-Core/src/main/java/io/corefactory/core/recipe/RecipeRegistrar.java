package io.corefactory.core.recipe;

import io.corefactory.api.TntDefinition;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.item.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registers the crafting recipes of components and TNT with the server, and removes them again on reload.
 */
public final class RecipeRegistrar {

    private final Plugin plugin;
    private final ItemFactory items;
    private final Logger log;
    private final Set<NamespacedKey> registered = new HashSet<>();

    // The last inputs, so the recipes can be registered again when the server drops them (a data reload does).
    private Map<String, TntDefinition> lastTnts;
    private Map<String, RecipeSpec> lastTntRecipes;
    private Map<String, ComponentDefinition> lastComponents;

    public RecipeRegistrar(Plugin plugin, ItemFactory items) {
        this.plugin = plugin;
        this.items = items;
        this.log = plugin.getLogger();
    }

    /** Removes every recipe this registrar added. */
    public void unregisterAll() {
        for (NamespacedKey key : registered) {
            Bukkit.removeRecipe(key);
        }
        registered.clear();
    }

    /**
     * Registers all recipes.
     *
     * @return the number of recipes successfully registered
     */
    /**
     * Registers the recipes again if the server has dropped any of them. Running a data reload (a datapack being enabled or
     * disabled, for example) removes recipes added through the Bukkit API.
     *
     * @return {@code true} if the recipes were registered again
     */
    public boolean restoreIfMissing() {
        if (lastTnts == null || registered.isEmpty()) {
            return false;
        }
        boolean missing = registered.stream().anyMatch(key -> Bukkit.getRecipe(key) == null);
        if (missing) {
            registerAll(lastTnts, lastTntRecipes, lastComponents);
        }
        return missing;
    }

    public int registerAll(Map<String, TntDefinition> tnts, Map<String, RecipeSpec> tntRecipes,
                           Map<String, ComponentDefinition> components) {
        lastTnts = tnts;
        lastTntRecipes = tntRecipes;
        lastComponents = components;
        unregisterAll();
        int count = 0;
        for (ComponentDefinition component : components.values()) {
            if (component.recipe() != null
                    && register("component_" + component.id(), component.recipe(),
                    items.component(component, component.recipe().amount()), components, tnts)) {
                count++;
            }
        }
        for (Map.Entry<String, RecipeSpec> entry : tntRecipes.entrySet()) {
            TntDefinition definition = tnts.get(entry.getKey());
            if (definition == null) {
                continue;
            }
            ItemStack result = items.tnt(definition, entry.getValue().amount());
            if (register("tnt_" + entry.getKey(), entry.getValue(), result, components, tnts)) {
                count++;
            }
        }
        return count;
    }

    private boolean register(String keyName, RecipeSpec spec, ItemStack result,
                             Map<String, ComponentDefinition> components, Map<String, TntDefinition> tnts) {
        NamespacedKey key = new NamespacedKey(ItemFactory.NAMESPACE, keyName);
        try {
            Recipe recipe = spec.shaped()
                    ? shaped(key, spec, result, components, tnts)
                    : shapeless(key, spec, result, components, tnts);
            if (recipe == null) {
                return false;
            }
            Bukkit.addRecipe(recipe);
            registered.add(key);
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.log(Level.WARNING, "Recipe " + keyName + " is invalid and was skipped: " + e.getMessage());
            return false;
        }
    }

    private Recipe shaped(NamespacedKey key, RecipeSpec spec, ItemStack result,
                          Map<String, ComponentDefinition> components, Map<String, TntDefinition> tnts) {
        if (spec.pattern().isEmpty() || spec.pattern().size() > 3) {
            log.warning("Shaped recipe " + key.getKey() + " needs 1 to 3 pattern rows.");
            return null;
        }
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(spec.pattern().toArray(String[]::new));
        for (Map.Entry<Character, String> ingredient : spec.ingredients().entrySet()) {
            RecipeChoice choice = choice(ingredient.getValue(), components, tnts);
            if (choice == null) {
                log.warning("Recipe " + key.getKey() + " has an unknown ingredient: " + ingredient.getValue());
                return null;
            }
            recipe.setIngredient(ingredient.getKey(), choice);
        }
        return recipe;
    }

    private Recipe shapeless(NamespacedKey key, RecipeSpec spec, ItemStack result,
                             Map<String, ComponentDefinition> components, Map<String, TntDefinition> tnts) {
        if (spec.ingredientList().isEmpty() || spec.ingredientList().size() > 9) {
            log.warning("Shapeless recipe " + key.getKey() + " needs 1 to 9 ingredients.");
            return null;
        }
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        List<RecipeChoice> choices = new ArrayList<>();
        for (String ingredient : spec.ingredientList()) {
            RecipeChoice choice = choice(ingredient, components, tnts);
            if (choice == null) {
                log.warning("Recipe " + key.getKey() + " has an unknown ingredient: " + ingredient);
                return null;
            }
            choices.add(choice);
        }
        choices.forEach(recipe::addIngredient);
        return recipe;
    }

    /** Resolves an ingredient string to a choice, or {@code null} if it names nothing that exists. */
    private RecipeChoice choice(String text, Map<String, ComponentDefinition> components,
                                Map<String, TntDefinition> tnts) {
        String ingredient = text.trim();
        if (ingredient.startsWith("item:")) {
            ComponentDefinition component = components.get(ingredient.substring(5));
            return component == null ? null
                    : new RecipeChoice.ExactChoice(items.component(component, 1));
        }
        if (ingredient.startsWith("tnt:")) {
            TntDefinition definition = tnts.get(ingredient.substring(4));
            return definition == null ? null
                    : new RecipeChoice.ExactChoice(items.tnt(definition, 1));
        }
        String materialName = ingredient.startsWith("material:") ? ingredient.substring(9) : ingredient;
        Material material = Material.matchMaterial(materialName);
        return material == null ? null : new RecipeChoice.MaterialChoice(material);
    }

    /** The plugin that owns the recipe keys. */
    public Plugin plugin() {
        return plugin;
    }
}
