package io.corefactory.resourcepack.art;

import io.corefactory.resourcepack.art.MachineArt.Box;
import io.corefactory.resourcepack.art.MachineArt.Face;
import io.corefactory.resourcepack.art.MachineArt.Spec;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class OilVariantsTest {

    /** The ids of OilModels.all() in CoreFactory-Core, in its order: the water pipe models without the pumps, with the oil prefix. */
    private static final List<String> EXPECTED = List.of(
            "oil_pipe", "oil_pipe_z", "oil_pipe_y", "oil_pipe_hub",
            "oil_pipe_bend_north_east", "oil_pipe_bend_north_west", "oil_pipe_bend_north_up",
            "oil_pipe_bend_north_down", "oil_pipe_bend_south_east", "oil_pipe_bend_south_west",
            "oil_pipe_bend_south_up", "oil_pipe_bend_south_down", "oil_pipe_bend_east_up",
            "oil_pipe_bend_east_down", "oil_pipe_bend_west_up", "oil_pipe_bend_west_down",
            "oil_pipe_arm_north", "oil_pipe_arm_south", "oil_pipe_arm_east", "oil_pipe_arm_west",
            "oil_pipe_arm_up", "oil_pipe_arm_down",
            "oil_splitter_t_north_east", "oil_splitter_t_north_up", "oil_splitter_t_south_east",
            "oil_splitter_t_south_up", "oil_splitter_t_east_north", "oil_splitter_t_east_up",
            "oil_splitter_t_west_north", "oil_splitter_t_west_up", "oil_splitter_t_up_north",
            "oil_splitter_t_up_east", "oil_splitter_t_down_north", "oil_splitter_t_down_east",
            "oil_splitter_x_xz", "oil_splitter_x_xy", "oil_splitter_x_zy");

    private static final String ACCENT = "#E8731E";
    private static final String BAND = "#FFA04A";

    @Test
    void theIdsAreTheOilModelsInTheirOrder() {
        assertThat(EXPECTED).hasSize(37);
        assertThat(OilVariants.ids()).containsExactlyElementsOf(EXPECTED);
        assertThat(OilVariants.ALL.stream().map(Spec::id).toList()).containsExactlyElementsOf(EXPECTED);
    }

    /** No pump: every oil model is a tube, and none is a water model. */
    @Test
    void thereIsNoPumpAndNoWaterId() {
        for (String id : OilVariants.ids()) {
            assertThat(id).startsWith("oil_").doesNotContain("pump").doesNotStartWith("water_");
        }
    }

    /** Every oil model is in the machine list exactly once, so it is written, and its item model with it. */
    @Test
    void everyOilModelIsOneMachineModel() {
        List<String> machines = MachineArt.ALL.stream().map(Spec::id).toList();
        for (String id : EXPECTED) {
            assertThat(machines.stream().filter(id::equals).count()).as(id).isEqualTo(1);
        }
        assertThat(machines.stream().filter(id -> id.startsWith("oil_")).count()).isEqualTo(EXPECTED.size());
    }

    /** Each oil model has the shape of its water twin: the same boxes and the same painted faces, so only the palette differs. */
    @Test
    void eachOilModelHasTheShapeOfItsWaterTwin() {
        for (String id : EXPECTED) {
            Spec oil = spec(id);
            Spec water = WaterVariants.ALL.stream().filter(s -> s.id().equals("water_" + id.substring("oil_".length())))
                    .findFirst().orElseThrow();
            assertThat(oil.boxes()).as(id).isEqualTo(water.boxes());
            assertThat(oil.faces().keySet()).as(id).isEqualTo(water.faces().keySet());
        }
    }

    @Test
    void everyBendHasFlangesOnItsTwoFaces() {
        for (String id : EXPECTED.stream().filter(id -> id.startsWith("oil_pipe_bend_")).toList()) {
            String[] parts = id.split("_");
            Face p = face(parts[3]);
            Face q = face(parts[4]);
            assertThat(WaterVariantsTest.flangeFaces(spec(id))).as(id).isEqualTo(EnumSet.of(p, q));
        }
    }

    @Test
    void everyTeeHasFlangesOnItsInputAndBothOutputs() {
        for (String id : EXPECTED.stream().filter(id -> id.startsWith("oil_splitter_t_")).toList()) {
            String[] parts = id.split("_");
            Face input = face(parts[3]);
            Face output = face(parts[4]);
            assertThat(WaterVariantsTest.flangeFaces(spec(id))).as(id).isEqualTo(EnumSet.of(input, output, output.opposite()));
        }
    }

    @Test
    void everyArmHasAFlangeOnItsFace() {
        for (String id : EXPECTED.stream().filter(id -> id.startsWith("oil_pipe_arm_")).toList()) {
            assertThat(WaterVariantsTest.flangeFaces(spec(id))).as(id)
                    .isEqualTo(EnumSet.of(face(id.substring(id.lastIndexOf('_') + 1))));
        }
    }

    @Test
    void everyStraightPipeHasFlangesOnItsEnds() {
        assertThat(WaterVariantsTest.flangeFaces(spec("oil_pipe"))).isEqualTo(EnumSet.of(Face.EAST, Face.WEST));
        assertThat(WaterVariantsTest.flangeFaces(spec("oil_pipe_z"))).isEqualTo(EnumSet.of(Face.NORTH, Face.SOUTH));
        assertThat(WaterVariantsTest.flangeFaces(spec("oil_pipe_y"))).isEqualTo(EnumSet.of(Face.UP, Face.DOWN));
    }

    @Test
    void everyCrossHasFlangesOnItsFourArms() {
        assertThat(WaterVariantsTest.flangeFaces(spec("oil_splitter_x_xz")))
                .isEqualTo(EnumSet.of(Face.EAST, Face.WEST, Face.NORTH, Face.SOUTH));
        assertThat(WaterVariantsTest.flangeFaces(spec("oil_splitter_x_xy")))
                .isEqualTo(EnumSet.of(Face.EAST, Face.WEST, Face.UP, Face.DOWN));
        assertThat(WaterVariantsTest.flangeFaces(spec("oil_splitter_x_zy")))
                .isEqualTo(EnumSet.of(Face.NORTH, Face.SOUTH, Face.UP, Face.DOWN));
    }

    /** The baked boxes stay inside the block, and each has volume. */
    @Test
    void everyOilBoxIsInsideTheBlock() {
        for (Spec spec : OilVariants.ALL) {
            assertThat(spec.boxes()).as(spec.id()).isNotEmpty();
            for (Box box : spec.boxes()) {
                for (double v : new double[]{box.x0(), box.y0(), box.z0(), box.x1(), box.y1(), box.z1()}) {
                    assertThat(v).as(spec.id()).isBetween(0.0, 16.0);
                }
                assertThat(box.x0()).as(spec.id()).isLessThan(box.x1());
                assertThat(box.y0()).as(spec.id()).isLessThan(box.y1());
                assertThat(box.z0()).as(spec.id()).isLessThan(box.z1());
            }
        }
    }

    /** The three item sprites exist, are dark, and carry the orange accent; the water sprites they follow are brighter. */
    @Test
    void theThreeSpritesAreDarkWithOrangeAccents() {
        List<String> sprites = List.of("oil_pipe", "oil_splitter_t", "oil_splitter_x");
        assertThat(OilVariants.SPRITES.stream().map(ComponentArt.Spec::id).toList()).containsExactlyElementsOf(sprites);
        assertThat(ComponentArt.ALL.stream().map(ComponentArt.Spec::id).toList()).containsAll(sprites);
        List<String> waterTwins = List.of("water_pipe", "water_splitter_t", "water_splitter_x");
        for (int i = 0; i < sprites.size(); i++) {
            String id = sprites.get(i);
            ComponentArt.Spec spec = ComponentArt.ALL.stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
            Canvas canvas = new Canvas();
            ComponentArt.paint(canvas, spec);
            BufferedImage image = canvas.toImage();
            int accent = Color.hex(ACCENT);
            int accentPixels = 0;
            for (int y = 0; y < Canvas.SIZE; y++) {
                for (int x = 0; x < Canvas.SIZE; x++) {
                    if (image.getRGB(x, y) == accent) {
                        accentPixels++;
                    }
                }
            }
            assertThat(accentPixels).as(id + " carries the accent " + ACCENT).isPositive();
            assertThat(meanLuma(image)).as(id + " is dark").isLessThan(meanLuma(waterSprite(waterTwins.get(i))));
            assertThat(meanLuma(image)).as(id + " is dark").isLessThan(110);
        }
    }

    /** Every opaque pixel of every face of every oil model is warm: red is never below blue, so no water blue shows. */
    @Test
    void everyOilFaceIsWarmNotWaterBlue() {
        for (Spec spec : OilVariants.ALL) {
            for (Face face : Face.values()) {
                Canvas canvas = new Canvas();
                MachineArt.paintFace(canvas, spec, face);
                BufferedImage image = canvas.toImage();
                for (int y = 0; y < Canvas.SIZE; y++) {
                    for (int x = 0; x < Canvas.SIZE; x++) {
                        int argb = image.getRGB(x, y);
                        if (Color.alpha(argb) == 255) {
                            assertThat(Color.red(argb)).as(spec.id() + " " + face + " at " + x + "," + y)
                                    .isGreaterThanOrEqualTo(Color.blue(argb));
                        }
                    }
                }
            }
        }
    }

    /** The palette is the one of the oil tube: its accent and light band are the orange ones, and the bore is the dark one. */
    @Test
    void theOilPaletteIsDarkWithOrangeAccents() {
        assertThat(OilVariants.PALETTE.base()).isEqualTo("#2B2724");
        assertThat(OilVariants.PALETTE.accent()).isEqualTo(ACCENT);
        assertThat(OilVariants.PALETTE.light()).isEqualTo(BAND);
        assertThat(OilVariants.PALETTE.bore()).isEqualTo("#0B0907");
        for (Spec spec : OilVariants.ALL) {
            assertThat(spec.base()).as(spec.id()).isEqualTo("#2B2724");
            assertThat(spec.accent()).as(spec.id()).isEqualTo(ACCENT);
        }
    }

    private static BufferedImage waterSprite(String id) {
        ComponentArt.Spec spec = ComponentArt.ALL.stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
        Canvas canvas = new Canvas();
        ComponentArt.paint(canvas, spec);
        return canvas.toImage();
    }

    /** The mean luma of the opaque pixels, from 0 to 255. */
    private static double meanLuma(BufferedImage image) {
        double sum = 0;
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if (Color.alpha(argb) == 255) {
                    sum += 0.299 * Color.red(argb) + 0.587 * Color.green(argb) + 0.114 * Color.blue(argb);
                    count++;
                }
            }
        }
        return sum / count;
    }

    private static Spec spec(String id) {
        return OilVariants.ALL.stream().filter(spec -> spec.id().equals(id)).findFirst().orElseThrow();
    }

    private static Face face(String name) {
        return Face.valueOf(name.toUpperCase(Locale.ROOT));
    }
}
