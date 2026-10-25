package io.corefactory.paper;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The permission nodes of the plugin, registered when it enables. The {@code corefactory.*} node holds every other node. The defaults are
 * the ones the plugin has always had: operators for the administration, and everyone for the player's own features.
 */
final class CorePermissions {

    private static final String ALL = "corefactory.*";

    private CorePermissions() {
    }

    /**
     * Registers every node that is not registered yet, then the {@code corefactory.*} node that holds them.
     *
     * @param manager the server's plugin manager
     */
    static void register(PluginManager manager) {
        Map<String, Boolean> children = new LinkedHashMap<>();
        add(manager, "corefactory.give", "Give TNT and components, and take items from the catalogue (its left click).", PermissionDefault.OP, children);
        add(manager, "corefactory.configure", "Aim, target and unlink placed TNT.", PermissionDefault.OP, children);
        add(manager, "corefactory.reload", "Reload configuration and recipes.", PermissionDefault.OP, children);
        add(manager, "corefactory.guide", "Get the tutorial book and browse the catalogue.", PermissionDefault.TRUE, children);
        add(manager, "corefactory.achievements", "Open your own achievements.", PermissionDefault.TRUE, children);
        add(manager, "corefactory.recipes", "Open the recipe of any TNT or component.", PermissionDefault.TRUE, children);
        add(manager, "corefactory.geology", "List the deposits near you and what each has left (/cf geology deposits).",
                PermissionDefault.OP, children);
        add(manager, "corefactory.research.admin", "Finish research for players (/cf research).", PermissionDefault.OP, children);
        add(manager, "corefactory.lock", "Set, change and remove a PIN lock on your own nuclear TNT.", PermissionDefault.TRUE, children);
        add(manager, "corefactory.lock.admin",
                "Remove a PIN lock from any nuclear TNT without its PIN, for recovery. Each removal is logged.", PermissionDefault.OP,
                children);
        add(manager, "corefactory.pylons",
                "Show the grid outlines near you (/cf pylons show), and list the charts and the grid you stand in (/cf grid list, info).",
                PermissionDefault.TRUE, children);
        add(manager, "corefactory.grid", "Make, remove and change the grid charts (/cf grid create, remove, mode).",
                PermissionDefault.OP, children);
        add(manager, "corefactory.friends",
                "Keep your friend list (/cf friends add, remove, list). A turret never fires at a friend or its owner, except a player "
                        + "set to target or a teammate in team mode target.", PermissionDefault.TRUE, children);
        add(manager, "corefactory.turret", "Change the targeting of the turrets you placed, and their friendly fire (/cf turret friendlyfire).",
                PermissionDefault.TRUE, children);

        if (manager.getPermission(ALL) == null) {
            manager.addPermission(new Permission(ALL, "Every CoreFactory permission.", PermissionDefault.OP, children));
        }
    }

    private static void add(PluginManager manager, String name, String description, PermissionDefault defaults,
                            Map<String, Boolean> children) {
        children.put(name, true);
        if (manager.getPermission(name) == null) {
            manager.addPermission(new Permission(name, description, defaults));
        }
    }
}
