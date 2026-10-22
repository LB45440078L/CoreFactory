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
        assertThat(MachineAnimation.ALL.stream().map(MachineAnimation.Clip::id).toList()).containsExactlyInAnyOrder(
                "pylon_1", "pylon_2", "pylon_3", "accumulator_1", "accumulator_2", "accumulator_3",
                "wind_generator_top", "charcoal_generator_top", "petroleum_generator_top", "diesel_generator_top",
                "nuclear_generator_top", "ore_drill_4_top", "ore_drill_5_top", "petroleum_drill_4_top",
                "petroleum_drill_5_top", "solar_panel", "crude_oil_refinery", "particle_accelerator",
                "antimatter_condenser", "machine_gun_turret", "laser_turret", "machine_gun_turret_2",
                "machine_gun_turret_3", "machine_gun_turret_4", "machine_gun_turret_5", "laser_turret_2",
                "laser_turret_3_top", "laser_turret_4_top", "laser_turret_5_top", "water_pump", "water_pump_2",
                "water_pump_3", "water_pump_4", "water_pump_5", "vertical_belt_1", "vertical_belt_2", "vertical_belt_3");
        assertThat(MachineAnimation.FRAMES).isEqualTo(8);
        for (MachineAnimation.Clip clip : MachineAnimation.ALL) {
            MachineArt.Spec spec = MachineArt.ALL.stream().filter(s -> s.id().equals(clip.id())).findFirst().orElseThrow();
            assertThat(MachineAnimation.animates(spec, clip.face())).as(clip.id()).isTrue();
            assertThat(clip.frametime()).as(clip.id()).isBetween(1, 4);
            BufferedImage strip = MachineAnimation.strip(spec, clip.face());
            assertThat(strip.getWidth()).as(clip.id()).isEqualTo(MachineAnimation.FRAME);
            assertThat(strip.getHeight()).as(clip.id()).isEqualTo(MachineAnimation.FRAMES * strip.getWidth());
            for (int frame = 0; frame < MachineAnimation.FRAMES; frame++) {
                int next = (frame + 1) % MachineAnimation.FRAMES;
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
        assertThat(art).contains("ore_density_scanner", "solar_panel", "petroleum");
        if (Files.exists(configPath("materials.yml"))) {
            declared.addAll(ids("components", "materials.yml"));
            assertThat(art).containsExactlyInAnyOrderElementsOf(declared);
        }
    }

    @Test
    void everyMachineHasAnInventorySpriteAndAModel() {
        List<String> machines = MachineArt.ALL.stream().map(MachineArt.Spec::id).toList();
        assertThat(machines).containsExactlyInAnyOrder("crusher", "roaster", "separator", "purifier", "converter",
                "transmuter", "spectrometer", "research_console", "pylon_1", "pylon_2", "pylon_3", "accumulator_1",
                "accumulator_2", "accumulator_3", "petroleum_drill_1", "petroleum_drill_2", "petroleum_drill_3",
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
                "laser_turret_5_top", "water_pump", "water_pump_2", "water_pump_3", "water_pump_4", "water_pump_5",
                "water_pipe", "water_pipe_bend", "water_splitter_t", "water_splitter_x", "vertical_belt_1",
                "vertical_belt_2", "vertical_belt_3");
        // The upgraded turrets keep the item sprites of machine_gun_turret and laser_turret, so their lower halves have none.
        // The upgraded pumps keep the sprite of water_pump, and the pipe bend that of water_pipe.
        List<String> upgrades = List.of("machine_gun_turret_2", "machine_gun_turret_3", "machine_gun_turret_4",
                "machine_gun_turret_5", "laser_turret_2", "laser_turret_3", "laser_turret_4", "laser_turret_5",
                "water_pump_2", "water_pump_3", "water_pump_4", "water_pump_5", "water_pipe_bend");
        List<String> sprites = machines.stream().filter(id -> !id.endsWith("_top") && !upgrades.contains(id)).toList();
        assertThat(ComponentArt.ALL.stream().map(ComponentArt.Spec::id).toList()).containsAll(sprites);
        for (MachineArt.Spec spec : MachineArt.ALL) {
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
        return Path.of("..", "CoreFactory-Spigot", "src", "main", "resources", file);
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
