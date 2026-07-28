package org.debentialc.customitems.commands;

import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.debentialc.customitems.tools.permissions.Permissions;
import org.debentialc.service.CC;
import org.debentialc.service.General;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.io.IOException;

public class DarmaestriaCommand extends BaseCommand {

    @Command(name = "darmaestria", permission = Permissions.COMMAND + "darmaestria", inGameOnly = false)
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        if (command.length() < 2) {
            command.getSender().sendMessage(CC.translate("&cUso: /darmaestria <jugador> <cantidad>"));
            return;
        }

        String targetName = command.getArgs(0);
        double cantidad;
        try {
            cantidad = Double.parseDouble(command.getArgs(1));
        } catch (NumberFormatException e) {
            command.getSender().sendMessage(CC.translate("&cCantidad inválida."));
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            command.getSender().sendMessage(CC.translate("&cJugador no encontrado: &f" + targetName));
            return;
        }

        try {
            IDBCPlayer idbcPlayer = NpcAPI.Instance().getPlayer(target.getName()).getDBCPlayer();
            General.giveMastery(idbcPlayer, cantidad);
            command.getSender().sendMessage(CC.translate("&a✓ Se han dado &6" + cantidad + " &apuntos de maestría a &6" + target.getName()));
            target.sendMessage(CC.translate("&a¡Has ganado &6" + cantidad + " &apuntos de maestría!"));
        } catch (Exception e) {
            command.getSender().sendMessage(CC.translate("&cError al dar maestría: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}
