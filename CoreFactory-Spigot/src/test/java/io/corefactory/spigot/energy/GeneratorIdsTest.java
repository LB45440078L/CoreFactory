package io.corefactory.spigot.energy;

import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.item.PlaceableItems;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratorIdsTest {

    @Test
    void eachKindHasItsComponentIdAndTheSolarPanelKeepsItsOwn() {
        assertThat(GeneratorIds.componentId(GeneratorKind.WIND)).isEqualTo("wind_generator");
        assertThat(GeneratorIds.componentId(GeneratorKind.SOLAR)).isEqualTo(PlaceableItems.SOLAR_PANEL);
        assertThat(GeneratorIds.componentId(GeneratorKind.CHARCOAL)).isEqualTo("charcoal_generator");
        assertThat(GeneratorIds.componentId(GeneratorKind.PETROLEUM)).isEqualTo("petroleum_generator");
        assertThat(GeneratorIds.componentId(GeneratorKind.DIESEL)).isEqualTo("diesel_generator");
        assertThat(GeneratorIds.componentId(GeneratorKind.NUCLEAR)).isEqualTo("nuclear_generator");
    }

    @Test
    void anIdNamesItsKindAndAnyOtherIdNamesNone() {
        for (GeneratorKind kind : GeneratorKind.values()) {
            assertThat(GeneratorIds.kindOf(GeneratorIds.componentId(kind))).contains(kind);
        }
        assertThat(GeneratorIds.kindOf("pylon_1")).isEmpty();
        assertThat(GeneratorIds.kindOf("petroleum")).isEmpty();
        assertThat(GeneratorIds.kindOf(null)).isEmpty();
    }

    @Test
    void everyGeneratorIdIsAnEnergyDeviceOfTheCore() {
        for (GeneratorKind kind : GeneratorKind.values()) {
            assertThat(PlaceableItems.ENERGY_DEVICES).contains(GeneratorIds.componentId(kind));
        }
    }
}
