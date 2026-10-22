package io.corefactory.core.deposit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DepositTextTest {

    private static Deposit deposit(long remaining, long initial) {
        return new Deposit(4, "world", 0, -42, 30, "galena_ore", initial, remaining);
    }

    @Test
    void thePlaceNamesTheDistanceAndThePointFromTheScanner() {
        assertThat(DepositText.place(deposit(1, 1), 0, 0)).isEqualTo("42 m N");
        assertThat(DepositText.place(deposit(1, 1), 0, -42)).isEqualTo(Compass.HERE);
    }

    @Test
    void aFarPlaceReadsInKilometres() {
        Deposit far = new Deposit(4, "world", 12_500, 12_500, 30, "galena_ore", 1, 1);

        assertThat(DepositText.place(far, 0, 0)).isEqualTo("17.6 km SE");
    }

    @Test
    void theActionLineReadsTheNearestDepositInOneLine() {
        Deposit rich = new Deposit(4, "world", 30, -30, 30, "galena_ore", 5_000_000, 3_200_000);

        assertThat(DepositText.actionLine("Galena Ore", rich, 0, 0))
                .isEqualTo("&7Nearest &fGalena Ore&7: &f42 m NE&7, &6Rich&7, &f3.2M &7left");
    }

    @Test
    void theListLineGivesTheShareLeft() {
        Deposit poor = new Deposit(4, "world", 0, 10, 30, "coal_ore", 2_000_000, 500_000);

        assertThat(DepositText.listLine("&7Coal Ore", poor, 0, 0))
                .isEqualTo("&8 - &f&7Coal Ore&7: &f10 m S&7, &7Poor&7, &f500k &7left &8(25%)");
    }

    @Test
    void theSurveyLineGivesTheCentreAndTheRadius() {
        assertThat(DepositText.surveyLine("&7Galena Ore", deposit(1, 1)))
                .isEqualTo("&8 - &f&7Galena Ore&7: centre &f0, -42&7, radius &f30 &7blocks");
    }
}
