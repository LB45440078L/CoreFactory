package io.corefactory.spigot;

import io.corefactory.core.engine.VisualCatalog;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.logistics.BeltShape;
import io.corefactory.spigot.logistics.BeltPath;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the item model keys against the resource pack. The pack defines each model at
 * {@code assets/corefactory/items/<kind>/<id>.json}, and the game asks for the key {@link ItemFactory#modelKey} builds. A
 * machine, ore, component, TNT or particle sprite whose key has the wrong prefix shows a missing-texture checkerboard, so
 * every id in the shipped data must have its definition under the kind it is looked up by. Skipped when the pack zip has
 * not been built yet.
 */
class ResourcePackKeysTest {

    private static final Path PACK = Path.of("..", "CoreFactory-ResourcePack", "target", "CoreFactory-ResourcePack.zip");
    private static final String NAMESPACE = "corefactory";

    @Test
    void everyMachineOreComponentTntAndParticleHasItsDefinitionUnderItsKind() throws IOException {
        Assumptions.assumeTrue(Files.isRegularFile(PACK), () -> "The resource pack is not built: " + PACK.toAbsolutePath());
        try (ZipFile zip = new ZipFile(PACK.toFile())) {
            Set<String> names = new HashSet<>();
            zip.stream().map(ZipEntry::getName).forEach(names::add);
            for (String id : ids("processes.yml", "machines")) {
                requireDefinition(zip, names, ItemFactory.MACHINE, id);
            }
            for (String id : ids("ores.yml", "ores")) {
                requireDefinition(zip, names, ItemFactory.ORE, id);
            }
            for (String id : ids("materials.yml", "components")) {
                requireDefinition(zip, names, ItemFactory.COMPONENT, id);
            }
            for (String id : ids("items.yml", "components")) {
                requireDefinition(zip, names, ItemFactory.COMPONENT, id);
            }
            for (String id : ids("tnts.yml", "tnts")) {
                requireDefinition(zip, names, ItemFactory.TNT, id);
            }
            for (String id : VisualCatalog.SPRITES) {
                requireDefinition(zip, names, ItemFactory.PARTICLE, id);
            }
        }
    }

    @Test
    void everyDeviceDisplayHasItsModelUnderItsKeyInThePack() throws IOException {
        Assumptions.assumeTrue(Files.isRegularFile(PACK), () -> "The resource pack is not built: " + PACK.toAbsolutePath());
        try (ZipFile zip = new ZipFile(PACK.toFile())) {
            Set<String> names = new HashSet<>();
            zip.stream().map(ZipEntry::getName).forEach(names::add);
            for (String id : DeviceDisplayKeysTest.machineDevices()) {
                assertThat(ItemFactory.displayKey(id)).as(id).isEqualTo(ItemFactory.MACHINE + "/" + id);
                requireDefinition(zip, names, ItemFactory.MACHINE, id);
            }
            for (String id : DeviceDisplayKeysTest.beltDevices()) {
                for (BeltShape shape : BeltShape.values()) {
                    requireBeltDefinition(zip, names, id, shape);
                }
            }
        }
    }

    /**
     * Checks one belt shape: its display key is the definition's path under {@code items/belt}, and the definition names the
     * shape's model, which is in the pack. The straight shape is {@code belt/<id>}; the left corner is {@code belt/<id>/corner}; the
     * right corner is {@code belt/<id>/corner_right}.
     */
    private static void requireBeltDefinition(ZipFile zip, Set<String> names, String id, BeltShape belt) throws IOException {
        String key = BeltPath.modelKeyOf(id, belt);
        String shape = switch (belt) {
            case STRAIGHT -> "straight";
            case CORNER_LEFT -> "corner";
            case CORNER_RIGHT -> "corner_right";
        };
        String definition = "assets/" + NAMESPACE + "/items/" + key + ".json";
        assertThat(names).as(key).contains(definition);
        assertThat(read(zip, definition)).as(definition)
                .contains("\"" + NAMESPACE + ":item/belt/" + id + "/" + shape + "\"");
        assertThat(names).as(key).contains("assets/" + NAMESPACE + "/models/item/belt/" + id + "/" + shape + ".json");
    }

    /**
     * Checks one entry: its key is {@code kind/id}, its definition is in the pack, and the definition names the model the
     * pack ships for it. An ore's model is a block model, and every other kind's is an item model.
     */
    private static void requireDefinition(ZipFile zip, Set<String> names, String kind, String id) throws IOException {
        String key = ItemFactory.modelKey(kind, id);
        assertThat(key).as(kind + " " + id).isEqualTo(kind + "/" + id);
        String definition = "assets/" + NAMESPACE + "/items/" + kind + "/" + id + ".json";
        assertThat(names).as(definition).contains(definition);
        String text = read(zip, definition);
        if (ItemFactory.ORE.equals(kind)) {
            assertThat(text).as(definition).contains("\"" + NAMESPACE + ":block/ore/" + id + "\"");
        } else {
            assertThat(text).as(definition).contains("\"" + NAMESPACE + ":item/" + key + "\"");
            assertThat(names).as(kind + " " + id).contains("assets/" + NAMESPACE + "/models/item/" + key + ".json");
        }
    }

    private static List<String> ids(String file, String section) {
        ConfigurationSection root = resource(file).getConfigurationSection(section);
        assertThat(root).as(file + " " + section).isNotNull();
        return List.copyOf(root.getKeys(false));
    }

    private static String read(ZipFile zip, String name) throws IOException {
        try (InputStream in = zip.getInputStream(zip.getEntry(name))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static YamlConfiguration resource(String name) {
        try (Reader reader = new InputStreamReader(
                ResourcePackKeysTest.class.getClassLoader().getResourceAsStream(name), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + name, e);
        }
    }
}
