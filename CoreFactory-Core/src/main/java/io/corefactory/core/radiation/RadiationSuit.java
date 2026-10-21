package io.corefactory.core.radiation;

import io.corefactory.core.item.ItemFactory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * The Radioactive Suit: four craftable armour pieces. Each worn piece gives a little blast protection and shields a
 * quarter of the fallout. All four together shield it completely, so the fallout does nothing to the wearer.
 */
public final class RadiationSuit {

    /** The component ids of the four pieces, in the order of armour slots from head to feet. */
    public static final List<String> PIECES = List.of(
            "radioactive_helmet", "radioactive_chestplate", "radioactive_leggings", "radioactive_boots");

    /** The share of blast damage one worn piece absorbs. */
    public static final double BLAST_REDUCTION_PER_PIECE = 0.04;

    private final ItemFactory items;

    public RadiationSuit(ItemFactory items) {
        this.items = items;
    }

    /** How many of the suit's pieces the player wears in the armour slots. */
    public int wornPieces(Player player) {
        int count = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece != null && items.componentId(piece).filter(PIECES::contains).isPresent()) {
                count++;
            }
        }
        return count;
    }

    /** The share of the fallout that still reaches the player: 1 with no pieces, 0 with the whole suit. */
    public double exposure(Player player) {
        return 1.0 - (double) wornPieces(player) / PIECES.size();
    }

    /** The share of blast damage the worn pieces absorb. */
    public double blastReduction(Player player) {
        return wornPieces(player) * BLAST_REDUCTION_PER_PIECE;
    }
}
