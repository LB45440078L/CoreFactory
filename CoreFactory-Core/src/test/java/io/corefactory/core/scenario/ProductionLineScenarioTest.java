package io.corefactory.core.scenario;

import io.corefactory.core.config.ConfigValues;
import io.corefactory.core.config.DefinitionParser;
import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DepositSettings;
import io.corefactory.core.deposit.DrillAccumulator;
import io.corefactory.core.deposit.DrillYield;
import io.corefactory.core.deposit.MemoryDepositStore;
import io.corefactory.core.deposit.TakeBatch;
import io.corefactory.core.device.Footprint;
import io.corefactory.core.drill.DrillBox;
import io.corefactory.core.drill.DrillCatalog;
import io.corefactory.core.drill.DrillKind;
import io.corefactory.core.drill.DrillDefinition;
import io.corefactory.core.drill.DrillDeposits;
import io.corefactory.core.drill.DrillPower;
import io.corefactory.core.drill.DrillRules;
import io.corefactory.core.drill.DrillSelection;
import io.corefactory.core.drill.DrillSpec;
import io.corefactory.core.drill.DrillState;
import io.corefactory.core.drill.DrillStatus;
import io.corefactory.core.drill.DrillStorage;
import io.corefactory.core.drill.DrillTable;
import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.ConsumerResult;
import io.corefactory.core.energy.EnergyNetwork;
import io.corefactory.core.energy.EnergyResult;
import io.corefactory.core.energy.EnergySnapshot;
import io.corefactory.core.energy.EnergyTable;
import io.corefactory.core.energy.Generator;
import io.corefactory.core.energy.GeneratorEnvironment;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.energy.GeneratorOutput;
import io.corefactory.core.energy.GeneratorState;
import io.corefactory.core.energy.GeneratorStep;
import io.corefactory.core.energy.GridRegistry;
import io.corefactory.core.energy.GridResult;
import io.corefactory.core.energy.OpenAir;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.energy.Pylon;
import io.corefactory.core.energy.PylonType;
import io.corefactory.core.energy.Wire;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.logistics.BeltItem;
import io.corefactory.core.logistics.Dir;
import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.LogisticsLevel;
import io.corefactory.core.logistics.LogisticsNetwork;
import io.corefactory.core.logistics.LogisticsTable;
import io.corefactory.core.logistics.Port;
import io.corefactory.core.oil.Fluid;
import io.corefactory.core.oil.FluidRules;
import io.corefactory.core.oil.FluidTank;
import io.corefactory.core.oil.OilJoinPlan;
import io.corefactory.core.oil.OilJoinPlan.Device;
import io.corefactory.core.oil.OilJoinPlan.Join;
import io.corefactory.core.oil.OilJoinPlan.Kind;
import io.corefactory.core.oil.OilJoinPlan.Tube;
import io.corefactory.core.oil.OilNetwork;
import io.corefactory.core.oil.OilSettings;
import io.corefactory.core.oil.Refinery;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.MachineState;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessEngine;
import io.corefactory.core.process.Stack;
import io.corefactory.core.scenario.ScenarioSupport.BarrelPort;
import io.corefactory.core.scenario.ScenarioSupport.ChestPort;
import io.corefactory.core.scenario.ScenarioSupport.FuelPort;
import io.corefactory.core.scenario.ScenarioSupport.MachineInputPort;
import io.corefactory.core.upgrade.LabLevels;
import io.corefactory.core.water.WaterNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import static io.corefactory.core.scenario.ScenarioSupport.STACK_LIMIT;
import static io.corefactory.core.scenario.ScenarioSupport.countOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The charcoal grid and the oil-to-diesel line, run tick by tick with the Core models for every part. The layout is fixed
 * (see the positions below), the world runs at 20 ticks a second, and each test runs the same ten minutes of game time and
 * checks one part of the outcome. The numbers asserted come from the Core's tables, the bundled YAML or a formula over them;
 * each one says where it comes from.
 *
 * <p>Two things are not the plugin: the consumers' ids are a fixed world name and the block coordinates (the plugin uses the
 * world's id), and the satisfaction of the grid is taken from the Core's solver each tick, as the plugin does.
 */
class ProductionLineScenarioTest {

    private static final Logger LOG = Logger.getLogger("ProductionLineScenarioTest");

    /** The world name in the consumer ids. */
    private static final String WORLD = "world";

    /** The y of every block of the layout. */
    private static final int Y = 64;

    /** One server tick, in seconds: the tick length every model uses. */
    private static final double DT = EnergyTable.SECONDS_PER_TICK;

    /** The seed of the world's wind, which the scenario does not use. */
    private static final long SEED = 7L;

    /** The world time the run starts at: noon, when the panels give their peak. */
    private static final long START_TICK = EnergyTable.NOON_TICK;

    /** Ten minutes of game time, 20 ticks a second. */
    private static final int RUN_TICKS = 12_000;

    /** The air above the generators: the wind is not used, so the open-air input is not read. */
    private static final OpenAir OPEN_AIR = new OpenAir(0, Y);

    /** Items a second a Level II picker moves, from LogisticsTable (2.0), used to compare the drill's rate with the picker's. */
    private static final double PICKER_II_ITEMS_PER_SECOND = 2.0;

    /** The Level III drill of both drill kinds: the level of the petroleum drill and of the ore drill. */
    private static final int DRILL_LEVEL = 3;

    // The layout. Every block is at y 64; the coordinates are (x, z). The charcoal feed runs along z 0, the oil field along z 0
    // to the east, and the ore line north of the ore drill at x 31.

    static final Pos FEED_CHEST = at(0, 0);
    static final Pos FEED_IN = at(1, 0);
    static final Pos FEED_OUT = at(9, 0);
    static final Pos CHARCOAL_GENERATOR = at(10, 0);
    static final Pos OIL_DRILL = at(30, 0);
    static final Pos INTAKE_TUBE = at(31, 0);
    static final Pos REFINERY = at(40, 0);
    static final Pos OUTLET_TUBE = at(41, 0);
    static final Pos DIESEL_GENERATOR = at(44, 0);
    static final Pos ORE_DRILL = at(31, -13);
    static final Pos ORE_IN = at(31, -14);
    static final Pos ORE_SPLIT = at(31, -17);
    static final Pos LEFT_BELT = at(30, -17);
    static final Pos RIGHT_BELT = at(32, -17);
    static final Pos MIDDLE_BELT = at(31, -18);
    static final Pos CRUSHER_FLUORITE = at(29, -17);
    static final Pos CRUSHER_SALTPETER = at(33, -17);
    static final Pos COPPER_CHEST = at(31, -19);

    /** The feed belts, from the chest's picker to the generator's picker: x 2 to 8 along z 0. */
    private static final int FEED_BELT_FIRST_X = 2;
    private static final int FEED_BELT_LAST_X = 8;

    /** The solar panels, six in a row at z 6, all wired to the first pylon. */
    static final List<Pos> SOLAR_PANELS = List.of(at(2, 6), at(3, 6), at(4, 6), at(5, 6), at(6, 6), at(7, 6));

    static final String CHARCOAL_GEN_ID = "charcoal-gen";
    static final String DIESEL_GEN_ID = "diesel-gen";

    /** The panels' ids, in the order of {@link #SOLAR_PANELS}. */
    static final List<String> SOLAR_IDS = List.of("solar-0", "solar-1", "solar-2", "solar-3", "solar-4", "solar-5");

    /** The four level I pylons: A near the feed, B between, E at the oil field, C at the ore line. */
    static final List<Pylon> PYLONS = List.of(
            new Pylon("pylon-a", at(5, 3), PylonType.SMALL),
            new Pylon("pylon-b", at(20, 0), PylonType.SMALL),
            new Pylon("pylon-e", at(35, -3), PylonType.SMALL),
            new Pylon("pylon-c", at(28, -19), PylonType.SMALL));

    /** The deposits, with the ids the drills' searches report. */
    private static final long CRUDE_ID = 1L;
    private static final long COAL_ID = 2L;
    private static final long FLUORITE_ID = 3L;
    private static final long SALTPETER_ID = 4L;

    /** What the deposits held when the scenario starts, in items or litres. The crude field is planned at the minimum amount. */
    private static final int COAL_START = 150;
    private static final int FLUORITE_START = 40;
    private static final int SALTPETER_START = 20;

    /** The diesel the outlet tube holds when the scenario starts, so the grid starts at full power (see the report). */
    private static final double SEED_DIESEL_LITRES = 1.0;

    /** The charcoal in the feed chest when the scenario starts: 54 slots of 64. */
    private static final int CHARCOAL_FEED_START = 54 * STACK_LIMIT;

    /**
     * The power the covered devices ask for: the feed line (picker 100 W, seven belts 200 W each, picker 100 W) 1600 W; the oil
     * drill 40000 W (level III); the refinery 4000 W (a lab machine at the lab default); the ore line (level II picker 200 W, five
     * belts 200 W, splitter 500 W) 1700 W; and the two crushers 4000 W each. The ore drill is outside every pylon's range, so its
     * 40000 W is not in the load: 1600 + 40000 + 4000 + 1700 + 8000 = 55300 W.
     */
    static final double COVERED_LOAD_W = 55_300.0;

    /** The four pylons carry this much between them: the most a grid can deliver, whatever its supply. */
    private static final double FOUR_PYLONS_CAPACITY_W = 4 * PylonType.SMALL.capacityW();

    /** The items. */
    private static final Item FLUORITE_ITEM = Item.component("raw_fluorite");
    private static final Item SALTPETER_ITEM = Item.component("raw_saltpeter");
    private static final Item CHARCOAL_ITEM = Item.material("CHARCOAL");
    private static final ItemRef RAW_FLUORITE = ItemRef.item("raw_fluorite");
    private static final ItemRef RAW_SALTPETER = ItemRef.item("raw_saltpeter");
    private static final ItemRef CHARCOAL_REF = ItemRef.material("CHARCOAL");
    private static final ItemRef CRUSHED_FLUORITE = ItemRef.item("crushed_fluorite");
    private static final ItemRef CRUDE_SALTPETER = ItemRef.item("crude_saltpeter");

    /** The number of crushed items one raw item makes, from processes.yml (crush_fluorite and crush_saltpeter give 2). */
    private static final int CRUSHED_PER_RAW = 2;

    // ----------------------------------------------------------------------------------------------------------------
    // Focused tests: the parts on their own.
    // ----------------------------------------------------------------------------------------------------------------

    /** A solar panel gives 320 W at the sun's peak, which is the 0.2 efficiency times 1.6 m2 times 1000 W/m2 of EnergyTable. */
    @Test
    void solarPanelsGiveFullOutputAtNoonAndNothingAtNight() {
        Generator panel = new Generator("solar", SOLAR_PANELS.get(0), GeneratorKind.SOLAR, GeneratorState.EMPTY);

        GeneratorStep noon = panel.tick(GeneratorEnvironment.at(START_TICK, SEED, OPEN_AIR, false), DT);
        assertThat(noon.outputW()).isCloseTo(320.0, within(1e-9));

        // Night is from 13000 to 23000 of the day: the sun has set and the panel gives nothing.
        assertThat(panel.tick(GeneratorEnvironment.at(13_000L, SEED, OPEN_AIR, false), DT).outputW()).isZero();
        assertThat(panel.tick(GeneratorEnvironment.at(18_000L, SEED, OPEN_AIR, false), DT).outputW()).isZero();

        // Six panels at noon give 1920 W in all.
        assertThat(SOLAR_PANELS).hasSize(6);
        assertThat(6 * noon.outputW()).isCloseTo(1920.0, within(1e-9));
    }

    /**
     * A charcoal item holds 3 MJ of heat at 25 % efficiency, so it gives 750 kJ of electricity, which lasts 30 s at 25 kW:
     * 600 ticks. The generator gives its rated output for each of those ticks and nothing after the item is gone.
     */
    @Test
    void charcoalGeneratorBurnsOneItemForItsBurnTimeAtRatedPower() {
        int burnTicks = (int) Math.round(GeneratorKind.CHARCOAL.burnSeconds() / DT);
        assertThat(burnTicks).isEqualTo(600);

        GeneratorEnvironment env = GeneratorEnvironment.at(START_TICK, SEED, OPEN_AIR, false);
        GeneratorState state = new GeneratorState(0.0, 1);
        for (int tick = 1; tick <= burnTicks; tick++) {
            GeneratorStep step = new Generator("charcoal", CHARCOAL_GENERATOR, GeneratorKind.CHARCOAL, state).tick(env, DT);
            assertThat(step.outputW()).as("tick %d of the burn", tick).isEqualTo(GeneratorKind.CHARCOAL.ratedW());
            state = step.state();
        }
        assertThat(state.fuelItems()).isZero();
        GeneratorStep after = new Generator("charcoal", CHARCOAL_GENERATOR, GeneratorKind.CHARCOAL, state).tick(env, DT);
        assertThat(after.outputW()).isZero();
    }

    /**
     * The grid's satisfaction is the power it delivers over the load of the devices in range. With the charcoal generator alone
     * at night the grid delivers 25 kW against 55.3 kW, so its satisfaction is 25/55.3. With the diesel generator running as
     * well the supply exceeds the four pylons' 92 kW, the delivery is the whole load, and the satisfaction is 1.
     */
    @Test
    void gridSatisfactionIsWhatTheGridDeliversOverTheCoveredLoad() {
        assertThat(FOUR_PYLONS_CAPACITY_W).isCloseTo(92_000.0, within(1e-9));

        EnergyResult night = solveWith(outputsAt(13_000L, 25_000.0, 0.0));
        GridResult nightGrid = night.grids().get(0);
        assertThat(nightGrid.loadW()).isCloseTo(COVERED_LOAD_W, within(1e-6));
        assertThat(nightGrid.deliveredW()).isCloseTo(25_000.0, within(1e-6));
        assertThat(nightGrid.satisfaction()).isCloseTo(25_000.0 / COVERED_LOAD_W, within(1e-9));

        EnergyResult day = solveWith(outputsAt(START_TICK, 25_000.0, 150_000.0));
        GridResult dayGrid = day.grids().get(0);
        assertThat(dayGrid.deliveredW()).isCloseTo(COVERED_LOAD_W, within(1e-6));
        assertThat(dayGrid.satisfaction()).isCloseTo(1.0, within(1e-9));
    }

    /** The covered load is the demand of every device inside a pylon's square: 20 of the 21 devices, all but the ore drill. */
    @Test
    void theCoveredLoadIsTheDemandOfEveryDeviceInPylonRange() {
        double covered = 0.0;
        int devices = 0;
        for (Consumer consumer : allConsumers(labDemandW())) {
            if (PYLONS.stream().anyMatch(pylon -> pylon.covers(consumer.pos()))) {
                covered += consumer.demandW();
                devices++;
            }
        }
        assertThat(devices).isEqualTo(20);
        assertThat(covered).isCloseTo(COVERED_LOAD_W, within(1e-6));
        assertThat(PYLONS.stream().noneMatch(pylon -> pylon.covers(ORE_DRILL))).isTrue();
    }

    /**
     * A Level I picker takes one item a second at full satisfaction (LogisticsTable), after a 10-tick delay at the start. From the
     * chest it therefore moves an item at ticks 10, 30, 50 and so on: 20 items in 400 ticks.
     */
    @Test
    void levelOnePickerMovesOneItemEachSecondFromItsChest() {
        LogisticsNetwork net = LogisticsNetwork.builder()
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.I)
                .belt(at(2, 0), Dir.EAST, LogisticsLevel.I)
                .build();
        ChestPort source = new ChestPort(full(54));
        ChestPort sink = new ChestPort(ScenarioSupport.slots(54));
        Map<Pos, Port> ports = Map.of(at(0, 0), source, at(3, 0), sink);

        int moved = 0;
        for (int tick = 0; tick < 400; tick++) {
            moved += net.tick(DT, Map.of(), ports).movedBy(LogisticsNetwork.pickerId("p"));
        }

        assertThat(LogisticsTable.pickerItemsPerSecond(LogisticsLevel.I)).isEqualTo(1.0);
        assertThat(moved).isEqualTo(20);
        assertThat(source.storedCount()).isEqualTo(54 * STACK_LIMIT - 20);
        assertThat(sink.storedCount() + net.itemsOn(at(2, 0)).size()).isEqualTo(20);
    }

    /**
     * A Level I belt moves an item 1.875 blocks a second (LogisticsTable), so 0.09375 of a block a tick: after 10 ticks the item is
     * at 0.9375 and still on the belt; on the 11th it reaches the exit and is handed to the port there.
     */
    @Test
    void levelOneBeltCarriesAnItemAtItsSpeedAndHandsItOverAtTheExit() {
        LogisticsNetwork net = LogisticsNetwork.builder().belt(at(0, 0), Dir.EAST, LogisticsLevel.I).build();
        ChestPort sink = new ChestPort(ScenarioSupport.slots(54));
        Map<Pos, Port> ports = Map.of(at(1, 0), sink);
        assertThat(net.insertAtEntry(at(0, 0), CHARCOAL_ITEM)).isTrue();

        for (int tick = 0; tick < 10; tick++) {
            net.tick(DT, Map.of(), ports);
        }
        assertThat(net.itemsOn(at(0, 0)).get(0).position())
                .isCloseTo(10 * LogisticsTable.beltSpeedBlocksPerSecond(LogisticsLevel.I) * DT, within(1e-9));
        assertThat(sink.storedCount()).isZero();

        net.tick(DT, Map.of(), ports);
        assertThat(sink.storedCount()).isEqualTo(1);
        assertThat(net.itemsOn(at(0, 0))).isEmpty();
    }

    /**
     * The crusher's process crush_fluorite runs 100 ticks and makes 2 crushed fluorite, both at chance 1, so the pass completes on
     * its 100th tick and not before.
     */
    @Test
    void aCrusherFinishesEachRawItemInItsProcessTicks() {
        ProcessCatalog catalog = ProcessCatalog.parse(ScenarioSupport.bundled("processes.yml"), LOG);
        ProcessEngine engine = new ProcessEngine(catalog, ProcessEngine.FUEL, ref -> STACK_LIMIT);
        int ticks = catalog.process("crush_fluorite").orElseThrow().ticks();
        assertThat(ticks).isEqualTo(100);

        List<Stack> inputs = ScenarioSupport.slots(1);
        inputs.get(0).set(RAW_FLUORITE, 1);
        MachineState crusher = new MachineState("crusher", inputs, ScenarioSupport.slots(3), List.of());
        Random random = new Random(1L);
        for (int tick = 1; tick < ticks; tick++) {
            assertThat(engine.tick(crusher, random, false)).isEqualTo(ProcessEngine.Step.RUNNING);
        }
        assertThat(engine.tick(crusher, random, false)).isEqualTo(ProcessEngine.Step.COMPLETED);

        assertThat(countOf(crusher.outputs(), CRUSHED_FLUORITE)).isEqualTo(CRUSHED_PER_RAW);
        assertThat(countOf(crusher.inputs(), RAW_FLUORITE)).isZero();
    }

    /**
     * The crude deposit is an ore of the stone blocks: its ores.yml entry replaces STONE, and the petroleum drill's square reaches its
     * circle. The ore drill's square reaches the three ore deposits, and a drill of each kind takes only its own deposits.
     */
    @Test
    void theCrudeDepositIsInStoneAndTheDrillsReachTheirDeposits() {
        Map<String, Object> oresFile = ScenarioSupport.bundled("ores.yml");
        OreDefinition crude = OreDefinition.parse(ConfigValues.optionalSection(oresFile, "ores"), LOG).stream()
                .filter(ore -> ore.id().equals(DrillRules.DEPOSIT))
                .findFirst()
                .orElseThrow();
        assertThat(crude.fluid()).isTrue();
        assertThat(crude.replace()).contains("STONE");
        assertThat(DrillRules.hardBase("STONE")).isTrue();

        Line line = Line.run(0);
        DrillSpec spec = DrillTable.level(DRILL_LEVEL);
        DrillBox oilBox = DrillBox.around(OIL_DRILL.x(), OIL_DRILL.y(), OIL_DRILL.z(), spec);
        DrillBox oreBox = DrillBox.around(ORE_DRILL.x(), ORE_DRILL.y(), ORE_DRILL.z(), spec);
        assertThat(DrillDeposits.touches(line.deposit(CRUDE_ID), oilBox)).isTrue();
        assertThat(DrillDeposits.touches(line.deposit(COAL_ID), oreBox)).isTrue();
        assertThat(DrillDeposits.touches(line.deposit(FLUORITE_ID), oreBox)).isTrue();
        assertThat(DrillDeposits.touches(line.deposit(SALTPETER_ID), oreBox)).isTrue();

        assertThat(DrillDeposits.kindTakes(DrillKind.PETROLEUM, DrillRules.DEPOSIT)).isTrue();
        assertThat(DrillDeposits.kindTakes(DrillKind.PETROLEUM, "coal_ore")).isFalse();
        assertThat(DrillDeposits.kindTakes(DrillKind.ORE, DrillRules.DEPOSIT)).isFalse();
        assertThat(DrillDeposits.kindTakes(DrillKind.ORE, "coal_ore")).isTrue();
    }

    /**
     * The diesel generator at rated output burns 0.0104 litres a second (150 kW over 36.12 MJ a litre at 40 %), so 0.00052 litres a tick.
     * It gives the share of that it draws: all of it at full draw, half at half, none at none, and no more than rated for more.
     */
    @Test
    void theDieselGeneratorGivesTheShareOfTheLitresItWants() {
        GeneratorEnvironment env = GeneratorEnvironment.at(START_TICK, SEED, OPEN_AIR, false);
        Generator diesel = new Generator(DIESEL_GEN_ID, DIESEL_GENERATOR, GeneratorKind.DIESEL, GeneratorState.EMPTY);
        double wanted = diesel.litresWanted(DT);

        assertThat(GeneratorKind.DIESEL.litresPerSecond())
                .isCloseTo(150_000.0 / (Fluid.DIESEL.joulesPerLitre() * EnergyTable.DIESEL_EFFICIENCY), within(1e-12));
        assertThat(wanted).isCloseTo(GeneratorKind.DIESEL.litresPerSecond() * DT, within(1e-12));

        assertThat(diesel.tickFluid(env, DT, wanted).outputW()).isCloseTo(150_000.0, within(1e-6));
        assertThat(diesel.tickFluid(env, DT, wanted / 2).outputW()).isCloseTo(75_000.0, within(1e-6));
        assertThat(diesel.tickFluid(env, DT, 0.0).outputW()).isZero();
        assertThat(diesel.tickFluid(env, DT, 2 * wanted).outputW()).isCloseTo(150_000.0, within(1e-6));
    }

    /**
     * The ore line's rates, as LogisticsTable and config.yml give them: a Level I belt holds 4 items a block and moves 1.875 blocks a
     * second, so it carries 7.5 items a second; the Level II picker moves 2 items a second, the Level I splitter 2; the Level III ore
     * drill yields 2.5 items a second at full power, so the picker is the limit on the line.
     */
    @Test
    void theOreLineRunsAtTheRatesOfItsLevels() {
        assertThat(LogisticsTable.BELT_CAPACITY * LogisticsTable.beltSpeedBlocksPerSecond(LogisticsLevel.I))
                .isCloseTo(7.5, within(1e-9));
        assertThat(LogisticsTable.pickerItemsPerSecond(LogisticsLevel.II)).isEqualTo(PICKER_II_ITEMS_PER_SECOND);
        assertThat(LogisticsTable.splitterItemsPerSecond(LogisticsLevel.I)).isEqualTo(2.0);

        List<Double> rates = ConfigValues.numberList(ScenarioSupport.bundled("config.yml"), "drill-rates", List.of());
        assertThat(rates.get(DRILL_LEVEL - 1)).isEqualTo(2.5);
        assertThat(rates.get(DRILL_LEVEL - 1)).isGreaterThan(PICKER_II_ITEMS_PER_SECOND);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // The scenario: ten minutes of the whole line, one named test for each part of the outcome.
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Every device in pylon range gets its full demand at every tick: the supply of the diesel generator alone (150 kW) exceeds the
     * 92 kW the four pylons carry, so the delivery is the whole 55.3 kW load. The ore drill, which no pylon reaches, is fed by the
     * petroleum network and gets no grid power, so its satisfaction from the grid is zero.
     */
    @Test
    void theGridCarriesEveryDeviceInPylonRangeAtFullSatisfactionForTheWholeRun() {
        Line line = Line.run(RUN_TICKS);

        assertThat(line.worstCoveredGap).isLessThan(1e-9);
        assertThat(line.minLoadW).isCloseTo(COVERED_LOAD_W, within(1e-6));
        assertThat(line.maxLoadW).isCloseTo(COVERED_LOAD_W, within(1e-6));
        assertThat(line.minGridSatisfaction).isCloseTo(1.0, within(1e-9));
        assertThat(line.worstOreSatisfaction).isZero();
    }

    /**
     * The feed chest gives charcoal to the generator through the belts at one item a second, the generator burns it at rated
     * power, and every item that leaves the chest is either in the generator, on a belt or burnt: the feed balance holds at every
     * tick.
     */
    @Test
    void theCharcoalGeneratorIsFedFromTheChestAndBurnsAtRatedPower() {
        Line line = Line.run(RUN_TICKS);

        assertThat(line.badCharcoalOutputs).isZero();
        assertThat(line.brokenFeedBalance).isZero();
        assertThat(line.charcoalFuel.accepted()).isPositive();
        assertThat(line.lastCharcoalW).isEqualTo(GeneratorKind.CHARCOAL.ratedW());
        GeneratorState fuel = line.charcoalFuel.state();
        assertThat(fuel.fuelItems() > 0 || fuel.remainingSeconds() > 0).isTrue();
        assertThat(line.feedChest.storedCount()).isLessThan(CHARCOAL_FEED_START);
    }

    /**
     * The petroleum drill pumps 2.5 litres a second (drill-rates, level III); the refinery takes 0.1 litre a second at full speed
     * and makes two thirds of a litre of diesel from each litre; the diesel generator draws its litres and gives its rated 150 kW
     * for every tick. The mass balance of both networks holds to the last litre.
     */
    @Test
    void theOilLineRefinesPetroleumAtTheRefineryRateAndKeepsTheDieselGeneratorAtRatedPower() {
        Line line = Line.run(RUN_TICKS);

        assertThat(line.refineryTicks).isPositive();
        assertThat(line.shortRefineryTicks).isZero();
        assertThat(line.refineryDrawn).isCloseTo(Refinery.PETROLEUM_PER_SECOND * DT * line.refineryTicks, within(1e-6));
        assertThat(line.worstDieselGapW).isLessThan(1e-6);
        assertThat(line.lastDieselW).isCloseTo(GeneratorKind.DIESEL.ratedW(), within(1e-6));
        assertThat(line.dieselSupplied).isPositive();
        assertThat(line.tubes.joinsAt(DIESEL_GENERATOR)).contains(new Join(line.tubes.networkOf(OUTLET_TUBE), Kind.SINK, null));
        assertThat(line.outlet.litres())
                .isCloseTo(SEED_DIESEL_LITRES + line.dieselSupplied - line.dieselDrawn, within(1e-6));
        assertThat(line.intake.litres())
                .isCloseTo(line.pumped - line.refineryDrawn - line.oreDrawn, within(1e-6));
        assertThat(line.intake.litres()).isPositive();
    }

    /**
     * The ore drill mines the three deposits in proportion to what each holds, so they run out together. Fluorite goes left to its
     * crusher, saltpeter right to its crusher, and charcoal straight on into the copper chest. Each crusher gets only its own raw
     * item, and every mined item ends where it belongs.
     */
    @Test
    void theOreLineRoutesFluoriteLeftSaltpeterRightAndCharcoalStraightIntoTheChest() {
        Line line = Line.run(RUN_TICKS);

        assertThat(line.misroutedItems).isZero();
        assertThat(line.badCrusherInputs).isZero();
        assertThat(line.depositRemaining(COAL_ID)).isZero();
        assertThat(line.depositRemaining(FLUORITE_ID)).isZero();
        assertThat(line.depositRemaining(SALTPETER_ID)).isZero();
        assertThat(line.copperChest.count(CHARCOAL_REF)).isEqualTo(COAL_START);
        assertThat(line.copperChest.storedCount()).isEqualTo(COAL_START);
        assertThat(countOf(line.fluoriteCrusher.outputs(), CRUSHED_FLUORITE)).isEqualTo(CRUSHED_PER_RAW * FLUORITE_START);
        assertThat(countOf(line.saltpeterCrusher.outputs(), CRUDE_SALTPETER)).isEqualTo(CRUSHED_PER_RAW * SALTPETER_START);
        assertThat(countOf(line.fluoriteCrusher.inputs(), RAW_FLUORITE)).isZero();
        assertThat(countOf(line.saltpeterCrusher.inputs(), RAW_SALTPETER)).isZero();
    }

    /**
     * Petroleum and diesel never share a tube: the refinery's intake and outlet are two networks of their own, joined to the refinery as an
     * intake and an output. The ore drill draws its petroleum from the intake network.
     */
    @Test
    void petroleumAndDieselNeverShareATubeAndTheOreDrillDrawsFromThePetroleumNetwork() {
        Line line = Line.run(RUN_TICKS);

        assertThat(FluidRules.mayJoin(EnumSet.of(Fluid.PETROLEUM), EnumSet.of(Fluid.DIESEL))).isFalse();
        int intake = line.tubes.networkOf(INTAKE_TUBE);
        int outlet = line.tubes.networkOf(OUTLET_TUBE);
        assertThat(intake).isNotEqualTo(outlet);
        assertThat(line.tubes.joinsAt(REFINERY)).containsExactlyInAnyOrder(
                new Join(intake, Kind.SINK, null), new Join(outlet, Kind.SOURCE, Fluid.DIESEL));
        assertThat(line.tubes.joinsAt(ORE_DRILL)).containsExactly(new Join(intake, Kind.SINK, null));
        assertThat(line.tubes.joinsAt(OIL_DRILL)).containsExactly(new Join(intake, Kind.SOURCE, Fluid.PETROLEUM));
        assertThat(line.intake.kinds()).containsOnly(Fluid.PETROLEUM);
        assertThat(line.outlet.kinds()).containsOnly(Fluid.DIESEL);
        assertThat(line.oreDrawn).isPositive();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // The layout and the Core models that build it.
    // ----------------------------------------------------------------------------------------------------------------

    static Pos at(int x, int z) {
        return new Pos(x, Y, z);
    }

    /** The consumer id of a block: the world and the three block coordinates, as the plugin makes them. */
    static String consumerId(Pos pos) {
        return WORLD + ":" + pos.x() + ":" + pos.y() + ":" + pos.z();
    }

    static Consumer consumer(Pos pos, double demandW) {
        return new Consumer(consumerId(pos), pos, demandW);
    }

    /** The lab machines' demand, from config.yml (energy.consumers.lab-demand). */
    static double labDemandW() {
        return ConfigValues.integer(ScenarioSupport.bundled("config.yml"), "energy.consumers.lab-demand", 0);
    }

    /** The feed line, the ore line, the oil drill, the refinery, the ore drill and the two crushers, each with its demand. */
    static List<Consumer> allConsumers(double labDemandW) {
        List<Consumer> consumers = new ArrayList<>();
        for (Consumer node : logisticsNetwork().consumers()) {
            consumers.add(consumer(node.pos(), node.demandW()));
        }
        consumers.add(consumer(OIL_DRILL, EnergyTable.drillDemandW(DRILL_LEVEL)));
        consumers.add(consumer(REFINERY, labDemandW));
        consumers.add(consumer(ORE_DRILL, EnergyTable.drillDemandW(DRILL_LEVEL)));
        consumers.add(consumer(CRUSHER_FLUORITE, labDemandW));
        consumers.add(consumer(CRUSHER_SALTPETER, labDemandW));
        return consumers;
    }

    /**
     * The belts, pickers and splitter of both lines. The feed: a picker takes from the chest and puts on a belt of seven blocks
     * that a second picker empties into the generator. The ore: a picker takes from the ore drill's barrel, a belt carries to a
     * splitter (fluorite to the west, saltpeter to the east, the rest straight north).
     */
    static LogisticsNetwork logisticsNetwork() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder()
                .picker("feed-in", FEED_IN, Dir.WEST, Dir.EAST, LogisticsLevel.I);
        for (int x = FEED_BELT_FIRST_X; x <= FEED_BELT_LAST_X; x++) {
            builder.belt(at(x, 0), Dir.EAST, LogisticsLevel.I);
        }
        return builder
                .picker("feed-out", FEED_OUT, Dir.WEST, Dir.EAST, LogisticsLevel.I)
                .picker("ore-in", ORE_IN, Dir.SOUTH, Dir.NORTH, LogisticsLevel.II)
                .belt(at(31, -15), Dir.NORTH, LogisticsLevel.I)
                .belt(at(31, -16), Dir.NORTH, LogisticsLevel.I)
                .splitter("ore-split", ORE_SPLIT, Dir.NORTH, Set.of(FLUORITE_ITEM), Set.of(SALTPETER_ITEM), LogisticsLevel.I)
                .belt(LEFT_BELT, Dir.WEST, LogisticsLevel.I)
                .belt(RIGHT_BELT, Dir.EAST, LogisticsLevel.I)
                .belt(MIDDLE_BELT, Dir.NORTH, LogisticsLevel.I)
                .build();
    }

    /**
     * The oil tubes as the plugin builds them: the intake pipes from the petroleum drill to the refinery's west face, the ore drill's pipe
     * run joined to the intake, and the outlet pipes from the refinery's east face to the diesel generator. The drills and the generator join
     * the networks beside them; the refinery keeps its two networks apart, taking petroleum from the intake and giving diesel to the outlet.
     */
    static OilJoinPlan oilTubes() {
        List<Tube> tubes = new ArrayList<>();
        for (int x = INTAKE_TUBE.x(); x <= 39; x++) {
            tubes.add(pipe(at(x, 0)));
        }
        for (int z = -1; z >= -12; z--) {
            tubes.add(pipe(at(31, z)));
        }
        for (int x = OUTLET_TUBE.x(); x <= 43; x++) {
            tubes.add(pipe(at(x, 0)));
        }
        return OilJoinPlan.of(tubes, List.of(
                new Device(OIL_DRILL, Footprint.heightOf("petroleum_drill_" + DRILL_LEVEL), Fluid.PETROLEUM, null, false),
                new Device(ORE_DRILL, Footprint.heightOf("ore_drill_" + DRILL_LEVEL), null, null, false),
                new Device(REFINERY, Footprint.heightOf("crude_oil_refinery"), Fluid.DIESEL, Fluid.PETROLEUM, true),
                new Device(DIESEL_GENERATOR, Footprint.heightOf("diesel_generator"), null, Fluid.DIESEL, false)));
    }

    /** A pipe of the oil tubes, with its tank empty. */
    private static Tube pipe(Pos pos) {
        return new Tube(pos, WaterNode.pipe(pos), null);
    }

    /** One tank for each tube of a network, of the given capacity. Devices hold nothing. */
    static List<FluidTank> tanksOf(OilJoinPlan plan, int networkId, double capacityLitres) {
        List<FluidTank> tanks = new ArrayList<>();
        for (int i = 0; i < plan.networks().get(networkId).tubes().size(); i++) {
            tanks.add(new FluidTank(capacityLitres));
        }
        return tanks;
    }

    /** The generators of one tick at a world time: the charcoal one and the diesel one at the given outputs, and the panels. */
    static List<GeneratorOutput> outputsAt(long worldTick, double charcoalW, double dieselW) {
        GeneratorEnvironment env = GeneratorEnvironment.at(worldTick, SEED, OPEN_AIR, false);
        List<GeneratorOutput> outputs = new ArrayList<>();
        outputs.add(new GeneratorOutput(CHARCOAL_GEN_ID, CHARCOAL_GENERATOR, charcoalW));
        outputs.add(new GeneratorOutput(DIESEL_GEN_ID, DIESEL_GENERATOR, dieselW));
        for (int i = 0; i < SOLAR_PANELS.size(); i++) {
            Generator panel = new Generator(SOLAR_IDS.get(i), SOLAR_PANELS.get(i), GeneratorKind.SOLAR, GeneratorState.EMPTY);
            outputs.add(GeneratorOutput.of(panel, panel.tick(env, DT)));
        }
        return outputs;
    }

    /** One solve of the grid with the layout's pylons and wires, a fresh grid registry and the given generator outputs. */
    static EnergyResult solveWith(List<GeneratorOutput> outputs) {
        return EnergyNetwork.solve(new EnergySnapshot(PYLONS, outputs, wires(), List.of(), List.of(),
                allConsumers(labDemandW()), GridRegistry.EMPTY), DT);
    }

    /** The wires: the charcoal generator and the six panels to pylon A, the diesel generator to pylon E, and the pylons in a chain. */
    static List<Wire> wires() {
        List<Wire> wires = new ArrayList<>();
        wires.add(new Wire("w-charcoal", CHARCOAL_GEN_ID, "pylon-a"));
        for (int i = 0; i < SOLAR_IDS.size(); i++) {
            wires.add(new Wire("w-" + SOLAR_IDS.get(i), SOLAR_IDS.get(i), "pylon-a"));
        }
        wires.add(new Wire("w-a-b", "pylon-a", "pylon-b"));
        wires.add(new Wire("w-b-e", "pylon-b", "pylon-e"));
        wires.add(new Wire("w-e-c", "pylon-e", "pylon-c"));
        wires.add(new Wire("w-diesel", DIESEL_GEN_ID, "pylon-e"));
        return wires;
    }

    /** A chest of slots, each with a full stack of charcoal. */
    static List<Stack> full(int slotCount) {
        List<Stack> slots = ScenarioSupport.slots(slotCount);
        for (Stack slot : slots) {
            slot.set(CHARCOAL_REF, STACK_LIMIT);
        }
        return slots;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // The simulation: the plugin's tick, step by step, with the Core models.
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * One run of the production line. The world is read from the bundled YAML; each tick the generators run first, then the
     * energy grid is solved, then the drills, the refinery, the crushers and the belts run with the satisfaction that solve gave.
     * The checks made at each tick are counted here and asserted by the tests.
     */
    private static final class Line {

        private final ProcessCatalog catalog;
        private final ProcessEngine engine;
        private final LabLevels labs;
        private final DrillYield yield;
        private final DrillDefinition oilDrill;
        private final DrillDefinition oreDrill;
        private final Map<String, OreDefinition> ores;
        private final Set<String> knownOres;
        private final int depositRadius;
        private final MemoryDepositStore store = new MemoryDepositStore();
        private final OilJoinPlan tubes;
        private final OilNetwork intake;
        private final OilNetwork outlet;
        private final LogisticsNetwork logistics;
        private final List<Consumer> consumers;
        private final Map<String, String> nodeConsumers = new HashMap<>();
        private final Map<Pos, Port> ports = new HashMap<>();
        private final Map<String, Double> satisfaction = new HashMap<>();
        private final ChestPort feedChest;
        private final FuelPort charcoalFuel;
        private final ChestPort copperChest;
        private final List<Stack> barrel;
        private final MachineState fluoriteCrusher;
        private final MachineState saltpeterCrusher;
        private final DrillAccumulator pumpAccumulator = new DrillAccumulator(DrillAccumulator.SECONDS_PER_TICK);
        private final DrillAccumulator oreAccumulator = new DrillAccumulator(DrillAccumulator.SECONDS_PER_TICK);
        private final TakeBatch oreBatch = new TakeBatch(TakeBatch.DEFAULT_SIZE, TakeBatch.DEFAULT_TICKS);
        private final DrillState oilState;
        private final Random random = new Random(1L);
        private final double labDemand = labDemandW();
        private GridRegistry registry = GridRegistry.EMPTY;
        private Long oilCurrent;
        private Long oreCurrent;
        private boolean pumpRefused;
        private double fluoriteCredit;
        private double saltpeterCredit;

        /** Totals over the run. */
        private double pumped;
        private double oreDrawn;
        private double refineryDrawn;
        private double dieselSupplied;
        private double dieselDrawn;
        private int refineryTicks;
        private int shortRefineryTicks;

        /** The checks over the run. */
        private double worstCoveredGap;
        private double worstOreSatisfaction;
        private double minLoadW = Double.MAX_VALUE;
        private double maxLoadW;
        private double minGridSatisfaction = Double.MAX_VALUE;
        private double worstDieselGapW;
        private double lastDieselW;
        private double lastCharcoalW;
        private int badCharcoalOutputs;
        private int misroutedItems;
        private int badCrusherInputs;
        private int brokenFeedBalance;

        private Line() {
            Map<String, Object> config = ScenarioSupport.bundled("config.yml");
            Map<String, Object> processes = ScenarioSupport.bundled("processes.yml");
            Map<String, Object> components = new LinkedHashMap<>(
                    ConfigValues.section(ScenarioSupport.bundled("items.yml"), "components"));
            components.putAll(ConfigValues.section(ScenarioSupport.bundled("materials.yml"), "components"));
            Map<String, Object> itemFile = new LinkedHashMap<>();
            itemFile.put("components", components);
            Map<String, ComponentDefinition> definitions = DefinitionParser.parse(null, itemFile, Set.of(), LOG).components();

            catalog = ProcessCatalog.parse(processes, LOG);
            engine = new ProcessEngine(catalog, ProcessEngine.FUEL, ref -> STACK_LIMIT);
            DrillCatalog drills = DrillCatalog.parse(processes, definitions, LOG);
            oilDrill = drills.drill("petroleum_drill_" + DRILL_LEVEL).orElseThrow();
            oreDrill = drills.drill("ore_drill_" + DRILL_LEVEL).orElseThrow();
            yield = new DrillYield(ConfigValues.numberList(config, "drill-rates", List.of()).stream()
                    .mapToDouble(Double::doubleValue).toArray());
            labs = new LabLevels(ConfigValues.numberList(config, "machine-upgrades.speed", List.of()).stream()
                    .mapToDouble(Double::doubleValue).toArray());
            OilSettings oil = OilSettings.from(config);
            DepositSettings deposits = DepositSettings.from(config, LOG);
            depositRadius = deposits.minRadius();

            Map<String, OreDefinition> byId = new LinkedHashMap<>();
            for (OreDefinition ore : OreDefinition.parse(
                    ConfigValues.optionalSection(ScenarioSupport.bundled("ores.yml"), "ores"), LOG)) {
                byId.put(ore.id(), ore);
            }
            ores = byId;
            knownOres = Set.copyOf(byId.keySet());

            long crudeAmount = deposits.minAmount();
            store.insertIfAbsent(new Deposit(CRUDE_ID, WORLD, 25, 0, depositRadius, DrillRules.DEPOSIT, crudeAmount, crudeAmount));
            store.insertIfAbsent(new Deposit(COAL_ID, WORLD, 26, -13, depositRadius, "coal_ore", crudeAmount, COAL_START));
            store.insertIfAbsent(new Deposit(FLUORITE_ID, WORLD, 26, -13, depositRadius, "fluorite_ore", crudeAmount, FLUORITE_START));
            store.insertIfAbsent(new Deposit(SALTPETER_ID, WORLD, 26, -13, depositRadius, "saltpeter_deposit", crudeAmount, SALTPETER_START));

            tubes = oilTubes();
            intake = new OilNetwork(tanksOf(tubes, tubes.networkOf(INTAKE_TUBE), oil.tubeCapacityLitres()));
            outlet = new OilNetwork(tanksOf(tubes, tubes.networkOf(OUTLET_TUBE), oil.tubeCapacityLitres()));
            outlet.supply(Fluid.DIESEL, SEED_DIESEL_LITRES);

            logistics = logisticsNetwork();
            consumers = allConsumers(labDemand);
            for (Consumer node : logistics.consumers()) {
                nodeConsumers.put(node.id(), consumerId(node.pos()));
            }

            List<Stack> feedSlots = full(54);
            feedChest = new ChestPort(feedSlots);
            copperChest = new ChestPort(ScenarioSupport.slots(54));
            charcoalFuel = new FuelPort(GeneratorKind.CHARCOAL, GeneratorState.EMPTY);
            barrel = ScenarioSupport.slots(DrillTable.OUTPUT_SLOTS);
            List<Stack> fluoriteInputs = ScenarioSupport.slots(1);
            List<Stack> saltpeterInputs = ScenarioSupport.slots(1);
            fluoriteCrusher = new MachineState("crusher", fluoriteInputs, ScenarioSupport.slots(3), List.of());
            saltpeterCrusher = new MachineState("crusher", saltpeterInputs, ScenarioSupport.slots(3), List.of());
            ports.put(FEED_CHEST, feedChest);
            ports.put(CHARCOAL_GENERATOR, charcoalFuel);
            ports.put(ORE_DRILL, new BarrelPort(barrel));
            ports.put(CRUSHER_FLUORITE, new MachineInputPort(catalog, "crusher", fluoriteInputs));
            ports.put(CRUSHER_SALTPETER, new MachineInputPort(catalog, "crusher", saltpeterInputs));
            ports.put(COPPER_CHEST, copperChest);

            oilState = new DrillState(oilDrill);
        }

        /** Runs the line for a number of ticks from the starting world time. */
        static Line run(int ticks) {
            Line line = new Line();
            for (int tick = 0; tick < ticks; tick++) {
                line.step(START_TICK + tick);
            }
            return line;
        }

        /** The deposit with this id, as the store holds it. */
        Deposit deposit(long id) {
            return store.find(id).orElseThrow();
        }

        /** How much of a deposit is left, from the store. */
        long depositRemaining(long id) {
            return store.find(id).orElseThrow().remaining();
        }

        private double sat(Pos pos) {
            return satisfaction.get(consumerId(pos));
        }

        private List<Deposit> inRange(DrillDefinition drill, Pos at) {
            DrillBox box = DrillBox.around(at.x(), at.y(), at.z(), drill.spec());
            List<Deposit> found = store.overlapping(WORLD, at.x(), at.z(), DrillDeposits.reach(drill.spec()));
            return DrillDeposits.inSquare(found, box, drill.kind(), knownOres::contains);
        }

        /** The item an ore gives: the drill's product of the ore, as the Core maps it. */
        private ItemRef productOf(String oreId) {
            return DrillRules.mined(oreId, ores.get(oreId).drop());
        }

        private void step(long worldTick) {
            GeneratorEnvironment env = GeneratorEnvironment.at(worldTick, SEED, OPEN_AIR, false);

            // Generators. The charcoal generator burns what its fuel port holds; the panels follow the sky; the diesel generator
            // draws the litres it wants from the outlet network it stands on, before the grid is solved.
            GeneratorStep charcoal = new Generator(CHARCOAL_GEN_ID, CHARCOAL_GENERATOR, GeneratorKind.CHARCOAL, charcoalFuel.state())
                    .tick(env, DT);
            charcoalFuel.state(charcoal.state());
            List<GeneratorOutput> outputs = new ArrayList<>();
            outputs.add(new GeneratorOutput(CHARCOAL_GEN_ID, CHARCOAL_GENERATOR, charcoal.outputW()));
            for (int i = 0; i < SOLAR_PANELS.size(); i++) {
                Generator panel = new Generator(SOLAR_IDS.get(i), SOLAR_PANELS.get(i), GeneratorKind.SOLAR, GeneratorState.EMPTY);
                outputs.add(GeneratorOutput.of(panel, panel.tick(env, DT)));
            }
            Generator diesel = new Generator(DIESEL_GEN_ID, DIESEL_GENERATOR, GeneratorKind.DIESEL, GeneratorState.EMPTY);
            double dieselDrawnNow = outlet.draw(Fluid.DIESEL, diesel.litresWanted(DT));
            GeneratorStep dieselStep = diesel.tickFluid(env, DT, dieselDrawnNow);
            outputs.add(GeneratorOutput.of(diesel, dieselStep));
            dieselDrawn += dieselDrawnNow;
            lastCharcoalW = charcoal.outputW();
            lastDieselW = dieselStep.outputW();
            worstDieselGapW = Math.max(worstDieselGapW,
                    Math.abs(dieselStep.outputW() - GeneratorKind.DIESEL.ratedW()));
            if (charcoal.outputW() != 0.0 && charcoal.outputW() != GeneratorKind.CHARCOAL.ratedW()) {
                badCharcoalOutputs++;
            }

            // The grid: one solve for the tick, which every device then reads.
            EnergyResult energy = EnergyNetwork.solve(
                    new EnergySnapshot(PYLONS, outputs, wires(), List.of(), List.of(), consumers, registry), DT);
            registry = energy.gridIds();
            satisfaction.clear();
            for (ConsumerResult result : energy.consumers()) {
                satisfaction.put(result.id(), result.satisfaction());
            }
            GridResult grid = energy.grids().get(0);
            minLoadW = Math.min(minLoadW, grid.loadW());
            maxLoadW = Math.max(maxLoadW, grid.loadW());
            minGridSatisfaction = Math.min(minGridSatisfaction, grid.satisfaction());
            for (Consumer consumer : consumers) {
                double s = satisfaction.get(consumer.id());
                if (consumer.pos().equals(ORE_DRILL)) {
                    worstOreSatisfaction = Math.max(worstOreSatisfaction, s);
                } else {
                    worstCoveredGap = Math.max(worstCoveredGap, Math.abs(1.0 - s));
                }
            }

            pumpPetroleum();
            refine();
            runOreDrill();
            runCrusher(fluoriteCrusher, CRUSHER_FLUORITE, true);
            runCrusher(saltpeterCrusher, CRUSHER_SALTPETER, false);

            Map<String, Double> nodeSatisfaction = new HashMap<>();
            for (Map.Entry<String, String> node : nodeConsumers.entrySet()) {
                nodeSatisfaction.put(node.getKey(), satisfaction.get(node.getValue()));
            }
            logistics.tick(DT, nodeSatisfaction, ports);
            checkItems();
        }

        /** The petroleum drill: pumps whole litres into the intake network while it has power and a deposit. */
        private void pumpPetroleum() {
            Pos at = OIL_DRILL;
            List<Deposit> inSquare = inRange(oilDrill, at);
            Optional<Deposit> crude = DrillDeposits.choose(DrillDeposits.mineable(inSquare, new DrillSelection()));
            oilCurrent = crude.map(Deposit::id).orElse(null);
            DrillPower power = DrillRules.withGrid(DrillRules.petroleumPower(0, List.of(), false), sat(at));
            if (power.powered() && crude.isPresent()) {
                double rate = yield.itemsPerSecond(DRILL_LEVEL) * power.efficiency();
                long whole = pumpAccumulator.step(rate, crude.get().remaining());
                if (whole > 0) {
                    double accepted = intake.supply(Fluid.PETROLEUM, whole);
                    pumpRefused = accepted <= 0;
                    pumped += accepted;
                    long litres = (long) Math.floor(accepted);
                    if (litres > 0) {
                        store.take(crude.get().id(), litres);
                    }
                }
            }
            DrillStatus status = DrillRules.status(power.powered(), !pumpRefused, !inSquare.isEmpty(), crude.isPresent());
            if (status == DrillStatus.RUNNING) {
                oilState.advance(power.efficiency());
                if (oilState.ready()) {
                    oilState.finishCycle(false);
                }
            }
        }

        /** The refinery: takes petroleum at its full rate, gives diesel to the outlet, and draws less when the outlet refuses. */
        private void refine() {
            double wanted = Refinery.petroleumWanted(DT);
            double available = intake.litres();
            double petroleum = Math.min(wanted, available);
            double diesel = Refinery.dieselFrom(petroleum);
            double accepted = outlet.supply(Fluid.DIESEL, diesel);
            if (accepted < diesel) {
                petroleum = accepted / Refinery.DIESEL_PER_PETROLEUM;
            }
            double drawn = petroleum > 0 ? intake.draw(Fluid.PETROLEUM, petroleum) : 0.0;
            refineryDrawn += drawn;
            dieselSupplied += accepted;
            if (available >= wanted) {
                refineryTicks++;
                if (Math.abs(drawn - wanted) > 1e-12) {
                    shortRefineryTicks++;
                }
            }
        }

        /** The ore drill's own petroleum: the Core's rate for its kind and level, drawn from the intake network each tick. */
        private boolean drawOreDrillPetroleum() {
            double wanted = oreDrill.kind().litresPerTick(oreDrill.spec().cycleTicks());
            double drawn = intake.draw(Fluid.PETROLEUM, wanted);
            oreDrawn += drawn;
            return drawn + 1e-9 >= wanted;
        }

        /**
         * The ore drill: searches its square, draws the petroleum it burns only on a tick it would run, produces into its batch while
         * it has room, and takes the batch into its barrel.
         */
        private void runOreDrill() {
            List<Deposit> inSquare = inRange(oreDrill, ORE_DRILL);
            Optional<Deposit> chosen = DrillDeposits.choose(DrillDeposits.mineable(inSquare, new DrillSelection()));
            Deposit current = oreCurrent == null ? null : store.find(oreCurrent).orElse(null);
            Optional<Deposit> ore = DrillDeposits.afterBatch(chosen, current, oreBatch.pending());
            oreCurrent = ore.map(Deposit::id).orElse(null);
            ItemRef product = ore.map(deposit -> productOf(deposit.ore())).orElse(null);

            long freeSpace = DrillStorage.room(barrel, product, STACK_LIMIT) - oreBatch.pending();
            boolean draws = DrillRules.drawsFuel(DrillKind.ORE, freeSpace > 0, !inSquare.isEmpty(), ore.isPresent());
            boolean fed = draws && drawOreDrillPetroleum();
            DrillPower power = DrillRules.withGrid(DrillRules.orePower(fed), sat(ORE_DRILL));
            DrillStatus status = DrillRules.status(power.powered() || !draws, freeSpace > 0, !inSquare.isEmpty(), ore.isPresent());
            if (status == DrillStatus.RUNNING) {
                long available = ore.get().remaining() - oreBatch.pending();
                if (available > 0 && freeSpace > 0) {
                    double rate = yield.itemsPerSecond(DRILL_LEVEL) * power.efficiency();
                    oreBatch.produce(Math.min(oreAccumulator.step(rate, available), freeSpace));
                }
            }
            if (oreBatch.due() && ore.isPresent()) {
                long amount = Math.min(oreBatch.pending(), DrillStorage.room(barrel, product, STACK_LIMIT));
                if (amount > 0) {
                    long taken = store.take(ore.get().id(), amount);
                    if (taken > 0) {
                        DrillStorage.add(barrel, product, (int) taken, STACK_LIMIT);
                    }
                    oreBatch.settle(amount, taken);
                }
            } else if (ore.isEmpty() && oreBatch.pending() > 0) {
                oreBatch.discard();
            }
            oreBatch.tick();
        }

        /** A lab machine: runs its process ticks at its satisfaction times its speed, as the plugin paces a lab machine. */
        private void runCrusher(MachineState crusher, Pos at, boolean fluorite) {
            double credit = fluorite ? fluoriteCredit : saltpeterCredit;
            LabLevels.Pace pace = LabLevels.pace(credit, sat(at), labs.speed(1));
            for (int run = 0; run < pace.runs(); run++) {
                ProcessEngine.Step step = engine.tick(crusher, random, false);
                if (step == ProcessEngine.Step.IDLE || step == ProcessEngine.Step.BLOCKED) {
                    break;
                }
            }
            if (fluorite) {
                fluoriteCredit = pace.credit();
            } else {
                saltpeterCredit = pace.credit();
            }
        }

        /** The item checks of the tick: where each item is, and whether the feed balances. */
        private void checkItems() {
            for (BeltItem item : logistics.itemsOn(LEFT_BELT)) {
                if (!FLUORITE_ITEM.equals(item.item())) {
                    misroutedItems++;
                }
            }
            for (BeltItem item : logistics.itemsOn(RIGHT_BELT)) {
                if (!SALTPETER_ITEM.equals(item.item())) {
                    misroutedItems++;
                }
            }
            for (BeltItem item : logistics.itemsOn(MIDDLE_BELT)) {
                if (!CHARCOAL_ITEM.equals(item.item())) {
                    misroutedItems++;
                }
            }
            badCrusherInputs += countNot(fluoriteCrusher.inputs(), RAW_FLUORITE);
            badCrusherInputs += countNot(saltpeterCrusher.inputs(), RAW_SALTPETER);

            int onFeedBelts = 0;
            for (int x = FEED_BELT_FIRST_X; x <= FEED_BELT_LAST_X; x++) {
                onFeedBelts += logistics.itemsOn(at(x, 0)).size();
            }
            if (feedChest.storedCount() + charcoalFuel.accepted() + onFeedBelts != CHARCOAL_FEED_START) {
                brokenFeedBalance++;
            }
        }

        /** Items in the slots that are not the given item, which the crusher's input must never hold. */
        private static int countNot(List<Stack> slots, ItemRef expected) {
            int wrong = 0;
            for (Stack slot : slots) {
                if (slot.count() > 0 && !expected.equals(slot.ref())) {
                    wrong += slot.count();
                }
            }
            return wrong;
        }
    }
}
