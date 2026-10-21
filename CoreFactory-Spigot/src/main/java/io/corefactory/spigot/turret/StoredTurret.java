package io.corefactory.spigot.turret;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.turret.RoundType;
import io.corefactory.core.turret.TargetPolicy;

import java.util.UUID;

/**
 * A turret as {@code turrets.yml} holds it: its id, its kind, the player who placed it, its block, the way it faces when placed, the rounds
 * it holds, its level, and its targeting choices. A laser turret holds no rounds, so its round type is null and its round count zero.
 *
 * @param id     the turret's id, unique among the turrets
 * @param kind   the kind of turret
 * @param owner  the player who placed it
 * @param world  the id of the world it stands in
 * @param pos    the block it stands in: the lower half of a tall turret
 * @param facing the yaw it faces when placed; the centre of its arc
 * @param round  the round type it holds, or null
 * @param rounds the rounds it holds
 * @param level  the upgrade level, from 1 to 5
 * @param policy the turret's friendly-fire switch, its per-turret target overrides, its player stances and its team mode
 */
public record StoredTurret(String id, TurretKind kind, UUID owner, UUID world, Pos pos, double facing, RoundType round, int rounds,
                           int level, TargetPolicy policy) {
}
