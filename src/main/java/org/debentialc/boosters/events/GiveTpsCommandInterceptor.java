package org.debentialc.boosters.events;

import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.debentialc.Main;
import org.debentialc.boosters.managers.GlobalBoosterManager;
import org.debentialc.boosters.managers.PersonalBoosterManager;
import org.debentialc.rebirths.managers.RebirthManager;
import org.debentialc.service.CC;
import org.debentialc.trainings.managers.TrainingManager;

import java.util.Locale;

public class GiveTpsCommandInterceptor implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String messageLower = message.toLowerCase();

        if (!messageLower.startsWith("/dartps")) {
            return;
        }

        String[] args = message.split(" ");

        if (args.length < 3) {
            event.getPlayer().sendMessage(CC.translate("&cUso: /dartps <jugador> <cantidad> [trainingId]"));
            event.setCancelled(true);
            return;
        }

        try {
            String targetName = args[1];
            int baseTPs = Integer.parseInt(args[2]);
            String trainingId = args.length >= 4 ? args[3] : null;

            Player target = Main.instance.getServer().getPlayer(targetName);
            if (target == null) {
                event.getPlayer().sendMessage(CC.translate("&cJugador no encontrado: " + targetName));
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            applyBoosterAndGiveTPs(event.getPlayer(), target, baseTPs, trainingId);

        } catch (NumberFormatException e) {
            event.getPlayer().sendMessage(CC.translate("&cCantidad inválida de TPs"));
            event.setCancelled(true);
        } catch (ArrayIndexOutOfBoundsException e) {
            event.getPlayer().sendMessage(CC.translate("&cUso: /dartps <jugador> <cantidad> [trainingId]"));
            event.setCancelled(true);
        }
    }

    public static void applyBoosterAndGiveTPs(CommandSender sender, Player target, int baseTPs) {
        applyBoosterAndGiveTPs(sender, target, baseTPs, null);
    }

    public static void applyBoosterAndGiveTPs(CommandSender sender, Player target, int baseTPs, String trainingId) {
        try {
            IDBCPlayer dbcPlayer = NpcAPI.Instance().getPlayer(target.getName()).getDBCPlayer();
            if (dbcPlayer == null) {
                sender.sendMessage(CC.translate("&cError: El jugador no tiene datos de DBC"));
                return;
            }

            double rebirthMultiplier = 1.0;
            int rebirthLevel = RebirthManager.getInstance().getPlayerRebirthLevel(target);
            if (rebirthLevel > 0) {
                rebirthMultiplier = RebirthManager.getInstance().getRebirthMultiplier(target);
            }

            double trainingMultiplier = 1.0;
            if (trainingId != null && !trainingId.isEmpty()) {
                trainingMultiplier = TrainingManager.getInstance().getTrainingMultiplier(target, trainingId);
            }

            double globalMultiplier = GlobalBoosterManager.getCurrentMultiplier();
            double personalMultiplier = PersonalBoosterManager.getActiveMultiplier(target.getUniqueId());
            double combinedMultiplier = rebirthMultiplier * trainingMultiplier * globalMultiplier * personalMultiplier;

            int totalTPs = (int) Math.round(baseTPs * combinedMultiplier);
            int rebirthBonus = (int) Math.round(baseTPs * rebirthMultiplier) - baseTPs;
            int trainingBonus = (int) Math.round(baseTPs * rebirthMultiplier * trainingMultiplier) - (int) Math.round(baseTPs * rebirthMultiplier);
            int boosterBonus = totalTPs - (int) Math.round(baseTPs * rebirthMultiplier * trainingMultiplier);

            int currentTP = dbcPlayer.getTP();
            dbcPlayer.setTP(currentTP + totalTPs);

            sendSuccessMessage(sender, target, baseTPs, totalTPs, rebirthBonus, trainingBonus, boosterBonus,
                    rebirthMultiplier, trainingMultiplier, globalMultiplier, personalMultiplier, trainingId);

        } catch (Exception e) {
            sender.sendMessage(CC.translate("&cError al dar TPs: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    public static void sendSuccessMessage(CommandSender sender, Player target, int baseTPs, int totalTPs,
                                          int rebirthBonus, int trainingBonus, int boosterBonus,
                                          double rebirthMult, double trainingMult, double globalMult,
                                          double personalMult, String trainingId) {

        sender.sendMessage(CC.translate("&a✓ Se han dado &6" + formatTP(totalTPs) + " TPs &aa &6" + target.getName()));

        target.sendMessage(CC.translate("&a+" + formatTP(baseTPs)));

        if (rebirthBonus > 0) {
            sender.sendMessage(CC.translate("&7Bonus de rebirth: &a+" + formatTP(rebirthBonus) + " &7(x" + String.format("%.2f", rebirthMult) + ")"));
            target.sendMessage(CC.translate("&a+" + formatTP(rebirthBonus) + " &7(Bonus rebirth x" + String.format("%.2f", rebirthMult) + ")"));
        }
        if (trainingBonus > 0 && trainingId != null) {
            target.sendMessage(CC.translate("&a+" + formatTP(trainingBonus) + " &7(Bonus " + trainingId + " x" + String.format("%.2f", trainingMult) + ")"));
        }
        if (boosterBonus > 0) {
            String boosterDesc = buildBoosterDesc(globalMult, personalMult);
            sender.sendMessage(CC.translate("&7Bonus de booster: &a+" + formatTP(boosterBonus) + " &7(" + boosterDesc + ")"));
            target.sendMessage(CC.translate("&a+" + formatTP(boosterBonus) + " &7(" + boosterDesc + ")"));
        }
    }

    private static String formatTP(int amount) {
        return String.format(Locale.US, "%,d", amount);
    }

    private static String buildBoosterDesc(double globalMult, double personalMult) {
        StringBuilder sb = new StringBuilder();
        if (personalMult > 1.0) {
            sb.append("Booster personal x").append(String.format("%.2f", personalMult));
        }
        if (globalMult > 1.0) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append("Booster global x").append(String.format("%.2f", globalMult));
        }
        return sb.toString();
    }
}