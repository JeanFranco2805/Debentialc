package org.debentialc.rebirths.commands;

import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.debentialc.Main;
import org.debentialc.boosters.integration.BoosterTPAPI;
import org.debentialc.customitems.tools.permissions.Permissions;
import org.debentialc.rebirths.managers.RebirthManager;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.io.IOException;

public class GiveTpsRebirthCommand extends BaseCommand {

    @Command(name = "dartprebirth", aliases = {"dartpr", "giverebirthtps"}, permission = Permissions.COMMAND + "dartprebirth", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        if (command.length() < 2) {
            command.getSender().sendMessage(CC.translate("&cUso: /dartprebirth <jugador> <cantidad>"));
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

        applyRebirthAndBoosterTPs(command.getSender(), target, baseTPs);
    }

    private void applyRebirthAndBoosterTPs(org.bukkit.command.CommandSender sender, Player target, int baseTPs) {
        try {
            IDBCPlayer dbcPlayer = NpcAPI.Instance().getPlayer(target.getName()).getDBCPlayer();
            if (dbcPlayer == null) {
                sender.sendMessage(CC.translate("&cError: El jugador no tiene datos de DBC"));
                return;
            }

            double rebirthMultiplier = RebirthManager.getInstance().getRebirthMultiplier(target);
            double boosterMultiplier = BoosterTPAPI.getCombinedMultiplier(target);
            double combinedMultiplier = rebirthMultiplier * boosterMultiplier;

            int totalTPs = (int) Math.round(baseTPs * combinedMultiplier);

            int currentTP = dbcPlayer.getTP();
            dbcPlayer.setTP(currentTP + totalTPs);

            sender.sendMessage(CC.translate("&a✓ Se han dado &6" + totalTPs + " TPs &a a &6" + target.getName()));
            target.sendMessage(CC.translate("&a+ " + totalTPs + " TPs"));

        } catch (Exception e) {
            sender.sendMessage(CC.translate("&cError al dar TPs: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}
