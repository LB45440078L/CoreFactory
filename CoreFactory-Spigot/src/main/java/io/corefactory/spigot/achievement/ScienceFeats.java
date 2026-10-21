package io.corefactory.spigot.achievement;

import io.corefactory.api.TntDefinition;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.process.Machine;
import io.corefactory.core.research.ResearchTree;
import io.corefactory.spigot.achievement.Achievement.Category;
import io.corefactory.spigot.achievement.Achievement.Trigger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The science feats: mining, making the products of the uranium chain and the hydrogen and antimatter chain, finishing
 * each research node, placing machines, and detonating the three top weapons. Built from the loaded data, so a node or
 * a TNT type that is missing from the files simply has no feat.
 */
public final class ScienceFeats {

    /** The machine whose output the first raw-ore crushing is credited to. */
    static final String CRUSHER = "crusher";

    /** The machine whose output the first research pack is credited to. */
    static final String SPECTROMETER = "spectrometer";

    /** The TNT types of the exotic tiers, each with a first-blast feat. */
    static final List<String> TOP_WEAPONS = List.of("exotic_neutron_bomb", "exotic_antimatter_bomb", "four_dimensional_tnt");

    /** The items whose first making is a feat. */
    static final List<String> PRODUCTS = List.of("yellowcake", "uf6_cylinder", "enriched_uranium", "u233_oxide",
            "plutonium_oxide", "deuterium_cell", "antimatter_condensate");

    /**
     * What the feats are built from.
     *
     * @param tree     the research tree, one feat per node
     * @param machines every machine kind
     * @param ores     every ore, for the mining feat's icon
     * @param tnts     every TNT type
     * @param names    the display name, with colour codes, of each component, machine and TNT type by id
     * @param icons    the icon of each component, machine and TNT type by id, with a fallback for unknown ids
     */
    public record Sources(ResearchTree tree, Collection<Machine> machines, List<OreDefinition> ores,
                          Collection<TntDefinition> tnts, Map<String, String> names,
                          Function<String, ItemStack> icons) {
    }

    private ScienceFeats() {
    }

    public static List<Achievement> build(Sources sources) {
        List<Achievement> list = new ArrayList<>();
        Map<String, String> names = sources.names();
        Function<String, ItemStack> icons = sources.icons();
        String ore = sources.ores().isEmpty() ? "" : sources.ores().get(0).drop();
        list.add(science("mine_ore", "Prospector", "Mine any ore block.", icons.apply(ore), Trigger.MINE,
                AchievementCatalog.ANY, 1, false));

        if (names.containsKey(CRUSHER)) {
            list.add(science("crush_ore", "Crushing Work", "Take crushed ore out of a " + plain(names.get(CRUSHER)) + ".",
                    icons.apply(CRUSHER), Trigger.PRODUCE, CRUSHER, 1, false));
        }
        if (names.containsKey(SPECTROMETER)) {
            list.add(science("first_research_pack", "First Research Pack",
                    "Take a research pack out of a " + plain(names.get(SPECTROMETER)) + ".",
                    icons.apply(SPECTROMETER), Trigger.PRODUCE, SPECTROMETER, 1, false));
        }
        for (String product : PRODUCTS) {
            if (names.containsKey(product)) {
                list.add(science("make_" + product, "Made: " + names.get(product),
                        "Take " + plain(names.get(product)) + " out of a machine.", icons.apply(product), Trigger.MAKE,
                        product, 1, product.equals("enriched_uranium") || product.equals("antimatter_condensate")));
            }
        }

        for (ResearchTree.Node node : sources.tree().nodes()) {
            String first = node.unlocks().isEmpty() ? "" : node.unlocks().get(0);
            list.add(science("research_" + node.id(), "Researched: " + node.name(),
                    "Finish " + plain(node.name()) + " at a Research Console.", icons.apply(first), Trigger.RESEARCH,
                    node.id(), 1, false));
        }

        Machine first = sources.machines().stream().findFirst().orElse(null);
        list.add(science("place_machine", "First Machine", "Place any machine.",
                icons.apply(first == null ? "" : first.id()), Trigger.PLACE, AchievementCatalog.ANY, 1, false));
        if (first != null) {
            list.add(science("place_every_machine", "Full Laboratory", "Place one machine of every kind.",
                    icons.apply(first.id()), Trigger.PLACE_EACH, AchievementCatalog.ANY, sources.machines().size(), true));
        }

        for (String id : TOP_WEAPONS) {
            sources.tnts().stream().filter(tnt -> tnt.id().equals(id)).findFirst().ifPresent(tnt ->
                    list.add(science("blast_" + id, "First Blast: " + tnt.displayName(),
                            "Set off one " + plain(tnt.displayName()) + ".", icons.apply(id), Trigger.DETONATE, id, 1,
                            true)));
        }
        return list;
    }

    private static Achievement science(String id, String title, String description, ItemStack icon, Trigger trigger,
                                       String param, int goal, boolean major) {
        return new Achievement(id, Category.SCIENCE, null, title, description, icon, trigger, param, goal, major);
    }

    private static String plain(String coloured) {
        return AchievementCatalog.plain(coloured);
    }

    /** The icon used when an id has none. */
    public static ItemStack fallbackIcon() {
        return new ItemStack(Material.ENCHANTED_BOOK);
    }
}
