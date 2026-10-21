package io.corefactory.api;

/**
 * Static access to the running {@link CoreFactoryApi}, for addons that cannot receive it through the
 * plugin manager. Returns {@code null} until CoreFactory has enabled.
 */
public final class CoreFactoryProvider {

    private static volatile CoreFactoryApi instance;

    private CoreFactoryProvider() {
    }

    public static CoreFactoryApi get() {
        return instance;
    }

    /** Called by the plugin during enable and disable. Not part of the addon contract. */
    public static void set(CoreFactoryApi api) {
        instance = api;
    }
}
