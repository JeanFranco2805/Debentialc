package org.debentialc.rebirths.commands;

import org.bukkit.entity.Player;
import org.debentialc.customitems.tools.permissions.Permissions;
import org.debentialc.rebirths.menus.RebirthAdminMenus;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.io.IOException;

public class RebirthAdminCommand extends BaseCommand {

    @Command(name = "rebirthadmin", aliases = {"radm", "rebirthadm"}, permission = Permissions.COMMAND + "rebirth.admin")
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        if (!command.isPlayer()) {
            command.getSender().sendMessage(CC.translate("&cEste comando solo puede usarse en el juego."));
            return;
        }

        Player player = command.getPlayer();
        RebirthAdminMenus.createMainMenu().open(player);
    }
}
