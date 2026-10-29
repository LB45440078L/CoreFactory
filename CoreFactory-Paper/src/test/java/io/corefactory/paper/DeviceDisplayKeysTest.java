package io.corefactory.paper;

import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.logistics.LogisticsLevel;
import io.corefactory.paper.energy.EnergyLevels;
import io.corefactory.paper.energy.GeneratorIds;
import io.corefactory.paper.logistics.LogisticsIds;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The display model key of every device: a pylon, accumulator, generator, picker or splitter shows its machine model, and a
 * belt shows its straight or corner model. No device shows its flat component sprite. Nothing here needs the pack or a server.
 */
class DeviceDisplayKeysTest {

    /** The component ids of the devices that show a machine model: energy devices, generators, pickers and splitters. */
    static List<String> machineDevices() {
        List<String> ids = new ArrayList<>();
        for (int level = 1; level <= EnergyLevels.MAX_LEVEL; level++) {
            ids.add(EnergyLevels.componentId(EnergyLevels.PYLON, level));
            ids.add(EnergyLevels.componentId(EnergyLevels.ACCUMULATOR, level));
        }
        for (GeneratorKind kind : GeneratorKind.values()) {
            ids.add(GeneratorIds.componentId(kind));
        }
        for (LogisticsLevel level : LogisticsLevel.values()) {
            ids.add(LogisticsIds.componentId(LogisticsIds.Kind.PICKER, level));
            if (level.compareTo(LogisticsLevel.III) <= 0) {
                ids.add(LogisticsIds.componentId(LogisticsIds.Kind.SPLITTER, level));
            }
        }
        return ids;
    }

    /** The component ids of the belts, one for each level. */
    static List<String> beltDevices() {
        List<String> ids = new ArrayList<>();
        for (LogisticsLevel level : LogisticsLevel.values()) {
            if (level.compareTo(LogisticsLevel.III) <= 0) {
                ids.add(LogisticsIds.componentId(LogisticsIds.Kind.BELT, level));
            }
        }
        return ids;
    }

    @Test
    void everyMachineDeviceDisplaysItsMachineModel() {
        assertThat(machineDevices()).hasSize(24);
        for (String id : machineDevices()) {
            assertThat(ItemFactory.displayKey(id)).as(id).isEqualTo(ItemFactory.MACHINE + "/" + id);
        }
    }

    @Test
    void everyBeltDisplaysItsStraightModelOrItsCornerModel() {
        for (String id : beltDevices()) {
            assertThat(ItemFactory.beltKey(id, false)).as(id).isEqualTo("belt/" + id);
            assertThat(ItemFactory.beltKey(id, true)).as(id).isEqualTo("belt/" + id + "/corner");
        }
    }

    @Test
    void noDeviceDisplayUsesTheComponentSprite() {
        for (String id : machineDevices()) {
            assertThat(ItemFactory.displayKey(id)).as(id).doesNotStartWith(ItemFactory.COMPONENT + "/");
        }
        for (String id : beltDevices()) {
            assertThat(ItemFactory.beltKey(id, false)).as(id).doesNotStartWith(ItemFactory.COMPONENT + "/");
            assertThat(ItemFactory.beltKey(id, true)).as(id).doesNotStartWith(ItemFactory.COMPONENT + "/");
        }
    }
}
