package org.debentialc.rebirths.commands;

import org.bukkit.entity.Player;
import org.debentialc.customitems.tools.permissions.Permissions;
import org.debentialc.rebirths.menus.RebirthPlayerMenus;
import org.debentialc.service.CC;
import org.debentialc.service.commands.BaseCommand;
import org.debentialc.service.commands.Command;
import org.debentialc.service.commands.CommandArgs;

import java.io.IOException;

public class RebirthCommand extends BaseCommand {

    @Command(name = "rebirth", aliases = {"rebirths", "reborn"}, permission = Permissions.COMMAND + "rebirth")
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        if (!command.isPlayer()) {
            command.getSender().sendMessage(CC.translate("&cEste comando solo puede usarse en el juego."));
            return;
        }

        Player player = command.getPlayer();
        RebirthPlayerMenus.createMainMenu(player).open(player);
    }
}
