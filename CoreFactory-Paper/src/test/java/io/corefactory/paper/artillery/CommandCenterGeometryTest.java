package io.corefactory.paper.artillery;

import io.corefactory.resourcepack.art.ArtilleryArt;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Checks the screen's place on the command center's block: its size, its plane, the text's scale, and the click target around it. */
class CommandCenterGeometryTest {

    @Test
    void theScreenIsSevenByFiveAndAQuarterUnitsOfTheModelOnItsPlane() {
        CommandCenterGeometry.Screen screen = CommandCenterGeometry.screen();
        assertThat(screen.width()).isCloseTo(7.0 / 16, within(1e-9));
        assertThat(screen.height()).isCloseTo(5.25 / 16, within(1e-9));
        assertThat(screen.z()).isCloseTo(2.0 / 16, within(1e-9));
        assertThat(screen.centreX()).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void theScreenMatchesTheResourcePackArt() {
        double[] art = ArtilleryArt.SCREEN;
        CommandCenterGeometry.Screen screen = CommandCenterGeometry.screen();
        assertThat(screen.x0()).isCloseTo(art[0] / 16, within(1e-9));
        assertThat(screen.y0()).isCloseTo(art[1] / 16, within(1e-9));
        assertThat(screen.x1()).isCloseTo(art[2] / 16, within(1e-9));
        assertThat(screen.y1()).isCloseTo(art[3] / 16, within(1e-9));
        assertThat(screen.z()).isCloseTo(art[4] / 16, within(1e-9));
    }

    @Test
    void theTextStandsInFrontOfTheScreenAndAtItsMiddle() {
        CommandCenterGeometry.Screen screen = CommandCenterGeometry.screen();
        assertThat(CommandCenterGeometry.textZ()).isLessThan(screen.z());
        assertThat(CommandCenterGeometry.textZ()).isCloseTo(screen.z(), within(0.01));
        assertThat(CommandCenterGeometry.textY()).isCloseTo(screen.centreY(), within(1e-9));
    }

    @Test
    void theTextScaleMakesTheLineWidthSpanTheScreen() {
        double spanned = CommandCenterGeometry.textScale() * CommandCenterGeometry.LINE_WIDTH * CommandCenterGeometry.PIXEL_BLOCKS;
        assertThat(spanned).isCloseTo(CommandCenterGeometry.screen().width(), within(1e-6));
    }

    @Test
    void theClickTargetCoversTheScreenWithAMargin() {
        CommandCenterGeometry.Screen screen = CommandCenterGeometry.screen();
        assertThat(CommandCenterGeometry.clickWidth()).isGreaterThan(screen.width());
        assertThat(CommandCenterGeometry.clickHeight()).isGreaterThan(screen.height());
        assertThat(CommandCenterGeometry.clickBottomY()).isLessThan(screen.y0());
        assertThat(CommandCenterGeometry.clickBottomY() + CommandCenterGeometry.clickHeight())
                .isGreaterThan(screen.y1());
        assertThat(CommandCenterGeometry.clickZ()).isCloseTo(screen.z(), within(1e-9));
    }
}
