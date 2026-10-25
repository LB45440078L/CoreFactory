package io.corefactory.paper.turret;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.turret.RoundType;
import io.corefactory.core.turret.TargetPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks that {@code turrets.yml} keeps the level, the player stances, the team mode and the kind, and reads older entries. */
class TurretStoreTest {

    private static final UUID OWNER = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID WORLD = UUID.fromString("66666666-7777-8888-9999-000000000000");
    private static final UUID FRIEND = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void theLevelPlayersTeamModeAndKindRoundTrip() {
        TargetPolicy policy = new TargetPolicy(true, Map.of("ZOMBIE", true), Map.of(FRIEND, TargetPolicy.PlayerStance.TARGET),
                TargetPolicy.TeamMode.TARGET);
        StoredTurret machine = new StoredTurret("a", TurretKind.MACHINE_GUN, OWNER, WORLD, new Pos(4, 64, -2), 90.0,
                RoundType.REGULAR, 10, 4, policy);
        StoredTurret laser = new StoredTurret("b", TurretKind.LASER, OWNER, WORLD, new Pos(0, 70, 0), 0.0, null, 0, 5,
                TargetPolicy.DEFAULT);

        List<StoredTurret> read = read(TurretStore.encode(List.of(machine, laser)));

        assertThat(read).hasSize(2);
        StoredTurret first = read.get(0);
        assertThat(first.kind()).isEqualTo(TurretKind.MACHINE_GUN);
        assertThat(first.level()).isEqualTo(4);
        assertThat(first.rounds()).isEqualTo(10);
        assertThat(first.policy().friendlyFire()).isTrue();
        assertThat(first.policy().overrides()).containsEntry("ZOMBIE", true);
        assertThat(first.policy().players()).containsEntry(FRIEND, TargetPolicy.PlayerStance.TARGET);
        assertThat(first.policy().teamMode()).isEqualTo(TargetPolicy.TeamMode.TARGET);
        StoredTurret second = read.get(1);
        assertThat(second.kind()).isEqualTo(TurretKind.LASER);
        assertThat(second.level()).isEqualTo(5);
        assertThat(second.round()).isNull();
    }

    @Test
    void aSpareStanceAndEveryTeamModeRoundTrip() {
        for (TargetPolicy.TeamMode mode : TargetPolicy.TeamMode.values()) {
            TargetPolicy policy = new TargetPolicy(false, Map.of(), Map.of(FRIEND, TargetPolicy.PlayerStance.SPARE), mode);
            StoredTurret turret = new StoredTurret("c", TurretKind.MACHINE_GUN, OWNER, WORLD, new Pos(1, 2, 3), 0.0, null, 0, 1,
                    policy);

            StoredTurret back = read(TurretStore.encode(List.of(turret))).get(0);

            assertThat(back.policy().teamMode()).isEqualTo(mode);
            assertThat(back.policy().players()).containsEntry(FRIEND, TargetPolicy.PlayerStance.SPARE);
        }
    }

    @Test
    void anEntryWrittenBeforeTheLevelsLoadsAtLevelOneWithTheDefaults() {
        String text = "version: 1\nturrets:\n  0:\n    id: old\n    owner: " + OWNER + "\n    world: " + WORLD
                + "\n    x: 1\n    y: 2\n    z: 3\n    facing: 0.0\n    rounds: 0\n    friendly-fire: false\n";

        List<StoredTurret> read = read(text);

        assertThat(read).hasSize(1);
        StoredTurret old = read.get(0);
        assertThat(old.kind()).isEqualTo(TurretKind.MACHINE_GUN);
        assertThat(old.level()).isEqualTo(1);
        assertThat(old.policy().players()).isEmpty();
        assertThat(old.policy().teamMode()).isEqualTo(TargetPolicy.TeamMode.SPARE);
    }

    @Test
    void aLaserTypeIsALaserAndAMissingLevelIsOne() {
        String text = "version: 1\nturrets:\n  0:\n    type: laser_turret\n    id: laser\n    owner: " + OWNER + "\n    world: " + WORLD
                + "\n    x: 0\n    y: 0\n    z: 0\n    facing: 0.0\n";

        StoredTurret laser = read(text).get(0);

        assertThat(laser.kind()).isEqualTo(TurretKind.LASER);
        assertThat(laser.level()).isEqualTo(1);
        assertThat(laser.round()).isNull();
    }

    @Test
    void aLevelOutsideOneToFiveIsClamped() {
        String text = "version: 1\nturrets:\n  0:\n    id: high\n    owner: " + OWNER + "\n    world: " + WORLD
                + "\n    x: 0\n    y: 0\n    z: 0\n    facing: 0.0\n    level: 9\n  1:\n    id: low\n    owner: " + OWNER
                + "\n    world: " + WORLD + "\n    x: 0\n    y: 0\n    z: 0\n    facing: 0.0\n    level: 0\n";

        List<StoredTurret> read = read(text);

        assertThat(read.get(0).level()).isEqualTo(5);
        assertThat(read.get(1).level()).isEqualTo(1);
    }

    @Test
    void aTeamModeThatIsNotKnownIsSpared() {
        String text = "version: 1\nturrets:\n  0:\n    id: t\n    owner: " + OWNER + "\n    world: " + WORLD
                + "\n    x: 0\n    y: 0\n    z: 0\n    facing: 0.0\n    team-mode: nobody\n";

        assertThat(read(text).get(0).policy().teamMode()).isEqualTo(TargetPolicy.TeamMode.SPARE);
    }

    @Test
    void aRailGunKeepsItsLevelAndItsSwitch() {
        StoredTurret off = new StoredTurret("r", TurretKind.RAILGUN, OWNER, WORLD, new Pos(2, 70, 2), 0.0, null, 0, 3,
                TargetPolicy.DEFAULT, false);
        StoredTurret on = new StoredTurret("s", TurretKind.RAILGUN, OWNER, WORLD, new Pos(5, 70, 2), 0.0, null, 0, 2,
                TargetPolicy.DEFAULT, true);

        List<StoredTurret> read = read(TurretStore.encode(List.of(off, on)));

        assertThat(read.get(0).kind()).isEqualTo(TurretKind.RAILGUN);
        assertThat(read.get(0).level()).isEqualTo(3);
        assertThat(read.get(0).on()).isFalse();
        assertThat(read.get(1).level()).isEqualTo(2);
        assertThat(read.get(1).on()).isTrue();
    }

    @Test
    void aRailGunWithoutASwitchKeyIsOnAndItsLevelIsCappedAtItsTop() {
        String text = "turrets:\n  0:\n    type: railgun\n    id: r\n    owner: " + OWNER + "\n    world: " + WORLD
                + "\n    x: 1\n    y: 70\n    z: 1\n    facing: 0.0\n    level: 5\n";

        StoredTurret rail = read(text).get(0);

        assertThat(rail.kind()).isEqualTo(TurretKind.RAILGUN);
        assertThat(rail.on()).isTrue();
        assertThat(rail.level()).isEqualTo(3);
    }

    private static List<StoredTurret> read(String text) {
        List<String> warnings = new ArrayList<>();
        List<StoredTurret> turrets = TurretStore.decode(text, warnings::add);
        assertThat(warnings).isEmpty();
        return turrets;
    }
}
