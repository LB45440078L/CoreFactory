package io.moretntplus.api;

/**
 * Static access to the running {@link MoreTntPlusApi}, for addons that cannot receive it through the
 * plugin manager. Returns {@code null} until MoreTNTPlus has enabled.
 */
public final class MoreTntPlusProvider {

    private static volatile MoreTntPlusApi instance;

    private MoreTntPlusProvider() {
    }

    public static MoreTntPlusApi get() {
        return instance;
    }

    /** Called by the plugin during enable and disable. Not part of the addon contract. */
    public static void set(MoreTntPlusApi api) {
        instance = api;
    }
}
