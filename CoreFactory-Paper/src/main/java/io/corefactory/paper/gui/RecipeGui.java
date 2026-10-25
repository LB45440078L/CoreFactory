package io.corefactory.paper.gui;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Machine;
import io.corefactory.core.process.ProcessDefinition;
import io.corefactory.core.text.Text;
import io.corefactory.paper.machine.ItemRefs;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
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
 * Shows how one TNT type or component is made, laid out as the crafting grid: the ingredients in their 3 x 3 places and the result on the
 * right. Read-only.
 *
 * <p>An item made by a machine has no crafting recipe, so its view is its process: the inputs of the pass, laid out in the grid, and the
 * machine that makes it, named on the info button. An ingredient that is one of the plugin's own items and can be made opens its own view,
 * one level deeper. The BACK button returns to the view that opened this one; the first view goes back to the catalogue.
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
    private View view;
    private Inventory inventory;

    private RecipeGui(CoreFactoryCore core, RecipeGui parent, String id) {
        this.core = core;
        this.parent = parent;
        this.id = id;
    }

    /**
     * Opens the recipe of {@code id} for {@code player}, as the top view of a new chain.
     *
     * @return {@code false} if the id has no recipe and no process makes it
     */
    public static boolean open(CoreFactoryCore core, Player player, String id) {
        return show(core, player, id, null);
    }

    /** Opens the recipe of {@code id}; {@code parent} is the view BACK returns to, or null for the catalogue. */
    private static boolean show(CoreFactoryCore core, Player player, String id, RecipeGui parent) {
        View shown = viewFor(core, id);
        if (shown == null) {
            return false;
        }
        RecipeGui gui = new RecipeGui(core, parent, id);
        gui.render(shown, player);
        return true;
    }

    /** What a view shows: the result, the grid's ingredients in order, and the line on the info button. */
    private record View(ItemStack result, List<ItemStack> ingredients, String info) {
    }

    private void render(View shown, Player player) {
        view = shown;
        ItemStack result = shown.result();
        Component name = result.getItemMeta() != null && result.getItemMeta().hasDisplayName()
                ? result.getItemMeta().displayName() : Component.text(id);
        inventory = Bukkit.createInventory(this, SIZE, Text.parse("&8Recipe: ").append(name));

        ItemStack filler = pane();
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        List<ItemStack> ingredients = shown.ingredients();
        for (int i = 0; i < Math.min(GRID.length, ingredients.size()); i++) {
            inventory.setItem(GRID[i], linked(ingredients.get(i)));
        }
        inventory.setItem(ARROW, labelled(Material.ARROW, "&7Crafts into"));
        inventory.setItem(RESULT, result.clone());
        inventory.setItem(INFO, labelled(Material.BOOK, shown.info()));
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
        if (ingredientId.isPresent() && show(core, player, ingredientId.get(), this)) {
            GuiSounds.click(player);
        }
    }

    /** Returns to the view that opened this one, or to the catalogue when this is the top view. */
    private void back(Player player) {
        GuiSounds.back(player);
        if (parent != null) {
            parent.render(parent.view, player);
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

    /** The view of an id: its crafting recipe when it has one, otherwise the process that makes it, or null when neither does. */
    private static View viewFor(CoreFactoryCore core, String id) {
        Recipe recipe = recipeFor(id);
        if (recipe != null) {
            return new View(recipe.getResult(), ingredients(recipe), kind(recipe));
        }
        Optional<ProcessDefinition> process = producerOf(core, id);
        if (process.isEmpty()) {
            return null;
        }
        ItemRefs refs = new ItemRefs(core);
        ItemStack result = refs.create(ItemRef.item(id), 1);
        if (result == null) {
            return null;
        }
        List<ItemStack> ingredients = new ArrayList<>();
        for (var portion : process.get().inputs()) {
            ItemStack stack = refs.create(portion.ref(), portion.amount());
            if (stack != null) {
                ingredients.add(stack);
            }
        }
        return new View(result, ingredients, madeIn(core, process.get()));
    }

    /** The process that makes an item, or empty when none does. */
    private static Optional<ProcessDefinition> producerOf(CoreFactoryCore core, String id) {
        ItemRef item = ItemRef.item(id);
        return core.processes().processes().stream()
                .filter(process -> process.outputs().stream().anyMatch(output -> output.ref().equals(item)))
                .findFirst();
    }

    /** True when an id has a crafting recipe or a process that makes it. */
    private static boolean hasRecipe(CoreFactoryCore core, String id) {
        return recipeFor(id) != null || producerOf(core, id).isPresent();
    }

    /** The info line of a process: the machine that runs it, and how long a pass takes. */
    private static String madeIn(CoreFactoryCore core, ProcessDefinition process) {
        String machine = core.processes().machine(process.machine()).map(Machine::name).orElse(process.machine());
        return "&8Made in: &f" + machine + " &7(" + String.format("%.1f", process.ticks() / 20.0) + " s a pass)";
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
                            item = shown(choice);
                        }
                    }
                    out.add(item);
                }
            }
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            for (RecipeChoice choice : shapeless.getChoiceList()) {
                out.add(shown(choice));
            }
        }
        return out;
    }

    /**
     * The item that stands for a choice in the grid: the first item it accepts. A predicate choice has no item to show.
     */
    private static ItemStack shown(RecipeChoice choice) {
        if (choice instanceof RecipeChoice.ExactChoice exact) {
            return exact.getChoices().isEmpty() ? null : exact.getChoices().get(0);
        }
        if (choice instanceof RecipeChoice.MaterialChoice material) {
            return material.getChoices().isEmpty() ? null : new ItemStack(material.getChoices().get(0));
        }
        if (choice instanceof RecipeChoice.ItemTypeChoice types) {
            return types.itemTypes().resolve(Registry.ITEM).stream().findFirst().map(ItemType::createItemStack).orElse(null);
        }
        return null;
    }

    /**
     * A copy of an ingredient that says so when it opens a recipe of its own. Vanilla ingredients are left as they are.
     */
    private ItemStack linked(ItemStack ingredient) {
        if (ingredient == null) {
            return null;
        }
        Optional<String> ingredientId = core.tntIdOf(ingredient).or(() -> core.items().componentId(ingredient));
        if (ingredientId.isEmpty() || !hasRecipe(core, ingredientId.get())) {
            return ingredient;
        }
        ItemStack copy = ingredient.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
            lore.add(Text.item("&eClick: &7open this item's recipe"));
            meta.lore(lore);
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
            meta.displayName(Text.item(" "));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static ItemStack labelled(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(name));
            item.setItemMeta(meta);
        }
        return item;
    }
}
