package io.corefactory.paper.achievement;

import io.corefactory.api.TntDefinition;
import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.research.ResearchTree;
import io.corefactory.core.text.Text;
import io.corefactory.paper.achievement.Achievement.Category;
import io.corefactory.paper.achievement.Achievement.Trigger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every achievement, built from the loaded TNT types and components, so a new type adds its achievements on reload.
 *
 * <p>Each TNT has three: forge it, light it, and set it off. Each component has two: forge it, and use it as an
 * ingredient. On top of those come counters, special feats, and mastery groups that unlock once a whole set is done.
 */
public final class AchievementCatalog {

    /** Matches any id or any event: used for counters such as "detonate 100 TNT". */
    public static final String ANY = "*";

    /** The group that a mastery achievement with {@code param} {@code *} completes over: every non-group achievement. */
    public static final String EVERYTHING = "*";

    private static final String NUCLEAR_GROUP = "atomic";

    private final List<Achievement> all;
    private final Map<String, Achievement> byId = new LinkedHashMap<>();

    private AchievementCatalog(List<Achievement> all) {
        this.all = List.copyOf(all);
        for (Achievement achievement : this.all) {
            byId.put(achievement.id(), achievement);
        }
    }

    /**
     * @param core       the loaded TNT types and components
     * @param research   the research tree, which gives one science feat per node
     * @param processes  the machines, which give the every-machine feat its goal
     * @param ores       the ores, for the mining feat's icon
     */
    public static AchievementCatalog build(CoreFactoryCore core, ResearchTree research, ProcessCatalog processes,
                                           List<OreDefinition> ores) {
        List<Achievement> list = new ArrayList<>();
        for (TntDefinition tnt : core.definitions()) {
            addTnt(list, core, tnt);
        }
        for (ComponentDefinition component : core.componentDefinitions().values()) {
            addComponent(list, core, component);
        }
        addSpecials(list);
        list.addAll(ScienceFeats.build(scienceSources(core, research, processes, ores)));
        addMastery(list);
        list.sort(Comparator.comparing((Achievement a) -> a.category().ordinal())
                .thenComparing(Achievement::id));
        return new AchievementCatalog(list);
    }

    public List<Achievement> all() {
        return all;
    }

    public List<Achievement> withTrigger(Trigger trigger) {
        return all.stream().filter(a -> a.trigger() == trigger).toList();
    }

    public Achievement byId(String id) {
        return byId.get(id);
    }

    /** The achievements a group completion needs: the members of a named group, or everything but groups. */
    public List<Achievement> members(String group) {
        if (EVERYTHING.equals(group)) {
            return all.stream().filter(a -> a.trigger() != Trigger.GROUP).toList();
        }
        return all.stream().filter(a -> group.equals(a.group())).toList();
    }

    private static ScienceFeats.Sources scienceSources(CoreFactoryCore core, ResearchTree research,
                                                       ProcessCatalog processes, List<OreDefinition> ores) {
        Map<String, String> names = new LinkedHashMap<>();
        core.componentDefinitions().forEach((id, component) -> names.put(id, component.displayName()));
        for (TntDefinition tnt : core.definitions()) {
            names.put(tnt.id(), tnt.displayName());
        }
        return new ScienceFeats.Sources(research, processes.machines(), ores, core.definitions(), names,
                id -> iconOf(core, id));
    }

    /** The item of a TNT type or component, or a fallback when the id is neither. */
    private static ItemStack iconOf(CoreFactoryCore core, String id) {
        Optional<TntDefinition> tnt = core.definition(id);
        if (tnt.isPresent()) {
            return core.items().tnt(tnt.get(), 1);
        }
        ComponentDefinition component = core.componentDefinitions().get(id);
        return component == null ? ScienceFeats.fallbackIcon() : core.items().component(component, 1);
    }

    private static void addTnt(List<Achievement> list, CoreFactoryCore core, TntDefinition tnt) {
        String id = tnt.id();
        String name = tnt.displayName();
        String plain = plain(name);
        boolean nuclear = "nuclear".equals(tnt.mechanic());
        ItemStack icon = core.items().tnt(tnt, 1);
        list.add(new Achievement("craft_" + id, Category.FORGING, "craft_tnt", "Forged: " + name,
                "Craft a " + plain + " from its recipe.", icon, Trigger.CRAFT, id, 1, false));
        list.add(new Achievement("ignite_" + id, Category.LIGHTING, "ignite", "Lit: " + name,
                "Light a placed " + plain + " with fire, flint, or a fire charge.", icon, Trigger.IGNITE, id, 1, false));
        if (nuclear) {
            list.add(new Achievement("detonate_" + id, Category.NUCLEAR, NUCLEAR_GROUP, "Detonated: " + name,
                    "Set off a " + plain + ".", icon, Trigger.DETONATE, id, 1, true));
        } else {
            list.add(new Achievement("detonate_" + id, Category.DETONATION, "detonate", "Detonated: " + name,
                    "Set off a " + plain + ".", icon, Trigger.DETONATE, id, 1, false));
        }
    }

    private static void addComponent(List<Achievement> list, CoreFactoryCore core, ComponentDefinition component) {
        String id = component.id();
        String name = component.displayName();
        String plain = plain(name);
        ItemStack icon = core.items().component(component, 1);
        list.add(new Achievement("forge_" + id, Category.FORGING, "forge_all", "Forged: " + name,
                "Craft a " + plain + ".", icon, Trigger.CRAFT, id, 1, false));
        list.add(new Achievement("use_" + id, Category.USING, "use_components", "Used: " + name,
                "Use " + plain + " as an ingredient in a recipe.", icon, Trigger.USE, id, 1, false));
    }

    /** Counters and one-off feats, each with its own icon. */
    private static void addSpecials(List<Achievement> list) {
        ItemStack flint = new ItemStack(Material.FLINT_AND_STEEL);
        ItemStack tnt = new ItemStack(Material.TNT);
        list.add(special("first_steps", Category.STARTING, "First Steps", "Craft any TNT or component.",
                new ItemStack(Material.CRAFTING_TABLE), Trigger.CRAFT, ANY, 1, false));
        list.add(special("craft_25", Category.FORGING, "Busy Hands", "Craft 25 TNT or components.",
                new ItemStack(Material.ANVIL), Trigger.CRAFT, ANY, 25, false));
        list.add(special("craft_100", Category.FORGING, "Factory Floor", "Craft 100 TNT or components.",
                new ItemStack(Material.IRON_BLOCK), Trigger.CRAFT, ANY, 100, false));
        list.add(special("first_spark", Category.LIGHTING, "First Spark", "Light any TNT.", flint,
                Trigger.IGNITE, ANY, 1, false));
        list.add(special("light_50", Category.LIGHTING, "Pyromaniac", "Light 50 TNT.", flint,
                Trigger.IGNITE, ANY, 50, false));
        list.add(special("boom", Category.DETONATION, "Boom", "Set off your first TNT.", tnt,
                Trigger.DETONATE, ANY, 1, false));
        list.add(special("detonate_10", Category.DETONATION, "Ten Blasts", "Set off 10 TNT.", tnt,
                Trigger.DETONATE, ANY, 10, false));
        list.add(special("detonate_100", Category.DETONATION, "Demolition", "Set off 100 TNT.", tnt,
                Trigger.DETONATE, ANY, 100, false));
        list.add(special("detonate_1000", Category.DETONATION, "Armageddon", "Set off 1000 TNT.", tnt,
                Trigger.DETONATE, ANY, 1000, true));
        list.add(special("link_1", Category.USING, "Wired Up", "Link two placed TNT with the fuse linker.",
                new ItemStack(Material.REDSTONE), Trigger.LINK, ANY, 1, false));
        list.add(special("link_50", Category.USING, "Electrician", "Link 50 placed TNT.",
                new ItemStack(Material.REDSTONE_BLOCK), Trigger.LINK, ANY, 50, false));
        list.add(special("throw_1", Category.USING, "Throw It", "Drop a TNT item. It bursts if it is napalm.",
                new ItemStack(Material.SNOWBALL), Trigger.THROW, ANY, 1, false));
        list.add(special("throw_napalm", Category.USING, "Napalm Pitcher", "Throw a napalm TNT so it bursts on landing.",
                new ItemStack(Material.BLAZE_POWDER), Trigger.THROW, "napalm_tnt", 1, false));
        list.add(special("chain_3", Category.CHAOS, "Domino", "Start a chain of three detonations.",
                new ItemStack(Material.STRING), Trigger.CHAIN, "3", 1, false));
        list.add(special("chain_8", Category.CHAOS, "Cascade", "Start a chain of eight detonations.",
                new ItemStack(Material.TRIPWIRE_HOOK), Trigger.CHAIN, "8", 1, true));
        list.add(special("nuke_1", Category.NUCLEAR, "Mushroom Cloud", "Set off a nuclear weapon.",
                new ItemStack(Material.RED_MUSHROOM), Trigger.NUKE, ANY, 1, true));
        list.add(special("nuke_10", Category.NUCLEAR, "Nuclear Habit", "Set off ten nuclear weapons.",
                new ItemStack(Material.RED_MUSHROOM_BLOCK), Trigger.NUKE, ANY, 10, false));
        list.add(special("village_nuke", Category.CHAOS, "Village Nuker",
                "A nuclear blast that kills at least three villagers near its centre.",
                new ItemStack(Material.EMERALD), Trigger.VILLAGE, ANY, 1, true));
        list.add(special("survive_nuke", Category.NUCLEAR, "Still Standing",
                "Survive a nuclear blast within 1.5 times its radius, ten seconds after it.",
                new ItemStack(Material.GOLDEN_APPLE), Trigger.SURVIVE, ANY, 1, true));
    }

    /** Groups: each unlocks once every member is unlocked. */
    private static void addMastery(List<Achievement> list) {
        list.add(group("mastery_arsenal", "Full Arsenal", "Forge every TNT type.", "craft_tnt",
                new ItemStack(Material.TNT)));
        list.add(group("mastery_armoury", "Full Armoury", "Forge every component.", "forge_all",
                new ItemStack(Material.NETHER_STAR)));
        list.add(group("mastery_pyromaniac", "Every Spark", "Light every TNT type.", "ignite",
                new ItemStack(Material.FLINT_AND_STEEL)));
        list.add(group("mastery_annihilator", "Annihilator", "Set off every TNT type.", "detonate",
                new ItemStack(Material.TNT_MINECART)));
        list.add(group("mastery_atomic", "Atomic Age", "Set off every nuclear tier.", NUCLEAR_GROUP,
                new ItemStack(Material.SHROOMLIGHT)));
        list.add(group("mastery_engineer", "Engineer", "Use every component in a recipe.", "use_components",
                new ItemStack(Material.REDSTONE_BLOCK)));
        list.add(group("mastery_completionist", "Completionist", "Unlock every other achievement.", EVERYTHING,
                new ItemStack(Material.DRAGON_EGG)));
    }

    private static Achievement special(String id, Category category, String title, String description, ItemStack icon,
                                       Trigger trigger, String param, int goal, boolean major) {
        return new Achievement(id, category, null, title, description, icon, trigger, param, goal, major);
    }

    private static Achievement group(String id, String title, String description, String members, ItemStack icon) {
        return new Achievement(id, Category.MASTERY, null, title, description, icon, Trigger.GROUP, members, 1, true);
    }

    /** Removes colour codes, for use in a sentence. */
    static String plain(String coloured) {
        return Text.plain(Text.parse(coloured)).trim();
    }
}
