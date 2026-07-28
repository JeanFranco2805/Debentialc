package org.debentialc.swords;

import org.bukkit.Bukkit;
import org.debentialc.Main;
import org.debentialc.swords.listeners.SwordAbilityListener;

public class SwordsModule {

    public static void initialize() {
        Bukkit.getPluginManager().registerEvents(new SwordAbilityListener(), Main.instance);
        Main.instance.getLogger().info("[Swords] Modulo de espadas inicializado.");
    }
}
