package io.corefactory.resourcepack;

import io.corefactory.api.PackVersion;
import org.yaml.snakeyaml.Yaml;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Packs the generated art into the CoreFactory resource pack zip. The art comes from {@code art/specs} and the PNGs next
 * to it, written by {@code ArtGenerator}. For each descriptor this writes the texture, an item model and an item
 * definition, plus {@code pack.mcmeta} and {@code pack.png}. An ore descriptor also writes a block model, and overrides
 * the blockstate of its vanilla host block so that the host shows the ore.
 *
 * <p>Arguments: {@code art-dir} {@code output.zip} {@code pack-format} {@code description} {@code version} {@code build}.
 * The version and build are written to {@code corefactory-pack.properties}, which the plugin reads to decide whether
 * an installed pack is outdated.
 */
public final class ResourcePackBuilder {

    private static final String NAMESPACE = "corefactory";

    /** The sides of a machine model, in the order they are written. */
    private static final List<String> FACES = List.of("north", "south", "east", "west", "up", "down");

    /** The display transforms of a machine item: a small view in the GUI and full size when placed. */
    private static final Map<String, Object> DISPLAY = ordered(
            "gui", ordered("rotation", List.of(30, 225, 0), "translation", List.of(0, 0, 0),
                    "scale", List.of(0.625, 0.625, 0.625)),
            "fixed", ordered("rotation", List.of(0, 0, 0), "translation", List.of(0, 0, 0), "scale", List.of(1, 1, 1)));

    private ResourcePackBuilder() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 6) {
            System.err.println("usage: ResourcePackBuilder <art-dir> <output.zip> <pack-format> <description> <version> <build>");
            System.exit(2);
        }
        build(Path.of(args[0]), Path.of(args[1]), Integer.parseInt(args[2]), args[3], args[4], Long.parseLong(args[5]));
    }

    /** Writes the zip and returns the number of items it contains. */
    public static int build(Path art, Path output, int packFormat, String description, String version, long build)
            throws IOException {
        List<Map<String, Object>> specs = loadSpecs(art.resolve("specs"));
        if (specs.isEmpty()) {
            throw new IllegalStateException("No texture descriptors under " + art.resolve("specs")
                    + ". Run io.corefactory.resourcepack.art.ArtGenerator first.");
        }
        checkHosts(specs);
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            writeText(zip, "pack.mcmeta", "{\n  \"pack\": {\n    \"pack_format\": " + packFormat
                    + ",\n    \"description\": \"" + description + "\"\n  }\n}\n");
            writePng(zip, "pack.png", icon(art, specs));
            writeText(zip, PackVersion.ENTRY, PackVersion.properties(version, build, packFormat));
            for (Map<String, Object> spec : specs) {
                addItem(zip, art, spec);
            }
            addGuiTextures(zip, art.resolve("textures").resolve("gui"));
            SoundPack.add(zip, art);
        }
        System.out.println("Wrote " + specs.size() + " items to " + output.toAbsolutePath());
        return specs.size();
    }

    private static void addItem(ZipOutputStream zip, Path art, Map<String, Object> spec) throws IOException {
        String id = text(spec, "id");
        String kind = text(spec, "kind");
        String model = text(spec, "model");
        if ("machine".equals(model)) {
            addMachine(zip, art, spec);
            return;
        }
        if ("block".equals(model)) {
            addOre(zip, art, spec);
            return;
        }
        if ("belt".equals(model)) {
            addBelt(zip, art, spec);
            return;
        }
        String texturePath = copyTexture(zip, art, id, text(spec, "texture"));
        String reference = kind + "/" + id;

        String modelId = NAMESPACE + ":item/" + reference;
        if ("cube".equals(model)) {
            // The "fixed" context is what a placed TNT's item display uses. The base block model shrinks it to half size,
            // which left the vanilla block visible around the custom one, so the full size is set here explicitly.
            writeText(zip, "assets/" + NAMESPACE + "/models/item/" + reference + ".json",
                    "{\n  \"parent\": \"minecraft:block/cube_all\",\n  \"textures\": {\n    \"all\": \""
                            + NAMESPACE + ":" + texturePath + "\"\n  },\n  \"display\": {\n"
                            + "    \"fixed\": {\"rotation\": [0, 0, 0], \"translation\": [0, 0, 0], \"scale\": [1, 1, 1]},\n"
                            + "    \"gui\": {\"rotation\": [30, 225, 0], \"translation\": [0, 0, 0], \"scale\": [0.625, 0.625, 0.625]}\n"
                            + "  }\n}\n");
        } else if ("sprite".equals(model)) {
            writeText(zip, "assets/" + NAMESPACE + "/models/item/" + reference + ".json",
                    "{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": {\n    \"layer0\": \""
                            + NAMESPACE + ":" + texturePath + "\"\n  }\n}\n");
        } else {
            throw new IllegalStateException("Unknown model '" + model + "' for " + id);
        }
        writeText(zip, "assets/" + NAMESPACE + "/items/" + reference + ".json",
                "{\n  \"model\": {\n    \"type\": \"minecraft:model\",\n    \"model\": \"" + modelId + "\"\n  }\n}\n");
    }

    /**
     * An ore block, kind {@code ore}. The texture is a 16 x 16 block texture, shown on all six faces of a block model. The
     * item definition gives the ore's inventory icon, from that model. The blockstate of the vanilla host block is
     * replaced, so every facing of the host shows the ore: a blockstate override lists every variant the block has, so
     * each facing is given the same model and no rotation.
     */
    private static void addOre(ZipOutputStream zip, Path art, Map<String, Object> spec) throws IOException {
        String id = text(spec, "id");
        String texturePath = copyTexture(zip, art, id, text(spec, "texture"));
        String modelId = NAMESPACE + ":" + texturePath;
        writeText(zip, "assets/" + NAMESPACE + "/models/block/ore/" + id + ".json", json(ordered(
                "parent", "minecraft:block/cube_all",
                "textures", ordered("all", modelId))));
        writeText(zip, "assets/" + NAMESPACE + "/items/ore/" + id + ".json", json(ordered(
                "model", ordered("type", "minecraft:model", "model", modelId))));
        Map<String, Object> variants = new LinkedHashMap<>();
        for (String facing : List.of("north", "south", "east", "west")) {
            variants.put("facing=" + facing, ordered("model", modelId));
        }
        String host = text(spec, "host").toLowerCase(Locale.ROOT);
        writeText(zip, "assets/minecraft/blockstates/" + host + ".json", json(ordered("variants", variants)));
    }

    /** Writes the PNG of an item, block or machine into the zip, and returns its path without the folder and extension. */
    private static String copyTexture(ZipOutputStream zip, Path art, String id, String texture) throws IOException {
        Path png = art.resolve(texture);
        if (!Files.isRegularFile(png)) {
            throw new IllegalStateException("Texture for " + id + " is missing: " + png);
        }
        zip.putNextEntry(new ZipEntry("assets/" + NAMESPACE + "/" + texture));
        zip.write(Files.readAllBytes(png));
        zip.closeEntry();
        return texture.replaceFirst("^textures/", "").replaceFirst("\\.png$", "");
    }

    /** Fails if two ore descriptors name the same host block, since a host has only one blockstate. */
    private static void checkHosts(List<Map<String, Object>> specs) {
        Set<String> hosts = new HashSet<>();
        for (Map<String, Object> spec : specs) {
            if ("ore".equals(spec.get("kind")) && !hosts.add(text(spec, "host"))) {
                throw new IllegalStateException("Two ores share the host block " + spec.get("host"));
            }
        }
    }

    /**
     * A placed machine's 3D item model, kind {@code machine}. The descriptor gives six face textures and the cuboids of
     * the silhouette. Each cuboid becomes one element with all six faces, each referring to its side's texture. A face named
     * in the {@code animation} is a strip of square frames, and gets a {@code .mcmeta} beside its PNG with the frame time.
     * The model is written as {@code models/item/machine/ID.json}, with the item definition beside it.
     */
    @SuppressWarnings("unchecked")
    private static void addMachine(ZipOutputStream zip, Path art, Map<String, Object> spec) throws IOException {
        String id = text(spec, "id");
        Map<String, Object> faces = map(spec, "textures");
        Map<String, Object> textures = new LinkedHashMap<>();
        int frames = 0;
        int frametime = 0;
        List<String> animated = List.of();
        if (spec.get("animation") != null) {
            Map<String, Object> animation = map(spec, "animation");
            frames = number(animation, "frames", id);
            frametime = number(animation, "frametime", id);
            animated = list(animation, "textures").stream().map(Object::toString).toList();
            for (String side : animated) {
                if (!faces.containsKey(side)) {
                    throw new IllegalStateException("Machine " + id + " animates " + side + ", which is not one of its faces");
                }
            }
        }
        for (String side : FACES) {
            if (!faces.containsKey(side)) {
                throw new IllegalStateException("Machine " + id + " has no " + side + " texture");
            }
            String path = text(faces, side);
            Path png = art.resolve(path);
            if (!Files.isRegularFile(png)) {
                throw new IllegalStateException("Texture for machine " + id + " is missing: " + png);
            }
            zip.putNextEntry(new ZipEntry("assets/" + NAMESPACE + "/" + path));
            zip.write(Files.readAllBytes(png));
            zip.closeEntry();
            if (animated.contains(side)) {
                checkStrip(png, id, frames);
                writeText(zip, "assets/" + NAMESPACE + "/" + path + ".mcmeta",
                        json(ordered("animation", ordered("frametime", frametime))));
            }
            textures.put(side, NAMESPACE + ":" + path.replaceFirst("^textures/", "").replaceFirst("\\.png$", ""));
        }
        List<Object> elements = new ArrayList<>();
        for (Object item : list(spec, "elements")) {
            Map<String, Object> box = (Map<String, Object>) item;
            Map<String, Object> sides = new LinkedHashMap<>();
            for (String side : FACES) {
                sides.put(side, ordered("texture", "#" + side));
            }
            elements.add(ordered("from", box.get("from"), "to", box.get("to"), "faces", sides));
        }
        if (elements.isEmpty()) {
            throw new IllegalStateException("Machine " + id + " has no elements");
        }
        Map<String, Object> model = ordered("textures", textures, "elements", elements, "display", DISPLAY);
        String reference = "machine/" + id;
        writeText(zip, "assets/" + NAMESPACE + "/models/item/" + reference + ".json", json(model));
        writeText(zip, "assets/" + NAMESPACE + "/items/" + reference + ".json",
                "{\n  \"model\": {\n    \"type\": \"minecraft:model\",\n    \"model\": \"" + NAMESPACE + ":item/"
                        + reference + "\"\n  }\n}\n");
    }

    /**
     * A transport belt level, kind {@code belt}: a flat slab with a straight and a corner top. The tops named in the
     * descriptor's {@code animation} are strips of square frames, and each gets a {@code .mcmeta} beside its PNG. The
     * models are written as {@code models/item/belt/ID/straight.json}, {@code corner.json} and {@code corner_right.json}.
     * The top face is mapped with its v axis flipped, so the bottom edge of the texture is the belt's front, the +Z side
     * of the model, and the arrows, which point toward that bottom edge, follow the flow. The item definition at
     * {@code items/belt/ID.json} shows the straight model.
     *
     * <p>A corner's arc joins the middle of the texture's right edge, where its items come in, to the middle of its bottom
     * edge, where they leave. The left corner's top maps that right edge to the model's +X side, the input of a left
     * corner, and {@code corner_right.json} maps it to -X, the input of a right corner. So the right corner's top is the
     * left corner's top mirrored by its u axis. The display never mirrors a model with a negative scale: a mirrored
     * display turns its faces inside out and renders them dark, so the mirror is in the texture mapping instead.
     */
    @SuppressWarnings("unchecked")
    private static void addBelt(ZipOutputStream zip, Path art, Map<String, Object> spec) throws IOException {
        String id = text(spec, "id");
        Map<String, Object> textures = map(spec, "textures");
        Map<String, Object> animation = map(spec, "animation");
        int frames = number(animation, "frames", id);
        int frametime = number(animation, "frametime", id);
        List<String> animated = list(animation, "textures").stream().map(Object::toString).toList();
        Map<String, String> references = new LinkedHashMap<>();
        for (String key : textures.keySet()) {
            String path = text(textures, key);
            references.put(key, copyTexture(zip, art, id, path));
            if (animated.contains(key)) {
                checkStrip(art.resolve(path), id, frames);
                writeText(zip, "assets/" + NAMESPACE + "/" + path + ".mcmeta",
                        json(ordered("animation", ordered("frametime", frametime))));
            }
        }
        List<Object> topUv = List.of(0, 16, 16, 0);
        List<Object> mirroredTopUv = List.of(16, 16, 0, 0);
        for (String shape : List.of("straight", "corner", "corner_right")) {
            String top = shape.startsWith("corner") ? "top_corner" : "top_straight";
            Map<String, Object> modelTextures = ordered(
                    "top", NAMESPACE + ":" + references.get(top),
                    "side", NAMESPACE + ":" + references.get("side"));
            List<Object> faceUv = shape.equals("corner") ? mirroredTopUv : topUv;
            List<Object> elements = new ArrayList<>();
            for (Object item : list(spec, "elements")) {
                Map<String, Object> box = (Map<String, Object>) item;
                Map<String, Object> sides = new LinkedHashMap<>();
                for (String side : FACES) {
                    sides.put(side, side.equals("up")
                            ? ordered("texture", "#top", "uv", faceUv)
                            : ordered("texture", "#side"));
                }
                elements.add(ordered("from", box.get("from"), "to", box.get("to"), "faces", sides));
            }
            writeText(zip, "assets/" + NAMESPACE + "/models/item/belt/" + id + "/" + shape + ".json",
                    json(ordered("textures", modelTextures, "elements", elements, "display", DISPLAY)));
        }
        writeText(zip, "assets/" + NAMESPACE + "/items/belt/" + id + ".json", json(ordered(
                "model", ordered("type", "minecraft:model", "model", NAMESPACE + ":item/belt/" + id + "/straight"))));
        writeText(zip, "assets/" + NAMESPACE + "/items/belt/" + id + "/corner.json", json(ordered(
                "model", ordered("type", "minecraft:model", "model", NAMESPACE + ":item/belt/" + id + "/corner"))));
        writeText(zip, "assets/" + NAMESPACE + "/items/belt/" + id + "/corner_right.json", json(ordered(
                "model", ordered("type", "minecraft:model", "model", NAMESPACE + ":item/belt/" + id + "/corner_right"))));
    }

    /** Fails unless an animated texture is {@code frames} square frames stacked top to bottom. */
    private static void checkStrip(Path png, String id, int frames) throws IOException {
        if (!Files.isRegularFile(png)) {
            throw new IllegalStateException("Animated texture for " + id + " is missing: " + png);
        }
        BufferedImage image = ImageIO.read(png.toFile());
        if (image == null || image.getHeight() != image.getWidth() * frames) {
            throw new IllegalStateException("Animated texture for " + id + " must be " + frames
                    + " square frames stacked vertically: " + png);
        }
    }

    /** Packs every texture under {@code art/textures/gui}, such as the advancement background, at its own path. */
    private static void addGuiTextures(ZipOutputStream zip, Path gui) throws IOException {
        if (!Files.isDirectory(gui)) {
            return;
        }
        List<Path> files;
        try (Stream<Path> paths = Files.walk(gui)) {
            files = paths.filter(Files::isRegularFile).sorted().toList();
        }
        for (Path file : files) {
            String relative = gui.getParent().relativize(file).toString().replace('\\', '/');
            zip.putNextEntry(new ZipEntry("assets/" + NAMESPACE + "/textures/" + relative));
            zip.write(Files.readAllBytes(file));
            zip.closeEntry();
        }
    }

    /** A 128 x 128 pack icon: the first TNT texture, scaled up four times. */
    private static BufferedImage icon(Path art, List<Map<String, Object>> specs) throws IOException {
        Map<String, Object> first = specs.stream()
                .filter(spec -> "tnt".equals(spec.get("kind")))
                .findFirst()
                .orElse(specs.get(0));
        BufferedImage source = ImageIO.read(art.resolve(text(first, "texture")).toFile());
        BufferedImage large = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = large.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source, 0, 0, 128, 128, null);
        g.dispose();
        return large;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> loadSpecs(Path specDir) throws IOException {
        List<Map<String, Object>> specs = new ArrayList<>();
        if (!Files.isDirectory(specDir)) {
            return specs;
        }
        List<Path> files;
        try (Stream<Path> paths = Files.walk(specDir)) {
            files = paths.filter(p -> p.toString().endsWith(".yml")).sorted(Comparator.naturalOrder()).toList();
        }
        Yaml yaml = new Yaml();
        for (Path file : files) {
            try (InputStream in = Files.newInputStream(file)) {
                Object root = yaml.load(in);
                if (root instanceof Map<?, ?> map) {
                    specs.add((Map<String, Object>) map);
                }
            }
        }
        return specs;
    }

    private static String text(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalStateException("A texture descriptor has no '" + key + "': " + map);
        }
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map<String, Object> spec, String key) {
        if (!(spec.get(key) instanceof Map<?, ?> value)) {
            throw new IllegalStateException("A descriptor has no '" + key + "' section: " + spec);
        }
        return (Map<String, Object>) value;
    }

    private static List<?> list(Map<String, Object> spec, String key) {
        if (!(spec.get(key) instanceof List<?> value)) {
            throw new IllegalStateException("A descriptor has no '" + key + "' list: " + spec);
        }
        return value;
    }

    private static int number(Map<String, Object> spec, String key, String id) {
        if (!(spec.get(key) instanceof Number value)) {
            throw new IllegalStateException("Descriptor " + id + " has no '" + key + "' number");
        }
        return value.intValue();
    }

    /** A map with its entries in the order given, as key, value pairs. */
    private static Map<String, Object> ordered(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    /** Writes a JSON document: maps keep their order, a list of numbers stays on one line, and the text ends in a newline. */
    private static String json(Object value) {
        StringBuilder out = new StringBuilder();
        writeJson(value, out, "");
        return out.append('\n').toString();
    }

    private static void writeJson(Object value, StringBuilder out, String indent) {
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                out.append("{}");
                return;
            }
            out.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                out.append(indent).append("  ").append(quote(entry.getKey().toString())).append(": ");
                writeJson(entry.getValue(), out, indent + "  ");
                out.append(++i < map.size() ? ",\n" : "\n");
            }
            out.append(indent).append('}');
        } else if (value instanceof List<?> list) {
            boolean numbers = list.stream().allMatch(item -> item instanceof Number);
            if (list.isEmpty() || numbers) {
                out.append('[');
                for (int i = 0; i < list.size(); i++) {
                    out.append(i > 0 ? ", " : "");
                    writeJson(list.get(i), out, indent);
                }
                out.append(']');
                return;
            }
            out.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                out.append(indent).append("  ");
                writeJson(list.get(i), out, indent + "  ");
                out.append(i + 1 < list.size() ? ",\n" : "\n");
            }
            out.append(indent).append(']');
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else {
            out.append(quote(value.toString()));
        }
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void writeText(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        Writer writer = new java.io.OutputStreamWriter(zip, StandardCharsets.UTF_8);
        writer.write(text);
        writer.flush();
        zip.closeEntry();
    }

    private static void writePng(ZipOutputStream zip, String name, BufferedImage image) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes.toByteArray());
        zip.closeEntry();
    }
}
