package io.corefactory.core.item;

import io.corefactory.core.recipe.RecipeSpec;
import org.bukkit.Material;

import java.util.List;

/**
 * A crafting component: a custom item that is not TNT but is needed to craft some of it.
 *
 * @param id          stable lower snake case id
 * @param displayName legacy-coloured display name
 * @param lore        legacy-coloured lore lines
 * @param material    base material of the item
 * @param recipe      how to craft it, or {@code null} when it can only be obtained otherwise
 */
public record ComponentDefinition(
        String id,
        String displayName,
        List<String> lore,
        Material material,
        RecipeSpec recipe
) {

    public ComponentDefinition {
        lore = List.copyOf(lore);
    }
}
