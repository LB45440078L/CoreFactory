package io.corefactory.paper.artillery;

import io.corefactory.api.MechanicSettings;
import io.corefactory.api.TntDefinition;
import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.artillery.ArtilleryDetonation;
import io.corefactory.core.artillery.ArtilleryFiringRules;
import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ArtillerySettings;
import io.corefactory.core.artillery.ChunkDisc;
import io.corefactory.core.artillery.GroundPoint;
import io.corefactory.paper.chunks.ChunkKey;
import io.corefactory.paper.chunks.ChunkTickets;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What a shell does where it lands. A blast, a nuclear burst and the small detonations of a cluster run through the core's TNT manager, which
 * is the path a placed TNT takes: the manager fires the detonation event, the mechanic runs (the standard blast, or the nuclear mechanic for a
 * warhead), and the detonation observers hear of it. A phosphorus shell makes a cloud, and a napalm shell sets fire to its ground. Nothing here
 * carves the terrain itself.
 *
 * <p>The chunks an effect touches are held loaded with plugin tickets: the shot holds them from its launch, and the landing holds them until
 * the effect has run ({@link #hold}). The disc an effect covers is cut to its {@link #MAX_IMPACT_CHUNKS} chunks nearest the landing, so a
 * blast of a few hundred blocks is loaded whole, and the far part of a huge one is left unloaded, as it was before.
 */
final class ArtilleryImpacts {

    /** The ticks between two submunitions of one shell, so the manager's rate limit of detonations is not reached in one tick. */
    static final long SUBMUNITION_SPACING_TICKS = 1L;

    /**
     * The most chunks an impact holds, the nearest to the landing. The chunks of a disc of 913 blocks are about 10,000, and loading them all at
     * once would stall the server and keep them in memory; 1024 is a disc of about 290 blocks, which is loaded in one go.
     */
    static final int MAX_IMPACT_CHUNKS = 1024;

    /**
     * The scale of a nuclear shell's disc. The mechanic cuts the crater to 0.6 of the blast radius and flattens out to 1.8 times the crater, so
     * the flatten reaches about 1.08 times the radius; 1.1 keeps its last ring inside the disc.
     */
    static final double NUCLEAR_TICKET_SCALE = 1.1;

    /**
     * How long an impact's chunks stay held after the landing of a blast, in ticks. The writes of a blast are streamed over ticks, and the
     * core's block jobs have no completion hook here, so the hold is a time: 90 seconds covers the streamed writes of the blasts this plugin
     * fires, and the flatten past it skips the chunks that have unloaded.
     */
    static final long BLAST_HOLD_TICKS = 20L * 90L;

    private final CoreFactoryCore core;
    private final ArtillerySettings settings;
    private final ArtilleryFiringRules rules;
    private final ArtilleryHazards hazards;
    private final ChunkTickets tickets;

    /**
     * @param core     the core, whose TNT manager runs the detonations and whose scheduler spaces the submunitions
     * @param settings the settings of the shells
     * @param rules    the firing rules, whose blast radius a shell's blast uses
     * @param hazards  the clouds and fires that outlast a landing
     */
    ArtilleryImpacts(CoreFactoryCore core, ArtillerySettings settings, ArtilleryFiringRules rules, ArtilleryHazards hazards) {
        this.core = core;
        this.settings = settings;
        this.rules = rules;
        this.hazards = hazards;
        this.tickets = ChunkTickets.of(core.plugin());
    }

    /** The plugin's chunk tickets, which the shots and the impacts hold their chunks in, next to the device chunk keeper. */
    ChunkTickets tickets() {
        return tickets;
    }

    /**
     * The chunks an effect touches, for a shell that lands at a plan's point: the chunks nearest the landing of the disc its effect covers, its
     * reach from {@link ArtilleryFiringRules#reachBlocks}, scaled for a nuclear shell, and cut to {@link #MAX_IMPACT_CHUNKS}.
     *
     * @param world the world of the shell
     * @param shell the shell
     * @param plan  the impact
     * @return the chunks, nearest the landing first
     */
    List<ChunkKey> chunksTouched(UUID world, ArtilleryShell shell, ArtilleryImpactPlan plan) {
        double reach = rules.reachBlocks(shell) * (shell.nuclear() ? NUCLEAR_TICKET_SCALE : 1.0);
        List<ChunkDisc.Chunk> disc = ChunkDisc.nearestFirst(plan.landing().x(), plan.landing().z(), reach);
        List<ChunkKey> chunks = new ArrayList<>(Math.min(MAX_IMPACT_CHUNKS, disc.size()));
        for (ChunkDisc.Chunk chunk : disc) {
            if (chunks.size() == MAX_IMPACT_CHUNKS) {
                break;
            }
            chunks.add(new ChunkKey(world, chunk.x(), chunk.z()));
        }
        return List.copyOf(chunks);
    }

    /**
     * Holds the chunks of a landing under an id, for as long as its effect needs them: a blast for {@link #BLAST_HOLD_TICKS}, a cloud for its
     * duration, a fire for its burning, and a cluster for its last submunition and the blast hold after it. The hold ends by itself: a scheduled
     * release removes the id, and a release that comes earlier or later does nothing more.
     *
     * @param world the world the shell lands in
     * @param id    the holder's id
     * @param shell the shell
     * @param plan  the impact
     */
    void hold(World world, String id, ArtilleryShell shell, ArtilleryImpactPlan plan) {
        tickets.hold(id, chunksTouched(world.getUID(), shell, plan));
        core.scheduleLater(holdTicks(shell, plan), () -> tickets.release(id));
    }

    /** How long a landing's chunks stay held, in ticks: the time its effect runs for. */
    private long holdTicks(ArtilleryShell shell, ArtilleryImpactPlan plan) {
        return switch (shell.effect()) {
            case BLAST -> BLAST_HOLD_TICKS;
            case SMOKE -> (long) Math.ceil(settings.phosphorus().durationSeconds() * 20.0) + ArtilleryHazards.STEP_TICKS;
            case FIRE -> (long) ArtilleryHazards.FIRE_SECONDS * 20L + ArtilleryHazards.STEP_TICKS;
            case SUBMUNITIONS -> plan.submunitions().size() * SUBMUNITION_SPACING_TICKS + BLAST_HOLD_TICKS;
        };
    }

    /**
     * Makes a shell land: its effect, at the point the plan gives.
     *
     * @param world   the world the shell lands in
     * @param shooter the player who fired it, or null when they are offline
     * @param shell   the shell
     * @param plan    the impact, drawn before the shot was fired
     */
    void land(World world, Entity shooter, ArtilleryShell shell, ArtilleryImpactPlan plan) {
        GroundPoint at = plan.landing();
        ArtillerySettings.Presentation presentation = settings.presentation();
        switch (shell.effect()) {
            case BLAST -> {
                Location origin = origin(world, at, plan.altitude());
                double tons = settings.shell(shell).yieldTons();
                if (shell.nuclear()) {
                    nuclear(shooter, origin, tons, "artillery_" + shell.key());
                } else {
                    standard(shooter, origin, tons, rules.blastRadiusBlocks(tons), "artillery_" + shell.key());
                }
                // The blast's own explosion sound comes from the core, so only the debris of a blast on the ground is added here.
                if (plan.altitude() == 0.0) {
                    ArtilleryCues.debris(origin.clone().add(0.0, -0.5, 0.0), presentation.debrisParticles());
                }
            }
            case SMOKE -> hazards.addCloud(world, origin(world, at, 0.0), origin(world, at, plan.altitude()),
                    settings.phosphorus());
            case FIRE -> {
                hazards.addFire(world, plan.fire());
                ArtilleryCues.thud(origin(world, at, 0.0), presentation.impactVolume());
            }
            case SUBMUNITIONS -> submunitions(world, shooter, shell, plan);
        }
    }

    /** The submunitions of a cluster, or the bombs of a cluster enriched uranium shell, one after another a tick apart. */
    private void submunitions(World world, Entity shooter, ArtilleryShell shell, ArtilleryImpactPlan plan) {
        double tons = settings.shell(shell).yieldTons();
        long delay = 0L;
        for (GroundPoint point : plan.submunitions()) {
            delay += SUBMUNITION_SPACING_TICKS;
            long wait = delay;
            if (shell.nuclear()) {
                core.scheduleLater(wait, () -> nuclear(shooter, origin(world, point, plan.altitude()), tons,
                        "artillery_bomb"));
            } else {
                double radius = ArtilleryDetonation.submunitionRadiusBlocks(tons, settings.safety().blastRadiusConstant());
                core.scheduleLater(wait, () -> standard(shooter, origin(world, point, 0.0), tons, radius,
                        "artillery_submunition"));
            }
        }
    }

    /** A standard blast of a yield, with its radius and centre damage, through the TNT manager. */
    private void standard(Entity shooter, Location at, double tons, double radius, String id) {
        Map<String, Object> raw = Map.<String, Object>of("damage", ArtilleryDetonation.damage(tons));
        detonate(shooter, at, definition(id, radius, "standard", raw));
    }

    /**
     * A nuclear burst of a yield, through the TNT manager and the nuclear mechanic. The burst flattens only the columns whose chunks are loaded
     * ({@code skip-unloaded-chunks}): it never loads a chunk to cut or clear it, which the mechanic would do one column at a time on the main
     * thread. The chunks nearest the burst are held loaded by {@link #hold}, so the flatten reaches them; for a condensed antimatter blast, whose
     * reach is about 913 blocks, the far terrain beyond those {@link #MAX_IMPACT_CHUNKS} chunks is left as it is.
     */
    private void nuclear(Entity shooter, Location at, double tons, String id) {
        Map<String, Object> raw = Map.<String, Object>of("yield-megatons", ArtilleryDetonation.megatons(tons),
                "skip-unloaded-chunks", 1);
        detonate(shooter, at, definition(id, rules.blastRadiusBlocks(tons), "nuclear", raw));
    }

    private void detonate(Entity shooter, Location at, TntDefinition definition) {
        core.manager().detonate(definition, at, shooter, null, 0, null);
    }

    /** The definition an artillery detonation runs with: a TNT type that exists only for the shot, with its mechanic and its settings. */
    private static TntDefinition definition(String id, double radius, String mechanic, Map<String, Object> raw) {
        return new TntDefinition(id, "&6Artillery shell", List.of(), Material.TNT, 0, radius, mechanic,
                new MechanicSettings(raw), Set.of());
    }

    /**
     * The point above a ground point at which a burst goes off: the air over the top of the ground, raised by the altitude. The point is the
     * middle of its block, so the burst's blocks are the ones the point stands in.
     */
    static Location origin(World world, GroundPoint at, double altitude) {
        int x = (int) Math.floor(at.x());
        int z = (int) Math.floor(at.z());
        int top = world.getHighestBlockYAt(x, z);
        return new Location(world, x + 0.5, top + 1.0 + altitude, z + 0.5);
    }
}
