package io.corefactory.core.recipe;

import java.util.List;
import java.util.Map;

/**
 * A crafting recipe as written in {@code tnts.yml} or {@code items.yml}.
 *
 * <p>Shaped recipes use {@link #pattern()} (up to three rows of three characters, a space meaning empty) and
 * {@link #ingredients()}. Shapeless recipes use {@link #ingredientList()}. Ingredients are written as
 * {@code material:NAME} for vanilla items, {@code item:<component id>} for components and
 * {@code tnt:<tnt id>} for TNT items. A bare name is read as a vanilla material.
 *
 * @param shaped         {@code true} for a shaped recipe, {@code false} for shapeless
 * @param pattern        the shape rows, shaped only
 * @param ingredients    character to ingredient, shaped only
 * @param ingredientList ingredients in any order, shapeless only
 * @param amount         how many results one craft yields
 */
public record RecipeSpec(
        boolean shaped,
        List<String> pattern,
        Map<Character, String> ingredients,
        List<String> ingredientList,
        int amount
) {

    public RecipeSpec {
        pattern = List.copyOf(pattern);
        ingredients = Map.copyOf(ingredients);
        ingredientList = List.copyOf(ingredientList);
    }
}
