package io.corefactory.paper.artillery;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the map's buttons: a click at each button's centre hits that button, a click outside hits none, and no two buttons overlap. */
class TargetButtonTest {

    @Test
    void aClickAtTheCentreOfEachButtonHitsThatButton() {
        for (TargetButton button : TargetButton.values()) {
            assertThat(TargetButton.at(button.centreX(), button.centreY())).isEqualTo(Optional.of(button));
        }
    }

    @Test
    void aClickOutsideEveryButtonHitsNothing() {
        assertThat(TargetButton.at(64, 64)).isEmpty();
        assertThat(TargetButton.at(0, 127)).isEmpty();
        assertThat(TargetButton.at(127, 60)).isEmpty();
        assertThat(TargetButton.at(-1, -1)).isEmpty();
    }

    @Test
    void theRightEdgeAndBottomEdgeOfAButtonAreOutsideIt() {
        TargetButton zoomIn = TargetButton.ZOOM_IN;
        assertThat(TargetButton.at(zoomIn.x1(), zoomIn.centreY())).isEmpty();
        assertThat(TargetButton.at(zoomIn.centreX(), zoomIn.y1())).isEmpty();
        assertThat(TargetButton.at(zoomIn.x0(), zoomIn.y0())).isEqualTo(Optional.of(zoomIn));
    }

    @Test
    void noTwoButtonsOverlap() {
        TargetButton[] buttons = TargetButton.values();
        for (int i = 0; i < buttons.length; i++) {
            for (int j = i + 1; j < buttons.length; j++) {
                TargetButton a = buttons[i];
                TargetButton b = buttons[j];
                boolean apart = a.x1() <= b.x0() || b.x1() <= a.x0() || a.y1() <= b.y0() || b.y1() <= a.y0();
                assertThat(apart).as(a + " and " + b).isTrue();
            }
        }
    }

    @Test
    void everyButtonLiesOnTheMap() {
        for (TargetButton button : TargetButton.values()) {
            assertThat(button.x0()).isGreaterThanOrEqualTo(0);
            assertThat(button.y0()).isGreaterThanOrEqualTo(0);
            assertThat(button.x1()).isLessThanOrEqualTo(TargetView.PIXELS);
            assertThat(button.y1()).isLessThanOrEqualTo(TargetView.PIXELS);
        }
    }
}
