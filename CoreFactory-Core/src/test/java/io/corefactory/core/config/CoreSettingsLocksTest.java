package io.corefactory.core.config;

import io.corefactory.core.text.Text;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoreSettingsLocksTest {

    @Test
    void theLockSettingsHaveTheirDocumentedDefaults() {
        CoreSettings settings = CoreSettings.from(Map.of());

        assertThat(settings.lockMaxAttempts()).isEqualTo(3);
        assertThat(settings.lockoutSeconds()).isEqualTo(30);
    }

    @Test
    void theLockSettingsAreReadFromTheLocksSection() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "locks.max-attempts", 5);
        TestConfig.set(config, "locks.lockout-seconds", 60);

        CoreSettings settings = CoreSettings.from(config);

        assertThat(settings.lockMaxAttempts()).isEqualTo(5);
        assertThat(settings.lockoutSeconds()).isEqualTo(60);
    }

    @Test
    void anAttemptLimitBelowOneIsRaisedToOne() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "locks.max-attempts", 0);

        assertThat(CoreSettings.from(config).lockMaxAttempts()).isEqualTo(1);
    }

    @Test
    void theLockedMessageHasTheWordingOfTheBrief() {
        String text = Text.plain(new Messages(null).raw("locked-tnt"));

        assertThat(text).isEqualTo("This nuclear TNT is locked.");
    }
}
