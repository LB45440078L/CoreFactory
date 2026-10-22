package io.corefactory.spigot.guide;

import io.corefactory.api.TntDefinition;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Machine;
import io.corefactory.core.process.Portion;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessDefinition;
import io.corefactory.core.process.Product;
import io.corefactory.core.recipe.RecipeSpec;
import io.corefactory.core.research.ResearchTree;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * The eight science chapters of the guide book. The prose is written here, but every number, name, recipe, cost and
 * time is read from the loaded data, so the book cannot drift from the files.
 */
public final class ScienceGuide {

    /**
     * The data the chapters are written from.
     *
     * @param tree       the research tree
     * @param processes  the machines and their processes
     * @param ores       the ores
     * @param tnts       the TNT types
     * @param components the components and machines, by id
     */
    public record Source(ResearchTree tree, ProcessCatalog processes, List<OreDefinition> ores,
                         Collection<TntDefinition> tnts, Map<String, ComponentDefinition> components) {
    }

    /** One chapter: its title, then its paragraphs, the first of which is the title line. */
    public record Chapter(String title, List<String> paragraphs) {
    }

    private static final String TITLE = "&6&l";
    private static final String HEADING = "&e";
    private static final String BODY = "&7";
    private static final int TICKS_PER_SECOND = 20;
    private static final double MEGATONS_FROM = 0.1;
    private static final double KILOTONS_FROM = 1.0;

    private static final Map<String, String> WHY = Map.of(
            "uraninite_ore", "The main uranium ore. Black, pitchy, faintly warm.",
            "pitchblende_ore", "Almost pure uranium oxide.",
            "monazite_ore", "Rare earths and thorium.",
            "thorite_ore", "Thorium-232, the fertile isotope.",
            "galena_ore", "Lead, for shielding.",
            "cinnabar_ore", "Mercury. Roasting frees it.",
            "fluorite_ore", "Fluorine, for converting uranium into a gas.");

    private final Source source;

    private ScienceGuide(Source source) {
        this.source = source;
    }

    /** The eight chapters, in the order of docs/SCIENCE.md. */
    public static List<Chapter> chapters(Source source) {
        ScienceGuide guide = new ScienceGuide(source);
        return List.of(
                guide.ore(),
                guide.uranium(),
                guide.breeding(),
                guide.hydrogen(),
                guide.exotic(),
                guide.research(),
                guide.warheads(),
                guide.machines());
    }

    private Chapter ore() {
        List<String> text = new ArrayList<>();
        text.add(BODY + source.ores().size() + " ores are laid on top of the vanilla terrain. Each chunk is filled from "
                + "the world seed, so the same seed gives the same deposits.");
        text.add("");
        for (OreDefinition ore : source.ores()) {
            String range = ore.dropMin() == ore.dropMax() ? String.valueOf(ore.dropMin())
                    : ore.dropMin() + " to " + ore.dropMax();
            text.add(HEADING + plain(ore.name()));
            text.add(BODY + "Found from y " + ore.minY() + " to y " + ore.maxY() + ". " + WHY.getOrDefault(ore.id(), "")
                    + " Mining gives " + range + " " + name(ore.drop()) + ".");
            text.add("");
        }
        text.add(BODY + "Run /cf geology deposits [radius] to list the deposits near you and what each has left.");
        return chapter("1. Find the ore", text);
    }

    private Chapter uranium() {
        List<String> text = new ArrayList<>();
        text.add(BODY + "Uranium must be concentrated, turned into a gas, and enriched until its U-235 is a larger share. "
                + "Each step is a machine.");
        text.add("");
        text.add(HEADING + "Crusher");
        text.add(BODY + "Makes crushed ore from raw ore, with one recipe for each ore. For example: " + first("crusher"));
        text.add("");
        text.add(HEADING + "Roasting kiln");
        text.add(BODY + "Roasts crushed uranium ore with coal or charcoal as fuel. " + first("roaster"));
        text.add("");
        text.add(HEADING + "Centrifugal separator");
        text.add(BODY + "Separates calcine by density into yellowcake and tailings. " + first("separator"));
        text.add("");
        text.add(HEADING + "Fluorination converter");
        text.add(BODY + "Makes UF6 gas from yellowcake and crushed fluorite. " + first("converter"));
        text.add("");
        text.add(HEADING + "Enrichment cascade");
        text.add(BODY + "Enriches the UF6 in a bank of centrifuges. " + first("enrichment_cascade") + " The depleted "
                + "uranium is kept as a by-product.");
        return chapter("2. The uranium chain", text);
    }

    private Chapter breeding() {
        List<String> text = new ArrayList<>();
        text.add(BODY + "Thorium and monazite do not split on their own. They breed fissile isotopes in the neutron "
                + "transmuter, which takes a flux cell with its feed. The chance of each product is shown in its line.");
        text.add("");
        for (ProcessDefinition process : source.processes().processesFor("transmuter")) {
            text.add(BODY + render(process));
            text.add("");
        }
        text.add(BODY + "A plutonium pit is pressed from plutonium oxide: " + recipe("plutonium_pit") + ".");
        return chapter("3. Thorium, U-233 and plutonium", text);
    }

    private Chapter hydrogen() {
        List<String> text = new ArrayList<>();
        text.add(BODY + "The purification still splits water into a deuterium cell, and refines iron and coal into purified "
                + "iron and high-purity graphite.");
        text.add("");
        for (ProcessDefinition process : source.processes().processesFor("purifier")) {
            text.add(BODY + render(process));
            text.add("");
        }
        text.add(BODY + "The particle accelerator makes antiprotons from deuterium and a superconducting magnet, and "
                + "neutronium from flux cells.");
        text.add("");
        for (ProcessDefinition process : source.processes().processesFor("particle_accelerator")) {
            text.add(BODY + render(process));
            text.add("");
        }
        text.add(BODY + "The antimatter condenser holds antiprotons in a field until they condense.");
        text.add("");
        for (ProcessDefinition process : source.processes().processesFor("antimatter_condenser")) {
            text.add(BODY + render(process));
        }
        return chapter("4. Hydrogen and particles", text);
    }

    private Chapter exotic() {
        List<String> text = new ArrayList<>();
        text.add(BODY + "The accelerator can run neutron-rich material into a neutronium slug, which is heavier than the "
                + "rest of your inventory together.");
        text.add("");
        text.add(BODY + first("particle_accelerator", "neutronium_slug"));
        text.add("");
        text.add(BODY + "The tesseract lattice is built by hand. It is the hardest item to make: "
                + recipe("tesseract_lattice") + ".");
        return chapter("5. Exotic matter", text);
    }

    private Chapter research() {
        List<String> text = new ArrayList<>();
        text.add(BODY + "The research tree is one line. Each node needs the one before it, and no node can be skipped. "
                + "A node is finished by feeding its packs into a research console.");
        text.add("");
        text.add(BODY + "The spectrometer reads a sample and prints a pack:");
        for (ProcessDefinition process : source.processes().processesFor("spectrometer")) {
            text.add(BODY + render(process));
        }
        text.add("");
        int number = 1;
        for (ResearchTree.Node node : line()) {
            text.add(HEADING + number + ". " + plain(node.name()));
            text.add(BODY + (node.free() ? "No packs. Finished once its requirements are." : "Cost: " + costs(node) + "."));
            number++;
        }
        return chapter("6. Research", text);
    }

    private Chapter warheads() {
        List<String> text = new ArrayList<>();
        text.add(BODY + "Each warhead is unlocked by the research node named under it. Its yield is the energy of the blast.");
        text.add("");
        for (TntDefinition tnt : source.tnts()) {
            if (!"nuclear".equals(tnt.mechanic()) && !"neutron".equals(tnt.mechanic())) {
                continue;
            }
            String gate = source.tree().gatesOf(tnt.id()).stream().findFirst()
                    .flatMap(source.tree()::node).map(node -> plain(node.name())).orElse("no research");
            text.add(HEADING + plain(tnt.displayName()));
            text.add(BODY + "Yield " + yieldText(tnt.settings().getDouble("yield-megatons", 0.0)) + ". Research: " + gate + ".");
            text.add("");
        }
        text.add(BODY + "The largest tiers also show beacon beams to nearby players.");
        text.add("");
        text.add(BODY + "Fallout is the danger after the blast. Wear the radioactive armour and carry a Geiger counter.");
        return chapter("7. Warheads", text);
    }

    private Chapter machines() {
        List<String> text = new ArrayList<>();
        text.add(BODY + "Each machine has its own screen. Put items in the input slots, take them from the output slots, "
                + "and keep fuel in the fuel slot where a machine asks for it. Machines only work in loaded chunks.");
        text.add("");
        for (Machine machine : source.processes().machines()) {
            text.add(HEADING + plain(machine.name()));
            StringBuilder line = new StringBuilder(BODY + "Inputs " + machine.inputSlots() + ", outputs "
                    + machine.outputSlots());
            if (machine.fuelSlots() > 0) {
                line.append(", fuel ").append(machine.fuelSlots());
            }
            line.append(". Placed on ").append(pretty(machine.host())).append(".");
            text.add(line.toString());
            Set<String> makes = new LinkedHashSet<>();
            for (ProcessDefinition process : source.processes().processesFor(machine.id())) {
                for (Product product : process.outputs()) {
                    makes.add(itemName(product.ref()));
                }
            }
            if (!makes.isEmpty()) {
                text.add(BODY + "Makes " + String.join(", ", makes) + ".");
            }
            text.add("");
        }
        return chapter("8. Machines", text);
    }

    /** The first process of a machine, rendered, or empty when the machine has none. */
    private String first(String machineId) {
        List<ProcessDefinition> processes = source.processes().processesFor(machineId);
        return processes.isEmpty() ? "" : render(processes.get(0));
    }

    /** The first process of a machine that makes the item, rendered, or empty when none does. */
    private String first(String machineId, String item) {
        return source.processes().processesFor(machineId).stream()
                .filter(process -> process.outputs().stream().anyMatch(out -> item.equals(out.ref().name())))
                .findFirst().map(this::render).orElse("");
    }

    /** One process as a line: what it takes, what it makes, and how long it runs. */
    String render(ProcessDefinition process) {
        String inputs = process.inputs().stream().map(this::amount).collect(Collectors.joining(" + "));
        String outputs = process.outputs().stream().map(out -> {
            String text = amount(out.ref(), out.amount());
            return out.chance() < 1.0 ? text + " (" + percent(out.chance()) + "%)" : text;
        }).collect(Collectors.joining(", "));
        return inputs + " -> " + outputs + ", " + seconds(process.ticks()) + (process.fuel() ? ", uses one fuel" : "") + ".";
    }

    /** A recipe as ingredient counts, in the order the recipe names them. */
    String recipe(String componentId) {
        ComponentDefinition component = source.components().get(componentId);
        if (component == null || component.recipe() == null) {
            return name(componentId);
        }
        RecipeSpec spec = component.recipe();
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (spec.shaped()) {
            for (String row : spec.pattern()) {
                for (char key : row.toCharArray()) {
                    String ingredient = spec.ingredients().get(key);
                    if (key != ' ' && ingredient != null) {
                        counts.merge(ingredient, 1, Integer::sum);
                    }
                }
            }
        } else {
            for (String ingredient : spec.ingredientList()) {
                counts.merge(ingredient, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .map(entry -> entry.getValue() + " " + ingredient(entry.getKey()))
                .collect(Collectors.joining(", "));
    }

    /**
     * The nodes in the order of the line: each after the nodes it requires. The tree does not keep the file's order,
     * so the line is rebuilt from the requirements.
     */
    private List<ResearchTree.Node> line() {
        List<ResearchTree.Node> ordered = new ArrayList<>();
        Set<String> placed = new HashSet<>();
        boolean progress = true;
        while (progress && ordered.size() < source.tree().nodes().size()) {
            progress = false;
            for (ResearchTree.Node node : source.tree().nodes()) {
                if (!placed.contains(node.id()) && placed.containsAll(node.requires())) {
                    ordered.add(node);
                    placed.add(node.id());
                    progress = true;
                }
            }
        }
        return ordered;
    }

    private String costs(ResearchTree.Node node) {
        Map<String, Integer> sorted = new TreeMap<>();
        node.cost().forEach((pack, count) -> sorted.put(name(pack), count));
        return sorted.entrySet().stream().map(entry -> entry.getValue() + " " + entry.getKey())
                .collect(Collectors.joining(", "));
    }

    private String amount(Portion portion) {
        return amount(portion.ref(), portion.amount());
    }

    private String amount(ItemRef ref, int count) {
        return count + " " + itemName(ref);
    }

    private String ingredient(String raw) {
        if (raw.startsWith("item:") || raw.startsWith("tnt:")) {
            return name(raw.substring(raw.indexOf(':') + 1));
        }
        return pretty(raw.startsWith("material:") ? raw.substring("material:".length()) : raw);
    }

    private String itemName(ItemRef ref) {
        return ref.kind() == ItemRef.Kind.ITEM ? name(ref.name()) : pretty(ref.name());
    }

    /** The display name of a component, machine or TNT type, without colour codes. */
    String name(String id) {
        ComponentDefinition component = source.components().get(id);
        if (component != null) {
            return plain(component.displayName());
        }
        return source.tnts().stream().filter(tnt -> tnt.id().equals(id)).findFirst()
                .map(tnt -> plain(tnt.displayName())).orElseGet(() -> pretty(id));
    }

    static String seconds(int ticks) {
        BigDecimal seconds = BigDecimal.valueOf(ticks).divide(BigDecimal.valueOf(TICKS_PER_SECOND), 1, RoundingMode.HALF_UP);
        return seconds.stripTrailingZeros().toPlainString() + " s";
    }

    static String percent(double chance) {
        return BigDecimal.valueOf(chance * 100).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    /** A yield in the largest unit that keeps it at or above one: megatons, kilotons, or tons. */
    static String yieldText(double megatons) {
        if (megatons >= MEGATONS_FROM) {
            return trimmed(megatons) + " Mt";
        }
        if (megatons * 1000 >= KILOTONS_FROM) {
            return trimmed(megatons * 1000) + " kt";
        }
        return trimmed(megatons * 1_000_000) + " t";
    }

    private static String trimmed(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /** Upper-case snake case as a name: {@code WATER_BUCKET} becomes {@code Water Bucket}. */
    static String pretty(String text) {
        List<String> out = new ArrayList<>();
        for (String word : text.toLowerCase(Locale.ROOT).split("_")) {
            if (!word.isEmpty()) {
                out.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
            }
        }
        return String.join(" ", out);
    }

    static String plain(String coloured) {
        return coloured.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "").trim();
    }

    private static Chapter chapter(String title, List<String> body) {
        List<String> paragraphs = new ArrayList<>();
        paragraphs.add(TITLE + title);
        paragraphs.add("");
        paragraphs.addAll(body);
        return new Chapter(title, paragraphs);
    }
}
