package io.corefactory.spigot.logistics;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Dir;
import io.corefactory.core.logistics.PlacementReason;

import java.util.function.Supplier;

/**
 * The verdict on placing a belt, picker or splitter against the face of the block a player aims at. A picker and a splitter
 * need a side face (north, south, east or west), an input that is a machine, a generator, a belt flowing into the spot or a straight
 * belt flowing past its side, and a spot that is free of blocks, devices and entities. A picker also takes from a vanilla storage
 * block (a chest, a barrel, a shulker box, a copper chest) on its input side; a splitter does not. A belt needs only the spot. The network's own rules (OCCUPIED, MERGE,
 * BLOCKED) are asked last, through {@link Supplier}, so they are only checked for a spot that is otherwise valid. The order of the
 * checks is the order the player's refusal names: the side, the research, the input, the space, then the network. Pure.
 *
 * <p>Left-clicks are decided here too: a left-click with a logistics item in hand does not break a device.
 */
public final class SidePlacement {

    /** What the aimed block is, as far as the input of a picker or a splitter goes. */
    public enum Input {
        /** A machine or a generator. Its port may give nothing yet; the device still waits for items. */
        PORT,
        /** A belt whose flow goes into the spot. */
        BELT_INTO,
        /** A straight belt that flows past the spot's side: a picker beside it takes from it. */
        BELT_SIDE,
        /** A vanilla storage block: a chest, a trapped chest, a barrel, a shulker box or a copper chest. Only a picker takes from it. */
        STORAGE,
        /** Any other block, or a belt that flows away from the spot, or a corner beside it, or a device that gives nothing. */
        OTHER
    }

    /** What stands in the spot the device would fill. */
    public enum Space {
        /** Air, a liquid or a plant that gives way: the spot is free. */
        FREE,
        /** A solid block. */
        SOLID,
        /** A device or a machine. */
        DEVICE,
        /** An entity that is not a display, such as a player, an item or a mob. */
        ENTITY
    }

    /** The verdict. {@link #OK} lets the device go in. */
    public enum Verdict {
        OK,
        /** A picker or a splitter was aimed at a top or bottom face. */
        NOT_SIDE,
        /** The research that unlocks the item is not finished. */
        LOCKED,
        /** The aimed block cannot feed a picker or a splitter. */
        NO_INPUT,
        /** The spot is a solid block. */
        SOLID,
        /** An entity stands in the spot. */
        ENTITY,
        /** Another device or machine stands in the spot. */
        OCCUPIED,
        /** The placement would give a belt a second input. */
        MERGE,
        /** The placement would feed a belt head-on, or feed a picker or splitter from a side it does not take. */
        BLOCKED
    }

    private SidePlacement() {
    }

    /**
     * The verdict of one placement.
     *
     * @param kind       the kind of device
     * @param face       the face offset the player aimed through, one component of magnitude one
     * @param input      what the aimed block is
     * @param researched true when the player has the research for the item
     * @param space      what stands in the spot the device would fill
     * @param network    the network's reason for the spot, asked only when the spot is otherwise valid
     * @return the verdict
     */
    public static Verdict decide(LogisticsIds.Kind kind, Pos face, Input input, boolean researched, Space space,
                                 Supplier<PlacementReason> network) {
        boolean belt = kind == LogisticsIds.Kind.BELT;
        if (!belt && PlacementAim.side(face).isEmpty()) {
            return Verdict.NOT_SIDE;
        }
        if (!researched) {
            return Verdict.LOCKED;
        }
        if (!belt && !takesFrom(kind, input)) {
            return Verdict.NO_INPUT;
        }
        switch (space) {
            case SOLID -> {
                return Verdict.SOLID;
            }
            case ENTITY -> {
                return Verdict.ENTITY;
            }
            case DEVICE -> {
                return Verdict.OCCUPIED;
            }
            case FREE -> {
            }
        }
        return switch (network.get()) {
            case OK -> Verdict.OK;
            case OCCUPIED -> Verdict.OCCUPIED;
            case MERGE -> Verdict.MERGE;
            case BLOCKED -> Verdict.BLOCKED;
        };
    }

    /**
     * Whether a picker or a splitter can take from an input. A picker takes from a belt beside it as well as from a belt that flows
     * into it, and from a vanilla storage block. A splitter takes only from the belt behind it, so a belt beside a splitter, and a
     * storage block, is not an input for it.
     */
    private static boolean takesFrom(LogisticsIds.Kind kind, Input input) {
        return switch (input) {
            case OTHER -> false;
            case BELT_SIDE, STORAGE -> kind == LogisticsIds.Kind.PICKER;
            case BELT_INTO, PORT -> true;
        };
    }

    /**
     * What a belt is, as the input of a picker placed on the face {@code toward} of the belt's block. A belt that flows into the spot
     * is the input from the front. A straight belt that flows past the spot's side is the input from the side. A belt that flows
     * away from the spot, or a corner beside it, is not an input.
     *
     * @param flow     the belt's flow
     * @param toward   the direction from the belt to the spot
     * @param straight true when the belt is a straight unit, not a corner
     * @return {@link Input#BELT_INTO}, {@link Input#BELT_SIDE} or {@link Input#OTHER}
     */
    public static Input beltInput(Dir flow, Dir toward, boolean straight) {
        if (flow == toward) {
            return Input.BELT_INTO;
        }
        if (flow == toward.opposite() || !straight) {
            return Input.OTHER;
        }
        return Input.BELT_SIDE;
    }

    /**
     * Whether a left-click breaks a logistics device. A left-click with a belt, picker or splitter in hand does nothing to the
     * device, so a device is not broken by accident while the player is placing one; with any other item or an empty hand it
     * breaks the device as before.
     *
     * @param holdingLogistics true when the item in the main hand is a belt, picker or splitter
     */
    public static boolean breaksOnLeftClick(boolean holdingLogistics) {
        return !holdingLogistics;
    }
}
