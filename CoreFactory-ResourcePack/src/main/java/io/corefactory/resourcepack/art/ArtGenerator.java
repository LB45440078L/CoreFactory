package io.corefactory.resourcepack.art;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Paints every TNT and component texture and writes the files the resource pack builder reads:
 *
 * <pre>
 * OUT/textures/block/tnt/ID.png          TNT block textures (32 x 32)
 * OUT/textures/item/component/ID.png     component item sprites (32 x 32)
 * OUT/specs/tnt/ID.yml                   one descriptor per TNT
 * OUT/specs/component/ID.yml             one descriptor per component
 * OUT/textures/block/machine/ID/FACE.png  six face textures per machine (block atlas) (north, south, east, west, up, down);
 *                                        an animated face is a 16 x 128 strip of 8 frames
 * OUT/specs/machine/ID.yml               one descriptor per machine: its face textures, animation and cuboids
 * OUT/textures/block/ore/ID.png          ore block textures (16 x 16)
 * OUT/specs/ore/ID.yml                   one descriptor per ore: its vanilla host block and colours
 * OUT/textures/item/particle/ID.png      effect sprites (32 x 32)
 * OUT/specs/particle/ID.yml              one descriptor per effect sprite
 * OUT/textures/block/belt/ID/top_straight.png  animated belt top, a 16 x 128 strip of 8 frames
 * OUT/textures/block/belt/ID/top_corner.png    animated corner top, a 16 x 128 strip of 8 frames
 * OUT/textures/block/belt/ID/side.png          belt side (16 x 16)
 * OUT/specs/belt/ID.yml                  one descriptor per belt level: its tops, side and animation
 * OUT/contact-sheet.png                  every texture on one sheet, for review only
 * </pre>
 *
 * <p>Run it after changing a painter: {@code java -cp target/classes io.corefactory.resourcepack.art.ArtGenerator
 * art}. The output is deterministic, so running it twice gives identical files.
 */
public final class ArtGenerator {

    private static final int SHEET_COLUMNS = 10;
    private static final int SHEET_SCALE = 3;
    private static final int SHEET_GAP = 6;

    private ArtGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "art");
        clear(out.resolve("textures"));
        clear(out.resolve("specs"));

        List<BufferedImage> tiles = new ArrayList<>();
        for (TntArt.Spec spec : TntArt.ALL) {
            Canvas canvas = new Canvas();
            TntArt.paint(canvas, spec);
            tiles.add(canvas.toImage());
            writePng(canvas.toImage(), out.resolve("textures/block/tnt/" + spec.id() + ".png"));
            writeText(out.resolve("specs/tnt/" + spec.id() + ".yml"), tntDescriptor(spec));
        }
        for (ComponentArt.Spec spec : ComponentArt.ALL) {
            Canvas canvas = new Canvas();
            ComponentArt.paint(canvas, spec);
            tiles.add(canvas.toImage());
            writePng(canvas.toImage(), out.resolve("textures/item/component/" + spec.id() + ".png"));
            writeText(out.resolve("specs/component/" + spec.id() + ".yml"), componentDescriptor(spec));
        }
        for (MachineArt.Spec spec : MachineArt.ALL) {
            for (MachineArt.Face face : MachineArt.Face.values()) {
                Canvas canvas = new Canvas();
                MachineArt.paintFace(canvas, spec, face);
                tiles.add(canvas.toImage());
                BufferedImage image = MachineAnimation.animates(spec, face)
                        ? MachineAnimation.strip(spec, face) : canvas.toImage();
                writePng(image, out.resolve(machineTexture(spec, face)));
            }
            writeText(out.resolve("specs/machine/" + spec.id() + ".yml"), machineDescriptor(spec));
        }
        for (OreArt.Spec spec : OreArt.ALL) {
            Canvas canvas = new Canvas();
            OreArt.paint(canvas, spec);
            BufferedImage block = OreArt.block(canvas);
            tiles.add(block);
            writePng(block, out.resolve("textures/block/ore/" + spec.id() + ".png"));
            writeText(out.resolve("specs/ore/" + spec.id() + ".yml"), oreDescriptor(spec));
        }
        for (ParticleArt.Spec spec : ParticleArt.ALL) {
            Canvas canvas = new Canvas();
            ParticleArt.paint(canvas, spec);
            tiles.add(canvas.toImage());
            writePng(canvas.toImage(), out.resolve("textures/item/particle/" + spec.id() + ".png"));
            writeText(out.resolve("specs/particle/" + spec.id() + ".yml"), particleDescriptor(spec));
        }
        for (BeltArt.Spec spec : BeltArt.ALL) {
            String dir = "textures/block/belt/" + spec.id() + "/";
            BufferedImage straight = BeltArt.strip(spec, false);
            writePng(straight, out.resolve(dir + "top_straight.png"));
            writePng(BeltArt.strip(spec, true), out.resolve(dir + "top_corner.png"));
            writePng(BeltArt.side(spec), out.resolve(dir + "side.png"));
            tiles.add(straight.getSubimage(0, 0, BeltArt.TILE, BeltArt.TILE));
            tiles.add(BeltArt.side(spec));
            writeText(out.resolve("specs/belt/" + spec.id() + ".yml"), beltDescriptor(spec));
        }
        ProcessMachineArt.write(out, tiles);
        LevelArt.write(out, tiles);
        Canvas background = new Canvas();
        BackgroundArt.paint(background);
        writePng(background.toImage(), out.resolve(BackgroundArt.PATH));
        writePng(contactSheet(tiles), out.resolve("contact-sheet.png"));
        System.out.println("Wrote " + tiles.size() + " textures to " + out.toAbsolutePath());
    }

    /** The art-relative path of one machine face, such as {@code textures/block/machine/crusher/north.png}. */
    private static String machineTexture(MachineArt.Spec spec, MachineArt.Face face) {
        return "textures/block/machine/" + spec.id() + "/" + face.key() + ".png";
    }

    /**
     * A machine descriptor. The model kind {@code machine} has the six face textures and the cuboids of the silhouette;
     * {@code ResourcePackBuilder} turns them into the item model JSON. A machine with an animated face names it in its
     * {@code animation}, with the frame count and the frame time of that face's strip.
     */
    private static String machineDescriptor(MachineArt.Spec spec) {
        StringBuilder text = new StringBuilder(header());
        text.append("id: ").append(spec.id()).append("\n");
        text.append("kind: machine\n");
        text.append("model: machine\n");
        text.append("palette:\n");
        text.append("  base: \"").append(spec.base()).append("\"\n");
        text.append("  accent: \"").append(spec.accent()).append("\"\n");
        text.append("textures:\n");
        for (MachineArt.Face face : MachineArt.Face.values()) {
            text.append("  ").append(face.key()).append(": ").append(machineTexture(spec, face)).append("\n");
        }
        MachineAnimation.clip(spec.id()).ifPresent(clip -> text.append("animation:\n")
                .append("  frames: ").append(MachineAnimation.FRAMES).append("\n")
                .append("  frametime: ").append(clip.frametime()).append("\n")
                .append("  textures: [").append(clip.face().key()).append("]\n"));
        text.append("elements:\n");
        for (MachineArt.Box box : spec.boxes()) {
            text.append("  - from: [").append(num(box.x0())).append(", ").append(num(box.y0())).append(", ")
                    .append(num(box.z0())).append("]\n");
            text.append("    to: [").append(num(box.x1())).append(", ").append(num(box.y1())).append(", ")
                    .append(num(box.z1())).append("]\n");
        }
        return text.toString();
    }

    /** A coordinate as YAML writes it: {@code 4} for a whole number, {@code 9.5} otherwise. */
    private static String num(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    private static String tntDescriptor(TntArt.Spec spec) {
        return header()
                + "id: " + spec.id() + "\n"
                + "kind: tnt\n"
                + "model: cube\n"
                + "texture: textures/block/tnt/" + spec.id() + ".png\n"
                + "label: \"" + spec.label() + "\"\n"
                + "motif: " + spec.icon() + "\n"
                + "palette:\n"
                + "  base: \"" + spec.base() + "\"\n"
                + "  accent: \"" + spec.accent() + "\"\n"
                + "band: " + spec.band() + "\n"
                + "panel: " + spec.panel() + "\n";
    }

    /**
     * An ore descriptor. The model kind {@code block} is a full block: the texture on all six faces, and the vanilla
     * {@code host} block whose blockstate the pack overrides so that the placed ore shows this texture.
     */
    private static String oreDescriptor(OreArt.Spec spec) {
        return header()
                + "id: " + spec.id() + "\n"
                + "kind: ore\n"
                + "model: block\n"
                + "host: " + spec.host() + "\n"
                + "texture: textures/block/ore/" + spec.id() + ".png\n"
                + "palette:\n"
                + "  base: \"" + spec.base() + "\"\n"
                + "  accent: \"" + spec.accent() + "\"\n";
    }

    /**
     * A belt descriptor. The model kind {@code belt} is a flat slab of 16 x {@link BeltArt#SLAB_HEIGHT} x 16: the straight
     * and the corner top share the side, and the animation names the tops that scroll.
     */
    private static String beltDescriptor(BeltArt.Spec spec) {
        String dir = "textures/block/belt/" + spec.id() + "/";
        return header()
                + "id: " + spec.id() + "\n"
                + "kind: belt\n"
                + "model: belt\n"
                + "palette:\n"
                + "  base: \"" + spec.base() + "\"\n"
                + "  accent: \"" + spec.accent() + "\"\n"
                + "textures:\n"
                + "  top_straight: " + dir + "top_straight.png\n"
                + "  top_corner: " + dir + "top_corner.png\n"
                + "  side: " + dir + "side.png\n"
                + "animation:\n"
                + "  frames: " + BeltArt.FRAMES + "\n"
                + "  frametime: " + BeltArt.FRAME_TIME + "\n"
                + "  textures: [top_straight, top_corner]\n"
                + "elements:\n"
                + "  - from: [0, 0, 0]\n"
                + "    to: [16, " + BeltArt.SLAB_HEIGHT + ", 16]\n";
    }

    private static String particleDescriptor(ParticleArt.Spec spec) {
        return header()
                + "id: " + spec.id() + "\n"
                + "kind: particle\n"
                + "model: sprite\n"
                + "texture: textures/item/particle/" + spec.id() + ".png\n";
    }

    private static String componentDescriptor(ComponentArt.Spec spec) {
        return header()
                + "id: " + spec.id() + "\n"
                + "kind: component\n"
                + "model: sprite\n"
                + "texture: textures/item/component/" + spec.id() + ".png\n"
                + "palette:\n"
                + "  base: \"" + spec.base() + "\"\n"
                + "  accent: \"" + spec.accent() + "\"\n";
    }

    private static String header() {
        return "# Generated by io.corefactory.resourcepack.art.ArtGenerator. Change the painter, then run the generator.\n";
    }

    /** A grid of every texture, scaled up, on a dark background. */
    private static BufferedImage contactSheet(List<BufferedImage> tiles) {
        int cell = Canvas.SIZE * SHEET_SCALE + SHEET_GAP;
        int rows = (tiles.size() + SHEET_COLUMNS - 1) / SHEET_COLUMNS;
        BufferedImage sheet = new BufferedImage(SHEET_COLUMNS * cell + SHEET_GAP, rows * cell + SHEET_GAP,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new java.awt.Color(0x1E, 0x20, 0x26));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int i = 0; i < tiles.size(); i++) {
            int x = SHEET_GAP + (i % SHEET_COLUMNS) * cell;
            int y = SHEET_GAP + (i / SHEET_COLUMNS) * cell;
            g.drawImage(tiles.get(i), x, y, Canvas.SIZE * SHEET_SCALE, Canvas.SIZE * SHEET_SCALE, null);
        }
        g.dispose();
        return sheet;
    }

    private static void clear(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static void writePng(BufferedImage image, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
    }

    private static void writeText(Path file, String text) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
