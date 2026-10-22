package io.corefactory.core.drill;

import io.corefactory.core.geology.Placement;

/**
 * The block region a drill works in: a square centred on the drill, measured on x and z, and a depth that goes down
 * from the drill's own layer. An even area puts the extra block on the low side, so a 10 wide square runs from x - 5
 * to x + 4, and an odd area is centred, so a 9 wide square runs from x - 4 to x + 4.
 *
 * @param minX the lowest x
 * @param minY the lowest y
 * @param minZ the lowest z
 * @param maxX the highest x
 * @param maxY the highest y, the drill's own layer
 * @param maxZ the highest z
 */
public record DrillBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    /** The region of a drill at this block, with the area and depth of its spec. */
    public static DrillBox around(int x, int y, int z, DrillSpec spec) {
        int low = spec.area() / 2;
        int high = spec.area() - 1 - low;
        return new DrillBox(x - low, y - spec.depth() + 1, z - low, x + high, y, z + high);
    }

    /** True when the block lies inside the region. */
    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /** True when the placement lies inside the region. */
    public boolean contains(Placement placement) {
        return contains(placement.x(), placement.y(), placement.z());
    }
}
