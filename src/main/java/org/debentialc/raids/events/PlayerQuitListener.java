package org.debentialc.raids.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.debentialc.raids.managers.RaidSessionManager;
import org.debentialc.raids.managers.PartyManager;
import org.debentialc.raids.menus.RaidCategoryChatInputManager;
import org.debentialc.raids.menus.RaidChatInputManager;
import org.debentialc.raids.models.RaidSession;
import org.debentialc.raids.models.Party;
import org.debentialc.service.CC;

/**
 * PlayerQuitListener - Escucha cuando un jugador se desconecta
 * Maneja lo que ocurre cuando un jugador abandona el servidor durante una raid
 */
public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        RaidSession session = RaidSessionManager.getPlayerSession(player.getUniqueId());
        if (session != null) {
            handlePlayerLeftRaid(player, session);
        }

        Party party = PartyManager.getPlayerParty(player.getUniqueId());
        if (party != null) {
            handlePlayerLeftParty(player, party);
        }

        if (RaidChatInputManager.isInputting(player)) {
            RaidChatInputManager.cancelInput(player);
        }
        if (RaidCategoryChatInputManager.isInputting(player)) {
            RaidCategoryChatInputManager.cancelInput(player);
        }
    }

    private void handlePlayerLeftRaid(Player player, RaidSession session) {
        RaidSessionManager.playerLeft(player.getUniqueId());

        notifyOtherPlayersAboutQuit(player, session);

        if (session.isRaidFailed()) {
            RaidSessionManager.failRaid(session);
        }
    }

    private void notifyOtherPlayersAboutQuit(Player quitPlayer, RaidSession session) {
        for (java.util.UUID playerId : session.getActivePlayers()) {
            Player player = org.bukkit.Bukkit.getPlayer(playerId);
            if (player != null && !player.equals(quitPlayer)) {
                player.sendMessage("");
                player.sendMessage(CC.translate("&c⚠ " + quitPlayer.getName() + " se desconectó de la raid"));
                player.sendMessage(CC.translate("&7Jugadores activos: " + session.getActivePlayers().size()));
                player.sendMessage("");
            }
        }
    }

    private void handlePlayerLeftParty(Player player, Party party) {
        PartyManager.leaveParty(player.getUniqueId());

        notifyOtherPartyMembers(player, party);
    }

    private void notifyOtherPartyMembers(Player quitPlayer, Party party) {
        for (java.util.UUID memberId : party.getActivePlayers()) {
            Player player = org.bukkit.Bukkit.getPlayer(memberId);
            if (player != null && !player.equals(quitPlayer)) {
                player.sendMessage("");
                player.sendMessage(CC.translate("&c⚠ " + quitPlayer.getName() + " se desconectó"));
                player.sendMessage(CC.translate("&7Miembros de party: " + party.getMemberCount() + "/" + party.getMaxSize()));
                player.sendMessage("");
            }
        }
    }
}