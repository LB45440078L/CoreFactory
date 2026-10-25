package io.corefactory.core.scenario;

import io.corefactory.core.config.TestConfig;
import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.ConsumerResult;
import io.corefactory.core.energy.DeviceKind;
import io.corefactory.core.energy.Endpoint;
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
import io.corefactory.core.energy.GridGraph;
import io.corefactory.core.energy.GridRegistry;
import io.corefactory.core.energy.GridResult;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.energy.Pylon;
import io.corefactory.core.energy.PylonType;
import io.corefactory.core.energy.Units;
import io.corefactory.core.energy.Wire;
import io.corefactory.core.energy.WireCheck;
import io.corefactory.core.energy.WireRules;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.MachineState;
import io.corefactory.core.process.Portion;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessDefinition;
import io.corefactory.core.process.ProcessEngine;
import io.corefactory.core.process.Product;
import io.corefactory.core.process.Stack;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.PumpFuel;
import io.corefactory.core.water.PumpLevels;
import io.corefactory.core.water.WaterFlow;
import io.corefactory.core.water.WaterGrid;
import io.corefactory.core.water.WaterKind;
import io.corefactory.core.water.WaterModels;
import io.corefactory.core.water.WaterNode;
import io.corefactory.core.water.WaterOrientation;
import io.corefactory.core.water.WaterRules;
import io.corefactory.core.water.WaterSettings;
import io.corefactory.core.water.WaterSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A scenario over many ticks: a level IV water pump with a pipe of 36 pipes that ends in a pool, and a second pipe of 37 pipes
 * with two T splitters, whose first T feeds a purifier. The pump is powered by two level III pylons and 32 charcoal generators.
 * The models are the Core's; the numbers are read from the bundled config.yml and processes.yml, or taken from the energy and
 * water rules, with the source named beside each.
 *
 * <p>The Core does not model these, so the scenario does not check them: where a block may be placed, the pump's upper block,
 * the water a splitter sends down each branch (WaterFlow has no branch ratio), and the Paper service's request windows.
 */
class WaterScenarioTest {

    private static final Logger LOG = Logger.getLogger("water-scenario-test");
    private static final Path RESOURCES = Path.of("..", "CoreFactory-Paper", "src", "main", "resources");
    private static final double DT = EnergyTable.SECONDS_PER_TICK;
    private static final double EPS = 1e-9;

    /** Ten minutes of game time at 20 ticks per second. */
    private static final int RUN_TICKS = 10 * 60 * EnergyTable.TICKS_PER_SECOND;
    private static final int LEVEL = 4;
    private static final int GENERATORS = 32;
    private static final int CHARGE_ITEMS = 16;

    /** Ticks the generators burn for: 16 items of 30 s, see GeneratorKind.burnSeconds, at 20 ticks per second: 9600. */
    private static final int BURN_TICKS =
            (int) Math.round(CHARGE_ITEMS * GeneratorKind.CHARCOAL.burnSeconds() * EnergyTable.TICKS_PER_SECOND);

    private static final String HEAVY_PROCESS = "electrolyse_heavy_water";
    private static final ItemRef WATER_BUCKET = ItemRef.material("WATER_BUCKET");
    private static final ItemRef EMPTY_BUCKET = ItemRef.material("BUCKET");
    private static final ItemRef DEUTERIUM_CELL = ItemRef.item("deuterium_cell");

    private static final String PUMP_ID = "pump-a";
    private static final String PYLON_1 = "pylon-1";
    private static final String PYLON_2 = "pylon-2";
    private static final Pos PYLON_1_POS = new Pos(4, 70, 4);
    private static final Pos PYLON_2_POS = new Pos(4, 70, 16);

    private record Config(WaterSettings water, ProcessCatalog processes) {

        static Config load() {
            return new Config(WaterSettings.from(yaml("config.yml")), ProcessCatalog.parse(yaml("processes.yml"), LOG));
        }
    }

    private static final Config CONFIG = Config.load();
    private static final double[] FLOW = CONFIG.water().flowTable();
    private static final int HEAVY_TICKS = CONFIG.processes().process(HEAVY_PROCESS).orElseThrow().ticks();

    // ---------------------------------------------------------------- the water geometry

    private static final Pos PUMP = new Pos(0, 64, 0);

    /** Pipe A: from the pump's west face, west 6, south 6, up 2, east 5, north 16. 36 pipes, ending in the pool. */
    private static final List<Pos> PIPES_A = pipeRunA();
    private static final Pos PIPE_A_END = PIPES_A.get(PIPES_A.size() - 1);

    /** Pipe B's first part: from the pump's east face, 23 pipes to the input of the first T. */
    private static final List<Pos> RUN_B = pipeRunB();
    private static final Pos FIRST_T = new Pos(10, 66, 12);
    /** The first T's left side: its first cell is the T itself, then 12 pipes west, the last joins the second T. */
    private static final List<Pos> LEFT_RUN = walk(List.of(FIRST_T), Face.WEST, 12);
    private static final Pos SECOND_T = Face.WEST.step(LEFT_RUN.get(LEFT_RUN.size() - 1));
    /** The first T's right side: the purifier stands on the T's output face and joins it directly. */
    private static final Pos PURIFIER = Face.EAST.step(FIRST_T);
    /** The second T's two outputs, which are dead-end stubs, and a second consumer at the end of the south stub. */
    private static final Pos STUB_NORTH = Face.NORTH.step(SECOND_T);
    private static final Pos STUB_SOUTH = Face.SOUTH.step(SECOND_T);
    private static final Pos CONSUMER_TWO = Face.SOUTH.step(STUB_SOUTH);
    /** Pipe B: 23 + 12 + the two stubs = 37 pipes. */
    private static final List<Pos> PIPES_B = pipesB();

    /** The pool at the end of pipe A: 9 by 10 by 2 blocks, 180 in all, more than the cap of 101. */
    private static final Set<Pos> POOL = slab(-6, 2, 65, 66, -20, -11);

    private static final WaterGrid GRID = WaterGrid.of(waterBlocks());

    // ---------------------------------------------------------------- the power geometry

    private static final GridRegistry NO_GRIDS = GridRegistry.EMPTY;

    /** The simulation of the whole scenario, run once for the tests that read it. */
    private static final Simulation SIM = Simulation.run();

    // ---------------------------------------------------------------- the pump and its settings

    @Test
    void theLevelFourPumpTakesItsFlowAndPowerFromConfig() {
        WaterSettings water = CONFIG.water();

        // water.pump-flow, the fourth of 2, 4, 8, 12, 16 (config.yml lines 305 to 309): level IV gives 12 units per second
        assertThat(water.flowTable()[LEVEL - 1]).isEqualTo(12.0);
        // water.pump-power-w, the fourth of 0, 0, 60000, 120000, 240000 (lines 313 to 317): level IV draws 120 kW
        assertThat(water.pumpPowerW().get(LEVEL - 1)).isEqualTo(120_000);
        // water.source-min-blocks (line 298), source-check-ticks (line 300), solve-ticks (line 302), unit-per-process-run (line 320)
        assertThat(water.sourceMinBlocks()).isEqualTo(100);
        assertThat(water.sourceCheckTicks()).isEqualTo(200);
        assertThat(water.solveTicks()).isEqualTo(5);
        assertThat(water.unitPerProcessRun()).isEqualTo(1);
    }

    @Test
    void aLevelFourPumpNeedsPowerAndNoFuel() {
        // PumpLevels.FIRST_POWERED_LEVEL is 3: level IV draws power, and it is two blocks tall from level IV
        assertThat(PumpLevels.powered(LEVEL)).isTrue();
        assertThat(PumpLevels.height(LEVEL)).isEqualTo(2);

        // Coal or charcoal is fuel for level I only; level II burns diesel; a level IV pump takes no fuel item
        ItemRef charcoal = ItemRef.material("CHARCOAL");
        assertThat(PumpFuel.accepts(LEVEL, charcoal)).isFalse();
        assertThat(PumpFuel.accepts(1, charcoal)).isTrue();
        assertThat(PumpFuel.drawsFluid(LEVEL)).isFalse();
        assertThat(PumpFuel.litresWanted(LEVEL)).isZero();
    }

    @Test
    void aLevelOnePumpStopsWhenItsFuelRunsOut() {
        // The fuel rule a level IV pump does not have: one item burns ITEM_TICKS (1200, 60 s) ticks, then the pump stops
        PumpFuel.Burn burn = new PumpFuel.Burn(1, 0);
        int running = 0;
        for (int tick = 0; tick < PumpFuel.ITEM_TICKS + 1; tick++) {
            PumpFuel.Step step = PumpFuel.step(burn, true);
            if (step.running()) {
                running++;
            }
            burn = step.burn();
        }
        assertThat(running).isEqualTo(PumpFuel.ITEM_TICKS);
        assertThat(burn.held()).isZero();
    }

    @Test
    void aLevelFourPumpGivesItsFullFlowToAConsumerThatAsksForMore() {
        WaterFlow.Result result = WaterFlow.solve(GRID, FLOW,
                Map.of(PUMP, new WaterFlow.PumpSupply(LEVEL, true)), Map.of(PURIFIER, 20.0));
        // The flow is 12 units per second, from the table; the one consumer asking for 20 gets all 12 of it
        assertThat(result.pumpOutput().get(PUMP)).isCloseTo(12.0, within(EPS));
        assertThat(result.consumerDelivered().get(PURIFIER)).isCloseTo(12.0, within(EPS));

        WaterFlow.Result stopped = WaterFlow.solve(GRID, FLOW,
                Map.of(PUMP, new WaterFlow.PumpSupply(LEVEL, false)), Map.of(PURIFIER, 20.0));
        assertThat(stopped.pumpOutput().get(PUMP)).isZero();
        assertThat(stopped.consumerDelivered().get(PURIFIER)).isZero();
    }

    @Test
    void aPumpSourceNeedsOneHundredConnectedWaterBlocks() {
        int minimum = CONFIG.water().sourceMinBlocks();
        Pos end = PIPE_A_END;
        Set<Pos> ninetyNine = slab(-6, 2, 66, 66, -21, -11);   // 9 by 11 by 1 = 99 blocks, touching the end
        Set<Pos> hundred = slab(-6, 3, 66, 66, -20, -11);      // 10 by 10 by 1 = 100 blocks, touching the end

        // A pipe end is not water, so the count starts from the water on its faces (WaterSource.sourceAround)
        assertThat(WaterSource.sourceAround(end.x(), end.y(), end.z(), probe(ninetyNine), minimum + 1)).isEqualTo(99);
        assertThat(WaterSource.sourceAround(end.x(), end.y(), end.z(), probe(hundred), minimum + 1)).isEqualTo(100);
        // The real pool is counted up to the cap of minimum plus one, which the Paper service uses (WaterService.networkSource)
        assertThat(WaterSource.sourceAround(end.x(), end.y(), end.z(), probe(POOL), minimum + 1)).isEqualTo(minimum + 1);
    }

    // ---------------------------------------------------------------- the pipes and their bends

    @Test
    void pipeAHasThirtySixPipesWithTwoVerticalAndTwoHorizontalBends() {
        // 1 + 6 + 6 + 2 + 5 + 16 = 36 pipes: at least 35, as the scenario asks
        assertThat(PIPES_A).hasSize(36);
        assertThat(bends(PIPES_A)).hasSize(4);
        assertThat(verticalBends(PIPES_A)).isEqualTo(2);
        assertThat(PIPES_A.stream().anyMatch(pipe -> shape(pipe) == WaterOrientation.PipeShape.JUNCTION)).isFalse();
    }

    @Test
    void pipeBHasThirtySevenPipesWithSixHorizontalAndTwoVerticalBends() {
        // 23 + 12 + the two stubs = 37 pipes, at least 35, as the scenario asks
        assertThat(PIPES_B).hasSize(37);
        assertThat(bends(PIPES_B)).hasSize(8);
        assertThat(verticalBends(PIPES_B)).isEqualTo(2);
        assertThat(PIPES_B.stream().anyMatch(pipe -> shape(pipe) == WaterOrientation.PipeShape.JUNCTION)).isFalse();
    }

    @Test
    void theBendsTakeTheModelsOfTheirFacePairs() {
        Map<Pos, String> models = new HashMap<>();
        for (Pos pipe : bends(PIPES_A)) {
            models.put(pipe, WaterModels.pipe(GRID.connectedFaces(pipe)));
        }
        for (Pos pipe : bends(PIPES_B)) {
            models.put(pipe, WaterModels.pipe(GRID.connectedFaces(pipe)));
        }
        // WaterModels.bend names the pair with the face earlier in Face first: NORTH, SOUTH, EAST, WEST, UP, DOWN
        assertThat(models).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                // pipe A: horizontal at its first corner, vertical twice, horizontal at its second corner
                Map.entry(new Pos(-7, 64, 0), "water_pipe_bend_south_east"),
                Map.entry(new Pos(-7, 64, 6), "water_pipe_bend_north_up"),
                Map.entry(new Pos(-7, 66, 6), "water_pipe_bend_east_down"),
                Map.entry(new Pos(-2, 66, 6), "water_pipe_bend_north_west"),
                // pipe B: six horizontal bends and two vertical ones
                Map.entry(new Pos(4, 64, 0), "water_pipe_bend_south_west"),
                Map.entry(new Pos(4, 64, 2), "water_pipe_bend_north_east"),
                Map.entry(new Pos(6, 64, 2), "water_pipe_bend_south_west"),
                Map.entry(new Pos(6, 64, 4), "water_pipe_bend_north_east"),
                Map.entry(new Pos(8, 64, 4), "water_pipe_bend_west_up"),
                Map.entry(new Pos(8, 66, 4), "water_pipe_bend_south_down"),
                Map.entry(new Pos(8, 66, 6), "water_pipe_bend_north_east"),
                Map.entry(new Pos(10, 66, 6), "water_pipe_bend_south_west")));
        // every bend model is one the resource pack has
        assertThat(WaterModels.all()).containsAll(models.values());
    }

    @Test
    void theStraightPipesTakeTheStraightModels() {
        // the first pipe of A joins the pump on its east face and a pipe on its west face: east-west, a straight run
        assertThat(WaterModels.pipe(GRID.connectedFaces(new Pos(-1, 64, 0)))).isEqualTo("water_pipe");
        assertThat(WaterModels.pipe(GRID.connectedFaces(new Pos(4, 64, 1)))).isEqualTo("water_pipe_z");
        assertThat(WaterModels.pipe(GRID.connectedFaces(new Pos(-7, 65, 6)))).isEqualTo("water_pipe_y");
        // the end of pipe A joins only its south face, so it is a straight run along north-south
        assertThat(WaterModels.pipe(GRID.connectedFaces(PIPE_A_END))).isEqualTo("water_pipe_z");
    }

    @Test
    void theWaterReachesBothPipeEnds() {
        assertThat(GRID.networks()).hasSize(1);
        int network = GRID.networkOf(PUMP);
        assertThat(GRID.connectedFaces(PUMP)).containsExactlyInAnyOrder(Face.EAST, Face.WEST);
        assertThat(GRID.networkOf(PIPE_A_END)).isEqualTo(network);
        assertThat(GRID.networkOf(LEFT_RUN.get(1))).isEqualTo(network);
        assertThat(GRID.networkOf(STUB_NORTH)).isEqualTo(network);
        assertThat(GRID.networkOf(STUB_SOUTH)).isEqualTo(network);
        assertThat(GRID.networkOf(PURIFIER)).isEqualTo(network);
        // the network's source is the pool, counted to the cap of 101 (minimum 100 plus one)
        assertThat(networkSource(GRID, POOL, CONFIG.water().sourceMinBlocks() + 1)).isEqualTo(101);
    }

    // ---------------------------------------------------------------- the splitters and the purifier

    @Test
    void aSplitterJoinsItsInputAndBothOutputsAndNothingElse() {
        assertThat(GRID.connectedFaces(FIRST_T)).containsExactlyInAnyOrder(Face.NORTH, Face.WEST, Face.EAST);
        assertThat(GRID.connectedFaces(SECOND_T)).containsExactlyInAnyOrder(Face.EAST, Face.NORTH, Face.SOUTH);
        // A T's model is named by its input and the first of its two outputs, so the pair across the input gives one model
        assertThat(WaterModels.tee(Face.NORTH, Face.WEST)).isEqualTo("water_splitter_t_north_east");
        assertThat(WaterModels.tee(Face.NORTH, Face.EAST)).isEqualTo("water_splitter_t_north_east");
        assertThat(WaterModels.tee(Face.EAST, Face.NORTH)).isEqualTo("water_splitter_t_east_north");
    }

    @Test
    void aSplitterFeedsAPurifierStandingOnItsOutput() {
        // The purifier sits on the T's output face (north), so it joins the T and takes the water the T sends north
        assertThat(WaterRules.kindsConnect(WaterKind.SPLITTER_T, WaterKind.CONSUMER)).isTrue();

        Pos pump = new Pos(0, 64, 0);
        Pos pipe = new Pos(1, 64, 0);
        Pos tee = new Pos(2, 64, 0);
        Pos purifier = new Pos(2, 64, -1);
        WaterGrid grid = WaterGrid.of(List.of(
                WaterNode.pump(pump),
                WaterNode.pipe(pipe),
                WaterNode.splitter(tee, WaterKind.SPLITTER_T, Face.WEST, Face.NORTH),
                WaterNode.consumer(purifier, Set.of(Face.SOUTH))));

        assertThat(grid.connectedFaces(tee)).containsExactlyInAnyOrder(Face.WEST, Face.NORTH);
        assertThat(grid.networkOf(purifier)).isEqualTo(grid.networkOf(pump));
        WaterFlow.Result result = WaterFlow.solve(grid, FLOW,
                Map.of(pump, new WaterFlow.PumpSupply(LEVEL, true)), Map.of(purifier, 0.05));
        assertThat(result.consumerDelivered().get(purifier)).isCloseTo(0.05, within(1e-9));
    }

    @Test
    void theSplitGivesEachAskingConsumerItsShareWhicheverBranchItIsOn() {
        // WaterFlow has no branch ratio: a splitter only joins the network, and the water is shared by requests (max-min fair).
        // The purifier, on the first T's right, and the consumer at the second T's end each ask for 20 of the 12 the pump gives.
        WaterFlow.Result result = WaterFlow.solve(GRID, FLOW,
                Map.of(PUMP, new WaterFlow.PumpSupply(LEVEL, true)), Map.of(PURIFIER, 20.0, CONSUMER_TWO, 20.0));
        assertThat(result.consumerDelivered().get(PURIFIER)).isCloseTo(6.0, within(EPS));
        assertThat(result.consumerDelivered().get(CONSUMER_TWO)).isCloseTo(6.0, within(EPS));
    }

    @Test
    void thePurifierIsFedDirectlyByTheFirstTSplitter() {
        assertThat(GRID.connectedFaces(PURIFIER)).containsExactly(Face.WEST);
        assertThat(GRID.connectedFaces(FIRST_T)).contains(Face.EAST);
    }

    @Test
    void theHeavyWaterProcessIsElectrolyseHeavyWaterOnThePurifier() {
        // processes.yml lines 281 to 288: there is no distillation process, the heavy water one is electrolysis
        ProcessDefinition heavy = CONFIG.processes().process(HEAVY_PROCESS).orElseThrow();
        assertThat(heavy.machine()).isEqualTo("purifier");
        assertThat(heavy.ticks()).isEqualTo(400);
        assertThat(heavy.fuel()).isFalse();
        assertThat(heavy.inputs()).containsExactly(new Portion(WATER_BUCKET, 2));
        assertThat(heavy.outputs()).containsExactly(new Product(DEUTERIUM_CELL, 1, 1.0), new Product(EMPTY_BUCKET, 2, 1.0));
    }

    @Test
    void aWateredPurifierRunTakesFourHundredTicksAndMakesOneDeuteriumCell() {
        ProcessEngine engine = engine();
        MachineState purifier = new MachineState("purifier", slots(2), slots(2), List.of());

        // The first tick starts the run and each later one counts a tick; the run ends on tick 400, so 399 ticks are RUNNING
        for (int tick = 1; tick < HEAVY_TICKS; tick++) {
            assertThat(engine.tick(purifier, noChance(), true)).isEqualTo(ProcessEngine.Step.RUNNING);
        }
        assertThat(engine.tick(purifier, noChance(), true)).isEqualTo(ProcessEngine.Step.COMPLETED);
        assertThat(count(purifier.outputs(), DEUTERIUM_CELL)).isEqualTo(1);
        // The water came from the pipe, so the run gives back no empty bucket
        assertThat(count(purifier.outputs(), EMPTY_BUCKET)).isZero();
    }

    @Test
    void aPurifierWithoutWaterDoesNotStartAndLosesItsRunWhenTheWaterStops() {
        ProcessEngine engine = engine();
        MachineState purifier = new MachineState("purifier", slots(2), slots(2), List.of());

        assertThat(engine.tick(purifier, noChance(), false)).isEqualTo(ProcessEngine.Step.IDLE);
        assertThat(purifier.processId()).isNull();

        for (int tick = 0; tick < 100; tick++) {
            engine.tick(purifier, noChance(), true);
        }
        assertThat(purifier.progress()).isEqualTo(100);

        assertThat(engine.tick(purifier, noChance(), false)).isEqualTo(ProcessEngine.Step.IDLE);
        assertThat(purifier.processId()).isNull();
        assertThat(purifier.progress()).isZero();
    }

    @Test
    void thePurifierAsksForOneUnitOverItsRun() {
        // WaterService.canSupply (Paper): unit-per-process-run * 20 / process ticks = 1 * 20 / 400 = 0.05 units per second
        assertThat(purifierRate()).isCloseTo(0.05, within(1e-12));
        // over the run's 20 seconds that is the one unit of water.unit-per-process-run (config.yml line 320)
        assertThat(purifierRate() * HEAVY_TICKS / EnergyTable.TICKS_PER_SECOND)
                .isCloseTo(CONFIG.water().unitPerProcessRun(), within(EPS));
    }

    // ---------------------------------------------------------------- the power network

    @Test
    void aLevelThreePylonCarriesThreeHundredSixtyEightKilowatts() {
        // PylonType.LARGE is the level III pylon (EnergyTable.LARGE_*): 1600 A at the nominal 230 V
        assertThat(PylonType.LARGE.maxCurrentA()).isEqualTo(1600.0);
        assertThat(PylonType.LARGE.capacityW()).isEqualTo(368_000.0);
        assertThat(PylonType.LARGE.capacityW()).isEqualTo(Units.NOMINAL_VOLTAGE_V * PylonType.LARGE.maxCurrentA());
    }

    @Test
    void thePumpIsInTheRangeOfTheFirstPylonOnly() {
        Pylon first = new Pylon(PYLON_1, PYLON_1_POS, PylonType.LARGE);
        Pylon second = new Pylon(PYLON_2, PYLON_2_POS, PylonType.LARGE);
        // The half-side is 15 blocks (EnergyTable.LARGE_HALF_SIDE): the pump is 4 blocks from the first pylon on x and z, and 16 blocks
        // along z from the second
        assertThat(first.covers(PUMP)).isTrue();
        assertThat(second.covers(PUMP)).isFalse();
    }

    @Test
    void thirtyTwoGeneratorsOnTheFirstPylonFormOneGridWithTheSecond() {
        EnergySnapshot snapshot = snapshot(fullOutputs(), PUMP, pumpDemandW(), true, NO_GRIDS);
        List<GridGraph.Component> components = GridGraph.components(snapshot);
        assertThat(components).hasSize(1);
        GridGraph.Component component = components.get(0);
        assertThat(component.pylonIds()).containsExactly(PYLON_1, PYLON_2);
        assertThat(component.generatorIds()).hasSize(GENERATORS);
        // The longest wire is from the generator at (17, 70, 0) to the first pylon at (4, 70, 4): sqrt(13 * 13 + 4 * 4) blocks
        assertThat(component.wireBlocks()).isCloseTo(Math.sqrt(185.0), within(1e-9));
        for (int i = 0; i < GENERATORS; i++) {
            assertThat(WireRules.validate(
                    new Endpoint(genId(i), DeviceKind.GENERATOR, genPos(i)),
                    new Endpoint(PYLON_1, DeviceKind.PYLON, PYLON_1_POS))).isEqualTo(WireCheck.OK);
        }
        // The link between the pylons is 12 blocks, within EnergyTable.WIRE_MAX_LENGTH (24)
        assertThat(WireRules.validate(
                new Endpoint(PYLON_2, DeviceKind.PYLON, PYLON_2_POS),
                new Endpoint(PYLON_1, DeviceKind.PYLON, PYLON_1_POS))).isEqualTo(WireCheck.OK);
    }

    @Test
    void withoutTheLinkTheSecondPylonIsAGridOfItsOwn() {
        List<GridGraph.Component> apart = GridGraph.components(snapshot(fullOutputs(), PUMP, pumpDemandW(), false, NO_GRIDS));
        assertThat(apart).hasSize(2);
        GridGraph.Component alone = apart.stream().filter(c -> c.pylonIds().contains(PYLON_2)).findFirst().orElseThrow();
        assertThat(alone.generatorIds()).isEmpty();
        GridGraph.Component withGenerators = apart.stream().filter(c -> c.pylonIds().contains(PYLON_1)).findFirst().orElseThrow();
        assertThat(withGenerators.generatorIds()).hasSize(GENERATORS);
    }

    @Test
    void aCharcoalItemBurnsThirtySecondsSoSixteenLastEightMinutes() {
        GeneratorKind kind = GeneratorKind.CHARCOAL;
        // 3 MJ times 25 % is 750 kJ an item; at its 25 kW that is 30 s (EnergyTable.CHARCOAL_*)
        assertThat(kind.ratedW()).isEqualTo(25_000.0);
        assertThat(kind.burnSeconds()).isCloseTo(30.0, within(EPS));

        Generator generator = new Generator("gen", new Pos(10, 70, 0), kind, new GeneratorState(0.0, CHARGE_ITEMS));
        GeneratorEnvironment env = new GeneratorEnvironment(0L, 0L, 0.0, null, false);
        int burningTicks = 0;
        double joules = 0;
        // The generator burns whether or not anything draws from it: Generator.tick takes no load
        for (int tick = 0; tick < 10_000; tick++) {
            GeneratorStep step = generator.tick(env, DT);
            if (step.outputW() > 0) {
                burningTicks++;
            }
            joules += step.outputW() * DT;
            generator = new Generator(generator.id(), generator.pos(), kind, step.state());
        }
        // 16 items times 30 s times 20 ticks per second
        assertThat(burningTicks).isEqualTo(BURN_TICKS);
        assertThat(joules).isCloseTo(CHARGE_ITEMS * kind.electricJoulesPerItem(), within(1.0));
        assertThat(generator.state().fuelItems()).isZero();
    }

    @Test
    void aGridCapsItsSupplyAtItsPylonCapacityAndCarriesThePumpsDemand() {
        EnergyResult energy = EnergyNetwork.solve(snapshot(fullOutputs(), PUMP, pumpDemandW(), true, NO_GRIDS), DT);
        GridResult grid = energy.grids().get(0);
        // 32 generators at 25 kW give 800 kW; two level III pylons carry 2 * 368 kW = 736 kW, so the grid delivers 736 kW at most
        assertThat(grid.grid().supplyW()).isEqualTo(GENERATORS * GeneratorKind.CHARCOAL.ratedW());
        assertThat(grid.grid().capacityW()).isEqualTo(736_000.0);
        assertThat(grid.grid().deliverableW()).isEqualTo(736_000.0);
        // The pump asks for 120 kW (config.yml line 316): the grid draws that and nothing more
        assertThat(grid.loadW()).isCloseTo(120_000.0, within(1e-6));
        assertThat(grid.deliveredW()).isCloseTo(120_000.0, within(1e-6));
        assertThat(grid.satisfaction()).isCloseTo(1.0, within(EPS));
    }

    @Test
    void aPumpOutsideEveryPylonRangeGetsNoPower() {
        Pos far = new Pos(60, 64, 60);
        EnergyResult energy = EnergyNetwork.solve(snapshot(fullOutputs(), far, pumpDemandW(), true, NO_GRIDS), DT);
        ConsumerResult pump = pumpResult(energy);
        assertThat(pump.satisfaction()).isZero();
        assertThat(pump.absorbedW()).isZero();
        assertThat(pump.voltageV()).isZero();
    }

    // ---------------------------------------------------------------- the simulation

    @Test
    void theGridSatisfiesThePumpWhileTheGeneratorsBurn() {
        // 32 generators at 25 kW give 800 kW while they burn; the grid carries 736 kW and the pump draws 120 kW of it
        assertThat(SIM.supplyW[1]).isEqualTo(800_000.0);
        assertThat(SIM.supplyW[BURN_TICKS]).isEqualTo(800_000.0);
        double worst = IntStream.rangeClosed(1, BURN_TICKS).mapToDouble(t -> SIM.satisfaction[t]).min().orElseThrow();
        assertThat(worst).isCloseTo(1.0, within(EPS));
        assertThat(IntStream.rangeClosed(1, BURN_TICKS).filter(t -> SIM.pumpRuns[t]).count()).isEqualTo(BURN_TICKS);
        assertThat(SIM.firstGrid.grid().pylonIds()).containsExactly(PYLON_1, PYLON_2);
    }

    @Test
    void thePumpStopsWhenThePowerIsCut() {
        int cut = BURN_TICKS + 1;   // 9601: every generator has burnt its sixteen items
        assertThat(SIM.supplyW[cut]).isZero();
        assertThat(IntStream.rangeClosed(cut, RUN_TICKS).mapToDouble(t -> SIM.satisfaction[t]).max().orElseThrow()).isZero();
        assertThat(IntStream.rangeClosed(cut, RUN_TICKS).filter(t -> SIM.pumpRuns[t]).count()).isZero();
        // The water solves after the cut (from tick 9605, the next solve) give the pump nothing
        assertThat(SIM.solves.stream().filter(s -> s[0] >= cut).mapToDouble(s -> s[1]).max().orElseThrow()).isZero();
    }

    @Test
    void thePurifierIsWateredWhileThePumpRunsAndCompletesTwentyFourRuns() {
        // The solves run every 5 ticks (config.yml solve-ticks). The purifier is watered from the solve at tick 5 until the last
        // solve with the pump powered, which is tick 9600; the first solve without it is tick 9605.
        assertThat(SIM.watered[4]).isFalse();
        assertThat(SIM.watered[5]).isTrue();
        assertThat(SIM.watered[9604]).isTrue();
        assertThat(SIM.watered[9605]).isFalse();
        assertThat(IntStream.rangeClosed(1, RUN_TICKS).filter(t -> SIM.watered[t]).count()).isEqualTo(BURN_TICKS);

        // 9600 watered ticks at 400 ticks a run: 24 runs. The first ends at tick 404 and the last at tick 9604.
        assertThat(SIM.completions).hasSize(24);
        assertThat(SIM.completions.get(0)).isEqualTo(404);
        assertThat(SIM.completions.get(23)).isEqualTo(9604);
        assertThat(SIM.cells).isEqualTo(24);

        // At the first solve the pump gives the purifier its rate, and the pump's output is that same rate
        double[] first = SIM.solves.get(0);
        assertThat(first[0]).isEqualTo(5);
        assertThat(first[1]).isCloseTo(purifierRate(), within(1e-9));
        assertThat(first[2]).isCloseTo(purifierRate(), within(1e-9));
    }

    // ---------------------------------------------------------------- helpers

    private static Map<String, Object> yaml(String file) {
        try {
            return TestConfig.yaml(Files.readString(RESOURCES.resolve(file)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The path after walking {@code count} cells in a direction, from its last cell; the path keeps its earlier cells. */
    private static List<Pos> walk(List<Pos> path, Face direction, int count) {
        List<Pos> out = new ArrayList<>(path);
        Pos at = path.get(path.size() - 1);
        for (int i = 0; i < count; i++) {
            at = direction.step(at);
            out.add(at);
        }
        return List.copyOf(out);
    }

    /** Pipe A: its first pipe joins the pump on the pump's west face. Corners at (-7,64,0), (-7,64,6), (-7,66,6), (-2,66,6). */
    private static List<Pos> pipeRunA() {
        List<Pos> path = List.of(new Pos(-1, 64, 0));
        path = walk(path, Face.WEST, 6);
        path = walk(path, Face.SOUTH, 6);
        path = walk(path, Face.UP, 2);
        path = walk(path, Face.EAST, 5);
        path = walk(path, Face.NORTH, 16);
        return path;
    }

    /** Pipe B's first part: its first pipe joins the pump on the pump's east face. Corners: six horizontal and two vertical. */
    private static List<Pos> pipeRunB() {
        List<Pos> path = List.of(new Pos(1, 64, 0));
        path = walk(path, Face.EAST, 3);
        path = walk(path, Face.SOUTH, 2);
        path = walk(path, Face.EAST, 2);
        path = walk(path, Face.SOUTH, 2);
        path = walk(path, Face.EAST, 2);
        path = walk(path, Face.UP, 2);
        path = walk(path, Face.SOUTH, 2);
        path = walk(path, Face.EAST, 2);
        path = walk(path, Face.SOUTH, 5);
        return path;
    }

    private static List<Pos> pipesB() {
        List<Pos> pipes = new ArrayList<>(RUN_B);
        pipes.addAll(LEFT_RUN.subList(1, LEFT_RUN.size()));
        pipes.add(STUB_NORTH);
        pipes.add(STUB_SOUTH);
        return List.copyOf(pipes);
    }

    private static List<WaterNode> waterBlocks() {
        List<WaterNode> blocks = new ArrayList<>();
        blocks.add(WaterNode.pump(PUMP));
        for (Pos pipe : PIPES_A) {
            blocks.add(WaterNode.pipe(pipe));
        }
        for (Pos pipe : PIPES_B) {
            blocks.add(WaterNode.pipe(pipe));
        }
        blocks.add(WaterNode.splitter(FIRST_T, WaterKind.SPLITTER_T, Face.NORTH, Face.WEST));
        blocks.add(WaterNode.splitter(SECOND_T, WaterKind.SPLITTER_T, Face.EAST, Face.NORTH));
        blocks.add(WaterNode.consumer(PURIFIER, Set.of(Face.WEST)));
        blocks.add(WaterNode.consumer(CONSUMER_TWO, Set.of(Face.NORTH)));
        return blocks;
    }

    private static Set<Pos> slab(int x0, int x1, int y0, int y1, int z0, int z1) {
        Set<Pos> blocks = new HashSet<>();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    blocks.add(new Pos(x, y, z));
                }
            }
        }
        return Set.copyOf(blocks);
    }

    private static WaterSource.BlockProbe probe(Set<Pos> water) {
        return (x, y, z) -> water.contains(new Pos(x, y, z));
    }

    /**
     * The source of a pump's network, counted as the Paper service counts it (WaterService.networkSource): the best block of the
     * network, a pump by its own block and a pipe by the water on its faces, capped at the given count.
     */
    private static int networkSource(WaterGrid grid, Set<Pos> water, int cap) {
        WaterSource.BlockProbe isWater = probe(water);
        int best = 0;
        for (Pos pos : grid.networks().get(grid.networkOf(PUMP))) {
            WaterNode node = grid.node(pos).orElseThrow();
            if (node.kind() == WaterKind.CONSUMER) {
                continue;
            }
            int count = node.kind() == WaterKind.PUMP
                    ? WaterSource.sourceBlocks(pos.x(), pos.y(), pos.z(), isWater, cap)
                    : WaterSource.sourceAround(pos.x(), pos.y(), pos.z(), isWater, cap);
            best = Math.max(best, count);
            if (best >= cap) {
                break;
            }
        }
        return best;
    }

    /** The purifier's rate: unit-per-process-run * 20 ticks a second / the process's ticks, as WaterService.canSupply counts it. */
    private static double purifierRate() {
        return (double) CONFIG.water().unitPerProcessRun() * EnergyTable.TICKS_PER_SECOND / HEAVY_TICKS;
    }

    private static double pumpDemandW() {
        return CONFIG.water().pumpPowerW().get(LEVEL - 1);
    }

    private static ProcessEngine engine() {
        return new ProcessEngine(CONFIG.processes(), ProcessEngine.FUEL, ref -> 64);
    }

    /** A roll that always lands: the random source returns 0, below every chance. */
    private static Random noChance() {
        return new Random() {
            @Override
            public double nextDouble() {
                return 0.0;
            }
        };
    }

    private static final class Slot implements Stack {
        private ItemRef ref;
        private int count;

        @Override
        public ItemRef ref() {
            return count > 0 ? ref : null;
        }

        @Override
        public int count() {
            return count;
        }

        @Override
        public void set(ItemRef ref, int count) {
            this.ref = count > 0 ? ref : null;
            this.count = Math.max(0, count);
        }
    }

    private static List<Stack> slots(int size) {
        List<Stack> slots = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            slots.add(new Slot());
        }
        return slots;
    }

    private static int count(List<Stack> slots, ItemRef ref) {
        int total = 0;
        for (Stack slot : slots) {
            if (ref.equals(slot.ref())) {
                total += slot.count();
            }
        }
        return total;
    }

    private static WaterOrientation.PipeShape shape(Pos pipe) {
        return WaterOrientation.pipe(GRID.connectedFaces(pipe)).shape();
    }

    private static List<Pos> bends(List<Pos> pipes) {
        return pipes.stream().filter(pipe -> shape(pipe) == WaterOrientation.PipeShape.BEND).toList();
    }

    private static long verticalBends(List<Pos> pipes) {
        return bends(pipes).stream()
                .filter(pipe -> GRID.connectedFaces(pipe).contains(Face.UP) || GRID.connectedFaces(pipe).contains(Face.DOWN))
                .count();
    }

    private static String genId(int i) {
        return String.format("gen-%02d", i);
    }

    private static Pos genPos(int i) {
        return new Pos(10 + i % 8, 70, i / 8);
    }

    private static List<GeneratorOutput> fullOutputs() {
        List<GeneratorOutput> outputs = new ArrayList<>(GENERATORS);
        for (int i = 0; i < GENERATORS; i++) {
            outputs.add(new GeneratorOutput(genId(i), genPos(i), GeneratorKind.CHARCOAL.ratedW()));
        }
        return outputs;
    }

    private static EnergySnapshot snapshot(List<GeneratorOutput> outputs, Pos pumpPos, double demandW,
                                           boolean pylonLink, GridRegistry registry) {
        List<Pylon> pylons = List.of(
                new Pylon(PYLON_1, PYLON_1_POS, PylonType.LARGE),
                new Pylon(PYLON_2, PYLON_2_POS, PylonType.LARGE));
        List<Wire> wires = new ArrayList<>();
        for (int i = 0; i < GENERATORS; i++) {
            wires.add(new Wire("wire-" + genId(i), genId(i), PYLON_1));
        }
        if (pylonLink) {
            wires.add(new Wire("link", PYLON_2, PYLON_1));
        }
        return new EnergySnapshot(pylons, outputs, wires, List.of(), List.of(),
                List.of(new Consumer(PUMP_ID, pumpPos, demandW)), registry);
    }

    private static ConsumerResult pumpResult(EnergyResult energy) {
        return energy.consumers().stream().filter(c -> c.id().equals(PUMP_ID)).findFirst().orElseThrow();
    }

    /**
     * One run of the scenario, tick by tick, in the order the Paper service runs it: the generators burn, the energy network is
     * solved, the pump runs if it has a source and power, the water is solved every solve-ticks, and the purifier takes a tick.
     */
    private static final class Simulation {

        final double[] satisfaction = new double[RUN_TICKS + 1];
        final boolean[] pumpRuns = new boolean[RUN_TICKS + 1];
        final boolean[] watered = new boolean[RUN_TICKS + 1];
        final double[] supplyW = new double[RUN_TICKS + 1];
        /** Each solve as {tick, the pump's output, the purifier's delivery}. */
        final List<double[]> solves = new ArrayList<>();
        final List<Integer> completions = new ArrayList<>();
        GridResult firstGrid;
        int cells;

        static Simulation run() {
            Simulation sim = new Simulation();
            WaterSettings water = CONFIG.water();
            double rate = purifierRate();
            double demandW = pumpDemandW();
            boolean hasSource = networkSource(GRID, POOL, water.sourceMinBlocks() + 1) >= water.sourceMinBlocks();
            ProcessEngine engine = engine();
            MachineState purifier = new MachineState("purifier", slots(2), slots(2), List.of());
            Random rolls = noChance();
            GeneratorState[] fuel = new GeneratorState[GENERATORS];
            for (int i = 0; i < GENERATORS; i++) {
                fuel[i] = new GeneratorState(0.0, CHARGE_ITEMS);
            }
            GridRegistry registry = NO_GRIDS;
            boolean isWatered = false;

            for (int t = 1; t <= RUN_TICKS; t++) {
                GeneratorEnvironment env = new GeneratorEnvironment(t, 0L, 0.0, null, false);
                List<GeneratorOutput> outputs = new ArrayList<>(GENERATORS);
                for (int i = 0; i < GENERATORS; i++) {
                    Generator generator = new Generator(genId(i), genPos(i), GeneratorKind.CHARCOAL, fuel[i]);
                    GeneratorStep step = generator.tick(env, DT);
                    fuel[i] = step.state();
                    outputs.add(GeneratorOutput.of(generator, step));
                }

                EnergyResult energy = EnergyNetwork.solve(
                        snapshot(outputs, PUMP, hasSource ? demandW : 0.0, true, registry), DT);
                registry = energy.gridIds();
                GridResult gridResult = energy.grids().get(0);
                if (t == 1) {
                    sim.firstGrid = gridResult;
                }
                double powered = pumpResult(energy).satisfaction();
                boolean running = hasSource && powered > 0.0;
                sim.satisfaction[t] = powered;
                sim.pumpRuns[t] = running;
                sim.supplyW[t] = gridResult.grid().supplyW();

                if (t % water.solveTicks() == 0) {
                    Map<Pos, WaterFlow.PumpSupply> pumps = Map.of(PUMP, new WaterFlow.PumpSupply(LEVEL, running));
                    Map<Pos, Double> asks = engine.select(purifier, true).isPresent()
                            ? Map.of(PURIFIER, rate)
                            : Map.of();
                    WaterFlow.Result result = WaterFlow.solve(GRID, FLOW, pumps, asks);
                    double delivered = result.consumerDelivered().getOrDefault(PURIFIER, 0.0);
                    isWatered = delivered + EPS >= rate;
                    sim.solves.add(new double[] {t, result.pumpOutput().getOrDefault(PUMP, 0.0), delivered});
                }
                sim.watered[t] = isWatered;

                if (engine.tick(purifier, rolls, isWatered) == ProcessEngine.Step.COMPLETED) {
                    sim.completions.add(t);
                }
            }
            sim.cells = count(purifier.outputs(), DEUTERIUM_CELL);
            return sim;
        }
    }
}
