package io.corefactory.paper.achievement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementJsonTest {

    /** A title with quotes and a non-ASCII letter, which must survive the trip into the file and back. */
    private static final String TITLE = "Say \"hi\" to café";

    private static final String DESCRIPTION = "Forge \\ a \"thing\"";

    @TempDir
    Path world;

    @Test
    void writesThePackMetaAndAnAdvancementThatParseAsJson() throws Exception {
        AchievementDatapack.DataFormat format = new AchievementDatapack.DataFormat(121, 121);
        AchievementDatapack.writePackMeta(world.toFile(), format);

        Path pack = world.resolve("datapacks").resolve(AchievementDatapack.FOLDER);
        Path advancement = pack.resolve("data").resolve(AchievementDatapack.NAMESPACE).resolve("advancement")
                .resolve("achievements").resolve("say_hi.json");
        Files.createDirectories(advancement.getParent());
        Map<String, Object> json = AchievementDatapack.achievementAdvancement(Achievement.Category.FORGING, TITLE,
                DESCRIPTION, false, Json.object("id", "minecraft:anvil"), List.of("step_1"));
        Files.writeString(advancement, Json.pretty(json) + "\n", StandardCharsets.UTF_8);

        JsonObject packMeta = parse(pack.resolve("pack.mcmeta")).getAsJsonObject("pack");
        assertThat(packMeta.get("min_format").getAsInt()).isEqualTo(121);
        assertThat(packMeta.get("max_format").getAsInt()).isEqualTo(121);
        assertThat(packMeta.get("description").getAsString()).startsWith("CoreFactory");

        JsonObject achievement = parse(advancement);
        assertThat(achievement.get("parent").getAsString()).isEqualTo("corefactory:category/forging");
        JsonObject display = achievement.getAsJsonObject("display");
        assertThat(display.getAsJsonObject("title").get("text").getAsString()).isEqualTo(TITLE);
        assertThat(display.getAsJsonObject("description").get("text").getAsString()).isEqualTo(DESCRIPTION);
        assertThat(display.getAsJsonObject("icon").get("id").getAsString()).isEqualTo("minecraft:anvil");
        assertThat(display.get("frame").getAsString()).isEqualTo("task");
        assertThat(achievement.getAsJsonObject("criteria").keySet()).containsExactly("step_1");
    }

    @Test
    void theTitleIsWrittenWithItsQuotesEscaped() throws Exception {
        Map<String, Object> json = AchievementDatapack.achievementAdvancement(Achievement.Category.FORGING, TITLE,
                DESCRIPTION, true, Json.object("id", "minecraft:anvil"), List.of("step_1"));

        String text = Json.compact(json);

        assertThat(text).contains("\"text\":\"Say \\\"hi\\\" to café\"");
        assertThat(text).contains("\"frame\":\"challenge\"");
    }

    @Test
    void escapesQuotesBackslashesAndControlCharacters() {
        assertThat(Json.compact("a \"b\" \\ c")).isEqualTo("\"a \\\"b\\\" \\\\ c\"");
        assertThat(Json.compact("line\nthree\tfour\u0001")).isEqualTo("\"line\\nthree\\tfour\\u0001\"");
    }

    @Test
    void writesNonAsciiCharactersAsUtf8() {
        assertThat(Json.compact("café")).isEqualTo("\"café\"");
    }

    @Test
    void writesHtmlCharactersAsTyped() {
        assertThat(Json.compact("<b> & 'x'")).isEqualTo("\"<b> & 'x'\"");
    }

    @Test
    void objectsKeepTheOrderTheyAreGiven() {
        assertThat(Json.compact(Json.object("b", 1, "a", true))).isEqualTo("{\"b\":1,\"a\":true}");
    }

    private static JsonObject parse(Path file) throws Exception {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
