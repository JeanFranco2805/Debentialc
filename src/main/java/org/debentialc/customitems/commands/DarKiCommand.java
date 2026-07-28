package org.debentialc.customitems.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.debentialc.customitems.tools.permissions.Permissions;
import org.debentialc.service.CC;
import org.debentialc.service.General;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.io.IOException;

public class DarKiCommand extends BaseCommand {

    @Command(name = "darki", permission = Permissions.COMMAND + "darki", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        if (command.length() < 2) {
            command.getSender().sendMessage(CC.translate("&cUso: /darki <jugador> <cantidad>"));
            return;
        }

        String targetName = command.getArgs(0);
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            command.getSender().sendMessage(CC.translate("&cJugador no encontrado: &f" + targetName));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(command.getArgs(1));
        } catch (NumberFormatException e) {
            command.getSender().sendMessage(CC.translate("&cCantidad inválida: &f" + command.getArgs(1)));
            return;
        }

        try {
            General.setRelease(target, (byte) amount);
            command.getSender().sendMessage(CC.translate("&a✓ Se ha dado &6" + amount + " &ade Ki a &6" + target.getName()));
            target.sendMessage(CC.translate("&a⚡ Has recibido &6" + amount + " &ade Ki."));
        } catch (Exception e) {
            command.getSender().sendMessage(CC.translate("&cError al dar Ki: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}
