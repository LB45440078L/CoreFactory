package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryDetonation;
import io.corefactory.core.artillery.ArtilleryDispersion;
import io.corefactory.core.artillery.ArtilleryEffects;
import io.corefactory.core.artillery.ArtilleryEffects.Column;
import io.corefactory.core.artillery.ArtilleryLevel;
import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ArtilleryTable;
import io.corefactory.core.artillery.ArtillerySettings;
import io.corefactory.core.artillery.ChargeTier;
import io.corefactory.core.artillery.GroundPoint;

import java.util.List;
import java.util.Random;

/**
 * Where a shot lands and what its shell does there, drawn from one seeded random source before the shot is fired. The landing is the aim point
 * scattered by the piece's dispersion; the altitude is the burst height of an air burst (0 for a shell that explodes on contact); the
 * submunitions are the points of a cluster or the bombs of a cluster enriched uranium shell; the fire columns are the napalm burst. A seed
 * gives the same plan, so a shot can be replayed in a test. Pure, so the layout can be tested without a server.
 *
 * @param landing     the point the shell comes down on, after the scatter
 * @param altitude    the height above the landing point at which an air burst goes off, in blocks; 0 for a contact shell
 * @param submunitions the points the submunitions or the bombs land on, empty for a shell with none
 * @param fire        the columns a napalm burst sets on fire, empty for a shell that does not burn
 */
public record ArtilleryImpactPlan(GroundPoint landing, double altitude, List<GroundPoint> submunitions, List<Column> fire) {

    /**
     * Plans the impact of a shot.
     *
     * @param settings   the settings of the shells, the scatter and the submunitions
     * @param shell      the shell fired
     * @param level      the level of the piece
     * @param tier       the charge tier loaded
     * @param aim        the aim point on the ground
     * @param rangeBlocks the horizontal distance from the piece to the aim point, in blocks
     * @param dirX       the x component of the unit direction on the ground, from the piece towards the aim
     * @param dirZ       the z component of the unit direction on the ground
     * @param random     the seeded random source of the shot
     * @return the plan
     * @throws IllegalStateException when the submunition points cannot be laid out
     */
    public static ArtilleryImpactPlan of(ArtillerySettings settings, ArtilleryShell shell, ArtilleryLevel level, ChargeTier tier,
                                         GroundPoint aim, double rangeBlocks, double dirX, double dirZ, Random random) {
        ArtilleryDispersion dispersion = new ArtilleryDispersion(new ArtilleryTable(settings));
        GroundPoint landing = dispersion.landing(aim, rangeBlocks, level, tier, random);
        double altitude = shell.detonation() == ArtilleryShell.Detonation.AIR_BURST
                ? ArtilleryDetonation.burstAltitude(settings.shell(shell), random)
                : 0.0;
        List<GroundPoint> submunitions = List.of();
        if (shell == ArtilleryShell.CLUSTER) {
            ArtillerySettings.Cluster cluster = settings.cluster();
            submunitions = ArtilleryEffects.clusterImpacts(landing.x(), landing.z(), cluster.spreadRadiusBlocks(),
                    cluster.submunitions(), random.nextLong(), cluster.minSpacingBlocks());
        } else if (shell == ArtilleryShell.CLUSTER_ENRICHED_URANIUM) {
            ArtillerySettings.ClusterEnriched bombs = settings.clusterEnriched();
            submunitions = ArtilleryEffects.clusterImpacts(landing.x(), landing.z(), bombs.spreadRadiusBlocks(),
                    bombs.bombs(), random.nextLong(), bombs.minSpacingBlocks());
        }
        List<Column> fire = shell.effect() == ArtilleryShell.Effect.FIRE
                ? ArtilleryEffects.napalmBurst(landing.x(), landing.z(), dirX, dirZ, settings.napalm().lengthBlocks(),
                        settings.napalm().widthBlocks())
                : List.of();
        return new ArtilleryImpactPlan(landing, altitude, submunitions, fire);
    }
}
