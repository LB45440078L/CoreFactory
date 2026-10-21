package io.corefactory.resourcepack.art;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** The process machines' models: their silhouettes, their animated strips, and their inventory sprites. No files are written. */
class ProcessMachineArtTest {

    private static final List<String> IDS = List.of("induction_furnace", "powder_mill", "cartridge_press",
            "bullet_swaging_machine", "ammunition_assembly", "metal_lathe", "component_assembly", "heavy_weapons_assembly");

    @Test
    void everyProcessMachineHasAModelAndAnInventorySprite() {
        List<String> models = ProcessMachineArt.ALL.stream().map(MachineArt.Spec::id).toList();
        assertThat(models).containsExactlyInAnyOrderElementsOf(IDS);
        assertThat(ProcessMachineArt.SPRITES.stream().map(ComponentArt.Spec::id).toList())
                .containsExactlyInAnyOrderElementsOf(IDS);
    }

    @Test
    void everyModelBoxIsInsideOneBlockAndHasVolume() {
        for (MachineArt.Spec spec : ProcessMachineArt.ALL) {
            assertThat(spec.boxes()).as(spec.id()).isNotEmpty();
            for (MachineArt.Box box : spec.boxes()) {
                for (double v : new double[]{box.x0(), box.y0(), box.z0(), box.x1(), box.y1(), box.z1()}) {
                    assertThat(v).as(spec.id()).isBetween(0.0, 16.0);
                }
                assertThat(box.x0()).as(spec.id()).isLessThan(box.x1());
                assertThat(box.y0()).as(spec.id()).isLessThan(box.y1());
                assertThat(box.z0()).as(spec.id()).isLessThan(box.z1());
            }
        }
    }

    @Test
    void thePressesAndTheHeavyAssemblyFillMoreOfTheBlockThanTheOthers() {
        double press = volume(ProcessMachineArt.ALL, "cartridge_press");
        double heavy = volume(ProcessMachineArt.ALL, "heavy_weapons_assembly");
        for (String id : List.of("induction_furnace", "powder_mill", "bullet_swaging_machine", "ammunition_assembly",
                "metal_lathe", "component_assembly")) {
            assertThat(volume(ProcessMachineArt.ALL, id)).as(id).isLessThan(press);
        }
        assertThat(heavy).isGreaterThanOrEqualTo(press);
    }

    @Test
    void everyAnimatedFaceIsAStripOfSquareFramesAndIsTheNorthFace() {
        for (ProcessMachineArt.Clip clip : ProcessMachineArt.CLIPS) {
            MachineArt.Spec spec = ProcessMachineArt.ALL.stream().filter(s -> s.id().equals(clip.id())).findFirst().orElseThrow();
            assertThat(clip.face()).as(clip.id()).isEqualTo(MachineArt.Face.NORTH);
            assertThat(clip.frametime()).as(clip.id()).isBetween(2, 4);
            BufferedImage strip = ProcessMachineArt.strip(spec, clip);
            assertThat(strip.getWidth()).as(clip.id()).isEqualTo(MachineAnimation.FRAME);
            assertThat(strip.getHeight()).as(clip.id()).isEqualTo(MachineAnimation.FRAME * MachineAnimation.FRAMES);
        }
    }

    @Test
    void everyAnimationChangesFromFrameToFrame() {
        for (ProcessMachineArt.Clip clip : ProcessMachineArt.CLIPS) {
            Set<Long> digests = new HashSet<>();
            for (int frame = 0; frame < MachineAnimation.FRAMES; frame++) {
                Canvas canvas = new Canvas();
                MachineArt.Spec spec = ProcessMachineArt.ALL.stream().filter(s -> s.id().equals(clip.id())).findFirst().orElseThrow();
                MachineArt.paintCasing(canvas, spec, clip.face());
                ProcessMachineArt.paintFrame(canvas, clip.id(), Color.hex(spec.base()), Color.hex(spec.accent()), frame);
                digests.add(canvas.digest());
            }
            assertThat(digests).as(clip.id()).hasSizeGreaterThan(MachineAnimation.FRAMES / 2);
        }
    }

    private static double volume(List<MachineArt.Spec> specs, String id) {
        double total = 0;
        for (MachineArt.Spec spec : specs) {
            if (spec.id().equals(id)) {
                for (MachineArt.Box box : spec.boxes()) {
                    total += (box.x1() - box.x0()) * (box.y1() - box.y0()) * (box.z1() - box.z0());
                }
            }
        }
        return total;
    }
}
