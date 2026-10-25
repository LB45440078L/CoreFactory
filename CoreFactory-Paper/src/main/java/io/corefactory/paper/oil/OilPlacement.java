package io.corefactory.paper.oil;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.oil.OilPart;
import io.corefactory.core.research.ResearchService;
import io.corefactory.core.text.Text;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.WaterOrientation;
import io.corefactory.paper.energy.DeviceRegistry;
import io.corefactory.paper.machine.MachineStore;
import io.corefactory.paper.water.WaterService;
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
 * Places an oil tube held in the main hand on the block the player chose. A tube goes on air and nowhere else: water and solid blocks refuse
 * it. A horizontal splitter takes its input from the side the player stands on; a splitter placed on the top or the bottom of a block is
 * vertical, its input faces that block and its outputs run across the player's view, as the water splitters do. The research that unlocks
 * the item, a device already at the block, a tube that would join petroleum to diesel and a protection plugin's cancel are checked too. Every
 * refusal sends one message that names the reason.
 */
final class OilPlacement {

    private final CoreFactoryCore core;
    private final OilService service;
    private final WaterService water;
    private final MachineStore machines;
    private final DeviceRegistry devices;
    private final ResearchService research;

    OilPlacement(CoreFactoryCore core, OilService service, WaterService water, MachineStore machines, DeviceRegistry devices,
                 ResearchService research) {
        this.core = core;
        this.service = service;
        this.water = water;
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
    boolean place(Player player, Location target, Location against, Face clickedFace, ItemStack held, OilPart part) {
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
        if (!onAir(target.getBlock().getType())) {
            say(player, "&cOil tubes go on air.");
            return false;
        }
        if (service.at(target).isPresent() || water.at(target).isPresent() || machines.at(target).isPresent()
                || devices.at(target).isPresent()) {
            say(player, "&cAnother device already stands there.");
            return false;
        }
        if (service.wouldMixTube(target)) {
            say(player, "&cPetroleum and diesel cannot share a pipe.");
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
        service.place(target, part, player.getUniqueId(), orientation[0], orientation[1]);
        if (player.getGameMode() != GameMode.CREATIVE) {
            take(player, 1);
        }
        player.swingMainHand();
        return true;
    }

    /**
     * True when a block of this type takes an oil tube: air, cave air or void air. Water and every solid block refuse it. The check compares
     * the constants, since {@link Material#isAir} needs a running server.
     *
     * @param type the block's type
     */
    static boolean onAir(Material type) {
        return type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR;
    }

    /**
     * The input and output faces a part is placed with. A pipe has none. A horizontal splitter's input faces the player, and its output is the
     * default of its input. A vertical splitter's input faces the block it stands on, and its output runs to the player's left or right.
     *
     * @return the input and the output, either null
     */
    static Face[] orientation(OilPart part, Face clickedFace, float playerYaw) {
        if (part == OilPart.PIPE) {
            return new Face[] {null, null};
        }
        boolean vertical = clickedFace != null && !clickedFace.horizontal();
        if (vertical) {
            Face across = WaterOrientation.turn(WaterOrientation.lookFace(playerYaw), 90);
            return new Face[] {clickedFace.opposite(), across};
        }
        Face input = WaterOrientation.lookFace(playerYaw);
        return new Face[] {input, WaterOrientation.defaultOutput(input)};
    }

    /** Takes items from the stack in the player's main hand. */
    private static void take(Player player, int count) {
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand.getAmount() <= count) {
            player.getInventory().setItemInMainHand(null);
        } else {
            inHand.setAmount(inHand.getAmount() - count);
        }
    }

    /** Sends a line, with its colour codes read. */
    private static void say(Player player, String text) {
        player.sendMessage(Text.parse(text));
    }
}
