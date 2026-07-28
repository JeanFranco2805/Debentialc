package org.debentialc.trainings;

import org.bukkit.plugin.java.JavaPlugin;
import org.debentialc.trainings.events.TrainingInputListener;
import org.debentialc.trainings.managers.TrainingManager;

public class TrainingModule {

    private static JavaPlugin plugin;

    public static void initialize(JavaPlugin instance) {
        plugin = instance;
        TrainingManager.getInstance();

        plugin.getServer().getPluginManager().registerEvents(new TrainingInputListener(), plugin);

        plugin.getLogger().info("[Trainings] Sistema de trainings inicializado.");
    }

    public static JavaPlugin getPlugin() {
        return plugin;
    }
}
