package io.corefactory.paper.machine;

import io.corefactory.core.process.ItemRef;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StorageKindsTest {

    private static final List<String> COPPER_CHESTS = List.of("COPPER_CHEST", "EXPOSED_COPPER_CHEST", "WEATHERED_COPPER_CHEST",
            "OXIDIZED_COPPER_CHEST", "WAXED_COPPER_CHEST", "WAXED_EXPOSED_COPPER_CHEST", "WAXED_WEATHERED_COPPER_CHEST",
            "WAXED_OXIDIZED_COPPER_CHEST");

    private static final List<String> SHULKER_BOXES = List.of("SHULKER_BOX", "WHITE_SHULKER_BOX", "ORANGE_SHULKER_BOX",
            "MAGENTA_SHULKER_BOX", "LIGHT_BLUE_SHULKER_BOX", "YELLOW_SHULKER_BOX", "LIME_SHULKER_BOX", "PINK_SHULKER_BOX",
            "GRAY_SHULKER_BOX", "LIGHT_GRAY_SHULKER_BOX", "CYAN_SHULKER_BOX", "PURPLE_SHULKER_BOX", "BLUE_SHULKER_BOX",
            "BROWN_SHULKER_BOX", "GREEN_SHULKER_BOX", "RED_SHULKER_BOX", "BLACK_SHULKER_BOX");

    @Test
    void chestsBarrelsShulkerBoxesAndCopperChestsAreStorage() {
        assertThat(StorageKinds.isStorage("CHEST")).isTrue();
        assertThat(StorageKinds.isStorage("TRAPPED_CHEST")).isTrue();
        assertThat(StorageKinds.isStorage("BARREL")).isTrue();
        assertThat(StorageKinds.isStorage("RED_SHULKER_BOX")).isTrue();
        assertThat(StorageKinds.isStorage("OXIDIZED_COPPER_CHEST")).isTrue();
        assertThat(StorageKinds.isStorage("WAXED_COPPER_CHEST")).isTrue();
        assertThat(StorageKinds.isStorage("WAXED_OXIDIZED_COPPER_CHEST")).isTrue();
    }

    @Test
    void everyCopperChestAndShulkerBoxIsStorage() {
        for (String name : COPPER_CHESTS) {
            assertThat(StorageKinds.isStorage(name)).as(name).isTrue();
        }
        for (String name : SHULKER_BOXES) {
            assertThat(StorageKinds.isStorage(name)).as(name).isTrue();
        }
    }

    @Test
    void theCopperChestAndShulkerNamesAreMaterialsOfTheBuildsApi() {
        for (String name : COPPER_CHESTS) {
            assertThat(Material.matchMaterial(name)).as(name).isNotNull();
        }
        for (String name : SHULKER_BOXES) {
            assertThat(Material.matchMaterial(name)).as(name).isNotNull();
        }
        assertThat(Material.matchMaterial("CHEST")).isNotNull();
        assertThat(Material.matchMaterial("TRAPPED_CHEST")).isNotNull();
        assertThat(Material.matchMaterial("BARREL")).isNotNull();
    }

    @Test
    void furnacesBrewingStandsAndOtherContainersAreNotStorage() {
        assertThat(StorageKinds.isStorage("FURNACE")).isFalse();
        assertThat(StorageKinds.isStorage("BLAST_FURNACE")).isFalse();
        assertThat(StorageKinds.isStorage("SMOKER")).isFalse();
        assertThat(StorageKinds.isStorage("BREWING_STAND")).isFalse();
        assertThat(StorageKinds.isStorage("DISPENSER")).isFalse();
        assertThat(StorageKinds.isStorage("HOPPER")).isFalse();
        assertThat(StorageKinds.isStorage("ENDER_CHEST")).isFalse();
        assertThat(StorageKinds.isStorage("STONE")).isFalse();
    }

    @Test
    void aShulkerBoxItemIsRefusedByAShulkerBoxButNotByAnotherChest() {
        assertThat(StorageKinds.refuses("RED_SHULKER_BOX", ItemRef.material("SHULKER_BOX"))).isTrue();
        assertThat(StorageKinds.refuses("SHULKER_BOX", ItemRef.material("BLUE_SHULKER_BOX"))).isTrue();
        assertThat(StorageKinds.refuses("CHEST", ItemRef.material("RED_SHULKER_BOX"))).isFalse();
        assertThat(StorageKinds.refuses("BARREL", ItemRef.material("RED_SHULKER_BOX"))).isFalse();
    }

    @Test
    void aPlainItemAndAComponentAreNotRefusedByAShulkerBox() {
        assertThat(StorageKinds.refuses("SHULKER_BOX", ItemRef.material("COBBLESTONE"))).isFalse();
        assertThat(StorageKinds.refuses("SHULKER_BOX", ItemRef.item("SHULKER_BOX"))).isFalse();
    }

    @Test
    void aCrafterIsACrafterAndNotAChest() {
        assertThat(StorageKinds.isCrafter("CRAFTER")).isTrue();
        assertThat(StorageKinds.isStorage("CRAFTER")).isFalse();
        assertThat(StorageKinds.isCrafter("CHEST")).isFalse();
        assertThat(StorageKinds.isCrafter("CRAFTING_TABLE")).isFalse();
        assertThat(Material.matchMaterial("CRAFTER")).isNotNull();
    }

    @Test
    void theShulkerBoxKindIsOnlyTheShulkerBoxNames() {
        assertThat(StorageKinds.isShulkerBox("RED_SHULKER_BOX")).isTrue();
        assertThat(StorageKinds.isShulkerBox("CHEST")).isFalse();
        assertThat(StorageKinds.isShulkerBox("OXIDIZED_COPPER_CHEST")).isFalse();
    }
}
