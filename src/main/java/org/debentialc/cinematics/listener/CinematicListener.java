package org.debentialc.cinematics.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.debentialc.cinematics.manager.CinematicSessionManager;

public class CinematicListener implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!CinematicSessionManager.isPlaying(event.getPlayer())) return;
        // Cancel any movement not caused by the cinematic itself
        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        CinematicSessionManager.stop(event.getPlayer());
    }
}
