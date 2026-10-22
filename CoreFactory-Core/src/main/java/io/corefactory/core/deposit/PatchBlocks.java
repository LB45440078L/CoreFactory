package io.corefactory.core.deposit;

import io.corefactory.core.geology.OreDefinition;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * The rule that recognises the patch blocks of the deposits: the ore blocks that the laying of a chunk put down, and that a
 * player can mine by hand for their deposit. A block is a patch site of a deposit when the deposit covers its column, the ore
 * is the deposit's ore, the block's material is that ore's host, its y lies in the ore's height range, and
 * {@link OreBlocks#shows} holds for it. These are the same conditions the laying uses, so every site is a block the laying
 * could have made.
 *
 * <p>A site is not yet a patch block. A block that a player, a piston or a dispenser put on the site is excluded by the
 * server, which keeps the record of those positions. The rule reads only the position and the material, so it is pure.
 */
public final class PatchBlocks {

    private PatchBlocks() {
    }

    /**
     * @param deposit  the deposit
     * @param ore      the ore definition that the deposit's ore id names
     * @param material the vanilla material name of the block
     * @param x        the block's x
     * @param y        the block's y
     * @param z        the block's z
     * @param density  the ore block density of the server, from 0 to 1
     * @return true when the block is a patch site of the deposit
     */
    public static boolean isSite(Deposit deposit, OreDefinition ore, String material, int x, int y, int z, double density) {
        Objects.requireNonNull(deposit, "deposit");
        Objects.requireNonNull(ore, "ore");
        return ore.id().equals(deposit.ore())
                && ore.host().equals(material)
                && deposit.contains(x, z)
                && y >= ore.minY()
                && y <= ore.maxY()
                && OreBlocks.shows(deposit.id(), x, y, z, density);
    }

    /**
     * The first of the deposits, in list order, for which the block is a patch site.
     *
     * @param deposits the deposits that may cover the block, ordered by id; the first match wins
     * @param ores     the ore definition of an ore id, or null for an unknown id; such a deposit never matches
     * @param material the vanilla material name of the block
     * @param x        the block's x
     * @param y        the block's y
     * @param z        the block's z
     * @param density  the ore block density of the server, from 0 to 1
     * @return the deposit that the block is a patch site of, or empty
     */
    public static Optional<Deposit> siteOf(List<Deposit> deposits, Function<String, OreDefinition> ores, String material,
                                           int x, int y, int z, double density) {
        Objects.requireNonNull(deposits, "deposits");
        Objects.requireNonNull(ores, "ores");
        for (Deposit deposit : deposits) {
            OreDefinition ore = ores.apply(deposit.ore());
            if (ore != null && isSite(deposit, ore, material, x, y, z, density)) {
                return Optional.of(deposit);
            }
        }
        return Optional.empty();
    }
}
