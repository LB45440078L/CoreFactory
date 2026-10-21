package io.corefactory.core.energy;

/**
 * What a generator sees in one tick, gathered by the caller from the world.
 *
 * @param worldTick    the world's full time, which drives the wind curve and the sun
 * @param seed         the seed of the world's wind
 * @param irradianceWPerM2 the sun's irradiance on a panel, in W/m2; see {@link Sky#irradianceWPerM2(long)}
 * @param openAir      the open-air input of a wind generator
 * @param underwater   true when a nuclear generator stands in water, see {@link Generator#isUnderwater(boolean, int)}
 */
public record GeneratorEnvironment(
        long worldTick,
        long seed,
        double irradianceWPerM2,
        OpenAir openAir,
        boolean underwater) {

    /**
     * The environment at a world tick, with the irradiance taken from the world's time of day.
     *
     * @param worldTick the world's full time
     * @param seed      the seed of the world's wind
     * @param openAir   the open-air input of a wind generator
     * @param underwater true when a nuclear generator stands in water
     */
    public static GeneratorEnvironment at(long worldTick, long seed, OpenAir openAir, boolean underwater) {
        return new GeneratorEnvironment(worldTick, seed, Sky.irradianceWPerM2(Sky.dayTicks(worldTick)), openAir, underwater);
    }
}
