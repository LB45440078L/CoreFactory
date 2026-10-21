package io.corefactory.spigot.logistics;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Dir;
import io.corefactory.core.research.ResearchService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Places a belt, picker or splitter held in the main hand on the spot beside a block the player aimed at, through the face the
 * player aimed at. The spot is the aimed block moved one step out through that face.
 *
 * <p>A belt flows away from the player, as before. A picker and a splitter need a side face (north, south, east or west): their
 * input faces the aimed block, and their output (a picker) or straight output (a splitter) faces away from it. A picker or a
 * splitter takes its input from the aimed block, which must be a machine, a generator or a belt that flows into the spot. A picker
 * may also take from a straight belt that flows past its side, so it can be placed beside a belt. The spot must be free of solid
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
        SidePlacement.Verdict verdict = SidePlacement.decide(device.kind(), face,
                service.inputAt(clicked, PlacementAim.side(face).orElse(null)),
                research.isUnlocked(player.getUniqueId(), id),
                service.spaceAt(spot),
                () -> service.check(device.kind(), clicked.getWorld().getUID(), plan.orElseThrow().target(), dirOf(plan.get())));
        if (verdict != SidePlacement.Verdict.OK) {
            refuse(player, verdict, id);
            return false;
        }
        Block targetBlock = spot.getBlock();
        BlockState replaced = targetBlock.getState();
        ItemStack one = held.clone();
        one.setAmount(1);
        BlockPlaceEvent check = new BlockPlaceEvent(targetBlock, replaced, clicked.getBlock(), one, player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(check);
        if (check.isCancelled()) {
            player.sendMessage(Messages.color("&cThis spot is protected."));
            return false;
        }
        service.add(device.kind(), clicked.getWorld().getUID(), target, dirOf(plan.orElseThrow()), device.level(),
                player.getUniqueId());
        if (player.getGameMode() != GameMode.CREATIVE) {
            take(player);
        }
        player.swingMainHand();
        return true;
    }

    /** The direction a device is stored with: a belt's flow, or a picker's or splitter's input direction. */
    private static Dir dirOf(PlacementAim.Plan plan) {
        return plan.flow() != null ? plan.flow() : plan.inDir();
    }

    private void refuse(Player player, SidePlacement.Verdict verdict, String id) {
        String text = switch (verdict) {
            case OK -> "&7Placed.";
            case NOT_SIDE -> "&cPlace it on a side face of the machine: north, south, east or west, not its top or bottom.";
            case LOCKED -> research.nodeFor(player.getUniqueId(), id)
                    .map(node -> "&cResearch '" + node.name() + "' is needed.")
                    .orElse("&cResearch is needed for this item.");
            case NO_INPUT -> "&cA picker takes from a machine, a generator, or a straight belt that flows into it or past its side. "
                    + "A splitter takes from a machine, a generator, or a belt that flows into it.";
            case SOLID -> "&cThat side is blocked: the block beside the face you aim at must be air.";
            case ENTITY -> "&cSomething stands in the spot where it would go.";
            case OCCUPIED -> "&cAnother device already stands there.";
            case MERGE -> "&cThat would give a belt a second input. Two belts cannot feed one belt.";
            case BLOCKED -> "&cThat side is blocked: it would feed a belt head-on, or feed a picker or splitter from a side it does "
                    + "not take.";
        };
        player.sendMessage(Messages.color(text));
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
