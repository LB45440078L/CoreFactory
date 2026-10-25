package io.corefactory.core.oil;

import io.corefactory.core.device.Footprint;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.oil.OilJoinPlan.Device;
import io.corefactory.core.oil.OilJoinPlan.Tube;
import io.corefactory.core.water.WaterNode;
import io.corefactory.core.water.PumpLevels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The devices and tubes the oil join tests build, with the heights and roles the plugin gives them. */
final class OilPlanFixture {

    /** The capacity of one tube's tank, in litres: the default of config.yml. */
    static final double CAPACITY = 100.0;

    private OilPlanFixture() {
    }

    /** A pipe tube at a block, its tank holding the given fluid, or empty for null. */
    static Tube tube(Pos pos, Fluid fluid) {
        return new Tube(pos, WaterNode.pipe(pos), fluid);
    }

    /** The crude oil refinery at an anchor: one block, takes petroleum and gives diesel, keeping its networks apart. */
    static Device refinery(Pos anchor) {
        return new Device(anchor, Footprint.heightOf("crude_oil_refinery"), Fluid.DIESEL, Fluid.PETROLEUM, true);
    }

    /** A petroleum drill at an anchor, level 4 (two blocks tall): it gives petroleum into every network it joins. */
    static Device petroleumDrill(Pos anchor) {
        return new Device(anchor, Footprint.heightOf("petroleum_drill_4"), Fluid.PETROLEUM, null, false);
    }

    /** An ore drill at an anchor, level 4: it joins the networks it touches and takes no oil. */
    static Device oreDrill(Pos anchor) {
        return new Device(anchor, Footprint.heightOf("ore_drill_4"), null, null, false);
    }

    /** A petroleum generator at an anchor: it takes petroleum from the networks it joins. */
    static Device petroleumGenerator(Pos anchor) {
        return new Device(anchor, Footprint.heightOf("petroleum_generator"), null, Fluid.PETROLEUM, false);
    }

    /** A diesel generator at an anchor: it takes diesel from the networks it joins. */
    static Device dieselGenerator(Pos anchor) {
        return new Device(anchor, Footprint.heightOf("diesel_generator"), null, Fluid.DIESEL, false);
    }

    /** A water pump at level 2, which burns diesel from the oil network: it takes diesel. */
    static Device dieselPump(Pos anchor) {
        return new Device(anchor, PumpLevels.height(2), null, Fluid.DIESEL, false);
    }

    /**
     * The network of a tube, built from the tanks of its plan network: one tank for each tube, holding the fluid the tube was placed with.
     *
     * @param plan   the plan
     * @param tubes  the tubes the network was built from, by position, holding their fluids
     * @param pos    a tube of the network
     * @return the network over its tanks
     */
    static OilNetwork networkOf(OilJoinPlan plan, Map<Pos, Tube> tubes, Pos pos) {
        List<FluidTank> tanks = new ArrayList<>();
        for (Pos member : plan.networks().get(plan.networkOf(pos)).tubes()) {
            FluidTank tank = new FluidTank(CAPACITY);
            Fluid fluid = tubes.get(member).fluid();
            if (fluid != null) {
                tank.accept(fluid, 10.0);
            }
            tanks.add(tank);
        }
        return new OilNetwork(tanks);
    }
}
