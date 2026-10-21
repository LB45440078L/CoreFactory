package io.corefactory.spigot.turret;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.turret.ArmourBypass;
import io.corefactory.core.turret.BeamPlan;
import io.corefactory.core.turret.DualTargeting;
import io.corefactory.core.turret.DualTargeting.Spot;
import io.corefactory.core.turret.FireRate;
import io.corefactory.core.turret.LaserRules;
import io.corefactory.core.turret.Magazine;
import io.corefactory.core.turret.RoundType;
import io.corefactory.core.turret.TargetKind;
import io.corefactory.core.turret.TargetPolicy;
import io.corefactory.core.turret.TargetRules;
import io.corefactory.core.turret.TargetRules.Candidate;
import io.corefactory.core.turret.TargetRules.Point;
import io.corefactory.core.turret.TargetRules.Relation;
import io.corefactory.core.turret.TargetRules.Verdict;
import io.corefactory.core.turret.TurretAim;
import io.corefactory.core.turret.TurretLevels;
import io.corefactory.core.turret.TurretSettings;
import io.corefactory.spigot.energy.EnergyService;
import io.corefactory.spigot.energy.EnergySnapshots;
import io.corefactory.spigot.friends.Friends;
import io.corefactory.spigot.hologram.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Golem;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.UUID;

/**
 * The runtime of the turrets: the machine guns and the laser turrets. One scheduled task calls {@link #tick} every server tick, and the tick
 * serves every turret: each loaded turret is checked for power, its cannons take their targets, turn toward them or sweep the arc, and fire
 * when a barrel is on its target and its interval is up. A machine gun fires a round from its magazine, from one cannon, or from two at levels
 * 4 and 5; a laser fires an instant beam from its one cannon and needs no rounds.
 *
 * <p>A turret's stats come from its level: range, fire interval, ammunition, power, cannons and damage. A target is a mob or a player that
 * {@link TargetRules} allows, with the range measured as a sphere around the turret's head to the nearest point of the target's box. A cannon
 * keeps its target while it stays valid. When a cannon has none, and the search interval is up, the turret searches with
 * {@link DualTargeting#select}: the first cannon takes the nearest target in range, and the second one takes a target within the dual spacing
 * of the first. The turret's power demand is a consumer of the energy network while it can fire, and its fire interval grows as its
 * satisfaction falls.
 */
public final class TurretService implements EnergyService.ConsumerSource {

    private static final int ENSURE_TICKS = 20;
    private static final int TEXT_TICKS = 10;
    private static final int ROTATE_SOUND_TICKS = 12;
    private static final int FIRE_SOUND_TICKS = 3;
    private static final int CHARGE_SOUND_TICKS = 20;
    private static final int SAVE_TICKS = 300;
    private static final double SWEEP_FRACTION = 0.25;
    private static final double TURN_EPSILON = 0.01;
    private static final double DRAW_EPSILON = 0.25;
    private static final double SHOT_CLEAR = 0.5;
    private static final double BARREL_TO_MUZZLE = 0.6;
    private static final double RAY_SIZE = 0.1;

    private final CoreFactoryCore core;
    private final TurretRegistry registry;
    private final EnergyService energy;
    private final TurretProtection protection;
    private final Friends friends;
    private final TurretStore store;
    private final TurretStats stats;
    private final TurretEffects effects = new TurretEffects();
    private final LaserEffects laserEffects = new LaserEffects();
    private final LaserLight laserLight;
    private final Random random;
    private long ticks;

    /**
     * @param core       the core, for the items and the component definitions
     * @param registry   the turrets and their entities
     * @param energy     the energy network, which reads each turret's demand and gives its satisfaction
     * @param protection the claim systems a turret respects, and the claims that name a turret's friends
     * @param friends    the internal friend lists of the owners
     * @param store      the file the turrets are saved in
     * @param random     the random numbers of the spread; a seeded one makes the shots repeatable
     * @param stats      the stats of the turrets, by kind and level
     */
    public TurretService(CoreFactoryCore core, TurretRegistry registry, EnergyService energy, TurretProtection protection,
                         Friends friends, TurretStore store, Random random, TurretStats stats) {
        this.core = core;
        this.registry = registry;
        this.energy = energy;
        this.protection = protection;
        this.friends = friends;
        this.store = store;
        this.random = random;
        this.stats = stats;
        this.laserLight = new LaserLight(protection);
    }

    /** The stats the turrets run with: the general settings, and the level table of each kind. */
    public TurretStats stats() {
        return stats;
    }

    /** The general settings of the machine gun turrets, which the laser shares for targeting and turning. */
    public TurretSettings settings() {
        return stats.settings();
    }

    /** The turrets and their entities. */
    public TurretRegistry registry() {
        return registry;
    }

    /** Every turret that takes power while it can fire: its demand is the power demand of its kind and level. */
    @Override
    public Map<UUID, List<Consumer>> consumers() {
        Map<UUID, List<Consumer>> byWorld = new HashMap<>();
        if (!stats.settings().enabled()) {
            return byWorld;
        }
        for (TurretDevice turret : registry.all()) {
            if (!turret.isLoaded() || !isArmed(turret)) {
                continue;
            }
            Pos pos = turret.pos();
            String id = EnergySnapshots.consumerId(turret.world(), pos.x(), pos.y(), pos.z());
            byWorld.computeIfAbsent(turret.world(), key -> new ArrayList<>())
                    .add(new Consumer(id, pos, viewOf(turret).powerDemandW()));
        }
        return byWorld;
    }

    /**
     * Runs one server tick for every turret. Called by the one scheduled task of the turrets.
     */
    public void tick() {
        ticks++;
        if (!stats.settings().enabled()) {
            return;
        }
        protection.beginTick();
        effects.beginTick();
        laserEffects.beginTick();
        laserLight.tick(ticks);
        if (ticks % ENSURE_TICKS == 0) {
            registry.ensureEntities();
        }
        for (TurretDevice turret : registry.all()) {
            if (turret.isLoaded()) {
                tickOne(turret);
            }
        }
        if (ticks % SAVE_TICKS == 0) {
            save();
        }
    }

    /**
     * Places a turret on a block, and saves.
     *
     * @param kind  the kind of turret; a laser turret holds no rounds
     * @param level the level it is placed at, from 1 to 5: a turret item of a higher level places a higher turret
     */
    public TurretDevice place(Location block, UUID owner, double facing, TurretKind kind, int level) {
        int clamped = clampLevel(level);
        TurretDevice turret = registry.add(new TurretDevice(UUID.randomUUID().toString(), kind, owner, block, facing,
                Magazine.empty(stats.magazineCapacity(kind, clamped)), clamped));
        save();
        return turret;
    }

    /**
     * Removes a turret, and saves.
     *
     * @return the items it gives back: its turret item at its level, and the rounds it held, in stacks
     */
    public List<ItemStack> remove(TurretDevice turret) {
        List<ItemStack> drops = new ArrayList<>();
        registry.itemOf(turret).ifPresent(drops::add);
        drops.addAll(registry.roundStacks(turret.magazine()));
        registry.remove(turret);
        save();
        return drops;
    }

    /**
     * Moves a turret up to a level, and saves. The rounds it holds stay, and a machine gun's belt takes the new level's capacity. Its entities
     * are made again for the new height, and its targets are dropped.
     *
     * @param level the level it moves to, from 2 to 5
     */
    public void upgrade(TurretDevice turret, int level) {
        Magazine magazine = turret.magazine();
        turret.setLevel(clampLevel(level));
        if (turret.kind() == TurretKind.MACHINE_GUN) {
            turret.setMagazine(new Magazine(magazine.type(), magazine.count(), stats.magazineCapacity(turret.kind(), turret.level())));
        }
        dropTargets(turret);
        registry.rebuild(turret);
        save();
    }

    /**
     * Adds rounds of a type to a turret, up to its room, and saves. A laser turret takes no rounds.
     *
     * @return the rounds taken; zero when the magazine holds another type or is full
     */
    public int load(TurretDevice turret, RoundType type, int amount) {
        if (turret.kind() != TurretKind.MACHINE_GUN) {
            return 0;
        }
        Magazine magazine = turret.magazine();
        int take = Math.min(amount, magazine.roomFor(type));
        if (take <= 0) {
            return 0;
        }
        turret.setMagazine(magazine.add(type, take));
        TurretSounds.reload(turret.barrel());
        save();
        return take;
    }

    /**
     * Moves rounds from a player's inventory into a turret: one round type, up to the room the turret has for it. An empty turret takes the
     * type of the first round found. A laser turret takes no rounds.
     *
     * @return the rounds taken
     */
    public int loadFromInventory(TurretDevice turret, Player player) {
        if (turret.kind() != TurretKind.MACHINE_GUN) {
            return 0;
        }
        PlayerInventory inventory = player.getInventory();
        Magazine magazine = turret.magazine();
        RoundType wanted = magazine.type();
        if (wanted == null) {
            wanted = firstRoundIn(inventory).orElse(null);
        }
        if (wanted == null || magazine.roomFor(wanted) < 1) {
            return 0;
        }
        RoundType type = wanted;
        int room = magazine.roomFor(type);
        int taken = 0;
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length && taken < room; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || roundOf(stack).filter(type::equals).isEmpty()) {
                continue;
            }
            int from = Math.min(stack.getAmount(), room - taken);
            taken += from;
            ItemStack left = stack.clone();
            left.setAmount(stack.getAmount() - from);
            inventory.setItem(slot, left.getAmount() > 0 ? left : null);
        }
        if (taken > 0) {
            turret.setMagazine(magazine.add(type, taken));
            TurretSounds.reload(turret.barrel());
            save();
        }
        return taken;
    }

    /**
     * Takes every round out of a turret, and saves. The rounds go to the player's inventory, and what does not fit drops at the player.
     *
     * @return the rounds taken out
     */
    public int eject(TurretDevice turret, Player player) {
        List<ItemStack> stacks = registry.roundStacks(turret.magazine());
        int count = turret.magazine().count();
        turret.setMagazine(turret.magazine().ejectAll());
        for (ItemStack stack : stacks) {
            for (ItemStack leftover : player.getInventory().addItem(stack).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        save();
        return count;
    }

    /**
     * Sets the targeting choices of a turret: its friendly-fire switch, its overrides, its player stances and its team mode. The held targets
     * are dropped, so the turret searches again under the new choices, and the turret is saved.
     */
    public void setPolicy(TurretDevice turret, TargetPolicy policy) {
        turret.setPolicy(policy);
        dropTargets(turret);
        save();
    }

    /**
     * Sets back the light and the glow of the laser shots that are standing in a chunk that is unloading, so none is left in the saved chunk.
     */
    public void releaseLightsIn(Chunk chunk) {
        laserLight.restoreIn(chunk);
    }

    /** Writes every turret to the store file. */
    public void save() {
        store.write(registry.snapshot());
    }

    /** Sets back the lights, saves, and releases the entities of every turret. Called when the plugin stops. */
    public void shutdown() {
        laserLight.restoreAll();
        save();
        registry.releaseAll();
    }

    /** The level a turret may have, from 1 to the highest. */
    static int clampLevel(int level) {
        return Math.max(1, Math.min(level, TurretLevels.MAX_LEVEL));
    }

    /** The ids of the energy consumer of a turret. */
    private static String consumerId(TurretDevice turret) {
        Pos pos = turret.pos();
        return EnergySnapshots.consumerId(turret.world(), pos.x(), pos.y(), pos.z());
    }

    /** The settings a turret runs with at its level: its range, interval, ammunition and power, with the general settings. */
    private TurretSettings viewOf(TurretDevice turret) {
        return stats.view(turret.kind(), turret.level());
    }

    /** True when a turret can fire at all: a laser always can, a machine gun needs rounds for one shot. */
    private boolean isArmed(TurretDevice turret) {
        return turret.kind() == TurretKind.LASER || turret.magazine().canFire(stats.settings().ammoPerShot());
    }

    private void tickOne(TurretDevice turret) {
        TurretSettings view = viewOf(turret);
        if (!isArmed(turret)) {
            dropTargets(turret);
            turret.setSearchIn(0);
            turret.setSatisfaction(0.0);
            turret.setStatus(TurretStatus.NO_ROUNDS);
            refreshText(turret);
            return;
        }
        double satisfaction = energy.satisfaction(consumerId(turret));
        turret.setSatisfaction(satisfaction);
        for (int cannon = 0; cannon < TurretDevice.CANNONS; cannon++) {
            if (turret.cooldown(cannon) > 0) {
                turret.setCooldown(cannon, turret.cooldown(cannon) - 1);
            }
        }
        if (turret.fireSoundIn() > 0) {
            turret.setFireSoundIn(turret.fireSoundIn() - 1);
        }
        if (turret.rotateSoundIn() > 0) {
            turret.setRotateSoundIn(turret.rotateSoundIn() - 1);
        }
        if (turret.chargeSoundIn() > 0) {
            turret.setChargeSoundIn(turret.chargeSoundIn() - 1);
        }
        if (satisfaction <= 0.0) {
            dropTargets(turret);
            turret.setSearchIn(0);
            turret.setStatus(TurretStatus.NO_POWER);
            refreshText(turret);
            return;
        }

        int cannons = stats.cannons(turret.kind(), turret.level());
        Location barrel = turret.barrel();
        LivingEntity[] targets = resolveTargets(turret, barrel, view, cannons);
        boolean engaged = targets[0] != null;
        double speed = view.rotationSpeedDegrees();
        double[][] aims = new double[cannons][];
        boolean turning = false;
        for (int cannon = 0; cannon < cannons; cannon++) {
            double yaw = turret.yaw(cannon);
            double pitch = turret.pitch(cannon);
            if (targets[cannon] != null) {
                aims[cannon] = aimTo(barrel, targets[cannon]);
                if (cannon == 0 && turret.kind() == TurretKind.LASER && turret.status() != TurretStatus.ENGAGING
                        && turret.chargeSoundIn() <= 0) {
                    TurretSounds.laserCharge(barrel);
                    turret.setChargeSoundIn(CHARGE_SOUND_TICKS);
                }
                yaw = TurretAim.step(yaw, aims[cannon][0], speed);
                pitch = TurretAim.step(pitch, aims[cannon][1], speed);
            } else {
                if (cannon == 0 && view.idleSweep()) {
                    TurretAim.Idle idle = TurretAim.idle(yaw, turret.facing(), view.arcDegrees(),
                            speed * SWEEP_FRACTION, turret.sweepingForward());
                    yaw = idle.yaw();
                    turret.setSweepingForward(idle.forward());
                }
                pitch = TurretAim.step(pitch, 0.0, speed);
            }
            turning |= Math.abs(TurretAim.wrap(yaw - turret.yaw(cannon))) > TURN_EPSILON
                    || Math.abs(pitch - turret.pitch(cannon)) > TURN_EPSILON;
            turret.aim(cannon, yaw, pitch);
        }
        turret.setStatus(engaged ? TurretStatus.ENGAGING : TurretStatus.IDLE);
        if (TurretAim.playsRotationSound(engaged, turning) && turret.rotateSoundIn() <= 0) {
            TurretSounds.rotate(barrel);
            turret.setRotateSoundIn(ROTATE_SOUND_TICKS);
        }
        drawAim(turret);

        for (int cannon = 0; cannon < cannons; cannon++) {
            if (aims[cannon] == null || turret.cooldown(cannon) > 0) {
                continue;
            }
            if (!TurretAim.aligned(turret.yaw(cannon), turret.pitch(cannon), aims[cannon][0], aims[cannon][1])) {
                continue;
            }
            OptionalInt interval = FireRate.intervalTicks(view.firingIntervalTicks(), satisfaction);
            if (interval.isEmpty()) {
                continue;
            }
            boolean fired = turret.kind() == TurretKind.LASER
                    ? fireLaser(turret, targets[cannon], barrel, view)
                    : fireRound(turret, cannon, targets[cannon], barrel, view);
            if (fired) {
                turret.setCooldown(cannon, interval.getAsInt());
            }
        }
        refreshText(turret);
    }

    /**
     * Resolves the targets of the cannons for this tick. A cannon keeps its target while it stays valid, and the held target of the first
     * cannon always leads. A cannon with no target is filled when the search is due: with the nearest target in range, or, for a second
     * cannon beside a held one, the nearest target within the dual spacing of it.
     *
     * @return the target of each cannon, null where a cannon has none
     */
    private LivingEntity[] resolveTargets(TurretDevice turret, Location barrel, TurretSettings view, int cannons) {
        LivingEntity[] kept = new LivingEntity[cannons];
        int held = 0;
        for (int cannon = 0; cannon < cannons; cannon++) {
            LivingEntity living = keepTarget(turret, cannon, barrel, view);
            if (living != null) {
                kept[held++] = living;
            }
        }
        if (held < cannons) {
            if (turret.searchIn() > 0) {
                turret.setSearchIn(turret.searchIn() - 1);
            } else {
                turret.setSearchIn(view.searchIntervalTicks());
                List<Spot<LivingEntity>> spots = spotsAround(turret, barrel, view, kept);
                List<LivingEntity> chosen;
                if (held == 0) {
                    chosen = DualTargeting.select(spots, cannons, stats.spacing());
                } else {
                    List<Spot<LivingEntity>> seeded = new ArrayList<>(spots.size() + 1);
                    seeded.add(new Spot<>(kept[0], nearestPointOf(barrel, kept[0]), true));
                    seeded.addAll(spots);
                    chosen = DualTargeting.select(seeded, cannons, stats.spacing());
                }
                for (int i = 0; i < chosen.size() && i < cannons; i++) {
                    if (kept[i] == null) {
                        kept[i] = chosen.get(i);
                    }
                }
            }
        }
        for (int cannon = 0; cannon < cannons; cannon++) {
            turret.setTarget(cannon, kept[cannon] == null ? null : kept[cannon].getUniqueId());
        }
        return kept;
    }

    /** The held target of a cannon while it is still valid; the target is dropped otherwise. */
    private LivingEntity keepTarget(TurretDevice turret, int cannon, Location barrel, TurretSettings view) {
        UUID id = turret.target(cannon);
        if (id == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(id);
        if (!(entity instanceof LivingEntity living) || living.isDead() || !living.getWorld().equals(barrel.getWorld())) {
            dropTarget(turret, cannon);
            return null;
        }
        if (verdictOf(turret, living, barrel, view) != Verdict.VALID) {
            dropTarget(turret, cannon);
            return null;
        }
        return living;
    }

    private static void dropTarget(TurretDevice turret, int cannon) {
        turret.setTarget(cannon, null);
        turret.setSearchIn(0);
    }

    private static void dropTargets(TurretDevice turret) {
        for (int cannon = 0; cannon < TurretDevice.CANNONS; cannon++) {
            dropTarget(turret, cannon);
        }
    }

    /**
     * The candidates of a search, nearest first, each with its nearest point and whether it can be fired at now: in range as a sphere around
     * the turret's head, a valid target under the rules, and a clear line from the barrel. Targets already held are left out.
     */
    private List<Spot<LivingEntity>> spotsAround(TurretDevice turret, Location barrel, TurretSettings view, LivingEntity[] held) {
        World world = barrel.getWorld();
        double range = view.range();
        List<Entity> near = new ArrayList<>(world.getNearbyEntities(barrel, range, range, range,
                entity -> entity instanceof LivingEntity));
        near.sort(Comparator.comparingDouble(entity -> distanceTo(barrel, (LivingEntity) entity)));
        List<Spot<LivingEntity>> spots = new ArrayList<>(near.size());
        for (Entity entity : near) {
            LivingEntity living = (LivingEntity) entity;
            if (isAmong(living, held)) {
                continue;
            }
            Point nearest = nearestPointOf(barrel, living);
            boolean fireable = verdictOf(turret, living, barrel, view) == Verdict.VALID;
            spots.add(new Spot<>(living, nearest, fireable));
        }
        return spots;
    }

    private static boolean isAmong(LivingEntity living, LivingEntity[] entities) {
        for (LivingEntity entity : entities) {
            if (entity != null && entity.getUniqueId().equals(living.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    /** Whether the verdict lets a round or a beam harm the entity it hits. Only the entities that are not protected and are hostile are harmed. */
    private static boolean mayHarm(Verdict verdict) {
        return verdict == Verdict.VALID || verdict == Verdict.OUT_OF_RANGE || verdict == Verdict.OUT_OF_ARC || verdict == Verdict.BLOCKED;
    }

    /**
     * The verdict of the targeting rule on an entity: its protection, its relation and team, the turret's own policy, the range as a
     * sphere around the turret's head to the nearest point of the entity's box, and the line of sight from the head to its centre.
     */
    private Verdict verdictOf(TurretDevice turret, LivingEntity living, Location barrel, TurretSettings view) {
        double[] aim = aimTo(barrel, living);
        boolean withinArc = TurretAim.inReach(aim[0], aim[1], turret.facing(), view);
        boolean protectedHere = protection.protects(living.getLocation());
        Relation relation = relationOf(turret, living);
        boolean teamMate = living instanceof Player player && teamMateOf(turret, player);
        Candidate candidate = candidateOf(living, aim[2], withinArc, protectedHere, relation, teamMate, clearShot(barrel, living));
        return TargetRules.verdict(candidate, view, turret.policy(), pointOf(barrel), nearestPointOf(barrel, living));
    }

    /**
     * How a player stands to the turret: its owner, a friend of the owner or trusted in the turret's claim, or anyone else. The owner is
     * always the owner. The friend checks run only while the turret's friendly-fire switch is on, since with the switch off no player is a
     * target anyway.
     */
    private Relation relationOf(TurretDevice turret, LivingEntity living) {
        if (!(living instanceof Player player)) {
            return Relation.NONE;
        }
        UUID id = player.getUniqueId();
        if (id.equals(turret.owner())) {
            return Relation.OWNER;
        }
        if (!turret.policy().friendlyFire()) {
            return Relation.OTHER;
        }
        if (friends.isFriend(turret.owner(), id) || protection.trusts(turret.block(), id, player.getName())) {
            return Relation.FRIEND;
        }
        return Relation.OTHER;
    }

    /**
     * True when a player is on the owner's scoreboard team. The owner's own scoreboard is read while the owner is online; while the owner is
     * away, the main scoreboard is read in its place.
     */
    private static boolean teamMateOf(TurretDevice turret, Player player) {
        Player owner = Bukkit.getPlayer(turret.owner());
        Scoreboard board;
        String ownerName;
        if (owner != null) {
            board = owner.getScoreboard();
            ownerName = owner.getName();
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(turret.owner());
            ownerName = offline.getName();
            board = Bukkit.getScoreboardManager() == null ? null : Bukkit.getScoreboardManager().getMainScoreboard();
        }
        if (board == null || ownerName == null) {
            return false;
        }
        Team team = board.getEntryTeam(ownerName);
        return team != null && team.hasEntry(player.getName());
    }

    private static Candidate candidateOf(LivingEntity living, double distance, boolean withinArc, boolean protectedHere,
                                         Relation relation, boolean teamMate, boolean clearShot) {
        boolean tamed = living instanceof Tameable tameable && tameable.isTamed()
                || living instanceof AbstractHorse horse && horse.isTamed();
        UUID playerId = living instanceof Player player ? player.getUniqueId() : null;
        return new Candidate(kindOf(living), living instanceof Player, living instanceof AbstractVillager,
                living instanceof Golem, living instanceof Animals, tamed, living.getCustomName() != null,
                protectedHere, distance, withinArc, living.getType().name(), relation, playerId, teamMate, clearShot);
    }

    private static TargetKind kindOf(LivingEntity living) {
        if (living instanceof Monster) {
            return TargetKind.MONSTER;
        }
        if (living instanceof Phantom) {
            return TargetKind.PHANTOM;
        }
        if (living instanceof Slime) {
            return TargetKind.SLIME;
        }
        if (living instanceof Ghast) {
            return TargetKind.GHAST;
        }
        return TargetKind.OTHER;
    }

    /** The point of a location, as the targeting rule reads it. */
    private static Point pointOf(Location location) {
        return new Point(location.getX(), location.getY(), location.getZ());
    }

    private static Point pointOf(Vector vector) {
        return new Point(vector.getX(), vector.getY(), vector.getZ());
    }

    /** The nearest point of an entity's bounding box to the turret's head. */
    private static Point nearestPointOf(Location barrel, LivingEntity living) {
        BoundingBox box = living.getBoundingBox();
        return TargetRules.nearestPoint(pointOf(barrel), pointOf(box.getMin()), pointOf(box.getMax()));
    }

    /** The yaw, pitch and distance from a barrel to the centre of an entity. */
    private static double[] aimTo(Location barrel, LivingEntity living) {
        BoundingBox box = living.getBoundingBox();
        Vector centre = box.getCenter();
        double dx = centre.getX() - barrel.getX();
        double dy = centre.getY() - barrel.getY();
        double dz = centre.getZ() - barrel.getZ();
        return new double[] {TurretAim.yawOf(dx, dz), TurretAim.pitchOf(dx, dy, dz), Math.sqrt(dx * dx + dy * dy + dz * dz)};
    }

    private static double distanceTo(Location barrel, LivingEntity living) {
        return aimTo(barrel, living)[2];
    }

    /** True when no block that solid stands between the barrel and the entity. */
    private static boolean clearShot(Location barrel, LivingEntity living) {
        Vector centre = living.getBoundingBox().getCenter();
        Vector toward = centre.clone().subtract(barrel.toVector());
        double distance = toward.length();
        if (distance < 1e-6) {
            return true;
        }
        RayTraceResult hit = barrel.getWorld().rayTraceBlocks(barrel, toward.clone().normalize(), distance,
                FluidCollisionMode.NEVER, true);
        return hit == null || hit.getHitPosition().distance(barrel.toVector()) >= distance - SHOT_CLEAR;
    }

    /**
     * Fires one round from a cannon at its target. The shot takes its rounds, flies along the cannon's spread, and harms the first entity it
     * reaches. The damage is the level's damage with its boost, whatever the round type: the round type gives the armour it bypasses, its fire
     * and its spread.
     *
     * @return false, and nothing is fired, when the magazine has no round for a shot or the line to the target is blocked
     */
    private boolean fireRound(TurretDevice turret, int cannon, LivingEntity target, Location barrel, TurretSettings view) {
        RoundType type = turret.magazine().type();
        if (type == null || !turret.magazine().canFire(view.ammoPerShot())) {
            return false;
        }
        if (!clearShot(barrel, target)) {
            dropTarget(turret, cannon);
            return false;
        }
        TurretSettings.Round round = stats.settings().round(type);
        double spread = view.accuracySpread() * round.spreadFactor();
        double yaw = turret.yaw(cannon) + TurretAim.spreadOffset(random.nextDouble() * 2.0 - 1.0, spread);
        double pitch = turret.pitch(cannon) + TurretAim.spreadOffset(random.nextDouble() * 2.0 - 1.0, spread);
        double[] direction = TurretAim.direction(yaw, pitch);
        Vector heading = new Vector(direction[0], direction[1], direction[2]);
        World world = barrel.getWorld();
        Location muzzle = barrel.clone().add(heading.clone().multiply(BARREL_TO_MUZZLE));

        turret.setMagazine(turret.magazine().fire(view.ammoPerShot()).orElse(turret.magazine()));
        RayTraceResult hit = world.rayTrace(muzzle, heading, view.range(), FluidCollisionMode.NEVER, true, RAY_SIZE,
                entity -> entity instanceof LivingEntity);
        Location end = hit != null ? hit.getHitPosition().toLocation(world)
                : muzzle.clone().add(heading.clone().multiply(view.range()));
        effects.shot(world, barrel, heading, end, type);
        fireSound(turret, barrel, false);
        if (hit != null && hit.getHitEntity() instanceof LivingEntity struck) {
            harm(turret, struck, barrel, view, stats.damage(TurretKind.MACHINE_GUN, turret.level()), round);
        }
        return true;
    }

    /** Deals one round's damage to an entity, when the targeting rule and the claims allow it. */
    private void harm(TurretDevice turret, LivingEntity struck, Location barrel, TurretSettings view, double damage,
                      TurretSettings.Round round) {
        Verdict verdict = verdictOf(turret, struck, barrel, view);
        if (!mayHarm(verdict)) {
            return;
        }
        struck.damage(ArmourBypass.adjustedDamage(damage, armourOf(struck), round.armourBypass()));
        if (round.fireTicks() > 0) {
            struck.setFireTicks(Math.max(struck.getFireTicks(), round.fireTicks()));
        }
    }

    /**
     * Fires one beam of a laser turret along its barrel. The beam needs no rounds and has no spread: it flies where the barrel points, and
     * stops on the first block or living entity on its line within range. It harms that entity when the targeting rule and the claims allow it.
     *
     * @return false, and nothing is fired, when a block stands between the barrel and the target
     */
    private boolean fireLaser(TurretDevice turret, LivingEntity target, Location barrel, TurretSettings view) {
        if (!clearShot(barrel, target)) {
            dropTarget(turret, 0);
            return false;
        }
        double[] direction = TurretAim.direction(turret.yaw(0), turret.pitch(0));
        Vector heading = new Vector(direction[0], direction[1], direction[2]);
        World world = barrel.getWorld();
        Location muzzle = barrel.clone().add(heading.clone().multiply(BARREL_TO_MUZZLE));
        RayTraceResult hit = world.rayTrace(barrel, heading, view.range(), FluidCollisionMode.NEVER, true, RAY_SIZE,
                entity -> entity instanceof LivingEntity);
        Location end = hit != null ? hit.getHitPosition().toLocation(world)
                : barrel.clone().add(heading.clone().multiply(view.range()));
        BeamPlan beam = BeamPlan.of(end.distance(muzzle), stats.laser().beamParticles());
        laserEffects.shot(world, muzzle, heading, beam, hit != null ? end : null, turret.level() >= TurretLevels.MAX_LEVEL);
        fireSound(turret, barrel, true);
        laserLight.flash(barrel, heading, turret.block(), ticks);
        if (turret.display() != null && Bukkit.getEntity(turret.display()) instanceof Display display) {
            laserLight.glow(display, ticks);
        }
        if (hit != null && hit.getHitEntity() instanceof LivingEntity struck) {
            harmWithBeam(turret, struck, barrel, view, stats.damage(TurretKind.LASER, turret.level()));
        }
        return true;
    }

    /** Deals one beam's damage to an entity, when the targeting rule and the claims allow it. The beam ignores the armour its share says. */
    private void harmWithBeam(TurretDevice turret, LivingEntity struck, Location barrel, TurretSettings view, double damage) {
        if (!mayHarm(verdictOf(turret, struck, barrel, view))) {
            return;
        }
        struck.damage(LaserRules.damageAfterArmour(damage, armourOf(struck), stats.laser().armourBypass()));
    }

    /**
     * Plays a turret's firing sound at the barrel of the cannon that fires. The sound is throttled by the turret's fire countdown, so two
     * cannons firing in the same tick play one sound.
     */
    private static void fireSound(TurretDevice turret, Location barrel, boolean laser) {
        if (turret.fireSoundIn() > 0) {
            return;
        }
        if (laser) {
            TurretSounds.laserFire(barrel);
        } else {
            TurretSounds.fire(barrel);
        }
        turret.setFireSoundIn(FIRE_SOUND_TICKS);
    }

    private static double armourOf(LivingEntity living) {
        AttributeInstance armour = living.getAttribute(Attribute.ARMOR);
        return armour == null ? 0.0 : armour.getValue();
    }

    /**
     * Turns the item displays to the barrel's yaw and pitch, when they moved enough to be worth a redraw. The lower half turns with the
     * first cannon's barrel in both yaw and pitch. The upper half of a tall turret turns with it in yaw only, as the mount above the barrel.
     */
    private void drawAim(TurretDevice turret) {
        double yaw = turret.yaw(0);
        double pitch = turret.pitch(0);
        if (!Double.isNaN(turret.shownYaw())
                && Math.abs(TurretAim.wrap(yaw - turret.shownYaw())) < DRAW_EPSILON
                && Math.abs(pitch - turret.shownPitch()) < DRAW_EPSILON) {
            return;
        }
        float scale = core.settings().displayScale();
        if (turret.display() != null && Bukkit.getEntity(turret.display()) instanceof ItemDisplay display) {
            Quaternionf left = new Quaternionf().rotationY((float) Math.toRadians(-yaw))
                    .rotateX((float) Math.toRadians(pitch));
            setTransform(display, left, scale);
        }
        if (turret.topDisplay() != null && Bukkit.getEntity(turret.topDisplay()) instanceof ItemDisplay top) {
            setTransform(top, new Quaternionf().rotationY((float) Math.toRadians(-yaw)), scale);
        }
        turret.setShown(yaw, pitch);
    }

    private static void setTransform(ItemDisplay display, Quaternionf left, float scale) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(2);
        display.setTransformation(new Transformation(new Vector3f(0f, 0f, 0f), left,
                new Vector3f(scale, scale, scale), new Quaternionf()));
    }

    /** Redraws the hologram and the open menu, every few ticks, only when their text changed. */
    private void refreshText(TurretDevice turret) {
        if (turret.textIn() > 0) {
            turret.setTextIn(turret.textIn() - 1);
            return;
        }
        turret.setTextIn(TEXT_TICKS);
        List<String> lines = TurretText.lines(turret.kind(), turret.level(), turret.magazine(), turret.status(),
                turret.satisfaction());
        Hologram hologram = turret.hologram();
        if (hologram != null && !hologram.lines().equals(lines)) {
            hologram.setLines(lines);
        }
        if (turret.existingMenu() != null && turret.existingMenu().isOpen()) {
            turret.existingMenu().refresh();
        }
    }

    /** The round type of an item, or empty when the item is no round. */
    private Optional<RoundType> roundOf(ItemStack stack) {
        return core.items().componentId(stack).flatMap(RoundType::ofItem);
    }

    /** The round type of the first round in an inventory, or empty when it holds none. */
    private Optional<RoundType> firstRoundIn(PlayerInventory inventory) {
        return Arrays.stream(inventory.getContents()).filter(Objects::nonNull).map(this::roundOf)
                .flatMap(Optional::stream).findFirst();
    }
}
