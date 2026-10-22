package io.corefactory.spigot.water;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import io.corefactory.core.research.ResearchService;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.WaterOrientation;
import io.corefactory.core.water.WaterPart;
import io.corefactory.core.water.WaterText;
import io.corefactory.spigot.energy.DeviceRegistry;
import io.corefactory.spigot.machine.MachineStore;
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
 * Places a water block held in the main hand on the block the player chose. A pipe and a splitter go on air or water; a pump goes on a
 * water block, and its source must reach the minimum. A splitter takes its input from the side the player stands on, so it cannot go on
 * the top or the bottom of a block. The research that unlocks the item and a protection plugin's cancel are checked too. Every refusal
 * sends one message that names the reason.
 */
final class WaterPlacement {

    private final CoreFactoryCore core;
    private final WaterService service;
    private final MachineStore machines;
    private final DeviceRegistry devices;
    private final ResearchService research;

    WaterPlacement(CoreFactoryCore core, WaterService service, MachineStore machines, DeviceRegistry devices,
                   ResearchService research) {
        this.core = core;
        this.service = service;
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
        if (part.splitter() && clickedFace != null && !clickedFace.horizontal()) {
            say(player, "&cA splitter takes water from its side, so it cannot go on the top or bottom of a block. "
                    + "Place it on a side face.");
            return false;
        }
        Material type = target.getBlock().getType();
        if (part == WaterPart.PUMP) {
            if (type != Material.WATER) {
                say(player, "&cA pump goes on a water block. Aim at the water.");
                return false;
            }
        } else if (type.isSolid()) {
            say(player, "&cThat spot is solid. A pipe or splitter goes on air or water.");
            return false;
        }
        if (service.at(target).isPresent() || machines.at(target).isPresent() || devices.at(target).isPresent()) {
            say(player, "&cAnother device already stands there.");
            return false;
        }
        if (part == WaterPart.PUMP) {
            int minimum = service.sourceMinBlocks();
            int count = service.sourceAt(target, minimum + 1);
            if (count < minimum) {
                say(player, "&c" + WaterText.sourceTooSmall(count, minimum));
                return false;
            }
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
        Face face = part == WaterPart.PIPE ? null : WaterOrientation.inputFromLook(player.getLocation().getYaw());
        int level = part == WaterPart.PUMP ? WaterEntities.levelOf(held) : 1;
        service.place(target, part, player.getUniqueId(), face, level);
        if (player.getGameMode() != GameMode.CREATIVE) {
            take(player, 1);
        }
        player.swingMainHand();
        return true;
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
        player.sendMessage(Messages.color(text));
    }
}
