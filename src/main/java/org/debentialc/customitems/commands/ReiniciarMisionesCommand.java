package org.debentialc.customitems.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.debentialc.customitems.tools.permissions.Permissions;
import org.debentialc.service.CC;
import org.debentialc.service.General;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

public class ReiniciarMisionesCommand extends BaseCommand {

    @Command(name = "reiniciarMisiones",
            aliases = {"reiniciarmisiones", "rmisiones", "restartallquest"},
            permission = Permissions.COMMAND + "reiniciarmisiones",
            description = "Reinicia todas las misiones completadas de un jugador",
            usage = "/reiniciarMisiones <jugador>",
            inGameOnly = false)
    public void onCommand(CommandArgs args) {
        if (args.length() < 1) {
            args.getSender().sendMessage(CC.translate("&cUso: /reiniciarMisiones <jugador>"));
            return;
        }

        String targetName = args.getArgs(0);
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            args.getSender().sendMessage(CC.translate("&cJugador no encontrado: &f" + targetName));
            return;
        }

        try {
            General.restartAllQuest(target);
            args.getSender().sendMessage(CC.translate("&a\u2713 Misiones reiniciadas para &e" + target.getName()));
            target.sendMessage(CC.translate("&a\u26a1 Tus misiones han sido reiniciadas."));
        } catch (Exception e) {
            args.getSender().sendMessage(CC.translate("&cError al reiniciar misiones: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}
