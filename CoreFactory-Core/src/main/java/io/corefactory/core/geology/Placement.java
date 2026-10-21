package io.corefactory.core.geology;

/**
 * One block an ore vein takes.
 *
 * @param x        block x
 * @param y        block y
 * @param z        block z
 * @param oreIndex index of the ore in the list {@link GeologyService} holds
 */
public record Placement(int x, int y, int z, int oreIndex) {
}
