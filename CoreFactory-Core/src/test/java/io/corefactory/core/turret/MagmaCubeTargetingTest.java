package io.corefactory.core.turret;

import io.corefactory.core.turret.TargetRules.Candidate;
import io.corefactory.core.turret.TargetRules.Relation;
import io.corefactory.core.turret.TargetRules.Verdict;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A magma cube is a slime, and the turret's menu toggles it by its own entity key. With that toggle on, a magma cube in range and in the
 * line of fire must be a valid target, whatever the category list of the shipped settings says.
 */
class MagmaCubeTargetingTest {

    private static final TurretSettings SETTINGS = TurretSettings.defaults();
    private static final TargetRules.Point HEAD = new TargetRules.Point(0, 0, 0);
    private static final TargetRules.Point NEAREST = new TargetRules.Point(5, 0, 0);

    private static Candidate magmaCube() {
        return new Candidate(TargetKind.SLIME, false, false, false, false, false, false, false, 5.0, true, "MAGMA_CUBE",
                Relation.NONE);
    }

    @Test
    void aMagmaCubeIsValidWhenItsOwnToggleIsOn() {
        TargetPolicy policy = TargetPolicy.DEFAULT.withOverride("MAGMA_CUBE", true);

        assertThat(TargetRules.verdict(magmaCube(), SETTINGS, policy, HEAD, NEAREST)).isEqualTo(Verdict.VALID);
    }

    @Test
    void aMagmaCubeIsRefusedWhileItsToggleIsOff() {
        TargetPolicy policy = TargetPolicy.DEFAULT.withOverride("MAGMA_CUBE", false);

        assertThat(TargetRules.verdict(magmaCube(), SETTINGS, policy, HEAD, NEAREST)).isEqualTo(Verdict.NOT_A_TARGET_KIND);
    }

    @Test
    void aMagmaCubeIsRefusedWithNoToggleAndTheShippedCategories() {
        assertThat(TargetRules.verdict(magmaCube(), SETTINGS, TargetPolicy.DEFAULT, HEAD, NEAREST))
                .isEqualTo(Verdict.NOT_A_TARGET_KIND);
    }

    @Test
    void aMagmaCubeIsValidWhenTheSlimeCategoryIsOn() {
        TargetPolicy policy = TargetPolicy.DEFAULT.withOverride("slimes", true);

        assertThat(TargetRules.verdict(magmaCube(), SETTINGS, policy, HEAD, NEAREST)).isEqualTo(Verdict.VALID);
    }
}
