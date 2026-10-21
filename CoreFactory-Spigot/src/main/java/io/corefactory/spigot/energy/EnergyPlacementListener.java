package io.corefactory.spigot.energy;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import io.corefactory.core.device.Footprint;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.item.PlaceableItems;
import io.corefactory.core.research.ResearchService;
import io.corefactory.core.research.ResearchTree;
import io.corefactory.spigot.machine.MachineStore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Places a pylon, accumulator or generator held in the main hand on the face of a block the player right-clicks. The target is the block
 * beside that face, and it must be air; a nuclear generator may also go into water, since it runs only there. The placement is refused
 * with a message when the target is not open, another device stands there, the player has not finished the research that unlocks the
 * item, or a protection plugin cancels a placement event fired for the spot. The device is placed on air, with no vanilla block, so
 * a solar panel is a generator device and never a daylight detector. Sneaking places too. A clicked lab tool keeps its own menu.
 *
 * <p>A tall device (see {@link Footprint}) fills the target and the block above it, so both must be free: air, a replaceable block
 * that is not liquid (cleared on placement), or water for a nuclear generator. Both blocks are checked with the protection event.
 */
public final class EnergyPlacementListener implements Listener {

    private final CoreFactoryCore core;
    private final DeviceRegistry devices;
    private final EnergyService energy;
    private final MachineStore machines;
    private final ResearchService research;

    public EnergyPlacementListener(CoreFactoryCore core, DeviceRegistry devices, EnergyService energy, MachineStore machines,
                                   ResearchService research) {
        this.core = core;
        this.devices = devices;
        this.energy = energy;
        this.machines = machines;
        this.research = research;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack held = event.getItem();
        Block clicked = event.getClickedBlock();
        if (held == null || clicked == null) {
            return;
        }
        Optional<String> id = core.items().componentId(held).filter(PlaceableItems.ENERGY_DEVICES::contains);
        if (id.isEmpty()) {
            return;
        }
        if (machines.at(clicked.getLocation()).isPresent()) {
            if (event.getPlayer().isSneaking()) {
                event.setCancelled(true);
                event.setUseItemInHand(Event.Result.DENY);
            }
            return;
        }
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        Player player = event.getPlayer();
        Block target = clicked.getRelative(event.getBlockFace() == null ? BlockFace.UP : event.getBlockFace());
        Block upper = target.getRelative(BlockFace.UP);
        Location spot = target.getLocation();
        Optional<GeneratorKind> generator = GeneratorIds.kindOf(id.get());
        boolean nuclear = generator.filter(GeneratorKind.NUCLEAR::equals).isPresent();
        boolean tall = Footprint.heightOf(id.get()) > Footprint.SHORT;
        boolean open = free(target, nuclear, tall) && (!tall || free(upper, nuclear, true));
        boolean occupied = devices.at(spot).isPresent() || machines.at(spot).isPresent()
                || (tall && (devices.at(upper.getLocation()).isPresent() || machines.at(upper.getLocation()).isPresent()));
        boolean unlocked = research.isUnlocked(player.getUniqueId(), id.get());
        Optional<PlacementRules.Refusal> refusal = PlacementRules.before(core.isDisabledWorld(target.getWorld()),
                open, occupied, unlocked);
        if (refusal.isPresent()) {
            refuse(player, refusal.get(), id.get());
            return;
        }
        ItemStack one = held.clone();
        one.setAmount(1);
        BlockPlaceEvent check = new BlockPlaceEvent(target, target.getState(), clicked, one, player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(check);
        if (check.isCancelled()) {
            refuse(player, PlacementRules.Refusal.PROTECTED, id.get());
            return;
        }
        if (tall) {
            BlockPlaceEvent upperCheck = new BlockPlaceEvent(upper, upper.getState(), clicked, one, player, true,
                    EquipmentSlot.HAND);
            Bukkit.getPluginManager().callEvent(upperCheck);
            if (upperCheck.isCancelled()) {
                refuse(player, PlacementRules.Refusal.PROTECTED, id.get());
                return;
            }
            clearCover(upper);
        }
        clearCover(target);
        Optional<Integer> pylonLevel = EnergyLevels.levelOf(id.get(), EnergyLevels.PYLON);
        if (generator.isPresent()) {
            energy.addGenerator(spot, generator.get());
        } else if (pylonLevel.isPresent()) {
            energy.addPylon(spot, pylonLevel.get());
        } else {
            energy.addAccumulator(spot, EnergyLevels.levelOf(id.get(), EnergyLevels.ACCUMULATOR).orElse(1));
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            take(player);
        }
        player.swingMainHand();
    }

    private void refuse(Player player, PlacementRules.Refusal refusal, String id) {
        switch (refusal) {
            case DISABLED_WORLD -> player.sendMessage(core.messages().get("disabled-world"));
            case NOT_AIR -> player.sendMessage(Messages.color("&cThat spot is taken: the blocks the device fills must be free, "
                    + "air or water for a nuclear generator."));
            case OCCUPIED -> player.sendMessage(Messages.color("&cA device already stands there."));
            case LOCKED -> research.nodeFor(player.getUniqueId(), id).ifPresent(node -> player.sendMessage(
                    Messages.color("&cRequires research: " + node.name() + "&c. Finish the earlier nodes at a Research Console.")));
            case PROTECTED -> player.sendMessage(Messages.color("&cThis spot is protected."));
        }
    }

    /**
     * Whether a block may hold a device's block: air, or water for a nuclear generator. A block of a tall device may also be a
     * replaceable block that is not liquid, such as grass or a flower, which is cleared when the device is placed.
     */
    private static boolean free(Block block, boolean nuclear, boolean tall) {
        Material type = block.getType();
        if (type.isAir() || (nuclear && type == Material.WATER)) {
            return true;
        }
        return tall && Tag.REPLACEABLE.isTagged(type) && !block.isLiquid();
    }

    /** Clears a replaceable block a tall device covers, as vanilla placement replaces it. Air and water are left as they are. */
    private static void clearCover(Block block) {
        if (!block.getType().isAir() && !block.isLiquid()) {
            block.setType(Material.AIR, false);
        }
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
