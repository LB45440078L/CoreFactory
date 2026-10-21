package io.corefactory.core;

import io.corefactory.api.BlockChangeSet;
import io.corefactory.api.BlockEditor;
import io.corefactory.api.BlockGenerator;
import io.corefactory.api.ClientEffects;
import io.corefactory.api.Mechanic;
import io.corefactory.api.CoreFactoryApi;
import io.corefactory.api.PlacedTnt;
import io.corefactory.api.ProtectionService;
import io.corefactory.api.TntDefinition;
import io.corefactory.core.config.CoreSettings;
import io.corefactory.core.config.DefinitionParser;
import io.corefactory.core.config.DefinitionParser.Definitions;
import io.corefactory.core.config.Messages;
import io.corefactory.core.display.PlacedDisplays;
import io.corefactory.core.engine.BlockJobs;
import io.corefactory.core.engine.ClaimGuard;
import io.corefactory.core.engine.Blast;
import io.corefactory.core.engine.DetonationObserver;
import io.corefactory.core.engine.Effects;
import io.corefactory.core.engine.Explosions;
import io.corefactory.core.engine.ProtectionImpl;
import io.corefactory.core.engine.Tasks;
import io.corefactory.core.engine.TntManager;
import io.corefactory.core.engine.Visuals;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.mechanics.Mechanics;
import io.corefactory.core.mechanics.Toolkit;
import io.corefactory.core.placement.PlacedTntStore;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.radiation.Radiation;
import io.corefactory.core.recipe.RecipeRegistrar;
import io.corefactory.core.recipe.RecipeSpec;
import io.corefactory.core.research.ResearchService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The implementation of {@link CoreFactoryApi}. Owns every registry and service, loads configuration, and drives
 * the per-tick work. The Spigot module creates one instance, wires listeners and commands to it, and calls
 * {@link #tick()} once per server tick.
 */
public final class CoreFactoryCore implements CoreFactoryApi {

    private final Plugin plugin;
    private final Logger log;
    private final ClientEffects clientEffects;
    private final ItemFactory items = new ItemFactory();
    private final RecipeRegistrar recipes;
    private final PlacedTntStore store = new PlacedTntStore();
    private final ProtectionImpl protection;
    private final Effects effects;
    private final Explosions explosions;
    private final Blast blast;
    private final BlockJobs jobs;
    private final TntManager manager;
    private final PlacedDisplays displays;
    private final Radiation radiation;
    private final Map<String, TntDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, RecipeSpec> tntRecipes = new LinkedHashMap<>();
    private final Map<String, ComponentDefinition> components = new LinkedHashMap<>();
    private final Map<String, OreDefinition> ores = new LinkedHashMap<>();
    private final Map<String, Mechanic> mechanics = new LinkedHashMap<>();
    private ResearchService research;
    private ProcessCatalog processes;
    private Set<String> itemComponentIds = Set.of();
    private CoreSettings settings;
    private Messages messages;
    private File dataFile;
    private boolean placedLoaded;
    private ConfigurationSection cachedTnts;
    private ConfigurationSection cachedItems;
    private long tick;

    public CoreFactoryCore(Plugin plugin, BlockEditor blockEditor, ClientEffects clientEffects) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.clientEffects = clientEffects;
        this.settings = CoreSettings.from(new MemoryConfiguration());
        this.messages = new Messages(null);
        this.recipes = new RecipeRegistrar(plugin, items);
        this.protection = new ProtectionImpl(settings);
        this.effects = new Effects(settings);
        this.explosions = new Explosions(effects);
        this.blast = new Blast(plugin, effects, explosions, settings);
        this.jobs = new BlockJobs(plugin, blockEditor, settings, this::forgetOverwritten);
        this.manager = new TntManager(this);
        this.displays = new PlacedDisplays(settings.displayScale());
        this.radiation = new Radiation(items);
        for (Mechanic mechanic : new Mechanics(new Toolkit(effects, explosions, blast, () -> messages, radiation)).all()) {
            mechanics.put(mechanic.id(), mechanic);
        }
    }

    /**
     * Loads or reloads configuration. Safe to call again on {@code /cf reload}: recipes are rebuilt and
     * already-placed TNT keeps working.
     *
     * @return the number of TNT types and components that loaded
     */
    public int reload(ConfigurationSection config, ConfigurationSection tntSection, ConfigurationSection itemSection,
                      File dataFolder) {
        this.settings = CoreSettings.from(config);
        this.messages = new Messages(config.getConfigurationSection("messages"));
        this.dataFile = new File(dataFolder, settings.dataFileName());
        protection.reconfigure(settings);
        effects.reconfigure(settings);
        blast.reconfigure(settings);
        jobs.reconfigure(settings);
        displays.reconfigure(settings.displayScale());
        this.cachedTnts = tntSection;
        this.cachedItems = itemSection;
        return applyDefinitions();
    }

    /** Parses the cached sections against the current mechanics, then rebuilds the recipes. */
    private int applyDefinitions() {
        Definitions parsed = DefinitionParser.parse(cachedTnts, cachedItems, mechanics.keySet(), log);
        definitions.clear();
        definitions.putAll(parsed.tnts());
        tntRecipes.clear();
        tntRecipes.putAll(parsed.tntRecipes());
        components.clear();
        components.putAll(parsed.components());

        if (settings.debug()) {
            log.info("Loaded " + definitions.size() + " TNT types and " + components.size() + " components.");
        }
        int registered = recipes.registerAll(definitions, tntRecipes, components);
        log.info("Registered " + registered + " crafting recipes.");
        manager.resetCooldowns();
        return definitions.size() + components.size();
    }

    /**
     * Adds a mechanic, or replaces the one with the same id. TNT types that name it become usable at once, and
     * their recipes are registered, when the configuration has already loaded.
     */
    @Override
    public void registerMechanic(Mechanic mechanic) {
        Mechanic previous = mechanics.put(mechanic.id(), mechanic);
        if (previous != null) {
            log.info("Mechanic '" + mechanic.id() + "' replaced an earlier registration.");
        }
        if (cachedTnts != null) {
            applyDefinitions();
        }
    }

    /** Loads the placed TNT registry from disk and rebuilds anchor fields. Call once after {@link #reload}. */
    public void loadPlaced() {
        store.load(dataFile, log);
        placedLoaded = true;
        protection.clearAnchors();
        for (PlacedTnt placed : store.all()) {
            registerSpecial(placed);
        }
        int removed = displays.sweepOrphans(ownedDisplays());
        if (removed > 0) {
            log.info("Removed " + removed + " orphaned TNT displays.");
        }
    }

    /**
     * Adds a claim or region system (WorldGuard, GriefPrevention) that blasts must respect. Call during enable,
     * before the first tick. Nothing is checked when no guard is added.
     */
    public void addClaimGuard(ClaimGuard guard) {
        protection.addGuard(guard);
    }

    /** Saves the placed TNT registry. */
    public void savePlaced() {
        if (dataFile != null && placedLoaded) {
            store.save(dataFile, log);
        }
    }

    /** Sets the platform's sprite effects that the mechanics play. Until it is set, no sprite effect is shown. */
    public void setVisuals(Visuals visuals) {
        effects.setVisuals(visuals);
    }

    /** Stops all staged work. Call on disable, after {@link #savePlaced}. */
    public void shutdown() {
        jobs.shutdown();
    }

    /**
     * Per-tick work: resets the particle budget, runs periodic placed-TNT logic, and saves on schedule.
     * Called from a repeating task on the main thread.
     */
    public void tick() {
        tick++;
        effects.beginTick();
        protection.beginTick();
        if (tick % 5 == 0) {
            runPlacedTicks();
        }
        if (tick % 100 == 0) {
            sweepStalePlaced();
        }
        radiation.tick(tick);
        if (tick % settings.saveIntervalTicks() == 0) {
            savePlaced();
        }
    }

    /** True when TNT may not be placed or ignited in this world. */
    public boolean isDisabledWorld(World world) {
        return world != null && settings.disabledWorlds().contains(world.getName().toLowerCase(Locale.ROOT));
    }

    /** Drops registry entries whose block is no longer TNT, for example after an unmanaged removal. */
    private void sweepStalePlaced() {
        for (PlacedTnt placed : new ArrayList<>(store.all())) {
            Location at = placed.location();
            if (!at.getWorld().isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) {
                continue;
            }
            if (at.getBlock().getType() != Material.TNT) {
                unregister(placed);
            }
        }
    }

    private void runPlacedTicks() {
        for (PlacedTnt placed : new ArrayList<>(store.all())) {
            TntDefinition definition = definitions.get(placed.tntId());
            if (definition == null) {
                continue;
            }
            Mechanic mechanic = mechanics.get(definition.mechanic());
            if (mechanic == null || mechanic.placedTickInterval() <= 0) {
                continue;
            }
            long period = mechanic.placedTickInterval();
            if (tick % period < 5) {
                mechanic.onPlacedTick(placed, definition, this);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Placement, removal and state, used by the Spigot listeners and commands.
    // ---------------------------------------------------------------------------------------------

    /** Registers a placed custom TNT block and, when enabled, shows its custom model. */
    public PlacedTnt place(Location blockLocation, TntDefinition definition, Player placer) {
        // A stale entry at this spot (its block was removed without an event) must not leak its display or anchor.
        store.get(blockLocation).ifPresent(this::unregister);
        PlacedTnt placed = store.put(blockLocation, definition.id());
        if (placer != null) {
            placed.setOwner(placer.getUniqueId());
            if (settings.directionFromPlayer()) {
                Vector facing = placer.getLocation().getDirection().setY(0);
                if (facing.lengthSquared() > 0.01) {
                    placed.setDirection(facing.normalize());
                }
            }
        }
        if (settings.customTextures()) {
            placed.setDisplayId(displays.spawn(placed.location(), items.tnt(definition, 1)));
        }
        registerSpecial(placed);
        return placed;
    }

    /** Removes a placed TNT from the registry and its display, leaving the world block as it is. */
    public void unregister(PlacedTnt placed) {
        unlinkAll(placed);
        store.remove(placed.location());
        displays.remove(placed.displayId());
        protection.removeAnchor(PlacedTntStore.key(placed.location()));
    }

    /** Unregisters a placed TNT and, when {@code clearBlock} is true, sets its block to air. */
    public void forget(PlacedTnt placed, boolean clearBlock) {
        unregister(placed);
        if (clearBlock) {
            Block block = placed.location().getBlock();
            if (block.getType() == Material.TNT) {
                block.setType(Material.AIR, false);
            }
        }
    }

    /** Called before the block pipeline overwrites a position: a placed TNT there stops being tracked. */
    private void forgetOverwritten(World world, int x, int y, int z) {
        Location at = new Location(world, x, y, z);
        store.get(at).ifPresent(this::unregister);
    }

    /** Links two placed TNT blocks in both directions. Returns false if they are the same block. */
    public boolean link(PlacedTnt a, PlacedTnt b) {
        if (a.location().equals(b.location())) {
            return false;
        }
        if (!containsLink(a, b.location())) {
            a.links().add(b.location());
        }
        if (!containsLink(b, a.location())) {
            b.links().add(a.location());
        }
        return true;
    }

    /** Removes every link of {@code placed}, and the back links that point at it. */
    public void unlinkAll(PlacedTnt placed) {
        for (Location other : new ArrayList<>(placed.links())) {
            store.get(other).ifPresent(p -> p.links().removeIf(l -> sameBlock(l, placed.location())));
        }
        placed.links().clear();
    }

    private static boolean containsLink(PlacedTnt placed, Location target) {
        return placed.links().stream().anyMatch(link -> sameBlock(link, target));
    }

    static boolean sameBlock(Location a, Location b) {
        return a.getWorld() == b.getWorld()
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    /** Sets the placed TNT's direction, normalising horizontal and vertical components as given. */
    public void aim(PlacedTnt placed, Vector direction) {
        placed.setDirection(direction.lengthSquared() < 0.0001 ? null : direction.normalize());
    }

    private void registerSpecial(PlacedTnt placed) {
        TntDefinition definition = definitions.get(placed.tntId());
        if (definition != null && "anchor".equals(definition.mechanic())) {
            protection.addAnchor(PlacedTntStore.key(placed.location()), placed.location(), definition.radius());
        }
    }

    private Collection<UUID> ownedDisplays() {
        List<UUID> owned = new ArrayList<>();
        for (PlacedTnt placed : store.all()) {
            if (placed.displayId() != null) {
                owned.add(placed.displayId());
            }
        }
        return owned;
    }

    public CoreSettings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public ItemFactory items() {
        return items;
    }

    public RecipeRegistrar recipes() {
        return recipes;
    }

    public Map<String, RecipeSpec> tntRecipes() {
        return Collections.unmodifiableMap(tntRecipes);
    }

    public Map<String, ComponentDefinition> componentDefinitions() {
        return Collections.unmodifiableMap(components);
    }

    /** Sets the ores that have an ore block item. Called once, when the ores are parsed. */
    public void setOres(List<OreDefinition> definitions) {
        ores.clear();
        for (OreDefinition ore : definitions) {
            ores.put(ore.id(), ore);
        }
    }

    /** The ores that have an ore block item, in file order. */
    public Collection<OreDefinition> ores() {
        return Collections.unmodifiableCollection(ores.values());
    }

    /** Sets the research service that gates the catalogue's items. Called once, during enable. */
    public void setResearch(ResearchService service) {
        this.research = Objects.requireNonNull(service, "service");
    }

    /** The research service. Throws until {@link #setResearch} has been called. */
    public ResearchService research() {
        if (research == null) {
            throw new IllegalStateException("No research service is set. Call setResearch during enable.");
        }
        return research;
    }

    /** Sets the process catalog the catalogue lists its machines from. Called once, during enable. */
    public void setProcesses(ProcessCatalog catalog) {
        this.processes = Objects.requireNonNull(catalog, "catalog");
    }

    /** The process catalog. Throws until {@link #setProcesses} has been called. */
    public ProcessCatalog processes() {
        if (processes == null) {
            throw new IllegalStateException("No process catalog is set. Call setProcesses during enable.");
        }
        return processes;
    }

    /**
     * Sets the ids of the components that items.yml defines, as opposed to materials.yml. The catalogue splits its
     * crafting parts from its science materials by this. Called whenever the configuration loads.
     */
    public void setItemComponentIds(Set<String> ids) {
        this.itemComponentIds = Set.copyOf(ids);
    }

    /** The ids of the components that items.yml defines. */
    public Set<String> itemComponentIds() {
        return itemComponentIds;
    }

    /** The id of the ore an item is the ore block item of, or empty for any other item. */
    public Optional<String> oreIdOf(ItemStack item) {
        return items.oreId(item);
    }

    /** The ore block item of an ore id, or empty when no ore has that id or its host is not a block. */
    public Optional<ItemStack> createOreItem(String id, int amount) {
        return Optional.ofNullable(ores.get(id)).flatMap(ore -> items.ore(ore, amount));
    }

    public Optional<Mechanic> mechanicOf(TntDefinition definition) {
        return mechanic(definition.mechanic());
    }

    public TntManager manager() {
        return manager;
    }

    /**
     * Adds an observer that is told once for every detonation of a custom TNT, after its blast is scheduled. Platform code
     * registers one to act on the devices a blast reaches. Call during enable.
     */
    public void addDetonationObserver(DetonationObserver observer) {
        manager.addObserver(observer);
    }

    /** The fallout, the radioactive suit and the Geiger counter. */
    public Radiation radiation() {
        return radiation;
    }

    // ---------------------------------------------------------------------------------------------
    // CoreFactoryApi
    // ---------------------------------------------------------------------------------------------

    @Override
    public org.bukkit.plugin.Plugin plugin() {
        return plugin;
    }

    @Override
    public long currentTick() {
        return tick;
    }

    @Override
    public Collection<TntDefinition> definitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    @Override
    public Optional<TntDefinition> definition(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    @Override
    public Collection<Mechanic> mechanics() {
        return Collections.unmodifiableCollection(mechanics.values());
    }

    @Override
    public Optional<Mechanic> mechanic(String id) {
        return Optional.ofNullable(mechanics.get(id));
    }

    @Override
    public Optional<PlacedTnt> placedAt(Location blockLocation) {
        return store.get(blockLocation);
    }

    @Override
    public Collection<PlacedTnt> placedTnts() {
        return store.all();
    }

    @Override
    public Optional<String> tntIdOf(ItemStack item) {
        return items.tntId(item);
    }

    @Override
    public Optional<ItemStack> createTntItem(String id, int amount) {
        return definition(id).map(definition -> items.tnt(definition, amount));
    }

    @Override
    public Optional<ItemStack> createComponentItem(String id, int amount) {
        return Optional.ofNullable(components.get(id)).map(component -> items.component(component, amount));
    }

    @Override
    public ClientEffects clientEffects() {
        return clientEffects;
    }

    @Override
    public ProtectionService protection() {
        return protection;
    }

    @Override
    public void submitBlockChanges(World world, BlockChangeSet changes, Runnable onComplete) {
        jobs.submit(world, changes, onComplete);
    }

    @Override
    public void submitBlockGenerator(World world, BlockGenerator generator, Runnable onComplete) {
        jobs.submit(world, generator, onComplete);
    }

    @Override
    public void ignite(PlacedTnt placed, Entity source) {
        manager.ignite(placed, source);
    }

    /** Lights a placed TNT for a vanilla cause, such as flint and steel, lava, a fire arrow or an explosion. */
    public void ignite(PlacedTnt placed, Entity source, IgniteCause cause) {
        manager.ignite(placed, source, cause);
    }

    @Override
    public void detonate(String tntId, Location origin, Entity source, Vector direction, int depth) {
        definition(tntId).ifPresent(definition -> manager.detonate(definition, origin, source, direction, depth,
                placedAt(origin).orElse(null)));
    }

    @Override
    public void scheduleLater(long delayTicks, Runnable task) {
        Tasks.later(plugin, delayTicks, task);
    }
}
