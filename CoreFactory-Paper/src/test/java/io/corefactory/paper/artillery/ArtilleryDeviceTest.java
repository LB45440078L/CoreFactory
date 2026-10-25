package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryLevel;
import io.corefactory.core.energy.Pos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the reload window of a piece: it is busy for its reload after a shot, and never after none. */
class ArtilleryDeviceTest {

    private static final UUID OWNER = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID WORLD = UUID.fromString("66666666-7777-8888-9999-000000000000");

    private static ArtilleryDevice piece() {
        return new ArtilleryDevice("a", OWNER, WORLD, new Pos(4, 64, -2), ArtilleryLevel.I);
    }

    @Test
    void aPieceThatHasNotFiredIsNotReloading() {
        ArtilleryDevice piece = piece();
        assertThat(piece.reloadingAt(0L, 400)).isFalse();
        assertThat(piece.reloadingAt(Long.MAX_VALUE / 2, 400)).isFalse();
        assertThat(piece.hasAim()).isFalse();
    }

    @Test
    void aPieceReloadsForItsReloadInTicksFromItsLastShot() {
        ArtilleryDevice piece = piece();
        piece.setLastShotMillis(1_000L);
        long reloadMillis = 400L * ArtilleryDevice.MILLIS_PER_TICK;
        assertThat(piece.reloadingAt(1_000L + reloadMillis - 1, 400)).isTrue();
        assertThat(piece.reloadingAt(1_000L + reloadMillis, 400)).isFalse();
    }

    @Test
    void anAimIsKeptWithItsElevation() {
        ArtilleryDevice piece = piece();
        piece.setAim(120.5, -40.0, 45.0);
        assertThat(piece.hasAim()).isTrue();
        assertThat(piece.aimX()).isEqualTo(120.5);
        assertThat(piece.aimZ()).isEqualTo(-40.0);
        assertThat(piece.elevation()).isEqualTo(45.0);
    }
}
