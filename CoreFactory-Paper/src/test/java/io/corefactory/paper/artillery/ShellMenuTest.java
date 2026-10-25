package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ChargeTier;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the order of the shells, the warnings of the shells and the tiers, and the names the dialog shows. */
class ShellMenuTest {

    @Test
    void theShellsAreInTheOrderOfTheLevelThatFiresThem() {
        assertThat(ShellMenu.ordered()).containsExactly(
                ArtilleryShell.EXPLOSIVE, ArtilleryShell.HIGH_EXPLOSIVE, ArtilleryShell.WHITE_PHOSPHORUS, ArtilleryShell.NAPALM,
                ArtilleryShell.CLUSTER,
                ArtilleryShell.ENRICHED_URANIUM, ArtilleryShell.HYDROGEN_THERMONUCLEAR,
                ArtilleryShell.CLUSTER_ENRICHED_URANIUM, ArtilleryShell.CONDENSED_ANTIMATTER);
    }

    @Test
    void noShellWarnsWhenNoArtilleryIsConnected() {
        assertThat(ShellMenu.shells(0)).allMatch(row -> !row.warn() && row.reason().isEmpty());
    }

    @Test
    void aShellAboveTheHighestConnectedLevelWarnsWithItsReason() {
        List<ShellMenu.ShellRow> rows = ShellMenu.shells(3);
        ShellMenu.ShellRow uranium = rows.stream().filter(r -> r.shell() == ArtilleryShell.ENRICHED_URANIUM).findFirst().orElseThrow();
        assertThat(uranium.warn()).isTrue();
        assertThat(uranium.reason()).isEqualTo("needs level VI; the highest connected piece is level III");
        ShellMenu.ShellRow cluster = rows.stream().filter(r -> r.shell() == ArtilleryShell.CLUSTER).findFirst().orElseThrow();
        assertThat(cluster.warn()).isFalse();
        assertThat(rows.stream().filter(ShellMenu.ShellRow::warn).count()).isEqualTo(4);
    }

    @Test
    void noShellWarnsAtTheTopLevel() {
        assertThat(ShellMenu.shells(7)).allMatch(row -> !row.warn());
    }

    @Test
    void aTierNoConnectedPieceHasLoadedWarns() {
        List<ShellMenu.TierRow> none = ShellMenu.tiers(EnumSet.noneOf(ChargeTier.class));
        assertThat(none).allMatch(row -> !row.warn());

        List<ShellMenu.TierRow> tierOne = ShellMenu.tiers(EnumSet.of(ChargeTier.TIER_I));
        assertThat(tierOne).extracting(ShellMenu.TierRow::warn).containsExactly(false, true, true);
        assertThat(tierOne.get(1).reason()).isEqualTo("no connected piece has this charge loaded");
    }

    @Test
    void theChosenRowIsMarkedAndAccented() {
        assertThat(ShellMenu.label("High explosive", true, false)).isEqualTo("<accent>> High explosive</accent>");
        assertThat(ShellMenu.label("Tier II", true, false)).isEqualTo("<accent>> Tier II</accent>");
        assertThat(ShellMenu.label("Napalm", false, false)).isEqualTo("Napalm");
    }

    @Test
    void aWarningRowIsInTheWarningColourEvenWhenChosen() {
        assertThat(ShellMenu.label("Napalm", false, true)).isEqualTo("<warn>Napalm</warn>");
        assertThat(ShellMenu.label("Napalm", true, true)).isEqualTo("<warn>> Napalm</warn>");
    }

    @Test
    void theSelectionNamesTheShellAndTierOrSaysNoShellIsChosen() {
        assertThat(ShellMenu.selection(ArtilleryShell.HIGH_EXPLOSIVE, ChargeTier.TIER_II)).isEqualTo("High explosive, tier II");
        assertThat(ShellMenu.selection(null, ChargeTier.TIER_I)).isEqualTo("no shell chosen, tier I");
    }

    @Test
    void theNamesAreReadableAndTheTiersAreNumbered() {
        assertThat(ShellMenu.shellName(ArtilleryShell.HIGH_EXPLOSIVE)).isEqualTo("High explosive");
        assertThat(ShellMenu.shellName(ArtilleryShell.CLUSTER_ENRICHED_URANIUM)).isEqualTo("Cluster enriched uranium");
        assertThat(ShellMenu.tiers(EnumSet.allOf(ChargeTier.class))).extracting(ShellMenu.TierRow::name)
                .containsExactly("Tier I", "Tier II", "Tier III");
    }
}
