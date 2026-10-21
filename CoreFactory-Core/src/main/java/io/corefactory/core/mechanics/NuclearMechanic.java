package io.corefactory.core.mechanics;

import io.corefactory.api.BlockChangeSet;
import io.corefactory.api.BlockGenerator;
import io.corefactory.api.ClientEffects;
import io.corefactory.api.DetonationContext;
import io.corefactory.api.CoreFactoryApi;
import io.corefactory.api.ProtectionService;
import io.corefactory.api.TntDefinition;
import io.corefactory.core.engine.Tasks;
import io.corefactory.core.radiation.Radiation;
import io.corefactory.core.radiation.RadiationSuit;
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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The nuclear family. Every tier shares this mechanic and differs only in radius, fuse and settings in
 * {@code tnts.yml}. Each tier's {@code yield-megatons} sets its fallout and whether its clients see beacons.
 *
 * <p>Three zones, by distance from the centre as a fraction of the radius R:
 * <ul>
 *   <li>the fireball, within 0.3R: instant death, whatever the armour or health;</li>
 *   <li>the blast area, out to R: damage that falls off from the fireball's edge to R, reduced by blast protection
 *       and by the radioactive suit, and burning for {@code blast-fire-seconds} unless fire resistance is active;</li>
 *   <li>the fallout, out to 3R: a radiation dose that builds while standing in it, with nausea and health loss as it
 *       grows, and that lasts for hours (see {@link Radiation}).</li>
 * </ul>
 * Each area is named in the countdown and the detonation titles. For a yield of
 * {@link #BEACON_MEGATONS} or more, a 5x5 array of beacons draws beams to the sky, for the player's client only, and
 * the rain stops and the clock holds at noon, then slides back to the real time (PacketEvents). Smaller blasts show
 * only the weather and clock change.
 *
 * <p>The ground is cut by one of two shapes (see {@link Shape}) and everything above it out to the flatten radius is
 * cleared. The cut is streamed over several ticks so the server never stalls. Debris lands on the flattened ring
 * around it, on fire.
 */
final class NuclearMechanic extends AbstractMechanic {

    /** The mechanic id in tnts.yml. */
    static final String ID = "nuclear";

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
    /** The fireball's radius as a fraction of the blast radius. Inside it nothing survives. */
    private static final double FIREBALL_FRACTION = 0.3;
    /** The fallout's radius as a multiple of the blast radius. */
    private static final double FALLOUT_FRACTION = 3.0;
    /** The yield, in megatons, from which clients see the beacon beams. */
    static final double BEACON_MEGATONS = 10.0;
    /** The yield range the mushroom cloud's height is scaled over. */
    private static final double MUSHROOM_MIN_MEGATONS = 0.5;
    private static final double MUSHROOM_MAX_MEGATONS = 50.0;
    /** The ground surge's radius as a fraction of the blast radius. It rolls out just inside the shockwave. */
    private static final double SURGE_FRACTION = 0.8;
    /** The most blast protection can absorb, as a share of the damage. */
    private static final double MAX_REDUCTION = 0.8;
    /** The share of blast damage one level of blast protection absorbs. */
    private static final double REDUCTION_PER_LEVEL = 0.08;
    /** The least share of the peak damage a player takes at the outer edge of the blast area. */
    private static final double BLAST_EDGE = 0.1;

    /** The cut's shape. A saucer is a shallow bowl from a surface burst; a sphere is a deep hemispherical hole. */
    enum Shape {
        SAUCER, SPHERE;

        static Shape of(String name) {
            return "sphere".equalsIgnoreCase(name) ? SPHERE : SAUCER;
        }
    }

    NuclearMechanic(Toolkit kit) {
        super(ID, "Gradual nuclear yields: countdown, instant kills, crater, debris and fire.", kit);
    }

    @Override
    public void onIgnite(Location origin, TntDefinition tnt, CoreFactoryApi api) {
        int seconds = Math.max(1, tnt.fuseTicks() / 20);
        double radius = tnt.radius();
        double warning = radius * FALLOUT_FRACTION;
        Tasks.repeat(api.plugin(), 20L, seconds, step -> {
            int remaining = seconds - step;
            for (Player player : kit.effects().nearby(origin, warning)) {
                Optional<Zone> zone = Zone.of(player.getLocation().distance(origin), radius);
                zone.ifPresent(area -> player.sendTitle(area.title(), "§eT-minus " + remaining + "s", 0, 25, 5));
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
        CoreFactoryApi api = ctx.api();
        Radiation radiation = kit.radiation();
        double megatons = ctx.settings().getDouble("yield-megatons", 0.0);
        int fireSeconds = Math.max(1, ctx.settings().getInt("blast-fire-seconds", 30));
        int beamSeconds = Math.max(1, ctx.settings().getInt("beam-seconds", 30));
        double peak = damage(ctx, 60.0);
        double fireball = radius * FIREBALL_FRACTION;
        kit.effects().visual(ctx.settings().getString("visual", "nuclear_fireball"), origin, fireball);
        kit.effects().visual("nuclear_shockwave", origin, radius);
        kit.effects().visual("nuclear_surge", origin, radius * SURGE_FRACTION);
        kit.effects().visual("nuclear_mushroom", origin, mushroomHeight(megatons));
        if (megatons >= 1.0) {
            kit.effects().visual("nuclear_fallout", origin, radius * FALLOUT_FRACTION);
        }

        BlockChangeSet beams = megatons >= BEACON_MEGATONS ? beaconArray(world, origin) : null;
        for (Player player : world.getPlayers()) {
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            double distance = player.getLocation().distance(origin);
            Optional<Zone> zone = Zone.of(distance, radius);
            if (zone.isEmpty()) {
                continue;
            }
            if (zone.get() == Zone.FIREBALL) {
                player.setHealth(0.0);
                continue;
            }
            if (zone.get() == Zone.BLAST) {
                blast(player, distance, radius, fireball, peak, fireSeconds, radiation.suit());
                if (player.isDead()) {
                    continue;
                }
            }
            player.sendTitle(zone.get().title(), "§7Get underground. Now.", 5, 70, 20);
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 10.0f, 0.6f);
            showToViewer(api, player, world, beams, beamSeconds);
        }
        radiation.fallout().add(world, origin.getX(), origin.getZ(), radius * FALLOUT_FRACTION, megatons,
                api.currentTick());

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
     * The mushroom cloud's height in blocks: a base of 10 plus 12 for each doubling of the yield, which is clamped to the
     * mushroom's range. A 50 megaton blast reaches about 78 blocks, well above its crater.
     */
    static double mushroomHeight(double megatons) {
        double yield = Math.clamp(megatons, MUSHROOM_MIN_MEGATONS, MUSHROOM_MAX_MEGATONS);
        return 10 + 12 * Math.log(1 + yield) / Math.log(2);
    }

    /**
     * What a viewer in the fallout sees: the beacon array when there is one, no rain, and the clock held at noon. After
     * {@link #DAY_HOLD_SECONDS} the clock slides back to the real time over {@link #DAY_RETURN_SECONDS} and is then
     * handed back to the server. The beams and the weather end after {@code beamSeconds}.
     *
     * @param beams the beacon array, or null when the yield is too small for beams
     */
    private static void showToViewer(CoreFactoryApi api, Player player, World world, BlockChangeSet beams,
                                     int beamSeconds) {
        ClientEffects client = api.clientEffects();
        if (beams != null) {
            client.showBlocks(player, world, beams);
        }
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
                if (beams != null) {
                    client.restoreBlocks(player, world, beams);
                }
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
     * A player in the blast area: the damage falls from its peak at the fireball's edge to a tenth of it at the outer
     * edge. Blast protection and the suit absorb a share of it, and the player burns unless they are fire resistant.
     */
    private static void blast(Player player, double distance, double radius, double fireball, double peak,
                              int fireSeconds, RadiationSuit suit) {
        double span = radius - fireball;
        double falloff = Math.clamp(1.0 - (distance - fireball) / span, BLAST_EDGE, 1.0);
        double reduction = Math.min(MAX_REDUCTION, blastProtection(player) + suit.blastReduction(player));
        player.damage(peak * falloff * (1.0 - reduction));
        if (!player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
            player.setFireTicks(fireSeconds * 20);
        }
    }

    /** The share of blast damage the worn armour's blast protection absorbs, at most {@link #MAX_REDUCTION}. */
    private static double blastProtection(Player player) {
        int levels = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece != null) {
                levels += piece.getEnchantmentLevel(Enchantment.BLAST_PROTECTION);
            }
        }
        return Math.min(MAX_REDUCTION, levels * REDUCTION_PER_LEVEL);
    }

    /** The three areas of a blast, from the centre out, each with the title a player in it sees. */
    enum Zone {
        FIREBALL("§4§lYOU ARE IN THE FIREBALL RADIUS - INSTANT DEATH"),
        BLAST("§c§lYOU ARE IN THE BLAST AREA - HIGH DAMAGE"),
        FALLOUT("§6§lYOU ARE IN THE RADIATION FALLOUT AREA; LEAVE IMMEDIATELY");

        private final String title;

        Zone(String title) {
            this.title = title;
        }

        String title() {
            return title;
        }

        /** The area a distance from the centre falls in, for a blast of radius {@code radius}. */
        static Optional<Zone> of(double distance, double radius) {
            if (distance <= radius * FIREBALL_FRACTION) {
                return Optional.of(FIREBALL);
            }
            if (distance <= radius) {
                return Optional.of(BLAST);
            }
            if (distance <= radius * FALLOUT_FRACTION) {
                return Optional.of(FALLOUT);
            }
            return Optional.empty();
        }
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
