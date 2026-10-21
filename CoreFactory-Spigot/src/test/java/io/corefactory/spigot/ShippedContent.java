package io.corefactory.spigot;

import io.corefactory.core.config.DefinitionParser;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.research.ResearchTree;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The shipped data files, parsed the way the plugin parses them at enable, for tests that need the real content.
 */
public final class ShippedContent {

    private static final Logger LOG = Logger.getLogger("ShippedContent");

    /**
     * The parsed content.
     *
     * @param tree        the research tree from research.yml
     * @param processes   the machines and processes from processes.yml
     * @param ores        the ores from ores.yml
     * @param definitions the TNT types and components, from tnts.yml, materials.yml and items.yml
     */
    public record Loaded(ResearchTree tree, ProcessCatalog processes, List<OreDefinition> ores,
                         DefinitionParser.Definitions definitions) {
    }

    private ShippedContent() {
    }

    public static Loaded load() {
        YamlConfiguration tnts = resource("tnts.yml");
        YamlConfiguration items = merged("materials.yml", "items.yml");
        Set<String> mechanics = new HashSet<>();
        ConfigurationSection tntSection = tnts.getConfigurationSection("tnts");
        for (String id : tntSection.getKeys(false)) {
            mechanics.add(tntSection.getConfigurationSection(id).getString("mechanic", "standard")
                    .toLowerCase(Locale.ROOT));
        }
        DefinitionParser.Definitions definitions = DefinitionParser.parse(tnts, items, mechanics, LOG);
        ResearchTree tree = ResearchTree.parse(resource("research.yml"), LOG);
        ProcessCatalog processes = ProcessCatalog.parse(resource("processes.yml"), LOG);
        List<OreDefinition> ores = OreDefinition.parse(resource("ores.yml").getConfigurationSection("ores"), LOG);
        return new Loaded(tree, processes, ores, definitions);
    }

    /** The ids of the components items.yml defines, as opposed to materials.yml. */
    public static Set<String> itemComponentIds() {
        return Set.copyOf(resource("items.yml").getConfigurationSection("components").getKeys(false));
    }

    /** One file's components section, with the components of the other files added, as the plugin merges them. */
    private static YamlConfiguration merged(String... names) {
        YamlConfiguration merged = new YamlConfiguration();
        for (String name : names) {
            ConfigurationSection section = resource(name).getConfigurationSection("components");
            for (String id : section.getKeys(false)) {
                if (!merged.contains("components." + id)) {
                    merged.createSection("components." + id, section.getConfigurationSection(id).getValues(true));
                }
            }
        }
        return merged;
    }

    private static YamlConfiguration resource(String name) {
        try (Reader reader = new InputStreamReader(
                ShippedContent.class.getClassLoader().getResourceAsStream(name), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read " + name, e);
        }
    }
}
