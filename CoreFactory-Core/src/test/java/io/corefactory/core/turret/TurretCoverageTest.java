package io.corefactory.core.turret;

import io.corefactory.core.turret.TargetRules.Candidate;
import io.corefactory.core.turret.TargetRules.Point;
import io.corefactory.core.turret.TargetRules.Relation;
import io.corefactory.core.turret.TargetRules.Verdict;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the coverage the owner asked for: a turret fires at every hostile target inside its spherical range, in every direction (all 360
 * degrees of yaw and the full pitch range), with nothing refused by the arc or the tilt limit, and refuses what it must.
 *
 * <p>The verdict is computed the way the Paper side computes it: the aim from the turret's head to the target's box centre gives the yaw
 * and pitch, {@link TurretAim#inReach} gives the arc and tilt verdict against the turret's facing, and {@link TargetRules#verdict} takes the
 * sphere from the head to the nearest point of the target's box. Pure: no world, no server.
 */
class TurretCoverageTest {

    /** The turret's head: the point it aims and fires from, above the centre of its block. */
    private static final Point HEAD = new Point(10.5, 65.75, -3.5);

    /** The half size of a zombie's box, width 0.6 and height 1.95. */
    private static final double HALF_WIDTH = 0.3;
    private static final double HALF_HEIGHT = 0.975;

    /** The facings a turret may have been placed with: a player's yaw at placement, including the seam at plus or minus 180. */
    private static final double[] FACINGS = {0.0, 123.0, -170.0};

    /** Every 15-degree yaw step. */
    private static final int YAW_STEPS = 24;

    /** The pitches the sweep covers, from straight up to straight down. */
    private static final double[] PITCHES = {-90, -75, -60, -45, -30, -15, 0, 15, 30, 45, 60, 75, 90};

    /** The turrets whose settings the sweep runs: each machine gun level's range, and the laser's. */
    private static final List<TurretSettings> TURRETS = turrets();

    @Test
    void theShippedDefaultArcAndPitchAllowTheWholeSphere() {
        TurretSettings settings = TurretSettings.defaults();

        assertThat(settings.arcDegrees()).isEqualTo(360.0);
        assertThat(settings.pitchLimitDegrees()).isEqualTo(90.0);
    }

    @Test
    void aZombieInsideTheSphereIsValidInEveryDirection() {
        List<String> refused = new ArrayList<>();
        for (TurretSettings settings : TURRETS) {
            for (double facing : FACINGS) {
                for (int step = 0; step < YAW_STEPS; step++) {
                    double yaw = step * 360.0 / YAW_STEPS;
                    for (double pitch : PITCHES) {
                        for (double distance : insideDistances(settings)) {
                            Verdict verdict = verdictOf(zombieAt(yaw, pitch, distance), facing, settings);
                            if (verdict != Verdict.VALID) {
                                refused.add(String.format("range %.0f facing %.0f yaw %.0f pitch %.0f distance %.1f: %s",
                                        settings.range(), facing, yaw, pitch, distance, verdict));
                            }
                        }
                    }
                }
            }
        }
        assertThat(refused).as("zombies inside the sphere that the turret refuses").isEmpty();
    }

    @Test
    void aZombieBehindTheTurretIsAValidTarget() {
        TurretSettings settings = TurretSettings.defaults();

        assertThat(verdictOf(zombieAt(180.0, 0.0, 10.0), 0.0, settings)).isEqualTo(Verdict.VALID);
        assertThat(verdictOf(zombieAt(180.0, 0.0, 10.0), 90.0, settings)).isEqualTo(Verdict.VALID);
    }

    @Test
    void aZombieStraightAboveOrBelowTheTurretIsAValidTarget() {
        TurretSettings settings = TurretSettings.defaults();

        assertThat(verdictOf(zombieAt(0.0, 90.0, 4.0), 0.0, settings)).isEqualTo(Verdict.VALID);
        assertThat(verdictOf(zombieAt(0.0, -90.0, 4.0), 0.0, settings)).isEqualTo(Verdict.VALID);
    }

    @Test
    void aZombieWhoseNearEdgeIsInsideTheSphereIsValidEvenIfItsCentreIsNot() {
        TurretSettings settings = TurretSettings.defaults();
        double centreBeyondRange = settings.range() + 0.2;

        for (int step = 0; step < YAW_STEPS; step++) {
            double yaw = step * 360.0 / YAW_STEPS;
            assertThat(verdictOf(zombieAt(yaw, 0.0, centreBeyondRange), 0.0, settings))
                    .as("yaw %s", yaw)
                    .isEqualTo(Verdict.VALID);
        }
    }

    @Test
    void aZombieJustOutsideTheSphereIsOutOfRangeInEveryDirection() {
        List<String> wrong = new ArrayList<>();
        for (TurretSettings settings : TURRETS) {
            for (int step = 0; step < YAW_STEPS; step++) {
                double yaw = step * 360.0 / YAW_STEPS;
                for (double pitch : PITCHES) {
                    Verdict verdict = verdictOf(zombieAt(yaw, pitch, settings.range() + 1.5), 0.0, settings);
                    if (verdict != Verdict.OUT_OF_RANGE) {
                        wrong.add(String.format("range %.0f yaw %.0f pitch %.0f: %s", settings.range(), yaw, pitch, verdict));
                    }
                }
            }
        }
        assertThat(wrong).isEmpty();
    }

    @Test
    void aPlayerOrAVillagerOrAGolemOrAnAnimalIsNeverAValidTarget() {
        TurretSettings settings = TurretSettings.defaults();
        UUID stranger = UUID.randomUUID();

        assertThat(TargetRules.verdict(player(Relation.OTHER, stranger), settings, TargetPolicy.DEFAULT.withFriendlyFire(true),
                HEAD, pointAt(5.0))).isEqualTo(Verdict.VALID);
        assertThat(TargetRules.verdict(player(Relation.OTHER, stranger), settings, TargetPolicy.DEFAULT,
                HEAD, pointAt(5.0))).isEqualTo(Verdict.PLAYER);
        assertThat(TargetRules.verdict(villager(), settings, TargetPolicy.DEFAULT, HEAD, pointAt(5.0)))
                .isEqualTo(Verdict.VILLAGER);
        assertThat(TargetRules.verdict(golem(), settings, TargetPolicy.DEFAULT, HEAD, pointAt(5.0)))
                .isEqualTo(Verdict.GOLEM);
        assertThat(TargetRules.verdict(animal(), settings, TargetPolicy.DEFAULT, HEAD, pointAt(5.0)))
                .isEqualTo(Verdict.ANIMAL);
    }

    @Test
    void aFriendOrTheOwnerIsSpared() {
        TurretSettings settings = TurretSettings.defaults();
        UUID friend = UUID.randomUUID();

        assertThat(TargetRules.verdict(player(Relation.FRIEND, friend), settings, TargetPolicy.DEFAULT.withFriendlyFire(true),
                HEAD, pointAt(5.0))).isEqualTo(Verdict.FRIEND);
        assertThat(TargetRules.verdict(player(Relation.OWNER, UUID.randomUUID()), settings, TargetPolicy.DEFAULT.withFriendlyFire(true),
                HEAD, pointAt(5.0))).isEqualTo(Verdict.OWNER);
    }

    @Test
    void aZombieBehindASolidBlockIsRefusedInEveryDirection() {
        List<String> fired = new ArrayList<>();
        for (TurretSettings settings : TURRETS) {
            for (int step = 0; step < YAW_STEPS; step++) {
                double yaw = step * 360.0 / YAW_STEPS;
                for (double pitch : PITCHES) {
                    Verdict verdict = verdictOf(zombieAt(yaw, pitch, 6.0), 0.0, settings, false);
                    if (verdict != Verdict.BLOCKED) {
                        fired.add(String.format("range %.0f yaw %.0f pitch %.0f: %s", settings.range(), yaw, pitch, verdict));
                    }
                }
            }
        }
        assertThat(fired).as("zombies behind a solid block that the turret does not refuse").isEmpty();
    }

    @Test
    void aBlockedZombieIsRefusedEvenWhenItIsInRangeAndInArc() {
        assertThat(verdictOf(zombieAt(0.0, 0.0, 6.0), 0.0, TurretSettings.defaults(), false)).isEqualTo(Verdict.BLOCKED);
        assertThat(verdictOf(zombieAt(0.0, 0.0, 6.0), 0.0, TurretSettings.defaults(), true)).isEqualTo(Verdict.VALID);
    }

    @Test
    void aTamedOrNamedMobIsSparedByTheShippedSettings() {
        TurretSettings settings = TurretSettings.defaults();
        Candidate tamed = new Candidate(TargetKind.MONSTER, false, false, false, false, true, false, false, 5.0, true, "ZOMBIE",
                Relation.NONE);
        Candidate named = new Candidate(TargetKind.MONSTER, false, false, false, false, false, true, false, 5.0, true, "ZOMBIE",
                Relation.NONE);

        assertThat(TargetRules.verdict(tamed, settings, TargetPolicy.DEFAULT, HEAD, pointAt(5.0))).isEqualTo(Verdict.TAMED);
        assertThat(TargetRules.verdict(named, settings, TargetPolicy.DEFAULT, HEAD, pointAt(5.0))).isEqualTo(Verdict.NAMED);
    }

    /** The distances the sweep covers inside the range: near, middle, and just inside the edge. */
    private static double[] insideDistances(TurretSettings settings) {
        return new double[] {2.0, 5.0, 10.0, 18.0, settings.range() - 1.0};
    }

    /** The zombie whose box centre stands at a yaw, a pitch and a distance from the turret's head. */
    private static Placed zombieAt(double yaw, double pitch, double distance) {
        double[] d = TurretAim.direction(yaw, pitch);
        Point centre = new Point(HEAD.x() + d[0] * distance, HEAD.y() + d[1] * distance, HEAD.z() + d[2] * distance);
        return new Placed(centre, new Point(centre.x() - HALF_WIDTH, centre.y() - HALF_HEIGHT, centre.z() - HALF_WIDTH),
                new Point(centre.x() + HALF_WIDTH, centre.y() + HALF_HEIGHT, centre.z() + HALF_WIDTH));
    }

    /** The verdict for a zombie with a clear line of sight: the arc and tilt from the aim to its centre, and the sphere to its box. */
    private static Verdict verdictOf(Placed mob, double facing, TurretSettings settings) {
        return verdictOf(mob, facing, settings, true);
    }

    /**
     * The verdict the Paper side reaches for a zombie. The aim from the head to the box centre gives the arc and tilt, the sphere runs from
     * the head to the nearest point of the box, and {@code clearShot} is what the line of sight from the head to the centre reports.
     */
    private static Verdict verdictOf(Placed mob, double facing, TurretSettings settings, boolean clearShot) {
        double dx = mob.centre().x() - HEAD.x();
        double dy = mob.centre().y() - HEAD.y();
        double dz = mob.centre().z() - HEAD.z();
        double yaw = TurretAim.yawOf(dx, dz);
        double pitch = TurretAim.pitchOf(dx, dy, dz);
        boolean withinArc = TurretAim.inReach(yaw, pitch, facing, settings);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        Candidate zombie = new Candidate(TargetKind.MONSTER, false, false, false, false, false, false, false, distance, withinArc,
                "ZOMBIE", Relation.NONE, null, false, clearShot);
        Point nearest = TargetRules.nearestPoint(HEAD, mob.min(), mob.max());
        return TargetRules.verdict(zombie, settings, TargetPolicy.DEFAULT, HEAD, nearest);
    }

    private static Point pointAt(double distance) {
        return new Point(HEAD.x() + distance, HEAD.y(), HEAD.z());
    }

    private static Candidate player(Relation relation, UUID id) {
        return new Candidate(TargetKind.MONSTER, true, false, false, false, false, false, false, 5.0, true, "PLAYER", relation, id,
                false, true);
    }

    private static Candidate villager() {
        return new Candidate(TargetKind.OTHER, false, true, false, false, false, false, false, 5.0, true, "VILLAGER", Relation.NONE);
    }

    private static Candidate golem() {
        return new Candidate(TargetKind.OTHER, false, false, true, false, false, false, false, 5.0, true, "IRON_GOLEM", Relation.NONE);
    }

    private static Candidate animal() {
        return new Candidate(TargetKind.OTHER, false, false, false, true, false, false, false, 5.0, true, "COW", Relation.NONE);
    }

    /** The shipped machine gun settings at each level, and the laser's, with the default arc and tilt. */
    private static List<TurretSettings> turrets() {
        TurretSettings defaults = TurretSettings.defaults();
        List<TurretSettings> list = new ArrayList<>();
        for (int level = 1; level <= TurretLevels.MAX_LEVEL; level++) {
            list.add(withRange(defaults, TurretLevels.MACHINE_GUN.level(level).range()));
        }
        list.add(LaserSettings.defaults().asTurret(defaults));
        return List.copyOf(list);
    }

    private static TurretSettings withRange(TurretSettings d, double range) {
        return new TurretSettings(d.enabled(), range, d.searchIntervalTicks(), d.firingIntervalTicks(), d.rotationSpeedDegrees(),
                d.arcDegrees(), d.pitchLimitDegrees(), d.idleSweep(), d.accuracySpread(), d.ammoCapacity(), d.ammoPerShot(),
                d.powerDemandW(), d.targets(), d.targetNamed(), d.targetTamed(), d.rounds());
    }

    /** A mob placed in the world: its box centre, and its box corners. */
    private record Placed(Point centre, Point min, Point max) {
    }
}
