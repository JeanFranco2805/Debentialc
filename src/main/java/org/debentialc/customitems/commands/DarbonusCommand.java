package org.debentialc.customitems.commands;

import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.debentialc.Main;
import org.debentialc.boosters.core.BoosterParser;
import org.debentialc.customitems.tools.permissions.Permissions;
import org.debentialc.service.CC;
import org.debentialc.service.General;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.io.IOException;
import java.util.UUID;

public class DarbonusCommand extends BaseCommand {

    @Command(name = "darbonus", permission = Permissions.COMMAND + "darbonus", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        if (command.length() < 4) {
            command.getSender().sendMessage(CC.translate("&cUso: /darbonus <jugador> <stat> <50% o +300> <tiempo>"));
            command.getSender().sendMessage(CC.translate("&7Stats: STR, DEX, CON, WIL, MND, SPI"));
            command.getSender().sendMessage(CC.translate("&7Ej: /darbonus DelawareX STR 50% 10m"));
            return;
        }

        String targetName = command.getArgs(0);
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            command.getSender().sendMessage(CC.translate("&cJugador no encontrado: &f" + targetName));
            return;
        }

        String stat = command.getArgs(1).toUpperCase();
        String valueStr = command.getArgs(2);
        String timeStr = command.getArgs(3);

        String bonusStat = General.BONUS_STATS.get(stat);
        if (bonusStat == null) {
            command.getSender().sendMessage(CC.translate("&cStat inválido. Usa: STR, DEX, CON, WIL, MND, SPI"));
            return;
        }

        String operation;
        double value;
        String displayValue;
        try {
            if (valueStr.endsWith("%")) {
                operation = "*";
                double percent = Double.parseDouble(valueStr.replace("%", ""));
                value = 1.0 + (percent / 100.0);
                displayValue = "+" + (int) percent + "%";
            } else {
                operation = "+";
                value = Double.parseDouble(valueStr.replace("+", ""));
                displayValue = (value >= 0 ? "+" : "") + (int) value;
            }
        } catch (NumberFormatException e) {
            command.getSender().sendMessage(CC.translate("&cValor inválido. Usa 50% o +300"));
            return;
        }

        long seconds;
        try {
            seconds = BoosterParser.parseTimeToSeconds(timeStr);
        } catch (IllegalArgumentException e) {
            command.getSender().sendMessage(CC.translate("&cTiempo inválido: " + e.getMessage()));
            return;
        }

        String bonusId = "darbonus_" + UUID.randomUUID().toString().substring(0, 8);

        try {
            IDBCPlayer idbcPlayer = General.getDBCPlayer(target.getName());
            idbcPlayer.addBonusAttribute(bonusStat, bonusId, operation, value);

            String timeDisplay = BoosterParser.formatSecondsToTime(seconds);

            command.getSender().sendMessage(CC.translate("&a✓ Bonus &e" + stat + " " + displayValue + " &adado a &e" + target.getName() + " &apor &6" + timeDisplay));
            target.sendMessage(CC.translate("&a⚡ Has recibido &6" + displayValue + " " + stat + " &adurante &6" + timeDisplay));

            final Player finalTarget = target;
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        IDBCPlayer p = General.getDBCPlayer(finalTarget.getName());
                        p.removeBonusAttribute(bonusStat, bonusId);
                        finalTarget.sendMessage(CC.translate("&c✗ El bonus de &e" + stat + " &cha expirado."));
                    } catch (Exception ignored) {
                    }
                }
            }.runTaskLater(Main.instance, seconds * 20L);

        } catch (Exception e) {
            command.getSender().sendMessage(CC.translate("&cError al aplicar bonus: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}
