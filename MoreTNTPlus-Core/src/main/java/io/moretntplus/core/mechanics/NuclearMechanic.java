package io.moretntplus.core.mechanics;

import io.moretntplus.api.BlockChangeSet;
import io.moretntplus.api.BlockGenerator;
import io.moretntplus.api.ClientEffects;
import io.moretntplus.api.DetonationContext;
import io.moretntplus.api.MoreTntPlusApi;
import io.moretntplus.api.ProtectionService;
import io.moretntplus.api.TntDefinition;
import io.moretntplus.core.engine.Tasks;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The nuclear family. Every tier shares this mechanic and differs only in radius, fuse and settings in
 * {@code tnts.yml}.
 *
 * <p>Effects, by distance from the centre as a fraction of the radius R:
 * <ul>
 *   <li>within 1.5R: a countdown and red warning titles while the fuse burns, then an alarm at detonation;</li>
 *   <li>within 3R: for the player's client only, a 5x5 array of beacons draws beams to the sky, the rain stops,
 *       and the clock holds at noon, then slides back to the real time (PacketEvents);</li>
 *   <li>0.5R or less: killed instantly;</li>
 *   <li>0.5R to 0.8R: burning for {@code fire-far-seconds};</li>
 *   <li>0.8R to R: burning for {@code fire-near-seconds} and nausea.</li>
 * </ul>
 * The ground is cut by one of two shapes (see {@link Shape}) and everything above it out to the flatten radius is
 * cleared. The cut is streamed over several ticks so the server never stalls. Debris lands on the flattened ring
 * around it, on fire.
 */
final class NuclearMechanic extends AbstractMechanic {

    private static final Material[] DEBRIS = {
            Material.STONE, Material.DIRT, Material.ANDESITE, Material.COBBLESTONE,
            Material.GRAVEL, Material.DIORITE, Material.GRANITE
    };

    /** Noon, the brightest hour of the day, in ticks. */
    private static final long NOON = 6_000L;
    private static final long DAY_LENGTH = 24_000L;
    /** Seconds the noon hold lasts before the clock starts sliding back. */
    private static final int DAY_HOLD_SECONDS = 5;
    /** Seconds the slide back to the real time takes. */
    private static final int DAY_RETURN_SECONDS = 20;
    /** The fraction of the crater radius that is fused to green glass at the centre. */
    private static final double GLASS_CORE = 0.12;

    /** The cut's shape. A saucer is a shallow bowl from a surface burst; a sphere is a deep hemispherical hole. */
    enum Shape {
        SAUCER, SPHERE;

        static Shape of(String name) {
            return "sphere".equalsIgnoreCase(name) ? SPHERE : SAUCER;
        }
    }

    NuclearMechanic(Toolkit kit) {
        super("nuclear", "Gradual nuclear yields: countdown, instant kills, crater, debris and fire.", kit);
    }

    @Override
    public void onIgnite(Location origin, TntDefinition tnt, MoreTntPlusApi api) {
        int seconds = Math.max(1, tnt.fuseTicks() / 20);
        double warning = tnt.radius() * 1.5;
        Tasks.repeat(api.plugin(), 20L, seconds, step -> {
            int remaining = seconds - step;
            for (Player player : kit.effects().nearby(origin, warning)) {
                player.sendTitle("§4§l☢ NUCLEAR ☢", "§eT-minus " + remaining + "s", 0, 25, 5);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
            }
        }, () -> {
        });
    }

    @Override
    public void detonate(DetonationContext ctx) {
        double radius = ctx.tnt().radius();
        Location origin = ctx.origin();
        World world = ctx.world();
        MoreTntPlusApi api = ctx.api();
        int fireFar = Math.max(1, ctx.settings().getInt("fire-far-seconds", 20));
        int fireNear = Math.max(1, ctx.settings().getInt("fire-near-seconds", 5));
        int beamSeconds = Math.max(1, ctx.settings().getInt("beam-seconds", 30));

        BlockChangeSet beams = beaconArray(world, origin);
        List<Player> viewers = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            double distance = player.getLocation().distance(origin);
            if (distance <= radius * 0.5) {
                player.damage(1_000.0);
                continue;
            }
            if (distance <= radius * 0.8) {
                player.setFireTicks(fireFar * 20);
            } else if (distance <= radius) {
                player.setFireTicks(fireNear * 20);
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 15 * 20, 0));
            }
            if (distance <= radius * 1.5) {
                player.sendTitle("§4§l☢ NUCLEAR EXPLOSION ☢", "§7Get underground. Now.", 5, 70, 20);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 10.0f, 0.6f);
            }
            if (distance <= radius * 3.0) {
                viewers.add(player);
                showToViewer(api, player, world, beams, beamSeconds);
            }
        }

        kit.effects().particle(origin, Particle.EXPLOSION_EMITTER, 6, radius * 0.2, 0.2, radius * 3.0);

        int cx = origin.getBlockX();
        int cz = origin.getBlockZ();
        int craterRadius = Math.max(1, ctx.settings().getInt("crater-radius", (int) Math.round(radius * 0.6)));
        int reach = Math.max(craterRadius, ctx.settings().getInt("flatten-radius",
                (int) Math.round(craterRadius * 1.8)));
        Cut cut = new Cut(Shape.of(ctx.settings().getString("shape", "saucer")), craterRadius, reach,
                ctx.settings().getDouble("crater-depth", Math.max(3.0, radius * 0.2)),
                ctx.settings().getDouble("sink-depth", 0.0));
        BlockGenerator crater = new Crater(world, cx, cz, cut, world.getMinHeight(), api.protection());
        int debrisCount = Math.max(1, ctx.settings().getInt("debris-count", (int) (radius * 3)));
        BlockGenerator debris = new Debris(world, cx, cz, craterRadius, reach, debrisCount, api.protection());
        api.submitBlockGenerator(world, crater, () -> api.submitBlockGenerator(world, debris, () -> {
        }));

        double loud = radius * 3.0;
        for (Player player : kit.effects().nearby(origin, loud)) {
            player.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 10.0f, 0.5f);
        }
        kit.explosions().damage(origin, radius, damage(ctx, 60.0), ctx.source());
    }

    /**
     * What a viewer within 3R sees: the beacon array, no rain, and the clock held at noon. After
     * {@link #DAY_HOLD_SECONDS} the clock slides back to the real time over {@link #DAY_RETURN_SECONDS} and is then
     * handed back to the server. The beams and the weather end after {@code beamSeconds}.
     */
    private static void showToViewer(MoreTntPlusApi api, Player player, World world, BlockChangeSet beams,
                                     int beamSeconds) {
        ClientEffects client = api.clientEffects();
        client.showBlocks(player, world, beams);
        client.setClientWeather(player, false, false);
        client.setClientTime(player, NOON);
        Tasks.repeat(api.plugin(), 20L, DAY_HOLD_SECONDS + DAY_RETURN_SECONDS, step -> {
            if (step < DAY_HOLD_SECONDS || !player.isOnline()) {
                return;
            }
            double progress = (step - DAY_HOLD_SECONDS + 1) / (double) DAY_RETURN_SECONDS;
            long turn = shortestTurn(world.getTime() - NOON);
            client.setClientTime(player, NOON + Math.round(turn * progress));
        }, () -> client.resetClientTime(player));
        api.scheduleLater(beamSeconds * 20L, () -> {
            if (player.isOnline()) {
                client.restoreBlocks(player, world, beams);
                client.setClientWeather(player, world.hasStorm(), world.isThundering());
            }
            client.resetClientTime(player);
        });
    }

    /** The turn from {@link #NOON} to the real time that is shortest around the day, in ticks. */
    private static long shortestTurn(long difference) {
        return Math.floorMod(difference + DAY_LENGTH / 2, DAY_LENGTH) - DAY_LENGTH / 2;
    }

    /**
     * A 5x5 array of beacons, three blocks apart, around the blast centre. Each beacon needs two things on the client
     * before it draws a beam:
     * <ul>
     *   <li>a pyramid: the client only lists beam sections once the beacon has at least one level, and a level needs
     *       a 3x3 layer of beacon-base blocks (iron here) directly under it. The layer hangs one block above the
     *       ground: the blast changes ground blocks and the server sends those changes to every viewer, which would
     *       overwrite client-only blocks on the same spots. Air above the ground never changes, so it is safe;</li>
     *   <li>a clear path to the sky: the beam scan stops at the surface height, which on the client counts the beacon
     *       itself, so each column also gets an invisible light block at the top of the world. A light block at level
     *       0 lets the beam through.</li>
     * </ul>
     * The blocks exist only in the player's client, sent with PacketEvents: restoring the view is re-sending the real
     * blocks.
     */
    private static BlockChangeSet beaconArray(World world, Location origin) {
        BlockData beacon = Material.BEACON.createBlockData();
        BlockData base = Material.IRON_BLOCK.createBlockData();
        Light sky = (Light) Material.LIGHT.createBlockData();
        sky.setLevel(0);
        int top = world.getMaxHeight() - 1;
        BlockChangeSet set = new BlockChangeSet(250);
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                int x = origin.getBlockX() + i * 3;
                int z = origin.getBlockZ() + j * 3;
                int y = Math.min(top - 2, world.getHighestBlockYAt(x, z) + 2);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        set.set(x + dx, y - 1, z + dz, base);
                    }
                }
                set.set(x, y, z, beacon);
                set.set(x, top, z, sky);
            }
        }
        return set;
    }

    /**
     * The shape of the cut: its radius, the flattened reach, and the depth of its centre (saucer) or the rim sink
     * (sphere). The depth at a distance from the centre decides how far the ground is cut there.
     */
    record Cut(Shape shape, int radius, int reach, double depth, double sink) {

        /** How far below the surface the ground is cut at {@code distance} blocks from the centre. */
        double depthAt(double distance) {
            if (distance <= radius) {
                return shape == Shape.SAUCER
                        ? depth * (1 - square(distance / radius))
                        : Math.sqrt(square(radius) - square(distance));
            }
            if (shape == Shape.SPHERE && sink > 0 && distance < reach) {
                return sink * square(1 - (distance - radius) / (reach - radius));
            }
            return 0;
        }

        private static double square(double value) {
            return value * value;
        }
    }

    /**
     * Cuts the ground one column at a time out to the flatten reach. Inside the cut the ground is pushed down, into a
     * saucer or a hemisphere, and the exposed floor is baked (a saucer's floor is compacted coarse dirt, fused glass at
     * the centre). Everything above the ground out to the reach is cleared, so trees, grass and buildings flatten.
     * Sparse fire, and for a sphere magma and lava, is left on the floor. Protected blocks are never touched.
     */
    static final class Crater implements BlockGenerator {

        private final World world;
        private final int cx;
        private final int cz;
        private final Cut cut;
        private final int minY;
        private final ProtectionService protection;
        private final int side;
        private final int total;
        private final BlockChangeSet pending = new BlockChangeSet(64);
        private int pendingOffset;
        private int index;

        Crater(World world, int cx, int cz, Cut cut, int minY, ProtectionService protection) {
            this.world = world;
            this.cx = cx;
            this.cz = cz;
            this.cut = cut;
            this.minY = minY;
            this.protection = protection;
            this.side = 2 * cut.reach() + 1;
            this.total = side * side;
        }

        @Override
        public boolean next(BlockChangeSet out, int limit) {
            int written = 0;
            while (written < limit) {
                while (pendingOffset < pending.size() && written < limit) {
                    out.set(pending.x(pendingOffset), pending.y(pendingOffset), pending.z(pendingOffset),
                            pending.data(pendingOffset));
                    pendingOffset++;
                    written++;
                }
                if (pendingOffset < pending.size()) {
                    return true;
                }
                pending.clear();
                pendingOffset = 0;
                if (index >= total) {
                    return false;
                }
                int dx = index % side - cut.reach();
                int dz = index / side - cut.reach();
                index++;
                if (dx * dx + dz * dz <= cut.reach() * cut.reach()) {
                    column(cx + dx, cz + dz);
                }
            }
            return index < total || pendingOffset < pending.size();
        }

        private void column(int x, int z) {
            double distance = Math.hypot(x - cx, z - cz);
            int top = Math.min(world.getMaxHeight() - 1, world.getHighestBlockYAt(x, z));
            int ground = groundY(x, z, top);
            int depth = (int) Math.round(cut.depthAt(distance));
            int bottom = Math.max(minY, ground - depth);
            int lowest = Integer.MAX_VALUE;
            for (int y = top; y > bottom; y--) {
                Block block = world.getBlockAt(x, y, z);
                Material type = block.getType();
                if (type == Material.BEDROCK) {
                    break;
                }
                if (type.isAir() || type == Material.TNT || protection.isProtected(block)) {
                    continue;
                }
                pending.set(x, y, z, Material.AIR.createBlockData());
                lowest = y;
            }
            if (depth <= 0) {
                return;
            }
            if (cut.shape() == Shape.SAUCER && !protection.isProtected(world.getBlockAt(x, bottom, z))) {
                Material floor = distance <= cut.radius() * GLASS_CORE ? Material.GREEN_STAINED_GLASS
                        : Material.COARSE_DIRT;
                pending.set(x, bottom, z, floor.createBlockData());
            }
            if (lowest != Integer.MAX_VALUE && distance <= cut.radius()) {
                sprinkle(x, lowest, z);
            }
        }

        /** The top of the ground at a column: the highest solid block that is not a tree. */
        private int groundY(int x, int z, int top) {
            for (int y = top; y > minY; y--) {
                Material type = world.getBlockAt(x, y, z).getType();
                if (type.isSolid() && !Tag.LOGS.isTagged(type) && !Tag.LEAVES.isTagged(type)) {
                    return y;
                }
            }
            return minY;
        }

        private void sprinkle(int x, int y, int z) {
            double roll = ThreadLocalRandom.current().nextDouble();
            if (cut.shape() == Shape.SPHERE) {
                if (roll < 0.04) {
                    pending.set(x, y, z, Material.MAGMA_BLOCK.createBlockData());
                } else if (roll < 0.05) {
                    pending.set(x, y, z, Material.LAVA.createBlockData());
                } else if (roll < 0.08) {
                    pending.set(x, y, z, Material.FIRE.createBlockData());
                }
            } else if (roll < 0.03) {
                pending.set(x, y, z, Material.FIRE.createBlockData());
            }
        }
    }

    /**
     * Throws irregular clumps of stone, dirt and gravel onto the flattened ring around the cut, and sets fire to their
     * tops. Each clump is a short random walk so the shapes look thrown rather than placed.
     */
    static final class Debris implements BlockGenerator {

        private final World world;
        private final int cx;
        private final int cz;
        private final double innerRadius;
        private final double outerRadius;
        private final int clumps;
        private final ProtectionService protection;
        private final BlockChangeSet pending = new BlockChangeSet(64);
        private int pendingOffset;
        private int emitted;

        Debris(World world, int cx, int cz, double innerRadius, double outerRadius, int clumps,
               ProtectionService protection) {
            this.world = world;
            this.cx = cx;
            this.cz = cz;
            this.innerRadius = innerRadius * 1.1;
            this.outerRadius = outerRadius;
            this.clumps = clumps;
            this.protection = protection;
        }

        @Override
        public boolean next(BlockChangeSet out, int limit) {
            int written = 0;
            while (written < limit) {
                while (pendingOffset < pending.size() && written < limit) {
                    out.set(pending.x(pendingOffset), pending.y(pendingOffset), pending.z(pendingOffset),
                            pending.data(pendingOffset));
                    pendingOffset++;
                    written++;
                }
                if (pendingOffset < pending.size()) {
                    return true;
                }
                pending.clear();
                pendingOffset = 0;
                if (emitted >= clumps) {
                    return false;
                }
                emitted++;
                clump();
            }
            return emitted < clumps || pendingOffset < pending.size();
        }

        private void clump() {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = innerRadius + random.nextDouble() * (outerRadius - innerRadius);
            int x = cx + (int) Math.round(Math.cos(angle) * distance);
            int z = cz + (int) Math.round(Math.sin(angle) * distance);
            int size = 6 + random.nextInt(35);
            BlockData material = DEBRIS[random.nextInt(DEBRIS.length)].createBlockData();
            for (int step = 0; step < size; step++) {
                x += random.nextInt(3) - 1;
                z += random.nextInt(3) - 1;
                int y = world.getHighestBlockYAt(x, z) + 1 + random.nextInt(2);
                if (y >= world.getMaxHeight()) {
                    continue;
                }
                Block block = world.getBlockAt(x, y, z);
                if (!block.getType().isAir() || protection.isProtected(block)) {
                    continue;
                }
                pending.set(x, y, z, material);
                if (random.nextDouble() < 0.6) {
                    pending.set(x, y + 1, z, Material.FIRE.createBlockData());
                }
            }
        }
    }
}
