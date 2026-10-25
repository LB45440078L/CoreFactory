package io.corefactory.resourcepack.art;

import io.corefactory.resourcepack.art.MachineArt.Box;
import io.corefactory.resourcepack.art.MachineArt.Face;
import io.corefactory.resourcepack.art.MachineArt.Spec;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WaterVariantsTest {

    private static final double EPSILON = 0.01;

    /** The ids of WaterModels.all() in CoreFactory-Core, in its order. The two lists are checked against each other by hand. */
    private static final List<String> EXPECTED = List.of(
            "water_pipe", "water_pipe_z", "water_pipe_y", "water_pipe_hub",
            "water_pipe_bend_north_east", "water_pipe_bend_north_west", "water_pipe_bend_north_up",
            "water_pipe_bend_north_down", "water_pipe_bend_south_east", "water_pipe_bend_south_west",
            "water_pipe_bend_south_up", "water_pipe_bend_south_down", "water_pipe_bend_east_up",
            "water_pipe_bend_east_down", "water_pipe_bend_west_up", "water_pipe_bend_west_down",
            "water_pipe_arm_north", "water_pipe_arm_south", "water_pipe_arm_east", "water_pipe_arm_west",
            "water_pipe_arm_up", "water_pipe_arm_down",
            "water_splitter_t_north_east", "water_splitter_t_north_up", "water_splitter_t_south_east",
            "water_splitter_t_south_up", "water_splitter_t_east_north", "water_splitter_t_east_up",
            "water_splitter_t_west_north", "water_splitter_t_west_up", "water_splitter_t_up_north",
            "water_splitter_t_up_east", "water_splitter_t_down_north", "water_splitter_t_down_east",
            "water_splitter_x_xz", "water_splitter_x_xy", "water_splitter_x_zy",
            "water_pump_1_north", "water_pump_1_south", "water_pump_1_east", "water_pump_1_west",
            "water_pump_2_north", "water_pump_2_south", "water_pump_2_east", "water_pump_2_west",
            "water_pump_3_north", "water_pump_3_south", "water_pump_3_east", "water_pump_3_west",
            "water_pump_4_north", "water_pump_4_north_top", "water_pump_4_south", "water_pump_4_south_top",
            "water_pump_4_east", "water_pump_4_east_top", "water_pump_4_west", "water_pump_4_west_top",
            "water_pump_5_north", "water_pump_5_north_top", "water_pump_5_south", "water_pump_5_south_top",
            "water_pump_5_east", "water_pump_5_east_top", "water_pump_5_west", "water_pump_5_west_top");

    @Test
    void theIdsAreTheWaterModelsInTheirOrder() {
        assertThat(WaterVariants.ids()).containsExactlyElementsOf(EXPECTED);
        assertThat(WaterVariants.ALL.stream().map(Spec::id).toList()).containsExactlyElementsOf(EXPECTED);
    }

    /** Every water model is in the machine list exactly once, so it is written, and its item model with it. */
    @Test
    void everyWaterModelIsOneMachineModel() {
        List<String> machines = MachineArt.ALL.stream().map(Spec::id).toList();
        for (String id : EXPECTED) {
            assertThat(machines.stream().filter(id::equals).count()).as(id).isEqualTo(1);
        }
        assertThat(machines.stream().filter(id -> id.startsWith("water_")).count()).isEqualTo(EXPECTED.size());
    }

    @Test
    void everyBendHasFlangesOnItsTwoFaces() {
        for (String id : EXPECTED.stream().filter(id -> id.startsWith("water_pipe_bend_")).toList()) {
            String[] parts = id.split("_");
            Face p = face(parts[3]);
            Face q = face(parts[4]);
            assertThat(flangeFaces(spec(id))).as(id).isEqualTo(EnumSet.of(p, q));
            assertThat(flangeFaces(spec(id))).as(id).contains(p, q);
        }
    }

    @Test
    void everyTeeHasFlangesOnItsInputAndBothOutputs() {
        for (String id : EXPECTED.stream().filter(id -> id.startsWith("water_splitter_t_")).toList()) {
            String[] parts = id.split("_");
            Face input = face(parts[3]);
            Face output = face(parts[4]);
            assertThat(flangeFaces(spec(id))).as(id).isEqualTo(EnumSet.of(input, output, output.opposite()));
        }
    }

    @Test
    void everyArmHasAFlangeOnItsFace() {
        for (String id : EXPECTED.stream().filter(id -> id.startsWith("water_pipe_arm_")).toList()) {
            assertThat(flangeFaces(spec(id))).as(id).isEqualTo(EnumSet.of(face(id.substring(id.lastIndexOf('_') + 1))));
        }
    }

    @Test
    void everyStraightPipeHasFlangesOnItsEnds() {
        assertThat(flangeFaces(spec("water_pipe"))).isEqualTo(EnumSet.of(Face.EAST, Face.WEST));
        assertThat(flangeFaces(spec("water_pipe_z"))).isEqualTo(EnumSet.of(Face.NORTH, Face.SOUTH));
        assertThat(flangeFaces(spec("water_pipe_y"))).isEqualTo(EnumSet.of(Face.UP, Face.DOWN));
    }

    @Test
    void everyCrossHasFlangesOnItsFourArms() {
        assertThat(flangeFaces(spec("water_splitter_x_xz"))).isEqualTo(EnumSet.of(Face.EAST, Face.WEST, Face.NORTH, Face.SOUTH));
        assertThat(flangeFaces(spec("water_splitter_x_xy"))).isEqualTo(EnumSet.of(Face.EAST, Face.WEST, Face.UP, Face.DOWN));
        assertThat(flangeFaces(spec("water_splitter_x_zy"))).isEqualTo(EnumSet.of(Face.NORTH, Face.SOUTH, Face.UP, Face.DOWN));
    }

    /**
     * Each pump's impeller clip is on its front face, the face its id names, and no other. Every pump has its clip registered
     * under its own id, with two ticks a frame, and there are 28 of them.
     */
    @Test
    void everyPumpAnimatesItsImpellerOnItsFrontFace() {
        List<String> pumps = EXPECTED.stream().filter(id -> id.startsWith("water_pump_")).toList();
        assertThat(pumps).hasSize(28);
        assertThat(MachineAnimation.ALL.stream().filter(clip -> clip.id().startsWith("water_pump_")).count()).isEqualTo(28);
        for (String id : pumps) {
            Face front = face(id.split("_")[3]);
            MachineAnimation.Clip clip = MachineAnimation.clip(id).orElseThrow(() -> new AssertionError(id + " has no clip"));
            assertThat(clip.face()).as(id).isEqualTo(front);
            assertThat(clip.frametime()).as(id).isEqualTo(2);
            Spec spec = spec(id);
            for (Face face : Face.values()) {
                assertThat(MachineAnimation.animates(spec, face)).as(id + " " + face).isEqualTo(face == front);
            }
        }
    }

    /** The baked boxes stay inside the block, so the model is written as a model of one block. */
    @Test
    void everyWaterBoxIsInsideTheBlock() {
        for (Spec spec : WaterVariants.ALL) {
            for (Box box : spec.boxes()) {
                for (double v : coords(box)) {
                    assertThat(v).as(spec.id()).isBetween(0.0, 16.0);
                }
            }
        }
    }

    /**
     * The faces that carry a flange. A flange is a box that reaches the face's plane and is centred on it (its two coordinates along
     * the face are both 8, within {@link #EPSILON}). A face has a flange when such boxes span eight units across the face in both
     * directions, which is the flange's disc. The pipe's body is six units across, so it is not a flange.
     */
    static Set<Face> flangeFaces(Spec spec) {
        Set<Face> faces = EnumSet.noneOf(Face.class);
        for (Face face : Face.values()) {
            List<double[]> plate = spec.boxes().stream().map(WaterVariantsTest::coords)
                    .filter(c -> reachesPlane(c, face) && centred(c, face)).toList();
            if (plate.isEmpty()) {
                continue;
            }
            int[] axes = axes(face);
            boolean wide = true;
            for (int axis : axes) {
                double lo = plate.stream().mapToDouble(c -> c[axis]).min().orElseThrow();
                double hi = plate.stream().mapToDouble(c -> c[axis + 3]).max().orElseThrow();
                wide &= Math.abs(hi - lo - 8) <= EPSILON;
            }
            if (wide) {
                faces.add(face);
            }
        }
        return faces;
    }

    /** The minimum and maximum corners of a box as {x0, y0, z0, x1, y1, z1}. */
    private static double[] coords(Box box) {
        return new double[]{box.x0(), box.y0(), box.z0(), box.x1(), box.y1(), box.z1()};
    }

    /** Whether a box's extent reaches the plane of a face: its boundary on that side lies on the block's face. */
    private static boolean reachesPlane(double[] c, Face face) {
        return switch (face) {
            case NORTH -> Math.abs(c[2]) <= EPSILON;
            case SOUTH -> Math.abs(c[5] - 16) <= EPSILON;
            case EAST -> Math.abs(c[3] - 16) <= EPSILON;
            case WEST -> Math.abs(c[0]) <= EPSILON;
            case UP -> Math.abs(c[4] - 16) <= EPSILON;
            case DOWN -> Math.abs(c[1]) <= EPSILON;
        };
    }

    /** Whether a box is centred on a face: both of its coordinates along the face are 8, within {@link #EPSILON}. */
    private static boolean centred(double[] c, Face face) {
        for (int axis : axes(face)) {
            if (Math.abs((c[axis] + c[axis + 3]) / 2 - 8) > EPSILON) {
                return false;
            }
        }
        return true;
    }

    /** The two coordinate axes along a face's plane, as indices of the minimum corner: x 0, y 1, z 2. */
    private static int[] axes(Face face) {
        return switch (face) {
            case NORTH, SOUTH -> new int[]{0, 1};
            case EAST, WEST -> new int[]{1, 2};
            case UP, DOWN -> new int[]{0, 2};
        };
    }

    private static Spec spec(String id) {
        return WaterVariants.ALL.stream().filter(spec -> spec.id().equals(id)).findFirst().orElseThrow();
    }

    private static Face face(String name) {
        return Face.valueOf(name.toUpperCase(Locale.ROOT));
    }
}
