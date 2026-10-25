package io.corefactory.paper.water;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.research.ResearchService;
import io.corefactory.core.text.Text;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.PumpLevels;
import io.corefactory.core.water.WaterOrientation;
import io.corefactory.core.water.WaterPart;
import io.corefactory.paper.energy.DeviceRegistry;
import io.corefactory.paper.machine.MachineStore;
import io.corefactory.paper.oil.OilService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Places a water block held in the main hand on the block the player chose. Every part goes on air or water, and not on a solid block.
 * A pump needs no water of its own: its source is the water its network reaches, so it may stand on land beside a source tube. A
 * tall pump (levels 4 and 5) also needs the block above it free. A horizontal splitter takes its input from the side the player stands
 * on; a splitter placed on the top or the bottom of a block is vertical, its input faces that block and its outputs run across the
 * player's view. The research that unlocks the item and a protection plugin's cancel are checked too. Every refusal sends one message
 * that names the reason.
 */
final class WaterPlacement {

    private final CoreFactoryCore core;
    private final WaterService service;
    private final OilService oil;
    private final MachineStore machines;
    private final DeviceRegistry devices;
    private final ResearchService research;

    WaterPlacement(CoreFactoryCore core, WaterService service, OilService oil, MachineStore machines, DeviceRegistry devices,
                   ResearchService research) {
        this.core = core;
        this.service = service;
        this.oil = oil;
        this.machines = machines;
        this.devices = devices;
        this.research = research;
    }

    /**
     * Places the held part.
     *
     * @param player      the player who places it
     * @param target      the block the part stands in
     * @param against     the block the player aimed at, which the protection check names
     * @param clickedFace the face of the block the player aimed through, or null when none is known
     * @param held        the item in the player's main hand
     * @param part        the part the item names
     * @return true when the part was placed and the item taken
     */
    boolean place(Player player, Location target, Location against, Face clickedFace, ItemStack held, WaterPart part) {
        World world = target.getWorld();
        if (world == null) {
            return false;
        }
        if (core.isDisabledWorld(world)) {
            player.sendMessage(core.messages().get("disabled-world"));
            return false;
        }
        if (!research.isUnlocked(player.getUniqueId(), part.componentId())) {
            say(player, "&cResearch is needed for this item.");
            return false;
        }
        Material type = target.getBlock().getType();
        if (type.isSolid()) {
            say(player, "&cThat spot is solid. A pump, pipe or splitter goes on air or water.");
            return false;
        }
        if (service.at(target).isPresent() || oil.at(target).isPresent() || machines.at(target).isPresent()
                || devices.at(target).isPresent()) {
            say(player, "&cAnother device already stands there.");
            return false;
        }
        if (part == WaterPart.PUMP && oil.wouldMix(target)) {
            say(player, "&cThis would mix petroleum and diesel in one oil network.");
            return false;
        }
        int level = part == WaterPart.PUMP ? WaterEntities.levelOf(held) : 1;
        if (PumpLevels.height(level) > 1 && !service.spaceAbove(target)) {
            say(player, "&cA level " + level + " pump stands two blocks tall, and the block above this spot is in the way.");
            return false;
        }
        Block block = target.getBlock();
        ItemStack one = held.clone();
        one.setAmount(1);
        BlockPlaceEvent check = new BlockPlaceEvent(block, block.getState(), against.getBlock(), one, player, true,
                EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(check);
        if (check.isCancelled()) {
            say(player, "&cThis spot is protected.");
            return false;
        }
        Face[] orientation = orientation(part, clickedFace, player.getLocation().getYaw());
        service.place(target, part, player.getUniqueId(), orientation[0], orientation[1], level);
        if (player.getGameMode() != GameMode.CREATIVE) {
            take(player, 1);
        }
        player.swingMainHand();
        return true;
    }

    /**
     * The input and output faces a part is placed with. A pipe has none. A pump's front and a horizontal splitter's input face the player.
     * A vertical splitter's input faces the block it stands on, and its output runs to the player's left or right. A horizontal splitter's
     * output is the default of its input.
     *
     * @return the input (or the pump's front) and the output, either null
     */
    static Face[] orientation(WaterPart part, Face clickedFace, float playerYaw) {
        if (part == WaterPart.PIPE) {
            return new Face[] {null, null};
        }
        boolean vertical = clickedFace != null && !clickedFace.horizontal();
        if (part.splitter() && vertical) {
            Face across = WaterOrientation.turn(WaterOrientation.lookFace(playerYaw), 90);
            return new Face[] {clickedFace.opposite(), across};
        }
        if (part.splitter()) {
            Face input = WaterOrientation.lookFace(playerYaw);
            return new Face[] {input, WaterOrientation.defaultOutput(input)};
        }
        return new Face[] {WaterOrientation.inputFromLook(playerYaw), null};
    }

    /** Takes items from the stack in the player's main hand. */
    static void take(Player player, int count) {
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand.getAmount() <= count) {
            player.getInventory().setItemInMainHand(null);
        } else {
            inHand.setAmount(inHand.getAmount() - count);
        }
    }

    /** Sends a line, with its colour codes read. */
    static void say(Player player, String text) {
        player.sendMessage(Text.parse(text));
    }
}
