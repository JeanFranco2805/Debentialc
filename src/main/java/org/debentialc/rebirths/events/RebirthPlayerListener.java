package org.debentialc.rebirths.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.debentialc.rebirths.managers.RebirthManager;

public class RebirthPlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        RebirthManager.getInstance().loadPlayerData(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        RebirthManager.getInstance().unloadPlayerData(event.getPlayer());
    }
}
