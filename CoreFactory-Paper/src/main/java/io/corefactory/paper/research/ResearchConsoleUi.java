package io.corefactory.paper.research;

import io.corefactory.api.TntDefinition;
import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.engine.Visuals;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Machine;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.Stack;
import io.corefactory.core.research.Progression;
import io.corefactory.core.research.ResearchService;
import io.corefactory.core.research.ResearchTree;
import io.corefactory.core.text.Text;
import io.corefactory.paper.machine.MachineLayout;
import io.corefactory.paper.machine.MachineMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * The research console's menu. Six pack slots along the bottom take research packs. The rows above show every research
 * node in line order: finished nodes are green, available ones yellow, locked ones grey. Clicking an available node
 * feeds it the packs in the slots, and the packs it uses are taken out.
 *
 * <p>The node icons are drawn into the shared inventory, so they show the state for the player who last opened or fed
 * the console. The feed itself always checks the state of the player who clicked.
 */
public final class ResearchConsoleUi extends MachineMenu {

    private static final int NODE_ROWS = 5;
    private static final int NODE_COLUMNS = 7;
    private static final String PACK_PREFIX = "research_pack_";

    private final CoreFactoryCore core;
    private final ResearchService research;
    private final Visuals visuals;
    private final List<String> order;
    private final Map<Integer, String> nodeAt = new HashMap<>();

    public ResearchConsoleUi(CoreFactoryCore core, ProcessCatalog catalog, Machine machine, MachineLayout layout,
                             Location site, ResearchService research, Visuals visuals) {
        super(core, catalog, machine, layout, site);
        this.core = core;
        this.research = research;
        this.visuals = visuals;
        this.order = lineOf(research.tree());
    }

    @Override
    public void open(Player player) {
        refreshNodes(player);
        super.open(player);
    }

    /** Only research packs go into the console. */
    @Override
    public boolean accepts(ItemRef ref, MachineLayout.Kind kind) {
        return kind == MachineLayout.Kind.INPUT && isPack(ref);
    }

    @Override
    public void slotClick(InventoryClickEvent event, Player player) {
        int raw = event.getRawSlot();
        String node = raw >= 0 && raw < getInventory().getSize() ? nodeAt.get(raw) : null;
        if (node != null) {
            event.setCancelled(true);
            sync();
            feed(player, node);
            return;
        }
        super.slotClick(event, player);
    }

    private void feed(Player player, String nodeId) {
        ResearchTree.Node node = research.tree().node(nodeId).orElse(null);
        if (node == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Progression.Result result = research.feed(uuid, nodeId, offered());
        take(result.consumed());
        if (result.finished()) {
            player.sendMessage(Text.parse("&aResearch finished: " + node.name()));
            if (site() != null) {
                visuals.play("research_complete", site().clone().add(0.5, 1.0, 0.5), 1.5);
            }
        } else if (!result.consumed().isEmpty()) {
            player.sendMessage(Text.parse("&7Fed " + total(result.consumed()) + " pack(s) into " + node.name() + "&7."));
        } else if (research.isFinished(uuid, nodeId)) {
            player.sendMessage(Text.parse("&7" + node.name() + " is already finished."));
        } else if (!requirementsDone(uuid, node)) {
            player.sendMessage(Text.parse("&cRequires research: " + prerequisite(uuid, node)));
        } else {
            player.sendMessage(Text.parse("&7Put the packs for " + node.name() + " &7in the slots first."));
        }
        refreshNodes(player);
    }

    private Map<String, Integer> offered() {
        Map<String, Integer> offered = new HashMap<>();
        for (Stack slot : inputs()) {
            ItemRef ref = slot.ref();
            if (ref != null && isPack(ref)) {
                offered.merge(ref.name(), slot.count(), Integer::sum);
            }
        }
        return offered;
    }

    private void take(Map<String, Integer> consumed) {
        for (Map.Entry<String, Integer> part : consumed.entrySet()) {
            ItemRef ref = ItemRef.item(part.getKey());
            int left = part.getValue();
            for (Stack slot : inputs()) {
                if (left == 0) {
                    break;
                }
                if (ref.equals(slot.ref())) {
                    int taken = Math.min(left, slot.count());
                    slot.set(ref, slot.count() - taken);
                    left -= taken;
                }
            }
        }
    }

    private void refreshNodes(Player player) {
        Inventory inventory = getInventory();
        nodeAt.clear();
        for (int row = 0; row < NODE_ROWS; row++) {
            for (int column = 1; column <= NODE_COLUMNS; column++) {
                inventory.setItem(row * 9 + column, blank());
            }
        }
        for (int i = 0; i < order.size() && i < NODE_ROWS * NODE_COLUMNS; i++) {
            int slot = (i / NODE_COLUMNS) * 9 + 1 + i % NODE_COLUMNS;
            nodeAt.put(slot, order.get(i));
            inventory.setItem(slot, nodeItem(player.getUniqueId(), order.get(i)));
        }
    }

    private ItemStack nodeItem(UUID player, String nodeId) {
        ResearchTree.Node node = research.tree().node(nodeId).orElseThrow();
        boolean finished = research.isFinished(player, nodeId);
        boolean available = !finished && requirementsDone(player, node);
        Material material = finished ? Material.LIME_CONCRETE : available ? Material.YELLOW_CONCRETE : Material.GRAY_CONCRETE;
        List<String> lore = new ArrayList<>();
        lore.add("&8" + node.description());
        lore.add("");
        if (finished) {
            lore.add("&aFinished.");
            if (node.unlocks().isEmpty()) {
                lore.add("&7It unlocks nothing.");
            } else {
                lore.add("&7Unlocks:");
                for (String unlock : node.unlocks()) {
                    lore.add("&f - " + unlockName(unlock));
                }
            }
        } else {
            if (available) {
                lore.add("&eAvailable. Click to feed the packs in the slots.");
            } else {
                lore.add("&8Locked. Requires: " + prerequisite(player, node));
            }
            lore.addAll(costLines(player, node));
        }
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(node.name()));
            meta.lore(Text.lore(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> costLines(UUID player, ResearchTree.Node node) {
        List<String> lines = new ArrayList<>();
        Map<String, Integer> fed = research.progressOf(player, node.id());
        for (Map.Entry<String, Integer> part : new TreeMap<>(node.cost()).entrySet()) {
            lines.add("&7" + packName(part.getKey()) + " &f" + fed.getOrDefault(part.getKey(), 0)
                    + "&7/&f" + part.getValue());
        }
        return lines;
    }

    private boolean requirementsDone(UUID player, ResearchTree.Node node) {
        return node.requires().stream().allMatch(required -> research.isFinished(player, required));
    }

    private String prerequisite(UUID player, ResearchTree.Node node) {
        return node.requires().stream()
                .filter(required -> !research.isFinished(player, required))
                .findFirst()
                .flatMap(required -> research.tree().node(required))
                .map(ResearchTree.Node::name)
                .orElse("nothing");
    }

    private String packName(String pack) {
        return core.componentDefinitions().containsKey(pack)
                ? core.componentDefinitions().get(pack).displayName()
                : pack;
    }

    private String unlockName(String id) {
        if (core.componentDefinitions().containsKey(id)) {
            return core.componentDefinitions().get(id).displayName();
        }
        return core.definition(id).map(TntDefinition::displayName).orElse(id);
    }

    private static boolean isPack(ItemRef ref) {
        return ref.kind() == ItemRef.Kind.ITEM && ref.name().startsWith(PACK_PREFIX);
    }

    private static ItemStack blank() {
        ItemStack item = new ItemStack(Material.BLUE_STAINED_GLASS_PANE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    /** The nodes in the order of the line: each one after the nodes it requires. */
    private static List<String> lineOf(ResearchTree tree) {
        List<String> ordered = new ArrayList<>();
        Set<String> placed = new HashSet<>();
        boolean progress = true;
        while (progress && ordered.size() < tree.nodes().size()) {
            progress = false;
            for (ResearchTree.Node node : tree.nodes()) {
                if (!placed.contains(node.id()) && placed.containsAll(node.requires())) {
                    ordered.add(node.id());
                    placed.add(node.id());
                    progress = true;
                }
            }
        }
        return ordered;
    }

    private static int total(Map<String, Integer> counts) {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }
}
