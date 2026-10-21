package io.corefactory.resourcepack.art;

import java.util.List;

/**
 * The sprites of in-world effects: fireballs, shock rings, clouds, sparks. Each is a 32 x 32 texture on a transparent
 * background, shown by an item display. A sprite is either soft, a glow or a cloud that fades to nothing at its edge, or
 * hard, a shape with a crisp outline, as suits its name.
 */
public final class ParticleArt {

    /** Paints one sprite's pixels onto a transparent canvas. */
    @FunctionalInterface
    public interface Painter {
        void paint(Canvas c, long seed);
    }

    /**
     * One sprite's look.
     *
     * @param id      the sprite's name, which is its file name and its item model name
     * @param painter draws the sprite
     */
    public record Spec(String id, Painter painter) {
    }

    /** Every particle sprite. */
    public static final List<Spec> ALL = List.of(
            new Spec("fireball_core", ParticleArt::fireballCore),
            new Spec("fireball_ember", ParticleArt::fireballEmber),
            new Spec("shock_ring", ParticleArt::shockRing),
            new Spec("smoke_puff", ParticleArt::smokePuff),
            new Spec("steam_puff", ParticleArt::steamPuff),
            new Spec("dust_cloud", ParticleArt::dustCloud),
            new Spec("neutron_spark", ParticleArt::neutronSpark),
            new Spec("radiation_trefoil", ParticleArt::radiationTrefoil),
            new Spec("antimatter_flare", ParticleArt::antimatterFlare),
            new Spec("tesseract_shard", ParticleArt::tesseractShard),
            new Spec("research_spark", ParticleArt::researchSpark),
            new Spec("geiger_tick", ParticleArt::geigerTick),
            new Spec("crystal_glint", ParticleArt::crystalGlint),
            new Spec("machine_steam", ParticleArt::machineSteam),
            new Spec("fluorine_fume", ParticleArt::fluorineFume));

    private ParticleArt() {
    }

    /** Paints one sprite onto a transparent canvas. */
    public static void paint(Canvas c, Spec spec) {
        c.clear(0x00000000);
        spec.painter().paint(c, spec.id().hashCode());
    }

    private static void fireballCore(Canvas c, long seed) {
        glow(c, 16, 16, 15, Color.hex("#FF6A10"), 0.45, 1.5);
        glow(c, 16, 16, 11, Color.hex("#FFA030"), 0.6, 1.3);
        glow(c, 16, 16, 7, Color.hex("#FFD040"), 0.9, 1.0);
        c.paint(Area.disc(16, 16, 4), Color.hex("#FFF6C8"));
        c.paint(Area.disc(16, 16, 2), Color.WHITE);
    }

    private static void fireballEmber(Canvas c, long seed) {
        glow(c, 16, 16, 10, Color.hex("#FF4A10"), 0.6, 1.6);
        c.paint(Area.disc(16, 16, 3.5), Color.hex("#FF5A1A"));
        c.paint(Area.disc(16, 16, 1.4), Color.hex("#FFD080"));
        c.paint(Area.disc(8, 10, 0.9), Color.hex("#FFB060"));
        c.paint(Area.disc(24, 22, 0.9), Color.hex("#FFB060"));
    }

    private static void shockRing(Canvas c, long seed) {
        c.paint(Area.ring(16, 16, 12, 3), Color.withAlpha(Color.hex("#7FD8FF"), 0.45));
        c.paint(Area.ring(16, 16, 12, 1.2), Color.hex("#F0FBFF"));
    }

    private static void smokePuff(Canvas c, long seed) {
        cloud(c, Color.hex("#8A9099"), seed, 0.75, new double[][]{
                {12, 15, 7}, {20, 14, 8}, {16, 20, 8}, {10, 21, 6}, {22, 21, 6}, {16, 11, 6}});
    }

    private static void steamPuff(Canvas c, long seed) {
        cloud(c, Color.hex("#F4F7FA"), seed, 0.6, new double[][]{
                {11, 16, 8}, {21, 15, 8}, {16, 12, 7}, {16, 21, 7}});
    }

    private static void dustCloud(Canvas c, long seed) {
        cloud(c, Color.hex("#8C7E6C"), seed, 0.6, new double[][]{
                {9, 18, 7}, {16, 16, 9}, {23, 18, 7}, {13, 23, 6}, {20, 11, 6}, {25, 25, 5}});
    }

    private static void machineSteam(Canvas c, long seed) {
        cloud(c, Color.hex("#D8CFC4"), seed, 0.55, new double[][]{
                {15, 17, 8}, {22, 14, 6}, {10, 13, 6}, {17, 9, 5}});
    }

    private static void fluorineFume(Canvas c, long seed) {
        cloud(c, Color.hex("#B8F0C4"), seed, 0.55, new double[][]{
                {12, 16, 7}, {20, 15, 7}, {16, 20, 7}, {16, 11, 5}});
    }

    private static void neutronSpark(Canvas c, long seed) {
        glow(c, 16, 16, 13, Color.hex("#5AB8FF"), 0.35, 1.2);
        c.paint(Area.polygon(16, 2, 18.5, 13.5, 30, 16, 18.5, 18.5, 16, 30, 13.5, 18.5, 2, 16, 13.5, 13.5),
                Color.hex("#D6F2FF"));
        c.paint(Area.disc(16, 16, 2.5), Color.WHITE);
    }

    private static void radiationTrefoil(Canvas c, long seed) {
        int yellow = Color.hex("#FFE000");
        for (int k = 0; k < 3; k++) {
            double a = -Math.PI / 2 + k * 2 * Math.PI / 3;
            c.paint(Area.arc(16, 16, 5, 15, a - Math.PI / 6, a + Math.PI / 6), yellow);
        }
        c.paint(Area.disc(16, 16, 3.2), yellow);
    }

    private static void antimatterFlare(Canvas c, long seed) {
        glow(c, 16, 16, 15, Color.hex("#8A5CFF"), 0.35, 1.4);
        Icon.FLARE.draw(c, 16, 16, 14, Color.hex("#E6DBFF"), Color.hex("#1A0830"));
    }

    private static void tesseractShard(Canvas c, long seed) {
        int ink = Color.hex("#FF4BE0");
        double w = 1.4;
        c.paint(Area.line(7, 9, 20, 9, w), ink);
        c.paint(Area.line(20, 9, 20, 22, w), ink);
        c.paint(Area.line(20, 22, 7, 22, w), ink);
        c.paint(Area.line(7, 22, 7, 9, w), ink);
        c.paint(Area.line(11, 5, 24, 5, w), ink);
        c.paint(Area.line(24, 5, 24, 18, w), ink);
        c.paint(Area.line(24, 18, 11, 18, w), ink);
        c.paint(Area.line(11, 18, 11, 5, w), ink);
        c.paint(Area.line(7, 9, 11, 5, w), ink);
        c.paint(Area.line(20, 9, 24, 5, w), ink);
        c.paint(Area.line(20, 22, 24, 18, w), ink);
        c.paint(Area.line(7, 22, 11, 18, w), ink);
    }

    private static void researchSpark(Canvas c, long seed) {
        glow(c, 16, 16, 12, Color.hex("#5CFF8A"), 0.4, 1.5);
        c.paint(Area.polygon(16, 6, 18, 14, 26, 6, 18, 18, 16, 26, 14, 18, 6, 26, 14, 14), Color.hex("#7CFFA8"));
        c.paint(Area.disc(16, 16, 2), Color.hex("#F0FFF6"));
    }

    private static void geigerTick(Canvas c, long seed) {
        glow(c, 16, 16, 9, Color.hex("#FFE000"), 0.35, 1.2);
        c.paint(Area.line(13, 12, 19, 20, 2.4), Color.hex("#FFE000"));
    }

    private static void crystalGlint(Canvas c, long seed) {
        glow(c, 16, 16, 15, Color.WHITE, 0.3, 1.6);
        c.paint(Area.polygon(16, 2, 22, 16, 16, 30, 10, 16), Color.WHITE);
        c.paint(Area.line(16, 2, 16, 30, 0.6), Color.withAlpha(Color.hex("#A0C8E8"), 0.8));
        c.paint(Area.line(10, 16, 22, 16, 0.6), Color.withAlpha(Color.hex("#A0C8E8"), 0.8));
    }

    /** A soft disc, opaque at its centre and fading to nothing at its edge. {@code power} sets how fast it fades. */
    private static void glow(Canvas c, double cx, double cy, double r, int colour, double peak, double power) {
        c.shade(Area.disc(cx, cy, r), (x, y, current) -> {
            double d = Math.hypot(x + 0.5 - cx, y + 0.5 - cy) / r;
            return Color.over(current, Color.withAlpha(colour, peak * Math.pow(1 - d, power)));
        });
    }

    /** Soft puffs of one colour, each {@code {x, y, radius}}, with the opacity broken up by grain. */
    private static void cloud(Canvas c, int colour, long seed, double peak, double[][] puffs) {
        for (double[] puff : puffs) {
            glow(c, puff[0], puff[1], puff[2], colour, peak, 1.2);
        }
        c.shade(Area.rect(0, 0, Canvas.SIZE, Canvas.SIZE), (x, y, current) -> {
            double k = 0.8 + 0.4 * Noise.smooth(x / 3.0, y / 3.0, seed);
            return Color.withAlpha(current, Color.alpha(current) / 255.0 * k);
        });
    }
}
