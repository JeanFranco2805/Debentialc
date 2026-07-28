package org.debentialc.boosters.integration;

import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.debentialc.Main;
import org.debentialc.boosters.core.BoosterModule;
import org.debentialc.boosters.managers.GlobalBoosterManager;
import org.debentialc.boosters.managers.PersonalBoosterManager;
import org.debentialc.service.CC;
import org.debentialc.service.ServerUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TPConsumeListener implements Listener {

    private static final Map<UUID, Integer> lastTPValues = new HashMap<>();
    private static final long CHECK_INTERVAL = 10L;

    public static void startTPMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Player[] players = ServerUtil.getOnlinePlayers();
                for (Player player : players) {
                    checkTPConsumption(player);
                }
            }
        }.runTaskTimer(BoosterModule.getPlugin(), CHECK_INTERVAL, CHECK_INTERVAL);
    }

    private static void checkTPConsumption(Player player) {
        if (player == null) return;

        try {
            IDBCPlayer dbcPlayer = NpcAPI.Instance().getPlayer(player.getName()).getDBCPlayer();
            if (dbcPlayer == null) return;

            int currentTP = dbcPlayer.getTP();
            UUID playerId = player.getUniqueId();

            if (lastTPValues.containsKey(playerId)) {
                int previousTP = lastTPValues.get(playerId);
                int tpChange = currentTP - previousTP;

                if (tpChange < 0) {
                    handleTPConsumption(player, dbcPlayer, Math.abs(tpChange));
                }
            }

            lastTPValues.put(playerId, currentTP);

        } catch (Exception e) {
        }
    }

    private static void handleTPConsumption(Player player, IDBCPlayer dbcPlayer, int tpsConsumed) {
        double globalMultiplier = GlobalBoosterManager.getCurrentMultiplier();
        double personalMultiplier = PersonalBoosterManager.getActiveMultiplier(player.getUniqueId());
        double combinedMultiplier = globalMultiplier * personalMultiplier;

        if (combinedMultiplier <= 1.0) {
            return;
        }

        int bonusTPs = (int) Math.round(tpsConsumed * (combinedMultiplier - 1.0));

        if (bonusTPs > 0) {
            int currentTP = dbcPlayer.getTP();
            dbcPlayer.setTP(currentTP + bonusTPs);

            sendConsumeBoosterNotification(player, tpsConsumed, bonusTPs, globalMultiplier, personalMultiplier, combinedMultiplier);
        }
    }

    private static void sendConsumeBoosterNotification(Player player, int tpsConsumed, int bonusTPs,
                                                       double globalMult, double personalMult, double combinedMult) {

        player.sendMessage(CC.translate("&a+" + bonusTPs + " TPs"));
        if (globalMult > 1.0) {
            String globalPercent = String.format("%.0f%%", (globalMult - 1.0) * 100);
            player.sendMessage(CC.translate("  &6⚡ Booster Global: &a+" + globalPercent));
        }

        if (personalMult > 1.0) {
            String personalPercent = String.format("%.0f%%", (personalMult - 1.0) * 100);
            player.sendMessage(CC.translate("  &b⚡ Booster Personal: &a+" + personalPercent));
        }
    }

    public static void removePlayer(Player player) {
        if (player != null) {
            lastTPValues.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        removePlayer(event.getPlayer());
    }
}