package org.debentialc.scoreboard;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.debentialc.scoreboard.commands.ScoreboardCommand;
import org.debentialc.scoreboard.config.ScoreboardConfig;
import org.debentialc.scoreboard.listener.ScoreboardListener;
import org.debentialc.scoreboard.manager.ScoreboardManager;
import org.debentialc.service.ServerUtil;

public class ScoreboardModule {

    private static JavaPlugin plugin;
    private static ScoreboardManager manager;
    private static ScoreboardListener listener;
    private static ScoreboardCommand command;

    public static void initialize(JavaPlugin instance) {
        plugin = instance;
        manager = new ScoreboardManager();
        listener = new ScoreboardListener(manager);
        command = new ScoreboardCommand();

        ScoreboardConfig.load(plugin);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        if (ScoreboardConfig.isEnabled()) {
            manager.start();
            for (Player player : ServerUtil.getOnlinePlayers()) {
                manager.createScoreboard(player);
            }
        }

        plugin.getLogger().info("Sistema de Scoreboard inicializado correctamente");
    }

    public static void shutdown() {
        if (manager != null) {
            manager.stop();
        }
        plugin.getLogger().info("Sistema de Scoreboard detenido");
    }

    public static void reload() {
        if (manager != null) {
            manager.reload();
        }
    }

    public static ScoreboardManager getManager() {
        return manager;
    }

    public static JavaPlugin getPlugin() {
        return plugin;
    }
}
