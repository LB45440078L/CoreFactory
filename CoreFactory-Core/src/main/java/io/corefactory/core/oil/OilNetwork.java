package io.corefactory.core.oil;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The fluid of one oil network: the tanks of its tubes, which together hold one fluid kind at a time. A supply is split over the tanks in
 * proportion to their free space, and a draw is taken from them in proportion to what each holds, so the tubes fill and empty evenly. A
 * fluid other than the one the network holds is refused, so two kinds never share a network. Pure.
 */
public final class OilNetwork {

    private final List<FluidTank> tanks;

    /**
     * @param tanks the tanks of the network's tubes; they may hold one fluid kind between them, or none
     * @throws NullPointerException     when the list or a tank is null
     * @throws IllegalArgumentException when the tanks hold two fluid kinds
     */
    public OilNetwork(List<FluidTank> tanks) {
        this.tanks = List.copyOf(Objects.requireNonNull(tanks, "tanks"));
        if (!FluidRules.allowed(kinds())) {
            throw new IllegalArgumentException("a network holds one fluid kind, got " + kinds());
        }
    }

    /** The fluids the network holds: none for an empty network, otherwise the one kind it carries. */
    public Set<Fluid> kinds() {
        EnumSet<Fluid> kinds = EnumSet.noneOf(Fluid.class);
        for (FluidTank tank : tanks) {
            tank.fluid().ifPresent(kinds::add);
        }
        return Collections.unmodifiableSet(kinds);
    }

    /**
     * Gives litres of a fluid to the network. The litres are split over the tanks in proportion to their free space, so a tank with more room
     * takes more; when the network has less room than the litres, every tank is filled and the rest is refused. A fluid other than the one the
     * network holds takes nothing.
     *
     * @param fluid  the fluid offered
     * @param litres the litres offered, finite and 0 or more
     * @return the litres taken in
     * @throws IllegalArgumentException when the litres are negative or not finite
     */
    public double supply(Fluid fluid, double litres) {
        Objects.requireNonNull(fluid, "fluid");
        requireLitres(litres);
        Set<Fluid> held = kinds();
        if (!held.isEmpty() && !held.contains(fluid)) {
            return 0.0;
        }
        double room = 0.0;
        for (FluidTank tank : tanks) {
            room += tank.space();
        }
        if (room <= 0.0) {
            return 0.0;
        }
        double taken = 0.0;
        for (FluidTank tank : tanks) {
            double space = tank.space();
            double share = litres >= room ? space : litres * space / room;
            taken += tank.accept(fluid, share);
        }
        return taken;
    }

    /**
     * Takes litres of a fluid out of the network. The litres are drawn from the tanks that hold the fluid in proportion to what each holds, so
     * a fuller tank gives more; when the network holds less than the litres, every such tank is emptied. A fluid the network does not hold
     * gives nothing.
     *
     * @param fluid  the fluid asked for
     * @param litres the litres asked for, finite and 0 or more
     * @return the litres given out
     * @throws IllegalArgumentException when the litres are negative or not finite
     */
    public double draw(Fluid fluid, double litres) {
        Objects.requireNonNull(fluid, "fluid");
        requireLitres(litres);
        double held = 0.0;
        for (FluidTank tank : tanks) {
            if (holds(tank, fluid)) {
                held += tank.litres();
            }
        }
        if (held <= 0.0) {
            return 0.0;
        }
        double given = 0.0;
        for (FluidTank tank : tanks) {
            if (!holds(tank, fluid)) {
                continue;
            }
            double share = litres >= held ? tank.litres() : litres * tank.litres() / held;
            given += tank.draw(fluid, share);
        }
        return given;
    }

    /** The litres the network holds, of whatever fluid. */
    public double litres() {
        double total = 0.0;
        for (FluidTank tank : tanks) {
            total += tank.litres();
        }
        return total;
    }

    private static boolean holds(FluidTank tank, Fluid fluid) {
        return tank.fluid().filter(held -> held.equals(fluid)).isPresent();
    }

    private static void requireLitres(double litres) {
        if (!Double.isFinite(litres) || litres < 0) {
            throw new IllegalArgumentException("litres must be finite and 0 or more, got " + litres);
        }
    }
}
