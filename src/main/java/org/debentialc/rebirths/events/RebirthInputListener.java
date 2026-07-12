package org.debentialc.rebirths.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.debentialc.rebirths.managers.RebirthBlockInputManager;
import org.debentialc.rebirths.managers.RebirthInputManager;

public class RebirthInputListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (RebirthInputManager.isInputting(player) || RebirthBlockInputManager.isInputting(player)) {
            event.setCancelled(true);
            processChat(player, event.getMessage().trim());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();

        if (RebirthInputManager.isInputting(player) || RebirthBlockInputManager.isInputting(player)) {
            event.setCancelled(true);
            processChat(player, event.getMessage().trim());
        }
    }

    private void processChat(Player player, String message) {
        if (message.equalsIgnoreCase("cancelar")) {
            if (RebirthInputManager.isInputting(player)) {
                RebirthInputManager.cancelInput(player);
            }
            if (RebirthBlockInputManager.isInputting(player)) {
                RebirthBlockInputManager.cancelInput(player);
            }
            return;
        }

        if (RebirthInputManager.isInputting(player)) {
            RebirthInputManager.processInput(player, message);
            return;
        }

        if (RebirthBlockInputManager.isInputting(player)) {
            RebirthBlockInputManager.processInput(player, message);
        }
    }
}
