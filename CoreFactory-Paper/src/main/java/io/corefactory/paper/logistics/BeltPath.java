package io.corefactory.paper.logistics;

import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.logistics.BeltShape;
import io.corefactory.core.logistics.Dir;

/**
 * Where an item shows on a belt unit: the offset from the centre of the block, in blocks, for a position along the unit. A
 * straight unit runs from its back edge to its front edge. A corner runs from the edge its items come in by, through the centre,
 * to the edge they leave by: a straight line in two parts, which is an approximation of the curve for the display only. Pure.
 */
public final class BeltPath {

    private BeltPath() {
    }

    /**
     * The item model key of a belt's display for a shape. The straight shape uses the straight model. The left corner uses the
     * corner model, and the right corner uses its own model, the corner with its top mirrored in the texture: a display is never
     * mirrored with a negative scale, because that turns the faces inside out.
     *
     * @param componentId the belt's component id, such as {@code belt_1}
     * @param shape        the unit's shape
     * @return the key, without the namespace
     */
    public static String modelKeyOf(String componentId, BeltShape shape) {
        return switch (shape) {
            case STRAIGHT -> ItemFactory.beltKey(componentId, false);
            case CORNER_LEFT -> ItemFactory.beltKey(componentId, true);
            case CORNER_RIGHT -> ItemFactory.beltKey(componentId, true) + "_right";
        };
    }

    /**
     * The item model key of a belt duplex's display: the anchor cell's half is {@code machine/<componentId>}, and the second cell's half is
     * {@code machine/<componentId>_right}. Both are machine-kind models, as the resource pack's art defines them, turned so the flow is the
     * model's north.
     *
     * @param componentId the duplex's component id, such as {@code belt_duplex_1}
     * @param second      true for the half on the second cell, false for the anchor cell's half
     * @return the key, without the namespace
     */
    public static String duplexModelKeyOf(String componentId, boolean second) {
        return ItemFactory.modelKey(ItemFactory.MACHINE, second ? componentId + "_right" : componentId);
    }

    /**
     * The offset of a point on a unit.
     *
     * @param shape    the unit's shape
     * @param flow     the unit's flow
     * @param input    the side the unit's items come from; for a straight unit, the back
     * @param position the place along the unit, from 0 at the entry to 1 at the exit
     * @return the offset {x, z} from the block centre
     */
    public static double[] offsetOn(BeltShape shape, Dir flow, Dir input, double position) {
        double p = Math.max(0.0, Math.min(1.0, position));
        double[] out = Orientation.vectorOf(flow);
        double[] in = Orientation.vectorOf(input);
        if (shape == BeltShape.STRAIGHT) {
            return new double[] {out[0] * (p - 0.5), out[1] * (p - 0.5)};
        }
        if (p <= 0.5) {
            double weight = 0.5 * (1.0 - 2.0 * p);
            return new double[] {in[0] * weight, in[1] * weight};
        }
        double weight = 0.5 * (2.0 * p - 1.0);
        return new double[] {out[0] * weight, out[1] * weight};
    }
}
