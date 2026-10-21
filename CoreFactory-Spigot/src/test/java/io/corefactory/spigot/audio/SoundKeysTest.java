package io.corefactory.spigot.audio;

import io.corefactory.core.drill.DrillKind;
import io.corefactory.core.energy.GeneratorKind;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the sound keys against the resource pack: every generator kind, drill kind and process machine in processes.yml
 * maps to a sound the pack ships. The sounds are read from the pack's art folder, and from the built zip when it exists.
 * Nothing here needs a server.
 */
class SoundKeysTest {

    private static final Path SOUNDS = Path.of("..", "CoreFactory-ResourcePack", "art", "sounds");
    private static final Path PACK = Path.of("..", "CoreFactory-ResourcePack", "target", "CoreFactory-ResourcePack.zip");
    private static final String NAMESPACE = "corefactory";
    private static final String OGG = ".ogg";

    @Test
    void everyGeneratorKindHasItsOwnSoundInThePack() throws IOException {
        Set<String> sounds = artKeys();
        Set<String> keys = new HashSet<>();
        for (GeneratorKind kind : GeneratorKind.values()) {
            String key = SoundKeys.generator(kind);
            assertThat(sounds).as(kind + " " + key).contains(key);
            keys.add(key);
        }
        assertThat(keys).as("generator sounds").hasSize(GeneratorKind.values().length);
    }

    @Test
    void everyDrillKindHasItsOwnSoundInThePack() throws IOException {
        Set<String> sounds = artKeys();
        Set<String> keys = new HashSet<>();
        for (DrillKind kind : DrillKind.values()) {
            String key = SoundKeys.drill(kind);
            assertThat(sounds).as(kind + " " + key).contains(key);
            keys.add(key);
        }
        assertThat(keys).as("drill sounds").hasSize(DrillKind.values().length);
    }

    @Test
    void everyProcessMachineInProcessesYmlHasAPackSound() throws IOException {
        Set<String> sounds = artKeys();
        for (String id : machineIds()) {
            assertThat(sounds).as(id).contains(SoundKeys.process(id));
        }
    }

    @Test
    void everyRowOfTheProcessTableNamesAMachineAndAPackSound() throws IOException {
        Set<String> machines = Set.copyOf(machineIds());
        Set<String> sounds = artKeys();
        for (Map.Entry<String, String> row : SoundKeys.PROCESSES.entrySet()) {
            assertThat(machines).as(row.getKey()).contains(row.getKey());
            assertThat(sounds).as(row.getKey()).contains(row.getValue());
        }
    }

    @Test
    void everyCompletionSoundIsInThePack() throws IOException {
        Set<String> sounds = artKeys();
        for (String id : machineIds()) {
            SoundKeys.completion(id).ifPresent(key -> assertThat(sounds).as(id + " " + key).contains(key));
        }
    }

    @Test
    void theBuiltPackShipsEveryKeyTheMappingNames() throws IOException {
        Assumptions.assumeTrue(Files.isRegularFile(PACK), () -> "The resource pack is not built: " + PACK.toAbsolutePath());
        assertThat(packKeys()).containsAll(mappedKeys());
    }

    @Test
    void theTurretAndLaserSoundsAreInThePack() throws IOException {
        Set<String> sounds = artKeys();
        for (String key : List.of("turret_fire", "turret_reload", "turret_rotate", "laser_fire", "laser_charge")) {
            assertThat(sounds).as(key).contains(key);
        }
    }

    @Test
    void theTurretAndLaserAccessorsNameTheirSounds() {
        assertThat(SoundKeys.turretFire()).isEqualTo("turret_fire");
        assertThat(SoundKeys.turretReload()).isEqualTo("turret_reload");
        assertThat(SoundKeys.turretRotate()).isEqualTo("turret_rotate");
        assertThat(SoundKeys.laserFire()).isEqualTo("laser_fire");
        assertThat(SoundKeys.laserCharge()).isEqualTo("laser_charge");
    }

    @Test
    void aMachineWithNoSoundOfItsOwnRunsTheGenericLabSound() {
        assertThat(SoundKeys.process("crusher")).isEqualTo(SoundKeys.LAB);
        assertThat(SoundKeys.process("not_a_machine")).isEqualTo(SoundKeys.LAB);
        assertThat(SoundKeys.process(null)).isEqualTo(SoundKeys.LAB);
        assertThat(SoundKeys.LAB).isEqualTo("lab_run");
    }

    @Test
    void theNamedMachinesKeepTheirSounds() {
        assertThat(SoundKeys.process("powder_mill")).isEqualTo("mill_grind");
        assertThat(SoundKeys.process("induction_furnace")).isEqualTo("induction_hiss");
        assertThat(SoundKeys.process("research_console")).isEqualTo("research_console");
        assertThat(SoundKeys.completion("cartridge_press")).contains("press_stamp");
        assertThat(SoundKeys.completion("separator")).isEmpty();
    }

    @Test
    void eachGeneratorAndDrillNamesItsOwnSound() {
        assertThat(SoundKeys.generator(GeneratorKind.WIND)).isEqualTo("wind_generator");
        assertThat(SoundKeys.generator(GeneratorKind.SOLAR)).isEqualTo("solar_panel");
        assertThat(SoundKeys.generator(GeneratorKind.CHARCOAL)).isEqualTo("charcoal_generator");
        assertThat(SoundKeys.generator(GeneratorKind.PETROLEUM)).isEqualTo("petroleum_generator");
        assertThat(SoundKeys.generator(GeneratorKind.DIESEL)).isEqualTo("diesel_generator");
        assertThat(SoundKeys.generator(GeneratorKind.NUCLEAR)).isEqualTo("nuclear_generator");
        assertThat(SoundKeys.drill(DrillKind.ORE)).isEqualTo("ore_drill");
        assertThat(SoundKeys.drill(DrillKind.PETROLEUM)).isEqualTo("petroleum_drill");
    }

    /** Every key the mapping can return: each generator, drill, process machine and completion sound, and the generic one. */
    private static Set<String> mappedKeys() throws IOException {
        Set<String> keys = new HashSet<>();
        keys.add(SoundKeys.LAB);
        keys.add(SoundKeys.turretFire());
        keys.add(SoundKeys.turretReload());
        keys.add(SoundKeys.turretRotate());
        keys.add(SoundKeys.laserFire());
        keys.add(SoundKeys.laserCharge());
        for (GeneratorKind kind : GeneratorKind.values()) {
            keys.add(SoundKeys.generator(kind));
        }
        for (DrillKind kind : DrillKind.values()) {
            keys.add(SoundKeys.drill(kind));
        }
        for (String id : machineIds()) {
            keys.add(SoundKeys.process(id));
            SoundKeys.completion(id).ifPresent(keys::add);
        }
        return keys;
    }

    /** The keys of the sounds in the art folder: one per .ogg file. */
    private static Set<String> artKeys() throws IOException {
        assertThat(SOUNDS).as("sound art folder").isDirectory();
        try (Stream<Path> files = Files.list(SOUNDS)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(OGG))
                    .map(name -> name.substring(0, name.length() - OGG.length()))
                    .collect(Collectors.toSet());
        }
    }

    /** The keys of the sounds in the built pack's zip, from its assets/corefactory/sounds entries. */
    private static Set<String> packKeys() throws IOException {
        String prefix = "assets/" + NAMESPACE + "/sounds/";
        try (ZipFile zip = new ZipFile(PACK.toFile())) {
            return zip.stream().map(ZipEntry::getName)
                    .filter(name -> name.startsWith(prefix) && name.endsWith(OGG))
                    .map(name -> name.substring(prefix.length(), name.length() - OGG.length()))
                    .collect(Collectors.toSet());
        }
    }

    /** The machine ids of processes.yml. */
    private static List<String> machineIds() {
        ConfigurationSection machines = resource("processes.yml").getConfigurationSection("machines");
        assertThat(machines).as("processes.yml machines").isNotNull();
        return List.copyOf(machines.getKeys(false));
    }

    private static YamlConfiguration resource(String name) {
        try (Reader reader = new InputStreamReader(
                SoundKeysTest.class.getClassLoader().getResourceAsStream(name), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + name, e);
        }
    }
}
