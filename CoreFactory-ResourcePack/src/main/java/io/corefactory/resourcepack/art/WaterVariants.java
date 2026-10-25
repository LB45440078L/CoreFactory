package io.corefactory.resourcepack.art;

import io.corefactory.resourcepack.art.MachineArt.Box;
import io.corefactory.resourcepack.art.MachineArt.Face;
import io.corefactory.resourcepack.art.MachineArt.Spec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * The models of the water system, one for every orientation. The game draws each model exactly as it is authored, with no display
 * rotation, so every orientation of a pipe, a splitter and a pump has its own model. The orientation is baked into the geometry and
 * the face textures here, from the canonical models in {@link WaterArt}.
 *
 * <p>Model coordinates are world coordinates: +X is east, +Y is up, +Z is south, and the north face is the face at z 0. A model is
 * baked in a right-handed {@link Frame}: three world faces X', Y' and Z' that the canonical +X, +Y and +Z go to. Each canonical point
 * (relative to the block centre 8, 8, 8) goes to {@code centre + dx X' + dy Y' + dz Z'}, so every box stays axis aligned. A canonical
 * face goes to the world face its outward normal turns into, and it keeps its painter and its shared pattern.
 *
 * <p>The ids are those of {@code WaterModels.all()} in CoreFactory-Core, in the same order, and {@link #ids()} lists them. The
 * pump's impeller is animated on the front face of each pump model (the face that the canonical north face goes to).
 */
public final class WaterVariants {

    /** The ticks each frame of a pump's impeller is shown, the same as the canonical pumps. */
    private static final int FRAME_TIME = 2;

    /** The pump levels: one to five. */
    private static final int LEVELS = 5;

    /** From this level a pump is two blocks tall, so each of its fronts has an upper block as well. */
    private static final int TALL_FROM = 4;

    /** The horizontal faces, which a pump's front may look toward. */
    private static final List<Face> HORIZONTAL = List.of(Face.NORTH, Face.SOUTH, Face.EAST, Face.WEST);

    /** One model to bake: its id, the canonical model it is built from, and the frame that orients it. */
    private record Variant(String id, String canonical, Frame frame) {
    }

    private static final List<Variant> VARIANTS = variants();

    /** Every model id, in the order of WaterModels.all(). */
    private static final List<String> IDS = VARIANTS.stream().map(Variant::id).toList();

    /** Every water model: the pipes, the bends, the arms, the splitters and the pumps, each in its orientation. */
    public static final List<Spec> ALL = bake(WaterArt.WATER, id -> true, UnaryOperator.identity());

    /** The impeller of every pump model: its front face, eight frames, two ticks each. */
    public static final List<MachineAnimation.Clip> CLIPS = VARIANTS.stream()
            .filter(variant -> variant.id().startsWith("water_pump_"))
            .map(variant -> new MachineAnimation.Clip(variant.id(), variant.frame().map(Face.NORTH), FRAME_TIME))
            .toList();

    private WaterVariants() {
    }

    /** Every model id of the water system, in the order of WaterModels.all(). */
    public static List<String> ids() {
        return IDS;
    }

    /** Whether an animated face of this class belongs to the machine id. */
    static boolean owns(String id) {
        return CLIPS.stream().anyMatch(clip -> clip.id().equals(id));
    }

    /**
     * The models, in the order of WaterModels.all(): the straight pipes, the hub, the twelve bends, the six arms, the twelve T
     * splitters, the three crosses, and the pumps, each level's fronts with an upper block after the front that has one.
     */
    private static List<Variant> variants() {
        List<Variant> variants = new ArrayList<>();
        // A straight pipe runs along its axis: X' is the axis, and Y' is up when the pipe lies flat, so its light band stays on top.
        variants.add(new Variant("water_pipe", "water_pipe", Frame.ofXY(Face.EAST, Face.UP)));
        variants.add(new Variant("water_pipe_z", "water_pipe", Frame.ofXY(Face.SOUTH, Face.UP)));
        variants.add(new Variant("water_pipe_y", "water_pipe", Frame.ofXY(Face.UP, Face.EAST)));
        variants.add(new Variant("water_pipe_hub", "water_pipe_hub", Frame.ofXY(Face.EAST, Face.UP)));
        Face[] faces = Face.values();
        for (Face a : faces) {
            for (Face b : faces) {
                if (a.ordinal() < b.ordinal() && a != b.opposite()) {
                    variants.add(bend(a, b));
                }
            }
        }
        for (Face face : faces) {
            variants.add(new Variant("water_pipe_arm_" + token(face), "water_pipe_arm",
                    Frame.ofXY(face, face.horizontal() ? Face.UP : Face.EAST)));
        }
        for (Face input : faces) {
            for (Face output : faces) {
                if (output != input && output != input.opposite() && output.ordinal() < output.opposite().ordinal()) {
                    // The canonical -X end is the input, and the canonical +Z end is one of the outputs. The T is symmetric across
                    // its input, so either output may be +Z: when the input and the outputs are horizontal, the sign is chosen so
                    // that Y' is up. A vertical input keeps the named output as +Z.
                    Frame frame = Frame.ofXZ(input.opposite(), output);
                    if (input.horizontal() && output.horizontal() && frame.y() != Face.UP) {
                        frame = Frame.ofXZ(input.opposite(), output.opposite());
                    }
                    variants.add(new Variant("water_splitter_t_" + token(input) + "_" + token(output), "water_splitter_t", frame));
                }
            }
        }
        // The canonical cross lies in the horizontal plane: xz is the identity, xy turns its +Z arms up and down, zy turns its +X arms.
        variants.add(new Variant("water_splitter_x_xz", "water_splitter_x", Frame.ofXY(Face.EAST, Face.UP)));
        variants.add(new Variant("water_splitter_x_xy", "water_splitter_x", Frame.ofXZ(Face.EAST, Face.UP)));
        variants.add(new Variant("water_splitter_x_zy", "water_splitter_x", Frame.ofXZ(Face.SOUTH, Face.UP)));
        for (int level = 1; level <= LEVELS; level++) {
            for (Face front : HORIZONTAL) {
                // The canonical north face is the front, so it goes to the front, with Y' up.
                Frame frame = Frame.ofYZ(Face.UP, front.opposite());
                String id = "water_pump_" + level + "_" + token(front);
                variants.add(new Variant(id, pumpCanonical(level), frame));
                if (level >= TALL_FROM) {
                    variants.add(new Variant(id + "_top", pumpCanonical(level) + "_top", frame));
                }
            }
        }
        return List.copyOf(variants);
    }

    /** The canonical pump of a level: water_pump for level one, and water_pump_n above it. */
    private static String pumpCanonical(int level) {
        return level == 1 ? "water_pump" : "water_pump_" + level;
    }

    /**
     * A bend between two faces that are not opposite, with the canonical -X end at one face and the +Z end at the other. Both
     * orderings give the same arc, so the one whose Y' is up is taken, which keeps the bend's light band on top when it lies flat.
     */
    private static Variant bend(Face p, Face q) {
        Frame byP = Frame.ofXZ(p.opposite(), q);
        Frame byQ = Frame.ofXZ(q.opposite(), p);
        Frame frame = byP.y() != Face.UP && byQ.y() == Face.UP ? byQ : byP;
        return new Variant("water_pipe_bend_" + token(p) + "_" + token(q), "water_pipe_bend", frame);
    }

    /**
     * The models of the variants that {@code keep} accepts, baked in a palette and in the order of the variants. Each keeps its shape,
     * its orientation and its painters, and {@code ids} names it. The oil tubes bake the same variants in their own palette and ids.
     */
    static List<Spec> bake(WaterArt.Palette palette, Predicate<String> keep, UnaryOperator<String> ids) {
        List<Spec> canonical = WaterArt.canonical(palette);
        return VARIANTS.stream().filter(variant -> keep.test(variant.id()))
                .map(variant -> build(variant, canonical, ids.apply(variant.id()))).toList();
    }

    /** The model of one canonical geometry turned into its frame, with its painters on the faces it now has. */
    private static Spec build(Variant variant, List<Spec> canonicals, String id) {
        Spec canonical = canonical(variant.canonical(), canonicals);
        Frame frame = variant.frame();
        Map<Face, ComponentArt.Painter> faces = new EnumMap<>(Face.class);
        for (Face face : Face.values()) {
            ComponentArt.Painter listed = canonical.faces().get(face);
            ComponentArt.Painter painter = listed != null ? listed
                    : face == Face.NORTH ? canonical.front() : unlisted(face);
            faces.put(frame.map(face), painter);
        }
        List<Box> boxes = canonical.boxes().stream().map(frame::map).toList();
        return new Spec(id, canonical.base(), canonical.accent(), boxes, canonical.front(), faces);
    }

    /** The shared pattern of a canonical side that has no painter of its own, painted on that side wherever it goes. */
    private static ComponentArt.Painter unlisted(Face face) {
        return (c, base, accent, seed) -> MachineArt.paintShared(c, face, base, accent, seed);
    }

    /** The canonical model with an id, from the canonical models of {@link WaterArt} in a palette. */
    private static Spec canonical(String id, List<Spec> canonicals) {
        return canonicals.stream()
                .filter(spec -> spec.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no canonical water model " + id));
    }

    private static String token(Face face) {
        return face.name().toLowerCase(Locale.ROOT);
    }

    /** The unit outward normal of a face: north is -Z, south +Z, east +X, west -X, up +Y and down -Y. */
    private static int[] vector(Face face) {
        return switch (face) {
            case NORTH -> new int[]{0, 0, -1};
            case SOUTH -> new int[]{0, 0, 1};
            case EAST -> new int[]{1, 0, 0};
            case WEST -> new int[]{-1, 0, 0};
            case UP -> new int[]{0, 1, 0};
            case DOWN -> new int[]{0, -1, 0};
        };
    }

    private static Face face(int[] vector) {
        for (Face face : Face.values()) {
            if (Arrays.equals(vector(face), vector)) {
                return face;
            }
        }
        throw new IllegalArgumentException("not a face normal: " + Arrays.toString(vector));
    }

    private static int[] cross(int[] a, int[] b) {
        return new int[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    /**
     * A right-handed frame of three world faces: the faces that canonical +X, +Y and +Z go to. Each frame is built from two of its
     * axes, and the third follows from the right-hand rule.
     *
     * @param x the world face that canonical +X goes to
     * @param y the world face that canonical +Y goes to
     * @param z the world face that canonical +Z goes to
     */
    private record Frame(Face x, Face y, Face z) {

        /** The frame with X' and Y' given: Z' is X' cross Y'. */
        static Frame ofXY(Face x, Face y) {
            return new Frame(x, y, face(cross(vector(x), vector(y))));
        }

        /** The frame with X' and Z' given: Y' is Z' cross X'. */
        static Frame ofXZ(Face x, Face z) {
            return new Frame(x, face(cross(vector(z), vector(x))), z);
        }

        /** The frame with Y' and Z' given: X' is Y' cross Z'. */
        static Frame ofYZ(Face y, Face z) {
            return new Frame(face(cross(vector(y), vector(z))), y, z);
        }

        /** The world face that a canonical face goes to: the face its outward normal turns into. */
        Face map(Face canonical) {
            int[] n = vector(canonical);
            double[] w = offset(n[0], n[1], n[2]);
            return face(new int[]{(int) w[0], (int) w[1], (int) w[2]});
        }

        /** A canonical box in world coordinates: the box of its eight corners, each taken about the block centre. */
        Box map(Box box) {
            double[] lo = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
            double[] hi = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
            for (double cx : new double[]{box.x0(), box.x1()}) {
                for (double cy : new double[]{box.y0(), box.y1()}) {
                    for (double cz : new double[]{box.z0(), box.z1()}) {
                        double[] w = offset(cx - 8, cy - 8, cz - 8);
                        for (int i = 0; i < 3; i++) {
                            lo[i] = Math.min(lo[i], 8 + w[i]);
                            hi[i] = Math.max(hi[i], 8 + w[i]);
                        }
                    }
                }
            }
            return new Box(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
        }

        /** A canonical offset from the block centre, turned into the world: dx X' + dy Y' + dz Z'. */
        private double[] offset(double dx, double dy, double dz) {
            int[] ax = vector(x);
            int[] ay = vector(y);
            int[] az = vector(z);
            double[] w = new double[3];
            for (int i = 0; i < 3; i++) {
                w[i] = dx * ax[i] + dy * ay[i] + dz * az[i];
            }
            return w;
        }
    }
}
