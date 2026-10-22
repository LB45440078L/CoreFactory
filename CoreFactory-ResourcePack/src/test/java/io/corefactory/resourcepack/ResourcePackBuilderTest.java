package io.corefactory.resourcepack;

import io.corefactory.api.PackVersion;
import io.corefactory.resourcepack.art.ArtGenerator;
import io.corefactory.resourcepack.art.BeltArt;
import io.corefactory.resourcepack.art.Color;
import io.corefactory.resourcepack.art.ComponentArt;
import io.corefactory.resourcepack.art.LevelArt;
import io.corefactory.resourcepack.art.MachineAnimation;
import io.corefactory.resourcepack.art.MachineArt;
import io.corefactory.resourcepack.art.OreArt;
import io.corefactory.resourcepack.art.ParticleArt;
import io.corefactory.resourcepack.art.ProcessMachineArt;
import io.corefactory.resourcepack.art.TntArt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

class ResourcePackBuilderTest {

    @TempDir
    Path dir;

    @Test
    void packsEveryItemWithItsTextureModelAndDefinition() throws IOException {
        Path art = dir.resolve("art");
        ArtGenerator.main(new String[]{art.toString()});
        Path zip = dir.resolve("pack.zip");

        int items = ResourcePackBuilder.build(art, zip, 84, "test", "1.2.3", 20261019120000L);

        assertThat(items).isEqualTo(TntArt.ALL.size() + ComponentArt.ALL.size() + MachineArt.ALL.size()
                + ProcessMachineArt.ALL.size() + LevelArt.LAB_MACHINES.size() * LevelArt.LEVELS.size()
                + OreArt.ALL.size() + ParticleArt.ALL.size() + BeltArt.ALL.size());
        try (ZipFile file = new ZipFile(zip.toFile())) {
            String mcmeta = read(file, "pack.mcmeta");
            assertThat(mcmeta).contains("\"pack_format\": 84");
            assertThat(read(file, PackVersion.ENTRY)).contains("version=1.2.3", "build=20261019120000");
            Set<String> names = new HashSet<>();
            file.stream().map(ZipEntry::getName).forEach(names::add);
            for (TntArt.Spec spec : TntArt.ALL) {
                assertThat(names).contains(
                        "assets/corefactory/textures/block/tnt/" + spec.id() + ".png",
                        "assets/corefactory/models/item/tnt/" + spec.id() + ".json",
                        "assets/corefactory/items/tnt/" + spec.id() + ".json");
            }
            for (ComponentArt.Spec spec : ComponentArt.ALL) {
                assertThat(names).contains(
                        "assets/corefactory/textures/item/component/" + spec.id() + ".png",
                        "assets/corefactory/models/item/component/" + spec.id() + ".json",
                        "assets/corefactory/items/component/" + spec.id() + ".json");
            }
            for (MachineArt.Spec spec : MachineArt.ALL) {
                assertThat(names).contains(
                        "assets/corefactory/models/item/machine/" + spec.id() + ".json",
                        "assets/corefactory/items/machine/" + spec.id() + ".json");
            }
            for (String id : List.of("energy_wire", "diesel", "nuclear_fuel_rod", "pylon_1", "pylon_2", "pylon_3",
                    "accumulator_1", "accumulator_2", "accumulator_3", "wind_generator", "solar_panel",
                    "charcoal_generator", "petroleum_generator", "diesel_generator", "nuclear_generator",
                    "crude_oil_refinery")) {
                assertThat(names).as(id).contains("assets/corefactory/items/component/" + id + ".json");
            }
            for (String id : List.of("pylon_1", "pylon_2", "pylon_3", "accumulator_1", "accumulator_2", "accumulator_3",
                    "wind_generator", "solar_panel", "charcoal_generator", "petroleum_generator", "diesel_generator",
                    "nuclear_generator", "crude_oil_refinery", "picker_1", "picker_2", "picker_3", "splitter_1",
                    "splitter_2", "splitter_3")) {
                assertThat(names).as(id).contains("assets/corefactory/models/item/machine/" + id + ".json");
            }
            for (BeltArt.Spec spec : BeltArt.ALL) {
                String id = spec.id();
                assertThat(names).contains(
                        "assets/corefactory/items/component/" + id + ".json",
                        "assets/corefactory/textures/item/component/" + id + ".png",
                        "assets/corefactory/models/item/belt/" + id + "/straight.json",
                        "assets/corefactory/models/item/belt/" + id + "/corner.json",
                        "assets/corefactory/models/item/belt/" + id + "/corner_right.json",
                        "assets/corefactory/items/belt/" + id + ".json",
                        "assets/corefactory/items/belt/" + id + "/corner_right.json",
                        "assets/corefactory/textures/block/belt/" + id + "/top_straight.png",
                        "assets/corefactory/textures/block/belt/" + id + "/top_straight.png.mcmeta",
                        "assets/corefactory/textures/block/belt/" + id + "/top_corner.png",
                        "assets/corefactory/textures/block/belt/" + id + "/top_corner.png.mcmeta",
                        "assets/corefactory/textures/block/belt/" + id + "/side.png");
            }
            String tntModel = read(file, "assets/corefactory/models/item/tnt/nuke_tsar_bomba.json");
            assertThat(tntModel).contains("corefactory:block/tnt/nuke_tsar_bomba");
            // A placed TNT is shown by an item display in the "fixed" context: it must be full size, or the vanilla block
            // shows around it.
            assertThat(tntModel).contains("\"fixed\": {\"rotation\": [0, 0, 0], \"translation\": [0, 0, 0], \"scale\": [1, 1, 1]}");
            assertThat(read(file, "assets/corefactory/models/item/component/nuclear_casing.json"))
                    .contains("corefactory:item/component/nuclear_casing");
        }
    }

    /**
     * Every machine's model has at least one element, and every face of every element refers to a texture variable the
     * model defines, whose PNG is in the zip. The model is JSON, which the YAML parser reads too.
     */
    @Test
    @SuppressWarnings("unchecked")
    void everyMachineModelHasElementsAndWrittenTextures() throws IOException {
        Path art = dir.resolve("art");
        ArtGenerator.main(new String[]{art.toString()});
        Path zip = dir.resolve("pack.zip");
        ResourcePackBuilder.build(art, zip, 84, "test", "1.2.3", 20261019120000L);

        try (ZipFile file = new ZipFile(zip.toFile())) {
            for (MachineArt.Spec spec : MachineArt.ALL) {
                Map<String, Object> model = new Yaml().load(
                        read(file, "assets/corefactory/models/item/machine/" + spec.id() + ".json"));
                Map<String, Object> textures = (Map<String, Object>) model.get("textures");
                assertThat(textures.keySet()).as(spec.id()).containsExactlyInAnyOrder(
                        "north", "south", "east", "west", "up", "down");
                for (String variable : textures.keySet()) {
                    String path = textures.get(variable).toString().replace("corefactory:", "");
                    ZipEntry texture = file.getEntry("assets/corefactory/textures/" + path + ".png");
                    assertThat(texture).as(spec.id() + " " + variable).isNotNull();
                    assertThat(texture.getSize()).as(spec.id() + " " + variable).isPositive();
                }

                List<Map<String, Object>> elements = (List<Map<String, Object>>) model.get("elements");
                assertThat(elements).as(spec.id()).isNotEmpty();
                for (Map<String, Object> element : elements) {
                    for (String corner : List.of("from", "to")) {
                        for (Object value : (List<Object>) element.get(corner)) {
                            assertThat(((Number) value).doubleValue()).as(spec.id()).isBetween(0.0, 16.0);
                        }
                    }
                    Map<String, Object> faces = (Map<String, Object>) element.get("faces");
                    assertThat(faces.keySet()).as(spec.id()).containsExactlyInAnyOrder(
                            "north", "south", "east", "west", "up", "down");
                    for (Object face : faces.values()) {
                        String reference = ((Map<String, Object>) face).get("texture").toString();
                        assertThat(reference).as(spec.id()).startsWith("#");
                        assertThat(textures).as(spec.id()).containsKey(reference.substring(1));
                    }
                }
            }
        }
    }

    /**
     * Every ore has its block texture, a block model that paints all six faces with it, an item definition for its
     * inventory icon, and a blockstate override of its host. Every variant of the override names a model in the zip.
     */
    @Test
    @SuppressWarnings("unchecked")
    void everyOreHasItsModelDefinitionAndHostBlockstate() throws IOException {
        Path art = dir.resolve("art");
        ArtGenerator.main(new String[]{art.toString()});
        Path zip = dir.resolve("pack.zip");
        ResourcePackBuilder.build(art, zip, 84, "test", "1.2.3", 20261019120000L);

        try (ZipFile file = new ZipFile(zip.toFile())) {
            for (OreArt.Spec spec : OreArt.ALL) {
                String id = spec.id();
                assertThat(file.getEntry("assets/corefactory/textures/block/ore/" + id + ".png")).as(id).isNotNull();

                Map<String, Object> model = new Yaml().load(
                        read(file, "assets/corefactory/models/block/ore/" + id + ".json"));
                assertThat(model.get("parent")).as(id).isEqualTo("minecraft:block/cube_all");
                assertThat(((Map<String, Object>) model.get("textures")).get("all")).as(id)
                        .isEqualTo("corefactory:block/ore/" + id);

                Map<String, Object> definition = new Yaml().load(read(file, "assets/corefactory/items/ore/" + id + ".json"));
                assertThat(((Map<String, Object>) definition.get("model")).get("model")).as(id)
                        .isEqualTo("corefactory:block/ore/" + id);

                String host = spec.host().toLowerCase(Locale.ROOT);
                Map<String, Object> blockstate = new Yaml().load(read(file, "assets/minecraft/blockstates/" + host + ".json"));
                Map<String, Object> variants = (Map<String, Object>) blockstate.get("variants");
                assertThat(variants.keySet()).as(id).containsExactlyInAnyOrder(
                        "facing=north", "facing=south", "facing=east", "facing=west");
                for (Object value : variants.values()) {
                    Map<String, Object> variant = (Map<String, Object>) value;
                    assertThat(variant.keySet()).as(id).containsExactly("model");
                    String reference = variant.get("model").toString();
                    int colon = reference.indexOf(':');
                    String modelEntry = "assets/" + reference.substring(0, colon) + "/models/"
                            + reference.substring(colon + 1) + ".json";
                    assertThat(file.getEntry(modelEntry)).as(id + " " + reference).isNotNull();
                }
            }
        }
    }

    /** Every effect sprite has its texture, an item model and an item definition, and its texture has visible pixels. */
    @Test
    @SuppressWarnings("unchecked")
    void everyParticleHasItsThreeFilesAndVisiblePixels() throws IOException {
        Path art = dir.resolve("art");
        ArtGenerator.main(new String[]{art.toString()});
        Path zip = dir.resolve("pack.zip");
        ResourcePackBuilder.build(art, zip, 84, "test", "1.2.3", 20261019120000L);

        try (ZipFile file = new ZipFile(zip.toFile())) {
            for (ParticleArt.Spec spec : ParticleArt.ALL) {
                String id = spec.id();
                String texture = "assets/corefactory/textures/item/particle/" + id + ".png";
                assertThat(file.getEntry(texture)).as(id).isNotNull();
                assertThat(file.getEntry("assets/corefactory/models/item/particle/" + id + ".json")).as(id).isNotNull();
                assertThat(file.getEntry("assets/corefactory/items/particle/" + id + ".json")).as(id).isNotNull();

                Map<String, Object> model = new Yaml().load(
                        read(file, "assets/corefactory/models/item/particle/" + id + ".json"));
                assertThat(((Map<String, Object>) model.get("textures")).get("layer0")).as(id)
                        .isEqualTo("corefactory:item/particle/" + id);

                BufferedImage image;
                try (var in = file.getInputStream(file.getEntry(texture))) {
                    image = ImageIO.read(in);
                }
                int visible = 0;
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        if (Color.alpha(image.getRGB(x, y)) > 0) {
                            visible++;
                        }
                    }
                }
                assertThat(visible).as(id).isPositive();
            }
        }
    }

    /**
     * The right corner is the left corner with its top mirrored by the texture mapping, and it has the same display as the other
     * belt models. A display that mirrors the model with a negative scale turns its faces inside out, so the mirror is never in
     * the display.
     */
    @Test
    @SuppressWarnings("unchecked")
    void theRightCornerMirrorsItsTopTextureAndNotItsDisplay() throws IOException {
        Path art = dir.resolve("art");
        ArtGenerator.main(new String[]{art.toString()});
        Path zip = dir.resolve("pack.zip");
        ResourcePackBuilder.build(art, zip, 84, "test", "1.2.3", 20261019120000L);

        try (ZipFile file = new ZipFile(zip.toFile())) {
            for (BeltArt.Spec spec : BeltArt.ALL) {
                String id = spec.id();
                String base = "assets/corefactory/models/item/belt/" + id + "/";
                Map<String, Object> left = new Yaml().load(read(file, base + "corner.json"));
                Map<String, Object> right = new Yaml().load(read(file, base + "corner_right.json"));
                assertThat(upUv(left)).as(id + " left").isEqualTo(List.of(16, 16, 0, 0));
                assertThat(upUv(right)).as(id + " right").isEqualTo(List.of(0, 16, 16, 0));
                assertThat(right.get("display")).as(id + " display").isEqualTo(left.get("display"));
                assertThat(right.get("textures")).as(id + " textures").isEqualTo(left.get("textures"));
            }
        }
    }

    /** The UV of the up face of a model's first element. */
    @SuppressWarnings("unchecked")
    private static Object upUv(Map<String, Object> model) {
        Map<String, Object> element = ((List<Map<String, Object>>) model.get("elements")).get(0);
        Map<String, Object> faces = (Map<String, Object>) element.get("faces");
        return ((Map<String, Object>) faces.get("up")).get("uv");
    }

    /**
     * Every belt level has a straight and a corner model: each is one flat slab of 16 x 2 x 16 whose top refers to its
     * animated top and whose sides refer to the side texture, all present in the zip. Each animated top has a
     * {@code .mcmeta} with the frame time, and its PNG is {@code FRAMES} square frames tall.
     */
    @Test
    @SuppressWarnings("unchecked")
    void everyBeltHasItsTwoSlabModelsAndAnimatedTops() throws IOException {
        Path art = dir.resolve("art");
        ArtGenerator.main(new String[]{art.toString()});
        Path zip = dir.resolve("pack.zip");
        ResourcePackBuilder.build(art, zip, 84, "test", "1.2.3", 20261019120000L);

        try (ZipFile file = new ZipFile(zip.toFile())) {
            for (BeltArt.Spec spec : BeltArt.ALL) {
                String id = spec.id();
                String base = "assets/corefactory/";
                for (String shape : List.of("straight", "corner", "corner_right")) {
                    Map<String, Object> model = new Yaml().load(
                            read(file, base + "models/item/belt/" + id + "/" + shape + ".json"));
                    Map<String, Object> textures = (Map<String, Object>) model.get("textures");
                    assertThat(textures.keySet()).as(id + " " + shape).containsExactlyInAnyOrder("top", "side");
                    assertThat(textures.get("top")).as(id + " " + shape)
                            .isEqualTo("corefactory:block/belt/" + id + "/top_" + shape.replace("_right", ""));
                    assertThat(textures.get("side")).as(id + " " + shape)
                            .isEqualTo("corefactory:block/belt/" + id + "/side");
                    for (Object value : textures.values()) {
                        String path = value.toString().replace("corefactory:", "");
                        assertThat(file.getEntry(base + "textures/" + path + ".png")).as(id + " " + path).isNotNull();
                    }

                    List<Map<String, Object>> elements = (List<Map<String, Object>>) model.get("elements");
                    assertThat(elements).as(id).hasSize(1);
                    Map<String, Object> element = elements.get(0);
                    for (String corner : List.of("from", "to")) {
                        for (Object value : (List<Object>) element.get(corner)) {
                            assertThat(((Number) value).doubleValue()).as(id).isBetween(0.0, 16.0);
                        }
                    }
                    assertThat(element.get("from")).as(id).isEqualTo(List.of(0, 0, 0));
                    assertThat(element.get("to")).as(id).isEqualTo(List.of(16, 2, 16));
                    Map<String, Object> faces = (Map<String, Object>) element.get("faces");
                    assertThat(faces.keySet()).as(id).containsExactlyInAnyOrder(
                            "north", "south", "east", "west", "up", "down");
                    assertThat(((Map<String, Object>) faces.get("up")).get("texture")).as(id).isEqualTo("#top");
                    for (Object face : faces.values()) {
                        String reference = ((Map<String, Object>) face).get("texture").toString();
                        assertThat(textures).as(id).containsKey(reference.substring(1));
                    }
                }

                Map<String, Object> definition = new Yaml().load(read(file, base + "items/belt/" + id + ".json"));
                assertThat(((Map<String, Object>) definition.get("model")).get("model")).as(id)
                        .isEqualTo("corefactory:item/belt/" + id + "/straight");

                for (String top : List.of("top_straight", "top_corner")) {
                    String name = base + "textures/block/belt/" + id + "/" + top + ".png";
                    Map<String, Object> meta = new Yaml().load(read(file, name + ".mcmeta"));
                    assertThat(((Map<String, Object>) meta.get("animation")).get("frametime")).as(id + " " + top)
                            .isEqualTo(BeltArt.FRAME_TIME);
                    BufferedImage strip;
                    try (var in = file.getInputStream(file.getEntry(name))) {
                        strip = ImageIO.read(in);
                    }
                    assertThat(strip.getWidth()).as(id + " " + top).isEqualTo(BeltArt.TILE);
                    assertThat(strip.getHeight()).as(id + " " + top).isEqualTo(BeltArt.FRAMES * BeltArt.TILE);
                }
            }
        }
    }

    /**
     * Every animated machine face is packed as a strip {@link MachineAnimation#FRAME} pixels wide and frames times that
     * tall, with a {@code .mcmeta} beside it that gives the frame time. The model refers to that strip, and no other
     * machine face has a {@code .mcmeta}.
     */
    @Test
    @SuppressWarnings("unchecked")
    void everyAnimatedMachineFaceIsAStripWithItsFrametime() throws IOException {
        Path art = dir.resolve("art");
        ArtGenerator.main(new String[]{art.toString()});
        Path zip = dir.resolve("pack.zip");
        ResourcePackBuilder.build(art, zip, 84, "test", "1.2.3", 20261019120000L);

        try (ZipFile file = new ZipFile(zip.toFile())) {
            Set<String> expectedMeta = new HashSet<>();
            List<MachineAnimation.Clip> clips = new ArrayList<>(MachineAnimation.ALL);
            for (ProcessMachineArt.Clip process : ProcessMachineArt.CLIPS) {
                clips.add(new MachineAnimation.Clip(process.id(), process.face(), process.frametime()));
            }
            for (MachineAnimation.Clip clip : clips) {
                String id = clip.id();
                String path = "block/machine/" + id + "/" + clip.face().key();
                expectedMeta.add("assets/corefactory/textures/" + path + ".png.mcmeta");
                if (LevelArt.LAB_MACHINES.contains(id)) {
                    for (int level : LevelArt.LEVELS) {
                        expectedMeta.add("assets/corefactory/textures/block/machine/" + id + "_" + level + "/"
                                + clip.face().key() + ".png.mcmeta");
                    }
                }

                Map<String, Object> model = new Yaml().load(read(file, "assets/corefactory/models/item/machine/" + id + ".json"));
                assertThat(((Map<String, Object>) model.get("textures")).get(clip.face().key())).as(id)
                        .isEqualTo("corefactory:" + path);

                Map<String, Object> meta = new Yaml().load(read(file, "assets/corefactory/textures/" + path + ".png.mcmeta"));
                assertThat(((Map<String, Object>) meta.get("animation")).get("frametime")).as(id)
                        .isEqualTo(clip.frametime());
                assertThat(clip.frametime()).as(id).isBetween(1, 4);

                BufferedImage strip;
                try (var in = file.getInputStream(file.getEntry("assets/corefactory/textures/" + path + ".png"))) {
                    strip = ImageIO.read(in);
                }
                assertThat(strip.getWidth()).as(id).isEqualTo(MachineAnimation.FRAME);
                assertThat(strip.getHeight()).as(id).isEqualTo(MachineAnimation.FRAMES * strip.getWidth());
            }
            Set<String> written = new HashSet<>();
            file.stream().map(ZipEntry::getName)
                    .filter(name -> name.startsWith("assets/corefactory/textures/block/machine/") && name.endsWith(".png.mcmeta"))
                    .forEach(written::add);
            assertThat(written).containsExactlyInAnyOrderElementsOf(expectedMeta);
        }
    }

    private static String read(ZipFile file, String name) throws IOException {
        ZipEntry entry = file.getEntry(name);
        assertThat(entry).as(name).isNotNull();
        try (var in = file.getInputStream(entry)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
