package io.corefactory.spigot.command;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StyleTest {

    @Test
    void eachRoleHasItsColour() {
        assertThat(Style.TITLE.of("x")).isEqualTo(ChatColor.GOLD + "" + ChatColor.BOLD + "x" + ChatColor.RESET);
        assertThat(Style.COMMAND.of("x")).isEqualTo(ChatColor.AQUA + "x" + ChatColor.RESET);
        assertThat(Style.ARGUMENT.of("x")).isEqualTo(ChatColor.YELLOW + "x" + ChatColor.RESET);
        assertThat(Style.DESCRIPTION.of("x")).isEqualTo(ChatColor.GRAY + "x" + ChatColor.RESET);
        assertThat(Style.HINT.of("x")).isEqualTo(ChatColor.DARK_GRAY + "x" + ChatColor.RESET);
        assertThat(Style.SUCCESS.of("x")).isEqualTo(ChatColor.GREEN + "x" + ChatColor.RESET);
        assertThat(Style.ERROR.of("x")).isEqualTo(ChatColor.RED + "x" + ChatColor.RESET);
        assertThat(Style.WARNING.of("x")).isEqualTo(ChatColor.GOLD + "x" + ChatColor.RESET);
        assertThat(Style.ID.of("x")).isEqualTo(ChatColor.WHITE + "x" + ChatColor.RESET);
    }

    @Test
    void aUsageLineHasTheCommandArgumentsAndDescription() {
        assertThat(Style.usage("/cf give", "<id> [player] [amount]", "give a TNT"))
                .isEqualTo(ChatColor.AQUA + "/cf give" + ChatColor.RESET
                        + " " + ChatColor.YELLOW + "<id> [player] [amount]" + ChatColor.RESET
                        + " " + ChatColor.DARK_GRAY + "- " + ChatColor.RESET
                        + ChatColor.GRAY + "give a TNT" + ChatColor.RESET);
    }

    @Test
    void aUsageLineWithoutArgumentsHasNoPlaceholderSegment() {
        String line = Style.usage("/cf list", "", "every id");

        assertThat(line).isEqualTo(ChatColor.AQUA + "/cf list" + ChatColor.RESET
                + " " + ChatColor.DARK_GRAY + "- " + ChatColor.RESET + ChatColor.GRAY + "every id" + ChatColor.RESET);
    }

    @Test
    void aUsageErrorStartsRedAndNamesTheCommand() {
        assertThat(Style.usageError("/cf pylons show", "[seconds]"))
                .isEqualTo(ChatColor.RED + "Usage: " + ChatColor.RESET + ChatColor.AQUA + "/cf pylons show"
                        + ChatColor.RESET + " " + ChatColor.YELLOW + "[seconds]" + ChatColor.RESET);
    }

    @Test
    void theStrippedTextOfAUsageLineIsPlain() {
        String line = Style.usage("/cf research grant", "<node> [player]", "finish a node");

        assertThat(ChatColor.stripColor(line))
                .isEqualTo("/cf research grant <node> [player] - finish a node");
    }
}
