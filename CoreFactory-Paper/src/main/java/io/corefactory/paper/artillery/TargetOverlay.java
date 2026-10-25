package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryEffects;
import io.corefactory.core.artillery.ArtilleryEffects.Column;
import io.corefactory.core.artillery.ArtilleryFiringRules;
import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ArtillerySettings;
import io.corefactory.core.artillery.GroundPoint;

import java.util.List;

/**
 * The strike area of a shell on the map: the blast circle of its reach, the submunition points of a cluster, the napalm ellipse, and the
 * phosphorus cloud. It is drawn from the same geometry the impact uses, with a fixed seed for the submunitions, so the preview is the same
 * from one redraw to the next. Pure, so it is tested without a server.
 *
 * @param aimX          the x of the aim point, in blocks
 * @param aimZ          the z of the aim point, in blocks
 * @param reachBlocks   the radius of the blast circle around the aim point, in blocks
 * @param submunitions  the points the submunitions of a cluster shell land on; empty for other shells
 * @param napalm        the columns a napalm burst covers; empty for other shells
 * @param phosphorus    the columns a phosphorus cloud covers on the ground; empty for other shells
 */
public record TargetOverlay(double aimX, double aimZ, double reachBlocks, List<GroundPoint> submunitions, List<Column> napalm,
                            List<Column> phosphorus) {

    /**
     * The strike area of a shell at an aim point.
     *
     * @param settings   the settings of the shells, the submunitions and the clouds
     * @param rules      the firing rules, whose reach of a shell is the blast circle
     * @param shell      the shell
     * @param aimX       the x of the aim point, in blocks
     * @param aimZ       the z of the aim point, in blocks
     * @param dirX       the x of the unit direction on the ground from the barrel to the aim; 0 when there is none
     * @param dirZ       the z of the unit direction on the ground
     * @param seed       the seed of the submunitions' layout
     * @return the strike area
     */
    public static TargetOverlay of(ArtillerySettings settings, ArtilleryFiringRules rules, ArtilleryShell shell, double aimX,
                                   double aimZ, double dirX, double dirZ, long seed) {
        List<GroundPoint> submunitions = List.of();
        if (shell == ArtilleryShell.CLUSTER) {
            ArtillerySettings.Cluster cluster = settings.cluster();
            submunitions = ArtilleryEffects.clusterImpacts(aimX, aimZ, cluster.spreadRadiusBlocks(), cluster.submunitions(), seed,
                    cluster.minSpacingBlocks());
        } else if (shell == ArtilleryShell.CLUSTER_ENRICHED_URANIUM) {
            ArtillerySettings.ClusterEnriched bombs = settings.clusterEnriched();
            submunitions = ArtilleryEffects.clusterImpacts(aimX, aimZ, bombs.spreadRadiusBlocks(), bombs.bombs(), seed,
                    bombs.minSpacingBlocks());
        }
        List<Column> napalm = shell.effect() == ArtilleryShell.Effect.FIRE
                ? ArtilleryEffects.napalmBurst(aimX, aimZ, dirX, dirZ, settings.napalm().lengthBlocks(), settings.napalm().widthBlocks())
                : List.of();
        List<Column> phosphorus = shell.effect() == ArtilleryShell.Effect.SMOKE
                ? ArtilleryEffects.phosphorusCloud(aimX, aimZ, settings.phosphorus().cloudRadiusBlocks(), 0).ground()
                : List.of();
        return new TargetOverlay(aimX, aimZ, rules.reachBlocks(shell), submunitions, napalm, phosphorus);
    }
}
