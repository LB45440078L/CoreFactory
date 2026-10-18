package io.moretntplus.spigot.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.moretntplus.core.engine.ClaimGuard;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Respects WorldGuard's {@code tnt} flag: a block inside a region that denies TNT is never changed by a blast.
 *
 * <p>Cost stays low. Per chunk and per tick, one overlap query decides whether any region with TNT rules touches the
 * chunk at all. Chunks with no such regions, which is most of the map, cost one map lookup per block and nothing more.
 */
public final class WorldGuardGuard implements ClaimGuard {

    private record ChunkKey(UUID world, int x, int z) {
    }

    private final Map<ChunkKey, Boolean> chunkHasRules = new HashMap<>();

    @Override
    public void beginTick() {
        chunkHasRules.clear();
    }

    @Override
    public boolean protects(Block block) {
        World world = block.getWorld();
        ChunkKey key = new ChunkKey(world.getUID(), block.getX() >> 4, block.getZ() >> 4);
        Boolean active = chunkHasRules.get(key);
        if (active == null) {
            active = hasTntRules(world, key.x(), key.z());
            chunkHasRules.put(key, active);
        }
        if (!active) {
            return false;
        }
        StateFlag.State state = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
                .queryState(BukkitAdapter.adapt(block.getLocation()), (LocalPlayer) null, Flags.TNT);
        return state == StateFlag.State.DENY;
    }

    private static boolean hasTntRules(World world, int chunkX, int chunkZ) {
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));
        if (manager == null) {
            return false;
        }
        ProtectedRegion global = manager.getRegion("__global__");
        if (global != null && global.getFlag(Flags.TNT) == StateFlag.State.DENY) {
            return true;
        }
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        ProtectedCuboidRegion chunk = new ProtectedCuboidRegion("moretntplus-probe",
                BlockVector3.at(minX, world.getMinHeight(), minZ),
                BlockVector3.at(minX + 15, world.getMaxHeight() - 1, minZ + 15));
        ApplicableRegionSet overlapping = manager.getApplicableRegions(chunk);
        return overlapping.size() > 0;
    }
}
