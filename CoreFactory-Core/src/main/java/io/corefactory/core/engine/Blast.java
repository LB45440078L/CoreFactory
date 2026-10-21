package io.corefactory.core.engine;

import io.corefactory.api.BlockChangeSet;
import io.corefactory.api.DetonationContext;
import io.corefactory.api.CoreFactoryApi;
import io.corefactory.api.PlacedTnt;
import io.corefactory.api.TntDefinition;
import io.corefactory.core.config.CoreSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * The standard blast every mechanic builds on: carve a sphere, damage what is inside, and apply the effect
 * modifiers that the TNT declares.
 *
 * <p>Effect modifiers ({@code effects:} in {@code tnts.yml}):
 * <ul>
 *   <li>{@code shockwave_ring}: a visible ring expands for {@code ring-ticks} before anything is destroyed.</li>
 *   <li>{@code debris}: some destroyed blocks fly out as falling blocks instead of vanishing.</li>
 *   <li>{@code echo}: {@code echo-count} extra booms follow, visual and audio only, with no damage.</li>
 *   <li>{@code silent}: no boom and no fuse sound; the ignition is delayed (see the manager).</li>
 * </ul>
 *
 * <p>Other TNT inside the radius is not destroyed. Placed custom TNT is ignited after a delay that grows
 * with distance, and vanilla TNT is primed the same way, so chains look like real chains.
 */
public final class Blast {

    private static final int MAX_DEBRIS = 80;
    private static final double DEBRIS_CHANCE = 0.12;

    private final Plugin plugin;
    private final Effects effects;
    private final Explosions explosions;
    private int chainBaseDelay;
    private boolean debug;

    public Blast(Plugin plugin, Effects effects, Explosions explosions, CoreSettings settings) {
        this.plugin = plugin;
        this.effects = effects;
        this.explosions = explosions;
        this.chainBaseDelay = settings.chainIgniteDelayTicks();
        this.debug = settings.debug();
    }

    public void reconfigure(CoreSettings settings) {
        this.chainBaseDelay = settings.chainIgniteDelayTicks();
        this.debug = settings.debug();
    }

    /**
     * Runs the blast.
     *
     * @param radius            sphere radius in blocks
     * @param damage            maximum entity damage at the centre
     * @param chainDelayPerBlock extra ignition delay per block of distance for TNT caught in the blast
     * @param after             receives the set of carved positions once they are queued (may be empty)
     */
    public void run(DetonationContext ctx, double radius, double damage, double chainDelayPerBlock,
                    Consumer<BlockChangeSet> after) {
        TntDefinition tnt = ctx.tnt();
        Location origin = ctx.origin();
        boolean silent = tnt.hasEffect("silent");
        if (!silent) {
            effects.boom(origin, (float) Math.max(2.0, radius / 3.0));
        }

        Runnable carve = () -> {
            BlockChangeSet set = carve(ctx, radius, chainDelayPerBlock);
            if (debug) {
                plugin.getLogger().info("[debug] blast " + ctx.tnt().id() + " at " + ctx.origin().getBlockX() + ","
                        + ctx.origin().getBlockY() + "," + ctx.origin().getBlockZ() + " radius=" + radius
                        + " source=" + (ctx.source() == null ? "none" : ctx.source().getType()) + " writes=" + set.size());
            }
            ctx.api().submitBlockChanges(ctx.world(), set, () -> {
            });
            after.accept(set);
            explosions.damage(origin, radius, damage, ctx.source());
        };

        if (tnt.hasEffect("shockwave_ring")) {
            int ringTicks = Math.max(2, ctx.settings().getInt("ring-ticks", 12));
            ring(origin, radius, ringTicks);
            ctx.api().scheduleLater(ringTicks, carve);
        } else {
            carve.run();
        }

        if (tnt.hasEffect("echo")) {
            echo(ctx, radius);
        }
    }

    /**
     * Works out the sphere's block writes without applying them. Destroyed blocks become air; TNT inside the
     * sphere is ignited instead of destroyed; protected blocks are left alone. Callers queue the result.
     */
    public BlockChangeSet carve(DetonationContext ctx, double radius, double chainDelayPerBlock) {
        CoreFactoryApi api = ctx.api();
        World world = ctx.world();
        Location origin = ctx.origin();
        boolean debris = ctx.tnt().hasEffect("debris");
        BlockChangeSet set = new BlockChangeSet(256);
        int[] debrisLeft = {MAX_DEBRIS};

        Shapes.sphere(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), radius,
                world.getMinHeight(), world.getMaxHeight() - 1, (x, y, z) -> {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    if (type.isAir()) {
                        return;
                    }
                    if (type == Material.TNT) {
                        Location at = new Location(world, x, y, z);
                        if (api.protection().inAnchorField(at)) {
                            return;
                        }
                        double distance = at.distance(origin);
                        Optional<PlacedTnt> placed = api.placedAt(at);
                        long delay = chainBaseDelay + Math.round(distance * chainDelayPerBlock);
                        if (placed.isPresent()) {
                            placed.get().setChainDepth(ctx.depth() + 1);
                            api.scheduleLater(delay, () -> api.ignite(placed.get(), ctx.source()));
                        } else {
                            primeVanilla(world, at, delay);
                            set.set(x, y, z, Material.AIR.createBlockData());
                        }
                        return;
                    }
                    if (api.protection().isProtected(block)) {
                        return;
                    }
                    if (debris && debrisLeft[0] > 0 && ThreadLocalRandom.current().nextDouble() < DEBRIS_CHANCE) {
                        debrisLeft[0]--;
                        spawnDebris(world, x, y, z, block.getBlockData(), origin);
                    }
                    set.set(x, y, z, Material.AIR.createBlockData());
                });

        return set;
    }

    /**
     * Ignites the TNT at one block that a blast did not reach: a placed TNT is ignited after a distance-based
     * delay, and a vanilla TNT block is replaced by a primed entity with the same delay.
     */
    public void igniteAt(DetonationContext ctx, World world, int x, int y, int z, double distance, double perBlock) {
        Location at = new Location(world, x, y, z);
        long delay = chainBaseDelay + Math.round(distance * perBlock);
        Optional<PlacedTnt> placed = ctx.api().placedAt(at);
        if (placed.isPresent()) {
            placed.get().setChainDepth(ctx.depth() + 1);
            ctx.api().scheduleLater(delay, () -> ctx.api().ignite(placed.get(), ctx.source()));
            return;
        }
        world.getBlockAt(x, y, z).setType(Material.AIR, false);
        primeVanilla(world, at, delay);
    }

    private void primeVanilla(World world, Location at, long fuse) {
        int ticks = (int) Math.max(1L, fuse);
        world.spawn(at.clone().add(0.5, 0.5, 0.5), TNTPrimed.class, tnt -> tnt.setFuseTicks(ticks));
    }

    private void spawnDebris(World world, int x, int y, int z, BlockData data, Location origin) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location at = new Location(world, x + 0.5, y + 0.5, z + 0.5);
        Vector away = at.toVector().subtract(origin.toVector());
        Vector velocity = (away.lengthSquared() < 0.01 ? new Vector(0, 1, 0) : away.normalize())
                .multiply(0.4 + random.nextDouble() * 0.5)
                .add(new Vector(0, 0.3 + random.nextDouble() * 0.4, 0));
        FallingBlock falling = world.spawnFallingBlock(at, data);
        falling.setDropItem(true);
        falling.setHurtEntities(false);
        falling.setVelocity(velocity);
    }

    private void ring(Location origin, double radius, int ticks) {
        Tasks.repeat(plugin, 1L, ticks, step -> {
            double r = radius * (step + 1) / ticks;
            int points = 16 + (int) (r * 3);
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                Location at = origin.clone().add(Math.cos(angle) * r, 0.2, Math.sin(angle) * r);
                effects.particle(at, Particle.FLAME, 1, 0.0, 0.0, 64.0);
            }
        }, () -> {
        });
    }

    private void echo(DetonationContext ctx, double radius) {
        int count = Math.max(1, ctx.settings().getInt("echo-count", 3));
        int interval = Math.max(2, ctx.settings().getInt("echo-interval", 8));
        double jitter = Math.max(0.0, ctx.settings().getDouble("echo-jitter", 4.0));
        Location origin = ctx.origin();
        Tasks.repeat(plugin, interval, count, step -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            Location at = origin.clone().add((random.nextDouble() - 0.5) * jitter, 0,
                    (random.nextDouble() - 0.5) * jitter);
            effects.boom(at, (float) Math.max(1.5, radius / 4.0));
        }, () -> {
        });
    }
}
