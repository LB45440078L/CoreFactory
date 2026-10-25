package io.corefactory.paper.logistics;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Dir;
import io.corefactory.core.logistics.Duplex;
import io.corefactory.core.research.ResearchService;
import io.corefactory.core.text.Text;
import io.corefactory.paper.research.ResearchHints;
import io.corefactory.paper.machine.StorageKinds;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Places a belt, picker or splitter held in the main hand on the spot beside a block the player aimed at, through the face the
 * player aimed at. The spot is the aimed block moved one step out through that face.
 *
 * <p>A belt flows away from the player, as before. A picker and a splitter need a side face (north, south, east or west): their
 * input faces the aimed block, and their output (a picker) or straight output (a splitter) faces away from it. A picker or a
 * splitter takes its input from the aimed block, which must be a machine, a generator or a belt that flows into the spot. A picker
 * may also take from a straight belt that flows past its side, so it can be placed beside a belt, and from a vanilla storage block
 * such as a chest, which a splitter does not take from. The spot must be free of solid
 * blocks, devices and entities. The network then decides OCCUPIED, MERGE or BLOCKED, through
 * {@link SidePlacement}. The research that unlocks the item and a protection plugin's cancel of the placement event are checked
 * too. Every refusal sends one message that names the reason.
 */
public final class LogisticsPlacement {

    private final CoreFactoryCore core;
    private final LogisticsService service;
    private final ResearchService research;

    /**
     * @param core     the core, which reads the world rules and the messages
     * @param service  the service that owns the devices
     * @param research the research, which unlocks the items
     */
    public LogisticsPlacement(CoreFactoryCore core, LogisticsService service, ResearchService research) {
        this.core = core;
        this.service = service;
        this.research = research;
    }

    /**
     * Places the held device against a face of the aimed block.
     *
     * @param player  the player who placed it
     * @param clicked the block the player aimed at: a machine's host, a generator's block, a device's block or any block
     * @param face    the face offset of the aim, one component of magnitude one
     * @param held    the item in the player's main hand
     * @param device  the kind and level the item names
     * @return true when the device was placed and the item taken
     */
    boolean place(Player player, Location clicked, Pos face, ItemStack held, LogisticsIds.Device device) {
        if (clicked.getWorld() == null) {
            return false;
        }
        if (core.isDisabledWorld(clicked.getWorld())) {
            player.sendMessage(core.messages().get("disabled-world"));
            return false;
        }
        Pos clickedPos = new Pos(clicked.getBlockX(), clicked.getBlockY(), clicked.getBlockZ());
        Optional<PlacementAim.Plan> plan = PlacementAim.plan(device.kind(), clickedPos, face, player.getLocation().getYaw());
        Pos target = new Pos(clickedPos.x() + face.x(), clickedPos.y() + face.y(), clickedPos.z() + face.z());
        Location spot = new Location(clicked.getWorld(), target.x(), target.y(), target.z());
        String id = LogisticsIds.componentId(device.kind(), device.level());
        boolean duplex = device.kind() == LogisticsIds.Kind.DUPLEX;
        Dir flow = duplex ? dirOf(plan.orElseThrow()) : null;
        Location secondSpot = null;
        if (duplex) {
            Pos second = Duplex.secondCell(target, flow);
            secondSpot = new Location(clicked.getWorld(), second.x(), second.y(), second.z());
        }
        SidePlacement.Input input = duplex ? service.duplexInputAt(spot, flow) : inputOf(clicked, PlacementAim.side(face).orElse(null));
        SidePlacement.Space space = service.spaceAt(spot);
        if (duplex && space == SidePlacement.Space.FREE) {
            space = service.spaceAt(secondSpot);
        }
        SidePlacement.Verdict verdict = SidePlacement.decide(device.kind(), face, input,
                research.isUnlocked(player.getUniqueId(), id),
                space,
                () -> service.check(device.kind(), clicked.getWorld().getUID(), plan.orElseThrow().target(), dirOf(plan.get())));
        if (verdict != SidePlacement.Verdict.OK) {
            refuse(player, verdict, id);
            return false;
        }
        List<Location> spots = duplex ? List.of(spot, secondSpot) : List.of(spot);
        for (Location cell : spots) {
            Block targetBlock = cell.getBlock();
            BlockState replaced = targetBlock.getState();
            ItemStack one = held.clone();
            one.setAmount(1);
            BlockPlaceEvent check = new BlockPlaceEvent(targetBlock, replaced, clicked.getBlock(), one, player, true,
                    EquipmentSlot.HAND);
            Bukkit.getPluginManager().callEvent(check);
            if (check.isCancelled()) {
                player.sendMessage(Text.parse("&cThis spot is protected."));
                return false;
            }
        }
        service.add(device.kind(), clicked.getWorld().getUID(), target, dirOf(plan.orElseThrow()), device.level(),
                player.getUniqueId());
        if (player.getGameMode() != GameMode.CREATIVE) {
            take(player);
        }
        player.swingMainHand();
        return true;
    }

    /**
     * What the aimed block is as the input of the device: the belt, machine or generator the service names, else a vanilla storage block
     * (a chest, a barrel, a shulker box or a copper chest) or a crafter, which only a picker takes from.
     */
    private SidePlacement.Input inputOf(Location clicked, Dir toward) {
        SidePlacement.Input input = service.inputAt(clicked, toward);
        String material = clicked.getBlock().getType().name();
        if (input == SidePlacement.Input.OTHER && (StorageKinds.isStorage(material) || StorageKinds.isCrafter(material))) {
            return SidePlacement.Input.STORAGE;
        }
        return input;
    }

    /** The direction a device is stored with: a belt's flow, or a picker's or splitter's input direction. */
    private static Dir dirOf(PlacementAim.Plan plan) {
        return plan.flow() != null ? plan.flow() : plan.inDir();
    }

    private void refuse(Player player, SidePlacement.Verdict verdict, String id) {
        Component text = switch (verdict) {
            case OK -> Text.parse("&7Placed.");
            case NOT_SIDE -> Text.parse("&cPlace it on a side face of the machine: north, south, east or west, not its top or bottom.");
            case LOCKED -> research.nodeFor(player.getUniqueId(), id).map(ResearchHints::needed)
                    .orElseGet(() -> Text.parse("&cResearch is needed for this item."));
            case NO_INPUT -> Text.parse("&cA picker takes from a machine, a generator, a chest, a crafter or other storage, "
                    + "or a straight belt that flows into it or past its side. A splitter takes from a machine, a generator, or a belt that flows into it.");
            case NO_BACK_BELT -> Text.parse("&cA belt duplex takes its input from a belt that flows into the back of one of its two cells, "
                    + "in the direction you face it.");
            case SOLID -> Text.parse("&cThat side is blocked: the block beside the face you aim at must be air.");
            case ENTITY -> Text.parse("&cSomething stands in the spot where it would go.");
            case OCCUPIED -> Text.parse("&cAnother device already stands there.");
            case MERGE -> Text.parse("&cThat would give a belt a second input. Two belts cannot feed one belt.");
            case BLOCKED -> Text.parse("&cThat side is blocked: it would feed a belt head-on, or feed a picker or splitter from a side it "
                    + "does not take.");
        };
        player.sendMessage(text);
    }

    private static void take(Player player) {
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            inHand.setAmount(inHand.getAmount() - 1);
        }
    }
}
