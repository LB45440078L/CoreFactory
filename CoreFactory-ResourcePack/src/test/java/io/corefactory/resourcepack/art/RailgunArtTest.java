package io.corefactory.resourcepack.art;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The railgun turret's models: their levels, their pivots, their screen and their charge animation. The numbers are the ones the Paper
 * side copies, so a change here must be made there too.
 */
class RailgunArtTest {

    private static final List<String> BODIES = List.of("railgun_1_body", "railgun_2_body", "railgun_3_body");
    private static final List<String> BARRELS = List.of("railgun_1_barrel", "railgun_2_barrel", "railgun_3_barrel");

    /** Every level is taller than the one below: one, one and a half, and two and a half blocks. */
    @Test
    void everyLevelIsTallerThanTheOneBelow() {
        assertThat(RailgunArt.modelHeight("railgun_1_body")).isEqualTo(16);
        assertThat(RailgunArt.modelHeight("railgun_2_body")).isEqualTo(24);
        assertThat(RailgunArt.modelHeight("railgun_3_barrel")).isEqualTo(40);
        assertThat(RailgunArt.modelHeight("railgun_3_top")).isEqualTo(40);
    }

    /** Each body stands on its block: its floor is the model's frame, at y -16, and its top is inside its level. */
    @Test
    void everyBodyStandsOnItsFloorAndStaysInsideItsLevel() {
        for (String id : BODIES) {
            double[] span = span(machine(id));
            assertThat(span[0]).as(id + " floor").isEqualTo(-16);
            assertThat(span[1]).as(id + " top").isLessThanOrEqualTo(RailgunArt.modelHeight(id) - 16);
        }
    }

    /** The top of level III starts where its body ends, two blocks up, and stands on the body. */
    @Test
    void theTopOfLevelThreeSitsOnTheBodyTwoBlocksUp() {
        assertThat(RailgunArt.displayOffset("railgun_3_top")).isEqualTo(2);
        assertThat(RailgunArt.displayOffset("railgun_3_body")).isZero();
        assertThat(RailgunArt.displayOffset("railgun_3_barrel")).isZero();
        assertThat(RailgunArt.displayLift("railgun_3_top")).isEqualTo(1);
        assertThat(span(machine("railgun_3_top"))[0]).isEqualTo(-16);
        assertThat(span(machine("railgun_3_body"))[1]).isEqualTo(16);
    }

    /**
     * Each barrel tilts about its trunnion, which lies inside the barrel's span. The trunnion is the middle of the barrel's cross-section,
     * and the plugin turns the barrel about it at the middle of the block: 0.75, 1.25 and 1.625 blocks for levels I to III.
     */
    @Test
    void eachBarrelTiltsAboutATrunnionInsideItsSpan() {
        double[] trunnions = {0.75, 1.25, 1.625};
        for (int level = 1; level <= 3; level++) {
            String barrel = "railgun_" + level + "_barrel";
            assertThat(RailgunArt.trunnionBlocks(barrel)).as(barrel).isEqualTo(trunnions[level - 1]);
            double pivot = RailgunArt.trunnionBlocks(barrel) * 16 - 16;
            double[] span = span(machine(barrel));
            assertThat(pivot).as(barrel).isBetween(span[0], span[1]);
        }
        assertThatThrownBy(() -> RailgunArt.trunnionBlocks("railgun_1_body")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RailgunArt.displayLift("artillery_1_body")).isInstanceOf(IllegalArgumentException.class);
    }

    /** The screen is a flat rectangle on the front of every body: a box whose north face, at its lowest z, is the screen's plane. */
    @Test
    void theScreenIsFlatOnTheFrontOfEveryBody() {
        double x0 = RailgunArt.SCREEN[0];
        double y0 = RailgunArt.SCREEN[1];
        double x1 = RailgunArt.SCREEN[2];
        double y1 = RailgunArt.SCREEN[3];
        double z = RailgunArt.SCREEN[4];
        for (String id : BODIES) {
            assertThat(machine(id).boxes()).as(id).anyMatch(box -> box.z0() == z && box.x0() <= x0 && box.x1() >= x1
                    && box.y0() <= y0 && box.y1() >= y1);
        }
    }

    /**
     * The charge animation: each animated front is a strip of frames, the brightest frame is the last one at full charge, and the first
     * frame, the empty charge, is the darkest.
     */
    @Test
    void theBrightestFrameIsFullChargeAndTheDarkestIsEmpty() {
        for (MachineArt.Spec spec : RailgunArt.ALL) {
            BufferedImage strip = MachineAnimation.strip(spec, MachineArt.Face.NORTH);
            assertThat(strip.getHeight()).as(spec.id()).isEqualTo(RailgunArt.FRAMES * MachineAnimation.FRAME);
            double[] light = new double[RailgunArt.FRAMES];
            for (int frame = 0; frame < RailgunArt.FRAMES; frame++) {
                light[frame] = brightness(strip, frame);
            }
            int last = RailgunArt.FRAMES - 1;
            for (int frame = 0; frame < last; frame++) {
                assertThat(light[frame]).as(spec.id() + " frame " + frame).isLessThan(light[last]);
            }
            for (int frame = 1; frame <= last; frame++) {
                assertThat(light[0]).as(spec.id() + " empty against frame " + frame).isLessThan(light[frame]);
            }
        }
        assertThat(MachineAnimation.clip("railgun_3_body")).isPresent();
        assertThat(MachineAnimation.clip("railgun_3_body").orElseThrow().frametime()).isEqualTo(RailgunArt.FRAMETIME);
    }

    /** The turret, the rail coil and the rail barrel each have an inventory sprite. */
    @Test
    void theTurretAndItsPartsHaveInventorySprites() {
        assertThat(RailgunArt.SPRITES.stream().map(ComponentArt.Spec::id).toList())
                .containsExactly("railgun_turret", "rail_coil", "rail_barrel");
    }

    private static MachineArt.Spec machine(String id) {
        return RailgunArt.ALL.stream().filter(spec -> spec.id().equals(id)).findFirst().orElseThrow();
    }

    /** The lowest and highest model coordinates of a model along y. */
    private static double[] span(MachineArt.Spec spec) {
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (MachineArt.Box box : spec.boxes()) {
            low = Math.min(low, box.y0());
            high = Math.max(high, box.y1());
        }
        return new double[]{low, high};
    }

    /** The light of one frame of a strip: the sum of its colour channels, weighted by alpha. */
    private static double brightness(BufferedImage strip, int frame) {
        double sum = 0;
        for (int y = 0; y < MachineAnimation.FRAME; y++) {
            for (int x = 0; x < MachineAnimation.FRAME; x++) {
                int argb = strip.getRGB(x, frame * MachineAnimation.FRAME + y);
                double alpha = Color.alpha(argb) / 255.0;
                sum += alpha * (Color.red(argb) + Color.green(argb) + Color.blue(argb));
            }
        }
        return sum;
    }
}
