package io.moretntplus.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MechanicSettingsTest {

    @Test
    void readsTypedValues() {
        MechanicSettings settings = new MechanicSettings(Map.of(
                "radius", 4,
                "name", "crater",
                "enabled", true,
                "scale", 1.5));

        assertThat(settings.getInt("radius", 0)).isEqualTo(4);
        assertThat(settings.getDouble("scale", 0)).isEqualTo(1.5);
        assertThat(settings.getString("name", "")).isEqualTo("crater");
        assertThat(settings.getBoolean("enabled", false)).isTrue();
    }

    @Test
    void missingOrWrongTypeFallsBackToDefault() {
        MechanicSettings settings = new MechanicSettings(Map.of("radius", "big"));

        assertThat(settings.getInt("radius", 7)).isEqualTo(7);
        assertThat(settings.getDouble("missing", 2.5)).isEqualTo(2.5);
        assertThat(settings.getBoolean("missing", true)).isTrue();
    }

    @Test
    void readsVectorsAndLists() {
        MechanicSettings settings = new MechanicSettings(Map.of(
                "vector", List.of(1, 0.5, 0),
                "materials", List.of("STONE", "DIRT")));

        assertThat(settings.getDoubleArray("vector", null)).containsExactly(1.0, 0.5, 0.0);
        assertThat(settings.getStringList("materials")).containsExactly("STONE", "DIRT");
        assertThat(settings.getStringList("absent")).isEmpty();
    }

    @Test
    void vectorWithNonNumericEntryUsesDefault() {
        MechanicSettings settings = new MechanicSettings(Map.of("vector", List.of(1, "north", 0)));

        assertThat(settings.getDoubleArray("vector", new double[]{9})).containsExactly(9.0);
    }

    @Test
    void readsNestedSectionsAndSectionLists() {
        MechanicSettings settings = new MechanicSettings(Map.of(
                "pool", List.of(
                        Map.of("material", "STONE", "weight", 6),
                        "not a section",
                        Map.of("material", "DIRT", "weight", 2)),
                "inner", Map.of("depth", 3)));

        List<MechanicSettings> pool = settings.getSectionList("pool");
        assertThat(pool).hasSize(2);
        assertThat(pool.get(0).getString("material", "")).isEqualTo("STONE");
        assertThat(pool.get(1).getInt("weight", 0)).isEqualTo(2);
        assertThat(settings.getSection("inner").getInt("depth", 0)).isEqualTo(3);
        assertThat(settings.getSection("absent")).isSameAs(MechanicSettings.EMPTY);
    }
}
