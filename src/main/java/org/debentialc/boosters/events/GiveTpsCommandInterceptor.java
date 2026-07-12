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
import org.debentialc.service.CC;

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
            event.getPlayer().sendMessage(CC.translate("&cUso: /dartps <jugador> <cantidad>"));
            event.setCancelled(true);
            return;
        }

        try {
            String targetName = args[1];
            int baseTPs = Integer.parseInt(args[2]);

            Player target = Main.instance.getServer().getPlayer(targetName);
            if (target == null) {
                event.getPlayer().sendMessage(CC.translate("&cJugador no encontrado: " + targetName));
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            applyBoosterAndGiveTPs(event.getPlayer(), target, baseTPs);

        } catch (NumberFormatException e) {
            event.getPlayer().sendMessage(CC.translate("&cCantidad inválida de TPs"));
            event.setCancelled(true);
        } catch (ArrayIndexOutOfBoundsException e) {
            event.getPlayer().sendMessage(CC.translate("&cUso: /dartps <jugador> <cantidad>"));
            event.setCancelled(true);
        }
    }

    public static void applyBoosterAndGiveTPs(CommandSender sender, Player target, int baseTPs) {
        try {
            IDBCPlayer dbcPlayer = NpcAPI.Instance().getPlayer(target.getName()).getDBCPlayer();
            if (dbcPlayer == null) {
                sender.sendMessage(CC.translate("&cError: El jugador no tiene datos de DBC"));
                return;
            }

            double globalMultiplier = GlobalBoosterManager.getCurrentMultiplier();
            double personalMultiplier = PersonalBoosterManager.getActiveMultiplier(target.getUniqueId());
            double combinedMultiplier = globalMultiplier * personalMultiplier;

            int totalTPs = (int) Math.round(baseTPs * combinedMultiplier);
            int bonusTPs = totalTPs - baseTPs;

            int currentTP = dbcPlayer.getTP();
            dbcPlayer.setTP(currentTP + totalTPs);

            sendSuccessMessage(sender, target, baseTPs, bonusTPs, totalTPs, globalMultiplier, personalMultiplier, combinedMultiplier);

        } catch (Exception e) {
            sender.sendMessage(CC.translate("&cError al dar TPs: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    public static void sendSuccessMessage(CommandSender sender, Player target, int baseTPs, int bonusTPs, int totalTPs,
                                          double globalMult, double personalMult, double combinedMult) {

        if (bonusTPs > 0) {
            sender.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            sender.sendMessage(CC.translate("&a✓ TPs Entregados a &6" + target.getName()));
            sender.sendMessage("");
            sender.sendMessage(CC.translate("  &eTPs Base: &a+" + baseTPs));
            sender.sendMessage(CC.translate("  &eBonus de Booster: &6+" + bonusTPs));
            sender.sendMessage(CC.translate("  &eTotal Entregado: &b+" + totalTPs + " TPs"));
            sender.sendMessage("");

            if (globalMult > 1.0) {
                String globalPercent = String.format("%.0f%%", (globalMult - 1.0) * 100);
                sender.sendMessage(CC.translate("  &6⚡ Booster Global: &a+" + globalPercent));
            }

            if (personalMult > 1.0) {
                String personalPercent = String.format("%.0f%%", (personalMult - 1.0) * 100);
                sender.sendMessage(CC.translate("  &b⚡ Booster Personal: &a+" + personalPercent));
            }

            String totalPercent = String.format("%.0f%%", (combinedMult - 1.0) * 100);
            sender.sendMessage(CC.translate("  &a⚡ Multiplicador Total: &6x" + String.format("%.2f", combinedMult) + " &7(+" + totalPercent + ")"));
            sender.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

            target.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            target.sendMessage(CC.translate("&6&l⚡ TPS RECIBIDOS ⚡"));
            target.sendMessage("");
            target.sendMessage(CC.translate("  &eTPs Base: &a+" + baseTPs));
            target.sendMessage(CC.translate("  &eBonus: &6+" + bonusTPs + " TPs"));
            target.sendMessage(CC.translate("  &eTotal: &b+" + totalTPs + " TPs"));
            target.sendMessage("");

            if (globalMult > 1.0) {
                String globalPercent = String.format("%.0f%%", (globalMult - 1.0) * 100);
                target.sendMessage(CC.translate("  &6⚡ Booster Global: &a+" + globalPercent));
            }

            if (personalMult > 1.0) {
                String personalPercent = String.format("%.0f%%", (personalMult - 1.0) * 100);
                target.sendMessage(CC.translate("  &b⚡ Booster Personal: &a+" + personalPercent));
            }
            target.sendMessage(CC.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

        } else {
            sender.sendMessage(CC.translate("&a✓ Se han dado &6" + String.format(Locale.US, "%,d", totalTPs) + " TPs &aa &6" + target.getName()));
            target.sendMessage(CC.translate("&8[&c+&8] &c" + String.format(Locale.US, "%,d", totalTPs) + " TPS"));
        }
    }
}