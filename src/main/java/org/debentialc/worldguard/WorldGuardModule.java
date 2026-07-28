package org.debentialc.worldguard;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.debentialc.Main;

public class WorldGuardModule {

    public static void initialize() {
        Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (worldGuard == null || !worldGuard.isEnabled()) {
            Main.instance.getLogger().info("[WorldGuard] No detectado. Flags personalizadas no se cargarán.");
            return;
        }

        WorldGuardFlagManager.registerFlags();
        Bukkit.getPluginManager().registerEvents(new WorldGuardListener(), Main.instance);
        Main.instance.getLogger().info("[WorldGuard] Listener de entry-lvl y entry-quest activado.");
    }
}
