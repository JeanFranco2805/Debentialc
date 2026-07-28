package org.debentialc.trainings.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.debentialc.trainings.managers.TrainingInputManager;
import org.debentialc.trainings.managers.TrainingNpcInputManager;

public class TrainingInputListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (TrainingInputManager.isInputting(player)) {
            event.setCancelled(true);
            processChat(player, event.getMessage().trim());
            return;
        }
        if (TrainingNpcInputManager.isInputting(player)) {
            event.setCancelled(true);
            processNpcChat(player, event.getMessage().trim());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (TrainingInputManager.isInputting(player)) {
            event.setCancelled(true);
            processChat(player, event.getMessage().trim());
            return;
        }
        if (TrainingNpcInputManager.isInputting(player)) {
            event.setCancelled(true);
            processNpcChat(player, event.getMessage().trim());
        }
    }

    private void processChat(Player player, String message) {
        if (message.equalsIgnoreCase("cancelar")) {
            TrainingInputManager.cancelInput(player);
            return;
        }
        TrainingInputManager.processInput(player, message);
    }

    private void processNpcChat(Player player, String message) {
        if (message.equalsIgnoreCase("cancelar")) {
            TrainingNpcInputManager.cancelInput(player);
            return;
        }
        TrainingNpcInputManager.processInput(player, message);
    }
}
