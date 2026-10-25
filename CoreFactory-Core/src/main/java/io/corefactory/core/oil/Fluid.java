package io.corefactory.core.oil;

import java.util.Optional;

/**
 * The fluids that flow in oil tubes. A fluid is not an item: it is a volume in litres inside a network, drawn by the machines that burn it
 * or refine it. The physical constants are the ones a tank of fuel is measured by: density in kilograms per litre, and the lower heating
 * value in joules per kilogram. Pure.
 */
public enum Fluid {

    /** Crude petroleum: a density of 0.80 kg/L and a lower heating value of 43 MJ/kg. */
    PETROLEUM("petroleum", 0.80, 43.0e6),

    /** Diesel: a density of 0.84 kg/L and a lower heating value of 43 MJ/kg. */
    DIESEL("diesel", 0.84, 43.0e6);

    private final String id;
    private final double densityKgPerLitre;
    private final double lowerHeatingValueJoulesPerKg;

    Fluid(String id, double densityKgPerLitre, double lowerHeatingValueJoulesPerKg) {
        this.id = id;
        this.densityKgPerLitre = densityKgPerLitre;
        this.lowerHeatingValueJoulesPerKg = lowerHeatingValueJoulesPerKg;
    }

    /** The id of the fluid in the config and the saved files, such as {@code petroleum}. */
    public String id() {
        return id;
    }

    /** The fluid with an id, or empty for an id that names none. */
    public static Optional<Fluid> ofId(String id) {
        for (Fluid fluid : values()) {
            if (fluid.id.equals(id)) {
                return Optional.of(fluid);
            }
        }
        return Optional.empty();
    }

    /** The energy of one litre burnt completely, in joules: density times the lower heating value. */
    public double joulesPerLitre() {
        return densityKgPerLitre * lowerHeatingValueJoulesPerKg;
    }
}
