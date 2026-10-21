package io.corefactory.spigot.machine;

import io.corefactory.core.engine.VisualCatalog;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every machine that runs a process has a lab effect for its completed passes, and each one names an effect the catalog
 * defines. Read from the shipped processes.yml, so a new machine without a lab effect fails here.
 */
class LabEffectsTest {

    @Test
    void everyMachineThatRunsAProcessHasALabEffectTheCatalogDefines() throws Exception {
        Set<String> machines = processMachines();
        assertThat(machines).isNotEmpty();
        for (String machine : machines) {
            Optional<String> effect = MachineTicker.labEffect(machine);
            assertThat(effect).as(machine).isPresent();
            assertThat(VisualCatalog.find(effect.get())).as(machine).isPresent();
        }
    }

    private static Set<String> processMachines() throws Exception {
        YamlConfiguration yaml;
        try (Reader reader = new InputStreamReader(
                LabEffectsTest.class.getClassLoader().getResourceAsStream("processes.yml"), StandardCharsets.UTF_8)) {
            yaml = YamlConfiguration.loadConfiguration(reader);
        }
        ConfigurationSection processes = yaml.getConfigurationSection("processes");
        Set<String> machines = new TreeSet<>();
        for (String id : processes.getKeys(false)) {
            machines.add(processes.getConfigurationSection(id).getString("machine"));
        }
        return machines;
    }
}
