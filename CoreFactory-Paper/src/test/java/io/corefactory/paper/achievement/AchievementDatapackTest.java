package io.corefactory.paper.achievement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementDatapackTest {

    /** The built-in data pack metadata of Spigot 26.3, as the server ships it. */
    private static final String BUILT_IN = "{\n  \"features\": {\n    \"enabled\": [\n      \"minecraft:minecart_improvements\"\n    ]\n  },\n"
            + "  \"pack\": {\n    \"description\": {\n      \"translate\": \"dataPack.minecart_improvements.description\"\n    },\n"
            + "    \"max_format\": 121,\n    \"min_format\": 121\n  }\n}";

    /** The server's own bukkit data pack, as it is written in a world folder: its format numbers are decimals. */
    private static final String BUKKIT_PACK = "{\n    \"pack\": {\n        \"description\": \"Data pack for resources provided by Bukkit plugins\",\n"
            + "        \"min_format\": 121.0,\n        \"max_format\": 121.0\n    }\n}\n";

    @TempDir
    Path world;

    @Test
    void readsTheFormatRangeFromBuiltInMetadata() {
        assertThat(AchievementDatapack.number(BUILT_IN, "min_format")).contains(121);
        assertThat(AchievementDatapack.number(BUILT_IN, "max_format")).contains(121);
        assertThat(AchievementDatapack.number(BUILT_IN, "pack_format")).isEmpty();
    }

    @Test
    void readsDecimalFormatNumbersAsTheServerWritesThem() {
        assertThat(AchievementDatapack.number(BUKKIT_PACK, "min_format")).contains(121);
        assertThat(AchievementDatapack.number(BUKKIT_PACK, "max_format")).contains(121);
    }

    @Test
    void readsTheMajorNumberFromTheArrayFormSomeVersionsWrite() {
        String text = "{\"pack\": {\"min_format\": [101, 1], \"max_format\": [101, 1]}}";

        assertThat(AchievementDatapack.number(text, "min_format")).contains(101);
        assertThat(AchievementDatapack.number(text, "max_format")).contains(101);
    }

    @Test
    void theRootUsesTheCoreFactoryBackgroundNotAVanillaPath() {
        assertThat(AchievementDatapack.rootJson())
                .contains("\"background\":\"corefactory:textures/gui/advancements/background.png\"")
                .doesNotContain("minecraft:textures/block/tnt_side.png");
    }

    @Test
    void takesTheFormatFromTheWorldsBukkitDataPackFirst() throws Exception {
        Path bukkit = world.resolve("datapacks").resolve("bukkit");
        Files.createDirectories(bukkit);
        Files.writeString(bukkit.resolve("pack.mcmeta"), BUKKIT_PACK);

        assertThat(AchievementDatapack.dataFormat(world.toFile(), getClass().getClassLoader()))
                .contains(new AchievementDatapack.DataFormat(121, 121));
    }

    @Test
    void removeDeletesOnlyTheAchievementsDataPack() throws Exception {
        Path ours = world.resolve("datapacks").resolve(AchievementDatapack.FOLDER);
        Path bukkit = world.resolve("datapacks").resolve("bukkit");
        Files.createDirectories(ours);
        Files.createDirectories(bukkit);
        Files.writeString(bukkit.resolve("pack.mcmeta"), BUKKIT_PACK);

        AchievementDatapack.remove(world.toFile());

        assertThat(Files.exists(ours)).isFalse();
        assertThat(Files.exists(bukkit.resolve("pack.mcmeta"))).isTrue();
    }

    @Test
    void writesTheSameMinAndMaxFormatFormTheServerUses() {
        String mcmeta = new AchievementDatapack.DataFormat(121, 121).mcmeta();

        assertThat(mcmeta).contains("\"min_format\":121").contains("\"max_format\":121");
        assertThat(mcmeta).doesNotContain("supported_formats");
    }

    @Test
    void stringsAreEscapedForJson() {
        assertThat(Json.compact("say \"hi\"\\")).isEqualTo("\"say \\\"hi\\\"\\\\\"");
    }
}
