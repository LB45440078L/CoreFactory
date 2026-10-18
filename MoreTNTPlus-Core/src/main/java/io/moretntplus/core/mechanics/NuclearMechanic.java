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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
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
 *   <li>within 3R: the client's clock is set to noon for {@code beam-seconds} and a 5x5 array of beacons
 *       draws its beams for that player only (PacketEvents);</li>
 *   <li>0.5R or less: killed instantly;</li>
 *   <li>0.5R to 0.8R: burning for {@code fire-far-seconds};</li>
 *   <li>0.8R to R: burning for {@code fire-near-seconds} and nausea.</li>
 * </ul>
 * The crater is a circle of radius {@code crater-fraction} x R, cut straight down to bedrock, and is streamed
 * over several ticks so the server never stalls. Debris lands on the surface around it, on fire.
 */
final class NuclearMechanic extends AbstractMechanic {

    private static final Material[] DEBRIS = {
            Material.STONE, Material.DIRT, Material.ANDESITE, Material.COBBLESTONE,
            Material.GRAVEL, Material.DIORITE, Material.GRANITE
    };

    NuclearMechanic(Toolkit kit) {
        super("nuclear", "Gradual nuclear yields: countdown, instant kills, crater to bedrock, debris and fire.", kit);
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

        double craterRadius = radius * ctx.settings().getDouble("crater-fraction", 0.45);
        BlockGenerator crater = new Crater(world, origin.getBlockX(), origin.getBlockZ(), (int) Math.round(craterRadius),
                world.getMinHeight(), api.protection());
        int debrisCount = Math.max(1, ctx.settings().getInt("debris-count", (int) (radius * 3)));
        BlockGenerator debris = new Debris(world, origin.getBlockX(), origin.getBlockZ(), craterRadius, radius,
                debrisCount, api.protection());
        api.submitBlockGenerator(world, crater, () -> api.submitBlockGenerator(world, debris, () -> {
        }));

        double loud = radius * 3.0;
        for (Player player : kit.effects().nearby(origin, loud)) {
            player.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 10.0f, 0.5f);
        }
        kit.explosions().damage(origin, radius, damage(ctx, 60.0), ctx.source());
    }

    private static void showToViewer(MoreTntPlusApi api, Player player, World world, BlockChangeSet beams,
                                     int beamSeconds) {
        ClientEffects client = api.clientEffects();
        client.setClientTime(player, 6000L);
        client.showBlocks(player, world, beams);
        api.scheduleLater(beamSeconds * 20L, () -> {
            if (player.isOnline()) {
                client.restoreBlocks(player, world, beams);
                client.resetClientTime(player);
            }
        });
    }

    /**
     * A 5x5 array of beacons, three blocks apart, standing on the surface around the blast centre. Each beacon draws
     * its own beam straight up to the sky. The beacons exist only in the player's client, sent with PacketEvents:
     * the server never changes them, so restoring the player's view is just re-sending the real blocks.
     */
    private static BlockChangeSet beaconArray(World world, Location origin) {
        BlockData beacon = Material.BEACON.createBlockData();
        BlockChangeSet set = new BlockChangeSet(25);
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                int x = origin.getBlockX() + i * 3;
                int z = origin.getBlockZ() + j * 3;
                int y = Math.min(world.getMaxHeight() - 1, world.getHighestBlockYAt(x, z) + 1);
                set.set(x, y, z, beacon);
            }
        }
        return set;
    }

    /**
     * Clears the circular hole from the surface down to bedrock, one column at a time. Sparse magma, lava and
     * fire are placed on the hole's floor.
     */
    static final class Crater implements BlockGenerator {

        private final World world;
        private final int cx;
        private final int cz;
        private final int radius;
        private final int minY;
        private final ProtectionService protection;
        private final int side;
        private final int total;
        private final BlockChangeSet pending = new BlockChangeSet(64);
        private int pendingOffset;
        private int index;

        Crater(World world, int cx, int cz, int radius, int minY, ProtectionService protection) {
            this.world = world;
            this.cx = cx;
            this.cz = cz;
            this.radius = radius;
            this.minY = minY;
            this.protection = protection;
            this.side = 2 * radius + 1;
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
                int dx = index % side - radius;
                int dz = index / side - radius;
                index++;
                if (dx * dx + dz * dz <= radius * radius) {
                    column(cx + dx, cz + dz);
                }
            }
            return index < total || pendingOffset < pending.size();
        }

        private void column(int x, int z) {
            int top = Math.min(world.getMaxHeight() - 1, world.getHighestBlockYAt(x, z));
            int floor = Integer.MAX_VALUE;
            for (int y = top; y >= minY; y--) {
                Block block = world.getBlockAt(x, y, z);
                Material type = block.getType();
                if (type == Material.BEDROCK) {
                    break;
                }
                if (type.isAir() || type == Material.TNT || protection.isProtected(block)) {
                    continue;
                }
                pending.set(x, y, z, Material.AIR.createBlockData());
                floor = y;
            }
            if (floor == Integer.MAX_VALUE) {
                return;
            }
            double roll = ThreadLocalRandom.current().nextDouble();
            if (roll < 0.04) {
                pending.set(x, floor, z, Material.MAGMA_BLOCK.createBlockData());
            } else if (roll < 0.05) {
                pending.set(x, floor, z, Material.LAVA.createBlockData());
            } else if (roll < 0.08) {
                pending.set(x, floor, z, Material.FIRE.createBlockData());
            }
        }
    }

    /**
     * Throws irregular clumps of stone, dirt and gravel onto the ground around the crater, and sets fire to their
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

        Debris(World world, int cx, int cz, double innerRadius, double radius, int clumps,
               ProtectionService protection) {
            this.world = world;
            this.cx = cx;
            this.cz = cz;
            this.innerRadius = innerRadius * 1.1;
            this.outerRadius = radius * 1.25;
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
