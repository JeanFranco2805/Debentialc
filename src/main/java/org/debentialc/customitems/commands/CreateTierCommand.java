package org.debentialc.customitems.commands;

import org.bukkit.entity.Player;
import org.debentialc.customitems.tools.fragments.FragmentManager;
import org.debentialc.customitems.tools.permissions.Permissions;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

public class CreateTierCommand extends BaseCommand {

    @Command(name = "createtier", aliases = {"ctier"}, permission = Permissions.COMMAND + "fragmentadmin")
    public void onCommand(CommandArgs command) {
        Player player = command.getPlayer();
        if (player == null) {
            command.getSender().sendMessage(CC.translate("&c\u2717 Solo jugadores pueden usar este comando."));
            return;
        }
        if (command.length() < 1) {
            player.sendMessage(CC.translate("&cUso: /createtier <nombre>"));
            return;
        }
        String tierName = command.getArgs(0).toUpperCase();
        if (!tierName.matches("^[A-Z0-9_]+$")) {
            player.sendMessage(CC.translate("&c\u2717 Nombre inv\u00e1lido. Solo may\u00fasculas, n\u00fameros y guiones bajos."));
            return;
        }
        if (FragmentManager.getInstance().getTierConfig().getAllTiers().containsKey(tierName)) {
            player.sendMessage(CC.translate("&c\u2717 El tier &f" + tierName + " &cya existe."));
            return;
        }
        String[] attributes = {"STR", "CON", "DEX", "WIL", "MND", "SPI"};
        boolean success = true;
        for (String attr : attributes) {
            if (!FragmentManager.getInstance().getTierConfig().setLimit(tierName, attr, 10)) {
                success = false;
                break;
            }
        }
        if (success) {
            player.sendMessage(CC.translate("&a\u2713 Tier &f" + tierName + " &acreado con l\u00edmite inicial de &f10&a."));
        } else {
            player.sendMessage(CC.translate("&c\u2717 Error al crear el tier."));
        }
    }
}
