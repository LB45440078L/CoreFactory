package io.corefactory.resourcepack.art;

/**
 * The tile behind the CoreFactory tab of the Advancements screen: dark painted metal with rivets and a warning band at the
 * top and bottom. It repeats seamlessly, because every pattern here has a period that divides the tile size.
 */
public final class BackgroundArt {

    /** The pack path of the tile, as the advancement's {@code background} refers to it. */
    public static final String PATH = "textures/gui/advancements/background.png";

    private static final long SEED = "corefactory-background".hashCode();

    private BackgroundArt() {
    }

    public static void paint(Canvas c) {
        int base = Color.hex("#23252B");
        int warning = Color.hex("#8A2A14");
        c.shade(Area.rect(0, 0, Canvas.SIZE, Canvas.SIZE), (x, y, current) -> {
            double grain = (Noise.hash(x, y, SEED) - 0.5) * 0.10;
            double mottle = (Noise.smooth(x / 4.0, y / 4.0, SEED) - 0.5) * 0.12;
            return Color.scale(base, 1 + grain + mottle);
        });
        // Warning bands along the top and bottom edges, the same on both, so the tiles join without a seam.
        Area bands = Area.rect(0, 0, Canvas.SIZE, 3).or(Area.rect(0, 29, Canvas.SIZE, Canvas.SIZE));
        c.shade(bands, (x, y, current) -> ((x + y) / 4) % 2 == 0 ? Color.mix(current, warning, 0.85) : current);
        // A rivet at every crossing of an 8-pixel grid.
        int rivet = Color.scale(base, 0.55);
        for (int x = 4; x < Canvas.SIZE; x += 8) {
            for (int y = 8; y < 24; y += 8) {
                c.set(x, y, rivet);
            }
        }
    }
}
