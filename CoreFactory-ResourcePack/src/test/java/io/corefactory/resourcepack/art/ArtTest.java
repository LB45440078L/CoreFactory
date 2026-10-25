package io.corefactory.resourcepack.art;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArtTest {

    @Test
    void everyTextureIsThirtyTwoPixelsSquare() {
        for (TntArt.Spec spec : TntArt.ALL) {
            Canvas canvas = new Canvas();
            TntArt.paint(canvas, spec);
            assertThat(canvas.toImage().getWidth()).as(spec.id()).isEqualTo(Canvas.SIZE);
            assertThat(canvas.toImage().getHeight()).as(spec.id()).isEqualTo(Canvas.SIZE);
        }
        for (ComponentArt.Spec spec : ComponentArt.ALL) {
            Canvas canvas = new Canvas();
            ComponentArt.paint(canvas, spec);
            assertThat(canvas.toImage().getWidth()).as(spec.id()).isEqualTo(Canvas.SIZE);
        }
        for (MachineArt.Spec spec : MachineArt.ALL) {
            for (MachineArt.Face face : MachineArt.Face.values()) {
                Canvas canvas = new Canvas();
                MachineArt.paintFace(canvas, spec, face);
                assertThat(canvas.toImage().getWidth()).as(spec.id() + " " + face).isEqualTo(Canvas.SIZE);
            }
        }
        for (ParticleArt.Spec spec : ParticleArt.ALL) {
            Canvas canvas = new Canvas();
            ParticleArt.paint(canvas, spec);
            assertThat(canvas.toImage().getWidth()).as(spec.id()).isEqualTo(Canvas.SIZE);
        }
    }

    @Test
    void everyOreIsAnOpaqueSixteenPixelBlock() {
        for (OreArt.Spec spec : OreArt.ALL) {
            Canvas canvas = new Canvas();
            OreArt.paint(canvas, spec);
            BufferedImage block = OreArt.block(canvas);
            assertThat(block.getWidth()).as(spec.id()).isEqualTo(OreArt.BLOCK_SIZE);
            assertThat(block.getHeight()).as(spec.id()).isEqualTo(OreArt.BLOCK_SIZE);
            for (int y = 0; y < OreArt.BLOCK_SIZE; y++) {
                for (int x = 0; x < OreArt.BLOCK_SIZE; x++) {
                    assertThat(Color.alpha(block.getRGB(x, y))).as(spec.id() + " at " + x + "," + y).isEqualTo(255);
                }
            }
        }
    }

    /** A particle is on a transparent background: its corners are clear, and it has visible pixels. */
    @Test
    void everyParticleSpriteHasATransparentBorderAndVisiblePixels() {
        assertThat(ParticleArt.ALL.stream().map(ParticleArt.Spec::id).toList()).containsExactlyInAnyOrder(
                "fireball_core", "fireball_ember", "shock_ring", "smoke_puff", "steam_puff", "dust_cloud",
                "neutron_spark", "radiation_trefoil", "antimatter_flare", "tesseract_shard", "research_spark",
                "geiger_tick", "crystal_glint", "machine_steam", "fluorine_fume");
        int last = Canvas.SIZE - 1;
        for (ParticleArt.Spec spec : ParticleArt.ALL) {
            Canvas canvas = new Canvas();
            ParticleArt.paint(canvas, spec);
            for (int[] corner : new int[][]{{0, 0}, {last, 0}, {0, last}, {last, last}}) {
                assertThat(Color.alpha(canvas.get(corner[0], corner[1]))).as(spec.id()).isZero();
            }
            int visible = 0;
            for (int y = 0; y < Canvas.SIZE; y++) {
                for (int x = 0; x < Canvas.SIZE; x++) {
                    if (Color.alpha(canvas.get(x, y)) > 0) {
                        visible++;
                    }
                }
            }
            assertThat(visible).as(spec.id()).isPositive();
        }
    }

    @Test
    void everyTextureIsUnique() {
        Set<Long> digests = new HashSet<>();
        for (TntArt.Spec spec : TntArt.ALL) {
            Canvas canvas = new Canvas();
            TntArt.paint(canvas, spec);
            assertThat(digests.add(canvas.digest())).as("TNT " + spec.id() + " repeats another texture").isTrue();
        }
        for (ComponentArt.Spec spec : ComponentArt.ALL) {
            Canvas canvas = new Canvas();
            ComponentArt.paint(canvas, spec);
            assertThat(digests.add(canvas.digest())).as("component " + spec.id() + " repeats another texture").isTrue();
        }
        for (MachineArt.Spec spec : MachineArt.ALL) {
            for (MachineArt.Face face : MachineArt.Face.values()) {
                Canvas canvas = new Canvas();
                MachineArt.paintFace(canvas, spec, face);
                assertThat(digests.add(canvas.digest())).as("machine " + spec.id() + " " + face + " repeats another texture")
                        .isTrue();
            }
        }
        for (OreArt.Spec spec : OreArt.ALL) {
            Canvas canvas = new Canvas();
            OreArt.paint(canvas, spec);
            assertThat(digests.add(canvas.digest())).as("ore " + spec.id() + " repeats another texture").isTrue();
        }
        for (ParticleArt.Spec spec : ParticleArt.ALL) {
            Canvas canvas = new Canvas();
            ParticleArt.paint(canvas, spec);
            assertThat(digests.add(canvas.digest())).as("particle " + spec.id() + " repeats another texture").isTrue();
        }
        for (BeltArt.Spec spec : BeltArt.ALL) {
            for (BufferedImage image : List.of(BeltArt.strip(spec, false), BeltArt.strip(spec, true), BeltArt.side(spec))) {
                assertThat(digests.add(digest(image))).as("belt " + spec.id() + " repeats another texture").isTrue();
            }
        }
        assertThat(digests).hasSize(TntArt.ALL.size() + ComponentArt.ALL.size() + 6 * MachineArt.ALL.size()
                + OreArt.ALL.size() + ParticleArt.ALL.size() + 3 * BeltArt.ALL.size());
    }

    @Test
    void paintingIsDeterministic() {
        for (TntArt.Spec spec : TntArt.ALL) {
            Canvas first = new Canvas();
            Canvas second = new Canvas();
            TntArt.paint(first, spec);
            TntArt.paint(second, spec);
            assertThat(first.digest()).as(spec.id()).isEqualTo(second.digest());
        }
        for (MachineArt.Spec spec : MachineArt.ALL) {
            for (MachineArt.Face face : MachineArt.Face.values()) {
                Canvas first = new Canvas();
                Canvas second = new Canvas();
                MachineArt.paintFace(first, spec, face);
                MachineArt.paintFace(second, spec, face);
                assertThat(first.digest()).as(spec.id() + " " + face).isEqualTo(second.digest());
            }
        }
        for (OreArt.Spec spec : OreArt.ALL) {
            Canvas first = new Canvas();
            Canvas second = new Canvas();
            OreArt.paint(first, spec);
            OreArt.paint(second, spec);
            assertThat(first.digest()).as(spec.id()).isEqualTo(second.digest());
        }
        for (ParticleArt.Spec spec : ParticleArt.ALL) {
            Canvas first = new Canvas();
            Canvas second = new Canvas();
            ParticleArt.paint(first, spec);
            ParticleArt.paint(second, spec);
            assertThat(first.digest()).as(spec.id()).isEqualTo(second.digest());
        }
    }

    /** Every ore the plugin declares has art, and the art names the same vanilla host block as ores.yml. */
    @Test
    @SuppressWarnings("unchecked")
    void everyOreInOresYmlHasArtOnItsHostBlock() throws Exception {
        Map<String, Object> ores = entries("ores", "ores.yml");
        assertThat(ores.keySet()).containsExactlyInAnyOrderElementsOf(
                OreArt.ALL.stream().map(OreArt.Spec::id).toList());
        for (OreArt.Spec spec : OreArt.ALL) {
            assertThat(((Map<String, Object>) ores.get(spec.id())).get("host")).as(spec.id()).isEqualTo(spec.host());
        }
    }

    /** An animated belt top is a 16 wide strip of {@link BeltArt#FRAMES} square frames: its height is frames x width. */
    @Test
    void everyBeltTopIsAnAnimatedStripOfSquareFrames() {
        assertThat(BeltArt.ALL.stream().map(BeltArt.Spec::id).toList())
                .containsExactly("belt_1", "belt_2", "belt_3");
        for (BeltArt.Spec spec : BeltArt.ALL) {
            for (boolean corner : new boolean[]{false, true}) {
                BufferedImage strip = BeltArt.strip(spec, corner);
                assertThat(strip.getWidth()).as(spec.id()).isEqualTo(BeltArt.TILE);
                assertThat(strip.getHeight()).as(spec.id()).isEqualTo(BeltArt.FRAMES * strip.getWidth());
                for (int frame = 0; frame < BeltArt.FRAMES; frame++) {
                    assertThat(countArrowPixels(strip, frame, spec)).as(spec.id() + " frame " + frame).isPositive();
                }
                assertThat(digest(frame(strip, 0))).as(spec.id()).isNotEqualTo(digest(frame(strip, 1)));
            }
            BufferedImage side = BeltArt.side(spec);
            assertThat(side.getWidth()).isEqualTo(BeltArt.TILE);
            assertThat(side.getHeight()).isEqualTo(BeltArt.TILE);
        }
        assertThat(BeltArt.FRAME_TIME).isEqualTo(2);
    }

    /**
     * The straight arrows move toward the bottom of the texture, the way their tips point: frame k at row y shows what
     * frame 0 shows at row y - k. Only the arrows are compared; the steel grain stays where it is. The strip loops because
     * the arrow pattern repeats every {@link BeltArt#FRAMES} rows, and that divides the tile.
     */
    @Test
    void theStraightArrowsScrollOnePixelPerFrame() {
        for (BeltArt.Spec spec : BeltArt.ALL) {
            int arrow = Color.hex(spec.accent());
            BufferedImage strip = BeltArt.strip(spec, false);
            BufferedImage first = frame(strip, 0);
            for (int frame = 0; frame < BeltArt.FRAMES; frame++) {
                BufferedImage shown = frame(strip, frame);
                for (int y = 0; y < BeltArt.TILE; y++) {
                    for (int x = 0; x < BeltArt.TILE; x++) {
                        boolean onArrow = shown.getRGB(x, y) == arrow;
                        boolean onArrowBefore = first.getRGB(x, (y - frame + BeltArt.TILE) % BeltArt.TILE) == arrow;
                        assertThat(onArrow).as(spec.id() + " frame " + frame + " at " + x + "," + y)
                                .isEqualTo(onArrowBefore);
                    }
                }
            }
        }
    }

    /**
     * Each arrowhead is on the output side of its arrow. Along the arrows, a tip lies further toward the output than the
     * arm pixels behind it, so the mean position of the tips is past the mean position of the arms. The output is the
     * direction the arrows move, toward the bottom edge of the texture on both tops. The corner is compared over the
     * arrows whose arms lie wholly on the arc, because the arc cuts off the far end of the last arrow.
     */
    @Test
    void theArrowheadsAreOnTheOutputSideOfEachArrow() {
        for (BeltArt.Spec spec : BeltArt.ALL) {
            int arrow = Color.hex(spec.accent());
            BufferedImage straight = frame(BeltArt.strip(spec, false), 0);
            BufferedImage corner = frame(BeltArt.strip(spec, true), 0);
            List<Double> straightTips = new ArrayList<>();
            List<Double> straightArms = new ArrayList<>();
            List<Double> cornerTips = new ArrayList<>();
            List<Double> cornerArms = new ArrayList<>();
            for (int y = 0; y < BeltArt.TILE; y++) {
                for (int x = 0; x < BeltArt.TILE; x++) {
                    if (straight.getRGB(x, y) == arrow) {
                        double across = Math.abs(x + 0.5 - BeltArt.TILE / 2.0);
                        if (across < 1) {
                            straightTips.add(y + 0.5);
                        } else if (across >= 2.5) {
                            straightArms.add(y + 0.5);
                        }
                    }
                    if (corner.getRGB(x, y) == arrow) {
                        double dx = x + 0.5 - BeltArt.TILE;
                        double dy = y + 0.5 - BeltArt.TILE;
                        double along = -BeltArt.TILE / 2.0 * Math.atan2(-dy, -dx);
                        double across = Math.abs(Math.hypot(dx, dy) - BeltArt.TILE / 2.0);
                        if (along < -8.5 || along > -0.5) {
                            continue;
                        }
                        if (across < 1) {
                            cornerTips.add(along);
                        } else if (across >= 2.5) {
                            cornerArms.add(along);
                        }
                    }
                }
            }
            assertThat(straightArms).as(spec.id() + " straight arms").isNotEmpty();
            assertThat(cornerArms).as(spec.id() + " corner arms").isNotEmpty();
            assertThat(mean(straightArms)).as(spec.id() + " straight").isLessThan(mean(straightTips));
            assertThat(mean(cornerArms)).as(spec.id() + " corner").isLessThan(mean(cornerTips));
        }
    }

    /** The arrows of a level are painted in that level's colour. */
    @Test
    void theArrowsAreInTheLevelColour() {
        for (BeltArt.Spec spec : BeltArt.ALL) {
            int arrow = Color.hex(spec.accent());
            BufferedImage frame = frame(BeltArt.strip(spec, true), 0);
            int found = 0;
            for (int y = 0; y < BeltArt.TILE; y++) {
                for (int x = 0; x < BeltArt.TILE; x++) {
                    if (frame.getRGB(x, y) == arrow) {
                        found++;
                    }
                }
            }
            assertThat(found).as(spec.id()).isPositive();
        }
    }

    /**
     * Every animated machine face is a strip of {@link MachineAnimation#FRAMES} frames, each {@link MachineAnimation#FRAME}
     * pixels square, and each frame differs from the next, so the face moves and loops.
     */
    @Test
    void everyAnimatedMachineFaceIsAStripOfChangingFrames() {
        // Each water pump model has its own clip on its front, named by WaterVariants: 28 of them.
        List<String> pumpClips = WaterVariants.ids().stream().filter(id -> id.startsWith("water_pump_")).toList();
        assertThat(pumpClips).hasSize(28);
        List<String> clips = new ArrayList<>(List.of(
                "pylon_1", "pylon_2", "pylon_3", "pylon_4", "pylon_5", "accumulator_1", "accumulator_2", "accumulator_3",
                "accumulator_4", "accumulator_5", "wind_generator_top", "charcoal_generator_top", "petroleum_generator_top",
                "diesel_generator_top", "nuclear_generator_top", "ore_drill_4_top", "ore_drill_5_top", "petroleum_drill_4_top",
                "petroleum_drill_5_top", "solar_panel", "crude_oil_refinery", "particle_accelerator",
                "antimatter_condenser", "machine_gun_turret", "laser_turret", "machine_gun_turret_2",
                "machine_gun_turret_3", "machine_gun_turret_4", "machine_gun_turret_5", "laser_turret_2",
                "laser_turret_3_top", "laser_turret_4_top", "laser_turret_5_top", "vertical_belt_1",
                "vertical_belt_2", "vertical_belt_3", "belt_duplex_1", "belt_duplex_1_right", "belt_duplex_2",
                "belt_duplex_2_right", "belt_duplex_3", "belt_duplex_3_right", "artillery_1_body", "artillery_1_barrel",
                "artillery_2_body", "artillery_2_barrel", "artillery_3_body", "artillery_3_barrel", "artillery_4_body",
                "artillery_4_barrel", "artillery_5_body", "artillery_5_barrel", "artillery_6_body", "artillery_6_barrel",
                "artillery_7_body", "artillery_7_barrel", "artillery_7_top", "command_center", "railgun_1_body",
                "railgun_1_barrel", "railgun_2_body", "railgun_2_barrel", "railgun_3_body", "railgun_3_barrel", "railgun_3_top"));
        clips.addAll(pumpClips);
        assertThat(MachineAnimation.ALL.stream().map(MachineAnimation.Clip::id).toList())
                .containsExactlyInAnyOrderElementsOf(clips);
        assertThat(MachineAnimation.FRAMES).isEqualTo(8);
        for (MachineAnimation.Clip clip : MachineAnimation.ALL) {
            MachineArt.Spec spec = MachineArt.ALL.stream().filter(s -> s.id().equals(clip.id())).findFirst().orElseThrow();
            assertThat(MachineAnimation.animates(spec, clip.face())).as(clip.id()).isTrue();
            assertThat(clip.frametime()).as(clip.id()).isBetween(1, 4);
            assertThat(clip.frames()).as(clip.id()).isBetween(4, 8);
            BufferedImage strip = MachineAnimation.strip(spec, clip.face());
            assertThat(strip.getWidth()).as(clip.id()).isEqualTo(MachineAnimation.FRAME);
            assertThat(strip.getHeight()).as(clip.id()).isEqualTo(clip.frames() * strip.getWidth());
            for (int frame = 0; frame < clip.frames(); frame++) {
                int next = (frame + 1) % clip.frames();
                assertThat(digest(frame(strip, frame, MachineAnimation.FRAME)))
                        .as(clip.id() + " frame " + frame)
                        .isNotEqualTo(digest(frame(strip, next, MachineAnimation.FRAME)));
            }
        }
    }

    /**
     * Every lab machine has a level II and a level III model. Only the front and top faces carry the trim, so those differ from
     * level I and the other four sides are the level I sides.
     */
    @Test
    void everyLabMachineHasLevelTwoAndLevelThreeModels() {
        assertThat(LevelArt.LAB_MACHINES).containsExactlyInAnyOrder("crusher", "roaster", "separator", "purifier",
                "converter", "enrichment_cascade", "transmuter", "spectrometer", "particle_accelerator",
                "antimatter_condenser", "research_console", "crude_oil_refinery", "induction_furnace", "powder_mill",
                "cartridge_press", "bullet_swaging_machine", "ammunition_assembly", "metal_lathe", "component_assembly",
                "heavy_weapons_assembly");
        assertThat(LevelArt.LEVELS).containsExactly(2, 3);
        for (String id : LevelArt.LAB_MACHINES) {
            MachineArt.Spec base = LevelArt.base(id);
            for (int level : LevelArt.LEVELS) {
                MachineArt.Spec spec = LevelArt.levelSpec(base, level);
                assertThat(spec.id()).isEqualTo(id + "_" + level);
                for (MachineArt.Face face : MachineArt.Face.values()) {
                    Canvas levelCanvas = new Canvas();
                    MachineArt.paintFace(levelCanvas, spec, face);
                    Canvas baseCanvas = new Canvas();
                    MachineArt.paintFace(baseCanvas, base, face);
                    boolean trimmed = face == MachineArt.Face.NORTH || face == MachineArt.Face.UP;
                    assertThat(levelCanvas.digest() != baseCanvas.digest())
                            .as(spec.id() + " " + face + " differs from level I").isEqualTo(trimmed);
                }
            }
        }
    }

    /** The level II and III front and top faces are all different from one another, so no two levels look the same. */
    @Test
    void theLevelTexturesAreDistinct() {
        Set<Long> digests = new HashSet<>();
        for (String id : LevelArt.LAB_MACHINES) {
            for (int level : LevelArt.LEVELS) {
                MachineArt.Spec spec = LevelArt.levelSpec(LevelArt.base(id), level);
                for (MachineArt.Face face : List.of(MachineArt.Face.NORTH, MachineArt.Face.UP)) {
                    Canvas canvas = new Canvas();
                    MachineArt.paintFace(canvas, spec, face);
                    assertThat(digests.add(canvas.digest())).as(spec.id() + " " + face + " repeats another texture").isTrue();
                }
            }
        }
        assertThat(digests).hasSize(LevelArt.LAB_MACHINES.size() * LevelArt.LEVELS.size() * 2);
    }

    /**
     * The animated faces of the lab machines keep their motion at every level: each level's strip has {@link MachineAnimation#FRAMES}
     * frames that change from one to the next. Eleven lab faces are animated.
     */
    @Test
    void theAnimatedFacesOfLabMachinesStayAnimatedAtEveryLevel() {
        List<String> animated = new ArrayList<>();
        for (String id : LevelArt.LAB_MACHINES) {
            MachineArt.Spec base = LevelArt.base(id);
            for (MachineArt.Face face : MachineArt.Face.values()) {
                boolean moves = MachineAnimation.animates(base, face)
                        || ProcessMachineArt.clip(id).filter(clip -> clip.face() == face).isPresent();
                if (!moves) {
                    continue;
                }
                animated.add(id + " " + face);
                for (int level : LevelArt.LEVELS) {
                    BufferedImage strip = LevelArt.strip(base, face, level);
                    assertThat(strip.getWidth()).as(id + " level " + level).isEqualTo(MachineAnimation.FRAME);
                    assertThat(strip.getHeight()).as(id + " level " + level)
                            .isEqualTo(MachineAnimation.FRAMES * MachineAnimation.FRAME);
                    for (int frame = 0; frame < MachineAnimation.FRAMES; frame++) {
                        int next = (frame + 1) % MachineAnimation.FRAMES;
                        assertThat(digest(frame(strip, frame, MachineAnimation.FRAME)))
                                .as(id + " level " + level + " frame " + frame)
                                .isNotEqualTo(digest(frame(strip, next, MachineAnimation.FRAME)));
                    }
                }
            }
        }
        assertThat(animated).containsExactlyInAnyOrder("crude_oil_refinery UP", "particle_accelerator NORTH",
                "antimatter_condenser NORTH", "induction_furnace NORTH", "powder_mill NORTH", "cartridge_press NORTH",
                "bullet_swaging_machine NORTH", "ammunition_assembly NORTH", "metal_lathe NORTH",
                "component_assembly NORTH", "heavy_weapons_assembly NORTH");
    }

    /**
     * The belt duplex has a model for each half of each level, an inventory sprite for each level, and the frame time of its
     * north face: level I changes frame every two ticks, and levels II and III every tick.
     */
    @Test
    void theBeltDuplexHasTwoHalvesPerLevelAndPacedBelts() {
        assertThat(DuplexArt.ALL.stream().map(MachineArt.Spec::id).toList()).containsExactly(
                "belt_duplex_1", "belt_duplex_1_right", "belt_duplex_2", "belt_duplex_2_right",
                "belt_duplex_3", "belt_duplex_3_right");
        assertThat(DuplexArt.SPRITES.stream().map(ComponentArt.Spec::id).toList()).containsExactly(
                "belt_duplex_1", "belt_duplex_2", "belt_duplex_3");
        assertThat(DuplexArt.CLIPS.stream().map(MachineAnimation.Clip::frametime).toList()).containsExactly(
                2, 2, 1, 1, 1, 1);
    }

    /**
     * Every artillery level is as tall as its height: 1.5, 1.5, 2.5, 2.5, 2.5, 3.0 and 3.75 blocks. Each model is written from y -16 of its
     * frame, and the plugin lifts it one block, and places the upper model of level VII and its barrel three blocks above the base. So the
     * level's bottom is the floor of its base at 0, and its top is the top of its highest model. Level VII's upper model starts where the base
     * ends, at 48 units, and its barrel lies inside that upper part.
     */
    @Test
    void everyArtilleryLevelIsAsTallAsItsHeight() {
        assertThat(ArtilleryArt.ALL.stream().map(MachineArt.Spec::id).toList()).containsExactly(
                "artillery_1_body", "artillery_1_barrel", "artillery_2_body", "artillery_2_barrel", "artillery_3_body",
                "artillery_3_barrel", "artillery_4_body", "artillery_4_barrel", "artillery_5_body", "artillery_5_barrel",
                "artillery_6_body", "artillery_6_barrel", "artillery_7_body", "artillery_7_barrel", "artillery_7_top",
                "command_center");
        assertThat(ArtilleryArt.LEVEL_SEVEN_TOP).isEqualTo("artillery_7_top");
        assertThat(ArtilleryArt.LEVEL_SEVEN_BARREL).isEqualTo("artillery_7_barrel");
        assertThat(ArtilleryArt.displayOffset(ArtilleryArt.LEVEL_SEVEN_TOP)).isEqualTo(3);
        assertThat(ArtilleryArt.displayOffset(ArtilleryArt.LEVEL_SEVEN_BARREL)).isEqualTo(3);
        assertThat(ArtilleryArt.displayOffset("artillery_7_body")).isZero();
        assertThat(ArtilleryArt.displayOffset("artillery_6_barrel")).isZero();
        double[] blocks = {1.5, 1.5, 2.5, 2.5, 2.5, 3.0, 3.75};
        for (int level = 1; level <= 7; level++) {
            double height = blocks[level - 1] * 16;
            double bottom = Double.POSITIVE_INFINITY;
            double top = Double.NEGATIVE_INFINITY;
            for (MachineArt.Spec spec : ArtilleryArt.ALL) {
                if (!spec.id().startsWith("artillery_" + level + "_")) {
                    continue;
                }
                assertThat(ArtilleryArt.modelHeight(spec.id())).as(spec.id()).isEqualTo(height);
                double[] span = span(spec);
                bottom = Math.min(bottom, span[0]);
                top = Math.max(top, span[1]);
                if (spec.id().endsWith("_barrel")) {
                    assertThat(span[0]).as(spec.id() + " starts above the floor").isGreaterThanOrEqualTo(0);
                    assertThat(span[1]).as(spec.id() + " ends inside the level").isLessThanOrEqualTo(height);
                }
            }
            assertThat(bottom).as("level " + level + " bottom").isZero();
            assertThat(top).as("level " + level + " top").isEqualTo(height);
        }
        double[] base = span(machine("artillery_7_body"));
        double[] upper = span(machine("artillery_7_top"));
        assertThat(upper[0]).as("upper model of level VII starts where the base ends").isEqualTo(base[1]);
        assertThat(ArtilleryArt.modelHeight("command_center")).isEqualTo(16);
    }

    /**
     * Each barrel tilts about its trunnion, which lies on the barrel's own cross-section, in the middle of the block on the horizontal axes:
     * the barrel's model is written around the trunnion, so the trunnion is inside the barrel's span. The trunnion heights are the ones the
     * plugin's layout uses.
     */
    @Test
    void eachBarrelTiltsAboutATrunnionInsideItsSpan() {
        double[] trunnions = {1.125, 1.09375, 2.09375, 2.0625, 2.0625, 2.53125, 3.25};
        for (int level = 1; level <= 7; level++) {
            String barrel = "artillery_" + level + "_barrel";
            assertThat(ArtilleryArt.trunnionBlocks(barrel)).as(barrel).isEqualTo(trunnions[level - 1]);
            double[] span = span(machine(barrel));
            assertThat(ArtilleryArt.trunnionBlocks(barrel) * 16).as(barrel).isBetween(span[0], span[1]);
        }
        assertThatThrownBy(() -> ArtilleryArt.trunnionBlocks("artillery_1_body")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArtilleryArt.trunnionBlocks("command_center")).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * The lowest and highest world heights of a model, in model units, when the plugin places its display at the block's origin plus
     * {@link ArtilleryArt#displayOffset} and lifts it by {@link ArtilleryArt#displayLift}.
     */
    private static double[] span(MachineArt.Spec spec) {
        double origin = (ArtilleryArt.displayOffset(spec.id()) + ArtilleryArt.displayLift(spec.id())) * 16;
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (MachineArt.Box box : spec.boxes()) {
            low = Math.min(low, origin + box.y0());
            high = Math.max(high, origin + box.y1());
        }
        return new double[]{low, high};
    }

    /**
     * Every artillery level has textures of its own: each of the six faces differs from the same face of every other level, so no
     * two levels look the same. An animated north face is compared at its first frame. A barrel shares the casing and the front of its
     * level's body, and the upper model of level VII shares the casing of its base, so each level is compared by its body, and the
     * command center by its own.
     */
    @Test
    void everyArtilleryLevelHasItsOwnTextures() {
        List<MachineArt.Spec> compared = ArtilleryArt.ALL.stream()
                .filter(spec -> !spec.id().endsWith("_barrel") && !spec.id().endsWith("_top")).toList();
        assertThat(compared).hasSize(8);
        for (MachineArt.Face face : MachineArt.Face.values()) {
            Set<Long> digests = new HashSet<>();
            for (MachineArt.Spec spec : compared) {
                Canvas canvas = new Canvas();
                MachineArt.paintFace(canvas, spec, face);
                assertThat(digests.add(canvas.digest())).as(spec.id() + " " + face + " repeats another level").isTrue();
            }
            assertThat(digests).hasSize(compared.size());
        }
    }

    /**
     * The screen of the command center is a flat area on the front face of its pedestal: the rectangle {@link ArtilleryArt#SCREEN}
     * lies inside the north face of a cuboid, on that cuboid's plane, so the plugin can place the hologram in it.
     */
    @Test
    void theCommandCenterScreenIsFlatOnThePedestalFrontFace() {
        MachineArt.Spec center = machine("command_center");
        double x0 = ArtilleryArt.SCREEN[0];
        double y0 = ArtilleryArt.SCREEN[1];
        double x1 = ArtilleryArt.SCREEN[2];
        double y1 = ArtilleryArt.SCREEN[3];
        double z = ArtilleryArt.SCREEN[4];
        assertThat(center.boxes()).anyMatch(box -> box.z0() == z && box.x0() <= x0 && box.x1() >= x1
                && box.y0() <= y0 && box.y1() >= y1);
    }

    /**
     * Every shell and charge is drawn: the nine shells and the three tiers of charge are each on a transparent background with visible
     * pixels, and each is a texture of its own. The two animated shells are strips of square frames that change from one frame to the next.
     */
    @Test
    void everyShellAndChargeIsDrawn() {
        assertThat(ShellArt.SPRITES.stream().map(ComponentArt.Spec::id).toList()).containsExactly(
                "explosive_shell", "high_explosive_shell", "white_phosphorus_shell", "napalm_shell", "cluster_shell",
                "enriched_uranium_shell", "hydrogen_thermonuclear_shell", "cluster_enriched_uranium_shell",
                "condensed_antimatter_shell", "tier1_explosive_charge", "tier2_explosive_charge", "tier3_explosive_charge");
        Set<Long> digests = new HashSet<>();
        for (ComponentArt.Spec spec : ShellArt.SPRITES) {
            Canvas canvas = new Canvas();
            ComponentArt.paint(canvas, spec);
            assertThat(Color.alpha(canvas.get(0, 0))).as(spec.id()).isZero();
            int visible = 0;
            for (int y = 0; y < Canvas.SIZE; y++) {
                for (int x = 0; x < Canvas.SIZE; x++) {
                    if (Color.alpha(canvas.get(x, y)) > 0) {
                        visible++;
                    }
                }
            }
            assertThat(visible).as(spec.id()).isPositive();
            assertThat(digests.add(canvas.digest())).as(spec.id() + " repeats another sprite").isTrue();
        }
        assertThat(ShellArt.CLIPS.stream().map(ShellArt.Clip::id).toList())
                .containsExactly("white_phosphorus_shell", "napalm_shell");
        for (ShellArt.Clip clip : ShellArt.CLIPS) {
            ComponentArt.Spec spec = ShellArt.SPRITES.stream().filter(s -> s.id().equals(clip.id())).findFirst().orElseThrow();
            BufferedImage strip = ShellArt.strip(spec);
            assertThat(strip.getWidth()).as(clip.id()).isEqualTo(Canvas.SIZE);
            assertThat(strip.getHeight()).as(clip.id()).isEqualTo(clip.frames() * Canvas.SIZE);
            assertThat(clip.frames()).as(clip.id()).isBetween(4, 8);
            for (int frame = 0; frame < clip.frames(); frame++) {
                int next = (frame + 1) % clip.frames();
                assertThat(digest(frame(strip, frame, Canvas.SIZE))).as(clip.id() + " frame " + frame)
                        .isNotEqualTo(digest(frame(strip, next, Canvas.SIZE)));
            }
        }
    }

    @Test
    void everyTntInTntsYmlHasATexture() throws Exception {
        assertThat(ids("tnts", "tnts.yml")).containsExactlyElementsOf(
                TntArt.ALL.stream().map(TntArt.Spec::id).toList());
    }

    /**
     * Every component the plugin declares has a sprite. items.yml is always checked; materials.yml (the science
     * components and the machines) is checked too once it exists, and then the sprites must match it exactly.
     */
    @Test
    void everyComponentInItemsAndMaterialsYmlHasATexture() throws Exception {
        List<String> art = ComponentArt.ALL.stream().map(ComponentArt.Spec::id).toList();
        List<String> declared = new ArrayList<>(ids("components", "items.yml"));
        assertThat(art).containsAll(declared);
        assertThat(art).contains("ore_density_scanner", "solar_panel");
        if (Files.exists(configPath("materials.yml"))) {
            declared.addAll(ids("components", "materials.yml"));
            assertThat(art)
                    .containsExactlyInAnyOrderElementsOf(declared);
        }
    }

    @Test
    void everyMachineHasAnInventorySpriteAndAModel() {
        List<String> machines = MachineArt.ALL.stream().map(MachineArt.Spec::id).toList();
        List<String> expectedMachines = new ArrayList<>(List.of("crusher", "roaster", "separator", "purifier", "converter",
                "transmuter", "spectrometer", "research_console", "pylon_1", "pylon_2", "pylon_3", "pylon_4", "pylon_5",
                "accumulator_1", "accumulator_2", "accumulator_3", "accumulator_4", "accumulator_5", "petroleum_drill_1", "petroleum_drill_2", "petroleum_drill_3",
                "ore_drill_1", "ore_drill_2", "ore_drill_3", "picker_1", "picker_2", "picker_3", "splitter_1",
                "splitter_2", "splitter_3", "wind_generator", "wind_generator_top", "charcoal_generator",
                "charcoal_generator_top", "petroleum_generator", "petroleum_generator_top", "diesel_generator",
                "diesel_generator_top", "nuclear_generator", "nuclear_generator_top", "solar_panel", "ore_drill_4",
                "ore_drill_4_top", "ore_drill_5", "ore_drill_5_top", "petroleum_drill_4", "petroleum_drill_4_top",
                "petroleum_drill_5", "petroleum_drill_5_top", "crude_oil_refinery", "particle_accelerator",
                "antimatter_condenser", "enrichment_cascade", "machine_gun_turret", "laser_turret",
                "machine_gun_turret_2", "machine_gun_turret_3", "machine_gun_turret_3_top", "machine_gun_turret_4",
                "machine_gun_turret_4_top", "machine_gun_turret_5", "machine_gun_turret_5_top", "laser_turret_2",
                "laser_turret_3", "laser_turret_3_top", "laser_turret_4", "laser_turret_4_top", "laser_turret_5",
                "laser_turret_5_top", "vertical_belt_1",
                "vertical_belt_2", "vertical_belt_3", "belt_duplex_1", "belt_duplex_1_right", "belt_duplex_2",
                "belt_duplex_2_right", "belt_duplex_3", "belt_duplex_3_right"));
        // The water models are one per orientation, and the water items keep their sprites: water_pump, water_pipe,
        // water_splitter_t and water_splitter_x. The models have no sprite of their own.
        expectedMachines.addAll(WaterVariants.ids());
        // The oil tubes are the water pipe models without the pumps, under the oil prefix. They have no sprite of their own either.
        expectedMachines.addAll(OilVariants.ids());
        // The artillery pieces: the body and the barrel of levels I to VII, and the upper model of level VII. Their item sprites are the
        // levels' own, which the component art lists, so the models have no sprite of their own.
        expectedMachines.addAll(List.of("artillery_1_body", "artillery_1_barrel", "artillery_2_body", "artillery_2_barrel",
                "artillery_3_body", "artillery_3_barrel", "artillery_4_body", "artillery_4_barrel", "artillery_5_body",
                "artillery_5_barrel", "artillery_6_body", "artillery_6_barrel", "artillery_7_body", "artillery_7_barrel",
                "artillery_7_top", "command_center"));
        // The railgun turret: the body and the barrel of levels I to III, and the top of level III. Its inventory sprite is the
        // turret's own item sprite, and its parts have their own sprites in the component art.
        expectedMachines.addAll(List.of("railgun_1_body", "railgun_1_barrel", "railgun_2_body", "railgun_2_barrel",
                "railgun_3_body", "railgun_3_barrel", "railgun_3_top"));
        assertThat(machines).containsExactlyInAnyOrderElementsOf(expectedMachines);
        // The upgraded turrets keep the item sprites of machine_gun_turret and laser_turret, so their lower halves have none.
        List<String> upgrades = List.of("machine_gun_turret_2", "machine_gun_turret_3", "machine_gun_turret_4",
                "machine_gun_turret_5", "laser_turret_2", "laser_turret_3", "laser_turret_4", "laser_turret_5");
        // The right cell of a belt duplex is the other half of its anchor's machine, so it has no sprite of its own. The body and the barrel of an
        // artillery piece are parts of its sprite, which is the level's item.
        List<String> sprites = machines.stream()
                .filter(id -> !id.endsWith("_top") && !id.endsWith("_right") && !id.endsWith("_body") && !id.endsWith("_barrel")
                        && !upgrades.contains(id) && !WaterVariants.ids().contains(id) && !OilVariants.ids().contains(id)).toList();
        assertThat(ComponentArt.ALL.stream().map(ComponentArt.Spec::id).toList()).containsAll(sprites)
                .containsAll(List.of("water_pump", "water_pipe", "water_splitter_t", "water_splitter_x",
                        "water_pump_upgrade_2", "water_pump_upgrade_3", "water_pump_upgrade_4", "water_pump_upgrade_5",
                        "oil_pipe", "oil_splitter_t", "oil_splitter_x", "artillery_1", "artillery_2", "artillery_3",
                        "artillery_4", "artillery_5", "artillery_6", "artillery_7"));
        for (MachineArt.Spec spec : MachineArt.ALL) {
            assertThat(spec.boxes()).as(spec.id()).isNotEmpty();
            // Every machine is within one block, except an artillery or railgun model, which is written from y -16 and may reach y 32,
            // and the fourth and fifth pylons, which are taller than a block and reach up to y 28.
            boolean artillery = ArtilleryArt.ALL.stream().anyMatch(model -> model.id().equals(spec.id()))
                    || RailgunArt.ALL.stream().anyMatch(model -> model.id().equals(spec.id()));
            boolean tallPylon = spec.id().equals("pylon_4") || spec.id().equals("pylon_5");
            double low = artillery ? -16.0 : 0.0;
            double high = artillery ? 32.0 : tallPylon ? 28.0 : 16.0;
            for (MachineArt.Box box : spec.boxes()) {
                for (double v : new double[]{box.x0(), box.z0(), box.x1(), box.z1()}) {
                    assertThat(v).as(spec.id()).isBetween(0.0, 16.0);
                }
                for (double v : new double[]{box.y0(), box.y1()}) {
                    assertThat(v).as(spec.id()).isBetween(low, high);
                }
                assertThat(box.x0()).as(spec.id()).isLessThan(box.x1());
                assertThat(box.y0()).as(spec.id()).isLessThan(box.y1());
                assertThat(box.z0()).as(spec.id()).isLessThan(box.z1());
            }
        }
    }

    /** Each pylon is taller than the level below it, so the five levels read as a sequence, from one block to the crowned tower. */
    @Test
    void eachPylonLevelIsTallerThanTheLast() {
        double previous = 0;
        for (int level = 1; level <= 5; level++) {
            double top = machine("pylon_" + level).boxes().stream().mapToDouble(MachineArt.Box::y1).max().orElseThrow();
            assertThat(top).as("pylon_" + level).isGreaterThan(previous);
            previous = top;
        }
    }

    @Test
    void labelsFitThePanel() {
        for (TntArt.Spec spec : TntArt.ALL) {
            assertThat(spec.label().length()).as(spec.id()).isLessThanOrEqualTo(5);
            assertThat(Font.width(spec.label())).as(spec.id()).isLessThanOrEqualTo(22);
        }
    }

    @Test
    void theThreeDigitColourFormIsRejected() {
        assertThatThrownBy(() -> Color.hex("#FFF")).isInstanceOf(IllegalArgumentException.class);
    }

    private static MachineArt.Spec machine(String id) {
        return MachineArt.ALL.stream().filter(spec -> spec.id().equals(id)).findFirst().orElseThrow();
    }

    private static BufferedImage frame(BufferedImage strip, int frame) {
        return frame(strip, frame, BeltArt.TILE);
    }

    private static BufferedImage frame(BufferedImage strip, int frame, int size) {
        return strip.getSubimage(0, frame * size, size, size);
    }

    private static int countArrowPixels(BufferedImage strip, int frame, BeltArt.Spec spec) {
        int arrow = Color.hex(spec.accent());
        BufferedImage image = frame(strip, frame);
        int count = 0;
        for (int y = 0; y < BeltArt.TILE; y++) {
            for (int x = 0; x < BeltArt.TILE; x++) {
                if (image.getRGB(x, y) == arrow) {
                    count++;
                }
            }
        }
        return count;
    }

    private static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    private static long digest(BufferedImage image) {
        long h = 1125899906842597L;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                h = 31 * h + image.getRGB(x, y);
            }
        }
        return h;
    }

    private static Path configPath(String file) {
        return Path.of("..", "CoreFactory-Paper", "src", "main", "resources", file);
    }

    private static List<String> ids(String section, String file) throws Exception {
        return entries(section, file).keySet().stream().collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entries(String section, String file) throws Exception {
        Path path = configPath(file);
        try (InputStream in = Files.newInputStream(path)) {
            Map<String, Object> root = new Yaml().load(in);
            return (Map<String, Object>) root.get(section);
        }
    }
}
