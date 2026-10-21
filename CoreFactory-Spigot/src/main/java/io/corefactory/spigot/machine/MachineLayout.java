package io.corefactory.spigot.machine;

import io.corefactory.core.config.Messages;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The inventory of one machine: its title, its size, which slots are inputs, outputs, fuel, progress cells and the
 * status item, and the glass and decor that fill the rest. Empty input, output and fuel slots show a hint item with a
 * line that says what goes there; the hint is not an item and is never saved or moved.
 */
public final class MachineLayout {

    /** What a slot of the layout is for. */
    public enum Kind {
        INPUT, OUTPUT, FUEL, PROGRESS, STATUS, INFO, FIREBOX, BUTTON, DECOR
    }

    private final String title;
    private final int size;
    private final Map<Integer, Kind> kinds;
    private final Map<Integer, Integer> indexes;
    private final Map<Integer, Material> decor;
    private final List<Integer> inputs;
    private final List<Integer> outputs;
    private final List<Integer> fuel;
    private final List<Integer> progress;
    private final int status;
    private final int info;
    private final int firebox;
    private final int button;
    private final String buttonHint;
    private final List<String> inputHints;
    private final List<String> outputHints;
    private final List<String> fuelHints;
    private final Material progressFill;
    private final Material progressEmpty;

    private MachineLayout(Builder builder) {
        this.title = builder.title;
        this.size = builder.size;
        this.kinds = Map.copyOf(builder.kinds);
        this.indexes = Map.copyOf(builder.indexes);
        this.inputs = List.copyOf(builder.inputs);
        this.outputs = List.copyOf(builder.outputs);
        this.fuel = List.copyOf(builder.fuel);
        this.progress = List.copyOf(builder.progress);
        this.status = builder.status;
        this.info = builder.info;
        this.firebox = builder.firebox;
        this.button = builder.button;
        this.buttonHint = builder.buttonHint;
        this.inputHints = List.copyOf(builder.inputHints);
        this.outputHints = List.copyOf(builder.outputHints);
        this.fuelHints = List.copyOf(builder.fuelHints);
        this.progressFill = builder.progressFill;
        this.progressEmpty = builder.progressEmpty;
        Map<Integer, Material> fixed = new LinkedHashMap<>();
        for (int slot = 0; slot < size; slot++) {
            if (!kinds.containsKey(slot)) {
                fixed.put(slot, builder.frame);
            }
        }
        fixed.putAll(builder.decorItems);
        this.decor = Map.copyOf(fixed);
    }

    /** Starts a layout of {@code rows} rows of nine slots, or the given size when it is a multiple of nine. */
    public static Builder builder(String title, int size) {
        return new Builder(title, size);
    }

    public String title() {
        return title;
    }

    public int size() {
        return size;
    }

    /** The slot of each input, in the order the inputs are numbered. */
    public List<Integer> inputSlots() {
        return inputs;
    }

    public List<Integer> outputSlots() {
        return outputs;
    }

    public List<Integer> fuelSlots() {
        return fuel;
    }

    /** The progress cells, filled from the first in order as the pass runs. */
    public List<Integer> progressSlots() {
        return progress;
    }

    /** The status item's slot, or -1 when the layout has none. */
    public int statusSlot() {
        return status;
    }

    /** The info item's slot, or -1 when the layout has none. */
    public int infoSlot() {
        return info;
    }

    /** The firebox indicator's slot, or -1 when the layout has none. */
    public int fireboxSlot() {
        return firebox;
    }

    /** The resources button's slot, or -1 when the layout has none. */
    public int buttonSlot() {
        return button;
    }

    /** The decor material a slot shows, or null when the slot shows no decor (an input, output, fuel or other kind). */
    public Material decorAt(int slot) {
        return decor.get(slot);
    }

    /**
     * The firebox indicator: blaze powder while the kiln burns fuel, gunpowder while it does not. Its name says what the item
     * is, and its lore says whether it is lit now.
     */
    public ItemStack firebox(boolean lit) {
        ItemStack item = new ItemStack(Firebox.material(lit), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&6Firebox: lit while the kiln burns fuel"));
            meta.setLore(List.of(Messages.color(lit ? "&7Lit: the kiln is burning fuel now." : "&8Unlit: no fuel is burning.")));
            item.setItemMeta(meta);
        }
        return item;
    }

    /** The resources button: a book that names what it opens, with the layout's hint as its lore. */
    public ItemStack buttonItem() {
        ItemStack item = new ItemStack(Material.BOOK, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&bResources"));
            meta.setLore(List.of(Messages.color("&7" + buttonHint)));
            item.setItemMeta(meta);
        }
        return item;
    }

    public Kind kindAt(int slot) {
        return kinds.getOrDefault(slot, Kind.DECOR);
    }

    /** The number of the slot among the slots of its kind, or -1 for a slot that is not an input, output or fuel slot. */
    public int indexAt(int slot) {
        return indexes.getOrDefault(slot, -1);
    }

    /** Draws the glass, decor and progress cells into an empty inventory. */
    public void decorate(Inventory inventory) {
        for (Map.Entry<Integer, Material> entry : decor.entrySet()) {
            inventory.setItem(entry.getKey(), decorItem(entry.getValue()));
        }
        for (int slot : progress) {
            inventory.setItem(slot, progressCell(false));
        }
        if (button >= 0) {
            inventory.setItem(button, buttonItem());
        }
    }

    /** The hint for an empty slot of the given kind and index. */
    public ItemStack hint(Kind kind, int index) {
        List<String> lines;
        Material material;
        String name;
        switch (kind) {
            case INPUT -> {
                lines = pick(inputHints, index, "Input. Put in the items this machine takes.");
                material = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                name = "&bInput";
            }
            case OUTPUT -> {
                lines = pick(outputHints, index, "Output. Products are put here.");
                material = Material.ORANGE_STAINED_GLASS_PANE;
                name = "&6Output";
            }
            case FUEL -> {
                lines = pick(fuelHints, index, "Fuel. Coal or charcoal.");
                material = Material.RED_STAINED_GLASS_PANE;
                name = "&cFuel";
            }
            default -> throw new IllegalArgumentException("No hint for " + kind);
        }
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(name));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7" + lines.get(0)));
            for (int i = 1; i < lines.size(); i++) {
                lore.add(Messages.color("&8" + lines.get(i)));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return ItemRefs.markHint(item);
    }

    /** One progress cell: filled when {@code filled}, otherwise empty. */
    public ItemStack progressCell(boolean filled) {
        ItemStack item = decorItem(filled ? progressFill : progressEmpty);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(filled ? "&aProgress" : "&8Progress"));
            item.setItemMeta(meta);
        }
        return item;
    }

    /** The decor item that stands for an empty glass pane or block, with a blank name. */
    static ItemStack decorItem(Material material) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private static List<String> pick(List<String> hints, int index, String fallback) {
        String text = index >= 0 && index < hints.size() ? hints.get(index) : fallback;
        return List.of(text.split("\n"));
    }

    /** Builds a {@link MachineLayout}. Each slot may be given to one role only. */
    public static final class Builder {

        private final String title;
        private final int size;
        private final Map<Integer, Kind> kinds = new HashMap<>();
        private final Map<Integer, Integer> indexes = new HashMap<>();
        private final Map<Integer, Material> decorItems = new LinkedHashMap<>();
        private final List<Integer> inputs = new ArrayList<>();
        private final List<Integer> outputs = new ArrayList<>();
        private final List<Integer> fuel = new ArrayList<>();
        private final List<Integer> progress = new ArrayList<>();
        private final List<String> inputHints = new ArrayList<>();
        private final List<String> outputHints = new ArrayList<>();
        private final List<String> fuelHints = new ArrayList<>();
        private Material frame = Material.GRAY_STAINED_GLASS_PANE;
        private Material progressFill = Material.LIME_STAINED_GLASS_PANE;
        private Material progressEmpty = Material.GRAY_STAINED_GLASS_PANE;
        private int status = -1;
        private int info = -1;
        private int firebox = -1;
        private int button = -1;
        private String buttonHint = "";

        private Builder(String title, int size) {
            if (size < 9 || size > 54 || size % 9 != 0) {
                throw new IllegalArgumentException("A machine menu has 9 to 54 slots in rows of nine: " + size);
            }
            this.title = Messages.color(title);
            this.size = size;
        }

        /** The glass that fills every slot no role or decor names. */
        public Builder frame(Material material) {
            this.frame = material;
            return this;
        }

        public Builder input(int slot, String hint) {
            claim(slot, Kind.INPUT);
            indexes.put(slot, inputs.size());
            inputs.add(slot);
            inputHints.add(hint);
            return this;
        }

        public Builder output(int slot, String hint) {
            claim(slot, Kind.OUTPUT);
            indexes.put(slot, outputs.size());
            outputs.add(slot);
            outputHints.add(hint);
            return this;
        }

        public Builder fuel(int slot, String hint) {
            claim(slot, Kind.FUEL);
            indexes.put(slot, fuel.size());
            fuel.add(slot);
            fuelHints.add(hint);
            return this;
        }

        /** Progress cells, in the order they fill. The colours are the filled and empty panes. */
        public Builder progress(Material filled, Material empty, int... slots) {
            this.progressFill = filled;
            this.progressEmpty = empty;
            for (int slot : slots) {
                claim(slot, Kind.PROGRESS);
                progress.add(slot);
            }
            return this;
        }

        public Builder status(int slot) {
            claim(slot, Kind.STATUS);
            this.status = slot;
            return this;
        }

        /** The slot of the info item a drill shows its stats in. Nothing can be taken from it. */
        public Builder info(int slot) {
            claim(slot, Kind.INFO);
            this.info = slot;
            return this;
        }

        /** The slot of a kiln's firebox indicator, lit while it burns fuel. Nothing can be taken from it. */
        public Builder firebox(int slot) {
            claim(slot, Kind.FIREBOX);
            this.firebox = slot;
            return this;
        }

        /** The resources button. Clicking it opens the menu the layout's owner gives it; nothing can be taken from it. */
        public Builder button(int slot, String hint) {
            claim(slot, Kind.BUTTON);
            this.button = slot;
            this.buttonHint = hint;
            return this;
        }

        /** Decor that nothing can be taken from: glass, blocks, or items drawn for looks. */
        public Builder decor(Material material, int... slots) {
            for (int slot : slots) {
                checkSlot(slot);
                if (kinds.containsKey(slot)) {
                    throw new IllegalArgumentException("Slot " + slot + " is already used in " + title);
                }
                decorItems.put(slot, material);
            }
            return this;
        }

        public MachineLayout build() {
            return new MachineLayout(this);
        }

        private void claim(int slot, Kind kind) {
            checkSlot(slot);
            if (kinds.containsKey(slot) || decorItems.containsKey(slot)) {
                throw new IllegalArgumentException("Slot " + slot + " is already used in " + title);
            }
            kinds.put(slot, kind);
        }

        private void checkSlot(int slot) {
            if (slot < 0 || slot >= size) {
                throw new IllegalArgumentException("Slot " + slot + " is outside a menu of " + size);
            }
        }
    }
}
