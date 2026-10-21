package io.corefactory.spigot.gui;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import io.corefactory.core.item.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Shows the crafting recipe of one TNT type or component, laid out as the crafting grid: the ingredients in their
 * 3 x 3 places and the result on the right. Read-only.
 *
 * <p>An ingredient that is one of the plugin's own items and has a recipe of its own opens that recipe, one level
 * deeper. The BACK button returns to the view that opened this one; the first view goes back to the catalogue.
 */
public final class RecipeGui implements MenuHolder {

    /** The nine ingredient slots of the 3 x 3 grid, row by row. */
    private static final int[] GRID = {11, 12, 13, 20, 21, 22, 29, 30, 31};
    private static final int ARROW = 23;
    private static final int RESULT = 25;
    private static final int INFO = 4;
    private static final int BACK = 40;
    private static final int SIZE = 45;

    private final CoreFactoryCore core;
    private final RecipeGui parent;
    private final String id;
    private Inventory inventory;

    private RecipeGui(CoreFactoryCore core, RecipeGui parent, String id) {
        this.core = core;
        this.parent = parent;
        this.id = id;
    }

    /**
     * Opens the recipe of {@code id} for {@code player}, as the top view of a new chain.
     *
     * @return {@code false} if the id has no registered recipe
     */
    public static boolean open(CoreFactoryCore core, Player player, String id) {
        return show(core, player, id, null);
    }

    /** Opens the recipe of {@code id}; {@code parent} is the view BACK returns to, or null for the catalogue. */
    private static boolean show(CoreFactoryCore core, Player player, String id, RecipeGui parent) {
        Recipe recipe = recipeFor(id);
        if (recipe == null) {
            return false;
        }
        RecipeGui gui = new RecipeGui(core, parent, id);
        gui.render(recipe, player);
        return true;
    }

    private void render(Recipe recipe, Player player) {
        ItemStack result = recipe.getResult();
        String name = result.getItemMeta() != null && result.getItemMeta().hasDisplayName()
                ? result.getItemMeta().getDisplayName() : id;
        inventory = Bukkit.createInventory(this, SIZE, Messages.color("&8Recipe: ") + name);

        ItemStack filler = pane();
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        List<ItemStack> ingredients = ingredients(recipe);
        for (int i = 0; i < Math.min(GRID.length, ingredients.size()); i++) {
            inventory.setItem(GRID[i], linked(ingredients.get(i)));
        }
        inventory.setItem(ARROW, labelled(Material.ARROW, "&7Crafts into"));
        inventory.setItem(RESULT, result.clone());
        inventory.setItem(INFO, labelled(Material.BOOK, kind(recipe)));
        inventory.setItem(BACK, labelled(Material.SPECTRAL_ARROW, parent == null ? "&c« Catalogue" : "&c« Back"));
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void click(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        if (slot == BACK) {
            back(player);
            return;
        }
        if (!isGridSlot(slot)) {
            return;
        }
        ItemStack ingredient = inventory.getItem(slot);
        if (ingredient == null) {
            return;
        }
        Optional<String> ingredientId = core.tntIdOf(ingredient).or(() -> core.items().componentId(ingredient));
        ingredientId.ifPresent(next -> show(core, player, next, this));
    }

    /** Returns to the view that opened this one, or to the catalogue when this is the top view. */
    private void back(Player player) {
        if (parent != null) {
            parent.render(recipeFor(parent.id), player);
        } else {
            CatalogGui.create(core).open(player);
        }
    }

    private static boolean isGridSlot(int slot) {
        for (int gridSlot : GRID) {
            if (gridSlot == slot) {
                return true;
            }
        }
        return false;
    }

    private static Recipe recipeFor(String id) {
        Recipe tnt = Bukkit.getRecipe(new NamespacedKey(ItemFactory.NAMESPACE, "tnt_" + id));
        if (tnt != null) {
            return tnt;
        }
        return Bukkit.getRecipe(new NamespacedKey(ItemFactory.NAMESPACE, "component_" + id));
    }

    /** The ingredients in grid order, as items the player can read. Empty grid cells are left out. */
    private static List<ItemStack> ingredients(Recipe recipe) {
        List<ItemStack> out = new ArrayList<>();
        if (recipe instanceof ShapedRecipe shaped) {
            String[] rows = shaped.getShape();
            Map<Character, RecipeChoice> choices = shaped.getChoiceMap();
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    ItemStack item = null;
                    if (row < rows.length && col < rows[row].length()) {
                        RecipeChoice choice = choices.get(rows[row].charAt(col));
                        if (choice != null) {
                            item = choice.getItemStack();
                        }
                    }
                    out.add(item);
                }
            }
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            for (RecipeChoice choice : shapeless.getChoiceList()) {
                out.add(choice.getItemStack());
            }
        }
        return out;
    }

    /**
     * A copy of an ingredient that says so when it opens a recipe of its own. Vanilla ingredients are left as they are.
     */
    private ItemStack linked(ItemStack ingredient) {
        if (ingredient == null) {
            return null;
        }
        Optional<String> ingredientId = core.tntIdOf(ingredient).or(() -> core.items().componentId(ingredient));
        if (ingredientId.isEmpty() || recipeFor(ingredientId.get()) == null) {
            return ingredient;
        }
        ItemStack copy = ingredient.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>(meta.getLore() == null ? List.of() : meta.getLore());
            lore.add(Messages.color("&eClick: &7open this item's recipe"));
            meta.setLore(lore);
            copy.setItemMeta(meta);
        }
        return copy;
    }

    private static String kind(Recipe recipe) {
        return recipe instanceof ShapedRecipe ? "&8Shaped: &7the grid matters" : "&8Shapeless: &7any arrangement";
    }

    private static ItemStack pane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static ItemStack labelled(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(name));
            item.setItemMeta(meta);
        }
        return item;
    }
}
