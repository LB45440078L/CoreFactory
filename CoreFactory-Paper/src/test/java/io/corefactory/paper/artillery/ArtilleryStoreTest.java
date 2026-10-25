package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryLevel;
import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ChargeTier;
import io.corefactory.core.energy.Pos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that {@code artillery.yml} keeps a piece's state and name, a Command Center's settings, the antimatter times, and skips an entry it
 * cannot read.
 */
class ArtilleryStoreTest {

    private static final UUID OWNER = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID WORLD = UUID.fromString("66666666-7777-8888-9999-000000000000");

    @Test
    void thePieceAndTheAntimatterTimesRoundTrip() {
        ArtilleryDevice loaded = new ArtilleryDevice("a", OWNER, WORLD, new Pos(4, 64, -2), ArtilleryLevel.VII);
        loaded.setOn(true);
        loaded.setName("North battery");
        loaded.setShell(ArtilleryShell.CONDENSED_ANTIMATTER);
        loaded.setTier(ChargeTier.TIER_III);
        loaded.setAim(1200.25, -900.5, 60.0);
        loaded.setLastShotMillis(5_000_000L);
        loaded.setMagazine(3, 2);
        ArtilleryDevice bare = new ArtilleryDevice("b", OWNER, WORLD, new Pos(0, 70, 0), ArtilleryLevel.I);
        Map<UUID, Long> antimatter = Map.of(OWNER, 42_000L);

        String text = ArtilleryStore.encode(new ArtilleryStore.Saved(List.of(loaded, bare), List.of(), antimatter));
        ArtilleryStore.Saved read = ArtilleryStore.decode(text, message -> { });

        assertThat(read.pieces()).hasSize(2);
        ArtilleryDevice back = read.pieces().get(0);
        assertThat(back.id()).isEqualTo("a");
        assertThat(back.owner()).isEqualTo(OWNER);
        assertThat(back.world()).isEqualTo(WORLD);
        assertThat(back.pos()).isEqualTo(new Pos(4, 64, -2));
        assertThat(back.level()).isEqualTo(ArtilleryLevel.VII);
        assertThat(back.name()).isEqualTo("North battery");
        assertThat(back.on()).isTrue();
        assertThat(back.shell()).isEqualTo(ArtilleryShell.CONDENSED_ANTIMATTER);
        assertThat(back.tier()).isEqualTo(ChargeTier.TIER_III);
        assertThat(back.aimX()).isEqualTo(1200.25);
        assertThat(back.aimZ()).isEqualTo(-900.5);
        assertThat(back.elevation()).isEqualTo(60.0);
        assertThat(back.bodyYaw()).isEqualTo(loaded.bodyYaw());
        assertThat(back.barrelPitch()).isEqualTo(60.0);
        assertThat(back.lastShotMillis()).isEqualTo(5_000_000L);
        assertThat(back.magazineShells()).isEqualTo(3);
        assertThat(back.magazineCharges()).isEqualTo(2);
        ArtilleryDevice plain = read.pieces().get(1);
        assertThat(plain.magazineShells()).isZero();
        assertThat(plain.magazineCharges()).isZero();
        assertThat(plain.shell()).isNull();
        assertThat(plain.tier()).isEqualTo(ChargeTier.TIER_I);
        assertThat(plain.hasAim()).isFalse();
        assertThat(plain.lastShotMillis()).isEqualTo(ArtilleryDevice.NEVER);
        assertThat(read.antimatterShots()).containsEntry(OWNER, 42_000L);
        assertThat(plain.name()).isEqualTo(ArtilleryDevice.DEFAULT_NAME);
    }

    @Test
    void aPieceWithNoShellHasAnEmptyMagazine() {
        ArtilleryDevice noShell = new ArtilleryDevice("n", OWNER, WORLD, new Pos(1, 64, 1), ArtilleryLevel.II);
        noShell.setMagazine(4, 4);

        String text = ArtilleryStore.encode(new ArtilleryStore.Saved(List.of(noShell), List.of(), Map.of()));
        ArtilleryDevice back = ArtilleryStore.decode(text, message -> { }).pieces().get(0);

        assertThat(back.shell()).isNull();
        assertThat(back.magazineShells()).isZero();
        assertThat(back.magazineCharges()).isZero();
    }

    @Test
    void aCommandCenterKeepsItsSettingsAndItsDefaults() {
        CommandCenter set = new CommandCenter("c", OWNER, WORLD, new Pos(8, 70, 8));
        set.setName("Hill post");
        set.setLoad(ArtilleryShell.CLUSTER, ChargeTier.TIER_II);
        set.setAim(640.5, -120.0);
        set.setElevation(60.0);
        CommandCenter bare = new CommandCenter("d", OWNER, WORLD, new Pos(0, 70, 0));

        String text = ArtilleryStore.encode(new ArtilleryStore.Saved(List.of(), List.of(set, bare), Map.of()));
        ArtilleryStore.Saved read = ArtilleryStore.decode(text, message -> { });

        assertThat(read.centers()).hasSize(2);
        CommandCenter back = read.centers().get(0);
        assertThat(back.id()).isEqualTo("c");
        assertThat(back.owner()).isEqualTo(OWNER);
        assertThat(back.pos()).isEqualTo(new Pos(8, 70, 8));
        assertThat(back.name()).isEqualTo("Hill post");
        assertThat(back.shell()).isEqualTo(ArtilleryShell.CLUSTER);
        assertThat(back.tier()).isEqualTo(ChargeTier.TIER_II);
        assertThat(back.aimX()).isEqualTo(640.5);
        assertThat(back.aimZ()).isEqualTo(-120.0);
        assertThat(back.elevation()).isEqualTo(60.0);
        CommandCenter plain = read.centers().get(1);
        assertThat(plain.shell()).isNull();
        assertThat(plain.tier()).isEqualTo(ChargeTier.TIER_I);
        assertThat(plain.hasAim()).isFalse();
        assertThat(plain.elevation()).isEqualTo(CommandCenter.DEFAULT_ELEVATION);
        assertThat(plain.name()).isEqualTo(CommandCenter.DEFAULT_NAME);
    }

    @Test
    void anEntryThatCannotBeReadIsSkippedAndReported() {
        String text = "pieces:\n"
                + "  0:\n    id: a\n    owner: not-a-uuid\n    world: " + WORLD + "\n    level: 2\n"
                + "  1:\n    id: b\n    owner: " + OWNER + "\n    world: " + WORLD + "\n    level: 9\n"
                + "  2:\n    id: c\n    owner: " + OWNER + "\n    world: " + WORLD + "\n    level: 3\n";
        List<String> warnings = new ArrayList<>();
        ArtilleryStore.Saved read = ArtilleryStore.decode(text, warnings::add);
        assertThat(read.pieces()).extracting(ArtilleryDevice::id).containsExactly("c");
        assertThat(warnings).hasSize(2);
    }
}
