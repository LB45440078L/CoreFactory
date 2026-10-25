package io.corefactory.paper.turret;

import io.corefactory.core.turret.TurretAim;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** The displays of a rail gun: the body and the top turn to the aim, the barrel tilts about its trunnion, and the screen turns with the body. */
class RailgunPoseTest {

    /** The model's front faces -z, which the barrel's muzzle is on. */
    private static final Vector3f FRONT = new Vector3f(0f, 0f, -1f);

    @Test
    void theBodyFacesTheAimInTheGamesConvention() {
        for (double yaw : new double[] {0, 45, 90, 180, 270}) {
            Transformation body = RailgunPose.body(yaw, 1f);
            Vector3f facing = new Quaternionf(body.getLeftRotation()).transform(new Vector3f(FRONT));
            double[] expected = TurretAim.direction(yaw, 0.0);

            assertThat(facing.x).as("yaw %s x", yaw).isCloseTo((float) expected[0], within(1e-5f));
            assertThat(facing.y).as("yaw %s y", yaw).isCloseTo(0f, within(1e-5f));
            assertThat(facing.z).as("yaw %s z", yaw).isCloseTo((float) expected[2], within(1e-5f));
        }
    }

    @Test
    void theBodyHasNoTranslationSoItTurnsAboutItsCentre() {
        Transformation body = RailgunPose.body(120.0, 1f);

        assertThat(body.getTranslation().x).isZero();
        assertThat(body.getTranslation().y).isZero();
        assertThat(body.getTranslation().z).isZero();
    }

    @Test
    void theBarrelMuzzleFollowsTheAimOfTheTurret() {
        for (double yaw : new double[] {0, 30, 150, 300}) {
            for (double pitch : new double[] {-20, 0, 35}) {
                Transformation barrel = RailgunPose.barrel(2, yaw, pitch, 1f);
                Vector3f muzzle = new Quaternionf(barrel.getLeftRotation()).transform(new Vector3f(FRONT));
                double[] expected = TurretAim.direction(yaw, pitch);

                assertThat(muzzle.x).as("yaw %s pitch %s x", yaw, pitch).isCloseTo((float) expected[0], within(1e-5f));
                assertThat(muzzle.y).as("yaw %s pitch %s y", yaw, pitch).isCloseTo((float) expected[1], within(1e-5f));
                assertThat(muzzle.z).as("yaw %s pitch %s z", yaw, pitch).isCloseTo((float) expected[2], within(1e-5f));
            }
        }
    }

    @Test
    void theTrunnionStaysWhereItRestsWhenTheBarrelTurnsAndTilts() {
        for (int level = 1; level <= 3; level++) {
            Transformation barrel = RailgunPose.barrel(level, 75.0, 40.0, 1f);
            Vector3f pivot = new Vector3f(0f, (float) (RailgunPose.trunnionBlocks(level) - RailgunPose.ORIGIN_BLOCKS), 0f);
            Vector3f moved = new Quaternionf(barrel.getLeftRotation()).transform(new Vector3f(pivot));
            Vector3f translation = barrel.getTranslation();

            assertThat(translation.x + moved.x).isCloseTo(pivot.x, within(1e-5f));
            assertThat(translation.y + moved.y).isCloseTo(pivot.y, within(1e-5f));
            assertThat(translation.z + moved.z).isCloseTo(pivot.z, within(1e-5f));
        }
    }

    @Test
    void theTrunnionsAreTheResourcePacksHeights() {
        assertThat(RailgunPose.trunnionBlocks(1)).isEqualTo(0.75);
        assertThat(RailgunPose.trunnionBlocks(2)).isEqualTo(1.25);
        assertThat(RailgunPose.trunnionBlocks(3)).isEqualTo(1.625);
    }

    @Test
    void theTopOfLevelThreeStandsTwoBlocksAboveTheBody() {
        assertThat(RailgunPose.LIFT_BLOCKS + RailgunPose.TOP_OFFSET_BLOCKS).isEqualTo(3.0);
    }

    @Test
    void theScreenStandsOnTheFrontOfTheBodyAndFacesIt() {
        // At yaw 180 the body faces north, so the screen stands north of the centre, 0.38 blocks out, and its text faces north.
        Transformation facingNorth = RailgunPose.screen(180.0, 1f);
        assertThat(facingNorth.getTranslation().x).isCloseTo(0f, within(1e-5f));
        assertThat(facingNorth.getTranslation().z).isCloseTo(-0.38f, within(1e-5f));
        Vector3f textFront = new Quaternionf(facingNorth.getLeftRotation()).transform(new Vector3f(0f, 0f, 1f));
        assertThat(textFront.z).isCloseTo(-1f, within(1e-5f));

        // At yaw 0 the body faces south, so the screen moves to the south side and its text turns with it.
        Transformation facingSouth = RailgunPose.screen(0.0, 1f);
        assertThat(facingSouth.getTranslation().z).isCloseTo(0.38f, within(1e-5f));
        Vector3f southText = new Quaternionf(facingSouth.getLeftRotation()).transform(new Vector3f(0f, 0f, 1f));
        assertThat(southText.z).isCloseTo(1f, within(1e-5f));
    }

    @Test
    void theScreenCentreIsThreeLevelUnitsUpFromTheBlockFloor() {
        // The screen spans level units 3 to 9, so its centre is 6 units, which is 0.375 blocks, above the floor. The display's origin is 1.5
        // blocks above the floor, so the screen sits 1.125 blocks below the origin.
        Transformation facingNorth = RailgunPose.screen(180.0, 1f);
        assertThat(RailgunPose.ORIGIN_BLOCKS + facingNorth.getTranslation().y).isCloseTo(0.375, within(1e-5));
    }

    @Test
    void theTextFillsTheScreenWidthAtTheCommandCentersScale() {
        assertThat(RailgunPose.screenWidth()).isCloseTo(0.4375, within(1e-9));
        assertThat(RailgunPose.textScale()).isCloseTo(0.0875, within(1e-9));
        assertThat(RailgunPose.LINE_WIDTH * RailgunPose.PIXEL_BLOCKS * RailgunPose.textScale())
                .isCloseTo(RailgunPose.screenWidth(), within(1e-9));
    }
}
