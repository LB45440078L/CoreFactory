package io.corefactory.core.engine;

/**
 * Integer-grid iteration over the shapes used by the mechanics. All scans are clamped to the world's
 * vertical range, so callers pass {@code world.getMinHeight()} and {@code world.getMaxHeight() - 1}.
 */
public final class Shapes {

    private Shapes() {
    }

    /** Receives one block coordinate. */
    @FunctionalInterface
    public interface Voxel {
        void accept(int x, int y, int z);
    }

    /** Decides whether an offset from the centre belongs to the shape. */
    @FunctionalInterface
    public interface Offset {
        boolean test(double dx, double dy, double dz);
    }

    /** Visits every block of the solid sphere of radius {@code r}. */
    public static void sphere(int cx, int cy, int cz, double r, int minY, int maxY, Voxel visitor) {
        double r2 = r * r;
        scan(cx, cy, cz, r, minY, maxY, (dx, dy, dz) -> dx * dx + dy * dy + dz * dz <= r2, visitor);
    }

    /**
     * Visits every block within {@code reach} of the centre whose offset passes {@code filter}.
     * {@code reach} bounds the box that is scanned, so it must cover the whole shape.
     */
    public static void scan(int cx, int cy, int cz, double reach, int minY, int maxY, Offset filter, Voxel visitor) {
        int ri = (int) Math.ceil(reach);
        int yFrom = Math.max(minY, cy - ri);
        int yTo = Math.min(maxY, cy + ri);
        for (int y = yFrom; y <= yTo; y++) {
            double dy = y - cy;
            for (int x = cx - ri; x <= cx + ri; x++) {
                double dx = x - cx;
                for (int z = cz - ri; z <= cz + ri; z++) {
                    if (filter.test(dx, dy, z - cz)) {
                        visitor.accept(x, y, z);
                    }
                }
            }
        }
    }
}
