package io.corefactory.resourcepack.art;

/**
 * A region of a canvas, tested at pixel centres. Coordinates are continuous, so pixel {@code i} is covered when
 * {@code i + 0.5} is inside the region. Regions combine with {@link #and}, {@link #or} and {@link #minus}.
 */
@FunctionalInterface
public interface Area {

    boolean covers(double x, double y);

    default Area and(Area other) {
        return (x, y) -> covers(x, y) && other.covers(x, y);
    }

    default Area or(Area other) {
        return (x, y) -> covers(x, y) || other.covers(x, y);
    }

    default Area minus(Area other) {
        return (x, y) -> covers(x, y) && !other.covers(x, y);
    }

    static Area disc(double cx, double cy, double r) {
        return (x, y) -> square(x - cx) + square(y - cy) <= r * r;
    }

    static Area ellipse(double cx, double cy, double rx, double ry) {
        return (x, y) -> square((x - cx) / rx) + square((y - cy) / ry) <= 1;
    }

    /** A ring of width {@code w} centred on radius {@code r}. */
    static Area ring(double cx, double cy, double r, double w) {
        return (x, y) -> Math.abs(Math.hypot(x - cx, y - cy) - r) <= w / 2;
    }

    /** A tilted elliptical band: the normalised radius stays within {@code 1 +- w}. */
    static Area orbit(double cx, double cy, double rx, double ry, double angle, double w) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return (x, y) -> {
            double dx = x - cx;
            double dy = y - cy;
            double u = dx * cos + dy * sin;
            double v = -dx * sin + dy * cos;
            return Math.abs(Math.sqrt(square(u / rx) + square(v / ry)) - 1) <= w;
        };
    }

    static Area rect(double x0, double y0, double x1, double y1) {
        return (x, y) -> x >= x0 && x <= x1 && y >= y0 && y <= y1;
    }

    static Area roundRect(double x0, double y0, double x1, double y1, double r) {
        return (x, y) -> {
            if (x < x0 || x > x1 || y < y0 || y > y1) {
                return false;
            }
            double cx = Math.clamp(x, x0 + r, x1 - r);
            double cy = Math.clamp(y, y0 + r, y1 - r);
            return square(x - cx) + square(y - cy) <= r * r;
        };
    }

    /** A straight stroke of width {@code w} between two points, with round ends. */
    static Area line(double x0, double y0, double x1, double y1, double w) {
        return (x, y) -> {
            double dx = x1 - x0;
            double dy = y1 - y0;
            double len2 = dx * dx + dy * dy;
            double t = len2 == 0 ? 0 : Math.clamp(((x - x0) * dx + (y - y0) * dy) / len2, 0, 1);
            return square(x0 + t * dx - x) + square(y0 + t * dy - y) <= (w / 2) * (w / 2);
        };
    }

    /** A filled polygon given as {@code x0, y0, x1, y1, ...}. Even-odd fill. */
    static Area polygon(double... xy) {
        return (x, y) -> {
            boolean inside = false;
            int n = xy.length / 2;
            for (int i = 0, j = n - 1; i < n; j = i++) {
                double xi = xy[2 * i];
                double yi = xy[2 * i + 1];
                double xj = xy[2 * j];
                double yj = xy[2 * j + 1];
                if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                    inside = !inside;
                }
            }
            return inside;
        };
    }

    /** An annular sector between radii {@code r0} and {@code r1}, swept from angle {@code a0} to {@code a1}. */
    static Area arc(double cx, double cy, double r0, double r1, double a0, double a1) {
        return (x, y) -> {
            double d = Math.hypot(x - cx, y - cy);
            return d >= r0 && d <= r1 && sweeps(Math.atan2(y - cy, x - cx), a0, a1);
        };
    }

    static Area union(Area... areas) {
        return (x, y) -> {
            for (Area area : areas) {
                if (area.covers(x, y)) {
                    return true;
                }
            }
            return false;
        };
    }

    private static boolean sweeps(double angle, double a0, double a1) {
        double rel = angle - a0;
        rel -= Math.floor(rel / (2 * Math.PI)) * 2 * Math.PI;
        return rel <= a1 - a0;
    }

    private static double square(double value) {
        return value * value;
    }
}
