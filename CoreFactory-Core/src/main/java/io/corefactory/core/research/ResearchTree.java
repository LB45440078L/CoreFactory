package io.corefactory.core.research;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * The research tree, read from {@code research.yml}. The tree is a line: a node may only require nodes listed before it,
 * so a player can never reach a later node without the earlier ones. Each gated id (a machine, component or TNT type
 * named in some node's {@code unlocks}) is unlocked by the nodes that list it.
 */
public final class ResearchTree {

    /**
     * One node of the tree.
     *
     * @param id          the node id
     * @param name        the display name, with colour codes
     * @param description one line of flavour text
     * @param requires    the nodes that must be finished first, all listed earlier in the file
     * @param cost        the packs fed in to finish the node, by pack id
     * @param unlocks     the ids this node gates
     */
    public record Node(String id, String name, String description, List<String> requires,
                       Map<String, Integer> cost, List<String> unlocks) {

        public Node {
            requires = List.copyOf(requires);
            cost = Map.copyOf(cost);
            unlocks = List.copyOf(unlocks);
        }

        /** A node with no cost is finished as soon as its requirements are. */
        public boolean free() {
            return cost.isEmpty();
        }
    }

    private final Map<String, Node> nodes;
    private final Map<String, List<String>> gates;

    private ResearchTree(Map<String, Node> nodes, Map<String, List<String>> gates) {
        this.nodes = nodes;
        this.gates = gates;
    }

    /**
     * Builds a tree, checking that every requirement names an earlier node and that no node is listed twice.
     *
     * @throws IllegalArgumentException when the nodes do not form a line
     */
    public static ResearchTree of(Collection<Node> ordered) {
        Map<String, Node> nodes = new LinkedHashMap<>();
        Map<String, List<String>> gates = new HashMap<>();
        for (Node node : ordered) {
            if (nodes.containsKey(node.id())) {
                throw new IllegalArgumentException("Research node '" + node.id() + "' is listed twice.");
            }
            for (String required : node.requires()) {
                if (!nodes.containsKey(required)) {
                    throw new IllegalArgumentException("Research node '" + node.id() + "' requires '" + required
                            + "', which is not an earlier node.");
                }
            }
            nodes.put(node.id(), node);
            for (String id : node.unlocks()) {
                gates.computeIfAbsent(id, key -> new ArrayList<>()).add(node.id());
            }
        }
        gates.replaceAll((id, list) -> List.copyOf(list));
        return new ResearchTree(Collections.unmodifiableMap(nodes), Map.copyOf(gates));
    }

    /**
     * Reads {@code nodes:} from research.yml. A node that is malformed is logged and left out; the rest still load.
     */
    public static ResearchTree parse(ConfigurationSection root, Logger log) {
        List<Node> ordered = new ArrayList<>();
        if (root == null || root.getConfigurationSection("nodes") == null) {
            return of(ordered);
        }
        ConfigurationSection section = root.getConfigurationSection("nodes");
        for (String id : section.getKeys(false)) {
            ConfigurationSection node = section.getConfigurationSection(id);
            if (node == null) {
                log.warning("Research node '" + id + "' is not a section, skipped.");
                continue;
            }
            Map<String, Integer> cost = new LinkedHashMap<>();
            ConfigurationSection costSection = node.getConfigurationSection("cost");
            if (costSection != null) {
                for (String pack : costSection.getKeys(false)) {
                    cost.put(pack, Math.max(0, costSection.getInt(pack)));
                }
            }
            ordered.add(new Node(id, node.getString("name", id), node.getString("description", ""),
                    node.getStringList("requires"), cost, node.getStringList("unlocks")));
        }
        try {
            return of(ordered);
        } catch (IllegalArgumentException e) {
            log.warning("research.yml is not a valid line: " + e.getMessage() + " Research is disabled for this start.");
            return of(List.of());
        }
    }

    public Optional<Node> node(String id) {
        return Optional.ofNullable(nodes.get(id));
    }

    /** Every node, in the order of the file. */
    public Collection<Node> nodes() {
        return nodes.values();
    }

    /** True when some node gates this id. Ids no node lists are free. */
    public boolean isGated(String id) {
        return gates.containsKey(id);
    }

    /** The nodes that gate an id, in file order. Empty for a free id. */
    public List<String> gatesOf(String id) {
        return gates.getOrDefault(id, List.of());
    }
}
