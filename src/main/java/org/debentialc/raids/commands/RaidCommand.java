package org.debentialc.raids.commands;

import org.debentialc.raids.menus.RaidUserMenu;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;
import org.bukkit.entity.Player;

/**
 * RaidCommand - Comando público /raid (alias /raids)
 * Abre un menú con las raids disponibles para iniciar como party.
 */
public class RaidCommand extends BaseCommand {

    @Command(name = "raid",
            aliases = {"raids"},
            description = "Abre el menú de raids disponibles",
            usage = "/raid",
            inGameOnly = true)
    public void onCommand(CommandArgs args) {
        Player player = args.getPlayer();
        if (player == null) {
            return;
        }

        try {
            RaidUserMenu.open(player);
        } catch (Exception e) {
            player.sendMessage(org.debentialc.service.CC.translate("&c✗ Error al abrir el menú de raids. Revisa la consola."));
            e.printStackTrace();
        }
    }
}
