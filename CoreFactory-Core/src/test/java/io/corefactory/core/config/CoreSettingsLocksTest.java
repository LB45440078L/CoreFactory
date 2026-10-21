package io.corefactory.core.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoreSettingsLocksTest {

    @Test
    void theLockSettingsHaveTheirDocumentedDefaults() {
        CoreSettings settings = CoreSettings.from(new MemoryConfiguration());

        assertThat(settings.lockMaxAttempts()).isEqualTo(3);
        assertThat(settings.lockoutSeconds()).isEqualTo(30);
    }

    @Test
    void theLockSettingsAreReadFromTheLocksSection() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("locks.max-attempts", 5);
        config.set("locks.lockout-seconds", 60);

        CoreSettings settings = CoreSettings.from(config);

        assertThat(settings.lockMaxAttempts()).isEqualTo(5);
        assertThat(settings.lockoutSeconds()).isEqualTo(60);
    }

    @Test
    void anAttemptLimitBelowOneIsRaisedToOne() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("locks.max-attempts", 0);

        assertThat(CoreSettings.from(config).lockMaxAttempts()).isEqualTo(1);
    }

    @Test
    void theLockedMessageHasTheWordingOfTheBrief() {
        String text = ChatColor.stripColor(new Messages(null).raw("locked-tnt"));

        assertThat(text).isEqualTo("This nuclear TNT is locked.");
    }
}
