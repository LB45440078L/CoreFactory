package io.moretntplus.resourcepack;

import org.yaml.snakeyaml.Yaml;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds the MoreTNT+ resource pack: one 16x16 texture, one cube model and one item definition per TNT and
 * component, plus {@code pack.mcmeta} and {@code pack.png}.
 *
 * <p>Arguments: {@code tnts.yml} {@code items.yml} {@code output.zip} {@code pack-format} {@code description}.
 * The item ids and model names match the ones the plugin sets on items, so the textures appear automatically.
 */
public final class ResourcePackBuilder {

    private static final String NAMESPACE = "moretntplus";

    private ResourcePackBuilder() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 5) {
            System.err.println("usage: ResourcePackBuilder <tnts.yml> <items.yml> <output.zip> <pack-format> <description>");
            System.exit(2);
        }
        Path tnts = Path.of(args[0]);
        Path items = Path.of(args[1]);
        Path output = Path.of(args[2]);
        int packFormat = Integer.parseInt(args[3]);
        String description = args[4];

        Map<String, Object> tntSection = section(load(tnts), "tnts");
        Map<String, Object> componentSection = section(load(items), "components");

        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        int count = 0;
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            writeText(zip, "pack.mcmeta", "{\n  \"pack\": {\n    \"pack_format\": " + packFormat
                    + ",\n    \"description\": \"" + description + "\"\n  }\n}\n");
            writePng(zip, "pack.png", icon());
            for (Map.Entry<String, Object> entry : tntSection.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> spec) {
                    addItem(zip, "tnt/" + entry.getKey(), texture(spec), entry.getKey());
                    count++;
                }
            }
            for (Map.Entry<String, Object> entry : componentSection.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> spec) {
                    addItem(zip, "component/" + entry.getKey(), texture(spec), entry.getKey());
                    count++;
                }
            }
        }
        System.out.println("Wrote " + count + " item models to " + output.toAbsolutePath());
    }

    private static void addItem(ZipOutputStream zip, String path, Map<?, ?> spec, String id) throws IOException {
        Map<?, ?> texture = spec;
        String base = string(texture, "base", "#808080");
        String accent = string(texture, "accent", "#FFFFFF");
        String pattern = string(texture, "pattern", "stripes");
        BufferedImage image = TextureRenderer.render(base, accent, pattern, id);
        writePng(zip, "assets/" + NAMESPACE + "/textures/block/" + path + ".png", image);
        writeText(zip, "assets/" + NAMESPACE + "/models/item/" + path + ".json",
                "{\n  \"parent\": \"minecraft:block/cube_all\",\n  \"textures\": {\n    \"all\": \""
                        + NAMESPACE + ":block/" + path + "\"\n  },\n  \"display\": {\n"
                        + "    \"fixed\": {\"rotation\": [0, 0, 0], \"translation\": [0, 0, 0], \"scale\": [1, 1, 1]},\n"
                        + "    \"gui\": {\"rotation\": [30, 225, 0], \"translation\": [0, 0, 0], \"scale\": [0.625, 0.625, 0.625]}\n"
                        + "  }\n}\n");
        writeText(zip, "assets/" + NAMESPACE + "/items/" + path + ".json",
                "{\n  \"model\": {\n    \"type\": \"minecraft:model\",\n    \"model\": \""
                        + NAMESPACE + ":item/" + path + "\"\n  }\n}\n");
    }

    private static Map<?, ?> texture(Map<?, ?> spec) {
        Object value = spec.get("texture");
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private static String string(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : value.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> load(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            Object root = new Yaml().load(in);
            return root instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        }
    }

    private static Map<String, Object> section(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) map;
            return typed;
        }
        return Map.of();
    }

    /** A 128x128 pack icon: the warning texture scaled up eight times. */
    private static BufferedImage icon() {
        BufferedImage image = TextureRenderer.render("#7A1E1E", "#FFD000", "stripes", "pack-icon");
        BufferedImage large = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                large.setRGB(x, y, image.getRGB(x / 8, y / 8));
            }
        }
        return large;
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
