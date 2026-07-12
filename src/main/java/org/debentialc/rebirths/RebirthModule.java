package org.debentialc.rebirths;

import org.bukkit.plugin.java.JavaPlugin;
import org.debentialc.rebirths.listeners.RebirthItemDragListener;
import org.debentialc.rebirths.managers.RebirthBlockManager;
import org.debentialc.rebirths.managers.RebirthManager;

public class RebirthModule {

    private static JavaPlugin plugin;

    public static void initialize(JavaPlugin instance) {
        plugin = instance;
        RebirthBlockManager.getInstance();
        RebirthManager.getInstance();

        plugin.getServer().getPluginManager().registerEvents(new RebirthItemDragListener(), plugin);

        plugin.getLogger().info("[Rebirths] Sistema de rebirths inicializado.");
    }

    public static JavaPlugin getPlugin() {
        return plugin;
    }
}
