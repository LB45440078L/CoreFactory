package io.corefactory.core.placement;

import io.corefactory.api.PinLock;
import io.corefactory.core.config.TestConfig;
import io.corefactory.core.lock.PinHash;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlacedTntStoreLockTest {

    /** Writes the lock inside a placed entry as the YAML file holds it, then reads the text back. */
    private static Map<?, ?> roundTrip(PinLock lock) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("id", "nuke_little_boy");
        entry.put("lock", PlacedTntStore.encodeLock(lock));
        String text = new Yaml().dump(Map.of("placed", List.of(entry)));
        Map<String, Object> read = TestConfig.yaml(text);
        return (Map<?, ?>) ((Map<?, ?>) ((List<?>) read.get("placed")).get(0)).get("lock");
    }

    @Test
    void aLockRoundTripsWithItsSaltHashAndState() throws Exception {
        byte[] salt = PinHash.newSalt();
        PinLock lock = new PinLock(salt, PinHash.hash(salt, "4821"), true);

        PinLock back = PlacedTntStore.decodeLock(roundTrip(lock));

        assertThat(back.salt()).isEqualTo(lock.salt());
        assertThat(back.hash()).isEqualTo(lock.hash());
        assertThat(back.locked()).isTrue();
        assertThat(PinHash.matches(back.salt(), back.hash(), "4821")).isTrue();
    }

    @Test
    void theUnlockedFlagRoundTrips() throws Exception {
        byte[] salt = PinHash.newSalt();
        PinLock open = new PinLock(salt, PinHash.hash(salt, "4821"), false);

        assertThat(PlacedTntStore.decodeLock(roundTrip(open)).locked()).isFalse();
    }

    @Test
    void theStoredFormIsHexWithoutThePin() {
        byte[] salt = PinHash.newSalt();
        PinLock lock = new PinLock(salt, PinHash.hash(salt, "4821"), true);

        Map<String, Object> stored = PlacedTntStore.encodeLock(lock);

        assertThat(stored.get("salt")).isEqualTo(HexFormat.of().formatHex(salt));
        assertThat(stored.get("hash")).isEqualTo(HexFormat.of().formatHex(lock.hash()));
        assertThat(stored.get("unlocked")).isEqualTo(false);
        assertThat(stored.values()).doesNotContain("4821");
    }

    @Test
    void hexThatReadsLikeANumberSurvivesTheYamlFile() throws Exception {
        byte[] digitsOnly = new byte[16];
        for (int i = 0; i < digitsOnly.length; i++) {
            digitsOnly[i] = (byte) (0x10 * (i % 10) + (i + 1) % 10);
        }
        PinLock lock = new PinLock(digitsOnly, PinHash.hash(digitsOnly, "0000"), true);

        PinLock back = PlacedTntStore.decodeLock(roundTrip(lock));

        assertThat(back.salt()).isEqualTo(digitsOnly);
        assertThat(PinHash.matches(back.salt(), back.hash(), "0000")).isTrue();
    }

    @Test
    void aDamagedLockStaysLockedAndNoPinOpensIt() {
        PinLock damaged = PlacedTntStore.decodeLock(Map.of("salt", "zz", "hash", "not-hex", "unlocked", true));

        assertThat(damaged.locked()).isTrue();
        assertThat(damaged.hash()).isEmpty();
        assertThat(PinHash.matches(damaged.salt(), damaged.hash(), "0000")).isFalse();
    }

    @Test
    void aLockWithNoUnlockedFlagIsLocked() {
        Map<String, Object> entry = Map.of("salt", HexFormat.of().formatHex(PinHash.newSalt()),
                "hash", HexFormat.of().formatHex(new byte[32]));

        assertThat(PlacedTntStore.decodeLock(entry).locked()).isTrue();
    }
}
