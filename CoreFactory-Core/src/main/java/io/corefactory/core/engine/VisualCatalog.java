package io.corefactory.core.engine;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The named visual effects and their geometry. Each effect is a shape filled with sprites, and each sprite is one of
 * the particle sprites in the resource pack. The geometry is pure: it says where each sprite starts and ends, when it
 * appears and how long it lives, and the platform turns that into displays.
 *
 * <p>Lengths are in blocks and times in ticks, counted from the moment the effect starts. Every placement ends by the
 * effect's lifetime.
 */
public final class VisualCatalog {

    /** The sprite ids the resource pack ships. Every sprite an effect names is one of these. */
    public static final List<String> SPRITES = List.of(
            "fireball_core", "fireball_ember", "shock_ring", "smoke_puff", "steam_puff", "dust_cloud",
            "neutron_spark", "radiation_trefoil", "antimatter_flare", "tesseract_shard", "research_spark",
            "geiger_tick", "crystal_glint", "machine_steam", "fluorine_fume");

    /** The largest sprite, in blocks, so a huge blast does not paint the sky with one texture. */
    private static final double MAX_SPRITE = 12.0;
    private static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));
    private static final double TAU = Math.PI * 2;
    /** The height at which a mushroom cloud has its base sprite count. Taller clouds get more sprites, up to the factor. */
    private static final double COLUMN_REFERENCE_HEIGHT = 26.0;
    private static final double COLUMN_MAX_FACTOR = 3.0;
    /** The share of a mushroom cloud's sprites that make up its stem. The rest make up the cap. */
    private static final double STEM_SHARE = 0.4;

    /** The shape an effect's sprites are laid out in. */
    public enum Shape {
        /** Sprites spread evenly over a sphere, expanding from the centre. */
        SPHERE_BURST,
        /** Sprites on a flat ring on the ground, expanding out to the radius. */
        RING,
        /** A stem of sprites rising from the ground, and a cap of sprites spreading at the top: a mushroom cloud. */
        COLUMN,
        /** Sprites scattered over a disc, drifting slowly and shrinking: fallout. */
        DRIFT,
        /** One flash that grows, then a sphere of sparks collapsing inward. */
        FLASH,
        /** Sprites orbiting the centre, spiralling outward. */
        SPIRAL,
        /** A few sprites rising in turn: steam. */
        PUFF,
        /** A flat star of sparks around the centre. */
        BURST,
        /** Sprites streaking one after another along a straight line through the centre: a particle beam. */
        BEAM;

        /**
         * The sprites of this shape.
         *
         * @param count    how many sprites the shape is made of; {@link #FLASH} adds one more for its flash
         * @param scale    the shape's size in blocks: a radius, a height or a spread
         * @param sprites  the sprite ids to draw from, cycled in order
         * @param lifetime how long the effect lasts, in ticks
         * @return every sprite of the effect, each ending by {@code lifetime}
         */
        public List<Placement> placements(int count, double scale, List<String> sprites, int lifetime) {
            int n = Math.max(1, count);
            double s = Math.max(0.1, scale);
            int life = Math.max(2, lifetime);
            return switch (this) {
                case SPHERE_BURST -> sphereBurst(n, s, sprites, life);
                case RING -> ring(n, s, sprites, life);
                case COLUMN -> column(n, s, sprites, life);
                case DRIFT -> drift(n, s, sprites, life);
                case FLASH -> flash(n, s, sprites, life);
                case SPIRAL -> spiral(n, s, sprites, life);
                case PUFF -> puff(n, s, sprites, life);
                case BURST -> burst(n, s, sprites, life);
                case BEAM -> beam(n, s, sprites, life);
            };
        }
    }

    /**
     * One sprite of an effect. It is spawned {@code delay} ticks after the effect starts, at {@code from} relative to
     * the centre. Over its {@code lifetime} ticks it moves to {@code offset} and changes size from {@code startScale}
     * to {@code endScale}. Its last {@code fade} ticks it shrinks to nothing.
     *
     * @param offset     where the sprite ends up, relative to the effect's centre
     * @param from       where the sprite starts, relative to the centre
     * @param startScale the sprite's size in blocks when it appears
     * @param endScale   the sprite's size in blocks when it is removed
     * @param sprite     the sprite id, one of {@link #SPRITES}
     * @param delay      ticks after the effect starts before the sprite appears
     * @param lifetime   ticks the sprite lives, counted from its appearance
     * @param fade       ticks at the end of its life spent shrinking to nothing, or 0 for none
     */
    public record Placement(Vector offset, Vector from, double startScale, double endScale, String sprite, int delay,
                            int lifetime, int fade) {

        /** A sprite that is removed where it is at the end of its life, without shrinking. */
        public Placement(Vector offset, Vector from, double startScale, double endScale, String sprite, int delay,
                         int lifetime) {
            this(offset, from, startScale, endScale, sprite, delay, lifetime, 0);
        }
    }

    /**
     * A named effect: its shape, the sprites it draws from, how many sprites it has, and how long it lasts.
     *
     * @param name     the name the core and the platform use to ask for it
     * @param shape    the layout of its sprites
     * @param sprites  the sprite ids, cycled through the shape
     * @param lifetime ticks the effect lasts
     * @param count    the number of sprites in its main body
     */
    public record Effect(String name, Shape shape, List<String> sprites, int lifetime, int count) {
    }

    /** Every named effect the plugin shows. */
    public static final List<Effect> EFFECTS = List.of(
            new Effect("nuclear_fireball", Shape.SPHERE_BURST, List.of("fireball_core", "fireball_ember"), 60, 28),
            new Effect("nuclear_shockwave", Shape.RING, List.of("shock_ring"), 40, 24),
            new Effect("nuclear_surge", Shape.RING, List.of("dust_cloud"), 100, 64),
            new Effect("nuclear_mushroom", Shape.COLUMN, List.of("dust_cloud", "smoke_puff", "steam_puff"), 200, 40),
            new Effect("nuclear_fallout", Shape.DRIFT, List.of("radiation_trefoil"), 100, 40),
            new Effect("neutron_burst", Shape.SPHERE_BURST, List.of("neutron_spark"), 40, 30),
            new Effect("antimatter_flash", Shape.FLASH, List.of("antimatter_flare", "crystal_glint"), 60, 24),
            new Effect("tesseract_fold", Shape.SPIRAL, List.of("tesseract_shard"), 100, 36),
            new Effect("machine_work", Shape.PUFF, List.of("machine_steam"), 30, 4),
            new Effect("research_complete", Shape.BURST, List.of("research_spark"), 40, 16),
            new Effect("ore_break", Shape.BURST, List.of("crystal_glint"), 20, 8),
            new Effect("geiger_crackle", Shape.PUFF, List.of("geiger_tick"), 10, 3),
            new Effect("lab_crusher", Shape.SPHERE_BURST, List.of("dust_cloud", "crystal_glint"), 30, 24),
            new Effect("lab_roaster", Shape.PUFF, List.of("fireball_ember", "smoke_puff"), 50, 18),
            new Effect("lab_separator", Shape.SPIRAL, List.of("research_spark", "steam_puff"), 50, 20),
            new Effect("lab_purifier", Shape.PUFF, List.of("steam_puff"), 60, 12),
            new Effect("lab_converter", Shape.PUFF, List.of("machine_steam", "fluorine_fume"), 60, 16),
            new Effect("lab_enrichment", Shape.SPIRAL, List.of("research_spark", "neutron_spark"), 60, 24),
            new Effect("lab_transmuter", Shape.BURST, List.of("neutron_spark"), 30, 12),
            new Effect("lab_spectrometer", Shape.FLASH, List.of("research_spark", "geiger_tick"), 40, 10),
            new Effect("lab_accelerator", Shape.BEAM, List.of("neutron_spark"), 40, 16),
            new Effect("lab_condenser", Shape.RING, List.of("antimatter_flare", "crystal_glint"), 40, 16));

    private VisualCatalog() {
    }

    /** The effect with this name, or empty when there is none. */
    public static Optional<Effect> find(String name) {
        for (Effect effect : EFFECTS) {
            if (effect.name().equals(name)) {
                return Optional.of(effect);
            }
        }
        return Optional.empty();
    }

    /** Every sprite of an effect at a size. */
    public static List<Placement> placements(Effect effect, double scale) {
        return effect.shape().placements(effect.count(), scale, effect.sprites(), effect.lifetime());
    }

    private static List<Placement> sphereBurst(int count, double scale, List<String> sprites, int lifetime) {
        double size = Math.clamp(scale * 0.45, 1.5, MAX_SPRITE);
        List<Placement> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Vector offset = onSphere(i, count).multiply(scale);
            out.add(new Placement(offset, new Vector(), size * 0.25, size, pick(sprites, i), 0, lifetime));
        }
        return out;
    }

    /** A flat ring on the ground. Its sprites appear over the first tenth of the lifetime and shrink out at the end. */
    private static List<Placement> ring(int count, double scale, List<String> sprites, int lifetime) {
        double size = Math.clamp(scale * 0.35, 1.5, MAX_SPRITE);
        int stagger = lifetime / 10;
        int fade = lifetime / 5;
        List<Placement> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = TAU * i / count;
            Vector offset = new Vector(Math.cos(angle) * scale, 0, Math.sin(angle) * scale);
            Vector from = offset.clone().multiply(0.1);
            int delay = (int) Math.round((double) stagger * i / count);
            out.add(new Placement(offset, from, size * 0.2, size, pick(sprites, i), delay,
                    remaining(lifetime, delay), fade));
        }
        return out;
    }

    /**
     * A mushroom cloud. A stem of dust and smoke puffs rises from the ground over the first three tenths of the
     * lifetime. A cap of puffs then spreads outward from the top of the stem: its rim is dust, its middle smoke and its
     * crown steam. The sprite count is {@code count} times the height over {@link #COLUMN_REFERENCE_HEIGHT}, up to
     * {@link #COLUMN_MAX_FACTOR} times. Every puff shrinks out over the last fifth of the lifetime.
     */
    private static List<Placement> column(int count, double height, List<String> sprites, int lifetime) {
        double factor = Math.clamp(height / COLUMN_REFERENCE_HEIGHT, 1.0, COLUMN_MAX_FACTOR);
        int total = (int) Math.round(count * factor);
        int stems = Math.max(1, (int) Math.round(total * STEM_SHARE));
        int caps = Math.max(1, total - stems);
        String dust = sprites.getFirst();
        String smoke = sprites.get(Math.min(1, sprites.size() - 1));
        String steam = sprites.getLast();
        int rise = (int) Math.round(lifetime * 0.3);
        int fade = lifetime / 5;
        double stemTop = height * 0.7;
        double stemSize = Math.clamp(height * 0.2, 2.0, MAX_SPRITE);
        List<Placement> out = new ArrayList<>(total);
        for (int i = 0; i < stems; i++) {
            double t = (i + 0.5) / stems;
            double angle = i * GOLDEN_ANGLE;
            double narrow = height * 0.06 * (1 - 0.5 * t);
            Vector offset = new Vector(Math.cos(angle) * narrow, height * (0.08 + 0.62 * t), Math.sin(angle) * narrow);
            Vector from = new Vector(offset.getX(), 0, offset.getZ());
            int delay = (int) Math.round(rise * t);
            out.add(new Placement(offset, from, stemSize * 0.4, stemSize, i % 2 == 0 ? dust : smoke, delay,
                    remaining(lifetime, delay), fade));
        }
        double capSize = Math.clamp(height * 0.26, 3.0, MAX_SPRITE);
        for (int i = 0; i < caps; i++) {
            double rho = Math.sqrt((i + 0.5) / caps);
            double crown = 1 - rho * rho;
            double angle = i * GOLDEN_ANGLE;
            double radius = height * 0.42 * rho;
            Vector offset = new Vector(Math.cos(angle) * radius, height * (0.7 + 0.25 * crown),
                    Math.sin(angle) * radius);
            Vector from = new Vector(offset.getX() * 0.25, stemTop, offset.getZ() * 0.25);
            int delay = (int) Math.round(rise * 0.6 + lifetime * 0.2 * rho);
            String sprite = rho > 0.8 ? dust : crown > 0.5 ? steam : smoke;
            out.add(new Placement(offset, from, capSize * 0.4, capSize, sprite, delay,
                    remaining(lifetime, delay), fade));
        }
        return out;
    }

    /**
     * Sprites streaking along a line through the centre, from {@code -scale} to {@code scale} on the x axis. Each streak
     * takes a third of the lifetime, and they start one after another across the lifetime, in two lanes.
     */
    private static List<Placement> beam(int count, double scale, List<String> sprites, int lifetime) {
        int streak = Math.max(2, lifetime / 3);
        double size = Math.clamp(scale * 0.15, 0.8, 2.5);
        List<Placement> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double lane = (i % 2 == 0 ? 0.3 : -0.3) * size;
            int delay = (int) Math.round((double) (lifetime - streak) * i / count);
            out.add(new Placement(new Vector(scale, 0, lane), new Vector(-scale, 0, lane), size, size,
                    pick(sprites, i), delay, streak));
        }
        return out;
    }

    private static List<Placement> drift(int count, double radius, List<String> sprites, int lifetime) {
        double size = Math.clamp(radius * 0.05, 3.0, 10.0);
        Vector wind = new Vector(0.5, 0.25, 0.3).multiply(Math.clamp(radius * 0.02, 1.0, 6.0));
        List<Placement> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double r = radius * Math.sqrt((i + 0.5) / count);
            double angle = i * GOLDEN_ANGLE;
            Vector base = new Vector(Math.cos(angle) * r, 1.0 + 0.5 * (i % 3), Math.sin(angle) * r);
            int delay = (int) Math.round(lifetime * 0.3 * i / count);
            out.add(new Placement(base.clone().add(wind), base, size, size * 0.3, pick(sprites, i), delay,
                    remaining(lifetime, delay)));
        }
        return out;
    }

    private static List<Placement> flash(int count, double scale, List<String> sprites, int lifetime) {
        String flare = sprites.getFirst();
        String spark = sprites.getLast();
        List<Placement> out = new ArrayList<>(count + 1);
        out.add(new Placement(new Vector(), new Vector(), Math.clamp(scale * 0.15, 1.5, 6.0),
                Math.clamp(scale * 0.6, 3.0, MAX_SPRITE), flare, 0, Math.max(1, lifetime / 2)));
        int sparkDelay = lifetime / 3;
        double size = Math.clamp(scale * 0.1, 1.0, 6.0);
        for (int i = 0; i < count; i++) {
            Vector start = onSphere(i, count).multiply(scale);
            out.add(new Placement(new Vector(), start, size, size * 0.4, spark, sparkDelay,
                    remaining(lifetime, sparkDelay)));
        }
        return out;
    }

    private static List<Placement> spiral(int count, double scale, List<String> sprites, int lifetime) {
        double turns = 3.0;
        double size = Math.clamp(scale * 0.08, 2.0, MAX_SPRITE);
        List<Placement> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double t = (double) i / Math.max(1, count - 1);
            double angle = TAU * turns * t;
            Vector from = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(scale * 0.2);
            Vector offset = new Vector(Math.cos(angle + Math.PI / 2), 0.3 * Math.sin(angle),
                    Math.sin(angle + Math.PI / 2)).multiply(scale);
            int delay = (int) Math.round(lifetime * 0.4 * t);
            out.add(new Placement(offset, from, size * 0.3, size, pick(sprites, i), delay, remaining(lifetime, delay)));
        }
        return out;
    }

    private static List<Placement> puff(int count, double scale, List<String> sprites, int lifetime) {
        double size = Math.clamp(scale * 1.6, 1.0, 5.0);
        List<Placement> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double t = (i + 1.0) / count;
            double angle = i * GOLDEN_ANGLE;
            Vector offset = new Vector(Math.cos(angle) * 0.4 * scale, scale * (1.0 + 1.8 * t),
                    Math.sin(angle) * 0.4 * scale);
            Vector from = new Vector(Math.cos(angle) * 0.2 * scale, 0.5 * scale, Math.sin(angle) * 0.2 * scale);
            int delay = (int) Math.round(lifetime * i / (3.0 * count));
            out.add(new Placement(offset, from, Math.clamp(scale * 0.5, 0.5, 3.0), size, pick(sprites, i), delay,
                    remaining(lifetime, delay)));
        }
        return out;
    }

    private static List<Placement> burst(int count, double scale, List<String> sprites, int lifetime) {
        double size = Math.clamp(scale * 0.5, 0.5, 3.0);
        List<Placement> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = TAU * i / count;
            double reach = scale * (i % 2 == 0 ? 1.0 : 0.45);
            Vector offset = new Vector(Math.cos(angle) * reach, 0, Math.sin(angle) * reach);
            out.add(new Placement(offset, new Vector(), size * 0.5, size, pick(sprites, i), 0, lifetime));
        }
        return out;
    }

    /** The i-th of {@code count} points spread evenly over the unit sphere, on a golden spiral. */
    private static Vector onSphere(int i, int count) {
        double y = 1 - 2 * (i + 0.5) / count;
        double ring = Math.sqrt(1 - y * y);
        double angle = i * GOLDEN_ANGLE;
        return new Vector(Math.cos(angle) * ring, y, Math.sin(angle) * ring);
    }

    private static int remaining(int lifetime, int delay) {
        return Math.max(1, lifetime - delay);
    }

    private static String pick(List<String> sprites, int i) {
        return sprites.get(i % sprites.size());
    }
}
