package io.corefactory.paper.pack;

import io.corefactory.paper.pack.PackInstaller.Action;
import io.corefactory.paper.pack.PackInstaller.Result;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Connects the pack installer to the plugin. The pack in the jar is copied to {@code plugins/CoreFactory/pack/pack.zip}
 * only when it is newer than the one already there, so an owner who swaps in a new jar without deleting the plugin
 * folder gets the new textures, and an older jar never overwrites a newer pack.
 */
public final class BundledPack {

    /** Where the build puts the zip inside the jar. */
    static final String RESOURCE = "resource-pack/CoreFactory-ResourcePack.zip";

    /** The folder under the plugin's data folder that RPMS reads. */
    static final String FOLDER = "pack";

    private BundledPack() {
    }

    /**
     * Installs the bundled pack if it is newer than the installed one. Called on load, before any plugin is enabled.
     * Failures are logged, never thrown, so a broken pack cannot stop the plugin from loading.
     */
    public static Result install(Plugin plugin) {
        Result result = PackInstaller.install(folder(plugin), bundledBytes(plugin));
        report(plugin.getLogger(), result);
        return result;
    }

    /** What {@link #install} would do now, without changing anything. For {@code /cf pack}. */
    public static Result inspect(Plugin plugin) {
        return PackInstaller.inspect(folder(plugin), bundledBytes(plugin));
    }

    private static Path folder(Plugin plugin) {
        return plugin.getDataFolder().toPath().resolve(FOLDER);
    }

    /** The pack bytes in the jar, or {@code null} if the jar has none. */
    private static byte[] bundledBytes(Plugin plugin) {
        try (InputStream in = plugin.getResource(RESOURCE)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not read the bundled resource pack from the jar.", e);
            return null;
        }
    }

    private static void report(Logger log, Result result) {
        String message = result.detail() + " (" + result.file() + ")";
        if (result.action() == Action.FAILED || result.action() == Action.NO_BUNDLE) {
            log.warning(message);
        } else {
            log.info(message);
        }
    }
}
