package org.debentialc.cinematics;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.debentialc.cinematics.commands.CinematicCommand;
import org.debentialc.cinematics.listener.CinematicListener;
import org.debentialc.cinematics.manager.CinematicManager;
import org.debentialc.service.ServerUtil;

public class CinematicModule {

    private static JavaPlugin plugin;
    private static CinematicManager manager;
    private static CinematicListener listener;

    public static void initialize(JavaPlugin instance) {
        plugin = instance;
        manager = new CinematicManager(plugin);
        listener = new CinematicListener();

        manager.loadAll();
        new CinematicCommand(manager);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        plugin.getLogger().info("Sistema de Cinematics inicializado correctamente");
    }

    public static void shutdown() {
        // Stop all active cinematics
        for (Player player : ServerUtil.getOnlinePlayers()) {
            org.debentialc.cinematics.manager.CinematicSessionManager.stop(player);
        }
        plugin.getLogger().info("Sistema de Cinematics detenido");
    }

    public static CinematicManager getManager() {
        return manager;
    }

    public static JavaPlugin getPlugin() {
        return plugin;
    }
}
