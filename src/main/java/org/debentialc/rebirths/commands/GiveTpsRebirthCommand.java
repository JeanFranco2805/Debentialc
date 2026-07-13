package org.debentialc.rebirths.commands;

import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.entity.Player;
import org.debentialc.Main;

import java.util.Locale;
import org.debentialc.boosters.integration.BoosterTPAPI;
import org.debentialc.customitems.tools.permissions.Permissions;
import org.debentialc.rebirths.managers.RebirthBlockManager;
import org.debentialc.rebirths.managers.RebirthManager;
import org.debentialc.rebirths.model.Rebirth;
import org.debentialc.rebirths.model.RebirthBlock;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.io.IOException;
import java.util.List;

public class GiveTpsRebirthCommand extends BaseCommand {

    @Command(name = "dartprebirth", aliases = {"dartpr", "giverebirthtps"}, permission = Permissions.COMMAND + "dartprebirth", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        if (command.length() < 2) {
            command.getSender().sendMessage(CC.translate("&cUso: /dartprebirth <jugador> <cantidad> [bloque]"));
            return;
        }

        String targetName = command.getArgs(0);
        Player target = Main.instance.getServer().getPlayer(targetName);
        if (target == null) {
            command.getSender().sendMessage(CC.translate("&cJugador no encontrado: " + targetName));
            return;
        }

        int baseTPs;
        try {
            baseTPs = Integer.parseInt(command.getArgs(1));
            if (baseTPs <= 0) {
                command.getSender().sendMessage(CC.translate("&cLa cantidad debe ser mayor a 0."));
                return;
            }
        } catch (NumberFormatException e) {
            command.getSender().sendMessage(CC.translate("&cCantidad inválida de TPs."));
            return;
        }

        Integer blockId = null;
        if (command.length() >= 3) {
            try {
                blockId = Integer.parseInt(command.getArgs(2));
            } catch (NumberFormatException e) {
                command.getSender().sendMessage(CC.translate("&cID de bloque inválido."));
                return;
            }
        }

        applyRebirthAndBoosterTPs(command.getSender(), target, baseTPs, blockId);
    }

    private void applyRebirthAndBoosterTPs(org.bukkit.command.CommandSender sender, Player target, int baseTPs, Integer blockId) {
        try {
            IDBCPlayer dbcPlayer = NpcAPI.Instance().getPlayer(target.getName()).getDBCPlayer();
            if (dbcPlayer == null) {
                sender.sendMessage(CC.translate("&cError: El jugador no tiene datos de DBC"));
                return;
            }

            double rebirthMultiplier;
            String multiplierSource;

            if (blockId != null) {
                RebirthBlock block = RebirthBlockManager.getInstance().getBlock(blockId);
                if (block == null) {
                    sender.sendMessage(CC.translate("&c✗ Bloque no encontrado: &f" + blockId));
                    return;
                }

                double totalPercent = 0.0;
                int playerRebirthLevel = RebirthManager.getInstance().getPlayerRebirthLevel(target);
                List<Rebirth> blockRebirths = RebirthManager.getInstance().getRebirthsInBlock(blockId);

                for (Rebirth rebirth : blockRebirths) {
                    if (playerRebirthLevel >= rebirth.getId()) {
                        totalPercent += rebirth.getTpBonusPercent();
                    }
                }

                rebirthMultiplier = 1.0 + (totalPercent / 100.0);
                multiplierSource = "Bloque " + blockId + " (" + String.format("%.2f", rebirthMultiplier) + "x)";
            } else {
                rebirthMultiplier = RebirthManager.getInstance().getRebirthMultiplier(target);
                multiplierSource = "Rebirth actual (" + String.format("%.2f", rebirthMultiplier) + "x)";
            }

            double boosterMultiplier = BoosterTPAPI.getCombinedMultiplier(target);
            double combinedMultiplier = rebirthMultiplier * boosterMultiplier;

            int totalTPs = (int) Math.round(baseTPs * combinedMultiplier);

            int currentTP = dbcPlayer.getTP();
            dbcPlayer.setTP(currentTP + totalTPs);

            sender.sendMessage(CC.translate("&a✓ Se han dado &6" + String.format(Locale.US, "%,d", totalTPs) + " TPs &aa &6" + target.getName()));
            sender.sendMessage(CC.translate("&7Multiplicador: &f" + multiplierSource + " &7x &fBooster " + String.format("%.2f", boosterMultiplier) + "x"));
            target.sendMessage(CC.translate("&8[&c+&8] &c" + String.format(Locale.US, "%,d", totalTPs) + " TPS"));

        } catch (Exception e) {
            sender.sendMessage(CC.translate("&cError al dar TPs: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}
